package com.joanathanps.battlearena.gui;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.joanathanps.battlearena.BattleRoyaleLite;
import com.joanathanps.battlearena.audio.AudioManager;
import com.joanathanps.battlearena.graphics.FontGenerator;
import com.joanathanps.battlearena.graphics.ResourceHandler;
import com.joanathanps.battlearena.languages.Internationalization;
import com.joanathanps.battlearena.scenes.MainMenu;
import com.joanathanps.battlearena.scheme.PlayerSettings;
import com.joanathanps.battlearena.scheme.PlayerSettings.Resolution;

import static com.joanathanps.battlearena.scheme.PlayerSettings.GAME_HEIGHT;
import static com.joanathanps.battlearena.scheme.PlayerSettings.GAME_WIDTH;

/** Window size and fullscreen. Nothing changes until Apply. The browser build only offers fullscreen. */
public class SettingsScreen implements Screen {

    private static final Color SELECTED = Color.valueOf("ffd700");

    private final BattleRoyaleLite game = BattleRoyaleLite.get();
    private final Internationalization i18n = new Internationalization();
    private final Viewport viewport = new FitViewport(GAME_WIDTH, GAME_HEIGHT, new OrthographicCamera());
    private final Stage stage;
    private final MenuBackdrop backdrop;
    private final Skin skin;

    private Resolution pendingResolution = PlayerSettings.getResolution();
    private boolean pendingFullscreen = PlayerSettings.getFullscreen();

    public SettingsScreen() {
        stage = new Stage(viewport, game.getBatch());
        backdrop = new MenuBackdrop(game.getResources());
        skin = game.getResources().getSkin(ResourceHandler.SkinPaths.BUTTON_SKIN);
        boolean web = Gdx.app.getType() == Application.ApplicationType.WebGL;

        Table root = new Table();
        root.setFillParent(true);
        root.center();
        root.add(new Label(text("settings"), style(42, Color.WHITE))).left().padBottom(24).row();

        if (!web) {
            root.add(new Label(text("settingsResolution"), style(18, Color.WHITE))).left().padLeft(10).row();
            final ButtonGroup<TextButton> sizes = new ButtonGroup<TextButton>();
            Table sizeRow = new Table();
            for (final Resolution resolution : Resolution.values()) {
                final TextButton button = toggle(resolution.label, resolution == pendingResolution, sizes);
                button.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        if (button.isChecked()) {
                            pendingResolution = resolution;
                            paint(sizes);
                        }
                    }
                });
                sizeRow.add(button).padRight(8);
            }
            paint(sizes);
            root.add(sizeRow).left().padBottom(16).row();
        }

        root.add(new Label(text("settingsWindowMode"), style(18, Color.WHITE))).left().padLeft(10).row();
        final ButtonGroup<TextButton> modes = new ButtonGroup<TextButton>();
        final TextButton windowed = toggle(text("settingsWindowed"), !pendingFullscreen, modes);
        final TextButton fullscreen = toggle(text("settingsFullscreen"), pendingFullscreen, modes);
        ChangeListener modeListener = new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                pendingFullscreen = fullscreen.isChecked();
                paint(modes);
            }
        };
        windowed.addListener(modeListener);
        fullscreen.addListener(modeListener);
        paint(modes);
        Table modeRow = new Table();
        modeRow.add(windowed).padRight(8);
        modeRow.add(fullscreen);
        root.add(modeRow).left().padBottom(28).row();

        PauseMenu.addButton(root, skin, text("settingsApply"), new Runnable() {
            public void run() {
                apply();
            }
        });
        PauseMenu.addButton(root, skin, text("pauseMenuBack"), new Runnable() {
            public void run() {
                back();
            }
        });
        stage.addActor(root);
        Gdx.input.setInputProcessor(stage);
    }

    private TextButton toggle(String label, boolean checked, ButtonGroup<TextButton> group) {
        TextButton button = new TextButton(label, skin, "white-button");
        group.add(button);
        group.setMinCheckCount(1);
        group.setMaxCheckCount(1);
        button.setChecked(checked);
        return button;
    }

    private void paint(ButtonGroup<TextButton> group) {
        for (TextButton button : group.getButtons()) {
            button.setColor(button.isChecked() ? SELECTED : Color.WHITE);
        }
    }

    private void apply() {
        PlayerSettings.setResolution(pendingResolution);
        PlayerSettings.setFullscreen(pendingFullscreen);
        if (pendingFullscreen) {
            Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
        } else {
            Gdx.graphics.setWindowedMode(pendingResolution.w, pendingResolution.h);
        }
        AudioManager.playClick();
    }

    private void back() {
        game.setScreen(new MainMenu());
    }

    private String text(String key) {
        return i18n.getBundle().get(key);
    }

    private Label.LabelStyle style(int size, Color color) {
        return new Label.LabelStyle(FontGenerator.generate(ResourceHandler.FontPath.BOMBARD, size, true), color);
    }

    @Override
    public void render(float delta) {
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
