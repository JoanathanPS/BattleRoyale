package com.joanathanps.battlearena.gui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.joanathanps.battlearena.BattleRoyaleArenaLite;
import com.joanathanps.battlearena.database.MatchResult;
import com.joanathanps.battlearena.database.MatchResultsRepository;
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
    private MatchResultsRepository repository;

    private Window window;
    private Table rowsContainer;

    private int width = 700;
    private int height = 500;

    // Async results are produced on a background thread (desktop) and consumed on the render thread.
    private volatile List<MatchResult> pendingResults;
    private volatile String pendingError;
    private boolean resultsApplied;

    public LeaderboardScreen(MatchResultsRepository repository) {
        this.repository = repository;
        setupStage();
        forgeLeaderboardScreen();
        requestLeaderboard();
    }

    private void setupStage() {
        viewport = new FitViewport(GAME_WIDTH, GAME_HEIGHT, new OrthographicCamera());
        stage = new Stage(viewport);
        skin = new Skin();
        skin.add("bombard", FontGenerator.generate(ResourceHandler.FontPath.BOMBARD, 26, false));
        skin.addRegions(new TextureAtlas(Gdx.files.internal("skins/vis/skin/x2/uiskin.atlas")));
        skin.load(Gdx.files.internal("skins/vis/skin/x2/uiskin.json"));
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

        headerTable.add(headerLabel("#")).width(40);
        headerTable.add(headerLabel("PLAYER")).width(180);
        headerTable.add(headerLabel("SCORE")).width(80);
        headerTable.add(headerLabel("TIME")).width(80);
        headerTable.add(headerLabel("RESULT")).width(100);
        window.add(headerTable).row();

        rowsContainer = new Table();
        rowsContainer.add(new Label("Loading leaderboard...", rowStyle(16, Color.WHITE))).padTop(20).row();
        window.add(rowsContainer).padTop(2).row();

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

    private Label headerLabel(String text) {
        return new Label(text, new Label.LabelStyle(
                FontGenerator.generate(ResourceHandler.FontPath.BOMBARD, 14, false), Color.LIGHT_GRAY));
    }

    private Label.LabelStyle rowStyle(int size, Color color) {
        return new Label.LabelStyle(FontGenerator.generate(ResourceHandler.FontPath.BOMBARD, size, false), color);
    }

    private void requestLeaderboard() {
        if (repository == null || !repository.isConnected()) {
            pendingError = "Supabase is not configured.";
            return;
        }
        repository.getLeaderboard(10, new MatchResultsRepository.LeaderboardCallback() {
            @Override
            public void onSuccess(List<MatchResult> results) {
                pendingResults = results;
            }

            @Override
            public void onFailure(String errorMessage) {
                pendingError = errorMessage;
            }
        });
    }

    private void consumeResults() {
        if (resultsApplied) {
            return;
        }
        if (pendingError != null) {
            showMessage(pendingError, Color.RED);
            resultsApplied = true;
            pendingError = null;
            return;
        }
        if (pendingResults == null) {
            return;
        }
        if (pendingResults.isEmpty()) {
            showMessage("No match results yet. Play a game!", Color.WHITE);
        } else {
            showRows(pendingResults);
        }
        resultsApplied = true;
        pendingResults = null;
    }

    private void showMessage(String message, Color color) {
        rowsContainer.clearChildren();
        rowsContainer.add(new Label(message, rowStyle(16, color))).padTop(20).row();
    }

    private void showRows(List<MatchResult> results) {
        rowsContainer.clearChildren();
        int rank = 1;
        for (MatchResult mr : results) {
            Table rowTable = new Table();

            Label rankLabel = new Label(Integer.toString(rank), rowStyle(14, Color.WHITE));
            Label nameLabel = new Label(mr.getPlayerName(), rowStyle(14, Color.WHITE));
            Label scoreLabel = new Label(Integer.toString(mr.getScore()), rowStyle(14, Color.CYAN));
            Label timeLabel = new Label(mr.getFormattedTime(), rowStyle(14, Color.WHITE));

            Color resultColor = "WIN".equals(mr.getResult()) ? Color.GREEN : Color.RED;
            Label resultLabel = new Label(mr.getResult(), rowStyle(14, resultColor));

            rowTable.add(rankLabel).width(40);
            rowTable.add(nameLabel).width(180);
            rowTable.add(scoreLabel).width(80);
            rowTable.add(timeLabel).width(80);
            rowTable.add(resultLabel).width(100);

            rowsContainer.add(rowTable).padTop(2).padBottom(2).row();
            rank++;
        }
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float delta) {
        consumeResults();
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