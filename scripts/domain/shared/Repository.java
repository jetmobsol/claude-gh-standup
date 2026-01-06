/**
 * Value object representing a GitHub repository (owner/repo).
 *
 * This is a pure domain object with no external dependencies.
 *
 * @param owner The repository owner (user or organization, required)
 * @param name  The repository name (required)
 */
public record Repository(String owner, String name) {

    /**
     * Compact constructor for validation.
     */
    public Repository {
        if (owner == null || owner.isBlank()) {
            throw new IllegalArgumentException("Owner is required");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Repository name is required");
        }
    }

    /**
     * Parse a repository from "owner/repo" format.
     *
     * @param fullName The full repository name (e.g., "octocat/hello-world")
     * @return Repository value object
     * @throws IllegalArgumentException if format is invalid
     */
    public static Repository parse(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("Repository name is required");
        }

        int slashIndex = fullName.indexOf('/');
        if (slashIndex <= 0 || slashIndex >= fullName.length() - 1) {
            throw new IllegalArgumentException("Invalid repository format. Expected 'owner/repo', got: " + fullName);
        }

        String owner = fullName.substring(0, slashIndex);
        String name = fullName.substring(slashIndex + 1);

        return new Repository(owner, name);
    }

    /**
     * Returns the repository in "owner/repo" format.
     */
    @Override
    public String toString() {
        return owner + "/" + name;
    }

    /**
     * Get the GitHub URL for this repository.
     */
    public String url() {
        return "https://github.com/" + owner + "/" + name;
    }

    /**
     * Get the GitHub API URL for this repository.
     */
    public String apiUrl() {
        return "https://api.github.com/repos/" + owner + "/" + name;
    }
}
