/**
 * Value object representing a section in a standup report.
 *
 * This is a pure domain object with no external dependencies.
 *
 * @param title   The section title (required)
 * @param content The section content (required, can be empty)
 */
public record ReportSection(String title, String content) {

    /**
     * Compact constructor for validation.
     */
    public ReportSection {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Title is required");
        }
        if (content == null) {
            throw new IllegalArgumentException("Content is required (can be empty string)");
        }
    }

    /**
     * Check if the section has any content.
     */
    public boolean hasContent() {
        return !content.isBlank();
    }

    /**
     * Create a section with empty content.
     */
    public static ReportSection empty(String title) {
        return new ReportSection(title, "");
    }
}
