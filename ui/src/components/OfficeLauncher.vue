<script setup lang="ts">
import { ref, computed } from 'vue';

interface Document {
  id: string;
  fileName: string;
  type: 'word' | 'excel' | 'powerpoint';
  base64Content?: string;
  lastModified?: string;
}

const props = defineProps<{
  document?: Document;
  documentId?: string;
  fileName?: string;
  base64Content?: string;
}>();

const emit = defineEmits<{
  (e: 'open', data: { documentId: string; fileName: string; app: string }): void;
  (e: 'error', message: string): void;
}>();

const isLoading = ref(false);

const documentType = computed(() => {
  const name = props.document?.fileName || props.fileName || '';
  const ext = name.toLowerCase().split('.').pop();

  if (['doc', 'docx'].includes(ext || '')) return 'word';
  if (['xls', 'xlsx'].includes(ext || '')) return 'excel';
  if (['ppt', 'pptx'].includes(ext || '')) return 'powerpoint';
  return 'word'; // default
});

const officeApps = {
  word: { name: 'Word', icon: 'W', color: '#2b579a', extensions: ['.doc', '.docx'] },
  excel: { name: 'Excel', icon: 'X', color: '#217346', extensions: ['.xls', '.xlsx'] },
  powerpoint: { name: 'PowerPoint', icon: 'P', color: '#d24726', extensions: ['.ppt', '.pptx'] }
};

const currentApp = computed(() => officeApps[documentType.value]);

const displayFileName = computed(() => {
  return props.document?.fileName || props.fileName || 'Document';
});

async function openWithOffice() {
  const docId = props.document?.id || props.documentId;
  const fileName = props.document?.fileName || props.fileName;

  if (!docId || !fileName) {
    emit('error', 'Document ID and filename are required');
    return;
  }

  isLoading.value = true;

  try {
    emit('open', {
      documentId: docId,
      fileName: fileName,
      app: documentType.value
    });
  } finally {
    isLoading.value = false;
  }
}

function openViaProtocol() {
  const docId = props.document?.id || props.documentId;
  if (!docId) {
    emit('error', 'Document ID is required');
    return;
  }

  const fileName = props.document?.fileName || props.fileName || 'document.docx';
  const url = `synth-office://open?docId=${encodeURIComponent(docId)}&fileName=${encodeURIComponent(fileName)}`;
  window.location.href = url;
}
</script>

<template>
  <div class="office-launcher">
    <div class="document-info">
      <div class="app-icon" :style="{ backgroundColor: currentApp.color }">
        {{ currentApp.icon }}
      </div>
      <div class="document-details">
        <span class="file-name">{{ displayFileName }}</span>
        <span class="app-name">{{ currentApp.name }} Document</span>
      </div>
    </div>

    <div class="actions">
      <button
        class="btn-open"
        :style="{ backgroundColor: currentApp.color }"
        :disabled="isLoading"
        @click="openWithOffice"
      >
        <span v-if="isLoading" class="spinner"></span>
        <span v-else>Open with {{ currentApp.name }}</span>
      </button>

      <button
        class="btn-protocol"
        @click="openViaProtocol"
        title="Open via custom protocol (requires registration)"
      >
        Open via Protocol
      </button>
    </div>

    <div class="supported-formats">
      <span class="label">Supported:</span>
      <span
        v-for="ext in currentApp.extensions"
        :key="ext"
        class="format-badge"
      >
        {{ ext }}
      </span>
    </div>
  </div>
</template>

<style scoped>
.office-launcher {
  font-family: 'Segoe UI', system-ui, sans-serif;
  background: #1e1e2e;
  border-radius: 12px;
  padding: 20px;
  color: #cdd6f4;
}

.document-info {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
}

.app-icon {
  width: 48px;
  height: 48px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  font-weight: bold;
  color: white;
}

.document-details {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.file-name {
  font-size: 16px;
  font-weight: 600;
  color: #f5f5f5;
}

.app-name {
  font-size: 13px;
  color: #a6adc8;
}

.actions {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.btn-open {
  flex: 1;
  padding: 12px 20px;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  color: white;
  cursor: pointer;
  transition: opacity 0.2s, transform 0.1s;
}

.btn-open:hover:not(:disabled) {
  opacity: 0.9;
  transform: translateY(-1px);
}

.btn-open:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-protocol {
  padding: 12px 16px;
  background: #313244;
  border: 1px solid #45475a;
  border-radius: 8px;
  font-size: 14px;
  color: #cdd6f4;
  cursor: pointer;
  transition: background 0.2s;
}

.btn-protocol:hover {
  background: #45475a;
}

.supported-formats {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
}

.label {
  color: #6c7086;
}

.format-badge {
  background: #313244;
  padding: 4px 8px;
  border-radius: 4px;
  color: #89b4fa;
}

.spinner {
  display: inline-block;
  width: 16px;
  height: 16px;
  border: 2px solid transparent;
  border-top-color: white;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>
