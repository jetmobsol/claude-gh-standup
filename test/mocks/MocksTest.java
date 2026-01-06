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
//SOURCES ../../scripts/ports/ActivityPort.java
//SOURCES ../../scripts/ports/DiffPort.java
//SOURCES ../../scripts/ports/GitPort.java
//SOURCES ../../scripts/ports/ReportGeneratorPort.java
//SOURCES ../../scripts/ports/ExportPort.java
//SOURCES MockActivityPort.java
//SOURCES MockDiffPort.java
//SOURCES MockGitPort.java
//SOURCES MockReportGeneratorPort.java
//SOURCES MockExportPort.java

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

/**
 * Tests for mock port implementations.
 */
public class MocksTest {

    private MockActivityPort activityPort;
    private MockDiffPort diffPort;
    private MockGitPort gitPort;
    private MockReportGeneratorPort reportGeneratorPort;
    private MockExportPort exportPort;

    @BeforeEach
    void setUp() {
        activityPort = new MockActivityPort();
        diffPort = new MockDiffPort();
        gitPort = new MockGitPort();
        reportGeneratorPort = new MockReportGeneratorPort();
        exportPort = new MockExportPort();
    }

    // --- MockActivityPort Tests ---

    @Test
    @DisplayName("MockActivityPort returns stubbed commits")
    void mockActivityPortReturnsStubbedCommits() {
        List<Commit> commits = List.of(
            new Commit("abc123", "Fix bug", "user", Instant.now(), "url", "repo")
        );
        activityPort.stubCommits(commits);

        List<Commit> result = activityPort.fetchCommits("user", DateRange.lastDays(1), null);

        assertEquals(1, result.size());
        assertEquals("abc123", result.get(0).sha());
    }

    @Test
    @DisplayName("MockActivityPort tracks method calls")
    void mockActivityPortTracksMethodCalls() {
        DateRange range = DateRange.lastDays(3);
        Repository repo = new Repository("owner", "repo");

        activityPort.fetchCommits("user1", range, repo);
        activityPort.fetchPullRequests("user1", range, repo);
        activityPort.fetchIssues("user1", range, repo);
        activityPort.fetchReviews("user1", range, repo);

        assertEquals(1, activityPort.getFetchCommitsCalls());
        assertEquals(1, activityPort.getFetchPullRequestsCalls());
        assertEquals(1, activityPort.getFetchIssuesCalls());
        assertEquals(1, activityPort.getFetchReviewsCalls());
        assertEquals("user1", activityPort.getLastUsername());
        assertEquals(range, activityPort.getLastDateRange());
        assertEquals(repo, activityPort.getLastRepository());
    }

    @Test
    @DisplayName("MockActivityPort reset clears state")
    void mockActivityPortResetClearsState() {
        activityPort.stubCommits(List.of(new Commit("sha", "msg", "author", null, null, null)));
        activityPort.fetchCommits("user", DateRange.lastDays(1), null);

        activityPort.reset();

        assertEquals(0, activityPort.getFetchCommitsCalls());
        assertTrue(activityPort.fetchCommits("x", DateRange.lastDays(1), null).isEmpty());
    }

    // --- MockDiffPort Tests ---

    @Test
    @DisplayName("MockDiffPort returns stubbed diff summary")
    void mockDiffPortReturnsStubbedSummary() {
        DiffSummary summary = new DiffSummary(10, 100, 50);
        diffPort.stubDiffSummary(summary);

        DiffSummary result = diffPort.fetchPRDiff(new Repository("o", "r"), 1);

        assertEquals(10, result.filesChanged());
        assertEquals(100, result.additions());
        assertEquals(50, result.deletions());
    }

    @Test
    @DisplayName("MockDiffPort supports per-PR stubs")
    void mockDiffPortSupportsPerPRStubs() {
        diffPort.stubDiffSummaryForPR(1, new DiffSummary(1, 10, 5));
        diffPort.stubDiffSummaryForPR(2, new DiffSummary(2, 20, 10));

        Repository repo = new Repository("o", "r");

        assertEquals(1, diffPort.fetchPRDiff(repo, 1).filesChanged());
        assertEquals(2, diffPort.fetchPRDiff(repo, 2).filesChanged());
    }

    // --- MockGitPort Tests ---

    @Test
    @DisplayName("MockGitPort returns stubbed branch")
    void mockGitPortReturnsStubbedBranch() {
        gitPort.stubCurrentBranch("feature/test");

        assertEquals("feature/test", gitPort.getCurrentBranch(Path.of(".")));
    }

    @Test
    @DisplayName("MockGitPort returns stubbed repository")
    void mockGitPortReturnsStubbedRepository() {
        Repository repo = new Repository("owner", "repo");
        gitPort.stubRepository(repo);

        assertTrue(gitPort.detectRepository(Path.of(".")).isPresent());
        assertEquals("owner", gitPort.detectRepository(Path.of(".")).get().owner());
    }

    @Test
    @DisplayName("MockGitPort returns empty when no repository stubbed")
    void mockGitPortReturnsEmptyWhenNoRepository() {
        assertTrue(gitPort.detectRepository(Path.of(".")).isEmpty());
    }

    // --- MockReportGeneratorPort Tests ---

    @Test
    @DisplayName("MockReportGeneratorPort captures prompts")
    void mockReportGeneratorPortCapturesPrompts() {
        reportGeneratorPort.generate("Prompt 1");
        reportGeneratorPort.generate("Prompt 2");

        assertEquals(2, reportGeneratorPort.getGenerateCalls());
        assertEquals(2, reportGeneratorPort.getCapturedPrompts().size());
        assertEquals("Prompt 2", reportGeneratorPort.getLastPrompt());
    }

    @Test
    @DisplayName("MockReportGeneratorPort checks prompt content")
    void mockReportGeneratorPortChecksPromptContent() {
        reportGeneratorPort.generate("Generate a standup report for user octocat");

        assertTrue(reportGeneratorPort.promptContains("octocat"));
        assertFalse(reportGeneratorPort.promptContains("nonexistent"));
    }

    // --- MockExportPort Tests ---

    @Test
    @DisplayName("MockExportPort returns stubbed export")
    void mockExportPortReturnsStubbedExport() {
        exportPort.stubExport("# Exported Report");

        StandupReport report = new StandupReport(List.of(), "user", 1, Instant.now());
        String result = exportPort.export(report);

        assertEquals("# Exported Report", result);
    }

    @Test
    @DisplayName("MockExportPort captures reports")
    void mockExportPortCapturesReports() {
        StandupReport report1 = new StandupReport(List.of(), "user1", 1, Instant.now());
        StandupReport report2 = new StandupReport(List.of(), "user2", 3, Instant.now());

        exportPort.export(report1);
        exportPort.export(report2);

        assertEquals(2, exportPort.getExportCalls());
        assertEquals("user2", exportPort.getLastReport().username());
    }

    // Main method to run tests via JBang
    public static void main(String[] args) {
        var launcher = org.junit.platform.launcher.core.LauncherFactory.create();
        var listener = new org.junit.platform.launcher.listeners.SummaryGeneratingListener();

        launcher.registerTestExecutionListeners(listener);
        launcher.execute(org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request()
            .selectors(org.junit.platform.engine.discovery.DiscoverySelectors.selectClass(MocksTest.class))
            .build());

        listener.getSummary().printTo(new java.io.PrintWriter(System.out));
        if (listener.getSummary().getTotalFailureCount() > 0) System.exit(1);
    }
}
