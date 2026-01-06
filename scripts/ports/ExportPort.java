/**
 * Port interface for exporting reports to various formats.
 *
 * This is a pure interface with no implementation - adapters
 * in the infrastructure layer provide concrete implementations.
 */
public interface ExportPort {

    /**
     * Export a standup report to a specific format.
     *
     * @param report The standup report to export
     * @return Formatted string representation
     */
    String export(StandupReport report);
}
