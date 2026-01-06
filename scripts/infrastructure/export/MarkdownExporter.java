import java.time.format.DateTimeFormatter;
import java.time.ZoneId;

/**
 * Infrastructure adapter that implements ExportPort for Markdown format.
 */
public class MarkdownExporter implements ExportPort {

    private static final DateTimeFormatter DATE_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    @Override
    public String export(StandupReport report) {
        StringBuilder sb = new StringBuilder();

        sb.append("# Standup Report for ").append(report.username()).append("\n\n");
        sb.append("**Period:** Last ").append(report.days()).append(" day(s)\n");
        sb.append("**Generated:** ").append(DATE_FORMATTER.format(report.generatedAt())).append("\n\n");

        if (!report.hasSections()) {
            sb.append("*No activity to report.*\n");
            return sb.toString();
        }

        for (ReportSection section : report.sections()) {
            sb.append("## ").append(section.title()).append("\n\n");
            if (section.hasContent()) {
                sb.append(section.content()).append("\n\n");
            } else {
                sb.append("*No items in this section.*\n\n");
            }
        }

        return sb.toString();
    }
}
