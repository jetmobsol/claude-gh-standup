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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

/**
 * Compilation test to verify all ports work with domain entities.
 */
public class PortsCompilationTest {

    @Test
    @DisplayName("ActivityPort interface compiles with domain entities")
    void activityPortCompiles() {
        // This test verifies compilation - if it runs, the interface compiles
        assertNotNull(ActivityPort.class);
    }

    @Test
    @DisplayName("DiffPort interface compiles with domain entities")
    void diffPortCompiles() {
        assertNotNull(DiffPort.class);
    }

    @Test
    @DisplayName("GitPort interface compiles with domain entities")
    void gitPortCompiles() {
        assertNotNull(GitPort.class);
    }

    @Test
    @DisplayName("ReportGeneratorPort interface compiles")
    void reportGeneratorPortCompiles() {
        assertNotNull(ReportGeneratorPort.class);
    }

    @Test
    @DisplayName("ExportPort interface compiles with domain entities")
    void exportPortCompiles() {
        assertNotNull(ExportPort.class);
    }

    @Test
    @DisplayName("Mock implementation of ActivityPort can be created")
    void mockActivityPortCanBeCreated() {
        ActivityPort mock = new ActivityPort() {
            @Override
            public List<Commit> fetchCommits(String username, DateRange range, Repository repo) {
                return List.of();
            }

            @Override
            public List<PullRequest> fetchPullRequests(String username, DateRange range, Repository repo) {
                return List.of();
            }

            @Override
            public List<Issue> fetchIssues(String username, DateRange range, Repository repo) {
                return List.of();
            }

            @Override
            public List<Review> fetchReviews(String username, DateRange range, Repository repo) {
                return List.of();
            }
        };

        assertNotNull(mock);
        assertEquals(0, mock.fetchCommits("user", DateRange.lastDays(1), null).size());
    }

    // Main method to run tests via JBang
    public static void main(String[] args) {
        var launcher = org.junit.platform.launcher.core.LauncherFactory.create();
        var listener = new org.junit.platform.launcher.listeners.SummaryGeneratingListener();

        launcher.registerTestExecutionListeners(listener);
        launcher.execute(org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request()
            .selectors(org.junit.platform.engine.discovery.DiscoverySelectors.selectClass(PortsCompilationTest.class))
            .build());

        listener.getSummary().printTo(new java.io.PrintWriter(System.out));
        if (listener.getSummary().getTotalFailureCount() > 0) System.exit(1);
    }
}
