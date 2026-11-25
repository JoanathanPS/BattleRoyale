package com.joanathanps.battlearena.gui;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.joanathanps.battlearena.graphics.ResourceHandler;

import static com.joanathanps.battlearena.scheme.PlayerSettings.GAME_HEIGHT;
import static com.joanathanps.battlearena.scheme.PlayerSettings.GAME_WIDTH;

/** The title-screen island, dimmed, behind the leaderboard and settings screens. */
final class MenuBackdrop {

    private final Texture sea;
    private final Texture island;
    private final Texture vignette;
    private final Texture black;

    MenuBackdrop(ResourceHandler resources) {
        sea = resources.getTexture(ResourceHandler.TexturePath.MENU_P0);
        island = resources.getTexture(ResourceHandler.TexturePath.MENU_P1);
        vignette = resources.getTexture(ResourceHandler.TexturePath.VIGNETTE);
        black = resources.getTexture(ResourceHandler.TexturePath.LOADING_SCREEN);
    }

    void draw(Batch batch) {
        batch.begin();
        batch.setColor(1, 1, 1, 1);
        batch.draw(sea, 0, 0, GAME_WIDTH, GAME_HEIGHT);
        batch.draw(island, (GAME_WIDTH - island.getWidth()) / 2f, (GAME_HEIGHT - island.getHeight()) / 2f);
        batch.draw(vignette, 0, 0, GAME_WIDTH, GAME_HEIGHT);
        batch.setColor(0, 0, 0, 0.62f);
        batch.draw(black, 0, 0, GAME_WIDTH, GAME_HEIGHT);
        batch.setColor(1, 1, 1, 1);
        batch.end();
    }
}
