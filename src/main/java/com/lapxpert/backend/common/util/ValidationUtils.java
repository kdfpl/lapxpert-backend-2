package com.lapxpert.backend.common.util;

import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.regex.Pattern;

/**
 * Shared validation utility class with Vietnamese validation patterns.
 * Provides common validation logic for business entities across the LapXpert system.
 * All error messages are in Vietnamese to maintain consistency with business terminology.
 */
@Slf4j
public class ValidationUtils {

    // Vietnamese name validation pattern (allows Vietnamese characters, spaces, and common punctuation)
    private static final Pattern VIETNAMESE_NAME_PATTERN = Pattern.compile(
        "^[a-zA-ZÀÁÂÃÈÉÊÌÍÒÓÔÕÙÚĂĐĨŨƠàáâãèéêìíòóôõùúăđĩũơƯĂẠẢẤẦẨẪẬẮẰẲẴẶẸẺẼỀỀỂưăạảấầẩẫậắằẳẵặẹẻẽềềểỄỆỈỊỌỎỐỒỔỖỘỚỜỞỠỢỤỦỨỪễệỉịọỏốồổỗộớờởỡợụủứừỬỮỰỲỴÝỶỸửữựỳỵýỷỹ\\s\\.\\-']+$"
    );

    // Vietnamese phone number pattern (supports various Vietnamese formats)
    private static final Pattern VIETNAMESE_PHONE_PATTERN = Pattern.compile(
        "^(\\+84|84|0)(3[2-9]|5[689]|7[06-9]|8[1-689]|9[0-46-9])[0-9]{7}$"
    );

    // Email validation pattern
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );

    // Product code pattern (alphanumeric with optional hyphens)
    private static final Pattern PRODUCT_CODE_PATTERN = Pattern.compile(
        "^[A-Z0-9][A-Z0-9\\-]*[A-Z0-9]$"
    );

    /**
     * Validate entity ID
     */
    public static void validateId(Object id, String entityName) {
        if (id == null) {
            throw new IllegalArgumentException(String.format("ID %s không được để trống", entityName));
        }
        if (id instanceof Long && (Long) id <= 0) {
            throw new IllegalArgumentException(String.format("ID %s phải là số dương", entityName));
        }
        if (id instanceof String && ((String) id).trim().isEmpty()) {
            throw new IllegalArgumentException(String.format("ID %s không được để trống", entityName));
        }
    }

    /**
     * Validate Vietnamese name (person name, product name, etc.)
     */
    public static void validateVietnameseName(String name, String fieldName) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException(String.format("%s không được để trống", fieldName));
        }
        
        String trimmedName = name.trim();
        if (trimmedName.length() < 2) {
            throw new IllegalArgumentException(String.format("%s phải có ít nhất 2 ký tự", fieldName));
        }
        
        if (trimmedName.length() > 100) {
            throw new IllegalArgumentException(String.format("%s không được vượt quá 100 ký tự", fieldName));
        }
        
        if (!VIETNAMESE_NAME_PATTERN.matcher(trimmedName).matches()) {
            throw new IllegalArgumentException(String.format("%s chứa ký tự không hợp lệ", fieldName));
        }
    }

    /**
     * Validate email address
     */
    public static void validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email không được để trống");
        }
        
        String trimmedEmail = email.trim().toLowerCase();
        if (!EMAIL_PATTERN.matcher(trimmedEmail).matches()) {
            throw new IllegalArgumentException("Định dạng email không hợp lệ");
        }
        
        if (trimmedEmail.length() > 100) {
            throw new IllegalArgumentException("Email không được vượt quá 100 ký tự");
        }
    }

    /**
     * Validate Vietnamese phone number
     */
    public static void validatePhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Số điện thoại không được để trống");
        }
        
        String trimmedPhone = phoneNumber.trim().replaceAll("\\s+", "");
        if (!VIETNAMESE_PHONE_PATTERN.matcher(trimmedPhone).matches()) {
            throw new IllegalArgumentException("Số điện thoại không đúng định dạng Việt Nam");
        }
    }

    /**
     * Validate product code
     */
    public static void validateProductCode(String code, String fieldName) {
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException(String.format("%s không được để trống", fieldName));
        }
        
        String trimmedCode = code.trim().toUpperCase();
        if (trimmedCode.length() < 2 || trimmedCode.length() > 20) {
            throw new IllegalArgumentException(String.format("%s phải có độ dài từ 2-20 ký tự", fieldName));
        }
        
        if (!PRODUCT_CODE_PATTERN.matcher(trimmedCode).matches()) {
            throw new IllegalArgumentException(String.format("%s chỉ được chứa chữ cái, số và dấu gạch ngang", fieldName));
        }
    }

    /**
     * Validate amount/price
     */
    public static void validateAmount(BigDecimal amount, String fieldName) {
        if (amount == null) {
            throw new IllegalArgumentException(String.format("%s không được để trống", fieldName));
        }
        
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(String.format("%s không được âm", fieldName));
        }
        
        if (amount.compareTo(new BigDecimal("999999999")) > 0) {
            throw new IllegalArgumentException(String.format("%s vượt quá giới hạn cho phép", fieldName));
        }
    }

    /**
     * Validate positive amount (must be greater than 0)
     */
    public static void validatePositiveAmount(BigDecimal amount, String fieldName) {
        validateAmount(amount, fieldName);
        
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(String.format("%s phải lớn hơn 0", fieldName));
        }
    }

    /**
     * Validate date range
     */
    public static void validateDateRange(LocalDate startDate, LocalDate endDate, String fieldName) {
        if (startDate == null) {
            throw new IllegalArgumentException(String.format("Ngày bắt đầu %s không được để trống", fieldName));
        }
        
        if (endDate == null) {
            throw new IllegalArgumentException(String.format("Ngày kết thúc %s không được để trống", fieldName));
        }
        
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException(String.format("Ngày bắt đầu %s không được sau ngày kết thúc", fieldName));
        }
    }

    /**
     * Validate datetime range
     */
    public static void validateDateTimeRange(LocalDateTime startDateTime, LocalDateTime endDateTime, String fieldName) {
        if (startDateTime == null) {
            throw new IllegalArgumentException(String.format("Thời gian bắt đầu %s không được để trống", fieldName));
        }
        
        if (endDateTime == null) {
            throw new IllegalArgumentException(String.format("Thời gian kết thúc %s không được để trống", fieldName));
        }
        
        if (startDateTime.isAfter(endDateTime)) {
            throw new IllegalArgumentException(String.format("Thời gian bắt đầu %s không được sau thời gian kết thúc", fieldName));
        }
    }

    /**
     * Validate string length
     */
    public static void validateStringLength(String value, String fieldName, int minLength, int maxLength) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(String.format("%s không được để trống", fieldName));
        }
        
        String trimmedValue = value.trim();
        if (trimmedValue.length() < minLength) {
            throw new IllegalArgumentException(String.format("%s phải có ít nhất %d ký tự", fieldName, minLength));
        }
        
        if (trimmedValue.length() > maxLength) {
            throw new IllegalArgumentException(String.format("%s không được vượt quá %d ký tự", fieldName, maxLength));
        }
    }

    /**
     * Validate percentage (0-100)
     */
    public static void validatePercentage(BigDecimal percentage, String fieldName) {
        if (percentage == null) {
            throw new IllegalArgumentException(String.format("%s không được để trống", fieldName));
        }
        
        if (percentage.compareTo(BigDecimal.ZERO) < 0 || percentage.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException(String.format("%s phải trong khoảng 0-100", fieldName));
        }
    }

    /**
     * Validate quantity (non-negative integer)
     */
    public static void validateQuantity(Integer quantity, String fieldName) {
        if (quantity == null) {
            throw new IllegalArgumentException(String.format("%s không được để trống", fieldName));
        }
        
        if (quantity < 0) {
            throw new IllegalArgumentException(String.format("%s không được âm", fieldName));
        }
    }

    /**
     * Validate positive quantity (must be greater than 0)
     */
    public static void validatePositiveQuantity(Integer quantity, String fieldName) {
        validateQuantity(quantity, fieldName);
        
        if (quantity <= 0) {
            throw new IllegalArgumentException(String.format("%s phải lớn hơn 0", fieldName));
        }
    }
}
