package synth.msoffice.tools;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Saves document changes back to Base64 for database storage.
 * Reads the modified file and returns Base64-encoded content.
 */
public class SaveDocumentTool {

    public Map<String, Object> getToolDefinition() {
        Map<String, Object> inputSchema = new LinkedHashMap<>();
        inputSchema.put("type", "object");

        Map<String, Object> properties = new LinkedHashMap<>();

        Map<String, Object> documentId = new LinkedHashMap<>();
        documentId.put("type", "string");
        documentId.put("description", "Document identifier to save");
        properties.put("documentId", documentId);

        inputSchema.put("properties", properties);
        inputSchema.put("required", Collections.singletonList("documentId"));

        Map<String, Object> definition = new LinkedHashMap<>();
        definition.put("name", "office_save_document");
        definition.put("description", "Save document changes back to Base64. Returns the updated Base64 content for database storage.");
        definition.put("inputSchema", inputSchema);
        return definition;
    }

    public Map<String, Object> execute(Map<String, Object> arguments) throws Exception {
        String documentId = (String) arguments.get("documentId");

        if (documentId == null) {
            throw new IllegalArgumentException("documentId is required");
        }

        DocumentTracker tracker = DocumentTracker.getInstance();
        DocumentTracker.TrackedDocument doc = tracker.getDocument(documentId);

        if (doc == null) {
            throw new IllegalArgumentException("Document not found in tracker: " + documentId);
        }

        Path filePath = Path.of(doc.filePath);

        if (!Files.exists(filePath)) {
            throw new IllegalArgumentException("Document file no longer exists: " + filePath);
        }

        // Read file and encode to Base64
        byte[] fileBytes = Files.readAllBytes(filePath);
        String base64Content = Base64.getEncoder().encodeToString(fileBytes);

        // Update tracker
        String modifiedTime = Files.getLastModifiedTime(filePath).toInstant().toString();
        tracker.markSynced(documentId, modifiedTime);

        // Return result with Base64 content
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("documentId", documentId);
        result.put("fileName", doc.originalFileName);
        result.put("base64Content", base64Content);
        result.put("sizeBytes", fileBytes.length);
        result.put("lastModified", modifiedTime);
        result.put("status", "synced");

        return result;
    }
}
