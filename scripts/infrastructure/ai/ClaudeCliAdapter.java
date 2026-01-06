import java.io.*;

/**
 * Infrastructure adapter that implements ReportGeneratorPort
 * using the Claude CLI.
 *
 * Uses claude -p for prompt mode with streaming output.
 */
public class ClaudeCliAdapter implements ReportGeneratorPort {

    @Override
    public void generate(String prompt) {
        try {
            ProcessBuilder pb = new ProcessBuilder("claude", "-p", prompt);

            // inheritIO() pipes claude's streaming output directly to stdout
            // This provides real-time output without buffering
            pb.inheritIO();

            Process process = pb.start();
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                System.err.println("Warning: Claude CLI exited with code " + exitCode);
            }
        } catch (IOException e) {
            System.err.println("Error: Could not execute claude CLI - " + e.getMessage());
            System.err.println("Make sure Claude CLI is installed and authenticated.");

            // Fallback: print the prompt so user can see what would be sent
            System.out.println("\n--- Prompt that would be sent to Claude ---\n");
            System.out.println(prompt);
            System.out.println("\n--- End of prompt ---");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Error: Claude generation was interrupted");
        }
    }
}
