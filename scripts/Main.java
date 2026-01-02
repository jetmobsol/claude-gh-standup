///usr/bin/env jbang "$0" "$@" ; exit $?

//DEPS com.google.code.gson:gson:2.10.1

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Main - Entry point for claude-gh-standup slash command
 *
 * Usage: jbang Main.java [--days N] [--user USERNAME] [--repo REPO] [--format FORMAT] [--team USERS...] [--output FILE]
 */
public class Main {

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    static class Args {
        int days = 1;
        String user = null;
        String repo = null;
        String format = "markdown";
        List<String> team = null;
        String output = null;
    }

    public static Args parseArgs(String[] args) {
        Args parsed = new Args();

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--days":
                case "-d":
                    if (i + 1 < args.length) {
                        parsed.days = Integer.parseInt(args[++i]);
                    }
                    break;
                case "--user":
                case "-u":
                    if (i + 1 < args.length) {
                        parsed.user = args[++i];
                    }
                    break;
                case "--repo":
                case "-r":
                    if (i + 1 < args.length) {
                        parsed.repo = args[++i];
                    }
                    break;
                case "--format":
                case "-f":
                    if (i + 1 < args.length) {
                        parsed.format = args[++i];
                    }
                    break;
                case "--team":
                    parsed.team = new ArrayList<>();
                    while (i + 1 < args.length && !args[i + 1].startsWith("--")) {
                        parsed.team.add(args[++i]);
                    }
                    break;
                case "--output":
                case "-o":
                    if (i + 1 < args.length) {
                        parsed.output = args[++i];
                    }
                    break;
                case "--help":
                case "-h":
                    printHelp();
                    System.exit(0);
                    break;
            }
        }

        return parsed;
    }

    public static void printHelp() {
        System.out.println("claude-gh-standup - Generate AI-powered GitHub standup reports");
        System.out.println();
        System.out.println("Usage: /claude-gh-standup [options]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --days, -d N        Number of days to look back (default: 1)");
        System.out.println("  --user, -u USER     GitHub username (default: current user)");
        System.out.println("  --repo, -r REPO     Filter by repository (format: owner/repo)");
        System.out.println("  --format, -f FMT    Output format: markdown, json, html (default: markdown)");
        System.out.println("  --team USERS...     Generate team report for multiple users");
        System.out.println("  --output, -o FILE   Write to file instead of stdout");
        System.out.println("  --help, -h          Show this help message");
    }

    public static String getCurrentUser() throws Exception {
        ProcessBuilder pb = new ProcessBuilder("gh", "api", "user", "--jq", ".login");
        Process process = pb.start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Failed to get current user from gh CLI");
        }

        return output.toString().trim();
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

                String repoName = repo.get("fullName").getAsString();
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

                String repoName = repo.get("fullName").getAsString();
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

                String repoName = repo.get("fullName").getAsString();
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

    public static String getCurrentRepository() throws Exception {
        // Check if we're in a git repository
        ProcessBuilder checkGit = new ProcessBuilder("git", "rev-parse", "--git-dir");
        Process checkProcess = checkGit.start();
        int checkExit = checkProcess.waitFor();

        if (checkExit != 0) {
            return null; // Not in a git repository
        }

        // Get remote URL
        ProcessBuilder pb = new ProcessBuilder("git", "remote", "get-url", "origin");
        Process process = pb.start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            return null; // No remote configured
        }

        String remoteUrl = output.toString().trim();

        // Extract owner/repo from GitHub URL
        // Supports: https://github.com/owner/repo.git or git@github.com:owner/repo.git
        if (remoteUrl.contains("github.com")) {
            String[] parts = remoteUrl.split("github.com[:/]");
            if (parts.length > 1) {
                String repoPath = parts[1].replaceAll("\\.git$", "");
                return repoPath;
            }
        }

        return null;
    }

    public static String runScript(String scriptName, List<String> scriptArgs) throws Exception {
        System.err.println("[DEBUG] runScript called for: " + scriptName);
        System.err.println("[DEBUG] Number of args: " + scriptArgs.size());
        for (int i = 0; i < scriptArgs.size(); i++) {
            System.err.println("[DEBUG] Arg " + i + " length: " + scriptArgs.get(i).length());
        }

        Path scriptPath = Paths.get("scripts", scriptName);
        if (!Files.exists(scriptPath)) {
            // Try relative to current directory
            scriptPath = Paths.get(scriptName);
        }
        System.err.println("[DEBUG] Script path: " + scriptPath);

        List<String> command = new ArrayList<>();
        command.add("jbang");
        command.add(scriptPath.toString());
        command.addAll(scriptArgs);

        System.err.println("[DEBUG] Creating ProcessBuilder...");
        ProcessBuilder pb = new ProcessBuilder(command);
        System.err.println("[DEBUG] Starting process...");
        Process process = pb.start();
        System.err.println("[DEBUG] Process started, reading stdout...");

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }
        System.err.println("[DEBUG] Finished reading stdout, waiting for exit...");

        int exitCode = process.waitFor();
        System.err.println("[DEBUG] Process exited with code: " + exitCode);

        if (exitCode != 0) {
            StringBuilder error = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    error.append(line).append("\n");
                }
            }
            System.err.println("Error running " + scriptName + ":");
            System.err.println(error.toString());
            throw new RuntimeException("Script " + scriptName + " failed with exit code " + exitCode);
        }

        System.err.println("[DEBUG] runScript completed for: " + scriptName);
        return output.toString();
    }

    public static void main(String... args) {
        try {
            Args parsed = parseArgs(args);

            // Auto-detect repository if not specified
            if (parsed.repo == null) {
                String detectedRepo = getCurrentRepository();
                if (detectedRepo != null) {
                    parsed.repo = detectedRepo;
                    System.err.println("Detected current repository: " + detectedRepo);
                } else {
                    System.err.println("Warning: Not in a git repository or no GitHub remote found.");
                    System.err.println("Activity will be searched across all repositories.");
                    System.err.println("Use --repo owner/repo to specify a repository.");
                }
            }

            // Auto-detect user if not specified
            if (parsed.user == null && parsed.team == null) {
                System.err.println("Detecting current GitHub user...");
                parsed.user = getCurrentUser();
            }

            // Team mode
            if (parsed.team != null) {
                System.err.println("Generating team report for " + parsed.team.size() + " members...");
                StringBuilder teamReports = new StringBuilder();

                for (String member : parsed.team) {
                    System.err.println("Generating report for " + member + "...");

                    // Collect activity
                    List<String> activityArgs = new ArrayList<>();
                    activityArgs.add(member);
                    activityArgs.add(String.valueOf(parsed.days));
                    if (parsed.repo != null) {
                        activityArgs.add(parsed.repo);
                    }
                    String activityJson = runScript("CollectActivity.java", activityArgs);

                    // Analyze diffs
                    List<String> diffArgs = new ArrayList<>();
                    diffArgs.add(activityJson);
                    String diffSummary = runScript("AnalyzeDiffs.java", diffArgs);

                    // Generate individual report (capture stdout differently for team mode)
                    // For simplicity, we'll generate reports and consolidate them
                    teamReports.append("## ").append(member).append("\n\n");
                    teamReports.append("[Individual report would go here]\n\n");
                }

                // Generate team report
                List<String> teamArgs = new ArrayList<>();
                teamArgs.add(teamReports.toString());
                runScript("TeamAggregator.java", teamArgs);

                System.exit(0);
            }

            // Single user mode
            System.err.println("Collecting activity for " + parsed.user + "...");

            // Collect activity
            List<String> activityArgs = new ArrayList<>();
            activityArgs.add(parsed.user);
            activityArgs.add(String.valueOf(parsed.days));
            if (parsed.repo != null) {
                activityArgs.add(parsed.repo);
            }
            String activityJson = runScript("CollectActivity.java", activityArgs);

            System.err.println("Analyzing file changes...");

            // Analyze diffs
            List<String> diffArgs = new ArrayList<>();
            diffArgs.add(activityJson);
            String diffSummary = runScript("AnalyzeDiffs.java", diffArgs);

            System.err.println("Generating standup report...");

            // Call claude directly (like tac-1 pattern) instead of through GenerateReport.java subprocess
            Path promptPath = Paths.get("prompts/standup.prompt.md");
            String promptTemplate = Files.readString(promptPath);

            // Parse and format activities
            JsonObject activity = com.google.gson.JsonParser.parseString(activityJson).getAsJsonObject();
            String formattedActivities = formatActivities(activity);

            // Inject data into template
            String fullPrompt = promptTemplate
                    .replace("{{activities}}", formattedActivities)
                    .replace("{{diffs}}", diffSummary);

            // Call claude -p with the full prompt (tac-1 pattern)
            ProcessBuilder claudeBuilder = new ProcessBuilder("claude", "-p", fullPrompt);
            claudeBuilder.inheritIO();
            Process claudeProcess = claudeBuilder.start();
            int claudeExitCode = claudeProcess.waitFor();

            if (claudeExitCode != 0) {
                System.err.println("Claude invocation failed with exit code: " + claudeExitCode);
                System.exit(claudeExitCode);
            }

            // Note: Export formatting is handled by GenerateReport for now
            // Full export integration would require capturing claude output

        } catch (NumberFormatException e) {
            System.err.println("Error: Invalid number format");
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
