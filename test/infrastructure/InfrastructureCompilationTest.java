///usr/bin/env jbang "$0" "$@" ; exit $?

//DEPS com.google.code.gson:gson:2.10.1
//DEPS org.junit.jupiter:junit-jupiter:5.10.0
//DEPS org.junit.platform:junit-platform-launcher:1.10.0
//SOURCES ../../scripts/domain/activity/Commit.java
//SOURCES ../../scripts/domain/activity/PullRequest.java
//SOURCES ../../scripts/domain/activity/Issue.java
//SOURCES ../../scripts/domain/activity/Review.java
//SOURCES ../../scripts/domain/activity/Activity.java
//SOURCES ../../scripts/domain/shared/DateRange.java
//SOURCES ../../scripts/domain/shared/Repository.java
//SOURCES ../../scripts/domain/report/DiffSummary.java
//SOURCES ../../scripts/domain/report/ReportSection.java
//SOURCES ../../scripts/domain/report/StandupReport.java
//SOURCES ../../scripts/ports/ActivityPort.java
//SOURCES ../../scripts/ports/DiffPort.java
//SOURCES ../../scripts/ports/GitPort.java
//SOURCES ../../scripts/ports/ReportGeneratorPort.java
//SOURCES ../../scripts/ports/ExportPort.java
//SOURCES ../../scripts/infrastructure/github/GitHubCliAdapter.java
//SOURCES ../../scripts/infrastructure/git/GitCliAdapter.java
//SOURCES ../../scripts/infrastructure/ai/ClaudeCliAdapter.java
//SOURCES ../../scripts/infrastructure/export/MarkdownExporter.java
//SOURCES ../../scripts/infrastructure/export/JsonExporter.java
//SOURCES ../../scripts/infrastructure/export/HtmlExporter.java

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.List;

/**
 * Compilation test to verify all infrastructure adapters work with ports.
 */
public class InfrastructureCompilationTest {

    @Test
    @DisplayName("GitHubCliAdapter implements ActivityPort and DiffPort")
    void gitHubCliAdapterImplementsPorts() {
        GitHubCliAdapter adapter = new GitHubCliAdapter();

        assertTrue(adapter instanceof ActivityPort);
        assertTrue(adapter instanceof DiffPort);
    }

    @Test
    @DisplayName("GitCliAdapter implements GitPort")
    void gitCliAdapterImplementsPort() {
        GitCliAdapter adapter = new GitCliAdapter();

        assertTrue(adapter instanceof GitPort);
    }

    @Test
    @DisplayName("ClaudeCliAdapter implements ReportGeneratorPort")
    void claudeCliAdapterImplementsPort() {
        ClaudeCliAdapter adapter = new ClaudeCliAdapter();

        assertTrue(adapter instanceof ReportGeneratorPort);
    }

    @Test
    @DisplayName("MarkdownExporter implements ExportPort")
    void markdownExporterImplementsPort() {
        MarkdownExporter exporter = new MarkdownExporter();

        assertTrue(exporter instanceof ExportPort);
    }

    @Test
    @DisplayName("JsonExporter implements ExportPort")
    void jsonExporterImplementsPort() {
        JsonExporter exporter = new JsonExporter();

        assertTrue(exporter instanceof ExportPort);
    }

    @Test
    @DisplayName("HtmlExporter implements ExportPort")
    void htmlExporterImplementsPort() {
        HtmlExporter exporter = new HtmlExporter();

        assertTrue(exporter instanceof ExportPort);
    }

    @Test
    @DisplayName("MarkdownExporter produces valid markdown")
    void markdownExporterProducesValidMarkdown() {
        MarkdownExporter exporter = new MarkdownExporter();
        StandupReport report = new StandupReport(
            List.of(new ReportSection("Test Section", "Test content")),
            "testuser",
            3,
            Instant.now()
        );

        String result = exporter.export(report);

        assertTrue(result.contains("# Standup Report for testuser"));
        assertTrue(result.contains("## Test Section"));
        assertTrue(result.contains("Test content"));
    }

    @Test
    @DisplayName("JsonExporter produces valid JSON")
    void jsonExporterProducesValidJson() {
        JsonExporter exporter = new JsonExporter();
        StandupReport report = new StandupReport(
            List.of(new ReportSection("Test", "Content")),
            "user",
            1,
            Instant.now()
        );

        String result = exporter.export(report);

        assertTrue(result.contains("\"username\": \"user\""));
        assertTrue(result.contains("\"sections\""));
    }

    @Test
    @DisplayName("HtmlExporter produces valid HTML")
    void htmlExporterProducesValidHtml() {
        HtmlExporter exporter = new HtmlExporter();
        StandupReport report = new StandupReport(
            List.of(new ReportSection("HTML Test", "Some content")),
            "htmluser",
            7,
            Instant.now()
        );

        String result = exporter.export(report);

        assertTrue(result.contains("<!DOCTYPE html>"));
        assertTrue(result.contains("<title>Standup Report - htmluser</title>"));
        assertTrue(result.contains("HTML Test"));
    }

    // Main method to run tests via JBang
    public static void main(String[] args) {
        var launcher = org.junit.platform.launcher.core.LauncherFactory.create();
        var listener = new org.junit.platform.launcher.listeners.SummaryGeneratingListener();

        launcher.registerTestExecutionListeners(listener);
        launcher.execute(org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request()
            .selectors(org.junit.platform.engine.discovery.DiscoverySelectors.selectClass(InfrastructureCompilationTest.class))
            .build());

        listener.getSummary().printTo(new java.io.PrintWriter(System.out));
        if (listener.getSummary().getTotalFailureCount() > 0) System.exit(1);
    }
}
