package com.bonsay.ai.agents.sandbox;

import javax.tools.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Stream;

/**
 * Sandbox that compiles in-process (javac never executes the code) and runs the result in a
 * separate, short-lived JVM with: an empty environment, a throwaway working directory, a heap
 * cap, a wall-clock timeout with forced kill, and capped output. Use {@link DockerSandbox}
 * for real network/filesystem isolation.
 */
public class ProcessSandbox implements Sandbox {

    protected static final int MAX_OUTPUT_BYTES = 64 * 1024;
    protected final Duration timeout;

    public ProcessSandbox(Duration timeout) {
        this.timeout = timeout;
    }

    @Override
    public ExecutionResult run(Map<String, String> sources, String mainClass) {
        Path work = null;
        try {
            work = Files.createTempDirectory("agent-sandbox-");
            Path classes = Files.createDirectories(work.resolve("classes"));
            Map<String, String> all = new LinkedHashMap<>(sources);
            all.put("Check", TestHarness.CHECK_SOURCE);
            String errors = compile(all, classes);
            if (errors != null) return ExecutionResult.compileFailure(errors);
            return execute(work, classes, mainClass);
        } catch (IOException e) {
            throw new IllegalStateException("sandbox I/O failure", e);
        } finally {
            deleteQuietly(work);
        }
    }

    /** Returns null on success, else the diagnostics. */
    protected String compile(Map<String, String> sources, Path outDir) {
        JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
        if (javac == null) throw new IllegalStateException("A JDK (not a JRE) is required to compile generated code");
        List<JavaFileObject> units = new ArrayList<>();
        sources.forEach((name, src) -> units.add(new SimpleJavaFileObject(
                java.net.URI.create("string:///" + name + ".java"), JavaFileObject.Kind.SOURCE) {
            @Override
            public CharSequence getCharContent(boolean ignoreEncodingErrors) { return src; }
        }));
        DiagnosticCollector<JavaFileObject> diags = new DiagnosticCollector<>();
        StringWriter out = new StringWriter();
        boolean ok = javac.getTask(out, null, diags,
                List.of("-d", outDir.toString(), "--release", "21", "-proc:none", "-Xlint:none"), null, units).call();
        if (ok) return null;
        StringBuilder sb = new StringBuilder();
        for (Diagnostic<? extends JavaFileObject> d : diags.getDiagnostics()) {
            if (d.getKind() != Diagnostic.Kind.ERROR) continue;
            String file = d.getSource() == null ? "?" : d.getSource().getName().replace("/", "");
            sb.append(file).append(':').append(d.getLineNumber()).append(": ").append(d.getMessage(Locale.ROOT)).append('\n');
        }
        return sb.toString();
    }

    protected List<String> command(Path work, Path classes, String mainClass) {
        String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        return List.of(java, "-Xmx128m", "-Xss1m", "-XX:+UseSerialGC", "-XX:TieredStopAtLevel=1",
                "-Djava.awt.headless=true", "-cp", classes.toString(), mainClass);
    }

    protected ExecutionResult execute(Path work, Path classes, String mainClass) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(command(work, classes, mainClass)).directory(work.toFile());
        pb.environment().clear();          // no secrets (API keys etc.) leak into generated code
        pb.environment().put("PATH", "/usr/bin:/bin");
        long start = System.nanoTime();
        Process p = pb.start();
        p.getOutputStream().close();
        ExecutorService readers = Executors.newVirtualThreadPerTaskExecutor();
        Future<String> out = readers.submit(() -> readCapped(p.getInputStream()));
        Future<String> err = readers.submit(() -> readCapped(p.getErrorStream()));
        boolean finished = false;
        try {
            finished = p.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!finished) {
                p.descendants().forEach(ProcessHandle::destroyForcibly);
                p.destroyForcibly().waitFor(5, TimeUnit.SECONDS);
            }
            long ms = (System.nanoTime() - start) / 1_000_000;
            return new ExecutionResult(true, "", finished ? p.exitValue() : -1,
                    getQuietly(out), getQuietly(err), !finished, ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            p.destroyForcibly();
            throw new IllegalStateException(e);
        } finally {
            readers.shutdownNow();
        }
    }

    private static String getQuietly(Future<String> f) {
        try {
            return f.get(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "";
        } catch (ExecutionException | TimeoutException e) {
            return "";   // stream closed by a forced kill - partial output is acceptable
        }
    }

    private static String readCapped(InputStream in) throws IOException {
        byte[] buf = in.readNBytes(MAX_OUTPUT_BYTES);
        in.transferTo(java.io.OutputStream.nullOutputStream()); // drain so the child never blocks
        return new String(buf, StandardCharsets.UTF_8);
    }

    protected static void deleteQuietly(Path dir) {
        if (dir == null) return;
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
        } catch (IOException ignored) {
            // best effort
        }
    }
}
