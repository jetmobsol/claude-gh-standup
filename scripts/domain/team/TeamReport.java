import java.time.Instant;
import java.util.List;

/**
 * Aggregate root representing a team standup report.
 *
 * This is a pure domain object with no external dependencies.
 *
 * @param members     List of team members with their activity (immutable)
 * @param days        Number of days of activity covered
 * @param generatedAt When the report was generated
 */
public record TeamReport(
    List<TeamMember> members,
    int days,
    Instant generatedAt
) {
    /**
     * Compact constructor for validation and immutability.
     */
    public TeamReport {
        if (days <= 0) {
            throw new IllegalArgumentException("Days must be positive");
        }
        if (generatedAt == null) {
            throw new IllegalArgumentException("Generated timestamp is required");
        }
        // Make members immutable
        members = members == null ? List.of() : List.copyOf(members);
    }

    /**
     * Total count of all activity across team.
     */
    public int totalActivityCount() {
        return members.stream()
            .mapToInt(TeamMember::totalActivityCount)
            .sum();
    }

    /**
     * Total commits across team.
     */
    public int totalCommits() {
        return members.stream()
            .filter(TeamMember::hasActivity)
            .mapToInt(m -> m.activity().commits().size())
            .sum();
    }

    /**
     * Total pull requests across team.
     */
    public int totalPullRequests() {
        return members.stream()
            .filter(TeamMember::hasActivity)
            .mapToInt(m -> m.activity().pullRequests().size())
            .sum();
    }

    /**
     * Total issues across team.
     */
    public int totalIssues() {
        return members.stream()
            .filter(TeamMember::hasActivity)
            .mapToInt(m -> m.activity().issues().size())
            .sum();
    }

    /**
     * Number of team members.
     */
    public int memberCount() {
        return members.size();
    }

    /**
     * Number of team members with activity.
     */
    public long activeMemberCount() {
        return members.stream().filter(m -> m.totalActivityCount() > 0).count();
    }
}
