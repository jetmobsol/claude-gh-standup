///usr/bin/env jbang "$0" "$@" ; exit $?

//DEPS com.google.code.gson:gson:2.10.1

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AnalyzeDiffs - Analyzes file diffs for PRs and commits
 *
 * Usage: jbang AnalyzeDiffs.java <activity-json> [--debug]
 */
public class AnalyzeDiffs {

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static boolean DEBUG = false;

    private static void debug(String message) {
        if (DEBUG) {
            System.err.println("[DEBUG] AnalyzeDiffs: " + message);
        }
    }

    public static class FileStat {
        String file;
        int additions;
        int deletions;

        public FileStat(String file) {
            this.file = file;
            this.additions = 0;
            this.deletions = 0;
        }
    }

    public static class DiffSummary {
        int filesChanged;
        int totalAdditions;
        int totalDeletions;
        List<FileStat> files;

        public DiffSummary() {
            this.filesChanged = 0;
            this.totalAdditions = 0;
            this.totalDeletions = 0;
            this.files = new ArrayList<>();
        }
    }

    public static String analyzePRDiff(String repo, int prNumber) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();
        command.add("gh");
        command.add("pr");
        command.add("diff");
        command.add(String.valueOf(prNumber));
        command.add("-R");
        command.add(repo);

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
            debug("PR diff request failed with exit code: " + exitCode);
            return null; // PR diff unavailable
        }

        debug("PR #" + prNumber + " diff received, length: " + output.length() + " chars");
        return output.toString();
    }

    public static DiffSummary parseDiff(String diffContent) {
        DiffSummary summary = new DiffSummary();
        if (diffContent == null || diffContent.isEmpty()) {
            return summary;
        }

        Map<String, FileStat> fileStats = new HashMap<>();
        String currentFile = null;

        for (String line : diffContent.split("\n")) {
            if (line.startsWith("diff --git")) {
                // Extract file path: diff --git a/path/to/file.java b/path/to/file.java
                String[] parts = line.split(" ");
                if (parts.length >= 4) {
                    currentFile = parts[3].substring(2); // Remove "b/" prefix
                    fileStats.put(currentFile, new FileStat(currentFile));
                }
            } else if (currentFile != null) {
                if (line.startsWith("+") && !line.startsWith("+++")) {
                    fileStats.get(currentFile).additions++;
                } else if (line.startsWith("-") && !line.startsWith("---")) {
                    fileStats.get(currentFile).deletions++;
                }
            }
        }

        for (FileStat stat : fileStats.values()) {
            summary.files.add(stat);
            summary.totalAdditions += stat.additions;
            summary.totalDeletions += stat.deletions;
        }
        summary.filesChanged = fileStats.size();

        debug("Parsed diff: " + summary.filesChanged + " files, +" + summary.totalAdditions + "/-" + summary.totalDeletions);
        return summary;
    }

    public static DiffSummary analyzePRDiffs(JsonArray prs) {
        DiffSummary totalSummary = new DiffSummary();
        debug("Analyzing diffs for " + prs.size() + " PRs");

        for (JsonElement prElement : prs) {
            JsonObject pr = prElement.getAsJsonObject();
            int prNumber = pr.get("number").getAsInt();

            // Extract repository name from repository object
            JsonObject repoObj = pr.getAsJsonObject("repository");
            String repoName = repoObj.get("nameWithOwner").getAsString();

            debug("Analyzing PR #" + prNumber + " in " + repoName);
            try {
                String diffContent = analyzePRDiff(repoName, prNumber);
                if (diffContent != null) {
                    DiffSummary prSummary = parseDiff(diffContent);
                    totalSummary.filesChanged += prSummary.filesChanged;
                    totalSummary.totalAdditions += prSummary.totalAdditions;
                    totalSummary.totalDeletions += prSummary.totalDeletions;
                    totalSummary.files.addAll(prSummary.files);
                }
            } catch (Exception e) {
                debug("Error analyzing PR #" + prNumber + ": " + e.getMessage());
                System.err.println("Warning: Could not analyze diff for PR #" + prNumber + ": " + e.getMessage());
            }
        }

        debug("Total diff summary: " + totalSummary.filesChanged + " files, +" + totalSummary.totalAdditions + "/-" + totalSummary.totalDeletions);
        return totalSummary;
    }

    public static String formatDiffSummary(DiffSummary summary) {
        if (summary.filesChanged == 0) {
            return "No file changes analyzed.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Files changed: ").append(summary.filesChanged).append("\n");
        sb.append("Lines added: ").append(summary.totalAdditions).append("\n");
        sb.append("Lines deleted: ").append(summary.totalDeletions).append("\n");
        sb.append("\nModified files:\n");

        for (FileStat stat : summary.files) {
            sb.append("- ").append(stat.file)
                    .append(" (+").append(stat.additions)
                    .append(", -").append(stat.deletions)
                    .append(")\n");
        }

        return sb.toString();
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

            if (positionalArgs.size() < 1) {
                System.err.println("Usage: jbang AnalyzeDiffs.java <activity-json> [--debug]");
                System.exit(1);
            }

            String activityJson = positionalArgs.get(0);
            debug("Activity JSON length: " + activityJson.length() + " chars");
            JsonObject activity = JsonParser.parseString(activityJson).getAsJsonObject();

            JsonArray prs = activity.getAsJsonArray("pull_requests");
            debug("Found " + prs.size() + " PRs to analyze");
            DiffSummary summary = analyzePRDiffs(prs);

            System.out.println(formatDiffSummary(summary));

        } catch (Exception e) {
            System.err.println("Error analyzing diffs: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
