/**
 * Domain entity representing a GitHub Pull Request.
 *
 * This is a pure domain object with no external dependencies.
 * JSON parsing happens in the infrastructure layer (adapters).
 *
 * @param number     The PR number (required, positive)
 * @param title      The PR title (required)
 * @param state      The PR state: "open", "closed", "merged" (required)
 * @param url        The URL to view the PR on GitHub (optional)
 * @param repository The repository in "owner/repo" format (optional)
 * @param additions  Number of lines added (non-negative)
 * @param deletions  Number of lines deleted (non-negative)
 */
public record PullRequest(
    int number,
    String title,
    String state,
    String url,
    String repository,
    int additions,
    int deletions
) {
    /**
     * Compact constructor for validation.
     */
    public PullRequest {
        if (number <= 0) {
            throw new IllegalArgumentException("PR number must be positive");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Title is required");
        }
        if (state == null || state.isBlank()) {
            throw new IllegalArgumentException("State is required");
        }
        if (additions < 0) {
            throw new IllegalArgumentException("Additions cannot be negative");
        }
        if (deletions < 0) {
            throw new IllegalArgumentException("Deletions cannot be negative");
        }
    }

    /**
     * Total lines changed (additions + deletions).
     */
    public int totalChanges() {
        return additions + deletions;
    }

    /**
     * Check if PR is still open.
     */
    public boolean isOpen() {
        return "open".equalsIgnoreCase(state);
    }

    /**
     * Check if PR was merged.
     */
    public boolean isMerged() {
        return "merged".equalsIgnoreCase(state);
    }
}
