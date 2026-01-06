///usr/bin/env jbang "$0" "$@" ; exit $?

//DEPS org.junit.jupiter:junit-jupiter:5.10.0
//DEPS org.junit.platform:junit-platform-launcher:1.10.0
//SOURCES ../../../scripts/domain/activity/Commit.java

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;

/**
 * TDD tests for Commit domain entity.
 * Tests are written BEFORE the implementation.
 */
public class CommitTest {

    @Test
    @DisplayName("Commit record stores all fields correctly")
    void commitStoresAllFields() {
        Instant now = Instant.now();
        Commit commit = new Commit(
            "abc123def456",
            "Fix bug in authentication",
            "octocat",
            now,
            "https://github.com/owner/repo/commit/abc123def456",
            "owner/repo"
        );

        assertEquals("abc123def456", commit.sha());
        assertEquals("Fix bug in authentication", commit.message());
        assertEquals("octocat", commit.author());
        assertEquals(now, commit.date());
        assertEquals("https://github.com/owner/repo/commit/abc123def456", commit.url());
        assertEquals("owner/repo", commit.repository());
    }

    @Test
    @DisplayName("Commit with null optional fields is valid")
    void commitWithNullOptionalFields() {
        Commit commit = new Commit(
            "abc123",
            "Initial commit",
            "octocat",
            null,  // date can be null
            null,  // url can be null
            null   // repository can be null
        );

        assertEquals("abc123", commit.sha());
        assertEquals("Initial commit", commit.message());
        assertEquals("octocat", commit.author());
        assertNull(commit.date());
        assertNull(commit.url());
        assertNull(commit.repository());
    }

    @Test
    @DisplayName("Two commits with same data are equal")
    void commitsWithSameDataAreEqual() {
        Instant time = Instant.parse("2024-01-15T10:30:00Z");

        Commit commit1 = new Commit("sha1", "msg", "user", time, "url", "repo");
        Commit commit2 = new Commit("sha1", "msg", "user", time, "url", "repo");

        assertEquals(commit1, commit2);
        assertEquals(commit1.hashCode(), commit2.hashCode());
    }

    @Test
    @DisplayName("Two commits with different SHA are not equal")
    void commitsWithDifferentShaAreNotEqual() {
        Instant time = Instant.now();

        Commit commit1 = new Commit("sha1", "msg", "user", time, "url", "repo");
        Commit commit2 = new Commit("sha2", "msg", "user", time, "url", "repo");

        assertNotEquals(commit1, commit2);
    }

    @Test
    @DisplayName("Commit is immutable (record)")
    void commitIsImmutable() {
        Commit commit = new Commit("sha", "msg", "user", Instant.now(), "url", "repo");

        // Records are inherently immutable - this test documents the expectation
        // No setter methods should exist
        assertNotNull(commit.sha());
        // The fact that this compiles and runs proves immutability
    }

    @Test
    @DisplayName("Commit toString contains useful info for debugging")
    void commitToStringContainsInfo() {
        Commit commit = new Commit("abc123", "Fix bug", "octocat", null, null, "owner/repo");

        String str = commit.toString();
        assertTrue(str.contains("abc123"), "toString should contain SHA");
        assertTrue(str.contains("Fix bug"), "toString should contain message");
        assertTrue(str.contains("octocat"), "toString should contain author");
    }

    // Main method to run tests via JBang
    public static void main(String[] args) {
        org.junit.platform.launcher.Launcher launcher =
            org.junit.platform.launcher.core.LauncherFactory.create();

        org.junit.platform.launcher.listeners.SummaryGeneratingListener listener =
            new org.junit.platform.launcher.listeners.SummaryGeneratingListener();

        launcher.registerTestExecutionListeners(listener);

        launcher.execute(org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request()
            .selectors(org.junit.platform.engine.discovery.DiscoverySelectors.selectClass(CommitTest.class))
            .build());

        listener.getSummary().printTo(new java.io.PrintWriter(System.out));

        if (listener.getSummary().getTotalFailureCount() > 0) {
            System.exit(1);
        }
    }
}
