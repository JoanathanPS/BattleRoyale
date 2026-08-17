package com.joanathanps.battlearena.gui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.joanathanps.battlearena.BattleRoyaleArenaLite;
import com.joanathanps.battlearena.database.DatabaseManager;
import com.joanathanps.battlearena.database.MatchResult;
import com.joanathanps.battlearena.graphics.FontGenerator;
import com.joanathanps.battlearena.graphics.ResourceHandler;
import com.joanathanps.battlearena.scenes.MainMenu;

import java.util.List;

import static com.joanathanps.battlearena.scheme.PhysicsAdapter.pCenter;
import static com.joanathanps.battlearena.scheme.PlayerSettings.GAME_HEIGHT;
import static com.joanathanps.battlearena.scheme.PlayerSettings.GAME_WIDTH;

public class LeaderboardScreen implements Screen {

    private Stage stage;
    private Viewport viewport;
    private Skin skin;
    private DatabaseManager dbManager;

    private Window window;
    private int width = 700;
    private int height = 500;

    public LeaderboardScreen(DatabaseManager dbManager) {
        this.dbManager = dbManager;
        setupStage();
        forgeLeaderboardScreen();
    }

    private void setupStage() {
        viewport = new FitViewport(GAME_WIDTH, GAME_HEIGHT, new OrthographicCamera());
        stage = new Stage(viewport);
        skin = new Skin(Gdx.files.internal("uiskin.json"));
        Gdx.input.setInputProcessor(stage);
    }

    private void forgeLeaderboardScreen() {
        window = new Window("LEADERBOARD", skin, "no-stage-background");
        window.setSize(width, height);
        window.setPosition(pCenter(GAME_WIDTH) - pCenter(width), pCenter(GAME_HEIGHT) - pCenter(height));

        Label titleLabel = new Label("TOP MATCHES", new Label.LabelStyle(
                FontGenerator.generate(ResourceHandler.FontPath.BOMBARD, 24, false), Color.GOLD));
        window.add(titleLabel).padTop(10).row();

        Table headerTable = new Table();
        headerTable.padTop(10);

        Label rankHeader = new Label("#", new Label.LabelStyle(
                FontGenerator.generate(ResourceHandler.FontPath.BOMBARD, 14, false), Color.LIGHT_GRAY));
        Label nameHeader = new Label("PLAYER", new Label.LabelStyle(
                FontGenerator.generate(ResourceHandler.FontPath.BOMBARD, 14, false), Color.LIGHT_GRAY));
        Label scoreHeader = new Label("SCORE", new Label.LabelStyle(
                FontGenerator.generate(ResourceHandler.FontPath.BOMBARD, 14, false), Color.LIGHT_GRAY));
        Label timeHeader = new Label("TIME", new Label.LabelStyle(
                FontGenerator.generate(ResourceHandler.FontPath.BOMBARD, 14, false), Color.LIGHT_GRAY));
        Label resultHeader = new Label("RESULT", new Label.LabelStyle(
                FontGenerator.generate(ResourceHandler.FontPath.BOMBARD, 14, false), Color.LIGHT_GRAY));

        headerTable.add(rankHeader).width(40);
        headerTable.add(nameHeader).width(180);
        headerTable.add(scoreHeader).width(80);
        headerTable.add(timeHeader).width(80);
        headerTable.add(resultHeader).width(100);
        window.add(headerTable).row();

        window.add(new Label("", skin)).row();

        if (dbManager != null && dbManager.isConnected()) {
            List<MatchResult> results = dbManager.getLeaderboard(10);

            if (results.isEmpty()) {
                Label noData = new Label("No match results yet. Play a game!", new Label.LabelStyle(
                        FontGenerator.generate(ResourceHandler.FontPath.BOMBARD, 16, false), Color.WHITE));
                window.add(noData).padTop(20).row();
            } else {
                int rank = 1;
                for (MatchResult mr : results) {
                    Table rowTable = new Table();

                    Label rankLabel = new Label(String.valueOf(rank), new Label.LabelStyle(
                            FontGenerator.generate(ResourceHandler.FontPath.BOMBARD, 14, false), Color.WHITE));
                    Label nameLabel = new Label(mr.getPlayerName(), new Label.LabelStyle(
                            FontGenerator.generate(ResourceHandler.FontPath.BOMBARD, 14, false), Color.WHITE));
                    Label scoreLabel = new Label(String.valueOf(mr.getScore()), new Label.LabelStyle(
                            FontGenerator.generate(ResourceHandler.FontPath.BOMBARD, 14, false), Color.CYAN));
                    Label timeLabel = new Label(mr.getFormattedTime(), new Label.LabelStyle(
                            FontGenerator.generate(ResourceHandler.FontPath.BOMBARD, 14, false), Color.WHITE));

                    Color resultColor = mr.getResult().equals("WIN") ? Color.GREEN : Color.RED;
                    Label resultLabel = new Label(mr.getResult(), new Label.LabelStyle(
                            FontGenerator.generate(ResourceHandler.FontPath.BOMBARD, 14, false), resultColor));

                    rowTable.add(rankLabel).width(40);
                    rowTable.add(nameLabel).width(180);
                    rowTable.add(scoreLabel).width(80);
                    rowTable.add(timeLabel).width(80);
                    rowTable.add(resultLabel).width(100);

                    window.add(rowTable).padTop(2).padBottom(2).row();
                    rank++;
                }
            }
        } else {
            Label errorLabel = new Label("Database not connected.", new Label.LabelStyle(
                    FontGenerator.generate(ResourceHandler.FontPath.BOMBARD, 16, false), Color.RED));
            window.add(errorLabel).padTop(20).row();
        }

        TextButton backButton = new TextButton("BACK", skin);
        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                BattleRoyaleArenaLite game = (BattleRoyaleArenaLite) Gdx.app.getApplicationListener();
                game.setScreen(new MainMenu(game.getBatch()));
            }
        });
        window.add(backButton).padTop(15).row();

        stage.addActor(window);
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
    }

    @Override
    public void dispose() {
        stage.dispose();
        skin.dispose();
    }

    public Stage getStage() {
        return stage;
    }
}
