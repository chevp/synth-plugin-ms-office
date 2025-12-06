package synth.msoffice.tools;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.*;

/**
 * Lists recently opened documents with their sync status.
 */
public class ListRecentTool {

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public Map<String, Object> getToolDefinition() {
        Map<String, Object> inputSchema = new LinkedHashMap<>();
        inputSchema.put("type", "object");
        inputSchema.put("properties", new LinkedHashMap<>());
        inputSchema.put("required", Collections.emptyList());

        Map<String, Object> definition = new LinkedHashMap<>();
        definition.put("name", "office_list_recent");
        definition.put("description", "List recently opened documents with their sync status. Shows which documents have unsaved changes.");
        definition.put("inputSchema", inputSchema);
        return definition;
    }

    public String execute(Map<String, Object> arguments) {
        DocumentTracker tracker = DocumentTracker.getInstance();
        List<DocumentTracker.TrackedDocument> documents = tracker.getAllDocuments();

        if (documents.isEmpty()) {
            return "No documents currently tracked.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Tracked Documents:\n");
        sb.append("==================\n\n");

        for (DocumentTracker.TrackedDocument doc : documents) {
            String syncStatus = doc.synced ? "[SYNCED]" : "[MODIFIED - needs sync]";
            sb.append(String.format("ID: %s\n", doc.documentId));
            sb.append(String.format("  File: %s\n", doc.originalFileName));
            sb.append(String.format("  Path: %s\n", doc.filePath));
            sb.append(String.format("  Opened: %s\n", doc.openedAt));
            sb.append(String.format("  Status: %s\n", syncStatus));
            sb.append("\n");
        }

        long unsyncedCount = documents.stream().filter(d -> !d.synced).count();
        if (unsyncedCount > 0) {
            sb.append(String.format("\n%d document(s) have unsaved changes.\n", unsyncedCount));
        }

        return sb.toString();
    }
}
