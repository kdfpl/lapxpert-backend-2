package com.lapxpert.backend.common.service;

import org.springframework.stereotype.Component;

import java.time.*;
import java.time.format.DateTimeFormatter;

/**
 * Service for handling Vietnam timezone operations in business logic
 * Provides centralized timezone management for the application
 * 
 * This service implements the hybrid timezone strategy:
 * - Database stores UTC Instant values
 * - Business logic uses Vietnam timezone (Asia/Ho_Chi_Minh)
 * - Frontend receives both UTC and Vietnam-formatted timestamps
 */
@Component
public class VietnamTimeZoneService {
    
    /**
     * Vietnam timezone constant
     */
    public static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    
    /**
     * Standard Vietnam date-time formatter for display
     */
    public static final DateTimeFormatter VIETNAM_DATETIME_FORMATTER = 
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(VIETNAM_ZONE);
    
    /**
     * Vietnam date formatter for display
     */
    public static final DateTimeFormatter VIETNAM_DATE_FORMATTER = 
        DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(VIETNAM_ZONE);
    
    /**
     * Convert UTC Instant to Vietnam business time
     * 
     * @param utcInstant UTC timestamp
     * @return ZonedDateTime in Vietnam timezone
     */
    public ZonedDateTime toVietnamTime(Instant utcInstant) {
        if (utcInstant == null) {
            return null;
        }
        return utcInstant.atZone(VIETNAM_ZONE);
    }
    
    /**
     * Get current Vietnam business time
     * 
     * @return Current time in Vietnam timezone
     */
    public ZonedDateTime getCurrentVietnamTime() {
        return ZonedDateTime.now(VIETNAM_ZONE);
    }
    
    /**
     * Get current date in Vietnam timezone
     * 
     * @return Current date in Vietnam timezone
     */
    public LocalDate getCurrentVietnamDate() {
        return LocalDate.now(VIETNAM_ZONE);
    }
    
    /**
     * Convert Vietnam ZonedDateTime to UTC Instant
     * 
     * @param vietnamTime Time in Vietnam timezone
     * @return UTC Instant
     */
    public Instant toUtcInstant(ZonedDateTime vietnamTime) {
        if (vietnamTime == null) {
            return null;
        }
        return vietnamTime.toInstant();
    }
    
    /**
     * Format UTC Instant as Vietnam time string
     * 
     * @param utcInstant UTC timestamp
     * @return Formatted Vietnam time string (dd/MM/yyyy HH:mm)
     */
    public String formatAsVietnamDateTime(Instant utcInstant) {
        if (utcInstant == null) {
            return null;
        }
        return VIETNAM_DATETIME_FORMATTER.format(utcInstant);
    }
    
    /**
     * Format UTC Instant as Vietnam date string
     * 
     * @param utcInstant UTC timestamp
     * @return Formatted Vietnam date string (dd/MM/yyyy)
     */
    public String formatAsVietnamDate(Instant utcInstant) {
        if (utcInstant == null) {
            return null;
        }
        return VIETNAM_DATE_FORMATTER.format(utcInstant);
    }
    
    /**
     * Check if current time is within Vietnam business hours (8 AM - 10 PM)
     * 
     * @return true if within business hours
     */
    public boolean isVietnamBusinessHours() {
        ZonedDateTime vietnamNow = getCurrentVietnamTime();
        int hour = vietnamNow.getHour();
        return hour >= 8 && hour <= 22;
    }
    
    /**
     * Check if a given instant falls on the same date in Vietnam timezone
     * 
     * @param instant1 First instant
     * @param instant2 Second instant
     * @return true if both instants are on the same Vietnam date
     */
    public boolean isSameVietnamDate(Instant instant1, Instant instant2) {
        if (instant1 == null || instant2 == null) {
            return false;
        }
        
        LocalDate date1 = instant1.atZone(VIETNAM_ZONE).toLocalDate();
        LocalDate date2 = instant2.atZone(VIETNAM_ZONE).toLocalDate();
        
        return date1.equals(date2);
    }
    
    /**
     * Check if an instant falls on today in Vietnam timezone
     * 
     * @param instant Instant to check
     * @return true if instant is today in Vietnam time
     */
    public boolean isToday(Instant instant) {
        if (instant == null) {
            return false;
        }
        
        LocalDate instantDate = instant.atZone(VIETNAM_ZONE).toLocalDate();
        LocalDate today = getCurrentVietnamDate();
        
        return instantDate.equals(today);
    }
    
    /**
     * Create an Instant from Vietnam date and time components
     * 
     * @param year Year
     * @param month Month (1-12)
     * @param day Day of month
     * @param hour Hour (0-23)
     * @param minute Minute (0-59)
     * @return UTC Instant representing the Vietnam local time
     */
    public Instant createVietnamInstant(int year, int month, int day, int hour, int minute) {
        LocalDateTime localDateTime = LocalDateTime.of(year, month, day, hour, minute);
        ZonedDateTime vietnamDateTime = localDateTime.atZone(VIETNAM_ZONE);
        return vietnamDateTime.toInstant();
    }
    
    /**
     * Get start of day in Vietnam timezone as UTC Instant
     * 
     * @param date Date in Vietnam timezone
     * @return UTC Instant representing start of day (00:00) in Vietnam
     */
    public Instant getStartOfDayVietnam(LocalDate date) {
        if (date == null) {
            return null;
        }
        
        LocalDateTime startOfDay = date.atStartOfDay();
        ZonedDateTime vietnamStartOfDay = startOfDay.atZone(VIETNAM_ZONE);
        return vietnamStartOfDay.toInstant();
    }
    
    /**
     * Get end of day in Vietnam timezone as UTC Instant
     * 
     * @param date Date in Vietnam timezone
     * @return UTC Instant representing end of day (23:59:59.999) in Vietnam
     */
    public Instant getEndOfDayVietnam(LocalDate date) {
        if (date == null) {
            return null;
        }
        
        LocalDateTime endOfDay = date.atTime(23, 59, 59, 999_999_999);
        ZonedDateTime vietnamEndOfDay = endOfDay.atZone(VIETNAM_ZONE);
        return vietnamEndOfDay.toInstant();
    }
}
