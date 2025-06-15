package com.lapxpert.backend.vietqr.domain;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * VietQR payment service for handling QR code generation and payment verification
 * Implements VietQR bank transfer integration following Vietnamese business requirements
 */
@Slf4j
@Service
public class VietQRService {

    /**
     * Create VietQR payment URL for bank transfer
     * @param amount Payment amount in VND
     * @param orderInfo Order information
     * @param orderId Order ID for correlation
     * @return VietQR image URL for QR code display
     */
    public String createPaymentUrl(long amount, String orderInfo, String orderId) {
        try {
            // Generate payment reference for tracking
            String paymentRef = VietQRConfig.generatePaymentReference(orderId);
            
            // Format payment description with order information
            String description = VietQRConfig.formatPaymentDescription(orderId, orderInfo);
            
            // Generate VietQR URL using configured bank details
            String qrUrl = VietQRConfig.generateVietQRUrl(
                VietQRConfig.vietqr_BankId,
                VietQRConfig.vietqr_AccountNo,
                amount,
                description,
                VietQRConfig.vietqr_Template
            );
            
            log.info("VietQR payment URL created for order {}: {}", orderId, qrUrl);
            return qrUrl;
            
        } catch (Exception e) {
            log.error("Error creating VietQR payment URL for order {}: {}", orderId, e.getMessage(), e);
            throw new RuntimeException("Error creating VietQR payment URL: " + e.getMessage(), e);
        }
    }

    /**
     * Verify VietQR payment through bank transfer confirmation
     * Note: VietQR payments are verified through bank webhooks or manual confirmation
     * @param transactionData Transaction data from bank or manual input
     * @return Payment verification result (1: success, 0: failed, -1: invalid)
     */
    public int verifyPayment(Map<String, String> transactionData) {
        try {
            String orderId = transactionData.get("orderId");
            String amount = transactionData.get("amount");
            String transactionId = transactionData.get("transactionId");
            String bankTransactionId = transactionData.get("bankTransactionId");
            String status = transactionData.get("status");
            
            // Validate required fields
            if (orderId == null || amount == null || transactionId == null) {
                log.error("Missing required fields in VietQR transaction data");
                return -1; // Invalid
            }
            
            // Check transaction status
            if ("SUCCESS".equals(status) || "COMPLETED".equals(status)) {
                log.info("VietQR payment successful for order {}, bank transaction ID: {}", orderId, bankTransactionId);
                return 1; // Success
            } else if ("FAILED".equals(status) || "CANCELLED".equals(status)) {
                log.warn("VietQR payment failed for order {}: {}", orderId, status);
                return 0; // Failed
            } else {
                log.warn("Unknown VietQR payment status for order {}: {}", orderId, status);
                return 0; // Failed
            }
            
        } catch (Exception e) {
            log.error("Error verifying VietQR payment: {}", e.getMessage(), e);
            return -1; // Invalid
        }
    }

    /**
     * Check VietQR payment status (manual verification helper)
     * @param orderId Order ID to check
     * @param expectedAmount Expected payment amount
     * @param bankTransactionId Bank transaction ID for verification
     * @return Payment status check result
     */
    public Map<String, Object> checkPaymentStatus(String orderId, long expectedAmount, String bankTransactionId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // In a real implementation, this would query the bank API or database
            // For now, we'll return a structure that can be used for manual verification
            
            result.put("orderId", orderId);
            result.put("expectedAmount", expectedAmount);
            result.put("bankTransactionId", bankTransactionId);
            result.put("status", "PENDING_VERIFICATION");
            result.put("message", "VietQR payment requires manual verification through bank statement");
            result.put("verificationInstructions", 
                "Please verify bank transfer with amount " + expectedAmount + " VND for order " + orderId);
            
            log.info("VietQR payment status check for order {}: PENDING_VERIFICATION", orderId);
            return result;
            
        } catch (Exception e) {
            log.error("Error checking VietQR payment status for order {}: {}", orderId, e.getMessage(), e);
            result.put("status", "ERROR");
            result.put("message", "Error checking payment status: " + e.getMessage());
            return result;
        }
    }

    /**
     * Generate VietQR payment instructions for customer
     * @param orderId Order ID
     * @param amount Payment amount
     * @param qrUrl QR code URL
     * @return Payment instructions
     */
    public Map<String, Object> generatePaymentInstructions(String orderId, long amount, String qrUrl) {
        Map<String, Object> instructions = new HashMap<>();
        
        try {
            instructions.put("qrUrl", qrUrl);
            instructions.put("bankId", VietQRConfig.vietqr_BankId);
            instructions.put("accountNo", VietQRConfig.vietqr_AccountNo);
            instructions.put("accountName", VietQRConfig.vietqr_AccountName);
            instructions.put("amount", amount);
            instructions.put("orderId", orderId);
            instructions.put("description", VietQRConfig.formatPaymentDescription(orderId, ""));
            
            // Payment instructions in Vietnamese
            instructions.put("instructions", new String[]{
                "1. Mở ứng dụng ngân hàng trên điện thoại",
                "2. Quét mã QR hoặc chuyển khoản thủ công",
                "3. Kiểm tra thông tin chuyển khoản",
                "4. Xác nhận giao dịch",
                "5. Lưu lại biên lai để đối chiếu"
            });
            
            instructions.put("manualTransferInfo", Map.of(
                "bankName", "Ngân hàng theo mã " + VietQRConfig.vietqr_BankId,
                "accountNumber", VietQRConfig.vietqr_AccountNo,
                "accountName", VietQRConfig.vietqr_AccountName,
                "amount", amount + " VND",
                "content", VietQRConfig.formatPaymentDescription(orderId, "")
            ));
            
            instructions.put("timeout", "15 phút"); // Payment timeout
            instructions.put("note", "Vui lòng chuyển khoản đúng số tiền và nội dung để được xử lý nhanh chóng");
            
            return instructions;
            
        } catch (Exception e) {
            log.error("Error generating VietQR payment instructions for order {}: {}", orderId, e.getMessage(), e);
            instructions.put("error", "Không thể tạo hướng dẫn thanh toán");
            return instructions;
        }
    }

    /**
     * Validate VietQR transaction data
     * @param transactionData Transaction data to validate
     * @return true if valid, false otherwise
     */
    public boolean validateTransactionData(Map<String, String> transactionData) {
        try {
            // Check required fields
            String orderId = transactionData.get("orderId");
            String amount = transactionData.get("amount");
            String transactionId = transactionData.get("transactionId");
            
            if (orderId == null || orderId.trim().isEmpty()) {
                log.error("VietQR transaction validation failed: missing orderId");
                return false;
            }
            
            if (amount == null || amount.trim().isEmpty()) {
                log.error("VietQR transaction validation failed: missing amount");
                return false;
            }
            
            if (transactionId == null || transactionId.trim().isEmpty()) {
                log.error("VietQR transaction validation failed: missing transactionId");
                return false;
            }
            
            // Validate amount is numeric
            try {
                Long.parseLong(amount);
            } catch (NumberFormatException e) {
                log.error("VietQR transaction validation failed: invalid amount format: {}", amount);
                return false;
            }
            
            return true;
            
        } catch (Exception e) {
            log.error("Error validating VietQR transaction data: {}", e.getMessage(), e);
            return false;
        }
    }
}
