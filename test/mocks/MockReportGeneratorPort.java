import java.util.ArrayList;
import java.util.List;

/**
 * Mock implementation of ReportGeneratorPort for testing.
 *
 * Captures prompts passed to generate() for verification
 * rather than actually calling an AI service.
 */
public class MockReportGeneratorPort implements ReportGeneratorPort {

    private List<String> capturedPrompts = new ArrayList<>();
    private int generateCalls = 0;

    // --- Port interface implementation ---

    @Override
    public void generate(String prompt) {
        generateCalls++;
        capturedPrompts.add(prompt);
    }

    // --- Verification methods ---

    public int getGenerateCalls() {
        return generateCalls;
    }

    public List<String> getCapturedPrompts() {
        return new ArrayList<>(capturedPrompts);
    }

    public String getLastPrompt() {
        if (capturedPrompts.isEmpty()) {
            return null;
        }
        return capturedPrompts.get(capturedPrompts.size() - 1);
    }

    /**
     * Check if any captured prompt contains the given substring.
     */
    public boolean promptContains(String substring) {
        return capturedPrompts.stream()
            .anyMatch(p -> p.contains(substring));
    }

    public void reset() {
        capturedPrompts.clear();
        generateCalls = 0;
    }
}
