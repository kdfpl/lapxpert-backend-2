package com.lapxpert.backend.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * JPA configuration for transaction management.
 * Note: JPA Auditing is configured in AuditConfig.java
 */
@Configuration
@EnableTransactionManagement
public class JpaConfig {
}
