import java.util.List;

/**
 * Port interface for fetching GitHub activity data.
 *
 * This is a pure interface with no implementation - adapters
 * in the infrastructure layer provide concrete implementations.
 */
public interface ActivityPort {

    /**
     * Fetch commits for a user within a date range.
     *
     * @param username GitHub username
     * @param range    Date range to query
     * @param repo     Optional repository filter (null for all repos)
     * @return List of commits, empty if none found
     */
    List<Commit> fetchCommits(String username, DateRange range, Repository repo);

    /**
     * Fetch pull requests for a user within a date range.
     *
     * @param username GitHub username
     * @param range    Date range to query
     * @param repo     Optional repository filter (null for all repos)
     * @return List of pull requests, empty if none found
     */
    List<PullRequest> fetchPullRequests(String username, DateRange range, Repository repo);

    /**
     * Fetch issues for a user within a date range.
     *
     * @param username GitHub username
     * @param range    Date range to query
     * @param repo     Optional repository filter (null for all repos)
     * @return List of issues, empty if none found
     */
    List<Issue> fetchIssues(String username, DateRange range, Repository repo);

    /**
     * Fetch code reviews for a user within a date range.
     *
     * @param username GitHub username
     * @param range    Date range to query
     * @param repo     Optional repository filter (null for all repos)
     * @return List of reviews, empty if none found
     */
    List<Review> fetchReviews(String username, DateRange range, Repository repo);
}
