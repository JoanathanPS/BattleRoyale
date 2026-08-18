package com.joanathanps.battlearena;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.joanathanps.battlearena.database.MatchResultsRepository;
import com.joanathanps.battlearena.database.SupabaseMatchResultsRepository;
import com.joanathanps.battlearena.scenes.MainMenu;

public class BattleRoyaleArenaLite extends Game {

    private SpriteBatch batch;
    private MatchResultsRepository matchResultsRepository;

    @Override
    public void create () {
        batch = new SpriteBatch();
        matchResultsRepository = new SupabaseMatchResultsRepository();
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