package com.lapxpert.backend.common.util;

import lombok.extern.slf4j.Slf4j;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Utility class for IP address detection and validation
 * Provides common IP address handling for payment gateways
 */
@Slf4j
public class IpAddressUtils {

    private static final String[] IP_HEADER_CANDIDATES = {
        "X-Forwarded-For",
        "Proxy-Client-IP",
        "WL-Proxy-Client-IP",
        "HTTP_X_FORWARDED_FOR",
        "HTTP_X_FORWARDED",
        "HTTP_X_CLUSTER_CLIENT_IP",
        "HTTP_CLIENT_IP",
        "HTTP_FORWARDED_FOR",
        "HTTP_FORWARDED",
        "HTTP_VIA",
        "REMOTE_ADDR"
    };

    /**
     * Get client IP address from HTTP request
     * Handles various proxy headers and load balancer configurations
     * 
     * @param request HTTP servlet request
     * @return Client IP address
     */
    public static String getClientIpAddress(HttpServletRequest request) {
        if (request == null) {
            log.warn("HttpServletRequest is null, returning unknown IP");
            return "unknown";
        }

        // Check various headers for the real client IP
        for (String header : IP_HEADER_CANDIDATES) {
            String ipList = request.getHeader(header);
            if (ipList != null && !ipList.isEmpty() && !"unknown".equalsIgnoreCase(ipList)) {
                // X-Forwarded-For can contain multiple IPs, take the first one
                String ip = ipList.split(",")[0].trim();
                if (isValidIPAddress(ip)) {
                    log.debug("Found client IP {} from header {}", ip, header);
                    return ip;
                }
            }
        }

        // Fallback to remote address
        String remoteAddr = request.getRemoteAddr();
        if (remoteAddr != null && !remoteAddr.isEmpty()) {
            log.debug("Using remote address as client IP: {}", remoteAddr);
            return remoteAddr;
        }

        log.warn("Could not determine client IP address, returning unknown");
        return "unknown";
    }

    /**
     * Validate IP address format
     * Supports both IPv4 and basic IPv6 validation
     * 
     * @param ipAddress IP address to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidIPAddress(String ipAddress) {
        if (ipAddress == null || ipAddress.trim().isEmpty()) {
            return false;
        }

        ipAddress = ipAddress.trim();

        // Check for IPv4
        if (isValidIPv4(ipAddress)) {
            return true;
        }

        // Check for IPv6 (basic validation)
        if (isValidIPv6(ipAddress)) {
            return true;
        }

        return false;
    }

    /**
     * Validate IPv4 address format
     * 
     * @param ipAddress IP address to validate
     * @return true if valid IPv4, false otherwise
     */
    private static boolean isValidIPv4(String ipAddress) {
        String[] parts = ipAddress.split("\\.");
        if (parts.length != 4) {
            return false;
        }

        try {
            for (String part : parts) {
                int num = Integer.parseInt(part);
                if (num < 0 || num > 255) {
                    return false;
                }
            }
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Basic IPv6 address validation
     * 
     * @param ipAddress IP address to validate
     * @return true if valid IPv6, false otherwise
     */
    private static boolean isValidIPv6(String ipAddress) {
        // Basic IPv6 validation - contains colons and hex characters
        if (!ipAddress.contains(":")) {
            return false;
        }

        // Split by colons and check each part
        String[] parts = ipAddress.split(":");
        if (parts.length > 8) {
            return false;
        }

        for (String part : parts) {
            if (!part.isEmpty()) {
                try {
                    int value = Integer.parseInt(part, 16);
                    if (value < 0 || value > 0xFFFF) {
                        return false;
                    }
                } catch (NumberFormatException e) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Check if IP address is from localhost
     * 
     * @param ipAddress IP address to check
     * @return true if localhost, false otherwise
     */
    public static boolean isLocalhost(String ipAddress) {
        if (ipAddress == null) {
            return false;
        }

        return "127.0.0.1".equals(ipAddress) || 
               "localhost".equalsIgnoreCase(ipAddress) ||
               "0:0:0:0:0:0:0:1".equals(ipAddress) ||
               "::1".equals(ipAddress);
    }

    /**
     * Sanitize IP address for logging (mask last octet for privacy)
     * 
     * @param ipAddress IP address to sanitize
     * @return Sanitized IP address
     */
    public static String sanitizeIpForLogging(String ipAddress) {
        if (ipAddress == null || ipAddress.isEmpty()) {
            return "unknown";
        }

        if (isValidIPv4(ipAddress)) {
            String[] parts = ipAddress.split("\\.");
            if (parts.length == 4) {
                return parts[0] + "." + parts[1] + "." + parts[2] + ".***";
            }
        }

        // For IPv6 or invalid IPs, just return first part
        if (ipAddress.length() > 8) {
            return ipAddress.substring(0, 8) + "***";
        }

        return ipAddress;
    }
}
