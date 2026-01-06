///usr/bin/env jbang "$0" "$@" ; exit $?

//DEPS org.junit.jupiter:junit-jupiter:5.10.0
//DEPS org.junit.platform:junit-platform-launcher:1.10.0
//SOURCES ../../../scripts/domain/shared/DateRange.java

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * TDD tests for DateRange value object.
 */
public class DateRangeTest {

    @Test
    @DisplayName("DateRange stores start and end dates")
    void dateRangeStoresDates() {
        LocalDate start = LocalDate.of(2024, 1, 1);
        LocalDate end = LocalDate.of(2024, 1, 7);

        DateRange range = new DateRange(start, end);

        assertEquals(start, range.start());
        assertEquals(end, range.end());
    }

    @Test
    @DisplayName("DateRange.lastDays creates range from N days ago to now")
    void lastDaysCreatesCorrectRange() {
        DateRange range = DateRange.lastDays(7);

        LocalDate expectedStart = LocalDate.now().minusDays(7);
        LocalDate expectedEnd = LocalDate.now();

        assertEquals(expectedStart, range.start());
        assertEquals(expectedEnd, range.end());
    }

    @Test
    @DisplayName("DateRange.lastDays with 1 day")
    void lastDaysWithOneDay() {
        DateRange range = DateRange.lastDays(1);

        assertEquals(LocalDate.now().minusDays(1), range.start());
        assertEquals(LocalDate.now(), range.end());
    }

    @Test
    @DisplayName("DateRange provides ISO date format for GitHub API")
    void dateRangeProvidesIsoFormat() {
        LocalDate date = LocalDate.of(2024, 3, 15);
        DateRange range = new DateRange(date, date);

        assertEquals("2024-03-15", range.startIso());
        assertEquals("2024-03-15", range.endIso());
    }

    @Test
    @DisplayName("DateRange calculates duration in days")
    void dateRangeCalculatesDays() {
        LocalDate start = LocalDate.of(2024, 1, 1);
        LocalDate end = LocalDate.of(2024, 1, 8);

        DateRange range = new DateRange(start, end);

        assertEquals(7, range.days());
    }

    @Test
    @DisplayName("DateRange with same start and end has 0 days duration")
    void sameStartEndHasZeroDays() {
        LocalDate date = LocalDate.now();
        DateRange range = new DateRange(date, date);

        assertEquals(0, range.days());
    }

    @Test
    @DisplayName("Two DateRanges with same dates are equal")
    void dateRangesWithSameDatesAreEqual() {
        LocalDate start = LocalDate.of(2024, 1, 1);
        LocalDate end = LocalDate.of(2024, 1, 7);

        DateRange range1 = new DateRange(start, end);
        DateRange range2 = new DateRange(start, end);

        assertEquals(range1, range2);
        assertEquals(range1.hashCode(), range2.hashCode());
    }

    @Test
    @DisplayName("DateRange rejects null dates")
    void dateRangeRejectsNullDates() {
        assertThrows(IllegalArgumentException.class, () -> new DateRange(null, LocalDate.now()));
        assertThrows(IllegalArgumentException.class, () -> new DateRange(LocalDate.now(), null));
    }

    @Test
    @DisplayName("DateRange rejects end before start")
    void dateRangeRejectsEndBeforeStart() {
        LocalDate start = LocalDate.of(2024, 1, 10);
        LocalDate end = LocalDate.of(2024, 1, 1);

        assertThrows(IllegalArgumentException.class, () -> new DateRange(start, end));
    }

    @Test
    @DisplayName("DateRange.lastDays rejects non-positive days")
    void lastDaysRejectsNonPositive() {
        assertThrows(IllegalArgumentException.class, () -> DateRange.lastDays(0));
        assertThrows(IllegalArgumentException.class, () -> DateRange.lastDays(-1));
    }

    // Main method to run tests via JBang
    public static void main(String[] args) {
        var launcher = org.junit.platform.launcher.core.LauncherFactory.create();
        var listener = new org.junit.platform.launcher.listeners.SummaryGeneratingListener();

        launcher.registerTestExecutionListeners(listener);
        launcher.execute(org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request()
            .selectors(org.junit.platform.engine.discovery.DiscoverySelectors.selectClass(DateRangeTest.class))
            .build());

        listener.getSummary().printTo(new java.io.PrintWriter(System.out));
        if (listener.getSummary().getTotalFailureCount() > 0) System.exit(1);
    }
}
