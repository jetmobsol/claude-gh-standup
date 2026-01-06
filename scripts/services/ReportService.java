/**
 * Service for generating standup reports using AI.
 *
 * Orchestrates the report generation by building prompts from
 * activity data and diff summaries, then calling the AI generator.
 */
public class ReportService {

    private final ReportGeneratorPort generatorPort;

    public ReportService(ReportGeneratorPort generatorPort) {
        this.generatorPort = generatorPort;
    }

    /**
     * Generate a standup report from activity and diff data.
     *
     * @param activity Activity data for the report
     * @param diffs    Diff summary for file changes
     */
    public void generate(Activity activity, DiffSummary diffs) {
        String prompt = buildPrompt(activity, diffs);
        generatorPort.generate(prompt);
    }

    /**
     * Build the prompt for AI generation.
     */
    private String buildPrompt(Activity activity, DiffSummary diffs) {
        StringBuilder sb = new StringBuilder();

        sb.append("Generate a standup report for user: ").append(activity.username()).append("\n\n");

        // Commits section
        if (!activity.commits().isEmpty()) {
            sb.append("## Commits\n");
            for (Commit commit : activity.commits()) {
                sb.append("- ").append(commit.message()).append(" (").append(commit.sha().substring(0, Math.min(7, commit.sha().length()))).append(")\n");
            }
            sb.append("\n");
        }

        // Pull Requests section
        if (!activity.pullRequests().isEmpty()) {
            sb.append("## Pull Requests\n");
            for (PullRequest pr : activity.pullRequests()) {
                sb.append("- #").append(pr.number()).append(": ").append(pr.title()).append(" [").append(pr.state()).append("]\n");
            }
            sb.append("\n");
        }

        // Issues section
        if (!activity.issues().isEmpty()) {
            sb.append("## Issues\n");
            for (Issue issue : activity.issues()) {
                sb.append("- #").append(issue.number()).append(": ").append(issue.title()).append(" [").append(issue.state()).append("]\n");
            }
            sb.append("\n");
        }

        // Reviews section
        if (!activity.reviews().isEmpty()) {
            sb.append("## Code Reviews\n");
            for (Review review : activity.reviews()) {
                sb.append("- PR #").append(review.prNumber()).append(": ").append(review.state());
                if (review.body() != null && !review.body().isBlank()) {
                    sb.append(" - ").append(review.body());
                }
                sb.append("\n");
            }
            sb.append("\n");
        }

        // Diff summary
        if (diffs.filesChanged() > 0) {
            sb.append("## File Changes\n");
            sb.append("- Files changed: ").append(diffs.filesChanged()).append("\n");
            sb.append("- Additions: ").append(diffs.additions()).append("\n");
            sb.append("- Deletions: ").append(diffs.deletions()).append("\n");
        }

        return sb.toString();
    }
}
