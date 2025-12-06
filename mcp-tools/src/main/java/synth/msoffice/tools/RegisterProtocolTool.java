package synth.msoffice.tools;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Registers the synth-office:// custom URL protocol handler.
 * This enables opening documents directly from web browsers via custom URLs.
 *
 * URL format: synth-office://open?docId=123&token=xyz
 */
public class RegisterProtocolTool {

    private static final String PROTOCOL_NAME = "synth-office";
    private static final Path HANDLER_DIR = Path.of(System.getProperty("user.home"), ".synth", "protocol-handler");

    public Map<String, Object> getToolDefinition() {
        Map<String, Object> inputSchema = new LinkedHashMap<>();
        inputSchema.put("type", "object");

        Map<String, Object> properties = new LinkedHashMap<>();

        Map<String, Object> action = new LinkedHashMap<>();
        action.put("type", "string");
        action.put("enum", Arrays.asList("register", "unregister", "status"));
        action.put("description", "Action to perform: register, unregister, or check status");
        properties.put("action", action);

        Map<String, Object> serverUrl = new LinkedHashMap<>();
        serverUrl.put("type", "string");
        serverUrl.put("description", "Base URL of synth-platform-web server (for document fetch)");
        properties.put("serverUrl", serverUrl);

        inputSchema.put("properties", properties);
        inputSchema.put("required", Collections.singletonList("action"));

        Map<String, Object> definition = new LinkedHashMap<>();
        definition.put("name", "office_register_protocol");
        definition.put("description", "Register/unregister the synth-office:// custom URL protocol handler for browser integration.");
        definition.put("inputSchema", inputSchema);
        return definition;
    }

    public String execute(Map<String, Object> arguments) throws Exception {
        String action = (String) arguments.get("action");
        String serverUrl = (String) arguments.getOrDefault("serverUrl", "http://localhost:4200");

        return switch (action) {
            case "register" -> registerProtocol(serverUrl);
            case "unregister" -> unregisterProtocol();
            case "status" -> checkStatus();
            default -> throw new IllegalArgumentException("Invalid action: " + action);
        };
    }

    private String registerProtocol(String serverUrl) throws Exception {
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("win")) {
            return registerWindowsProtocol(serverUrl);
        } else if (os.contains("mac")) {
            return "macOS protocol registration requires manual setup. See documentation.";
        } else {
            return registerLinuxProtocol(serverUrl);
        }
    }

    private String registerWindowsProtocol(String serverUrl) throws Exception {
        // Create handler script
        Files.createDirectories(HANDLER_DIR);
        Path handlerScript = HANDLER_DIR.resolve("synth-office-handler.bat");

        String jarPath = getJarPath();
        String scriptContent = String.format("""
            @echo off
            setlocal
            set URL=%%1
            java -cp "%s" synth.msoffice.ProtocolHandler "%%URL%%" "%s"
            """, jarPath, serverUrl);

        Files.writeString(handlerScript, scriptContent);

        // Create registry file
        Path regFile = HANDLER_DIR.resolve("register-protocol.reg");
        String handlerPath = handlerScript.toString().replace("\\", "\\\\");

        String regContent = String.format("""
            Windows Registry Editor Version 5.00

            [HKEY_CURRENT_USER\\Software\\Classes\\%s]
            @="URL:Synth Office Protocol"
            "URL Protocol"=""

            [HKEY_CURRENT_USER\\Software\\Classes\\%s\\shell]

            [HKEY_CURRENT_USER\\Software\\Classes\\%s\\shell\\open]

            [HKEY_CURRENT_USER\\Software\\Classes\\%s\\shell\\open\\command]
            @="\\"%s\\" \\"%%1\\""
            """, PROTOCOL_NAME, PROTOCOL_NAME, PROTOCOL_NAME, PROTOCOL_NAME, handlerPath);

        Files.writeString(regFile, regContent);

        // Apply registry
        ProcessBuilder pb = new ProcessBuilder("reg", "import", regFile.toString());
        Process process = pb.start();
        int exitCode = process.waitFor();

        if (exitCode == 0) {
            return String.format("""
                Protocol handler registered successfully!

                Protocol: %s://
                Handler: %s
                Server: %s

                You can now use URLs like:
                  %s://open?docId=123

                To open documents directly from the browser.
                """, PROTOCOL_NAME, handlerScript, serverUrl, PROTOCOL_NAME);
        } else {
            return "Failed to register protocol. Please run as administrator or import registry file manually:\n" + regFile;
        }
    }

    private String registerLinuxProtocol(String serverUrl) throws Exception {
        Files.createDirectories(HANDLER_DIR);

        // Create desktop entry
        Path desktopEntry = Path.of(System.getProperty("user.home"), ".local", "share", "applications", "synth-office-handler.desktop");
        Files.createDirectories(desktopEntry.getParent());

        String jarPath = getJarPath();
        String desktopContent = String.format("""
            [Desktop Entry]
            Type=Application
            Name=Synth Office Handler
            Exec=java -cp %s synth.msoffice.ProtocolHandler %%u %s
            StartupNotify=false
            MimeType=x-scheme-handler/%s;
            NoDisplay=true
            """, jarPath, serverUrl, PROTOCOL_NAME);

        Files.writeString(desktopEntry, desktopContent);

        // Register with xdg-mime
        ProcessBuilder pb = new ProcessBuilder("xdg-mime", "default", "synth-office-handler.desktop", "x-scheme-handler/" + PROTOCOL_NAME);
        pb.start().waitFor();

        return String.format("Protocol handler registered for Linux.\nDesktop entry: %s", desktopEntry);
    }

    private String unregisterProtocol() throws Exception {
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("win")) {
            ProcessBuilder pb = new ProcessBuilder("reg", "delete", "HKCU\\Software\\Classes\\" + PROTOCOL_NAME, "/f");
            pb.start().waitFor();
            return "Protocol handler unregistered.";
        } else {
            Path desktopEntry = Path.of(System.getProperty("user.home"), ".local", "share", "applications", "synth-office-handler.desktop");
            Files.deleteIfExists(desktopEntry);
            return "Protocol handler unregistered.";
        }
    }

    private String checkStatus() {
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("win")) {
            try {
                ProcessBuilder pb = new ProcessBuilder("reg", "query", "HKCU\\Software\\Classes\\" + PROTOCOL_NAME);
                Process process = pb.start();
                int exitCode = process.waitFor();
                return exitCode == 0 ? "Protocol handler is registered." : "Protocol handler is NOT registered.";
            } catch (Exception e) {
                return "Could not check status: " + e.getMessage();
            }
        } else {
            Path desktopEntry = Path.of(System.getProperty("user.home"), ".local", "share", "applications", "synth-office-handler.desktop");
            return Files.exists(desktopEntry) ? "Protocol handler is registered." : "Protocol handler is NOT registered.";
        }
    }

    private String getJarPath() {
        // Try to find the JAR file path
        String classPath = System.getProperty("java.class.path");
        if (classPath.endsWith(".jar")) {
            return classPath;
        }
        // Fallback to expected location
        return "synth-ms-office-1.0.0.jar";
    }
}
