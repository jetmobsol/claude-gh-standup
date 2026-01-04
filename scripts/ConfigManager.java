///usr/bin/env jbang "$0" "$@" ; exit $?

//DEPS com.google.code.gson:gson:2.10.1

import com.google.gson.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * ConfigManager - Manage multi-directory configuration for claude-gh-standup
 *
 * Usage: jbang ConfigManager.java <command> [args]
 */
public class ConfigManager {

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final String DEFAULT_CONFIG_PATH = "~/.claude-gh-standup/config.json";

    static class Config {
        String version = "1.0";
        List<Directory> directories = new ArrayList<>();
        ReportSettings reportSettings = new ReportSettings();
    }

    static class Directory {
        String id;
        String path;
        String branch;
        boolean enabled = true;
        String remoteUrl;
        String repoName;
    }

    static class ReportSettings {
        int defaultDays = 1;
        boolean autoSaveReports = true;
        String reportDirectory = "~/.claude-gh-standup/reports";
    }

    public static void main(String... args) {
        if (args.length == 0) {
            printUsage();
            System.exit(1);
        }

        String command = args[0];

        try {
            switch (command) {
                case "init":
                    initConfig();
                    break;
                case "load":
                    loadAndPrintConfig();
                    break;
                case "add":
                    if (args.length < 3) {
                        System.err.println("Usage: ConfigManager add <path> <id>");
                        System.exit(1);
                    }
                    addDirectory(args[1], args[2]);
                    break;
                case "remove":
                    if (args.length < 2) {
                        System.err.println("Usage: ConfigManager remove <id>");
                        System.exit(1);
                    }
                    removeDirectory(args[1]);
                    break;
                case "list":
                    listDirectories();
                    break;
                default:
                    System.err.println("Unknown command: " + command);
                    printUsage();
                    System.exit(1);
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void printUsage() {
        System.out.println("ConfigManager - Manage claude-gh-standup configuration");
        System.out.println();
        System.out.println("Usage: ConfigManager <command> [args]");
        System.out.println();
        System.out.println("Commands:");
        System.out.println("  init              Initialize empty config file");
        System.out.println("  load              Load and print current config");
        System.out.println("  add <path> <id>   Add directory to config");
        System.out.println("  remove <id>       Remove directory from config");
        System.out.println("  list              List all directories");
    }

    public static String expandTilde(String path) {
        if (path.startsWith("~/")) {
            return System.getProperty("user.home") + path.substring(1);
        }
        return path;
    }

    private static String getConfigPath() {
        return expandTilde(DEFAULT_CONFIG_PATH);
    }

    public static void initConfig() throws IOException {
        String configPath = getConfigPath();
        Path path = Paths.get(configPath);

        if (Files.exists(path)) {
            System.err.println("Config file already exists at: " + configPath);
            System.err.println("Use 'load' to view or manually delete to reinitialize");
            System.exit(1);
        }

        // Create parent directory if needed
        Files.createDirectories(path.getParent());

        Config config = new Config();
        saveConfig(config);

        System.out.println("✓ Initialized config at: " + configPath);
    }

    public static Config loadConfig() throws IOException {
        String configPath = getConfigPath();
        Path path = Paths.get(configPath);

        if (!Files.exists(path)) {
            return null;  // No config file
        }

        String json = Files.readString(path);

        try {
            return gson.fromJson(json, Config.class);
        } catch (JsonSyntaxException e) {
            System.err.println("❌ Invalid JSON in config file: " + configPath);
            System.err.println("   " + e.getMessage());
            throw e;
        }
    }

    private static void loadAndPrintConfig() throws IOException {
        Config config = loadConfig();
        if (config == null) {
            System.err.println("No config file found at: " + getConfigPath());
            System.err.println("Run 'init' to create one");
            System.exit(1);
        }

        System.out.println(gson.toJson(config));
    }

    public static void saveConfig(Config config) throws IOException {
        String configPath = getConfigPath();
        Path path = Paths.get(configPath);

        // Create parent directory if needed
        Files.createDirectories(path.getParent());

        // Atomic write: write to temp file, then rename
        Path tempPath = Paths.get(configPath + ".tmp");
        String json = gson.toJson(config);
        Files.writeString(tempPath, json);
        Files.move(tempPath, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    public static void addDirectory(String dirPath, String id) throws IOException {
        Config config = loadConfig();
        if (config == null) {
            config = new Config();
        }

        // Check for duplicate ID
        for (Directory dir : config.directories) {
            if (dir.id.equals(id)) {
                System.err.println("❌ Error: Directory with ID '" + id + "' already exists");
                System.exit(1);
            }
        }

        // Expand tilde in path
        String expandedPath = expandTilde(dirPath);

        // Verify path exists
        if (!Files.exists(Paths.get(expandedPath))) {
            System.err.println("❌ Error: Directory not found: " + expandedPath);
            System.exit(1);
        }

        // Detect git info
        GitInfo gitInfo = detectGitInfo(expandedPath);

        Directory directory = new Directory();
        directory.id = id;
        directory.path = dirPath;  // Store original path with tilde
        directory.branch = gitInfo.branch;
        directory.enabled = true;
        directory.remoteUrl = gitInfo.remoteUrl;
        directory.repoName = gitInfo.repoName;

        config.directories.add(directory);
        saveConfig(config);

        System.out.println("✓ Added directory:");
        System.out.println("  ID: " + id);
        System.out.println("  Path: " + dirPath);
        System.out.println("  Branch: " + gitInfo.branch);
        System.out.println("  Repository: " + gitInfo.repoName);
    }

    public static void removeDirectory(String id) throws IOException {
        Config config = loadConfig();
        if (config == null) {
            System.err.println("❌ Error: No config file found");
            System.exit(1);
        }

        boolean found = config.directories.removeIf(dir -> dir.id.equals(id));

        if (!found) {
            System.err.println("❌ Error: Directory with ID '" + id + "' not found");
            System.exit(1);
        }

        saveConfig(config);
        System.out.println("✓ Removed directory: " + id);
    }

    public static void listDirectories() throws IOException {
        Config config = loadConfig();
        if (config == null || config.directories.isEmpty()) {
            System.out.println("No directories configured");
            System.out.println("Use 'add <path> <id>' to add directories");
            return;
        }

        System.out.println("Configured directories:");
        System.out.println();
        System.out.printf("%-20s %-30s %-20s %-30s %s%n", "ID", "Path", "Branch", "Repository", "Enabled");
        System.out.println("-".repeat(110));

        for (Directory dir : config.directories) {
            System.out.printf("%-20s %-30s %-20s %-30s %s%n",
                dir.id,
                truncate(dir.path, 30),
                truncate(dir.branch, 20),
                truncate(dir.repoName, 30),
                dir.enabled ? "✓" : "✗"
            );
        }
    }

    private static String truncate(String str, int maxLen) {
        if (str == null) return "";
        if (str.length() <= maxLen) return str;
        return str.substring(0, maxLen - 3) + "...";
    }

    static class GitInfo {
        String branch;
        String remoteUrl;
        String repoName;
    }

    public static GitInfo detectGitInfo(String dirPath) throws IOException {
        GitInfo info = new GitInfo();

        // Detect branch
        ProcessBuilder pb = new ProcessBuilder("git", "-C", dirPath, "rev-parse", "--abbrev-ref", "HEAD");
        Process process = pb.start();
        try {
            process.waitFor();
            if (process.exitValue() != 0) {
                System.err.println("❌ Error: Not a git repository: " + dirPath);
                System.exit(1);
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            info.branch = reader.readLine();
        } catch (InterruptedException e) {
            throw new IOException("Git command interrupted", e);
        }

        // Detect remote URL
        pb = new ProcessBuilder("git", "-C", dirPath, "remote", "get-url", "origin");
        process = pb.start();
        try {
            process.waitFor();
            if (process.exitValue() != 0) {
                System.err.println("❌ Error: No remote 'origin' configured for: " + dirPath);
                System.exit(1);
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            info.remoteUrl = reader.readLine();
        } catch (InterruptedException e) {
            throw new IOException("Git command interrupted", e);
        }

        // Parse repoName from remoteUrl
        info.repoName = parseRepoName(info.remoteUrl);

        return info;
    }

    private static String parseRepoName(String remoteUrl) {
        // Handle SSH format: git@github.com:owner/repo.git
        // Handle HTTPS format: https://github.com/owner/repo.git

        String repoName = remoteUrl;

        // Remove git@ prefix
        if (repoName.startsWith("git@")) {
            repoName = repoName.substring(repoName.indexOf(':') + 1);
        }

        // Remove https:// prefix
        if (repoName.startsWith("https://") || repoName.startsWith("http://")) {
            repoName = repoName.substring(repoName.indexOf("//") + 2);
            repoName = repoName.substring(repoName.indexOf('/') + 1);
        }

        // Remove .git suffix
        if (repoName.endsWith(".git")) {
            repoName = repoName.substring(0, repoName.length() - 4);
        }

        return repoName;
    }
}
