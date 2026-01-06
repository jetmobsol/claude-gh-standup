/**
 * Entity representing a team member with their GitHub activity.
 *
 * This is a pure domain object with no external dependencies.
 *
 * @param username The GitHub username (required)
 * @param activity The member's activity (can be null if not yet collected)
 */
public record TeamMember(String username, Activity activity) {

    /**
     * Compact constructor for validation.
     */
    public TeamMember {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username is required");
        }
    }

    /**
     * Create a team member without activity (placeholder).
     */
    public static TeamMember pending(String username) {
        return new TeamMember(username, null);
    }

    /**
     * Check if activity has been collected for this member.
     */
    public boolean hasActivity() {
        return activity != null;
    }

    /**
     * Get total activity count (0 if no activity).
     */
    public int totalActivityCount() {
        return activity != null ? activity.totalCount() : 0;
    }
}
