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
//SOURCES ../../scripts/ports/DiffPort.java
//SOURCES ../mocks/MockDiffPort.java
//SOURCES ../../scripts/services/DiffService.java

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

/**
 * TDD tests for DiffService.
 * Write tests first, then implement DiffService to make them pass.
 */
public class DiffServiceTest {

    private MockDiffPort mockPort;
    private DiffService service;

    @BeforeEach
    void setUp() {
        mockPort = new MockDiffPort();
        service = new DiffService(mockPort);
    }

    @Test
    @DisplayName("DiffService analyzes diffs for all PRs")
    void analyzesDiffsForAllPRs() {
        // Given
        Repository repo = new Repository("owner", "repo");
        List<PullRequest> prs = List.of(
            new PullRequest(1, "PR 1", "open", "url1", "owner/repo", 10, 5),
            new PullRequest(2, "PR 2", "merged", "url2", "owner/repo", 20, 10)
        );

        mockPort.stubDiffSummaryForPR(1, new DiffSummary(3, 10, 5));
        mockPort.stubDiffSummaryForPR(2, new DiffSummary(5, 20, 10));

        // When
        DiffSummary result = service.analyze(prs, repo);

        // Then
        assertEquals(8, result.filesChanged());
        assertEquals(30, result.additions());
        assertEquals(15, result.deletions());
    }

    @Test
    @DisplayName("DiffService fetches diff for each PR")
    void fetchesDiffForEachPR() {
        // Given
        Repository repo = new Repository("owner", "repo");
        List<PullRequest> prs = List.of(
            new PullRequest(1, "PR 1", "open", "url", "owner/repo", 0, 0),
            new PullRequest(2, "PR 2", "open", "url", "owner/repo", 0, 0),
            new PullRequest(3, "PR 3", "open", "url", "owner/repo", 0, 0)
        );

        // When
        service.analyze(prs, repo);

        // Then
        assertEquals(3, mockPort.getFetchPRDiffCalls());
    }

    @Test
    @DisplayName("DiffService returns empty summary for no PRs")
    void returnsEmptySummaryForNoPRs() {
        // Given
        Repository repo = new Repository("owner", "repo");
        List<PullRequest> prs = List.of();

        // When
        DiffSummary result = service.analyze(prs, repo);

        // Then
        assertEquals(0, result.filesChanged());
        assertEquals(0, result.additions());
        assertEquals(0, result.deletions());
        assertEquals(0, mockPort.getFetchPRDiffCalls());
    }

    @Test
    @DisplayName("DiffService passes correct repo to port")
    void passesCorrectRepoToPort() {
        // Given
        Repository repo = new Repository("octocat", "hello-world");
        List<PullRequest> prs = List.of(
            new PullRequest(42, "Test PR", "open", "url", "octocat/hello-world", 0, 0)
        );

        // When
        service.analyze(prs, repo);

        // Then
        assertEquals(repo, mockPort.getLastRepository());
        assertEquals(42, mockPort.getLastPrNumber());
    }

    // Main method to run tests via JBang
    public static void main(String[] args) {
        var launcher = org.junit.platform.launcher.core.LauncherFactory.create();
        var listener = new org.junit.platform.launcher.listeners.SummaryGeneratingListener();

        launcher.registerTestExecutionListeners(listener);
        launcher.execute(org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request()
            .selectors(org.junit.platform.engine.discovery.DiscoverySelectors.selectClass(DiffServiceTest.class))
            .build());

        listener.getSummary().printTo(new java.io.PrintWriter(System.out));
        if (listener.getSummary().getTotalFailureCount() > 0) System.exit(1);
    }
}
