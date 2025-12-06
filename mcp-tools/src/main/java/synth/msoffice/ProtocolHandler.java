package synth.msoffice;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.InputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import synth.msoffice.tools.OpenDocumentTool;

/**
 * Handles synth-office:// protocol URLs from the browser.
 * Called by the registered protocol handler.
 *
 * URL format: synth-office://open?docId=123&token=xyz&fileName=report.docx
 */
public class ProtocolHandler {

    private static final Gson gson = new Gson();
    private static final HttpClient httpClient = HttpClient.newHttpClient();

    public static void main(String[] args) {
        if (args.length < 2) {
            showError("Usage: ProtocolHandler <url> <serverUrl>");
            return;
        }

        String url = args[0];
        String serverUrl = args[1];

        try {
            handleUrl(url, serverUrl);
        } catch (Exception e) {
            showError("Error: " + e.getMessage());
        }
    }

    private static void handleUrl(String url, String serverUrl) throws Exception {
        // Parse URL: synth-office://open?docId=123&token=xyz
        URI uri = new URI(url);
        String action = uri.getHost(); // "open"
        Map<String, String> params = parseQueryParams(uri.getQuery());

        if (!"open".equals(action)) {
            showError("Unknown action: " + action);
            return;
        }

        String docId = params.get("docId");
        String token = params.get("token");
        String fileName = params.get("fileName");

        if (docId == null) {
            showError("Missing docId parameter");
            return;
        }

        // Fetch document from server
        String fetchUrl = serverUrl + "/api/documents/" + docId + "/content";
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(fetchUrl))
            .header("Accept", "application/json")
            .header("Authorization", token != null ? "Bearer " + token : "")
            .GET()
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            showError("Failed to fetch document: HTTP " + response.statusCode());
            return;
        }

        JsonObject docResponse = gson.fromJson(response.body(), JsonObject.class);
        String base64Content = docResponse.has("base64Content")
            ? docResponse.get("base64Content").getAsString()
            : null;

        if (fileName == null && docResponse.has("fileName")) {
            fileName = docResponse.get("fileName").getAsString();
        }

        if (base64Content == null) {
            showError("No document content received");
            return;
        }

        // Open with Office
        OpenDocumentTool openTool = new OpenDocumentTool();
        Map<String, Object> openArgs = new HashMap<>();
        openArgs.put("documentId", docId);
        openArgs.put("fileName", fileName != null ? fileName : "document.docx");
        openArgs.put("base64Content", base64Content);

        String result = openTool.execute(openArgs);
        System.out.println(result);
    }

    private static Map<String, String> parseQueryParams(String query) {
        Map<String, String> params = new HashMap<>();
        if (query == null || query.isEmpty()) {
            return params;
        }

        for (String param : query.split("&")) {
            String[] pair = param.split("=", 2);
            if (pair.length == 2) {
                String key = URLDecoder.decode(pair[0], StandardCharsets.UTF_8);
                String value = URLDecoder.decode(pair[1], StandardCharsets.UTF_8);
                params.put(key, value);
            }
        }
        return params;
    }

    private static void showError(String message) {
        // Show error dialog on Windows
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            try {
                ProcessBuilder pb = new ProcessBuilder(
                    "mshta",
                    "javascript:alert('" + message.replace("'", "\\'") + "');close();"
                );
                pb.start();
            } catch (Exception e) {
                System.err.println(message);
            }
        } else {
            System.err.println(message);
        }
    }
}
