import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Infrastructure adapter that implements GitPort using the git CLI.
 *
 * This is the only place where git CLI calls happen.
 */
public class GitCliAdapter implements GitPort {

    @Override
    public String getCurrentBranch(Path repoPath) {
        try {
            String output = executeInDir(repoPath, "git", "rev-parse", "--abbrev-ref", "HEAD");
            return output.trim();
        } catch (Exception e) {
            System.err.println("Warning: Failed to detect current branch at " + repoPath + " - " + e.getMessage());
            return "unknown";
        }
    }

    @Override
    public List<String> getStagedFiles(Path repoPath) {
        try {
            String output = executeInDir(repoPath, "git", "diff", "--cached", "--name-only");
            if (output.isBlank()) {
                return List.of();
            }
            return Arrays.stream(output.split("\n"))
                .filter(s -> !s.isBlank())
                .toList();
        } catch (Exception e) {
            System.err.println("Warning: Failed to get staged files at " + repoPath + " - " + e.getMessage());
            return List.of();
        }
    }

    @Override
    public List<String> getUnstagedFiles(Path repoPath) {
        try {
            String output = executeInDir(repoPath, "git", "diff", "--name-only");
            if (output.isBlank()) {
                return List.of();
            }
            return Arrays.stream(output.split("\n"))
                .filter(s -> !s.isBlank())
                .toList();
        } catch (Exception e) {
            System.err.println("Warning: Failed to get unstaged files at " + repoPath + " - " + e.getMessage());
            return List.of();
        }
    }

    @Override
    public List<String> getUnpushedCommits(Path repoPath, String branch) {
        try {
            String output = executeInDir(repoPath,
                "git", "log", "origin/" + branch + ".." + branch, "--oneline");
            if (output.isBlank()) {
                return List.of();
            }
            return Arrays.stream(output.split("\n"))
                .filter(s -> !s.isBlank())
                .toList();
        } catch (Exception e) {
            System.err.println("Warning: Failed to get unpushed commits on " + branch + " at " + repoPath + " - " + e.getMessage());
            return List.of();
        }
    }

    @Override
    public Optional<Repository> detectRepository(Path repoPath) {
        try {
            String remoteUrl = executeInDir(repoPath, "git", "remote", "get-url", "origin");
            return parseGitRemote(remoteUrl.trim());
        } catch (Exception e) {
            System.err.println("Note: Could not detect repository at " + repoPath + " - " + e.getMessage());
            return Optional.empty();
        }
    }

    // --- Helper methods ---

    private String executeInDir(Path dir, String... cmd) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(dir.toFile());
        pb.redirectErrorStream(true);
        Process process = pb.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String output = reader.lines().collect(Collectors.joining("\n"));
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                throw new RuntimeException("Command failed: " + String.join(" ", cmd));
            }

            return output;
        }
    }

    /**
     * Parse git remote URL to extract owner/repo.
     *
     * Supports formats:
     * - https://github.com/owner/repo.git
     * - https://github.com/owner/repo
     * - git@github.com:owner/repo.git
     * - git@github.com:owner/repo
     */
    private Optional<Repository> parseGitRemote(String remoteUrl) {
        if (remoteUrl == null || remoteUrl.isBlank()) {
            return Optional.empty();
        }

        // HTTPS format: https://github.com/owner/repo.git
        if (remoteUrl.startsWith("https://")) {
            String path = remoteUrl
                .replace("https://github.com/", "")
                .replace(".git", "");
            return parseOwnerRepo(path);
        }

        // SSH format: git@github.com:owner/repo.git
        if (remoteUrl.startsWith("git@")) {
            String path = remoteUrl
                .replace("git@github.com:", "")
                .replace(".git", "");
            return parseOwnerRepo(path);
        }

        return Optional.empty();
    }

    private Optional<Repository> parseOwnerRepo(String path) {
        int slashIndex = path.indexOf('/');
        if (slashIndex > 0 && slashIndex < path.length() - 1) {
            String owner = path.substring(0, slashIndex);
            String name = path.substring(slashIndex + 1);
            return Optional.of(new Repository(owner, name));
        }
        return Optional.empty();
    }
}
