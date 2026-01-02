///usr/bin/env jbang "$0" "$@" ; exit $?

//DEPS com.google.code.gson:gson:2.10.1

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.io.File;

/**
 * CollectActivity - Collects GitHub activity using gh CLI
 *
 * Usage: jbang CollectActivity.java <username> <days> [repo]
 */
public class CollectActivity {

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static String getCurrentRepository() {
        try {
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
        } catch (Exception e) {
            return null; // Any error, return null
        }
    }

    public static String getUserCommits(String username, int days) throws IOException, InterruptedException {
        LocalDate since = LocalDate.now().minusDays(days);
        String sinceStr = since.format(DateTimeFormatter.ISO_DATE);

        List<String> command = new ArrayList<>();
        command.add("gh");
        command.add("search");
        command.add("commits");
        command.add("--author=" + username);
        command.add("--committer-date=>" + sinceStr);
        command.add("--json");
        command.add("sha,commit,repository");
        command.add("--limit");
        command.add("1000");

        ProcessBuilder pb = new ProcessBuilder(command);
        Process process = pb.start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            System.err.println("Warning: Commit search failed (this is common due to GitHub restrictions)");
            return "[]";
        }

        return output.toString().trim();
    }

    public static String getUserPRs(String username, int days, String repo) throws IOException, InterruptedException {
        LocalDate since = LocalDate.now().minusDays(days);
        String sinceStr = since.format(DateTimeFormatter.ISO_DATE);

        List<String> command = new ArrayList<>();
        command.add("gh");
        command.add("search");
        command.add("prs");
        command.add("--author=" + username);
        command.add("--created=>" + sinceStr);
        command.add("--json");
        command.add("number,title,state,repository,url");
        command.add("--limit");
        command.add("1000");

        if (repo != null && !repo.isEmpty()) {
            command.add("-R");
            command.add(repo);
        }

        ProcessBuilder pb = new ProcessBuilder(command);
        Process process = pb.start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            System.err.println("Warning: PR search failed");
            return "[]";
        }

        return output.toString().trim();
    }

    public static String getUserIssues(String username, int days, String repo) throws IOException, InterruptedException {
        LocalDate since = LocalDate.now().minusDays(days);
        String sinceStr = since.format(DateTimeFormatter.ISO_DATE);

        List<String> command = new ArrayList<>();
        command.add("gh");
        command.add("search");
        command.add("issues");
        command.add("--author=" + username);
        command.add("--created=>" + sinceStr);
        command.add("--json");
        command.add("number,title,state,repository,url");
        command.add("--limit");
        command.add("1000");

        if (repo != null && !repo.isEmpty()) {
            command.add("-R");
            command.add(repo);
        }

        ProcessBuilder pb = new ProcessBuilder(command);
        Process process = pb.start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            System.err.println("Warning: Issue search failed");
            return "[]";
        }

        return output.toString().trim();
    }

    public static JsonObject collectAllActivity(String username, int days, String repo) throws IOException, InterruptedException {
        JsonObject result = new JsonObject();

        // Collect commits
        String commitsJson = getUserCommits(username, days);
        JsonArray commits = JsonParser.parseString(commitsJson).getAsJsonArray();
        result.add("commits", commits);

        // Collect PRs
        String prsJson = getUserPRs(username, days, repo);
        JsonArray prs = JsonParser.parseString(prsJson).getAsJsonArray();
        result.add("pull_requests", prs);

        // Collect issues
        String issuesJson = getUserIssues(username, days, repo);
        JsonArray issues = JsonParser.parseString(issuesJson).getAsJsonArray();
        result.add("issues", issues);

        // Add metadata
        result.addProperty("username", username);
        result.addProperty("days", days);
        if (repo != null && !repo.isEmpty()) {
            result.addProperty("repository", repo);
        }

        return result;
    }

    public static void main(String... args) {
        try {
            if (args.length < 2) {
                System.err.println("Usage: jbang CollectActivity.java <username> <days> [repo]");
                System.exit(1);
            }

            String username = args[0];
            int days = Integer.parseInt(args[1]);
            String repo = args.length > 2 ? args[2] : null;

            // Auto-detect repository if not specified
            if (repo == null) {
                String detectedRepo = getCurrentRepository();
                if (detectedRepo != null) {
                    repo = detectedRepo;
                    System.err.println("Detected current repository: " + detectedRepo);
                }
            }

            JsonObject activity = collectAllActivity(username, days, repo);
            System.out.println(gson.toJson(activity));

        } catch (NumberFormatException e) {
            System.err.println("Error: days must be a valid integer");
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Error collecting activity: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
