import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Mock implementation of GitPort for testing.
 *
 * Provides stub methods to configure return values and
 * verification methods to check how the port was called.
 */
public class MockGitPort implements GitPort {

    private String stubbedBranch = "main";
    private List<String> stubbedStagedFiles = new ArrayList<>();
    private List<String> stubbedUnstagedFiles = new ArrayList<>();
    private List<String> stubbedUnpushedCommits = new ArrayList<>();
    private Repository stubbedRepository = null;

    // Call tracking
    private int getCurrentBranchCalls = 0;
    private int getStagedFilesCalls = 0;
    private int getUnstagedFilesCalls = 0;
    private int getUnpushedCommitsCalls = 0;
    private int detectRepositoryCalls = 0;
    private Path lastRepoPath;
    private String lastBranch;

    // --- Stub methods for test setup ---

    public void stubCurrentBranch(String branch) {
        this.stubbedBranch = branch;
    }

    public void stubStagedFiles(List<String> files) {
        this.stubbedStagedFiles = new ArrayList<>(files);
    }

    public void stubUnstagedFiles(List<String> files) {
        this.stubbedUnstagedFiles = new ArrayList<>(files);
    }

    public void stubUnpushedCommits(List<String> commits) {
        this.stubbedUnpushedCommits = new ArrayList<>(commits);
    }

    public void stubRepository(Repository repo) {
        this.stubbedRepository = repo;
    }

    // --- Port interface implementation ---

    @Override
    public String getCurrentBranch(Path repoPath) {
        getCurrentBranchCalls++;
        lastRepoPath = repoPath;
        return stubbedBranch;
    }

    @Override
    public List<String> getStagedFiles(Path repoPath) {
        getStagedFilesCalls++;
        lastRepoPath = repoPath;
        return stubbedStagedFiles;
    }

    @Override
    public List<String> getUnstagedFiles(Path repoPath) {
        getUnstagedFilesCalls++;
        lastRepoPath = repoPath;
        return stubbedUnstagedFiles;
    }

    @Override
    public List<String> getUnpushedCommits(Path repoPath, String branch) {
        getUnpushedCommitsCalls++;
        lastRepoPath = repoPath;
        lastBranch = branch;
        return stubbedUnpushedCommits;
    }

    @Override
    public Optional<Repository> detectRepository(Path repoPath) {
        detectRepositoryCalls++;
        lastRepoPath = repoPath;
        return Optional.ofNullable(stubbedRepository);
    }

    // --- Verification methods ---

    public int getGetCurrentBranchCalls() {
        return getCurrentBranchCalls;
    }

    public int getGetStagedFilesCalls() {
        return getStagedFilesCalls;
    }

    public int getGetUnstagedFilesCalls() {
        return getUnstagedFilesCalls;
    }

    public int getGetUnpushedCommitsCalls() {
        return getUnpushedCommitsCalls;
    }

    public int getDetectRepositoryCalls() {
        return detectRepositoryCalls;
    }

    public Path getLastRepoPath() {
        return lastRepoPath;
    }

    public String getLastBranch() {
        return lastBranch;
    }

    public void reset() {
        stubbedBranch = "main";
        stubbedStagedFiles.clear();
        stubbedUnstagedFiles.clear();
        stubbedUnpushedCommits.clear();
        stubbedRepository = null;
        getCurrentBranchCalls = 0;
        getStagedFilesCalls = 0;
        getUnstagedFilesCalls = 0;
        getUnpushedCommitsCalls = 0;
        detectRepositoryCalls = 0;
        lastRepoPath = null;
        lastBranch = null;
    }
}
