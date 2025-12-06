package synth.msoffice;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Standalone protocol handler for synth://office/* URLs.
 *
 * This class can be invoked directly by the OS protocol handler
 * OR by the centralized synth:// router.
 *
 * URL formats:
 *   synth://office/open?docId=123&token=xyz
 *   synth://office/open?filePath=/path/to/document.docx
 *   synth://office/formats
 *
 * Legacy format (still supported):
 *   synth-office://open?docId=123&token=xyz
 *
 * Usage from centralized handler:
 *   OfficeHandler handler = new OfficeHandler();
 *   handler.handle("open", params, serverUrl);
 *
 * Usage standalone (for testing or direct invocation):
 *   java -jar synth-ms-office.jar "synth://office/open?docId=123" "http://localhost:4200"
 */
public class ProtocolHandler {

    public static void main(String[] args) {
        if (args.length < 1) {
            showError("Usage: ProtocolHandler <url> [serverUrl]");
            return;
        }

        String url = args[0];
        String serverUrl = args.length > 1 ? args[1] : "http://localhost:4200";

        try {
            new ProtocolHandler().handle(url, serverUrl);
        } catch (Exception e) {
            showError("Error: " + e.getMessage());
        }
    }

    public void handle(String protocolUrl, String serverUrl) throws Exception {
        // Parse URL: synth://office/open?docId=123
        // Also support legacy: synth-office://open?docId=123

        String path;
        if (protocolUrl.startsWith("synth://office/")) {
            path = protocolUrl.substring("synth://office/".length());
        } else if (protocolUrl.startsWith("synth-office://")) {
            // Legacy support
            path = protocolUrl.substring("synth-office://".length());
        } else {
            throw new IllegalArgumentException("Invalid protocol URL. Expected synth://office/* or synth-office://*");
        }

        // Extract route and params
        String route;
        Map<String, String> params;

        int queryStart = path.indexOf("?");
        if (queryStart >= 0) {
            route = path.substring(0, queryStart);
            params = parseQueryParams(path.substring(queryStart + 1));
        } else {
            route = path;
            params = new HashMap<>();
        }

        // Delegate to OfficeHandler
        OfficeHandler handler = new OfficeHandler();
        String result = handler.handle(route, params, serverUrl);
        System.out.println(result);
    }

    private Map<String, String> parseQueryParams(String query) {
        Map<String, String> params = new HashMap<>();

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