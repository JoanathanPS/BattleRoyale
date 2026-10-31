package com.joanathanps.battlearena.scheme;

public final class SystemProps {

    private SystemProps() {}

    public static boolean getBoolean(String name) {
        try {
            String val = System.getProperty(name);
            return "true".equalsIgnoreCase(val);
        } catch (Throwable t) {
            return false;
        }
    }

    public static String getProperty(String name, String def) {
        try {
            String val = System.getProperty(name);
            return (val != null && !val.isEmpty()) ? val : def;
        } catch (Throwable t) {
            return def;
        }
    }

    public static String getProperty(String name) {
        return getProperty(name, null);
    }
}

