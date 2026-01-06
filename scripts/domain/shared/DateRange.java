import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * Value object representing a date range for activity queries.
 *
 * This is a pure domain object with no external dependencies.
 *
 * @param start The start date (inclusive, required)
 * @param end   The end date (inclusive, required, must be >= start)
 */
public record DateRange(LocalDate start, LocalDate end) {

    private static final DateTimeFormatter ISO_FORMAT = DateTimeFormatter.ISO_DATE;

    /**
     * Compact constructor for validation.
     */
    public DateRange {
        if (start == null) {
            throw new IllegalArgumentException("Start date is required");
        }
        if (end == null) {
            throw new IllegalArgumentException("End date is required");
        }
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("End date must be on or after start date");
        }
    }

    /**
     * Create a date range from N days ago until today.
     *
     * @param days Number of days to look back (must be positive)
     * @return DateRange from (today - days) to today
     */
    public static DateRange lastDays(int days) {
        if (days <= 0) {
            throw new IllegalArgumentException("Days must be positive");
        }
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(days);
        return new DateRange(start, end);
    }

    /**
     * Get start date in ISO format (YYYY-MM-DD) for GitHub API.
     */
    public String startIso() {
        return start.format(ISO_FORMAT);
    }

    /**
     * Get end date in ISO format (YYYY-MM-DD) for GitHub API.
     */
    public String endIso() {
        return end.format(ISO_FORMAT);
    }

    /**
     * Calculate the number of days between start and end (exclusive of end).
     * For a range of Jan 1 to Jan 3, this returns 2.
     */
    public long days() {
        return ChronoUnit.DAYS.between(start, end);
    }

    /**
     * Check if a date falls within this range (inclusive).
     */
    public boolean contains(LocalDate date) {
        return !date.isBefore(start) && !date.isAfter(end);
    }
}
