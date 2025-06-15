<template>
  <div class="websocket-test-container">
    <Card>
      <template #title>
        <div class="flex align-items-center gap-2">
          <i class="pi pi-wifi"></i>
          WebSocket Connection Test
        </div>
      </template>

      <template #content>
        <!-- Connection Status -->
        <div class="mb-4">
          <h5>Connection Status</h5>
          <div class="flex align-items-center gap-2 mb-2">
            <Badge
              :value="connectionStatus.text"
              :severity="connectionStatus.severity"
            />
            <Button
              v-if="!isConnected"
              @click="connect"
              label="Connect"
              icon="pi pi-play"
              size="small"
            />
            <Button
              v-if="isConnected"
              @click="disconnect"
              label="Disconnect"
              icon="pi pi-stop"
              size="small"
              severity="secondary"
            />
            <Button
              @click="reconnect"
              label="Reconnect"
              icon="pi pi-refresh"
              size="small"
              severity="info"
            />
          </div>
          <div v-if="connectionError" class="text-red-500 text-sm">
            {{ connectionError }}
          </div>
        </div>

        <!-- Test Message Sending -->
        <div class="mb-4">
          <h5>Send Test Message</h5>
          <div class="flex gap-2 mb-2">
            <InputText
              v-model="testMessage"
              placeholder="Enter test message"
              class="flex-1"
            />
            <Button
              @click="sendTestMessage"
              label="Send"
              icon="pi pi-send"
              :disabled="!isConnected"
            />
          </div>
        </div>

        <!-- Message History -->
        <div class="mb-4">
          <div class="flex justify-content-between align-items-center mb-2">
            <h5>Message History ({{ messageHistory.length }})</h5>
            <Button
              @click="clearHistory"
              label="Clear"
              icon="pi pi-trash"
              size="small"
              severity="secondary"
            />
          </div>

          <div class="message-history" style="max-height: 300px; overflow-y: auto;">
            <div
              v-for="message in messageHistory"
              :key="message.id"
              class="message-item p-2 mb-2 border-round surface-100"
            >
              <div class="flex justify-content-between align-items-start">
                <div class="flex-1">
                  <div class="font-semibold text-sm">{{ message.topic || 'Unknown Topic' }}</div>
                  <div class="text-sm">{{ formatMessage(message) }}</div>
                </div>
                <div class="text-xs text-500">
                  {{ formatTime(message.timestamp) }}
                </div>
              </div>
            </div>

            <div v-if="messageHistory.length === 0" class="text-center text-500 py-4">
              No messages received yet
            </div>
          </div>
        </div>

        <!-- Connection Info -->
        <div class="mb-4">
          <h5>Connection Info</h5>
          <div class="text-sm">
            <div><strong>WebSocket URL:</strong> {{ wsUrl }}</div>
            <div><strong>Protocol:</strong> STOMP over WebSocket with SockJS</div>
            <div><strong>Authentication:</strong> None (Public Access)</div>
            <div><strong>Subscribed Topics:</strong></div>
            <ul class="ml-3">
              <li>/topic/phieu-giam-gia/expired</li>
              <li>/topic/phieu-giam-gia/new</li>
              <li>/topic/phieu-giam-gia/alternatives</li>
              <li>/topic/gia-san-pham/updates</li>
            </ul>
          </div>
        </div>
      </template>
    </Card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRealTimeOrderManagement } from '@/composables/useRealTimeOrderManagement'
import Card from 'primevue/card'
import Badge from 'primevue/badge'
import Button from 'primevue/button'
import InputText from 'primevue/inputtext'

// WebSocket composable
const {
  isConnected,
  connectionStatus,
  connectionError,
  lastMessage,
  messageHistory,
  connect,
  disconnect,
  reconnect,
  sendMessage,
  clearMessageHistory
} = useRealTimeOrderManagement()

// Test message input
const testMessage = ref('')

// WebSocket URL for display
const wsUrl = import.meta.env.VITE_WS_URL || 'ws://localhost:8080/ws'

// Send test message
const sendTestMessage = () => {
  if (testMessage.value.trim()) {
    sendMessage({
      type: 'TEST_MESSAGE',
      content: testMessage.value,
      timestamp: new Date().toISOString()
    }, '/app/test-message')
    testMessage.value = ''
  }
}

// Clear message history
const clearHistory = () => {
  clearMessageHistory()
}

// Format message for display
const formatMessage = (message) => {
  if (typeof message === 'string') return message
  if (message.content) return message.content
  if (message.message) return message.message
  return JSON.stringify(message, null, 2)
}

// Format timestamp
const formatTime = (timestamp) => {
  if (!timestamp) return ''
  const date = new Date(timestamp)
  return date.toLocaleTimeString()
}
</script>

<style scoped>
.websocket-test-container {
  max-width: 800px;
  margin: 0 auto;
  padding: 1rem;
}

.message-item {
  font-family: 'Courier New', monospace;
}

.message-history {
  border: 1px solid var(--surface-border);
  border-radius: var(--border-radius);
  padding: 0.5rem;
}
</style>
