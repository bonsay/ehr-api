package com.bonsay.ai.agents.sandbox;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Static deny-list for generated code, applied before anything is compiled or executed. This is
 * defence in depth on top of process / container isolation - never the only layer.
 */
public final class CodePolicy {

    private record Rule(String id, Pattern pattern) {}

    private static final List<Rule> RULES = List.of(
            new Rule("PROCESS_EXEC", Pattern.compile("\\b(Runtime\\s*\\.\\s*getRuntime|ProcessBuilder|ProcessHandle)\\b")),
            new Rule("NETWORK", Pattern.compile("\\bjava\\s*\\.\\s*net\\b|\\b(Socket|URL|HttpClient|URI)\\s*[.(]")),
            new Rule("FILESYSTEM", Pattern.compile("\\bjava\\s*\\.\\s*(io|nio)\\b|\\b(Files|Paths|FileInputStream|FileOutputStream|RandomAccessFile)\\b")),
            new Rule("REFLECTION", Pattern.compile("\\bjava\\s*\\.\\s*lang\\s*\\.\\s*(reflect|invoke)\\b|\\bClass\\s*\\.\\s*forName|\\bgetDeclared(Method|Field|Constructor)|setAccessible")),
            new Rule("CLASSLOADING", Pattern.compile("\\bClassLoader\\b|\\bdefineClass\\b")),
            new Rule("UNSAFE", Pattern.compile("\\bsun\\s*\\.\\s*misc\\b|\\bjdk\\s*\\.\\s*internal\\b|\\bUnsafe\\b|\\bnative\\b")),
            new Rule("EXIT", Pattern.compile("\\bSystem\\s*\\.\\s*(exit|setProperty|getenv|load|loadLibrary)\\b")),
            new Rule("THREADS", Pattern.compile("\\bnew\\s+Thread\\b|\\bExecutors\\b|\\bThread\\s*\\.\\s*ofVirtual")));

    private CodePolicy() {}

    public static List<String> check(String source) {
        String code = stripComments(source);
        List<String> violations = new ArrayList<>();
        for (Rule r : RULES) if (r.pattern().matcher(code).find()) violations.add(r.id());
        return violations;
    }

    private static String stripComments(String s) {
        return s.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("//[^\\n]*", "");
    }
}
