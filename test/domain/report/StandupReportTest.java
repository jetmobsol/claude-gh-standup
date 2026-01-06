///usr/bin/env jbang "$0" "$@" ; exit $?

//DEPS org.junit.jupiter:junit-jupiter:5.10.0
//DEPS org.junit.platform:junit-platform-launcher:1.10.0
//SOURCES ../../../scripts/domain/report/ReportSection.java
//SOURCES ../../../scripts/domain/report/StandupReport.java

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.List;

/**
 * TDD tests for StandupReport aggregate.
 */
public class StandupReportTest {

    @Test
    @DisplayName("StandupReport stores sections and metadata")
    void standupReportStoresSectionsAndMetadata() {
        Instant timestamp = Instant.now();
        List<ReportSection> sections = List.of(
            new ReportSection("Yesterday", "Completed feature X"),
            new ReportSection("Today", "Working on feature Y")
        );

        StandupReport report = new StandupReport(sections, "testuser", 3, timestamp);

        assertEquals(2, report.sections().size());
        assertEquals("testuser", report.username());
        assertEquals(3, report.days());
        assertEquals(timestamp, report.generatedAt());
    }

    @Test
    @DisplayName("StandupReport with empty sections is valid")
    void standupReportWithEmptySections() {
        StandupReport report = new StandupReport(List.of(), "user", 1, Instant.now());

        assertTrue(report.sections().isEmpty());
    }

    @Test
    @DisplayName("StandupReport sections are immutable")
    void standupReportSectionsAreImmutable() {
        StandupReport report = new StandupReport(
            List.of(new ReportSection("Title", "Content")),
            "user", 1, Instant.now()
        );

        assertThrows(UnsupportedOperationException.class, () -> {
            report.sections().add(new ReportSection("New", "Section"));
        });
    }

    @Test
    @DisplayName("StandupReport provides full content as string")
    void standupReportFullContent() {
        List<ReportSection> sections = List.of(
            new ReportSection("Yesterday", "Did task A"),
            new ReportSection("Today", "Will do task B")
        );

        StandupReport report = new StandupReport(sections, "user", 1, Instant.now());
        String content = report.fullContent();

        assertTrue(content.contains("Yesterday"));
        assertTrue(content.contains("Did task A"));
        assertTrue(content.contains("Today"));
        assertTrue(content.contains("Will do task B"));
    }

    @Test
    @DisplayName("StandupReport finds section by title")
    void standupReportFindSection() {
        List<ReportSection> sections = List.of(
            new ReportSection("Yesterday", "Content 1"),
            new ReportSection("Today", "Content 2")
        );

        StandupReport report = new StandupReport(sections, "user", 1, Instant.now());

        assertTrue(report.findSection("Yesterday").isPresent());
        assertEquals("Content 1", report.findSection("Yesterday").get().content());
        assertTrue(report.findSection("Nonexistent").isEmpty());
    }

    @Test
    @DisplayName("Two StandupReports with same data are equal")
    void standupReportsWithSameDataAreEqual() {
        Instant time = Instant.parse("2024-01-15T10:00:00Z");
        List<ReportSection> sections = List.of(new ReportSection("Title", "Content"));

        StandupReport report1 = new StandupReport(sections, "user", 3, time);
        StandupReport report2 = new StandupReport(sections, "user", 3, time);

        assertEquals(report1, report2);
        assertEquals(report1.hashCode(), report2.hashCode());
    }

    @Test
    @DisplayName("StandupReport rejects invalid inputs")
    void standupReportRejectsInvalidInputs() {
        Instant time = Instant.now();
        List<ReportSection> sections = List.of();

        assertThrows(IllegalArgumentException.class, () ->
            new StandupReport(sections, null, 1, time));
        assertThrows(IllegalArgumentException.class, () ->
            new StandupReport(sections, "", 1, time));
        assertThrows(IllegalArgumentException.class, () ->
            new StandupReport(sections, "user", 0, time));
        assertThrows(IllegalArgumentException.class, () ->
            new StandupReport(sections, "user", 1, null));
    }

    @Test
    @DisplayName("ReportSection stores title and content")
    void reportSectionStoresTitleAndContent() {
        ReportSection section = new ReportSection("Yesterday", "Completed tasks A, B, C");

        assertEquals("Yesterday", section.title());
        assertEquals("Completed tasks A, B, C", section.content());
    }

    @Test
    @DisplayName("ReportSection with empty content is valid")
    void reportSectionWithEmptyContent() {
        ReportSection section = new ReportSection("Blockers", "");

        assertEquals("", section.content());
        assertFalse(section.hasContent());
    }

    @Test
    @DisplayName("ReportSection hasContent returns true when content present")
    void reportSectionHasContent() {
        ReportSection withContent = new ReportSection("Title", "Some content");
        ReportSection empty = new ReportSection("Title", "");
        ReportSection whitespace = new ReportSection("Title", "   ");

        assertTrue(withContent.hasContent());
        assertFalse(empty.hasContent());
        assertFalse(whitespace.hasContent());
    }

    // Main method to run tests via JBang
    public static void main(String[] args) {
        var launcher = org.junit.platform.launcher.core.LauncherFactory.create();
        var listener = new org.junit.platform.launcher.listeners.SummaryGeneratingListener();

        launcher.registerTestExecutionListeners(listener);
        launcher.execute(org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request()
            .selectors(org.junit.platform.engine.discovery.DiscoverySelectors.selectClass(StandupReportTest.class))
            .build());

        listener.getSummary().printTo(new java.io.PrintWriter(System.out));
        if (listener.getSummary().getTotalFailureCount() > 0) System.exit(1);
    }
}
