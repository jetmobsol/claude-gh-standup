import java.util.List;

/**
 * Aggregate root for GitHub activity.
 *
 * This is the main domain object that contains all activity types
 * for a user over a specified time period.
 *
 * This is a pure domain object with no external dependencies.
 * JSON parsing happens in the infrastructure layer (adapters).
 *
 * @param commits      List of commits (immutable)
 * @param pullRequests List of pull requests (immutable)
 * @param issues       List of issues (immutable)
 * @param reviews      List of code reviews (immutable)
 * @param username     The GitHub username (required)
 * @param days         Number of days of activity (positive)
 * @param repository   Optional repository filter ("owner/repo" format)
 */
public record Activity(
    List<Commit> commits,
    List<PullRequest> pullRequests,
    List<Issue> issues,
    List<Review> reviews,
    String username,
    int days,
    String repository
) {
    /**
     * Compact constructor for validation and immutability.
     */
    public Activity {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username is required");
        }
        if (days <= 0) {
            throw new IllegalArgumentException("Days must be positive");
        }
        // Make all lists immutable
        commits = commits == null ? List.of() : List.copyOf(commits);
        pullRequests = pullRequests == null ? List.of() : List.copyOf(pullRequests);
        issues = issues == null ? List.of() : List.copyOf(issues);
        reviews = reviews == null ? List.of() : List.copyOf(reviews);
    }

    /**
     * Total count of all activity items.
     */
    public int totalCount() {
        return commits.size() + pullRequests.size() + issues.size() + reviews.size();
    }

    /**
     * Check if there's any activity.
     */
    public boolean hasActivity() {
        return totalCount() > 0;
    }

    /**
     * Get count of open pull requests.
     */
    public long openPullRequestCount() {
        return pullRequests.stream().filter(PullRequest::isOpen).count();
    }

    /**
     * Get count of merged pull requests.
     */
    public long mergedPullRequestCount() {
        return pullRequests.stream().filter(PullRequest::isMerged).count();
    }

    /**
     * Get count of open issues.
     */
    public long openIssueCount() {
        return issues.stream().filter(Issue::isOpen).count();
    }

    /**
     * Get count of approved reviews given.
     */
    public long approvedReviewCount() {
        return reviews.stream().filter(Review::isApproved).count();
    }
}
