import java.util.ArrayList;
import java.util.List;

/**
 * Mock implementation of ExportPort for testing.
 *
 * Returns stubbed export strings and captures reports
 * passed to export() for verification.
 */
public class MockExportPort implements ExportPort {

    private String stubbedExport = "";
    private List<StandupReport> capturedReports = new ArrayList<>();
    private int exportCalls = 0;

    // --- Stub methods for test setup ---

    public void stubExport(String exportContent) {
        this.stubbedExport = exportContent;
    }

    // --- Port interface implementation ---

    @Override
    public String export(StandupReport report) {
        exportCalls++;
        capturedReports.add(report);
        return stubbedExport;
    }

    // --- Verification methods ---

    public int getExportCalls() {
        return exportCalls;
    }

    public List<StandupReport> getCapturedReports() {
        return new ArrayList<>(capturedReports);
    }

    public StandupReport getLastReport() {
        if (capturedReports.isEmpty()) {
            return null;
        }
        return capturedReports.get(capturedReports.size() - 1);
    }

    public void reset() {
        stubbedExport = "";
        capturedReports.clear();
        exportCalls = 0;
    }
}
