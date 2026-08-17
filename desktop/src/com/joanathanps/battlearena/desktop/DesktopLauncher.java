package com.joanathanps.battlearena.desktop;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.joanathanps.battlearena.BattleRoyaleArenaLite;
import com.joanathanps.battlearena.scheme.PlayerSettings;

public class DesktopLauncher {
    public static void main (String[] arg) {
        LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
        config.width = PlayerSettings.GAME_WIDTH;
        config.height = PlayerSettings.GAME_HEIGHT;
        config.fullscreen = PlayerSettings.FULLSCREEN;
        config.resizable = false;
        config.title = "Battle Royale Arena Lite";
        new LwjglApplication(new BattleRoyaleArenaLite(), config);
    }
}
