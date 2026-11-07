package com.bonsay.ai.agents.sandbox;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Runs compiled classes inside a locked-down container: no network, read-only root FS,
 * memory/CPU/PID limits, all capabilities dropped, non-root user. Compilation still happens on
 * the host (javac does not execute code), so only bytecode is mounted - read-only.
 */
public class DockerSandbox extends ProcessSandbox {

    private final String image;

    public DockerSandbox(Duration timeout, String image) {
        super(timeout);
        this.image = image;
    }

    public static boolean dockerAvailable() {
        try {
            Process p = new ProcessBuilder("docker", "info").redirectErrorStream(true).start();
            p.getInputStream().transferTo(java.io.OutputStream.nullOutputStream());
            return p.waitFor(5, TimeUnit.SECONDS) && p.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    protected List<String> command(Path work, Path classes, String mainClass) {
        return List.of("docker", "run", "--rm",
                "--network", "none",
                "--read-only", "--tmpfs", "/tmp:rw,size=16m",
                "--memory", "256m", "--cpus", "1", "--pids-limit", "64",
                "--cap-drop", "ALL", "--security-opt", "no-new-privileges",
                "--user", "65534:65534",
                "-v", classes.toAbsolutePath() + ":/sandbox:ro",
                image, "java", "-Xmx128m", "-cp", "/sandbox", mainClass);
    }
}
