///usr/bin/env jbang "$0" "$@" ; exit $?

//DEPS com.google.code.gson:gson:2.10.1

import com.google.gson.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * ActivityAggregator - Orchestrate multi-directory data collection
 *
 * Usage: jbang ActivityAggregator.java <config-json> <user> <days> [--debug]
 */
public class ActivityAggregator {

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static boolean DEBUG = false;

    private static void debug(String message) {
        if (DEBUG) {
            System.err.println("[DEBUG] ActivityAggregator: " + message);
        }
    }

    static class Directory {
        String id;
        String path;
        String branch;
        boolean enabled;
        String remoteUrl;
        String repoName;
    }

    static class AggregatedActivity {
        JsonObject githubActivity = new JsonObject();  // Single object with commits, PRs, issues arrays (ALL repos)
        JsonArray localChanges = new JsonArray();
        JsonObject metadata = new JsonObject();
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
        debug("Positional args count: " + positionalArgs.size());

        if (positionalArgs.size() < 3) {
            System.err.println("Usage: ActivityAggregator <config-json> <user> <days> [--debug]");
            System.exit(1);
        }

        String configJson = positionalArgs.get(0);
        String user = positionalArgs.get(1);
        int days = Integer.parseInt(positionalArgs.get(2));

        debug("user=" + user + ", days=" + days);
        debug("Config JSON length: " + configJson.length() + " chars");

        try {
            // Parse config
            JsonObject config = gson.fromJson(configJson, JsonObject.class);
            JsonArray dirsArray = config.getAsJsonArray("directories");

            List<Directory> directories = new ArrayList<>();
            for (JsonElement elem : dirsArray) {
                Directory dir = gson.fromJson(elem, Directory.class);
                directories.add(dir);
            }

            debug("Loaded " + directories.size() + " directories from config");

            // Filter enabled directories
            List<Directory> enabledDirs = directories.stream()
                .filter(d -> d.enabled)
                .filter(d -> {
                    String expandedPath = expandTilde(d.path);
                    if (!Files.exists(Paths.get(expandedPath))) {
                        debug("Directory not found: " + d.path);
                        System.err.println("⚠️  Directory not found: " + d.path + " (skipping)");
                        return false;
                    }
                    return true;
                })
                .collect(Collectors.toList());

            debug("Filtered to " + enabledDirs.size() + " enabled directories");

            if (enabledDirs.isEmpty()) {
                System.err.println("❌ No valid directories to process");
                System.exit(1);
            }

            // Aggregate activities
            AggregatedActivity aggregated = aggregateActivities(enabledDirs, user, days);

            // Output JSON
            String outputJson = gson.toJson(aggregated);
            debug("Output JSON length: " + outputJson.length() + " chars");
            System.out.println(outputJson);

        } catch (Exception e) {
            System.err.println("Error aggregating activities: " + e.getMessage());
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

    private static AggregatedActivity aggregateActivities(List<Directory> directories, String user, int days) throws Exception {
        AggregatedActivity aggregated = new AggregatedActivity();

        // Group directories by repoName for metadata (shows which repos have local tracking)
        Map<String, List<Directory>> repoMap = directories.stream()
            .collect(Collectors.groupingBy(d -> d.repoName));

        debug("Grouped into " + repoMap.size() + " unique repositories");
        System.err.println("Processing " + directories.size() + " directories across " + repoMap.size() + " repositories...");

        // Collect local changes in parallel
        debug("Starting local changes collection");
        long startLocal = System.currentTimeMillis();
        JsonArray localChanges = collectLocalChangesParallel(directories);
        long localElapsed = System.currentTimeMillis() - startLocal;
        debug("Local changes collected in " + localElapsed + "ms");
        aggregated.localChanges = localChanges;

        // Collect GitHub activity from ALL user repositories (not filtered by config)
        debug("Starting GitHub activity collection");
        long startGithub = System.currentTimeMillis();
        JsonObject githubActivity = collectGitHubActivityAllRepos(user, days);
        long githubElapsed = System.currentTimeMillis() - startGithub;
        debug("GitHub activity collected in " + githubElapsed + "ms");
        aggregated.githubActivity = githubActivity;

        // Add metadata
        aggregated.metadata.addProperty("user", user);
        aggregated.metadata.addProperty("days", days);
        aggregated.metadata.addProperty("directoryCount", directories.size());
        aggregated.metadata.addProperty("repoCount", repoMap.size());

        // Add configured repos to metadata (shows which repos have local tracking)
        JsonArray configuredRepos = new JsonArray();
        for (String repoName : repoMap.keySet()) {
            configuredRepos.add(repoName);
        }
        aggregated.metadata.add("configuredRepos", configuredRepos);

        debug("Aggregation complete");
        return aggregated;
    }

    private static JsonArray collectLocalChangesParallel(List<Directory> directories) throws Exception {
        int threadCount = Math.min(directories.size(), 4);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Future<JsonObject>> futures = new ArrayList<>();

        debug("Creating thread pool with " + threadCount + " threads");
        System.err.println("Collecting local changes (parallel with " + threadCount + " threads)...");

        for (Directory dir : directories) {
            debug("Submitting LocalChangesDetector task for: " + dir.id);
            Future<JsonObject> future = executor.submit(() -> {
                try {
                    return callLocalChangesDetector(dir);
                } catch (Exception e) {
                    debug("Failed to collect local changes for " + dir.id + ": " + e.getMessage());
                    System.err.println("⚠️  Failed to collect local changes for " + dir.id + ": " + e.getMessage());
                    // Return empty result on error
                    JsonObject empty = new JsonObject();
                    empty.addProperty("directoryId", dir.id);
                    empty.addProperty("path", dir.path);
                    empty.addProperty("branch", dir.branch);
                    empty.add("uncommitted", new JsonObject());
                    empty.add("unpushed", new JsonObject());
                    return empty;
                }
            });
            futures.add(future);
        }

        JsonArray localChanges = new JsonArray();
        for (Future<JsonObject> future : futures) {
            try {
                JsonObject result = future.get(30, TimeUnit.SECONDS);
                localChanges.add(result);
            } catch (TimeoutException e) {
                debug("LocalChangesDetector task timed out");
                System.err.println("⚠️  Local changes detection timed out (skipping)");
            }
        }

        executor.shutdown();
        debug("Collected local changes from " + localChanges.size() + " directories");
        return localChanges;
    }

    private static JsonObject callLocalChangesDetector(Directory dir) throws Exception {
        String installDir = System.getProperty("user.home") + "/.claude-gh-standup";
        String scriptPath = installDir + "/scripts/LocalChangesDetector.java";

        List<String> command = new ArrayList<>();
        command.add("jbang");
        command.add(scriptPath);
        command.add(dir.id);
        command.add(dir.path);
        command.add(dir.branch);
        if (DEBUG) {
            command.add("--debug");
        }

        debug("Calling LocalChangesDetector for " + dir.id + ": " + dir.path);
        ProcessBuilder pb = new ProcessBuilder(command);

        Process process = pb.start();
        StringBuilder output = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }
        }

        // Capture and forward stderr
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.err.println(line);
            }
        }

        int exitCode = process.waitFor();
        debug("LocalChangesDetector for " + dir.id + " exited with code: " + exitCode);
        if (exitCode != 0) {
            throw new RuntimeException("LocalChangesDetector failed for " + dir.id);
        }

        return gson.fromJson(output.toString(), JsonObject.class);
    }

    /**
     * Collects GitHub activity from ALL user repositories (not filtered by configured repos).
     * This ensures multi-directory mode shows the same activity as legacy mode.
     */
    private static JsonObject collectGitHubActivityAllRepos(String user, int days) {
        System.err.println("Collecting GitHub activity from ALL repositories...");

        try {
            // Call CollectActivity with null repo to get ALL user activity
            JsonObject activity = callCollectActivity(user, days, null);

            // Log activity counts
            int commits = activity.has("commits") ? activity.getAsJsonArray("commits").size() : 0;
            int prs = activity.has("pull_requests") ? activity.getAsJsonArray("pull_requests").size() : 0;
            int issues = activity.has("issues") ? activity.getAsJsonArray("issues").size() : 0;
            debug("GitHub activity: " + commits + " commits, " + prs + " PRs, " + issues + " issues");

            return activity;
        } catch (Exception e) {
            debug("Failed to collect GitHub activity: " + e.getMessage());
            System.err.println("⚠️ Failed to collect GitHub activity: " + e.getMessage());
            // Return empty activity structure on error (graceful degradation)
            JsonObject empty = new JsonObject();
            empty.add("commits", new JsonArray());
            empty.add("pull_requests", new JsonArray());
            empty.add("issues", new JsonArray());
            return empty;
        }
    }

    private static JsonObject callCollectActivity(String user, int days, String repo) throws Exception {
        String installDir = System.getProperty("user.home") + "/.claude-gh-standup";
        String scriptPath = installDir + "/scripts/CollectActivity.java";

        List<String> command = new ArrayList<>();
        command.add("jbang");
        command.add(scriptPath);
        command.add(user);
        command.add(String.valueOf(days));
        if (repo != null) {
            command.add(repo);  // Only add repo arg if specified (null means ALL repos)
        }
        if (DEBUG) {
            command.add("--debug");
        }

        debug("Calling CollectActivity: user=" + user + ", days=" + days + ", repo=" + repo);
        ProcessBuilder pb = new ProcessBuilder(command);

        Process process = pb.start();
        StringBuilder output = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }
        }

        // Capture stderr for debugging
        StringBuilder errors = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                errors.append(line).append("\n");
                System.err.println(line);  // Forward to stderr
            }
        }

        int exitCode = process.waitFor();
        debug("CollectActivity exited with code: " + exitCode);
        if (exitCode != 0) {
            System.err.println("CollectActivity stderr: " + errors.toString());
            throw new RuntimeException("CollectActivity failed for " + repo);
        }

        return gson.fromJson(output.toString(), JsonObject.class);
    }
}
