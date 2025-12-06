package synth.msoffice;

import com.google.gson.Gson;
import synth.msoffice.tools.OpenDocumentTool;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

/**
 * Handler for synth://office/* routes.
 *
 * This class is called by the centralized Synth protocol handler
 * when URLs like synth://office/open?docId=123 are invoked.
 *
 * Supported routes:
 *   synth://office/open?docId=123&token=xyz      - Open document in Office
 *   synth://office/open?filePath=/path/to/doc    - Open local file
 *   synth://office/formats                        - List supported formats
 *
 * The central synth:// handler routes to plugins based on path:
 *   synth://blender/*  -> BlenderHandler
 *   synth://office/*   -> OfficeHandler
 *   synth://server/*   -> ServerHandler
 */
public class OfficeHandler {

    private static final Gson gson = new Gson();
    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private final OpenDocumentTool openDocumentTool;

    public OfficeHandler() {
        this.openDocumentTool = new OpenDocumentTool();
    }

    /**
     * Handle a route from the centralized synth:// protocol.
     *
     * @param route The route path (e.g., "open", "formats")
     * @param params Query parameters from the URL
     * @param serverUrl Base URL of synth-platform-web for API calls
     * @return Result message
     */
    public String handle(String route, Map<String, String> params, String serverUrl) throws Exception {
        return switch (route) {
            case "open" -> handleOpen(params, serverUrl);
            case "formats" -> handleFormats();
            default -> throw new IllegalArgumentException("Unknown route: office/" + route);
        };
    }

    /**
     * Handle synth://office/open?docId=123&token=xyz
     * or    synth://office/open?filePath=/path/to/document.docx
     */
    private String handleOpen(Map<String, String> params, String serverUrl) throws Exception {
        String docId = params.get("docId");
        String filePath = params.get("filePath");
        String fileName = params.get("fileName");
        String token = params.get("token");

        if (filePath != null && !filePath.isEmpty()) {
            // Direct file path - no server fetch needed
            if (fileName == null) {
                fileName = filePath.substring(filePath.lastIndexOf('/') + 1);
                if (fileName.contains("\\")) {
                    fileName = fileName.substring(fileName.lastIndexOf('\\') + 1);
                }
            }

            Map<String, Object> openArgs = new HashMap<>();
            openArgs.put("documentId", filePath); // Use path as ID for tracking
            openArgs.put("fileName", fileName);
            openArgs.put("filePath", filePath);

            return openDocumentTool.execute(openArgs);
        }

        if (docId == null || docId.isEmpty()) {
            throw new IllegalArgumentException("Either docId or filePath parameter is required");
        }

        // Fetch document from server
        String fetchUrl = serverUrl + "/api/documents/" + docId + "/content";

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
            .uri(URI.create(fetchUrl))
            .header("Accept", "application/json")
            .GET();

        if (token != null && !token.isEmpty()) {
            requestBuilder.header("Authorization", "Bearer " + token);
        }

        HttpResponse<String> response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Server returned error: HTTP " + response.statusCode());
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> docResponse = gson.fromJson(response.body(), Map.class);

        String fetchedFileName = (String) docResponse.get("fileName");
        String base64Content = (String) docResponse.get("base64Content");

        if (base64Content == null) {
            throw new RuntimeException("Invalid server response: missing base64Content");
        }

        if (fileName == null) {
            fileName = fetchedFileName != null ? fetchedFileName : "document.docx";
        }

        Map<String, Object> openArgs = new HashMap<>();
        openArgs.put("documentId", docId);
        openArgs.put("fileName", fileName);
        openArgs.put("base64Content", base64Content);

        return openDocumentTool.execute(openArgs);
    }

    /**
     * Handle synth://office/formats - list supported formats
     */
    private String handleFormats() {
        return """
            Supported Microsoft Office formats:

            WORD:
              .docx (Word Document)
              .doc  (Word 97-2003 Document)

            EXCEL:
              .xlsx (Excel Workbook)
              .xls  (Excel 97-2003 Workbook)

            POWERPOINT:
              .pptx (PowerPoint Presentation)
              .ppt  (PowerPoint 97-2003 Presentation)
            """;
    }

    /**
     * Get the handler ID for registration with the central protocol router.
     */
    public static String getHandlerId() {
        return "office";
    }

    /**
     * Get supported routes for this handler.
     */
    public static List<String> getSupportedRoutes() {
        return Arrays.asList("open", "formats");
    }
}