package synth.msoffice.tools;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks opened documents for synchronization back to database.
 * Monitors file modifications and maintains document metadata.
 */
public class DocumentTracker {

    private static DocumentTracker instance;
    private static final Path TRACKER_FILE = Path.of(System.getProperty("java.io.tmpdir"), "synth-office", "tracker.json");
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private final Map<String, TrackedDocument> trackedDocuments = new ConcurrentHashMap<>();

    public static synchronized DocumentTracker getInstance() {
        if (instance == null) {
            instance = new DocumentTracker();
        }
        return instance;
    }

    private DocumentTracker() {
        loadTrackerState();
    }

    public void trackDocument(String documentId, Path filePath, String originalFileName) {
        TrackedDocument doc = new TrackedDocument();
        doc.documentId = documentId;
        doc.filePath = filePath.toString();
        doc.originalFileName = originalFileName;
        doc.openedAt = Instant.now().toString();
        doc.lastModified = getFileModifiedTime(filePath);
        doc.synced = true;

        trackedDocuments.put(documentId, doc);
        saveTrackerState();
    }

    public TrackedDocument getDocument(String documentId) {
        return trackedDocuments.get(documentId);
    }

    public List<TrackedDocument> getAllDocuments() {
        // Update modification status before returning
        for (TrackedDocument doc : trackedDocuments.values()) {
            String currentModified = getFileModifiedTime(Path.of(doc.filePath));
            if (!currentModified.equals(doc.lastModified)) {
                doc.synced = false;
            }
        }
        return new ArrayList<>(trackedDocuments.values());
    }

    public void markSynced(String documentId, String newModifiedTime) {
        TrackedDocument doc = trackedDocuments.get(documentId);
        if (doc != null) {
            doc.lastModified = newModifiedTime;
            doc.synced = true;
            saveTrackerState();
        }
    }

    public void removeDocument(String documentId) {
        trackedDocuments.remove(documentId);
        saveTrackerState();
    }

    public byte[] getDocumentBytes(String documentId) throws IOException {
        TrackedDocument doc = trackedDocuments.get(documentId);
        if (doc == null) {
            throw new IllegalArgumentException("Document not tracked: " + documentId);
        }
        return Files.readAllBytes(Path.of(doc.filePath));
    }

    private String getFileModifiedTime(Path path) {
        try {
            return Files.getLastModifiedTime(path).toInstant().toString();
        } catch (IOException e) {
            return "unknown";
        }
    }

    private void loadTrackerState() {
        try {
            if (Files.exists(TRACKER_FILE)) {
                String json = Files.readString(TRACKER_FILE);
                Map<String, TrackedDocument> loaded = gson.fromJson(json,
                    new TypeToken<Map<String, TrackedDocument>>(){}.getType());
                if (loaded != null) {
                    trackedDocuments.putAll(loaded);
                }
            }
        } catch (Exception e) {
            // Start fresh if loading fails
        }
    }

    private void saveTrackerState() {
        try {
            Files.createDirectories(TRACKER_FILE.getParent());
            Files.writeString(TRACKER_FILE, gson.toJson(trackedDocuments));
        } catch (IOException e) {
            System.err.println("Failed to save tracker state: " + e.getMessage());
        }
    }

    public static class TrackedDocument {
        public String documentId;
        public String filePath;
        public String originalFileName;
        public String openedAt;
        public String lastModified;
        public boolean synced;
    }
}
