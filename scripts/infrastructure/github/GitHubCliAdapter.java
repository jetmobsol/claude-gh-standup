import com.google.gson.*;
import java.io.*;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Infrastructure adapter that implements ActivityPort and DiffPort
 * using the GitHub CLI (gh).
 *
 * This is the only place where gh CLI calls and JSON parsing happen.
 */
public class GitHubCliAdapter implements ActivityPort, DiffPort {

    private static final Gson gson = new Gson();

    // --- ActivityPort implementation ---

    @Override
    public List<Commit> fetchCommits(String username, DateRange range, Repository repo) {
        try {
            String repoArg = repo != null ? " --repo=" + repo : "";
            String[] cmd = {
                "gh", "search", "commits",
                "--author=" + username,
                "--author-date=>" + range.startIso(),
                "--json=sha,commit",
                "--limit=100"
            };

            String json = executeCommand(cmd);
            if (json == null || json.isBlank()) {
                return List.of();
            }

            JsonArray items = gson.fromJson(json, JsonArray.class);
            List<Commit> commits = new ArrayList<>();

            for (JsonElement element : items) {
                JsonObject obj = element.getAsJsonObject();
                String sha = getStringOrNull(obj, "sha");
                JsonObject commitObj = obj.has("commit") ? obj.getAsJsonObject("commit") : null;
                String message = commitObj != null ? getStringOrNull(commitObj, "message") : "";

                if (sha != null) {
                    commits.add(new Commit(
                        sha,
                        message != null ? message.split("\n")[0] : "", // First line only
                        username,
                        null, // Date parsing would need more work
                        null, // URL
                        repo != null ? repo.toString() : null
                    ));
                }
            }

            return commits;
        } catch (Exception e) {
            // Graceful degradation - commit search often fails
            System.err.println("Warning: Commit search failed - " + e.getMessage());
            return List.of();
        }
    }

    @Override
    public List<PullRequest> fetchPullRequests(String username, DateRange range, Repository repo) {
        try {
            List<String> cmdList = new ArrayList<>(Arrays.asList(
                "gh", "search", "prs",
                "--author=" + username,
                "--created=>" + range.startIso(),
                "--json=number,title,state,url,additions,deletions,repository",
                "--limit=100"
            ));
            if (repo != null) {
                cmdList.add("--repo=" + repo);
            }

            String json = executeCommand(cmdList.toArray(new String[0]));
            if (json == null || json.isBlank()) {
                return List.of();
            }

            JsonArray items = gson.fromJson(json, JsonArray.class);
            List<PullRequest> prs = new ArrayList<>();

            for (JsonElement element : items) {
                JsonObject obj = element.getAsJsonObject();
                int number = obj.has("number") ? obj.get("number").getAsInt() : 0;
                String title = getStringOrNull(obj, "title");
                String state = getStringOrNull(obj, "state");
                String url = getStringOrNull(obj, "url");
                int additions = obj.has("additions") ? obj.get("additions").getAsInt() : 0;
                int deletions = obj.has("deletions") ? obj.get("deletions").getAsInt() : 0;

                JsonObject repoObj = obj.has("repository") ? obj.getAsJsonObject("repository") : null;
                String repoName = repoObj != null ? getStringOrNull(repoObj, "nameWithOwner") : null;

                if (number > 0 && title != null && state != null) {
                    prs.add(new PullRequest(number, title, state, url, repoName, additions, deletions));
                }
            }

            return prs;
        } catch (Exception e) {
            System.err.println("Warning: PR search failed - " + e.getMessage());
            return List.of();
        }
    }

    @Override
    public List<Issue> fetchIssues(String username, DateRange range, Repository repo) {
        try {
            List<String> cmdList = new ArrayList<>(Arrays.asList(
                "gh", "search", "issues",
                "--author=" + username,
                "--created=>" + range.startIso(),
                "--json=number,title,state,url,labels,repository",
                "--limit=100"
            ));
            if (repo != null) {
                cmdList.add("--repo=" + repo);
            }

            String json = executeCommand(cmdList.toArray(new String[0]));
            if (json == null || json.isBlank()) {
                return List.of();
            }

            JsonArray items = gson.fromJson(json, JsonArray.class);
            List<Issue> issues = new ArrayList<>();

            for (JsonElement element : items) {
                JsonObject obj = element.getAsJsonObject();
                int number = obj.has("number") ? obj.get("number").getAsInt() : 0;
                String title = getStringOrNull(obj, "title");
                String state = getStringOrNull(obj, "state");
                String url = getStringOrNull(obj, "url");

                List<String> labels = new ArrayList<>();
                if (obj.has("labels") && obj.get("labels").isJsonArray()) {
                    for (JsonElement labelEl : obj.getAsJsonArray("labels")) {
                        if (labelEl.isJsonObject()) {
                            String name = getStringOrNull(labelEl.getAsJsonObject(), "name");
                            if (name != null) labels.add(name);
                        }
                    }
                }

                JsonObject repoObj = obj.has("repository") ? obj.getAsJsonObject("repository") : null;
                String repoName = repoObj != null ? getStringOrNull(repoObj, "nameWithOwner") : null;

                if (number > 0 && title != null && state != null) {
                    issues.add(new Issue(number, title, state, url, repoName, labels));
                }
            }

            return issues;
        } catch (Exception e) {
            System.err.println("Warning: Issue search failed - " + e.getMessage());
            return List.of();
        }
    }

    @Override
    public List<Review> fetchReviews(String username, DateRange range, Repository repo) {
        // Note: gh search doesn't directly support reviews, so we use a different approach
        // This is a simplified implementation that gets reviews from PRs the user has reviewed
        try {
            List<String> cmdList = new ArrayList<>(Arrays.asList(
                "gh", "search", "prs",
                "--reviewed-by=" + username,
                "--created=>" + range.startIso(),
                "--json=number,repository",
                "--limit=50"
            ));
            if (repo != null) {
                cmdList.add("--repo=" + repo);
            }

            String json = executeCommand(cmdList.toArray(new String[0]));
            if (json == null || json.isBlank()) {
                return List.of();
            }

            JsonArray items = gson.fromJson(json, JsonArray.class);
            List<Review> reviews = new ArrayList<>();

            // For each PR, we create a simplified review entry
            for (JsonElement element : items) {
                JsonObject obj = element.getAsJsonObject();
                int prNumber = obj.has("number") ? obj.get("number").getAsInt() : 0;

                JsonObject repoObj = obj.has("repository") ? obj.getAsJsonObject("repository") : null;
                String repoName = repoObj != null ? getStringOrNull(repoObj, "nameWithOwner") : null;

                if (prNumber > 0) {
                    // Simplified: we don't know the exact review state without more API calls
                    reviews.add(new Review(prNumber, "REVIEWED", null, null, repoName));
                }
            }

            return reviews;
        } catch (Exception e) {
            System.err.println("Warning: Review search failed - " + e.getMessage());
            return List.of();
        }
    }

    // --- DiffPort implementation ---

    @Override
    public DiffSummary fetchPRDiff(Repository repo, int prNumber) {
        try {
            String[] cmd = {
                "gh", "pr", "view", String.valueOf(prNumber),
                "--repo=" + repo,
                "--json=additions,deletions,changedFiles"
            };

            String json = executeCommand(cmd);
            if (json == null || json.isBlank()) {
                return new DiffSummary(0, 0, 0);
            }

            JsonObject obj = gson.fromJson(json, JsonObject.class);
            int additions = obj.has("additions") ? obj.get("additions").getAsInt() : 0;
            int deletions = obj.has("deletions") ? obj.get("deletions").getAsInt() : 0;
            int filesChanged = obj.has("changedFiles") ? obj.get("changedFiles").getAsInt() : 0;

            return new DiffSummary(filesChanged, additions, deletions);
        } catch (Exception e) {
            System.err.println("Warning: PR diff fetch failed for #" + prNumber + " - " + e.getMessage());
            return new DiffSummary(0, 0, 0);
        }
    }

    // --- Helper methods ---

    private String executeCommand(String[] cmd) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String output = reader.lines().collect(Collectors.joining("\n"));
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                throw new RuntimeException("Command failed with exit code " + exitCode + ": " + output);
            }

            return output;
        }
    }

    private String getStringOrNull(JsonObject obj, String key) {
        if (obj.has(key) && !obj.get(key).isJsonNull()) {
            return obj.get(key).getAsString();
        }
        return null;
    }
}
