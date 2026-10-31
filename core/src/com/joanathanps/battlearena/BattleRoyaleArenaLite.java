package com.joanathanps.battlearena;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.joanathanps.battlearena.database.MatchResultsRepository;
import com.joanathanps.battlearena.database.SupabaseMatchResultsRepository;
import com.joanathanps.battlearena.scenes.MainMenu;
import com.joanathanps.battlearena.scheme.PlayerSettings;

public class BattleRoyaleArenaLite extends Game {

    private SpriteBatch batch;
    private MatchResultsRepository matchResultsRepository;

    @Override
    public void create () {
        batch = new SpriteBatch();
        matchResultsRepository = new SupabaseMatchResultsRepository();
        PlayerSettings.Resolution res = PlayerSettings.getResolution();
        if (PlayerSettings.getFullscreen()) {
            Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
        } else if (res != null) {
            Gdx.graphics.setWindowedMode(res.w, res.h);
        }
        setScreen(new MainMenu(batch));
    }

    @Override
    public void render () {
        super.render();
    }

    @Override
    public void dispose () {
        batch.dispose();
    }

    public MatchResultsRepository getMatchResultsRepository() {
        return matchResultsRepository;
    }

    public SpriteBatch getBatch() {
        return batch;
    }
}