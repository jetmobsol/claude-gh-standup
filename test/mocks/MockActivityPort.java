import java.util.ArrayList;
import java.util.List;

/**
 * Mock implementation of ActivityPort for testing.
 *
 * Provides stub methods to configure return values and
 * verification methods to check how the port was called.
 */
public class MockActivityPort implements ActivityPort {

    private List<Commit> stubbedCommits = new ArrayList<>();
    private List<PullRequest> stubbedPullRequests = new ArrayList<>();
    private List<Issue> stubbedIssues = new ArrayList<>();
    private List<Review> stubbedReviews = new ArrayList<>();

    // Call tracking
    private int fetchCommitsCalls = 0;
    private int fetchPullRequestsCalls = 0;
    private int fetchIssuesCalls = 0;
    private int fetchReviewsCalls = 0;
    private String lastUsername;
    private DateRange lastDateRange;
    private Repository lastRepository;

    // --- Stub methods for test setup ---

    public void stubCommits(List<Commit> commits) {
        this.stubbedCommits = new ArrayList<>(commits);
    }

    public void stubPullRequests(List<PullRequest> prs) {
        this.stubbedPullRequests = new ArrayList<>(prs);
    }

    public void stubIssues(List<Issue> issues) {
        this.stubbedIssues = new ArrayList<>(issues);
    }

    public void stubReviews(List<Review> reviews) {
        this.stubbedReviews = new ArrayList<>(reviews);
    }

    // --- Port interface implementation ---

    @Override
    public List<Commit> fetchCommits(String username, DateRange range, Repository repo) {
        fetchCommitsCalls++;
        lastUsername = username;
        lastDateRange = range;
        lastRepository = repo;
        return stubbedCommits;
    }

    @Override
    public List<PullRequest> fetchPullRequests(String username, DateRange range, Repository repo) {
        fetchPullRequestsCalls++;
        lastUsername = username;
        lastDateRange = range;
        lastRepository = repo;
        return stubbedPullRequests;
    }

    @Override
    public List<Issue> fetchIssues(String username, DateRange range, Repository repo) {
        fetchIssuesCalls++;
        lastUsername = username;
        lastDateRange = range;
        lastRepository = repo;
        return stubbedIssues;
    }

    @Override
    public List<Review> fetchReviews(String username, DateRange range, Repository repo) {
        fetchReviewsCalls++;
        lastUsername = username;
        lastDateRange = range;
        lastRepository = repo;
        return stubbedReviews;
    }

    // --- Verification methods ---

    public int getFetchCommitsCalls() {
        return fetchCommitsCalls;
    }

    public int getFetchPullRequestsCalls() {
        return fetchPullRequestsCalls;
    }

    public int getFetchIssuesCalls() {
        return fetchIssuesCalls;
    }

    public int getFetchReviewsCalls() {
        return fetchReviewsCalls;
    }

    public String getLastUsername() {
        return lastUsername;
    }

    public DateRange getLastDateRange() {
        return lastDateRange;
    }

    public Repository getLastRepository() {
        return lastRepository;
    }

    public void reset() {
        stubbedCommits.clear();
        stubbedPullRequests.clear();
        stubbedIssues.clear();
        stubbedReviews.clear();
        fetchCommitsCalls = 0;
        fetchPullRequestsCalls = 0;
        fetchIssuesCalls = 0;
        fetchReviewsCalls = 0;
        lastUsername = null;
        lastDateRange = null;
        lastRepository = null;
    }
}
