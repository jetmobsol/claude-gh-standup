import java.util.List;

/**
 * Domain entity representing a GitHub Issue.
 *
 * This is a pure domain object with no external dependencies.
 * JSON parsing happens in the infrastructure layer (adapters).
 *
 * @param number     The issue number (required, positive)
 * @param title      The issue title (required)
 * @param state      The issue state: "open", "closed" (required)
 * @param url        The URL to view the issue on GitHub (optional)
 * @param repository The repository in "owner/repo" format (optional)
 * @param labels     List of label names (optional, can be null or empty)
 */
public record Issue(
    int number,
    String title,
    String state,
    String url,
    String repository,
    List<String> labels
) {
    /**
     * Compact constructor for validation.
     */
    public Issue {
        if (number <= 0) {
            throw new IllegalArgumentException("Issue number must be positive");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Title is required");
        }
        if (state == null || state.isBlank()) {
            throw new IllegalArgumentException("State is required");
        }
        // Make labels immutable if provided
        if (labels != null) {
            labels = List.copyOf(labels);
        }
    }

    /**
     * Check if issue is still open.
     */
    public boolean isOpen() {
        return "open".equalsIgnoreCase(state);
    }

    /**
     * Check if issue has a specific label.
     */
    public boolean hasLabel(String label) {
        return labels != null && labels.contains(label);
    }
}
