///usr/bin/env jbang "$0" "$@" ; exit $?

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
//SOURCES ../../scripts/ports/ReportGeneratorPort.java
//SOURCES ../mocks/MockReportGeneratorPort.java
//SOURCES ../../scripts/services/ReportService.java

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.List;

/**
 * TDD tests for ReportService.
 * Write tests first, then implement ReportService to make them pass.
 */
public class ReportServiceTest {

    private MockReportGeneratorPort mockPort;
    private ReportService service;

    @BeforeEach
    void setUp() {
        mockPort = new MockReportGeneratorPort();
        service = new ReportService(mockPort);
    }

    @Test
    @DisplayName("ReportService generates report with activity")
    void generatesReportWithActivity() {
        // Given
        Activity activity = new Activity(
            List.of(new Commit("sha", "Fix bug", "user", Instant.now(), "url", "repo")),
            List.of(new PullRequest(1, "PR title", "open", "url", "repo", 10, 5)),
            List.of(),
            List.of(),
            "testuser",
            3,
            "owner/repo"
        );
        DiffSummary diffs = new DiffSummary(2, 10, 5);

        // When
        service.generate(activity, diffs);

        // Then
        assertEquals(1, mockPort.getGenerateCalls());
        assertTrue(mockPort.promptContains("testuser"));
        assertTrue(mockPort.promptContains("Fix bug"));
        assertTrue(mockPort.promptContains("PR title"));
    }

    @Test
    @DisplayName("ReportService includes diff summary in prompt")
    void includesDiffSummaryInPrompt() {
        // Given
        Activity activity = new Activity(
            List.of(),
            List.of(new PullRequest(1, "PR", "open", "url", "repo", 0, 0)),
            List.of(),
            List.of(),
            "user",
            1,
            null
        );
        DiffSummary diffs = new DiffSummary(5, 100, 50);

        // When
        service.generate(activity, diffs);

        // Then
        String prompt = mockPort.getLastPrompt();
        assertTrue(prompt.contains("100") || prompt.contains("additions"));
    }

    @Test
    @DisplayName("ReportService handles empty activity")
    void handlesEmptyActivity() {
        // Given
        Activity activity = new Activity(
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            "user",
            1,
            null
        );
        DiffSummary diffs = new DiffSummary(0, 0, 0);

        // When
        service.generate(activity, diffs);

        // Then
        assertEquals(1, mockPort.getGenerateCalls());
        assertTrue(mockPort.promptContains("user"));
    }

    @Test
    @DisplayName("ReportService includes issues and reviews in prompt")
    void includesIssuesAndReviewsInPrompt() {
        // Given
        Activity activity = new Activity(
            List.of(),
            List.of(),
            List.of(new Issue(1, "Bug: Critical error", "open", "url", "repo", List.of("bug"))),
            List.of(new Review(1, "APPROVED", "LGTM", "url", "repo")),
            "reviewer",
            7,
            null
        );
        DiffSummary diffs = new DiffSummary(0, 0, 0);

        // When
        service.generate(activity, diffs);

        // Then
        String prompt = mockPort.getLastPrompt();
        assertTrue(prompt.contains("Bug: Critical error") || prompt.contains("Issue"));
        assertTrue(prompt.contains("APPROVED") || prompt.contains("Review"));
    }

    // Main method to run tests via JBang
    public static void main(String[] args) {
        var launcher = org.junit.platform.launcher.core.LauncherFactory.create();
        var listener = new org.junit.platform.launcher.listeners.SummaryGeneratingListener();

        launcher.registerTestExecutionListeners(listener);
        launcher.execute(org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request()
            .selectors(org.junit.platform.engine.discovery.DiscoverySelectors.selectClass(ReportServiceTest.class))
            .build());

        listener.getSummary().printTo(new java.io.PrintWriter(System.out));
        if (listener.getSummary().getTotalFailureCount() > 0) System.exit(1);
    }
}
