/**
 * Port interface for fetching PR diff information.
 *
 * This is a pure interface with no implementation - adapters
 * in the infrastructure layer provide concrete implementations.
 */
public interface DiffPort {

    /**
     * Fetch diff summary for a pull request.
     *
     * @param repo     Repository containing the PR
     * @param prNumber Pull request number
     * @return DiffSummary with file change statistics
     */
    DiffSummary fetchPRDiff(Repository repo, int prNumber);
}
