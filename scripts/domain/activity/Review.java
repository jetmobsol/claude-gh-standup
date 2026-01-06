/**
 * Domain entity representing a GitHub Pull Request Review.
 *
 * This is a pure domain object with no external dependencies.
 * JSON parsing happens in the infrastructure layer (adapters).
 *
 * @param prNumber   The PR number this review belongs to (required, positive)
 * @param state      The review state: "APPROVED", "CHANGES_REQUESTED", "COMMENTED", "PENDING" (required)
 * @param body       The review comment body (optional, can be null or empty)
 * @param url        The URL to view the review on GitHub (optional)
 * @param repository The repository in "owner/repo" format (optional)
 */
public record Review(
    int prNumber,
    String state,
    String body,
    String url,
    String repository
) {
    /**
     * Compact constructor for validation.
     */
    public Review {
        if (prNumber <= 0) {
            throw new IllegalArgumentException("PR number must be positive");
        }
        if (state == null || state.isBlank()) {
            throw new IllegalArgumentException("State is required");
        }
    }

    /**
     * Check if review approved the PR.
     */
    public boolean isApproved() {
        return "APPROVED".equalsIgnoreCase(state);
    }

    /**
     * Check if review requested changes.
     */
    public boolean requestedChanges() {
        return "CHANGES_REQUESTED".equalsIgnoreCase(state);
    }

    /**
     * Check if review is just a comment.
     */
    public boolean isComment() {
        return "COMMENTED".equalsIgnoreCase(state);
    }
}
