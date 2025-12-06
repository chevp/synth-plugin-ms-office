<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue';

interface TrackedDocument {
  documentId: string;
  fileName: string;
  filePath: string;
  openedAt: string;
  lastModified: string;
  synced: boolean;
}

const props = defineProps<{
  pollInterval?: number;
}>();

const emit = defineEmits<{
  (e: 'sync', documentId: string): void;
  (e: 'syncAll'): void;
  (e: 'remove', documentId: string): void;
  (e: 'documentsChanged', documents: TrackedDocument[]): void;
}>();

const documents = ref<TrackedDocument[]>([]);
const isLoading = ref(false);
const lastUpdate = ref<string>('');
let pollTimer: ReturnType<typeof setInterval> | null = null;

const unsyncedCount = computed(() =>
  documents.value.filter(d => !d.synced).length
);

function formatTime(isoString: string): string {
  if (!isoString) return 'Unknown';
  const date = new Date(isoString);
  return date.toLocaleString();
}

function getAppIcon(fileName: string): { icon: string; color: string } {
  const ext = fileName.toLowerCase().split('.').pop();
  if (['doc', 'docx'].includes(ext || '')) return { icon: 'W', color: '#2b579a' };
  if (['xls', 'xlsx'].includes(ext || '')) return { icon: 'X', color: '#217346' };
  if (['ppt', 'pptx'].includes(ext || '')) return { icon: 'P', color: '#d24726' };
  return { icon: 'D', color: '#6c7086' };
}

async function syncDocument(docId: string) {
  emit('sync', docId);
}

async function syncAllDocuments() {
  emit('syncAll');
}

function removeDocument(docId: string) {
  emit('remove', docId);
  documents.value = documents.value.filter(d => d.documentId !== docId);
}

function updateDocuments(newDocs: TrackedDocument[]) {
  documents.value = newDocs;
  lastUpdate.value = new Date().toISOString();
  emit('documentsChanged', newDocs);
}

// Expose method for parent to update documents
defineExpose({
  updateDocuments
});

onMounted(() => {
  const interval = props.pollInterval || 5000;
  pollTimer = setInterval(() => {
    // Parent should call updateDocuments with fresh data
  }, interval);
});

onUnmounted(() => {
  if (pollTimer) {
    clearInterval(pollTimer);
  }
});
</script>

<template>
  <div class="document-sync">
    <div class="header">
      <h3>Document Sync</h3>
      <div class="header-actions">
        <span v-if="unsyncedCount > 0" class="badge-unsynced">
          {{ unsyncedCount }} unsynced
        </span>
        <button
          class="btn-sync-all"
          :disabled="unsyncedCount === 0"
          @click="syncAllDocuments"
        >
          Sync All
        </button>
      </div>
    </div>

    <div v-if="documents.length === 0" class="empty-state">
      <div class="empty-icon">📄</div>
      <p>No documents currently open</p>
      <span class="hint">Open a document with Office to track changes</span>
    </div>

    <div v-else class="document-list">
      <div
        v-for="doc in documents"
        :key="doc.documentId"
        class="document-item"
        :class="{ 'needs-sync': !doc.synced }"
      >
        <div
          class="doc-icon"
          :style="{ backgroundColor: getAppIcon(doc.fileName).color }"
        >
          {{ getAppIcon(doc.fileName).icon }}
        </div>

        <div class="doc-info">
          <span class="doc-name">{{ doc.fileName }}</span>
          <span class="doc-meta">
            Opened: {{ formatTime(doc.openedAt) }}
          </span>
        </div>

        <div class="doc-status">
          <span v-if="doc.synced" class="status synced">
            ✓ Synced
          </span>
          <span v-else class="status modified">
            ● Modified
          </span>
        </div>

        <div class="doc-actions">
          <button
            v-if="!doc.synced"
            class="btn-sync"
            @click="syncDocument(doc.documentId)"
            title="Save changes back to database"
          >
            Sync
          </button>
          <button
            class="btn-remove"
            @click="removeDocument(doc.documentId)"
            title="Stop tracking this document"
          >
            ×
          </button>
        </div>
      </div>
    </div>

    <div v-if="lastUpdate" class="footer">
      Last updated: {{ formatTime(lastUpdate) }}
    </div>
  </div>
</template>

<style scoped>
.document-sync {
  font-family: 'Segoe UI', system-ui, sans-serif;
  background: #1e1e2e;
  border-radius: 12px;
  padding: 16px;
  color: #cdd6f4;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #313244;
}

.header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.badge-unsynced {
  background: #f38ba8;
  color: #1e1e2e;
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
}

.btn-sync-all {
  padding: 8px 14px;
  background: #89b4fa;
  border: none;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 600;
  color: #1e1e2e;
  cursor: pointer;
  transition: opacity 0.2s;
}

.btn-sync-all:hover:not(:disabled) {
  opacity: 0.9;
}

.btn-sync-all:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.empty-state {
  text-align: center;
  padding: 32px 16px;
  color: #6c7086;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 12px;
  opacity: 0.5;
}

.empty-state p {
  margin: 0 0 8px 0;
  font-size: 14px;
}

.hint {
  font-size: 12px;
  color: #585b70;
}

.document-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.document-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: #313244;
  border-radius: 8px;
  transition: background 0.2s;
}

.document-item:hover {
  background: #45475a;
}

.document-item.needs-sync {
  border-left: 3px solid #f9e2af;
}

.doc-icon {
  width: 36px;
  height: 36px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: bold;
  color: white;
  flex-shrink: 0;
}

.doc-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.doc-name {
  font-size: 14px;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.doc-meta {
  font-size: 11px;
  color: #6c7086;
}

.doc-status {
  flex-shrink: 0;
}

.status {
  font-size: 12px;
  padding: 4px 8px;
  border-radius: 4px;
}

.status.synced {
  background: rgba(166, 227, 161, 0.2);
  color: #a6e3a1;
}

.status.modified {
  background: rgba(249, 226, 175, 0.2);
  color: #f9e2af;
}

.doc-actions {
  display: flex;
  gap: 6px;
  flex-shrink: 0;
}

.btn-sync {
  padding: 6px 12px;
  background: #89b4fa;
  border: none;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 600;
  color: #1e1e2e;
  cursor: pointer;
}

.btn-sync:hover {
  opacity: 0.9;
}

.btn-remove {
  width: 28px;
  height: 28px;
  background: transparent;
  border: 1px solid #45475a;
  border-radius: 4px;
  font-size: 16px;
  color: #6c7086;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}

.btn-remove:hover {
  background: #f38ba8;
  border-color: #f38ba8;
  color: #1e1e2e;
}

.footer {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #313244;
  font-size: 11px;
  color: #585b70;
  text-align: center;
}
</style>
