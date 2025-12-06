package synth.msoffice;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import synth.msoffice.tools.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * MCP Server for Microsoft Office integration.
 * Handles document opening, saving, and sync operations via stdio.
 */
public class MsOfficeTools {

    private static final Gson gson = new Gson();
    private static final String PROTOCOL_VERSION = "2024-11-05";
    private static final String SERVER_NAME = "synth-ms-office";
    private static final String SERVER_VERSION = "1.0.0";

    private final OpenDocumentTool openDocumentTool;
    private final SaveDocumentTool saveDocumentTool;
    private final ListRecentTool listRecentTool;
    private final RegisterProtocolTool registerProtocolTool;

    public MsOfficeTools() {
        this.openDocumentTool = new OpenDocumentTool();
        this.saveDocumentTool = new SaveDocumentTool();
        this.listRecentTool = new ListRecentTool();
        this.registerProtocolTool = new RegisterProtocolTool();
    }

    public static void main(String[] args) {
        new MsOfficeTools().run();
    }

    public void run() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
             PrintWriter writer = new PrintWriter(System.out, true, StandardCharsets.UTF_8)) {

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) continue;

                JsonObject request = gson.fromJson(line, JsonObject.class);
                JsonObject response = handleRequest(request);
                writer.println(gson.toJson(response));
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    private JsonObject handleRequest(JsonObject request) {
        String method = request.has("method") ? request.get("method").getAsString() : "";
        int id = request.has("id") ? request.get("id").getAsInt() : 0;

        return switch (method) {
            case "initialize" -> handleInitialize(id);
            case "tools/list" -> handleListTools(id);
            case "tools/call" -> handleCallTool(id, request.getAsJsonObject("params"));
            default -> createErrorResponse(id, -32601, "Method not found: " + method);
        };
    }

    private JsonObject handleInitialize(int id) {
        JsonObject result = new JsonObject();
        result.addProperty("protocolVersion", PROTOCOL_VERSION);

        JsonObject serverInfo = new JsonObject();
        serverInfo.addProperty("name", SERVER_NAME);
        serverInfo.addProperty("version", SERVER_VERSION);
        result.add("serverInfo", serverInfo);

        JsonObject capabilities = new JsonObject();
        JsonObject tools = new JsonObject();
        capabilities.add("tools", tools);
        result.add("capabilities", capabilities);

        return createSuccessResponse(id, result);
    }

    private JsonObject handleListTools(int id) {
        List<Map<String, Object>> tools = Arrays.asList(
            openDocumentTool.getToolDefinition(),
            saveDocumentTool.getToolDefinition(),
            listRecentTool.getToolDefinition(),
            registerProtocolTool.getToolDefinition()
        );

        JsonObject result = new JsonObject();
        result.add("tools", gson.toJsonTree(tools));
        return createSuccessResponse(id, result);
    }

    private JsonObject handleCallTool(int id, JsonObject params) {
        String toolName = params.has("name") ? params.get("name").getAsString() : "";
        Map<String, Object> arguments = params.has("arguments")
            ? gson.fromJson(params.get("arguments"), Map.class)
            : new HashMap<>();

        try {
            Object result = switch (toolName) {
                case "office_open_document" -> openDocumentTool.execute(arguments);
                case "office_save_document" -> saveDocumentTool.execute(arguments);
                case "office_list_recent" -> listRecentTool.execute(arguments);
                case "office_register_protocol" -> registerProtocolTool.execute(arguments);
                default -> throw new IllegalArgumentException("Unknown tool: " + toolName);
            };

            JsonObject content = new JsonObject();
            content.addProperty("type", "text");
            content.addProperty("text", result.toString());

            JsonObject toolResult = new JsonObject();
            toolResult.add("content", gson.toJsonTree(Collections.singletonList(content)));

            return createSuccessResponse(id, toolResult);
        } catch (Exception e) {
            return createErrorResponse(id, -32000, e.getMessage());
        }
    }

    private JsonObject createSuccessResponse(int id, JsonObject result) {
        JsonObject response = new JsonObject();
        response.addProperty("jsonrpc", "2.0");
        response.addProperty("id", id);
        response.add("result", result);
        return response;
    }

    private JsonObject createErrorResponse(int id, int code, String message) {
        JsonObject error = new JsonObject();
        error.addProperty("code", code);
        error.addProperty("message", message);

        JsonObject response = new JsonObject();
        response.addProperty("jsonrpc", "2.0");
        response.addProperty("id", id);
        response.add("error", error);
        return response;
    }
}
