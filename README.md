# synth-plugin-ms-office

Synth Plugin for Microsoft Office integration. Open Word, Excel, and PowerPoint documents directly from synth-platform-web using the native Office applications.

## Features

- **Open Documents** - Launch Word/Excel/PowerPoint with Base64 documents from database
- **Auto-Sync** - Track document changes and sync back to database
- **Protocol Handler** - Register `synth-office://` URL scheme for browser integration
- **Web Components** - Vue 3 UI components for document management

## Architecture

```
┌─────────────────────┐     ┌──────────────────────┐     ┌─────────────┐
│  synth-platform-web │────▶│  MCP Tools (Java)    │────▶│ Office.exe  │
│  (Base64 in SQLite) │     │  - Open/Save/Track   │     │             │
└─────────────────────┘     └──────────────────────┘     └─────────────┘
         │                           │                          │
         │                           ▼                          │
         │                  ┌──────────────────┐                │
         │                  │  Temp Files      │                │
         │                  │  + File Watcher  │◀──────-────────┘
         │                  └──────────────────┘
         │                           │
         └───────────────────────────┘
              Sync changes back
```

## MCP Tools

| Tool | Description |
|------|-------------|
| `office_open_document` | Open a document with Office (from Base64 or file path) |
| `office_save_document` | Save changes back to Base64 for database storage |
| `office_list_recent` | List tracked documents with sync status |
| `office_register_protocol` | Register `synth-office://` URL protocol |

## Web Components

| Component | Tag | Description |
|-----------|-----|-------------|
| OfficeLauncher | `<synth-office-launcher>` | Document open button with Office icon |
| DocumentSync | `<synth-document-sync>` | Sync status panel for tracked documents |

## Build

### Backend (Java MCP Tools)

```bash
cd mcp-tools
mvn clean package
```

Output: `mcp-tools/target/synth-ms-office-1.0.0.jar`

### Frontend (Vue Web Components)

```bash
cd ui
npm install
npm run build
```

Output: `ui/dist/synth-ms-office.js`

## Usage

### Opening a Document

```typescript
// Via MCP Tool
const result = await mcpClient.callTool('office_open_document', {
  documentId: 'doc-123',
  fileName: 'report.docx',
  base64Content: 'UEsDBBQAAAAI...' // Base64 encoded document
});
```

### Syncing Changes Back

```typescript
// Get updated Base64 content
const result = await mcpClient.callTool('office_save_document', {
  documentId: 'doc-123'
});

// result.base64Content contains the updated document
// Save to your SQLite database
```

### Protocol Handler

1. Register the protocol:
```typescript
await mcpClient.callTool('office_register_protocol', {
  action: 'register',
  serverUrl: 'http://localhost:4200'
});
```

2. Open documents via URL:
```
synth-office://open?docId=123&fileName=report.docx
```

## Requirements

- Java 17+
- Node.js 18+
- Microsoft Office (Word, Excel, PowerPoint)
- Windows, macOS, or Linux (with Wine for Office)

## License

Private - Synth Project
