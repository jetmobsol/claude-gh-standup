import java.time.format.DateTimeFormatter;
import java.time.ZoneId;

/**
 * Infrastructure adapter that implements ExportPort for HTML format.
 */
public class HtmlExporter implements ExportPort {

    private static final DateTimeFormatter DATE_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    @Override
    public String export(StandupReport report) {
        StringBuilder sb = new StringBuilder();

        // HTML header with basic styling
        sb.append("<!DOCTYPE html>\n");
        sb.append("<html lang=\"en\">\n");
        sb.append("<head>\n");
        sb.append("  <meta charset=\"UTF-8\">\n");
        sb.append("  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        sb.append("  <title>Standup Report - ").append(escapeHtml(report.username())).append("</title>\n");
        sb.append("  <style>\n");
        sb.append("    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; ");
        sb.append("           max-width: 800px; margin: 0 auto; padding: 20px; line-height: 1.6; }\n");
        sb.append("    h1 { color: #1a1a2e; border-bottom: 2px solid #4361ee; padding-bottom: 10px; }\n");
        sb.append("    h2 { color: #16213e; margin-top: 30px; }\n");
        sb.append("    .meta { color: #666; font-size: 0.9em; margin-bottom: 20px; }\n");
        sb.append("    .section { background: #f8f9fa; padding: 15px; border-radius: 8px; margin: 15px 0; }\n");
        sb.append("    .empty { color: #888; font-style: italic; }\n");
        sb.append("    pre { background: #f4f4f4; padding: 10px; border-radius: 4px; overflow-x: auto; }\n");
        sb.append("  </style>\n");
        sb.append("</head>\n");
        sb.append("<body>\n");

        // Content
        sb.append("  <h1>Standup Report for ").append(escapeHtml(report.username())).append("</h1>\n");
        sb.append("  <div class=\"meta\">\n");
        sb.append("    <strong>Period:</strong> Last ").append(report.days()).append(" day(s)<br>\n");
        sb.append("    <strong>Generated:</strong> ").append(DATE_FORMATTER.format(report.generatedAt())).append("\n");
        sb.append("  </div>\n");

        if (!report.hasSections()) {
            sb.append("  <p class=\"empty\">No activity to report.</p>\n");
        } else {
            for (ReportSection section : report.sections()) {
                sb.append("  <div class=\"section\">\n");
                sb.append("    <h2>").append(escapeHtml(section.title())).append("</h2>\n");
                if (section.hasContent()) {
                    // Convert markdown-style content to basic HTML
                    String htmlContent = convertMarkdownToHtml(section.content());
                    sb.append("    ").append(htmlContent).append("\n");
                } else {
                    sb.append("    <p class=\"empty\">No items in this section.</p>\n");
                }
                sb.append("  </div>\n");
            }
        }

        // HTML footer
        sb.append("</body>\n");
        sb.append("</html>");

        return sb.toString();
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
    }

    private String convertMarkdownToHtml(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return "";
        }

        String html = escapeHtml(markdown);

        // Convert bullet points to list
        if (html.contains("- ")) {
            StringBuilder listBuilder = new StringBuilder("<ul>\n");
            for (String line : html.split("\n")) {
                if (line.startsWith("- ")) {
                    listBuilder.append("      <li>").append(line.substring(2)).append("</li>\n");
                } else if (!line.isBlank()) {
                    listBuilder.append("      <li>").append(line).append("</li>\n");
                }
            }
            listBuilder.append("    </ul>");
            html = listBuilder.toString();
        } else {
            // Wrap plain text in paragraph
            html = "<p>" + html.replace("\n\n", "</p><p>").replace("\n", "<br>") + "</p>";
        }

        return html;
    }
}
