import java.time.Instant;

/**
 * Domain entity representing a Git commit.
 *
 * This is a pure domain object with no external dependencies.
 * JSON parsing happens in the infrastructure layer (adapters).
 *
 * @param sha        The commit SHA hash (required)
 * @param message    The commit message (required)
 * @param author     The commit author username (required)
 * @param date       The commit date (optional)
 * @param url        The URL to view the commit on GitHub (optional)
 * @param repository The repository in "owner/repo" format (optional)
 */
public record Commit(
    String sha,
    String message,
    String author,
    Instant date,
    String url,
    String repository
) {
    /**
     * Compact constructor for validation.
     * SHA, message, and author are required fields.
     */
    public Commit {
        if (sha == null || sha.isBlank()) {
            throw new IllegalArgumentException("SHA is required");
        }
        if (message == null) {
            throw new IllegalArgumentException("Message is required");
        }
        if (author == null || author.isBlank()) {
            throw new IllegalArgumentException("Author is required");
        }
    }
}
