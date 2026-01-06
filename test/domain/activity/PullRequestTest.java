///usr/bin/env jbang "$0" "$@" ; exit $?

//DEPS org.junit.jupiter:junit-jupiter:5.10.0
//DEPS org.junit.platform:junit-platform-launcher:1.10.0
//SOURCES ../../../scripts/domain/activity/PullRequest.java

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * TDD tests for PullRequest domain entity.
 */
public class PullRequestTest {

    @Test
    @DisplayName("PullRequest record stores all fields correctly")
    void pullRequestStoresAllFields() {
        PullRequest pr = new PullRequest(
            42,
            "Add new feature",
            "open",
            "https://github.com/owner/repo/pull/42",
            "owner/repo",
            150,
            30
        );

        assertEquals(42, pr.number());
        assertEquals("Add new feature", pr.title());
        assertEquals("open", pr.state());
        assertEquals("https://github.com/owner/repo/pull/42", pr.url());
        assertEquals("owner/repo", pr.repository());
        assertEquals(150, pr.additions());
        assertEquals(30, pr.deletions());
    }

    @Test
    @DisplayName("PullRequest with zero additions/deletions is valid")
    void pullRequestWithZeroChanges() {
        PullRequest pr = new PullRequest(1, "Docs update", "merged", "url", "repo", 0, 0);

        assertEquals(0, pr.additions());
        assertEquals(0, pr.deletions());
    }

    @Test
    @DisplayName("PullRequest with null optional fields is valid")
    void pullRequestWithNullOptionalFields() {
        PullRequest pr = new PullRequest(
            1,
            "Title",
            "open",
            null,  // url optional
            null,  // repository optional
            0,
            0
        );

        assertNull(pr.url());
        assertNull(pr.repository());
    }

    @Test
    @DisplayName("Two PRs with same data are equal")
    void pullRequestsWithSameDataAreEqual() {
        PullRequest pr1 = new PullRequest(1, "Title", "open", "url", "repo", 10, 5);
        PullRequest pr2 = new PullRequest(1, "Title", "open", "url", "repo", 10, 5);

        assertEquals(pr1, pr2);
        assertEquals(pr1.hashCode(), pr2.hashCode());
    }

    @Test
    @DisplayName("Two PRs with different numbers are not equal")
    void pullRequestsWithDifferentNumbersAreNotEqual() {
        PullRequest pr1 = new PullRequest(1, "Title", "open", "url", "repo", 10, 5);
        PullRequest pr2 = new PullRequest(2, "Title", "open", "url", "repo", 10, 5);

        assertNotEquals(pr1, pr2);
    }

    @Test
    @DisplayName("PullRequest state values are valid strings")
    void pullRequestStateValues() {
        // Common GitHub PR states
        PullRequest open = new PullRequest(1, "T", "open", null, null, 0, 0);
        PullRequest closed = new PullRequest(2, "T", "closed", null, null, 0, 0);
        PullRequest merged = new PullRequest(3, "T", "merged", null, null, 0, 0);

        assertEquals("open", open.state());
        assertEquals("closed", closed.state());
        assertEquals("merged", merged.state());
    }

    @Test
    @DisplayName("PullRequest toString contains useful info")
    void pullRequestToStringContainsInfo() {
        PullRequest pr = new PullRequest(42, "Fix bug", "open", null, "owner/repo", 10, 5);

        String str = pr.toString();
        assertTrue(str.contains("42"), "toString should contain number");
        assertTrue(str.contains("Fix bug"), "toString should contain title");
    }

    // Main method to run tests via JBang
    public static void main(String[] args) {
        var launcher = org.junit.platform.launcher.core.LauncherFactory.create();
        var listener = new org.junit.platform.launcher.listeners.SummaryGeneratingListener();

        launcher.registerTestExecutionListeners(listener);
        launcher.execute(org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request()
            .selectors(org.junit.platform.engine.discovery.DiscoverySelectors.selectClass(PullRequestTest.class))
            .build());

        listener.getSummary().printTo(new java.io.PrintWriter(System.out));
        if (listener.getSummary().getTotalFailureCount() > 0) System.exit(1);
    }
}
