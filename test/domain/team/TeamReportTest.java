///usr/bin/env jbang "$0" "$@" ; exit $?

//DEPS org.junit.jupiter:junit-jupiter:5.10.0
//DEPS org.junit.platform:junit-platform-launcher:1.10.0
//SOURCES ../../../scripts/domain/activity/Commit.java
//SOURCES ../../../scripts/domain/activity/PullRequest.java
//SOURCES ../../../scripts/domain/activity/Issue.java
//SOURCES ../../../scripts/domain/activity/Review.java
//SOURCES ../../../scripts/domain/activity/Activity.java
//SOURCES ../../../scripts/domain/team/TeamMember.java
//SOURCES ../../../scripts/domain/team/TeamReport.java

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.List;

/**
 * TDD tests for Team bounded context entities.
 */
public class TeamReportTest {

    // --- TeamMember Tests ---

    @Test
    @DisplayName("TeamMember stores username and activity")
    void teamMemberStoresData() {
        Activity activity = createActivity("user1", 2, 1, 0, 0);
        TeamMember member = new TeamMember("user1", activity);

        assertEquals("user1", member.username());
        assertEquals(activity, member.activity());
    }

    @Test
    @DisplayName("TeamMember with null activity is valid")
    void teamMemberWithNullActivity() {
        TeamMember member = new TeamMember("user1", null);

        assertNull(member.activity());
        assertFalse(member.hasActivity());
        assertEquals(0, member.totalActivityCount());
    }

    @Test
    @DisplayName("TeamMember.pending creates member without activity")
    void teamMemberPending() {
        TeamMember member = TeamMember.pending("user1");

        assertEquals("user1", member.username());
        assertFalse(member.hasActivity());
    }

    @Test
    @DisplayName("TeamMember rejects null/blank username")
    void teamMemberRejectsInvalidUsername() {
        assertThrows(IllegalArgumentException.class, () -> new TeamMember(null, null));
        assertThrows(IllegalArgumentException.class, () -> new TeamMember("", null));
        assertThrows(IllegalArgumentException.class, () -> new TeamMember("  ", null));
    }

    // --- TeamReport Tests ---

    @Test
    @DisplayName("TeamReport stores members and metadata")
    void teamReportStoresData() {
        Instant time = Instant.now();
        List<TeamMember> members = List.of(
            new TeamMember("user1", createActivity("user1", 3, 1, 0, 0)),
            new TeamMember("user2", createActivity("user2", 2, 2, 1, 0))
        );

        TeamReport report = new TeamReport(members, 7, time);

        assertEquals(2, report.memberCount());
        assertEquals(7, report.days());
        assertEquals(time, report.generatedAt());
    }

    @Test
    @DisplayName("TeamReport calculates total activity")
    void teamReportCalculatesTotalActivity() {
        List<TeamMember> members = List.of(
            new TeamMember("user1", createActivity("user1", 3, 2, 1, 1)),  // 7 items
            new TeamMember("user2", createActivity("user2", 2, 1, 0, 0))   // 3 items
        );

        TeamReport report = new TeamReport(members, 7, Instant.now());

        assertEquals(10, report.totalActivityCount());
        assertEquals(5, report.totalCommits());
        assertEquals(3, report.totalPullRequests());
        assertEquals(1, report.totalIssues());
    }

    @Test
    @DisplayName("TeamReport with empty members is valid")
    void teamReportWithEmptyMembers() {
        TeamReport report = new TeamReport(List.of(), 3, Instant.now());

        assertEquals(0, report.memberCount());
        assertEquals(0, report.totalActivityCount());
    }

    @Test
    @DisplayName("TeamReport members are immutable")
    void teamReportMembersAreImmutable() {
        TeamReport report = new TeamReport(
            List.of(new TeamMember("user1", null)),
            1, Instant.now()
        );

        assertThrows(UnsupportedOperationException.class, () -> {
            report.members().add(new TeamMember("user2", null));
        });
    }

    @Test
    @DisplayName("TeamReport calculates active member count")
    void teamReportActiveMemberCount() {
        List<TeamMember> members = List.of(
            new TeamMember("active1", createActivity("active1", 1, 0, 0, 0)),
            new TeamMember("active2", createActivity("active2", 0, 1, 0, 0)),
            new TeamMember("inactive", createActivity("inactive", 0, 0, 0, 0))
        );

        TeamReport report = new TeamReport(members, 7, Instant.now());

        assertEquals(3, report.memberCount());
        assertEquals(2, report.activeMemberCount());
    }

    @Test
    @DisplayName("TeamReport rejects invalid inputs")
    void teamReportRejectsInvalidInputs() {
        assertThrows(IllegalArgumentException.class, () ->
            new TeamReport(List.of(), 0, Instant.now()));
        assertThrows(IllegalArgumentException.class, () ->
            new TeamReport(List.of(), 1, null));
    }

    // Helper to create Activity with specified counts
    private Activity createActivity(String username, int commits, int prs, int issues, int reviews) {
        return new Activity(
            createCommits(commits),
            createPRs(prs),
            createIssues(issues),
            createReviews(reviews),
            username,
            7,
            null
        );
    }

    private List<Commit> createCommits(int count) {
        return java.util.stream.IntStream.range(0, count)
            .mapToObj(i -> new Commit("sha" + i, "msg" + i, "user", null, null, null))
            .toList();
    }

    private List<PullRequest> createPRs(int count) {
        return java.util.stream.IntStream.range(0, count)
            .mapToObj(i -> new PullRequest(i + 1, "PR " + i, "open", null, null, 0, 0))
            .toList();
    }

    private List<Issue> createIssues(int count) {
        return java.util.stream.IntStream.range(0, count)
            .mapToObj(i -> new Issue(i + 1, "Issue " + i, "open", null, null, null))
            .toList();
    }

    private List<Review> createReviews(int count) {
        return java.util.stream.IntStream.range(0, count)
            .mapToObj(i -> new Review(i + 1, "APPROVED", null, null, null))
            .toList();
    }

    // Main method to run tests via JBang
    public static void main(String[] args) {
        var launcher = org.junit.platform.launcher.core.LauncherFactory.create();
        var listener = new org.junit.platform.launcher.listeners.SummaryGeneratingListener();

        launcher.registerTestExecutionListeners(listener);
        launcher.execute(org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request()
            .selectors(org.junit.platform.engine.discovery.DiscoverySelectors.selectClass(TeamReportTest.class))
            .build());

        listener.getSummary().printTo(new java.io.PrintWriter(System.out));
        if (listener.getSummary().getTotalFailureCount() > 0) System.exit(1);
    }
}
