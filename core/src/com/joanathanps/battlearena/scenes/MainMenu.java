package com.joanathanps.battlearena.scenes;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.joanathanps.battlearena.BattleRoyaleLite;
import com.joanathanps.battlearena.audio.AudioManager;
import com.joanathanps.battlearena.events.InputTracker;
import com.joanathanps.battlearena.graphics.FontGenerator;
import com.joanathanps.battlearena.graphics.ResourceHandler;
import com.joanathanps.battlearena.gui.LeaderboardScreen;
import com.joanathanps.battlearena.gui.SettingsScreen;
import com.joanathanps.battlearena.languages.Internationalization;
import com.joanathanps.battlearena.scheme.Difficulty;
import com.joanathanps.battlearena.scheme.PlayerSettings;

import static com.joanathanps.battlearena.scheme.PlayerSettings.GAME_HEIGHT;
import static com.joanathanps.battlearena.scheme.PlayerSettings.GAME_WIDTH;

/** Title screen over the parallax island: name, difficulty, play, leaderboard, settings. */
public class MainMenu implements Screen {

    private static final Color SELECTED = Color.valueOf("ffd700");
    private static final Color HINT = new Color(0.92f, 0.92f, 0.92f, 1f);

    private final BattleRoyaleLite game;
    private final SpriteBatch batch;
    private final ResourceHandler resources;
    private final Internationalization i18n = new Internationalization();

    private final OrthographicCamera camera = new OrthographicCamera();
    private final Viewport viewport = new FitViewport(GAME_WIDTH, GAME_HEIGHT, camera);
    private final Stage stage;
    private final Stage loadingStage;

    private final Texture backgroundP0;
    private final Texture backgroundP1;
    private final Texture backgroundP2;
    private final Texture backgroundP3;
    private final Texture vignette;
    private final Texture logo;
    private final Texture loadingScreen;

    private Difficulty selectedDifficulty = PlayerSettings.getDifficulty();
    private TextField nameField;
    private Label difficultyHint;
    /** Frames drawn since Play was pressed; the match loads once the loading screen is on screen. */
    private int loadingFrames = -1;

    public MainMenu() {
        game = BattleRoyaleLite.get();
        batch = game.getBatch();
        resources = game.getResources();
        backgroundP0 = resources.getTexture(ResourceHandler.TexturePath.MENU_P0);
        backgroundP1 = resources.getTexture(ResourceHandler.TexturePath.MENU_P1);
        backgroundP2 = resources.getTexture(ResourceHandler.TexturePath.MENU_P2);
        backgroundP3 = resources.getTexture(ResourceHandler.TexturePath.MENU_P3);
        vignette = resources.getTexture(ResourceHandler.TexturePath.VIGNETTE);
        logo = resources.getTexture(ResourceHandler.TexturePath.MENU_LOGO);
        loadingScreen = resources.getTexture(ResourceHandler.TexturePath.LOADING_SCREEN);

        camera.position.set(GAME_WIDTH / 2f, GAME_HEIGHT / 2f, 0);
        game.useCursor(false);

        stage = new Stage(viewport, batch);
        loadingStage = new Stage(viewport, batch);
        buildMenu();
        buildLoadingScreen();
        Gdx.input.setInputProcessor(stage);
    }

    private void buildMenu() {
        Skin buttons = resources.getSkin(ResourceHandler.SkinPaths.BUTTON_SKIN);
        Skin window = resources.getSkin(ResourceHandler.SkinPaths.WINDOW_SKIN);
        Label.LabelStyle caption = new Label.LabelStyle(
                FontGenerator.generate(ResourceHandler.FontPath.BOMBARD, 18, true), Color.WHITE);

        Table table = new Table();
        table.setFillParent(true);
        table.left().top();
        table.padTop(196).padLeft(GAME_WIDTH / 2f - logo.getWidth() / 2f + 20);

        table.add(new Label(text("name"), caption)).left().padLeft(10).row();
        nameField = new TextField(PlayerSettings.getPlayerName(), window);
        nameField.setMaxLength(PlayerSettings.NAME_MAX_LENGTH);
        nameField.setTextFieldFilter(new TextField.TextFieldFilter() {
            @Override
            public boolean acceptChar(TextField field, char c) {
                return Character.isLetterOrDigit(c) || c == ' ' || c == '-' || c == '_';
            }
        });
        table.add(nameField).width(330).height(44).left().padLeft(6).padBottom(14).row();

        table.add(new Label(text("difficulty"), caption)).left().padLeft(10).row();
        Table difficultyRow = new Table();
        final ButtonGroup<TextButton> group = new ButtonGroup<TextButton>();
        group.setMinCheckCount(1);
        group.setMaxCheckCount(1);
        for (final Difficulty difficulty : Difficulty.values()) {
            final TextButton button = new TextButton(difficulty.getLabel(), buttons, "white-button");
            group.add(button);
            button.setChecked(difficulty == selectedDifficulty);
            button.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    if (button.isChecked() && selectedDifficulty != difficulty) {
                        AudioManager.playClick();
                        selectedDifficulty = difficulty;
                        PlayerSettings.setDifficulty(difficulty);
                        refreshDifficulty(group);
                    }
                }
            });
            difficultyRow.add(button).padRight(8);
        }
        table.add(difficultyRow).left().row();
        difficultyHint = new Label("", new Label.LabelStyle(
                FontGenerator.generate(ResourceHandler.FontPath.BOMBARD, 14, true), HINT));
        table.add(difficultyHint).left().padLeft(10).padBottom(10).row();
        refreshDifficulty(group);

        addMenuButton(table, buttons, text("play"), new Runnable() {
            public void run() {
                startMatch();
            }
        });
        addMenuButton(table, buttons, text("leaderboard"), new Runnable() {
            public void run() {
                saveName();
                game.setScreen(new LeaderboardScreen(game.getMatchResultsRepository()));
            }
        });
        addMenuButton(table, buttons, text("settings"), new Runnable() {
            public void run() {
                saveName();
                game.setScreen(new SettingsScreen());
            }
        });
        if (Gdx.app.getType() != Application.ApplicationType.WebGL) {
            addMenuButton(table, buttons, text("quit"), new Runnable() {
                public void run() {
                    Gdx.app.exit();
                }
            });
        }
        stage.addActor(table);
    }

    private void addMenuButton(Table table, Skin skin, String label, final Runnable action) {
        TextButton button = new TextButton(label, skin, "white-button");
        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                AudioManager.playClick();
                action.run();
            }
        });
        table.add(button).left().padTop(-6).row();
    }

    private void refreshDifficulty(ButtonGroup<TextButton> group) {
        for (TextButton button : group.getButtons()) {
            button.setColor(button.isChecked() ? SELECTED : Color.WHITE);
        }
        difficultyHint.setText(selectedDifficulty.getBotCount() + " " + text("bots"));
    }

    private void buildLoadingScreen() {
        Table table = new Table();
        table.setFillParent(true);
        table.right().bottom();
        table.add(new com.badlogic.gdx.scenes.scene2d.ui.Image(
                resources.getTexture(ResourceHandler.TexturePath.GAME_ICON))).padBottom(70).padRight(5);
        table.add(new Label(text("loading"), new Label.LabelStyle(
                FontGenerator.generate(ResourceHandler.FontPath.BOMBARD, 42, false), Color.WHITE)))
                .padBottom(70).padRight(100);
        loadingStage.addActor(table);
    }

    private void saveName() {
        PlayerSettings.setPlayerName(nameField.getText());
    }

    private void startMatch() {
        if (loadingFrames < 0) {
            saveName();
            loadingFrames = 0;
            Gdx.input.setInputProcessor(null);
        }
    }

    private String text(String key) {
        return i18n.getBundle().get(key);
    }

    @Override
    public void render(float delta) {
        if (loadingFrames < 0 && Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            startMatch();
        }

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        viewport.apply();
        batch.setProjectionMatrix(camera.combined);

        if (loadingFrames >= 0) {
            batch.begin();
            batch.draw(loadingScreen, 0, 0, GAME_WIDTH, GAME_HEIGHT);
            batch.end();
            loadingStage.draw();
            // Two frames guarantee the loading screen is visible before the blocking load starts.
            if (++loadingFrames > 2) {
                game.setScreen(new Match(selectedDifficulty));
            }
            return;
        }

        float mx = InputTracker.getRelativeX() * GAME_WIDTH / Gdx.graphics.getWidth();
        float my = InputTracker.getRelativeY() * GAME_HEIGHT / Gdx.graphics.getHeight();
        batch.begin();
        batch.draw(backgroundP0, 0, 0, GAME_WIDTH, GAME_HEIGHT);
        batch.draw(backgroundP1,
                (GAME_WIDTH - backgroundP1.getWidth()) / 2f - mx / 10,
                (GAME_HEIGHT - backgroundP1.getHeight()) / 2f + my / 10);
        batch.draw(logo,
                (GAME_WIDTH - logo.getWidth()) / 2f - mx / 15,
                GAME_HEIGHT - GAME_HEIGHT / 3f + 30 + my / 15);
        batch.draw(backgroundP2,
                (GAME_WIDTH - backgroundP2.getWidth() / 2f) / 2f - mx / 30,
                (GAME_HEIGHT - backgroundP2.getHeight() / 2f) / 2f + 450 + my / 30,
                backgroundP2.getWidth() / 2f, backgroundP2.getHeight() / 2f);
        batch.draw(backgroundP3,
                (GAME_WIDTH - backgroundP3.getWidth() / 2f) / 2f + 50 - mx / 15,
                (GAME_HEIGHT - backgroundP3.getHeight() / 2f) / 2f + 450 + my / 15,
                backgroundP3.getWidth() / 2f, backgroundP3.getHeight() / 2f);
        batch.draw(vignette, 0, 0, GAME_WIDTH, GAME_HEIGHT);
        batch.end();

        stage.act(delta);
        stage.draw();
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
        loadingStage.dispose();
    }
}
