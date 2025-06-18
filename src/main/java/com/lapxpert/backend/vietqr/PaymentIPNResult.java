package com.lapxpert.backend.vietqr;

/**
 * Result class for VietQR IPN (Instant Payment Notification) processing.
 * Provides structured response for payment verification results.
 */
public class PaymentIPNResult {
    
    private final boolean valid;
    private final boolean successful;
    private final String transactionRef;
    private final String status;
    private final String errorMessage;
    
    private PaymentIPNResult(boolean valid, boolean successful, String transactionRef, String status, String errorMessage) {
        this.valid = valid;
        this.successful = successful;
        this.transactionRef = transactionRef;
        this.status = status;
        this.errorMessage = errorMessage;
    }
    
    /**
     * Create a successful payment result.
     * 
     * @param transactionRef Transaction reference
     * @param status Payment status
     * @return PaymentIPNResult instance
     */
    public static PaymentIPNResult success(String transactionRef, String status) {
        return new PaymentIPNResult(true, true, transactionRef, status, null);
    }
    
    /**
     * Create a failed payment result.
     * 
     * @param transactionRef Transaction reference
     * @param status Payment status
     * @return PaymentIPNResult instance
     */
    public static PaymentIPNResult failed(String transactionRef, String status) {
        return new PaymentIPNResult(true, false, transactionRef, status, null);
    }
    
    /**
     * Create an invalid request result.
     * 
     * @param errorMessage Error message
     * @return PaymentIPNResult instance
     */
    public static PaymentIPNResult invalid(String errorMessage) {
        return new PaymentIPNResult(false, false, null, null, errorMessage);
    }
    
    /**
     * Create an error result.
     * 
     * @param errorMessage Error message
     * @return PaymentIPNResult instance
     */
    public static PaymentIPNResult error(String errorMessage) {
        return new PaymentIPNResult(false, false, null, null, errorMessage);
    }
    
    // Getters
    public boolean isValid() {
        return valid;
    }
    
    public boolean isSuccessful() {
        return successful;
    }
    
    public String getTransactionRef() {
        return transactionRef;
    }
    
    public String getStatus() {
        return status;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    @Override
    public String toString() {
        return "PaymentIPNResult{" +
                "valid=" + valid +
                ", successful=" + successful +
                ", transactionRef='" + transactionRef + '\'' +
                ", status='" + status + '\'' +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }
}
