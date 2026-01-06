///usr/bin/env jbang "$0" "$@" ; exit $?

//DEPS org.junit.jupiter:junit-jupiter:5.10.0
//DEPS org.junit.platform:junit-platform-launcher:1.10.0
//SOURCES ../../../scripts/domain/activity/Issue.java

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

/**
 * TDD tests for Issue domain entity.
 */
public class IssueTest {

    @Test
    @DisplayName("Issue record stores all fields correctly")
    void issueStoresAllFields() {
        List<String> labels = List.of("bug", "high-priority");
        Issue issue = new Issue(
            123,
            "Fix authentication bug",
            "open",
            "https://github.com/owner/repo/issues/123",
            "owner/repo",
            labels
        );

        assertEquals(123, issue.number());
        assertEquals("Fix authentication bug", issue.title());
        assertEquals("open", issue.state());
        assertEquals("https://github.com/owner/repo/issues/123", issue.url());
        assertEquals("owner/repo", issue.repository());
        assertEquals(labels, issue.labels());
    }

    @Test
    @DisplayName("Issue with empty labels list is valid")
    void issueWithEmptyLabels() {
        Issue issue = new Issue(1, "Title", "open", "url", "repo", List.of());

        assertTrue(issue.labels().isEmpty());
    }

    @Test
    @DisplayName("Issue with null optional fields is valid")
    void issueWithNullOptionalFields() {
        Issue issue = new Issue(1, "Title", "open", null, null, null);

        assertNull(issue.url());
        assertNull(issue.repository());
        assertNull(issue.labels());
    }

    @Test
    @DisplayName("Two issues with same data are equal")
    void issuesWithSameDataAreEqual() {
        List<String> labels = List.of("bug");
        Issue issue1 = new Issue(1, "Title", "open", "url", "repo", labels);
        Issue issue2 = new Issue(1, "Title", "open", "url", "repo", labels);

        assertEquals(issue1, issue2);
        assertEquals(issue1.hashCode(), issue2.hashCode());
    }

    @Test
    @DisplayName("Two issues with different numbers are not equal")
    void issuesWithDifferentNumbersAreNotEqual() {
        Issue issue1 = new Issue(1, "Title", "open", "url", "repo", null);
        Issue issue2 = new Issue(2, "Title", "open", "url", "repo", null);

        assertNotEquals(issue1, issue2);
    }

    @Test
    @DisplayName("Issue state values work correctly")
    void issueStateValues() {
        Issue open = new Issue(1, "T", "open", null, null, null);
        Issue closed = new Issue(2, "T", "closed", null, null, null);

        assertEquals("open", open.state());
        assertEquals("closed", closed.state());
    }

    @Test
    @DisplayName("Issue toString contains useful info")
    void issueToStringContainsInfo() {
        Issue issue = new Issue(42, "Bug report", "open", null, "owner/repo", List.of("bug"));

        String str = issue.toString();
        assertTrue(str.contains("42"), "toString should contain number");
        assertTrue(str.contains("Bug report"), "toString should contain title");
    }

    // Main method to run tests via JBang
    public static void main(String[] args) {
        var launcher = org.junit.platform.launcher.core.LauncherFactory.create();
        var listener = new org.junit.platform.launcher.listeners.SummaryGeneratingListener();

        launcher.registerTestExecutionListeners(listener);
        launcher.execute(org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request()
            .selectors(org.junit.platform.engine.discovery.DiscoverySelectors.selectClass(IssueTest.class))
            .build());

        listener.getSummary().printTo(new java.io.PrintWriter(System.out));
        if (listener.getSummary().getTotalFailureCount() > 0) System.exit(1);
    }
}
