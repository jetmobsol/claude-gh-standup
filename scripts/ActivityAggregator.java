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
 * Usage: jbang ActivityAggregator.java <config-json> <user> <days>
 */
public class ActivityAggregator {

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

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
        if (args.length < 3) {
            System.err.println("Usage: ActivityAggregator <config-json> <user> <days>");
            System.exit(1);
        }

        String configJson = args[0];
        String user = args[1];
        int days = Integer.parseInt(args[2]);

        try {
            // Parse config
            JsonObject config = gson.fromJson(configJson, JsonObject.class);
            JsonArray dirsArray = config.getAsJsonArray("directories");

            List<Directory> directories = new ArrayList<>();
            for (JsonElement elem : dirsArray) {
                Directory dir = gson.fromJson(elem, Directory.class);
                directories.add(dir);
            }

            // Filter enabled directories
            List<Directory> enabledDirs = directories.stream()
                .filter(d -> d.enabled)
                .filter(d -> {
                    String expandedPath = expandTilde(d.path);
                    if (!Files.exists(Paths.get(expandedPath))) {
                        System.err.println("⚠️  Directory not found: " + d.path + " (skipping)");
                        return false;
                    }
                    return true;
                })
                .collect(Collectors.toList());

            if (enabledDirs.isEmpty()) {
                System.err.println("❌ No valid directories to process");
                System.exit(1);
            }

            // Aggregate activities
            AggregatedActivity aggregated = aggregateActivities(enabledDirs, user, days);

            // Output JSON
            System.out.println(gson.toJson(aggregated));

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

        System.err.println("Processing " + directories.size() + " directories across " + repoMap.size() + " repositories...");

        // Collect local changes in parallel
        JsonArray localChanges = collectLocalChangesParallel(directories);
        aggregated.localChanges = localChanges;

        // Collect GitHub activity from ALL user repositories (not filtered by config)
        JsonObject githubActivity = collectGitHubActivityAllRepos(user, days);
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

        return aggregated;
    }

    private static JsonArray collectLocalChangesParallel(List<Directory> directories) throws Exception {
        int threadCount = Math.min(directories.size(), 4);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Future<JsonObject>> futures = new ArrayList<>();

        System.err.println("Collecting local changes (parallel with " + threadCount + " threads)...");

        for (Directory dir : directories) {
            Future<JsonObject> future = executor.submit(() -> {
                try {
                    return callLocalChangesDetector(dir);
                } catch (Exception e) {
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
                System.err.println("⚠️  Local changes detection timed out (skipping)");
            }
        }

        executor.shutdown();
        return localChanges;
    }

    private static JsonObject callLocalChangesDetector(Directory dir) throws Exception {
        String installDir = System.getProperty("user.home") + "/.claude-gh-standup";
        String scriptPath = installDir + "/scripts/LocalChangesDetector.java";

        ProcessBuilder pb = new ProcessBuilder(
            "jbang", scriptPath,
            dir.id, dir.path, dir.branch
        );

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
            return activity;
        } catch (Exception e) {
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
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            System.err.println("CollectActivity stderr: " + errors.toString());
            throw new RuntimeException("CollectActivity failed for " + repo);
        }

        return gson.fromJson(output.toString(), JsonObject.class);
    }
}
