package com.joanathanps.battlearena.scheme;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

/** Settings saved between sessions (desktop preferences file, or browser local storage on the web). */
public final class PlayerSettings {

    private PlayerSettings() { }

    /** The UI is laid out on this canvas and scaled to the window. */
    public static final int GAME_WIDTH = 1280;
    public static final int GAME_HEIGHT = 720;

    public static final int NAME_MAX_LENGTH = 16;
    private static final String DEFAULT_NAME = "Player";

    private static final String PREFS = "battle-royale-lite";
    private static final String KEY_FULLSCREEN = "fullscreen";
    private static final String KEY_RESOLUTION = "resolution";
    private static final String KEY_NAME = "name";
    private static final String KEY_DIFFICULTY = "difficulty";

    public enum Resolution {
        R_1280x720(1280, 720, "1280 x 720"),
        R_1600x900(1600, 900, "1600 x 900"),
        R_1920x1080(1920, 1080, "1920 x 1080");

        public final int w;
        public final int h;
        public final String label;

        Resolution(int w, int h, String label) {
            this.w = w;
            this.h = h;
            this.label = label;
        }
    }

    public static Resolution getResolution() {
        try {
            return Resolution.valueOf(prefs().getString(KEY_RESOLUTION, Resolution.R_1280x720.name()));
        } catch (IllegalArgumentException e) {
            return Resolution.R_1280x720;
        }
    }

    public static void setResolution(Resolution resolution) {
        prefs().putString(KEY_RESOLUTION, resolution.name()).flush();
    }

    public static boolean getFullscreen() {
        return prefs().getBoolean(KEY_FULLSCREEN, false);
    }

    public static void setFullscreen(boolean fullscreen) {
        prefs().putBoolean(KEY_FULLSCREEN, fullscreen).flush();
    }

    /** The name shown above your soldier, in the kill feed and on the leaderboard. */
    public static String getPlayerName() {
        String name = cleanName(prefs().getString(KEY_NAME, ""));
        return name.isEmpty() ? DEFAULT_NAME : name;
    }

    public static void setPlayerName(String name) {
        prefs().putString(KEY_NAME, cleanName(name)).flush();
    }

    /** Letters, digits, spaces, '-' and '_', trimmed to {@link #NAME_MAX_LENGTH}. */
    public static String cleanName(String raw) {
        if (raw == null) {
            return "";
        }
        StringBuilder clean = new StringBuilder();
        for (int i = 0; i < raw.length() && clean.length() < NAME_MAX_LENGTH; i++) {
            char c = raw.charAt(i);
            if (Character.isLetterOrDigit(c) || c == ' ' || c == '-' || c == '_') {
                clean.append(c);
            }
        }
        return clean.toString().trim();
    }

    public static Difficulty getDifficulty() {
        try {
            return Difficulty.valueOf(prefs().getString(KEY_DIFFICULTY, Difficulty.MEDIUM.name()));
        } catch (IllegalArgumentException e) {
            return Difficulty.MEDIUM;
        }
    }

    public static void setDifficulty(Difficulty difficulty) {
        prefs().putString(KEY_DIFFICULTY, difficulty.name()).flush();
    }

    private static Preferences prefs() {
        return Gdx.app.getPreferences(PREFS);
    }
}
