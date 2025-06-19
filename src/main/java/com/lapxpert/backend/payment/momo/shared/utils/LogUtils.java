package com.lapxpert.backend.payment.momo.shared.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LogUtils {
    private static Logger logger;

    public static void init() {
        logger = LogManager.getLogger(LogUtils.class);
    }

    public static void info(String serviceCode, Object object) {
        logger.info("[{}]: {}", serviceCode, object); // Sử dụng placeholder {} thay vì StringBuilder
    }

    public static void info(Object object) {
        logger.info(object);
    }

    public static void debug(Object object) {
        logger.debug(object);
    }

    public static void error(Object object) {
        logger.error(object);
    }

    public static void warn(Object object) {
        logger.warn(object);
    }
}