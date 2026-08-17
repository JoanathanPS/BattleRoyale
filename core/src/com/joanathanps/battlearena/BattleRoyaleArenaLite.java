package com.joanathanps.battlearena;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.joanathanps.battlearena.database.DatabaseManager;
import com.joanathanps.battlearena.scenes.MainMenu;

public class BattleRoyaleArenaLite extends Game {

    private SpriteBatch batch;
    private DatabaseManager dbManager;

    @Override
    public void create () {
        batch = new SpriteBatch();
        dbManager = new DatabaseManager();
        if (dbManager.connect()) {
            dbManager.initializeDatabase();
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
        if (dbManager != null) {
            dbManager.disconnect();
        }
    }

    public DatabaseManager getDbManager() {
        return dbManager;
    }

    public SpriteBatch getBatch() {
        return batch;
    }
}
