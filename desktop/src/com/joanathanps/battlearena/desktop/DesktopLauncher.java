package com.joanathanps.battlearena.desktop;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.joanathanps.battlearena.BattleRoyaleLite;
import com.joanathanps.battlearena.scheme.PlayerSettings;

public class DesktopLauncher {
    public static void main (String[] arg) {
        LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
        config.width = PlayerSettings.GAME_WIDTH;
        config.height = PlayerSettings.GAME_HEIGHT;
        config.fullscreen = false;
        config.resizable = false;
        config.title = "Battle Royale Lite";
        new LwjglApplication(new BattleRoyaleLite(), config);
    }
}
