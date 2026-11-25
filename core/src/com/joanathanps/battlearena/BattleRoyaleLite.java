package com.joanathanps.battlearena;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.joanathanps.battlearena.audio.AudioManager;
import com.joanathanps.battlearena.database.MatchResultsRepository;
import com.joanathanps.battlearena.database.SupabaseMatchResultsRepository;
import com.joanathanps.battlearena.graphics.FontGenerator;
import com.joanathanps.battlearena.graphics.ResourceHandler;
import com.joanathanps.battlearena.scenes.MainMenu;
import com.joanathanps.battlearena.scheme.PlayerSettings;

public class BattleRoyaleLite extends Game {

    private SpriteBatch batch;
    private ResourceHandler resources;
    private MatchResultsRepository matchResultsRepository;
    private Cursor menuCursor;
    private Cursor aimCursor;

    public static BattleRoyaleLite get() {
        return (BattleRoyaleLite) Gdx.app.getApplicationListener();
    }

    @Override
    public void create() {
        batch = new SpriteBatch();
        resources = new ResourceHandler();
        Pixmap menu = resources.getPixmap(ResourceHandler.PixmapPath.MENU_CURSOR);
        Pixmap aim = resources.getPixmap(ResourceHandler.PixmapPath.MATCH_CURSOR);
        menuCursor = Gdx.graphics.newCursor(menu, 0, 0);
        aimCursor = Gdx.graphics.newCursor(aim, aim.getWidth() / 2, aim.getHeight() / 2);
        AudioManager.init();
        matchResultsRepository = new SupabaseMatchResultsRepository();
        PlayerSettings.Resolution res = PlayerSettings.getResolution();
        if (PlayerSettings.getFullscreen()) {
            Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
        } else if (res != null) {
            Gdx.graphics.setWindowedMode(res.w, res.h);
        }
        setScreen(new MainMenu());
    }

    /** Replaces the current screen and frees the one it replaces. */
    @Override
    public void setScreen(Screen next) {
        Screen previous = getScreen();
        super.setScreen(next);
        if (previous != null && previous != next) {
            previous.dispose();
        }
    }

    @Override
    public void dispose() {
        Screen current = getScreen();
        if (current != null) {
            current.dispose();
        }
        batch.dispose();
        menuCursor.dispose();
        aimCursor.dispose();
        resources.dispose();
        FontGenerator.disposeAll();
        AudioManager.dispose();
    }

    /** Arrow cursor for menus, or crosshair for aiming in a match. */
    public void useCursor(boolean aiming) {
        Gdx.graphics.setCursor(aiming ? aimCursor : menuCursor);
    }

    public SpriteBatch getBatch() {
        return batch;
    }

    public ResourceHandler getResources() {
        return resources;
    }

    public MatchResultsRepository getMatchResultsRepository() {
        return matchResultsRepository;
    }
}
