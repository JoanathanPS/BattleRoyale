package com.joanathanps.battlearena.languages;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.I18NBundle;
import com.joanathanps.battlearena.scheme.PlayerSettings;

import java.util.Locale;

public class Internationalization {

    public enum Language {
        ENGLISH
    }

    private I18NBundle bundle;

    public Internationalization() {
        setupBundle();
    }

    private void setupBundle() {
        bundle = createBundleFor(PlayerSettings.getLanguage());
    }

    private static I18NBundle createBundleFor(Language language) {
        FileHandle baseFileHandle = Gdx.files.internal("localization/BattleRoyaleArenaLite");
        Locale locale = new Locale("", "");
        if (language == Language.ENGLISH) {
            locale = new Locale("en", "US");
        }
        return I18NBundle.createBundle(baseFileHandle, locale);
    }

    public I18NBundle getBundle() {
        return bundle;
    }

    public void setLanguage(Language language) {
        PlayerSettings.setLanguage(language);
        bundle = createBundleFor(language);
    }
}
