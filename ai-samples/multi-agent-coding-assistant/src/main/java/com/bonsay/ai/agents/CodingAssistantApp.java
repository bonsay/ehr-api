package com.bonsay.ai.agents;

import com.bonsay.ai.agents.graph.StateGraph;
import com.bonsay.ai.agents.sandbox.DockerSandbox;
import com.bonsay.ai.agents.sandbox.ProcessSandbox;
import com.bonsay.ai.agents.sandbox.Sandbox;
import com.bonsay.ai.llm.AnthropicClient;
import com.bonsay.ai.llm.LlmClient;

import java.time.Duration;

/**
 * CLI: {@code java -jar multi-agent-coding-assistant-1.0.0-all.jar "Implement a function that ..."}
 * Uses Claude when ANTHROPIC_API_KEY is set, otherwise an offline scripted demo.
 * Uses Docker isolation when available (set SANDBOX=process to force the process sandbox).
 */
public final class CodingAssistantApp {

    public static void main(String[] args) {
        boolean live = AnthropicClient.isConfigured();
        String task = args.length > 0 ? String.join(" ", args)
                : "Implement a function that converts an integer (1..3999) to a Roman numeral string.";
        LlmClient llm = live ? AnthropicClient.fromEnv() : DemoScriptedModel.create();

        Duration timeout = Duration.ofSeconds(10);
        boolean docker = !"process".equals(System.getenv("SANDBOX")) && DockerSandbox.dockerAvailable();
        Sandbox sandbox = docker ? new DockerSandbox(timeout, "eclipse-temurin:21-jre") : new ProcessSandbox(timeout);

        System.out.printf("Task: %s%nModel: %s | Sandbox: %s%n%n", task, live ? "Claude" : "offline demo",
                docker ? "docker (--network none)" : "process");

        StateGraph.Result<CodingState> result = new CodingWorkflow(llm, sandbox, 4).run(task,
                (step, node, s) -> System.out.printf("[%02d] %-11s %s%n", step, node, s.log.get(s.log.size() - 1)));

        CodingState s = result.state();
        System.out.println("\nPath: " + String.join(" -> ", result.path()));
        System.out.println(s.succeeded() ? "\nSUCCESS - final implementation:\n" : "\nFAILED - last attempt:\n");
        System.out.println(s.sourceCode);
        System.exit(s.succeeded() ? 0 : 1);
    }
}
