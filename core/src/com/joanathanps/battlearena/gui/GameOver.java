package com.joanathanps.battlearena.gui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.joanathanps.battlearena.BattleRoyaleLite;
import com.joanathanps.battlearena.database.MatchResultsRepository;
import com.joanathanps.battlearena.entities.Soldier;
import com.joanathanps.battlearena.graphics.FontGenerator;
import com.joanathanps.battlearena.graphics.ResourceHandler;
import com.joanathanps.battlearena.scenes.Match;
import com.joanathanps.battlearena.scheme.PlayerSettings;

import static com.joanathanps.battlearena.scheme.PhysicsAdapter.formatSeconds;
import static com.joanathanps.battlearena.scheme.PlayerSettings.GAME_HEIGHT;
import static com.joanathanps.battlearena.scheme.PlayerSettings.GAME_WIDTH;

/** End-of-match results: placement, kills, time, who got you, and what to do next. */
public class GameOver implements Disposable {

    private final Match match;
    private final Stage stage;

    public GameOver(Match match) {
        this.match = match;
        stage = new Stage(new FitViewport(GAME_WIDTH, GAME_HEIGHT, new OrthographicCamera()), match.getBatch());
    }

    /** Score: 100 per kill, 500 for a win, 10 per second survived. */
    public static int score(int kills, boolean won, int seconds) {
        return kills * 100 + (won ? 500 : 0) + seconds * 10;
    }

    public void show(boolean won, int placement, int kills, int seconds, Soldier killer) {
        int score = score(kills, won, seconds);
        saveResult(won, kills, seconds, score);

        Skin skin = match.getResources().getSkin(ResourceHandler.SkinPaths.BUTTON_SKIN);
        Image scrim = new Image(match.getResources().getTexture(ResourceHandler.TexturePath.BLACK_BACKGROUND));
        scrim.setBounds(0, 0, GAME_WIDTH, GAME_HEIGHT);
        scrim.setColor(0, 0, 0, 0.72f);
        stage.addActor(scrim);

        Table root = new Table();
        root.setFillParent(true);
        root.center();

        Image icon = new Image(match.getResources().getTexture(won
                ? ResourceHandler.TexturePath.SAILING : ResourceHandler.TexturePath.IS_DED));
        icon.setScaling(Scaling.fit);
        root.add(icon).size(220).padRight(56);

        Table column = new Table();
        column.left();
        Label title = new Label(text(won ? "victory" : "failure"), style(48, won ? new Color(1f, 0.84f, 0f, 1f) : Color.WHITE));
        column.add(title).left().row();

        String subtitle = won ? text("lastStanding")
                : killer == null ? text("killedByZone")
                : text("killedBy") + " " + killer.getName();
        column.add(new Label(subtitle, style(18, new Color(0.8f, 0.8f, 0.8f, 1f)))).left().padBottom(18).row();

        Table stats = new Table();
        addStat(stats, text("place"), "#" + placement + " / " + (match.getDifficulty().getBotCount() + 1));
        addStat(stats, text("kills"), String.valueOf(kills));
        addStat(stats, text("survived"), formatSeconds(seconds, false));
        addStat(stats, text("score"), String.valueOf(score));
        column.add(stats).left().padBottom(18).row();

        Table buttons = new Table();
        PauseMenu.addButton(buttons, skin, text("playAgain"), new Runnable() {
            public void run() {
                match.playAgain();
            }
        });
        PauseMenu.addButton(buttons, skin, text("leaderboard"), new Runnable() {
            public void run() {
                BattleRoyaleLite game = BattleRoyaleLite.get();
                game.setScreen(new LeaderboardScreen(game.getMatchResultsRepository()));
            }
        });
        PauseMenu.addButton(buttons, skin, text("pauseMenuBack"), new Runnable() {
            public void run() {
                match.backToMenu();
            }
        });
        column.add(buttons).left();
        root.add(column).left();

        root.getColor().a = 0;
        root.addAction(Actions.fadeIn(0.35f));
        stage.addActor(root);
    }

    private void addStat(Table stats, String label, String value) {
        stats.add(new Label(label, style(18, new Color(0.7f, 0.7f, 0.7f, 1f)))).left().padRight(28);
        stats.add(new Label(value, style(22, Color.WHITE))).left().row();
    }

    private void saveResult(boolean won, int kills, int seconds, int score) {
        MatchResultsRepository repository = BattleRoyaleLite.get().getMatchResultsRepository();
        if (repository != null) {
            repository.saveMatchResult(PlayerSettings.getPlayerName(), match.getDifficulty().getLabel(),
                    score, kills, seconds, won ? "WIN" : "LOSS");
        }
    }

    private Label.LabelStyle style(int size, Color color) {
        return new Label.LabelStyle(FontGenerator.generate(ResourceHandler.FontPath.BOMBARD, size, true), color);
    }

    private String text(String key) {
        return match.getI18n().getBundle().get(key);
    }

    public void draw(float delta) {
        stage.getViewport().apply();
        stage.act(delta);
        stage.draw();
    }

    public Stage getStage() {
        return stage;
    }

    @Override
    public void dispose() {
        stage.dispose();
    }
}
