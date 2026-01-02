///usr/bin/env jbang "$0" "$@" ; exit $?

//DEPS com.google.code.gson:gson:2.10.1

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * TeamAggregator - Aggregates individual standup reports into team summary
 *
 * Usage: jbang TeamAggregator.java <team-reports> [prompt-path]
 */
public class TeamAggregator {

    public static String loadPromptTemplate(String promptPath) throws IOException {
        if (!Files.exists(Paths.get(promptPath))) {
            throw new IOException("Prompt template not found: " + promptPath);
        }
        return Files.readString(Paths.get(promptPath));
    }

    public static String generateTeamReport(String teamReports, String promptPath)
            throws IOException, InterruptedException {

        // Load team prompt template
        String promptTemplate = loadPromptTemplate(promptPath);

        // Inject team reports
        String fullPrompt = promptTemplate.replace("{{team_reports}}", teamReports);

        // Call claude -p with the team prompt
        ProcessBuilder processBuilder = new ProcessBuilder("claude", "-p", fullPrompt);
        processBuilder.inheritIO();

        Process process = processBuilder.start();
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new RuntimeException("Claude invocation failed with exit code: " + exitCode);
        }

        return "Team report generated successfully";
    }

    public static void main(String... args) {
        try {
            if (args.length < 1) {
                System.err.println("Usage: jbang TeamAggregator.java <team-reports> [prompt-path]");
                System.exit(1);
            }

            String teamReports = args[0];
            String promptPath = args.length > 1 ? args[1] : "prompts/team.prompt.md";

            generateTeamReport(teamReports, promptPath);

        } catch (Exception e) {
            System.err.println("Error generating team report: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
