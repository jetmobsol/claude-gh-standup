import java.util.List;

/**
 * Service for collecting GitHub activity using ports.
 *
 * Orchestrates calls to ActivityPort to gather commits, PRs, issues, and reviews.
 */
public class ActivityService {

    private final ActivityPort activityPort;

    public ActivityService(ActivityPort activityPort) {
        this.activityPort = activityPort;
    }

    /**
     * Collect all activity for a user within a date range.
     *
     * @param username GitHub username
     * @param range    Date range to query
     * @param repo     Optional repository filter (null for all repos)
     * @return Activity aggregate with all collected data
     */
    public Activity collect(String username, DateRange range, Repository repo) {
        List<Commit> commits = activityPort.fetchCommits(username, range, repo);
        List<PullRequest> prs = activityPort.fetchPullRequests(username, range, repo);
        List<Issue> issues = activityPort.fetchIssues(username, range, repo);
        List<Review> reviews = activityPort.fetchReviews(username, range, repo);

        String repoString = repo != null ? repo.toString() : null;
        return new Activity(commits, prs, issues, reviews, username, (int) range.days(), repoString);
    }
}
