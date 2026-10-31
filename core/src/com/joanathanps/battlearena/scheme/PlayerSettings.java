package com.joanathanps.battlearena.scheme;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.joanathanps.battlearena.languages.Internationalization;

public final class PlayerSettings {

    private PlayerSettings() { }

    public static final int GAME_WIDTH  = 1280;
    public static final int GAME_HEIGHT = 720;
    public static final boolean DEBUG_MODE = false;

    private static final String PREFS          = "player-settings";
    private static final String KEY_FULLSCREEN = "fullscreen";
    private static final String KEY_LANGUAGE   = "language";
    private static final String KEY_RESOLUTION = "resolution";

    // -----------------------------------------------------------------------
    // Resolution
    // -----------------------------------------------------------------------

    /** Supported window resolutions. Default is 1280×720. */
    public enum Resolution {
        R_800x600  (800,  600,  "800 × 600"),
        R_1280x720 (1280, 720,  "1280 × 720"),
        R_1600x900 (1600, 900,  "1600 × 900"),
        R_1920x1080(1920, 1080, "1920 × 1080");

        public final int w;
        public final int h;
        public final String label;

        Resolution(int w, int h, String label) {
            this.w     = w;
            this.h     = h;
            this.label = label;
        }
    }

    public static Resolution getResolution() {
        try {
            return Resolution.valueOf(prefs().getString(KEY_RESOLUTION,
                    Resolution.R_1280x720.name()));
        } catch (Exception e) {
            return Resolution.R_1280x720;
        }
    }

    public static void setResolution(Resolution resolution) {
        prefs().putString(KEY_RESOLUTION, resolution.name()).flush();
    }

    // -----------------------------------------------------------------------
    // Fullscreen
    // -----------------------------------------------------------------------

    public static boolean getFullscreen() {
        return prefs().getBoolean(KEY_FULLSCREEN, false);
    }

    public static void setFullscreen(boolean fullscreen) {
        prefs().putBoolean(KEY_FULLSCREEN, fullscreen).flush();
    }

    // -----------------------------------------------------------------------
    // Language
    // -----------------------------------------------------------------------

    public static Internationalization.Language getLanguage() {
        try {
            return Internationalization.Language.valueOf(prefs().getString(KEY_LANGUAGE,
                    Internationalization.Language.ENGLISH.name()));
        } catch (Exception e) {
            return Internationalization.Language.ENGLISH;
        }
    }

    public static void setLanguage(Internationalization.Language language) {
        prefs().putString(KEY_LANGUAGE, language.name()).flush();
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static Preferences prefs() {
        return Gdx.app.getPreferences(PREFS);
    }
}
