package com.sivannsan.millidb;

/**
 * Log on console
 */
public final class MilliDBLogger {
    private MilliDBLogger() {
    }

    private static void log(String message) {
        System.out.println(message);
    }

    public static void info(String message) {
        log("[INFO]: " + message);
    }

    public static void warning(String message) {
        log("[WARNING]: " + message);
    }
}
