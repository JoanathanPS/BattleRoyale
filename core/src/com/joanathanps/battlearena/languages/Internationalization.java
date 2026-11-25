package com.joanathanps.battlearena.languages;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.I18NBundle;

/** All player-facing text, read from assets/localization/strings.properties. */
public class Internationalization {

    private static I18NBundle bundle;

    public I18NBundle getBundle() {
        if (bundle == null) {
            bundle = I18NBundle.createBundle(Gdx.files.internal("localization/strings"));
        }
        return bundle;
    }
}
