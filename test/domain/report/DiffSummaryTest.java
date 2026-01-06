///usr/bin/env jbang "$0" "$@" ; exit $?

//DEPS org.junit.jupiter:junit-jupiter:5.10.0
//DEPS org.junit.platform:junit-platform-launcher:1.10.0
//SOURCES ../../../scripts/domain/report/DiffSummary.java

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * TDD tests for DiffSummary value object.
 */
public class DiffSummaryTest {

    @Test
    @DisplayName("DiffSummary stores file change statistics")
    void diffSummaryStoresStats() {
        DiffSummary summary = new DiffSummary(5, 150, 30);

        assertEquals(5, summary.filesChanged());
        assertEquals(150, summary.additions());
        assertEquals(30, summary.deletions());
    }

    @Test
    @DisplayName("DiffSummary with zero values is valid")
    void diffSummaryWithZeroValues() {
        DiffSummary summary = new DiffSummary(0, 0, 0);

        assertEquals(0, summary.filesChanged());
        assertEquals(0, summary.additions());
        assertEquals(0, summary.deletions());
    }

    @Test
    @DisplayName("DiffSummary calculates total changes")
    void diffSummaryCalculatesTotalChanges() {
        DiffSummary summary = new DiffSummary(3, 100, 50);

        assertEquals(150, summary.totalChanges());
    }

    @Test
    @DisplayName("DiffSummary.empty creates empty summary")
    void diffSummaryEmptyFactory() {
        DiffSummary empty = DiffSummary.empty();

        assertEquals(0, empty.filesChanged());
        assertEquals(0, empty.additions());
        assertEquals(0, empty.deletions());
        assertTrue(empty.isEmpty());
    }

    @Test
    @DisplayName("DiffSummary.isEmpty returns true when no changes")
    void diffSummaryIsEmpty() {
        DiffSummary empty = new DiffSummary(0, 0, 0);
        DiffSummary notEmpty = new DiffSummary(1, 10, 5);

        assertTrue(empty.isEmpty());
        assertFalse(notEmpty.isEmpty());
    }

    @Test
    @DisplayName("Two DiffSummaries with same data are equal")
    void diffSummariesWithSameDataAreEqual() {
        DiffSummary summary1 = new DiffSummary(5, 100, 50);
        DiffSummary summary2 = new DiffSummary(5, 100, 50);

        assertEquals(summary1, summary2);
        assertEquals(summary1.hashCode(), summary2.hashCode());
    }

    @Test
    @DisplayName("DiffSummary rejects negative values")
    void diffSummaryRejectsNegativeValues() {
        assertThrows(IllegalArgumentException.class, () -> new DiffSummary(-1, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new DiffSummary(0, -1, 0));
        assertThrows(IllegalArgumentException.class, () -> new DiffSummary(0, 0, -1));
    }

    @Test
    @DisplayName("DiffSummary.combine merges two summaries")
    void diffSummaryCombine() {
        DiffSummary s1 = new DiffSummary(2, 50, 10);
        DiffSummary s2 = new DiffSummary(3, 30, 20);

        DiffSummary combined = s1.combine(s2);

        assertEquals(5, combined.filesChanged());
        assertEquals(80, combined.additions());
        assertEquals(30, combined.deletions());
    }

    @Test
    @DisplayName("DiffSummary toString contains useful info")
    void diffSummaryToString() {
        DiffSummary summary = new DiffSummary(5, 100, 50);

        String str = summary.toString();
        assertTrue(str.contains("5"), "toString should contain files changed");
        assertTrue(str.contains("100") || str.contains("additions"), "toString should contain additions info");
    }

    // Main method to run tests via JBang
    public static void main(String[] args) {
        var launcher = org.junit.platform.launcher.core.LauncherFactory.create();
        var listener = new org.junit.platform.launcher.listeners.SummaryGeneratingListener();

        launcher.registerTestExecutionListeners(listener);
        launcher.execute(org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request()
            .selectors(org.junit.platform.engine.discovery.DiscoverySelectors.selectClass(DiffSummaryTest.class))
            .build());

        listener.getSummary().printTo(new java.io.PrintWriter(System.out));
        if (listener.getSummary().getTotalFailureCount() > 0) System.exit(1);
    }
}
