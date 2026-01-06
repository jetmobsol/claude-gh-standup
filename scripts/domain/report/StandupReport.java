import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Aggregate root representing a standup report.
 *
 * This is a pure domain object with no external dependencies.
 *
 * @param sections    List of report sections (immutable)
 * @param username    The GitHub username this report is for
 * @param days        Number of days of activity covered
 * @param generatedAt When the report was generated
 */
public record StandupReport(
    List<ReportSection> sections,
    String username,
    int days,
    Instant generatedAt
) {
    /**
     * Compact constructor for validation and immutability.
     */
    public StandupReport {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username is required");
        }
        if (days <= 0) {
            throw new IllegalArgumentException("Days must be positive");
        }
        if (generatedAt == null) {
            throw new IllegalArgumentException("Generated timestamp is required");
        }
        // Make sections immutable
        sections = sections == null ? List.of() : List.copyOf(sections);
    }

    /**
     * Get the full report content as a single string.
     */
    public String fullContent() {
        return sections.stream()
            .map(s -> "## " + s.title() + "\n\n" + s.content())
            .collect(Collectors.joining("\n\n"));
    }

    /**
     * Find a section by title.
     */
    public Optional<ReportSection> findSection(String title) {
        return sections.stream()
            .filter(s -> s.title().equalsIgnoreCase(title))
            .findFirst();
    }

    /**
     * Check if the report has any sections.
     */
    public boolean hasSections() {
        return !sections.isEmpty();
    }

    /**
     * Get the number of sections with actual content.
     */
    public long sectionsWithContent() {
        return sections.stream().filter(ReportSection::hasContent).count();
    }
}
