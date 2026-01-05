///usr/bin/env jbang "$0" "$@" ; exit $?

//DEPS com.google.code.gson:gson:2.10.1

import com.google.gson.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * LocalChangesDetector - Detect uncommitted and unpushed changes in a git directory
 *
 * Usage: jbang LocalChangesDetector.java <directoryId> <path> <branch> [--debug]
 */
public class LocalChangesDetector {

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static boolean DEBUG = false;

    private static void debug(String message) {
        if (DEBUG) {
            System.err.println("[DEBUG] LocalChangesDetector: " + message);
        }
    }

    static class LocalChanges {
        String directoryId;
        String path;
        String branch;
        UncommittedChanges uncommitted = new UncommittedChanges();
        UnpushedCommits unpushed = new UnpushedCommits();
    }

    static class UncommittedChanges {
        boolean hasChanges = false;
        int filesChanged = 0;
        List<String> staged = new ArrayList<>();
        List<String> unstaged = new ArrayList<>();
        String summary = "";
    }

    static class UnpushedCommits {
        boolean hasCommits = false;
        int count = 0;
        List<String> commits = new ArrayList<>();
    }

    public static void main(String... args) {
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

        if (positionalArgs.size() < 3) {
            System.err.println("Usage: LocalChangesDetector <directoryId> <path> <branch> [--debug]");
            System.exit(1);
        }

        String directoryId = positionalArgs.get(0);
        String path = expandTilde(positionalArgs.get(1));
        String branch = positionalArgs.get(2);

        debug("directoryId=" + directoryId + ", path=" + path + ", branch=" + branch);

        try {
            LocalChanges changes = detectChanges(directoryId, path, branch);
            System.out.println(gson.toJson(changes));
        } catch (Exception e) {
            System.err.println("Error detecting changes: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static String expandTilde(String path) {
        if (path.startsWith("~/")) {
            return System.getProperty("user.home") + path.substring(1);
        }
        return path;
    }

    private static LocalChanges detectChanges(String directoryId, String path, String branch) throws Exception {
        LocalChanges changes = new LocalChanges();
        changes.directoryId = directoryId;
        changes.path = path;
        changes.branch = branch;

        // Verify directory exists
        if (!Files.exists(Paths.get(path))) {
            debug("Directory not found: " + path);
            System.err.println("⚠️  Directory not found: " + path);
            return changes;
        }

        debug("Directory exists, detecting changes");

        // Detect uncommitted changes
        detectUncommittedChanges(path, changes.uncommitted);

        // Detect unpushed commits
        detectUnpushedCommits(path, branch, changes.unpushed);

        debug("Detection complete: " + changes.uncommitted.filesChanged + " files changed, " +
              changes.unpushed.count + " unpushed commits");

        return changes;
    }

    private static void detectUncommittedChanges(String path, UncommittedChanges uncommitted) throws Exception {
        // Detect unstaged changes
        debug("Running: git -C " + path + " diff --name-only");
        ProcessBuilder pb = new ProcessBuilder("git", "-C", path, "diff", "--name-only");
        Process process = pb.start();
        List<String> unstaged = readLines(process);
        int exitCode = process.waitFor();
        debug("Unstaged files exit code: " + exitCode + ", count: " + unstaged.size());

        if (exitCode == 0 && !unstaged.isEmpty()) {
            uncommitted.unstaged.addAll(unstaged);
            uncommitted.hasChanges = true;
        }

        // Detect staged changes
        debug("Running: git -C " + path + " diff --cached --name-only");
        pb = new ProcessBuilder("git", "-C", path, "diff", "--cached", "--name-only");
        process = pb.start();
        List<String> staged = readLines(process);
        exitCode = process.waitFor();
        debug("Staged files exit code: " + exitCode + ", count: " + staged.size());

        if (exitCode == 0 && !staged.isEmpty()) {
            uncommitted.staged.addAll(staged);
            uncommitted.hasChanges = true;
        }

        // Calculate total files changed
        uncommitted.filesChanged = uncommitted.staged.size() + uncommitted.unstaged.size();

        // Generate summary if there are changes
        if (uncommitted.hasChanges) {
            debug("Generating diff summary");
            uncommitted.summary = generateSummary(path);
        }
    }

    private static String generateSummary(String path) throws Exception {
        // Get stat summary from git diff
        debug("Running: git -C " + path + " diff --stat");
        ProcessBuilder pb = new ProcessBuilder("git", "-C", path, "diff", "--stat");
        Process process = pb.start();
        List<String> lines = readLines(process);
        process.waitFor();

        // Get cached stat summary
        debug("Running: git -C " + path + " diff --cached --stat");
        pb = new ProcessBuilder("git", "-C", path, "diff", "--cached", "--stat");
        process = pb.start();
        List<String> cachedLines = readLines(process);
        process.waitFor();

        // Combine both summaries
        lines.addAll(cachedLines);

        // Extract summary line (last line usually contains the summary)
        for (int i = lines.size() - 1; i >= 0; i--) {
            String line = lines.get(i).trim();
            if (line.contains("changed") || line.contains("insertion") || line.contains("deletion")) {
                debug("Summary: " + line);
                return line;
            }
        }

        return "";
    }

    private static void detectUnpushedCommits(String path, String branch, UnpushedCommits unpushed) throws Exception {
        // Check if remote branch exists
        debug("Running: git -C " + path + " rev-parse --verify origin/" + branch);
        ProcessBuilder pb = new ProcessBuilder("git", "-C", path, "rev-parse", "--verify", "origin/" + branch);
        Process process = pb.start();
        int exitCode = process.waitFor();
        debug("Remote branch check exit code: " + exitCode);

        if (exitCode != 0) {
            // Remote branch doesn't exist (local-only branch)
            debug("No remote branch 'origin/" + branch + "'");
            System.err.println("⚠️  No remote branch 'origin/" + branch + "' (local-only branch)");
            return;
        }

        // Get unpushed commits
        debug("Running: git -C " + path + " log origin/" + branch + "..HEAD --oneline");
        pb = new ProcessBuilder("git", "-C", path, "log", "origin/" + branch + "..HEAD", "--oneline", "--format=%h %s");
        process = pb.start();
        List<String> commits = readLines(process);
        exitCode = process.waitFor();
        debug("Unpushed commits exit code: " + exitCode + ", count: " + commits.size());

        if (exitCode == 0 && !commits.isEmpty()) {
            unpushed.hasCommits = true;
            unpushed.count = commits.size();
            unpushed.commits.addAll(commits);
        }
    }

    private static List<String> readLines(Process process) throws IOException {
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        }
        return lines;
    }
}
