///usr/bin/env jbang "$0" "$@" ; exit $?

//DEPS com.google.code.gson:gson:2.10.1

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * GenerateReport - Generates standup report using Claude AI
 *
 * Usage: jbang GenerateReport.java <activity-json> <diff-summary> [prompt-path]
 */
public class GenerateReport {

    public static String loadPromptTemplate(String promptPath) throws IOException {
        Path path = Paths.get(promptPath);
        if (!Files.exists(path)) {
            throw new IOException("Prompt template not found: " + promptPath);
        }
        return Files.readString(path);
    }

    public static String formatActivities(JsonObject activity) {
        StringBuilder sb = new StringBuilder();

        // Format commits
        JsonArray commits = activity.getAsJsonArray("commits");
        if (commits != null && commits.size() > 0) {
            sb.append("COMMITS:\n");
            for (JsonElement commitElement : commits) {
                JsonObject commit = commitElement.getAsJsonObject();
                JsonObject commitData = commit.getAsJsonObject("commit");
                JsonObject repo = commit.getAsJsonObject("repository");

                String repoName = repo.get("nameWithOwner").getAsString();
                String message = commitData.get("message").getAsString().split("\n")[0]; // First line only
                String sha = commit.get("sha").getAsString().substring(0, 7);

                sb.append("- [").append(repoName).append("] ").append(message)
                        .append(" (").append(sha).append(")\n");
            }
            sb.append("\n");
        }

        // Format pull requests
        JsonArray prs = activity.getAsJsonArray("pull_requests");
        if (prs != null && prs.size() > 0) {
            sb.append("PULL REQUESTS:\n");
            for (JsonElement prElement : prs) {
                JsonObject pr = prElement.getAsJsonObject();
                JsonObject repo = pr.getAsJsonObject("repository");

                String repoName = repo.get("nameWithOwner").getAsString();
                int number = pr.get("number").getAsInt();
                String title = pr.get("title").getAsString();
                String state = pr.get("state").getAsString();

                sb.append("- [").append(repoName).append("] #").append(number).append(": ")
                        .append(title).append(" (").append(state).append(")\n");
            }
            sb.append("\n");
        }

        // Format issues
        JsonArray issues = activity.getAsJsonArray("issues");
        if (issues != null && issues.size() > 0) {
            sb.append("ISSUES:\n");
            for (JsonElement issueElement : issues) {
                JsonObject issue = issueElement.getAsJsonObject();
                JsonObject repo = issue.getAsJsonObject("repository");

                String repoName = repo.get("nameWithOwner").getAsString();
                int number = issue.get("number").getAsInt();
                String title = issue.get("title").getAsString();
                String state = issue.get("state").getAsString();

                sb.append("- [").append(repoName).append("] #").append(number).append(": ")
                        .append(title).append(" (").append(state).append(")\n");
            }
            sb.append("\n");
        }

        if (sb.length() == 0) {
            return "No GitHub activity found for this period.";
        }

        return sb.toString();
    }

    public static String generateStandupReport(JsonObject activity, String diffSummary, String promptPath)
            throws IOException, InterruptedException {

        System.err.println("[DEBUG] Starting generateStandupReport...");
        System.err.println("[DEBUG] Prompt path: " + promptPath);

        // Load prompt template
        System.err.println("[DEBUG] Loading prompt template...");
        String promptTemplate = loadPromptTemplate(promptPath);
        System.err.println("[DEBUG] Prompt template loaded, length: " + promptTemplate.length());

        // Format activities
        System.err.println("[DEBUG] Formatting activities...");
        String formattedActivities = formatActivities(activity);
        System.err.println("[DEBUG] Activities formatted, length: " + formattedActivities.length());
        System.err.println("[DEBUG] Activities content: " + formattedActivities);

        // Inject data into template
        System.err.println("[DEBUG] Injecting data into template...");
        String fullPrompt = promptTemplate
                .replace("{{activities}}", formattedActivities)
                .replace("{{diffs}}", diffSummary != null ? diffSummary : "No diff data available.");
        System.err.println("[DEBUG] Full prompt generated, length: " + fullPrompt.length());

        // Call claude -p with the full prompt
        System.err.println("[DEBUG] Creating ProcessBuilder for 'claude -p'...");
        System.err.println("[DEBUG] Checking if 'claude' command is available...");
        ProcessBuilder processBuilder = new ProcessBuilder("claude", "-p", fullPrompt);

        // Use inheritIO for seamless output piping
        System.err.println("[DEBUG] Setting inheritIO...");
        processBuilder.inheritIO();

        // Execute the command
        System.err.println("[DEBUG] Starting claude process...");
        Process process = processBuilder.start();
        System.err.println("[DEBUG] Claude process started, waiting for completion...");

        // Wait for the process to complete
        int exitCode = process.waitFor();
        System.err.println("[DEBUG] Claude process completed with exit code: " + exitCode);

        if (exitCode != 0) {
            throw new RuntimeException("Claude invocation failed with exit code: " + exitCode);
        }

        System.err.println("[DEBUG] Report generated successfully");
        return "Report generated successfully";
    }

    public static void main(String... args) {
        try {
            if (args.length < 2) {
                System.err.println("Usage: jbang GenerateReport.java <activity-json> <diff-summary> [prompt-path]");
                System.exit(1);
            }

            String activityJson = args[0];
            String diffSummary = args[1];
            String promptPath = args.length > 2 ? args[2] : "prompts/standup.prompt.md";

            JsonObject activity = JsonParser.parseString(activityJson).getAsJsonObject();

            String result = generateStandupReport(activity, diffSummary, promptPath);

            // IMPORTANT: Print to stdout so Main.java knows we're done
            // (even though claude output was piped via inheritIO)
            System.out.println(result);

        } catch (Exception e) {
            System.err.println("Error generating report: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
