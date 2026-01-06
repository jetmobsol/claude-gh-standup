///usr/bin/env jbang "$0" "$@" ; exit $?

//DEPS org.junit.jupiter:junit-jupiter:5.10.0
//DEPS org.junit.platform:junit-platform-launcher:1.10.0
//SOURCES ../../../scripts/domain/shared/Repository.java

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * TDD tests for Repository value object.
 */
public class RepositoryTest {

    @Test
    @DisplayName("Repository stores owner and name")
    void repositoryStoresOwnerAndName() {
        Repository repo = new Repository("octocat", "hello-world");

        assertEquals("octocat", repo.owner());
        assertEquals("hello-world", repo.name());
    }

    @Test
    @DisplayName("Repository.parse parses owner/repo format")
    void parseOwnerRepoFormat() {
        Repository repo = Repository.parse("octocat/hello-world");

        assertEquals("octocat", repo.owner());
        assertEquals("hello-world", repo.name());
    }

    @Test
    @DisplayName("Repository.parse handles organization repos")
    void parseOrganizationRepo() {
        Repository repo = Repository.parse("anthropics/claude-code");

        assertEquals("anthropics", repo.owner());
        assertEquals("claude-code", repo.name());
    }

    @Test
    @DisplayName("Repository.parse handles repos with dots and hyphens")
    void parseRepoWithSpecialChars() {
        Repository repo = Repository.parse("owner/my-repo.js");

        assertEquals("owner", repo.owner());
        assertEquals("my-repo.js", repo.name());
    }

    @Test
    @DisplayName("Repository.toString returns owner/repo format")
    void toStringReturnsOwnerRepo() {
        Repository repo = new Repository("octocat", "hello-world");

        assertEquals("octocat/hello-world", repo.toString());
    }

    @Test
    @DisplayName("Repository.parse and toString are symmetric")
    void parseAndToStringAreSymmetric() {
        String original = "anthropics/claude-code";
        Repository repo = Repository.parse(original);

        assertEquals(original, repo.toString());
    }

    @Test
    @DisplayName("Two repositories with same owner/name are equal")
    void repositoriesWithSameDataAreEqual() {
        Repository repo1 = new Repository("owner", "repo");
        Repository repo2 = new Repository("owner", "repo");

        assertEquals(repo1, repo2);
        assertEquals(repo1.hashCode(), repo2.hashCode());
    }

    @Test
    @DisplayName("Repository.parse rejects invalid format")
    void parseRejectsInvalidFormat() {
        assertThrows(IllegalArgumentException.class, () -> Repository.parse("invalid"));
        assertThrows(IllegalArgumentException.class, () -> Repository.parse(""));
        assertThrows(IllegalArgumentException.class, () -> Repository.parse(null));
        assertThrows(IllegalArgumentException.class, () -> Repository.parse("no-slash-here"));
    }

    @Test
    @DisplayName("Repository.parse rejects empty owner or name")
    void parseRejectsEmptyParts() {
        assertThrows(IllegalArgumentException.class, () -> Repository.parse("/repo"));
        assertThrows(IllegalArgumentException.class, () -> Repository.parse("owner/"));
        assertThrows(IllegalArgumentException.class, () -> Repository.parse("/"));
    }

    @Test
    @DisplayName("Repository constructor rejects null or blank values")
    void constructorRejectsInvalid() {
        assertThrows(IllegalArgumentException.class, () -> new Repository(null, "repo"));
        assertThrows(IllegalArgumentException.class, () -> new Repository("owner", null));
        assertThrows(IllegalArgumentException.class, () -> new Repository("", "repo"));
        assertThrows(IllegalArgumentException.class, () -> new Repository("owner", ""));
        assertThrows(IllegalArgumentException.class, () -> new Repository("  ", "repo"));
    }

    @Test
    @DisplayName("Repository provides GitHub URL")
    void repositoryProvidesGitHubUrl() {
        Repository repo = new Repository("octocat", "hello-world");

        assertEquals("https://github.com/octocat/hello-world", repo.url());
    }

    // Main method to run tests via JBang
    public static void main(String[] args) {
        var launcher = org.junit.platform.launcher.core.LauncherFactory.create();
        var listener = new org.junit.platform.launcher.listeners.SummaryGeneratingListener();

        launcher.registerTestExecutionListeners(listener);
        launcher.execute(org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request()
            .selectors(org.junit.platform.engine.discovery.DiscoverySelectors.selectClass(RepositoryTest.class))
            .build());

        listener.getSummary().printTo(new java.io.PrintWriter(System.out));
        if (listener.getSummary().getTotalFailureCount() > 0) System.exit(1);
    }
}
