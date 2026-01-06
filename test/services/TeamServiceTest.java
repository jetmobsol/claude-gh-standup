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
//SOURCES ../../scripts/domain/team/TeamMember.java
//SOURCES ../../scripts/domain/team/TeamReport.java
//SOURCES ../../scripts/ports/ActivityPort.java
//SOURCES ../mocks/MockActivityPort.java
//SOURCES ../../scripts/services/ActivityService.java
//SOURCES ../../scripts/services/TeamService.java

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.List;

/**
 * TDD tests for TeamService.
 * Write tests first, then implement TeamService to make them pass.
 */
public class TeamServiceTest {

    private MockActivityPort mockPort;
    private ActivityService activityService;
    private TeamService service;

    @BeforeEach
    void setUp() {
        mockPort = new MockActivityPort();
        activityService = new ActivityService(mockPort);
        service = new TeamService(activityService);
    }

    @Test
    @DisplayName("TeamService aggregates activity for multiple users")
    void aggregatesActivityForMultipleUsers() {
        // Given
        List<String> users = List.of("alice", "bob", "charlie");
        DateRange range = DateRange.lastDays(7);

        mockPort.stubCommits(List.of(
            new Commit("sha1", "Commit 1", "alice", Instant.now(), "url", "repo")
        ));
        mockPort.stubPullRequests(List.of(
            new PullRequest(1, "PR 1", "open", "url", "repo", 10, 5)
        ));

        // When
        TeamReport result = service.aggregate(users, range, null);

        // Then
        assertEquals(3, result.memberCount());
    }

    @Test
    @DisplayName("TeamService collects activity for each team member")
    void collectsActivityForEachTeamMember() {
        // Given
        List<String> users = List.of("user1", "user2");
        DateRange range = DateRange.lastDays(3);

        mockPort.stubCommits(List.of());
        mockPort.stubPullRequests(List.of());
        mockPort.stubIssues(List.of());
        mockPort.stubReviews(List.of());

        // When
        service.aggregate(users, range, null);

        // Then - each user requires 4 port calls (commits, PRs, issues, reviews)
        assertEquals(8, mockPort.getFetchCommitsCalls() +
                       mockPort.getFetchPullRequestsCalls() +
                       mockPort.getFetchIssuesCalls() +
                       mockPort.getFetchReviewsCalls());
    }

    @Test
    @DisplayName("TeamService includes activity in team members")
    void includesActivityInTeamMembers() {
        // Given
        List<String> users = List.of("active-user");
        DateRange range = DateRange.lastDays(1);

        mockPort.stubCommits(List.of(
            new Commit("sha", "msg", "active-user", null, null, null)
        ));
        mockPort.stubPullRequests(List.of());
        mockPort.stubIssues(List.of());
        mockPort.stubReviews(List.of());

        // When
        TeamReport result = service.aggregate(users, range, null);

        // Then
        assertEquals(1, result.memberCount());
        TeamMember member = result.members().get(0);
        assertEquals("active-user", member.username());
        assertTrue(member.hasActivity());
        assertEquals(1, member.totalActivityCount());
    }

    @Test
    @DisplayName("TeamService handles empty user list")
    void handlesEmptyUserList() {
        // Given
        List<String> users = List.of();
        DateRange range = DateRange.lastDays(1);

        // When
        TeamReport result = service.aggregate(users, range, null);

        // Then
        assertEquals(0, result.memberCount());
        assertEquals(0, result.totalActivityCount());
    }

    @Test
    @DisplayName("TeamService calculates team totals correctly")
    void calculatesTeamTotalsCorrectly() {
        // Given
        List<String> users = List.of("user1", "user2");
        DateRange range = DateRange.lastDays(3);

        // Stub different activity for each call
        mockPort.stubCommits(List.of(
            new Commit("sha1", "msg1", "user1", null, null, null),
            new Commit("sha2", "msg2", "user1", null, null, null)
        ));
        mockPort.stubPullRequests(List.of(
            new PullRequest(1, "PR", "open", null, null, 0, 0)
        ));
        mockPort.stubIssues(List.of());
        mockPort.stubReviews(List.of());

        // When
        TeamReport result = service.aggregate(users, range, null);

        // Then
        // Each user gets 2 commits + 1 PR = 3 items, so total = 6
        assertEquals(6, result.totalActivityCount());
    }

    // Main method to run tests via JBang
    public static void main(String[] args) {
        var launcher = org.junit.platform.launcher.core.LauncherFactory.create();
        var listener = new org.junit.platform.launcher.listeners.SummaryGeneratingListener();

        launcher.registerTestExecutionListeners(listener);
        launcher.execute(org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request()
            .selectors(org.junit.platform.engine.discovery.DiscoverySelectors.selectClass(TeamServiceTest.class))
            .build());

        listener.getSummary().printTo(new java.io.PrintWriter(System.out));
        if (listener.getSummary().getTotalFailureCount() > 0) System.exit(1);
    }
}
