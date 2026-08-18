package com.joanathanps.battlearena.client;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.backends.gwt.GwtApplication;
import com.badlogic.gdx.backends.gwt.GwtApplicationConfiguration;
import com.joanathanps.battlearena.BattleRoyaleArenaLite;
import com.joanathanps.battlearena.scheme.PlayerSettings;

public class HtmlLauncher extends GwtApplication {

    @Override
    public GwtApplicationConfiguration getConfig() {
        GwtApplicationConfiguration config =
                new GwtApplicationConfiguration(PlayerSettings.GAME_WIDTH, PlayerSettings.GAME_HEIGHT);
        return config;
    }

    @Override
    public ApplicationListener createApplicationListener() {
        return new BattleRoyaleArenaLite();
    }
}