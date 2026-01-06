import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Port interface for local git operations.
 *
 * This is a pure interface with no implementation - adapters
 * in the infrastructure layer provide concrete implementations.
 */
public interface GitPort {

    /**
     * Get the current branch name.
     *
     * @param repoPath Path to the git repository
     * @return Current branch name
     */
    String getCurrentBranch(Path repoPath);

    /**
     * Get list of staged files.
     *
     * @param repoPath Path to the git repository
     * @return List of staged file paths
     */
    List<String> getStagedFiles(Path repoPath);

    /**
     * Get list of unstaged modified files.
     *
     * @param repoPath Path to the git repository
     * @return List of unstaged file paths
     */
    List<String> getUnstagedFiles(Path repoPath);

    /**
     * Get list of unpushed commits on a branch.
     *
     * @param repoPath Path to the git repository
     * @param branch   Branch name to check
     * @return List of unpushed commit messages
     */
    List<String> getUnpushedCommits(Path repoPath, String branch);

    /**
     * Detect repository from git remote.
     *
     * @param repoPath Path to the git repository
     * @return Repository if detected, empty if not a git repo or no remote
     */
    Optional<Repository> detectRepository(Path repoPath);
}
