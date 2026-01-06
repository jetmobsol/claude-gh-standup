///usr/bin/env jbang "$0" "$@" ; exit $?

//DEPS com.google.code.gson:gson:2.10.1
//DEPS org.junit.jupiter:junit-jupiter:5.10.0
//DEPS org.junit.platform:junit-platform-launcher:1.10.0

// Domain entities
//SOURCES ../scripts/domain/activity/Commit.java
//SOURCES ../scripts/domain/activity/PullRequest.java
//SOURCES ../scripts/domain/activity/Issue.java
//SOURCES ../scripts/domain/activity/Review.java
//SOURCES ../scripts/domain/activity/Activity.java
//SOURCES ../scripts/domain/shared/DateRange.java
//SOURCES ../scripts/domain/shared/Repository.java
//SOURCES ../scripts/domain/report/DiffSummary.java
//SOURCES ../scripts/domain/report/ReportSection.java
//SOURCES ../scripts/domain/report/StandupReport.java
//SOURCES ../scripts/domain/team/TeamMember.java
//SOURCES ../scripts/domain/team/TeamReport.java

// Ports
//SOURCES ../scripts/ports/ActivityPort.java
//SOURCES ../scripts/ports/DiffPort.java
//SOURCES ../scripts/ports/GitPort.java
//SOURCES ../scripts/ports/ReportGeneratorPort.java
//SOURCES ../scripts/ports/ExportPort.java

// Mocks
//SOURCES mocks/MockActivityPort.java
//SOURCES mocks/MockDiffPort.java
//SOURCES mocks/MockGitPort.java
//SOURCES mocks/MockReportGeneratorPort.java
//SOURCES mocks/MockExportPort.java

// Services
//SOURCES ../scripts/services/ActivityService.java
//SOURCES ../scripts/services/DiffService.java
//SOURCES ../scripts/services/ReportService.java
//SOURCES ../scripts/services/TeamService.java

// Infrastructure
//SOURCES ../scripts/infrastructure/github/GitHubCliAdapter.java
//SOURCES ../scripts/infrastructure/git/GitCliAdapter.java
//SOURCES ../scripts/infrastructure/ai/ClaudeCliAdapter.java
//SOURCES ../scripts/infrastructure/export/MarkdownExporter.java
//SOURCES ../scripts/infrastructure/export/JsonExporter.java
//SOURCES ../scripts/infrastructure/export/HtmlExporter.java

// Test classes
//SOURCES domain/activity/CommitTest.java
//SOURCES domain/activity/PullRequestTest.java
//SOURCES domain/activity/IssueTest.java
//SOURCES domain/activity/ReviewTest.java
//SOURCES domain/activity/ActivityTest.java
//SOURCES domain/shared/DateRangeTest.java
//SOURCES domain/shared/RepositoryTest.java
//SOURCES domain/report/DiffSummaryTest.java
//SOURCES domain/report/StandupReportTest.java
//SOURCES domain/team/TeamReportTest.java
//SOURCES ports/PortsCompilationTest.java
//SOURCES mocks/MocksTest.java
//SOURCES services/ActivityServiceTest.java
//SOURCES services/DiffServiceTest.java
//SOURCES services/ReportServiceTest.java
//SOURCES services/TeamServiceTest.java
//SOURCES infrastructure/InfrastructureCompilationTest.java

import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

import java.io.PrintWriter;

/**
 * Test runner that executes all DDD tests.
 *
 * Run with: jbang test/RunAllTests.java
 */
public class RunAllTests {

    public static void main(String[] args) {
        System.out.println("╔═══════════════════════════════════════════════════════════╗");
        System.out.println("║       claude-gh-standup - DDD Architecture Test Suite      ║");
        System.out.println("╚═══════════════════════════════════════════════════════════╝");
        System.out.println();

        Launcher launcher = LauncherFactory.create();
        SummaryGeneratingListener listener = new SummaryGeneratingListener();
        launcher.registerTestExecutionListeners(listener);

        // Build discovery request with all test classes
        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
            .selectors(
                // Domain - Activity
                selectClass(CommitTest.class),
                selectClass(PullRequestTest.class),
                selectClass(IssueTest.class),
                selectClass(ReviewTest.class),
                selectClass(ActivityTest.class),

                // Domain - Shared
                selectClass(DateRangeTest.class),
                selectClass(RepositoryTest.class),

                // Domain - Report
                selectClass(DiffSummaryTest.class),
                selectClass(StandupReportTest.class),

                // Domain - Team
                selectClass(TeamReportTest.class),

                // Ports
                selectClass(PortsCompilationTest.class),

                // Mocks
                selectClass(MocksTest.class),

                // Services
                selectClass(ActivityServiceTest.class),
                selectClass(DiffServiceTest.class),
                selectClass(ReportServiceTest.class),
                selectClass(TeamServiceTest.class),

                // Infrastructure
                selectClass(InfrastructureCompilationTest.class)
            )
            .build();

        System.out.println("Running tests...");
        System.out.println();

        launcher.execute(request);

        TestExecutionSummary summary = listener.getSummary();

        // Print summary
        System.out.println();
        System.out.println("═══════════════════════════════════════════════════════════════");
        summary.printTo(new PrintWriter(System.out));

        // Print failures if any
        if (summary.getTotalFailureCount() > 0) {
            System.out.println();
            System.out.println("FAILURES:");
            System.out.println("---------");
            for (TestExecutionSummary.Failure failure : summary.getFailures()) {
                System.out.println("• " + failure.getTestIdentifier().getDisplayName());
                System.out.println("  " + failure.getException().getMessage());
                System.out.println();
            }
        }

        // Summary counts
        System.out.println();
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("Tests run:    " + summary.getTestsStartedCount());
        System.out.println("Passed:       " + summary.getTestsSucceededCount());
        System.out.println("Failed:       " + summary.getTestsFailedCount());
        System.out.println("Skipped:      " + summary.getTestsSkippedCount());
        System.out.println("═══════════════════════════════════════════════════════════════");

        if (summary.getTotalFailureCount() > 0) {
            System.out.println();
            System.out.println("❌ BUILD FAILED");
            System.exit(1);
        } else {
            System.out.println();
            System.out.println("✅ BUILD SUCCESS");
            System.exit(0);
        }
    }
}
