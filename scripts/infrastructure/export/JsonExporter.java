import com.google.gson.*;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;

/**
 * Infrastructure adapter that implements ExportPort for JSON format.
 */
public class JsonExporter implements ExportPort {

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final DateTimeFormatter ISO_FORMATTER =
        DateTimeFormatter.ISO_INSTANT;

    @Override
    public String export(StandupReport report) {
        JsonObject root = new JsonObject();

        root.addProperty("username", report.username());
        root.addProperty("days", report.days());
        root.addProperty("generatedAt", ISO_FORMATTER.format(report.generatedAt()));

        JsonArray sectionsArray = new JsonArray();
        for (ReportSection section : report.sections()) {
            JsonObject sectionObj = new JsonObject();
            sectionObj.addProperty("title", section.title());
            sectionObj.addProperty("content", section.content());
            sectionObj.addProperty("hasContent", section.hasContent());
            sectionsArray.add(sectionObj);
        }
        root.add("sections", sectionsArray);

        JsonObject stats = new JsonObject();
        stats.addProperty("totalSections", report.sections().size());
        stats.addProperty("sectionsWithContent", report.sectionsWithContent());
        root.add("stats", stats);

        return gson.toJson(root);
    }
}
