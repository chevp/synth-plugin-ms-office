package synth.msoffice.tools;

import java.nio.file.*;
import java.util.*;

/**
 * Information about the synth:// protocol handler for Microsoft Office.
 *
 * IMPORTANT: This plugin does NOT register its own protocol.
 * The unified synth:// protocol is registered by the central Synth host app
 * (synth-platform-console or similar).
 *
 * This tool provides information about how to use the Office handler
 * and checks if the central protocol is registered.
 *
 * URL format: synth://office/open?docId=123&token=xyz
 *
 * The central synth:// handler routes to plugins based on path:
 *   synth://blender/*  -> BlenderHandler
 *   synth://office/*   -> OfficeHandler
 *   synth://server/*   -> ServerHandler
 */
public class RegisterProtocolTool {

    private static final String PROTOCOL_NAME = "synth";

    public Map<String, Object> getToolDefinition() {
        Map<String, Object> inputSchema = new LinkedHashMap<>();
        inputSchema.put("type", "object");

        Map<String, Object> properties = new LinkedHashMap<>();

        Map<String, Object> action = new LinkedHashMap<>();
        action.put("type", "string");
        action.put("enum", Arrays.asList("status", "info"));
        action.put("description", "Action: 'status' to check if synth:// is registered, 'info' for usage information");
        properties.put("action", action);

        inputSchema.put("properties", properties);
        inputSchema.put("required", Collections.singletonList("action"));

        Map<String, Object> definition = new LinkedHashMap<>();
        definition.put("name", "office_register_protocol");
        definition.put("description", "Check synth:// protocol status and get usage info. Note: Registration is handled by the central Synth host app.");
        definition.put("inputSchema", inputSchema);
        return definition;
    }

    public String execute(Map<String, Object> arguments) throws Exception {
        String action = (String) arguments.getOrDefault("action", "info");

        return switch (action) {
            case "status" -> checkStatus();
            case "info" -> getInfo();
            default -> getInfo();
        };
    }

    private String checkStatus() {
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("win")) {
            try {
                ProcessBuilder pb = new ProcessBuilder("reg", "query", "HKCU\\Software\\Classes\\" + PROTOCOL_NAME);
                Process process = pb.start();
                int exitCode = process.waitFor();

                if (exitCode == 0) {
                    return """
                        synth:// protocol is REGISTERED.

                        You can use URLs like:
                          synth://office/open?docId=123&token=xyz
                          synth://office/open?filePath=C:/docs/report.docx

                        The central Synth handler will route to this plugin.
                        """;
                } else {
                    return """
                        synth:// protocol is NOT registered.

                        The synth:// protocol must be registered by the central Synth host app.
                        Please start synth-platform-console or run the Synth installer.

                        Once registered, you can use:
                          synth://office/open?docId=123
                        """;
                }
            } catch (Exception e) {
                return "Could not check status: " + e.getMessage();
            }
        } else if (os.contains("mac")) {
            return "macOS: Check if Synth.app is installed and associated with synth:// URLs.";
        } else {
            try {
                Path desktopEntry = Path.of(
                    System.getProperty("user.home"),
                    ".local", "share", "applications", "synth-handler.desktop"
                );
                if (Files.exists(desktopEntry)) {
                    return "synth:// protocol is REGISTERED (Linux desktop entry found).";
                } else {
                    return "synth:// protocol is NOT registered. Install the Synth desktop handler.";
                }
            } catch (Exception e) {
                return "Could not check status: " + e.getMessage();
            }
        }
    }

    private String getInfo() {
        return """
            Microsoft Office Integration - synth:// Protocol Handler

            This plugin handles routes under synth://office/*

            SUPPORTED URLS:
              synth://office/open?docId=123&token=xyz
                - Opens a document from the Synth server in Office

              synth://office/open?filePath=/path/to/document.docx
                - Opens a local file in Office

              synth://office/formats
                - Lists supported file formats

            SUPPORTED FORMATS:
              Word:       .docx, .doc
              Excel:      .xlsx, .xls
              PowerPoint: .pptx, .ppt

            PROTOCOL REGISTRATION:
              The unified synth:// protocol is registered by the central
              Synth host application (synth-platform-console).

              This plugin provides an OfficeHandler class that the central
              router calls when synth://office/* URLs are invoked.

            HANDLER CLASS:
              synth.msoffice.OfficeHandler

            EXAMPLE INTEGRATION:
              OfficeHandler handler = new OfficeHandler();
              handler.handle("open", params, serverUrl);
            """;
    }
}