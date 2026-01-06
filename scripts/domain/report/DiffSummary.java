/**
 * Value object representing file change statistics from a diff.
 *
 * This is a pure domain object with no external dependencies.
 * Diff parsing happens in the infrastructure layer (adapters).
 *
 * @param filesChanged Number of files changed (non-negative)
 * @param additions    Number of lines added (non-negative)
 * @param deletions    Number of lines deleted (non-negative)
 */
public record DiffSummary(int filesChanged, int additions, int deletions) {

    /**
     * Compact constructor for validation.
     */
    public DiffSummary {
        if (filesChanged < 0) {
            throw new IllegalArgumentException("Files changed cannot be negative");
        }
        if (additions < 0) {
            throw new IllegalArgumentException("Additions cannot be negative");
        }
        if (deletions < 0) {
            throw new IllegalArgumentException("Deletions cannot be negative");
        }
    }

    /**
     * Create an empty diff summary (no changes).
     */
    public static DiffSummary empty() {
        return new DiffSummary(0, 0, 0);
    }

    /**
     * Total lines changed (additions + deletions).
     */
    public int totalChanges() {
        return additions + deletions;
    }

    /**
     * Check if there are no changes.
     */
    public boolean isEmpty() {
        return filesChanged == 0 && additions == 0 && deletions == 0;
    }

    /**
     * Combine this summary with another (for aggregating multiple PRs).
     */
    public DiffSummary combine(DiffSummary other) {
        return new DiffSummary(
            this.filesChanged + other.filesChanged,
            this.additions + other.additions,
            this.deletions + other.deletions
        );
    }
}
