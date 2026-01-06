/**
 * Port interface for AI report generation.
 *
 * This is a pure interface with no implementation - adapters
 * in the infrastructure layer provide concrete implementations.
 */
public interface ReportGeneratorPort {

    /**
     * Generate a report from a prompt.
     *
     * The implementation should stream output directly to stdout
     * for real-time display.
     *
     * @param prompt The full prompt to send to the AI
     */
    void generate(String prompt);
}
