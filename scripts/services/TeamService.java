import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for aggregating team activity reports.
 *
 * Orchestrates activity collection for multiple team members
 * and produces a consolidated team report.
 */
public class TeamService {

    private final ActivityService activityService;

    public TeamService(ActivityService activityService) {
        this.activityService = activityService;
    }

    /**
     * Aggregate activity for multiple team members.
     *
     * @param usernames List of GitHub usernames
     * @param range     Date range to query
     * @param repo      Optional repository filter (null for all repos)
     * @return TeamReport with all members' activity
     */
    public TeamReport aggregate(List<String> usernames, DateRange range, Repository repo) {
        List<TeamMember> members = new ArrayList<>();

        for (String username : usernames) {
            Activity activity = activityService.collect(username, range, repo);
            members.add(new TeamMember(username, activity));
        }

        return new TeamReport(members, (int) range.days(), Instant.now());
    }
}
