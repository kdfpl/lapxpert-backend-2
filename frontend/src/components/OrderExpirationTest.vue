<template>
  <div class="order-expiration-test p-4 border rounded-lg bg-surface-50">
    <h3 class="font-semibold mb-4">Test Order Expiration Notifications</h3>
    
    <div class="space-y-3">
      <!-- Test Buttons -->
      <div class="flex gap-2 flex-wrap">
        <Button 
          label="Test Expiring Soon" 
          icon="pi pi-clock" 
          size="small"
          severity="warn"
          @click="testExpiringSoon"
        />
        <Button 
          label="Test Expired" 
          icon="pi pi-times-circle" 
          size="small"
          severity="danger"
          @click="testExpired"
        />
        <Button 
          label="Test Inventory Release" 
          icon="pi pi-box" 
          size="small"
          severity="info"
          @click="testInventoryRelease"
        />
        <Button 
          label="Test Status Change" 
          icon="pi pi-refresh" 
          size="small"
          severity="secondary"
          @click="testStatusChange"
        />
      </div>

      <!-- Current Status -->
      <div class="text-sm text-surface-600">
        <div>Expiring Orders: {{ expiringOrders.length }}</div>
        <div>Expired Orders: {{ expiredOrders.length }}</div>
        <div>Critical Orders: {{ criticalExpiringOrders.length }}</div>
        <div>Has Updates: {{ hasExpirationUpdates ? 'Yes' : 'No' }}</div>
      </div>

      <!-- Clear Button -->
      <Button 
        label="Clear All" 
        icon="pi pi-trash" 
        size="small"
        text
        @click="clearExpirationHistory"
      />
    </div>
  </div>
</template>

<script setup>
import Button from 'primevue/button'
import { useOrderExpiration } from '@/composables/useOrderExpiration'

// Order expiration composable
const {
  expiringOrders,
  expiredOrders,
  criticalExpiringOrders,
  hasExpirationUpdates,
  clearExpirationHistory,
  processExpirationMessage,
  handleOrderExpiringSoon,
  handleOrderExpired,
  handleInventoryReleased,
  handleOrderStatusChanged
} = useOrderExpiration()

// Test functions
const testExpiringSoon = () => {
  const testMessage = {
    type: 'ORDER_EXPIRING_SOON',
    orderId: Date.now(),
    orderCode: `DH${Math.floor(Math.random() * 1000).toString().padStart(3, '0')}`,
    customerName: 'Nguyễn Văn A',
    totalAmount: 1500000,
    expirationTime: new Date(Date.now() + 30 * 60 * 1000), // 30 minutes from now
    remainingMinutes: 30,
    message: 'Đơn hàng sắp hết hạn trong 30 phút',
    timestamp: new Date().toISOString()
  }
  
  handleOrderExpiringSoon(testMessage)
}

const testExpired = () => {
  const testMessage = {
    type: 'ORDER_EXPIRED',
    orderId: Date.now(),
    orderCode: `DH${Math.floor(Math.random() * 1000).toString().padStart(3, '0')}`,
    customerName: 'Trần Thị B',
    totalAmount: 2000000,
    expiredAt: new Date(),
    reason: 'Hết hạn thanh toán 24 giờ',
    message: 'Đơn hàng đã hết hạn',
    timestamp: new Date().toISOString()
  }
  
  handleOrderExpired(testMessage)
}

const testInventoryRelease = () => {
  const testMessage = {
    type: 'INVENTORY_RELEASED',
    orderId: Date.now(),
    orderCode: `DH${Math.floor(Math.random() * 1000).toString().padStart(3, '0')}`,
    releasedItems: [
      { productName: 'iPhone 15 Pro', serialNumber: '1111', sku: 'IP15P-BLK' },
      { productName: 'MacBook Pro', serialNumber: '2222', sku: 'MBP-SLV' }
    ],
    totalItemsReleased: 2,
    message: 'Đã giải phóng 2 sản phẩm về kho',
    timestamp: new Date().toISOString()
  }
  
  handleInventoryReleased(testMessage)
}

const testStatusChange = () => {
  const testMessage = {
    type: 'ORDER_STATUS_CHANGED',
    orderId: Date.now(),
    orderCode: `DH${Math.floor(Math.random() * 1000).toString().padStart(3, '0')}`,
    oldStatus: 'CHO_THANH_TOAN',
    newStatus: 'DA_HUY',
    reason: 'Hết hạn thanh toán',
    message: 'Trạng thái đơn hàng đã thay đổi do hết hạn',
    timestamp: new Date().toISOString()
  }
  
  handleOrderStatusChanged(testMessage)
}
</script>

<style scoped>
.order-expiration-test {
  max-width: 500px;
}
</style>
