package com.joanathanps.battlearena.scheme;

public final class SystemProps {

    private SystemProps() {}

    public static boolean getBoolean(String name) {
        return false;
    }

    public static String getProperty(String name, String def) {
        return def;
    }

    public static String getProperty(String name) {
        return null;
    }
}
