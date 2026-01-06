import java.util.HashMap;
import java.util.Map;

/**
 * Mock implementation of DiffPort for testing.
 *
 * Provides stub methods to configure return values and
 * verification methods to check how the port was called.
 */
public class MockDiffPort implements DiffPort {

    private DiffSummary defaultDiffSummary = new DiffSummary(0, 0, 0);
    private Map<Integer, DiffSummary> prDiffSummaries = new HashMap<>();

    // Call tracking
    private int fetchPRDiffCalls = 0;
    private Repository lastRepository;
    private int lastPrNumber;

    // --- Stub methods for test setup ---

    /**
     * Set default diff summary returned for any PR.
     */
    public void stubDiffSummary(DiffSummary summary) {
        this.defaultDiffSummary = summary;
    }

    /**
     * Set diff summary for a specific PR number.
     */
    public void stubDiffSummaryForPR(int prNumber, DiffSummary summary) {
        prDiffSummaries.put(prNumber, summary);
    }

    // --- Port interface implementation ---

    @Override
    public DiffSummary fetchPRDiff(Repository repo, int prNumber) {
        fetchPRDiffCalls++;
        lastRepository = repo;
        lastPrNumber = prNumber;

        return prDiffSummaries.getOrDefault(prNumber, defaultDiffSummary);
    }

    // --- Verification methods ---

    public int getFetchPRDiffCalls() {
        return fetchPRDiffCalls;
    }

    public Repository getLastRepository() {
        return lastRepository;
    }

    public int getLastPrNumber() {
        return lastPrNumber;
    }

    public void reset() {
        defaultDiffSummary = new DiffSummary(0, 0, 0);
        prDiffSummaries.clear();
        fetchPRDiffCalls = 0;
        lastRepository = null;
        lastPrNumber = 0;
    }
}
