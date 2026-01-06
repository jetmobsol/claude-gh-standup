///usr/bin/env jbang "$0" "$@" ; exit $?

//DEPS org.junit.jupiter:junit-jupiter:5.10.0
//DEPS org.junit.platform:junit-platform-launcher:1.10.0
//SOURCES ../../../scripts/domain/activity/Commit.java
//SOURCES ../../../scripts/domain/activity/PullRequest.java
//SOURCES ../../../scripts/domain/activity/Issue.java
//SOURCES ../../../scripts/domain/activity/Review.java
//SOURCES ../../../scripts/domain/activity/Activity.java

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.List;

/**
 * TDD tests for Activity aggregate root.
 */
public class ActivityTest {

    @Test
    @DisplayName("Activity aggregate stores all entity lists")
    void activityStoresAllLists() {
        List<Commit> commits = List.of(
            new Commit("sha1", "msg1", "user", Instant.now(), "url", "repo")
        );
        List<PullRequest> prs = List.of(
            new PullRequest(1, "PR Title", "open", "url", "repo", 10, 5)
        );
        List<Issue> issues = List.of(
            new Issue(1, "Issue Title", "open", "url", "repo", List.of("bug"))
        );
        List<Review> reviews = List.of(
            new Review(1, "APPROVED", "LGTM", "url", "repo")
        );

        Activity activity = new Activity(commits, prs, issues, reviews, "testuser", 7, "owner/repo");

        assertEquals(1, activity.commits().size());
        assertEquals(1, activity.pullRequests().size());
        assertEquals(1, activity.issues().size());
        assertEquals(1, activity.reviews().size());
        assertEquals("testuser", activity.username());
        assertEquals(7, activity.days());
        assertEquals("owner/repo", activity.repository());
    }

    @Test
    @DisplayName("Activity with empty lists is valid")
    void activityWithEmptyLists() {
        Activity activity = new Activity(
            List.of(), List.of(), List.of(), List.of(),
            "user", 3, null
        );

        assertTrue(activity.commits().isEmpty());
        assertTrue(activity.pullRequests().isEmpty());
        assertTrue(activity.issues().isEmpty());
        assertTrue(activity.reviews().isEmpty());
    }

    @Test
    @DisplayName("Activity with null repository is valid")
    void activityWithNullRepository() {
        Activity activity = new Activity(
            List.of(), List.of(), List.of(), List.of(),
            "user", 1, null
        );

        assertNull(activity.repository());
    }

    @Test
    @DisplayName("Activity lists are immutable")
    void activityListsAreImmutable() {
        Activity activity = new Activity(
            List.of(new Commit("sha", "msg", "user", null, null, null)),
            List.of(),
            List.of(),
            List.of(),
            "user", 1, null
        );

        // Attempting to modify should throw
        assertThrows(UnsupportedOperationException.class, () -> {
            activity.commits().add(new Commit("new", "new", "new", null, null, null));
        });
    }

    @Test
    @DisplayName("Activity provides total count of all items")
    void activityTotalCount() {
        Activity activity = new Activity(
            List.of(
                new Commit("sha1", "msg1", "user", null, null, null),
                new Commit("sha2", "msg2", "user", null, null, null)
            ),
            List.of(
                new PullRequest(1, "PR1", "open", null, null, 0, 0),
                new PullRequest(2, "PR2", "closed", null, null, 0, 0),
                new PullRequest(3, "PR3", "merged", null, null, 0, 0)
            ),
            List.of(
                new Issue(1, "Issue1", "open", null, null, null)
            ),
            List.of(
                new Review(1, "APPROVED", null, null, null),
                new Review(2, "COMMENTED", null, null, null)
            ),
            "user", 7, null
        );

        assertEquals(8, activity.totalCount());
        assertEquals(2, activity.commits().size());
        assertEquals(3, activity.pullRequests().size());
        assertEquals(1, activity.issues().size());
        assertEquals(2, activity.reviews().size());
    }

    @Test
    @DisplayName("Activity hasActivity returns true when there's any activity")
    void activityHasActivity() {
        Activity empty = new Activity(
            List.of(), List.of(), List.of(), List.of(),
            "user", 1, null
        );
        Activity withCommit = new Activity(
            List.of(new Commit("sha", "msg", "user", null, null, null)),
            List.of(), List.of(), List.of(),
            "user", 1, null
        );

        assertFalse(empty.hasActivity());
        assertTrue(withCommit.hasActivity());
    }

    @Test
    @DisplayName("Two activities with same data are equal")
    void activitiesWithSameDataAreEqual() {
        List<Commit> commits = List.of(new Commit("sha", "msg", "user", null, null, null));
        Activity activity1 = new Activity(commits, List.of(), List.of(), List.of(), "user", 1, "repo");
        Activity activity2 = new Activity(commits, List.of(), List.of(), List.of(), "user", 1, "repo");

        assertEquals(activity1, activity2);
        assertEquals(activity1.hashCode(), activity2.hashCode());
    }

    // Main method to run tests via JBang
    public static void main(String[] args) {
        var launcher = org.junit.platform.launcher.core.LauncherFactory.create();
        var listener = new org.junit.platform.launcher.listeners.SummaryGeneratingListener();

        launcher.registerTestExecutionListeners(listener);
        launcher.execute(org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request()
            .selectors(org.junit.platform.engine.discovery.DiscoverySelectors.selectClass(ActivityTest.class))
            .build());

        listener.getSummary().printTo(new java.io.PrintWriter(System.out));
        if (listener.getSummary().getTotalFailureCount() > 0) System.exit(1);
    }
}
