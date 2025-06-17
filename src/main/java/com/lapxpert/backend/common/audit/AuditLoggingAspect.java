package com.lapxpert.backend.common.audit;

import com.lapxpert.backend.common.util.IpAddressUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

import java.util.HashMap;
import java.util.Map;

/**
 * Cross-cutting audit logging aspect with IP tracking and performance monitoring.
 * Provides comprehensive audit logging for business service operations
 * with Vietnamese audit messages and detailed context tracking.
 */
@Aspect
@Component
@Slf4j
public class AuditLoggingAspect {

    /**
     * Audit all service methods in business packages
     */
    @Around("execution(* com.lapxpert.backend.*.domain.service.*Service.*(..))")
    public Object auditServiceMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        Object[] args = joinPoint.getArgs();
        
        // Get request context for IP tracking
        String clientIp = getCurrentClientIp();
        String userAgent = getCurrentUserAgent();
        
        // Create audit context
        Map<String, Object> auditContext = createAuditContext(className, methodName, args, clientIp, userAgent);
        
        try {
            log.info("Bắt đầu thực hiện phương thức: {}.{} - IP: {} - Args: {}", 
                    className, methodName, clientIp, getArgsSummary(args));
            
            // Execute the method
            Object result = joinPoint.proceed();
            
            long executionTime = System.currentTimeMillis() - startTime;
            
            // Log successful execution
            log.info("Hoàn thành phương thức: {}.{} - Thời gian: {}ms - IP: {}", 
                    className, methodName, executionTime, clientIp);
            
            // Log performance warning if method takes too long
            if (executionTime > 5000) { // 5 seconds
                log.warn("Phương thức chậm: {}.{} - Thời gian: {}ms - Cần tối ưu hóa", 
                        className, methodName, executionTime);
            }
            
            return result;
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            
            // Log error with context
            log.error("Lỗi khi thực hiện phương thức: {}.{} - Thời gian: {}ms - IP: {} - Lỗi: {}", 
                    className, methodName, executionTime, clientIp, e.getMessage(), e);
            
            // Create detailed error audit entry
            createErrorAuditEntry(auditContext, e, executionTime);
            
            throw e;
        }
    }

    /**
     * Audit CRUD operations specifically
     */
    @Around("execution(* com.lapxpert.backend.*.domain.service.*Service.create*(..)) || " +
            "execution(* com.lapxpert.backend.*.domain.service.*Service.update*(..)) || " +
            "execution(* com.lapxpert.backend.*.domain.service.*Service.delete*(..)) || " +
            "execution(* com.lapxpert.backend.*.domain.service.*Service.save*(..))")
    public Object auditCrudOperations(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String clientIp = getCurrentClientIp();
        
        // Determine operation type
        String operationType = determineOperationType(methodName);
        
        log.info("Thao tác CRUD: {} - Phương thức: {}.{} - IP: {}", 
                operationType, className, methodName, clientIp);
        
        try {
            Object result = joinPoint.proceed();
            
            log.info("Thành công thao tác CRUD: {} - Phương thức: {}.{} - IP: {}", 
                    operationType, className, methodName, clientIp);
            
            return result;
            
        } catch (Exception e) {
            log.error("Lỗi thao tác CRUD: {} - Phương thức: {}.{} - IP: {} - Lỗi: {}", 
                    operationType, className, methodName, clientIp, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Get current client IP address
     */
    private String getCurrentClientIp() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                return IpAddressUtils.getClientIpAddress(request);
            }
        } catch (Exception e) {
            log.debug("Không thể lấy IP address: {}", e.getMessage());
        }
        return "UNKNOWN";
    }

    /**
     * Get current user agent
     */
    private String getCurrentUserAgent() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                return request.getHeader("User-Agent");
            }
        } catch (Exception e) {
            log.debug("Không thể lấy User-Agent: {}", e.getMessage());
        }
        return "UNKNOWN";
    }

    /**
     * Create audit context for logging
     */
    private Map<String, Object> createAuditContext(String className, String methodName, 
                                                  Object[] args, String clientIp, String userAgent) {
        Map<String, Object> context = new HashMap<>();
        context.put("className", className);
        context.put("methodName", methodName);
        context.put("timestamp", LocalDateTime.now());
        context.put("clientIp", clientIp);
        context.put("userAgent", userAgent);
        context.put("argsCount", args != null ? args.length : 0);
        context.put("thread", Thread.currentThread().getName());
        
        return context;
    }

    /**
     * Create error audit entry
     */
    private void createErrorAuditEntry(Map<String, Object> context, Exception e, long executionTime) {
        try {
            Map<String, Object> errorAudit = new HashMap<>(context);
            errorAudit.put("errorType", e.getClass().getSimpleName());
            errorAudit.put("errorMessage", e.getMessage());
            errorAudit.put("executionTime", executionTime);
            errorAudit.put("stackTrace", getStackTraceSummary(e));
            
            // Log detailed error audit (could be enhanced to save to database)
            log.error("Chi tiết lỗi audit: {}", errorAudit);
            
        } catch (Exception auditException) {
            log.error("Lỗi khi tạo audit entry: {}", auditException.getMessage());
        }
    }

    /**
     * Get summary of method arguments (avoid logging sensitive data)
     */
    private String getArgsSummary(Object[] args) {
        if (args == null || args.length == 0) {
            return "[]";
        }
        
        StringBuilder summary = new StringBuilder("[");
        for (int i = 0; i < args.length; i++) {
            if (i > 0) summary.append(", ");
            
            Object arg = args[i];
            if (arg == null) {
                summary.append("null");
            } else if (arg instanceof String) {
                String str = (String) arg;
                // Mask potential sensitive data
                if (str.contains("password") || str.contains("token")) {
                    summary.append("***MASKED***");
                } else {
                    summary.append("String(").append(str.length()).append(")");
                }
            } else {
                summary.append(arg.getClass().getSimpleName());
            }
        }
        summary.append("]");
        
        return summary.toString();
    }

    /**
     * Determine CRUD operation type from method name
     */
    private String determineOperationType(String methodName) {
        String lowerMethodName = methodName.toLowerCase();
        
        if (lowerMethodName.startsWith("create") || lowerMethodName.startsWith("add") || 
            lowerMethodName.startsWith("insert") || lowerMethodName.contains("save")) {
            return "TẠO MỚI";
        } else if (lowerMethodName.startsWith("update") || lowerMethodName.startsWith("modify") || 
                   lowerMethodName.startsWith("edit")) {
            return "CẬP NHẬT";
        } else if (lowerMethodName.startsWith("delete") || lowerMethodName.startsWith("remove")) {
            return "XÓA";
        } else {
            return "KHÁC";
        }
    }

    /**
     * Get stack trace summary (first few lines)
     */
    private String getStackTraceSummary(Exception e) {
        StackTraceElement[] stackTrace = e.getStackTrace();
        if (stackTrace.length == 0) {
            return "No stack trace available";
        }
        
        StringBuilder summary = new StringBuilder();
        int maxLines = Math.min(3, stackTrace.length);
        
        for (int i = 0; i < maxLines; i++) {
            if (i > 0) summary.append(" -> ");
            summary.append(stackTrace[i].toString());
        }
        
        if (stackTrace.length > maxLines) {
            summary.append(" ... (").append(stackTrace.length - maxLines).append(" more)");
        }
        
        return summary.toString();
    }
}
