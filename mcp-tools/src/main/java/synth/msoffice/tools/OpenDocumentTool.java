package synth.msoffice.tools;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Opens a document with the appropriate Microsoft Office application.
 * Supports Base64-encoded documents from database or file paths.
 */
public class OpenDocumentTool {

    private static final Path TEMP_DIR = Path.of(System.getProperty("java.io.tmpdir"), "synth-office");
    private static final Map<String, String> OFFICE_APPS = Map.of(
        ".docx", "WINWORD.EXE",
        ".doc", "WINWORD.EXE",
        ".xlsx", "EXCEL.EXE",
        ".xls", "EXCEL.EXE",
        ".pptx", "POWERPNT.EXE",
        ".ppt", "POWERPNT.EXE"
    );

    public OpenDocumentTool() {
        try {
            Files.createDirectories(TEMP_DIR);
        } catch (IOException e) {
            // Temp dir might already exist
        }
    }

    public Map<String, Object> getToolDefinition() {
        Map<String, Object> inputSchema = new LinkedHashMap<>();
        inputSchema.put("type", "object");

        Map<String, Object> properties = new LinkedHashMap<>();

        Map<String, Object> documentId = new LinkedHashMap<>();
        documentId.put("type", "string");
        documentId.put("description", "Unique document identifier for tracking changes");
        properties.put("documentId", documentId);

        Map<String, Object> fileName = new LinkedHashMap<>();
        fileName.put("type", "string");
        fileName.put("description", "Original filename with extension (e.g., 'report.docx')");
        properties.put("fileName", fileName);

        Map<String, Object> base64Content = new LinkedHashMap<>();
        base64Content.put("type", "string");
        base64Content.put("description", "Base64-encoded document content from database");
        properties.put("base64Content", base64Content);

        Map<String, Object> filePath = new LinkedHashMap<>();
        filePath.put("type", "string");
        filePath.put("description", "Alternative: Direct file path instead of Base64 content");
        properties.put("filePath", filePath);

        inputSchema.put("properties", properties);
        inputSchema.put("required", Arrays.asList("documentId", "fileName"));

        Map<String, Object> definition = new LinkedHashMap<>();
        definition.put("name", "office_open_document");
        definition.put("description", "Open a document with Microsoft Office. Provide either base64Content (from DB) or filePath.");
        definition.put("inputSchema", inputSchema);
        return definition;
    }

    public String execute(Map<String, Object> arguments) throws Exception {
        String documentId = (String) arguments.get("documentId");
        String fileName = (String) arguments.get("fileName");
        String base64Content = (String) arguments.get("base64Content");
        String filePath = (String) arguments.get("filePath");

        if (documentId == null || fileName == null) {
            throw new IllegalArgumentException("documentId and fileName are required");
        }

        Path targetFile;

        if (base64Content != null && !base64Content.isEmpty()) {
            // Decode Base64 and write to temp file
            byte[] decoded = Base64.getDecoder().decode(base64Content);
            targetFile = TEMP_DIR.resolve(documentId + "_" + fileName);
            Files.write(targetFile, decoded);

            // Track the file for sync back
            DocumentTracker.getInstance().trackDocument(documentId, targetFile, fileName);
        } else if (filePath != null && !filePath.isEmpty()) {
            targetFile = Path.of(filePath);
            if (!Files.exists(targetFile)) {
                throw new IllegalArgumentException("File not found: " + filePath);
            }
        } else {
            throw new IllegalArgumentException("Either base64Content or filePath must be provided");
        }

        // Determine Office application
        String extension = fileName.substring(fileName.lastIndexOf('.')).toLowerCase();
        String officeApp = OFFICE_APPS.get(extension);

        if (officeApp == null) {
            // Fallback: open with default application
            java.awt.Desktop.getDesktop().open(targetFile.toFile());
            return String.format("Opened %s with default application (document ID: %s)", fileName, documentId);
        }

        // Try to find Office installation
        String officePath = findOfficeExecutable(officeApp);

        if (officePath != null) {
            ProcessBuilder pb = new ProcessBuilder(officePath, targetFile.toString());
            pb.start();
            return String.format("Opened %s with %s (document ID: %s, tracking enabled)",
                fileName, officeApp, documentId);
        } else {
            // Fallback to default application
            java.awt.Desktop.getDesktop().open(targetFile.toFile());
            return String.format("Opened %s with default application (document ID: %s)", fileName, documentId);
        }
    }

    private String findOfficeExecutable(String exeName) {
        // Common Office installation paths on Windows
        String[] searchPaths = {
            System.getenv("ProgramFiles") + "\\Microsoft Office\\root\\Office16",
            System.getenv("ProgramFiles(x86)") + "\\Microsoft Office\\root\\Office16",
            System.getenv("ProgramFiles") + "\\Microsoft Office\\Office16",
            System.getenv("ProgramFiles(x86)") + "\\Microsoft Office\\Office16",
            System.getenv("ProgramFiles") + "\\Microsoft Office 15\\root\\office15",
            System.getenv("ProgramFiles(x86)") + "\\Microsoft Office 15\\root\\office15"
        };

        for (String basePath : searchPaths) {
            if (basePath == null) continue;
            File exe = new File(basePath, exeName);
            if (exe.exists()) {
                return exe.getAbsolutePath();
            }
        }

        return null;
    }
}
