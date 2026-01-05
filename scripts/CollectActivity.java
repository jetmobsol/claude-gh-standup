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
 * Usage: jbang CollectActivity.java <username> <days> [repo] [--debug]
 */
public class CollectActivity {

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static boolean DEBUG = false;

    private static void debug(String message) {
        if (DEBUG) {
            System.err.println("[DEBUG] CollectActivity: " + message);
        }
    }

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

        debug("Executing: " + String.join(" ", command));
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
            debug("Commit search failed with exit code: " + exitCode);
            System.err.println("Warning: Commit search failed (this is common due to GitHub restrictions)");
            return "[]";
        }

        String result = output.toString().trim();
        JsonArray commits = JsonParser.parseString(result).getAsJsonArray();
        debug("Found " + commits.size() + " commits");
        return result;
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

        debug("Executing: " + String.join(" ", command));
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
            debug("PR search failed with exit code: " + exitCode);
            System.err.println("Warning: PR search failed");
            return "[]";
        }

        String result = output.toString().trim();
        JsonArray prs = JsonParser.parseString(result).getAsJsonArray();
        debug("Found " + prs.size() + " pull requests");
        return result;
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

        debug("Executing: " + String.join(" ", command));
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
            debug("Issue search failed with exit code: " + exitCode);
            System.err.println("Warning: Issue search failed");
            return "[]";
        }

        String result = output.toString().trim();
        JsonArray issues = JsonParser.parseString(result).getAsJsonArray();
        debug("Found " + issues.size() + " issues");
        return result;
    }

    public static JsonObject collectAllActivity(String username, int days, String repo) throws IOException, InterruptedException {
        JsonObject result = new JsonObject();

        debug("Collecting commits for " + username + " (last " + days + " days)");
        // Collect commits
        String commitsJson = getUserCommits(username, days);
        JsonArray commits = JsonParser.parseString(commitsJson).getAsJsonArray();
        result.add("commits", commits);

        debug("Collecting PRs for " + username);
        // Collect PRs
        String prsJson = getUserPRs(username, days, repo);
        JsonArray prs = JsonParser.parseString(prsJson).getAsJsonArray();
        result.add("pull_requests", prs);

        debug("Collecting issues for " + username);
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

        debug("Total activity: " + commits.size() + " commits, " + prs.size() + " PRs, " + issues.size() + " issues");
        return result;
    }

    public static void main(String... args) {
        try {
            // Parse --debug flag from any position
            List<String> positionalArgs = new ArrayList<>();
            for (String arg : args) {
                if (arg.equals("--debug") || arg.equals("-D")) {
                    DEBUG = true;
                } else {
                    positionalArgs.add(arg);
                }
            }

            debug("Debug mode enabled");
            debug("Positional args: " + positionalArgs);

            if (positionalArgs.size() < 2) {
                System.err.println("Usage: jbang CollectActivity.java <username> <days> [repo] [--debug]");
                System.exit(1);
            }

            String username = positionalArgs.get(0);
            int days = Integer.parseInt(positionalArgs.get(1));
            String repo = positionalArgs.size() > 2 ? positionalArgs.get(2) : null;

            debug("username=" + username + ", days=" + days + ", repo=" + repo);

            // Auto-detect repository if not specified
            if (repo == null) {
                debug("No repo specified, attempting auto-detection");
                String detectedRepo = getCurrentRepository();
                if (detectedRepo != null) {
                    repo = detectedRepo;
                    debug("Repository auto-detected: " + detectedRepo);
                    System.err.println("Detected current repository: " + detectedRepo);
                } else {
                    debug("Repository auto-detection failed");
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
