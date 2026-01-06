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
//SOURCES ../mocks/MockActivityPort.java
//SOURCES ../../scripts/services/ActivityService.java

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.List;

/**
 * TDD tests for ActivityService.
 * Write tests first, then implement ActivityService to make them pass.
 */
public class ActivityServiceTest {

    private MockActivityPort mockPort;
    private ActivityService service;

    @BeforeEach
    void setUp() {
        mockPort = new MockActivityPort();
        service = new ActivityService(mockPort);
    }

    @Test
    @DisplayName("ActivityService collects activity from all sources")
    void collectsActivityFromAllSources() {
        // Given
        DateRange range = DateRange.lastDays(3);
        Repository repo = new Repository("owner", "repo");

        List<Commit> commits = List.of(
            new Commit("sha1", "Fix bug", "user", Instant.now(), "url1", "owner/repo")
        );
        List<PullRequest> prs = List.of(
            new PullRequest(1, "Add feature", "open", "url2", "owner/repo", 10, 5)
        );
        List<Issue> issues = List.of(
            new Issue(1, "Bug report", "open", "url3", "owner/repo", List.of("bug"))
        );
        List<Review> reviews = List.of(
            new Review(1, "APPROVED", "Looks good", "url4", "owner/repo")
        );

        mockPort.stubCommits(commits);
        mockPort.stubPullRequests(prs);
        mockPort.stubIssues(issues);
        mockPort.stubReviews(reviews);

        // When
        Activity result = service.collect("user", range, repo);

        // Then
        assertEquals(1, result.commits().size());
        assertEquals(1, result.pullRequests().size());
        assertEquals(1, result.issues().size());
        assertEquals(1, result.reviews().size());
        assertEquals("user", result.username());
    }

    @Test
    @DisplayName("ActivityService calls port with correct parameters")
    void callsPortWithCorrectParameters() {
        // Given
        DateRange range = DateRange.lastDays(7);
        Repository repo = new Repository("octocat", "hello-world");

        // When
        service.collect("octocat", range, repo);

        // Then
        assertEquals(1, mockPort.getFetchCommitsCalls());
        assertEquals(1, mockPort.getFetchPullRequestsCalls());
        assertEquals(1, mockPort.getFetchIssuesCalls());
        assertEquals(1, mockPort.getFetchReviewsCalls());
        assertEquals("octocat", mockPort.getLastUsername());
        assertEquals(range, mockPort.getLastDateRange());
        assertEquals(repo, mockPort.getLastRepository());
    }

    @Test
    @DisplayName("ActivityService works with null repository")
    void worksWithNullRepository() {
        // Given
        DateRange range = DateRange.lastDays(1);
        mockPort.stubCommits(List.of());
        mockPort.stubPullRequests(List.of());
        mockPort.stubIssues(List.of());
        mockPort.stubReviews(List.of());

        // When
        Activity result = service.collect("user", range, null);

        // Then
        assertNotNull(result);
        assertNull(mockPort.getLastRepository());
    }

    @Test
    @DisplayName("ActivityService returns empty activity when no data")
    void returnsEmptyActivityWhenNoData() {
        // Given
        mockPort.stubCommits(List.of());
        mockPort.stubPullRequests(List.of());
        mockPort.stubIssues(List.of());
        mockPort.stubReviews(List.of());

        // When
        Activity result = service.collect("user", DateRange.lastDays(1), null);

        // Then
        assertTrue(result.commits().isEmpty());
        assertTrue(result.pullRequests().isEmpty());
        assertTrue(result.issues().isEmpty());
        assertTrue(result.reviews().isEmpty());
        assertEquals(0, result.totalCount());
    }

    @Test
    @DisplayName("ActivityService sets correct days on Activity")
    void setsCorrectDaysOnActivity() {
        // Given
        DateRange range = DateRange.lastDays(5);
        mockPort.stubCommits(List.of());
        mockPort.stubPullRequests(List.of());
        mockPort.stubIssues(List.of());
        mockPort.stubReviews(List.of());

        // When
        Activity result = service.collect("user", range, null);

        // Then
        assertEquals(5, result.days());
    }

    // Main method to run tests via JBang
    public static void main(String[] args) {
        var launcher = org.junit.platform.launcher.core.LauncherFactory.create();
        var listener = new org.junit.platform.launcher.listeners.SummaryGeneratingListener();

        launcher.registerTestExecutionListeners(listener);
        launcher.execute(org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request()
            .selectors(org.junit.platform.engine.discovery.DiscoverySelectors.selectClass(ActivityServiceTest.class))
            .build());

        listener.getSummary().printTo(new java.io.PrintWriter(System.out));
        if (listener.getSummary().getTotalFailureCount() > 0) System.exit(1);
    }
}
