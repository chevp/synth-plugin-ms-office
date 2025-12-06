import { defineCustomElement } from 'vue';
import OfficeLauncher from './components/OfficeLauncher.vue';
import DocumentSync from './components/DocumentSync.vue';

// Convert Vue components to Web Components
const OfficeLauncherElement = defineCustomElement(OfficeLauncher);
const DocumentSyncElement = defineCustomElement(DocumentSync);

// Register custom elements
customElements.define('synth-office-launcher', OfficeLauncherElement);
customElements.define('synth-document-sync', DocumentSyncElement);

// Export for library usage
export { OfficeLauncher, DocumentSync };
