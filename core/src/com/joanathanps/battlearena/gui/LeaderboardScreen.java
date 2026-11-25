package com.joanathanps.battlearena.gui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.joanathanps.battlearena.BattleRoyaleLite;
import com.joanathanps.battlearena.database.MatchResult;
import com.joanathanps.battlearena.database.MatchResultsRepository;
import com.joanathanps.battlearena.graphics.FontGenerator;
import com.joanathanps.battlearena.graphics.ResourceHandler;
import com.joanathanps.battlearena.scenes.MainMenu;
import com.joanathanps.battlearena.scheme.PlayerSettings;

import java.util.List;

import static com.joanathanps.battlearena.scheme.PlayerSettings.GAME_HEIGHT;
import static com.joanathanps.battlearena.scheme.PlayerSettings.GAME_WIDTH;

/** Top ten scores from the online leaderboard. Your own rows are highlighted. */
public class LeaderboardScreen implements Screen {

    private static final Color GOLD = Color.valueOf("ffd700");
    private static final Color MUTED = new Color(0.72f, 0.72f, 0.72f, 1f);
    private static final Color WIN = Color.valueOf("8fe36b");
    private static final String[] HEADERS = {"#", "PLAYER", "MODE", "KILLS", "SURVIVED", "SCORE", "RESULT"};
    private static final float[] WIDTHS = {40, 230, 100, 80, 110, 100, 90};

    private final BattleRoyaleLite game = BattleRoyaleLite.get();
    private final Viewport viewport = new FitViewport(GAME_WIDTH, GAME_HEIGHT, new OrthographicCamera());
    private final Stage stage;
    private final MenuBackdrop backdrop;
    private final Table rows = new Table();

    // Results arrive on a network thread and are applied on the render thread.
    private volatile List<MatchResult> pendingResults;
    private volatile String pendingError;
    private boolean applied;

    public LeaderboardScreen(MatchResultsRepository repository) {
        stage = new Stage(viewport, game.getBatch());
        backdrop = new MenuBackdrop(game.getResources());
        Skin buttons = game.getResources().getSkin(ResourceHandler.SkinPaths.BUTTON_SKIN);

        Table root = new Table();
        root.setFillParent(true);
        root.top().padTop(70);
        root.add(new Label("LEADERBOARD", style(42, Color.WHITE))).row();
        root.add(new Label("TOP 10 SCORES", style(16, MUTED))).padBottom(24).row();

        Table header = new Table();
        for (int i = 0; i < HEADERS.length; i++) {
            Label label = new Label(HEADERS[i], style(14, MUTED));
            label.setAlignment(i == 1 ? Align.left : Align.center);
            header.add(label).width(WIDTHS[i]);
        }
        root.add(header).padBottom(8).row();

        rows.add(new Label("Loading...", style(18, Color.WHITE))).padTop(20);
        root.add(rows).height(370).top().row();

        Table footer = new Table();
        PauseMenu.addButton(footer, buttons, "BACK", new Runnable() {
            public void run() {
                back();
            }
        });
        root.add(footer).padTop(10).row();
        root.add(new Label("Score = 100 per kill + 500 for a win + 10 per second survived", style(14, MUTED)))
                .padTop(6);
        stage.addActor(root);

        request(repository);
        Gdx.input.setInputProcessor(stage);
    }

    private void request(MatchResultsRepository repository) {
        if (repository == null || !repository.isConnected()) {
            pendingError = "The leaderboard is offline in this build.";
            return;
        }
        repository.getLeaderboard(10, new MatchResultsRepository.LeaderboardCallback() {
            @Override
            public void onSuccess(List<MatchResult> results) {
                pendingResults = results;
            }

            @Override
            public void onFailure(String errorMessage) {
                pendingError = "Could not load the leaderboard. Check your connection.";
            }
        });
    }

    private void applyResults() {
        if (applied) {
            return;
        }
        if (pendingError != null) {
            showMessage(pendingError);
        } else if (pendingResults != null) {
            if (pendingResults.isEmpty()) {
                showMessage("No matches yet. Play one and claim the top spot.");
            } else {
                showRows(pendingResults);
            }
        } else {
            return;
        }
        applied = true;
    }

    private void showMessage(String message) {
        rows.clearChildren();
        rows.add(new Label(message, style(18, Color.WHITE))).padTop(20);
    }

    private void showRows(List<MatchResult> results) {
        rows.clearChildren();
        String me = PlayerSettings.getPlayerName();
        int rank = 1;
        for (MatchResult result : results) {
            boolean mine = me.equalsIgnoreCase(result.getPlayerName());
            boolean won = "WIN".equals(result.getResult());
            Color text = mine ? GOLD : Color.WHITE;
            String[] cells = {
                    String.valueOf(rank),
                    result.getPlayerName(),
                    result.getDifficulty(),
                    String.valueOf(result.getKills()),
                    result.getFormattedTime(),
                    String.valueOf(result.getScore()),
                    won ? "WIN" : "LOSS"
            };
            for (int i = 0; i < cells.length; i++) {
                Color color = i == 6 ? (won ? WIN : MUTED) : text;
                Label label = new Label(cells[i], style(rank <= 3 ? 20 : 18, color));
                label.setAlignment(i == 1 ? Align.left : Align.center);
                label.setEllipsis(true);
                rows.add(label).width(WIDTHS[i]).height(34);
            }
            rows.row();
            rank++;
        }
    }

    private void back() {
        game.setScreen(new MainMenu());
    }

    private Label.LabelStyle style(int size, Color color) {
        return new Label.LabelStyle(FontGenerator.generate(ResourceHandler.FontPath.BOMBARD, size, true), color);
    }

    @Override
    public void render(float delta) {
        applyResults();
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        viewport.apply();
        game.getBatch().setProjectionMatrix(viewport.getCamera().combined);
        backdrop.draw(game.getBatch());
        stage.act(delta);
        stage.draw();
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            back();
        }
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void show() {
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
    }
}
