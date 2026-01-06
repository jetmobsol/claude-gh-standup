import java.util.List;

/**
 * Service for analyzing PR diffs using ports.
 *
 * Orchestrates calls to DiffPort to gather file change statistics
 * for pull requests.
 */
public class DiffService {

    private final DiffPort diffPort;

    public DiffService(DiffPort diffPort) {
        this.diffPort = diffPort;
    }

    /**
     * Analyze diffs for a list of pull requests.
     *
     * @param prs  List of pull requests to analyze
     * @param repo Repository containing the PRs
     * @return Aggregated DiffSummary with totals
     */
    public DiffSummary analyze(List<PullRequest> prs, Repository repo) {
        if (prs == null || prs.isEmpty()) {
            return new DiffSummary(0, 0, 0);
        }

        int totalFiles = 0;
        int totalAdditions = 0;
        int totalDeletions = 0;

        for (PullRequest pr : prs) {
            DiffSummary diff = diffPort.fetchPRDiff(repo, pr.number());
            totalFiles += diff.filesChanged();
            totalAdditions += diff.additions();
            totalDeletions += diff.deletions();
        }

        return new DiffSummary(totalFiles, totalAdditions, totalDeletions);
    }
}
