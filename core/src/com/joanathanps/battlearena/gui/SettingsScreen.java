package com.joanathanps.battlearena.gui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.joanathanps.battlearena.BattleRoyaleArenaLite;
import com.joanathanps.battlearena.graphics.FontGenerator;
import com.joanathanps.battlearena.graphics.ResourceHandler;
import com.joanathanps.battlearena.languages.Internationalization;
import com.joanathanps.battlearena.scenes.MainMenu;
import com.joanathanps.battlearena.scheme.PlayerSettings;
import com.joanathanps.battlearena.scheme.SystemProps;
import com.joanathanps.battlearena.scheme.PlayerSettings.Resolution;

import static com.joanathanps.battlearena.scheme.PhysicsAdapter.pCenter;
import static com.joanathanps.battlearena.scheme.PlayerSettings.GAME_HEIGHT;
import static com.joanathanps.battlearena.scheme.PlayerSettings.GAME_WIDTH;

/**
 * Clean Settings screen featuring:
 *   1. DISPLAY - Resolution selection & Fullscreen / Windowed toggle
 *   2. Actions - Apply (commits changes to graphics & preferences) and Back
 */
public class SettingsScreen implements Screen {

    // Stage / skin
    private Stage stage;
    private Viewport viewport;
    private Skin skin;

    // i18n
    private Internationalization i18n;

    // Controls (pending-state)
    private SelectBox<String> resolutionBox;
    private ButtonGroup<TextButton> windowModeGroup;
    private TextButton fullscreenBtn;
    private TextButton windowedBtn;

    // Pending values (only written to PlayerSettings on Apply)
    private Resolution pendingResolution;
    private boolean pendingFullscreen;

    // Layout constants
    private static final int WINDOW_W = 540;
    private static final int WINDOW_H = 380;
    private static final Color ACCENT = Color.valueOf("ffd700"); // gold
    private static final Color SECTION = Color.valueOf("aaaaaa"); // grey

    // Auto-test wiring
    private static final boolean AUTOSETTINGS =
            SystemProps.getBoolean("bral.autosettings");
    private static final float AUTOSETTINGS_SECONDS =
            Float.parseFloat(SystemProps.getProperty("bral.autosettings.seconds", "3"));
    private float autoExitElapsed;

    public SettingsScreen() {
        i18n = new Internationalization();
        pendingResolution = PlayerSettings.getResolution();
        pendingFullscreen = PlayerSettings.getFullscreen();

        setupStage();
        buildUI();
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

    private void buildUI() {
        Window window = new Window("", skin, "no-stage-background");
        window.setSize(WINDOW_W, WINDOW_H);
        window.setPosition(pCenter(GAME_WIDTH) - pCenter(WINDOW_W),
                pCenter(GAME_HEIGHT) - pCenter(WINDOW_H));

        Table content = new Table();
        content.top().pad(15);

        // Title
        content.add(titleLabel(i18n.getBundle().get("settings")))
                .padTop(10).padBottom(25).row();

        // DISPLAY section
        addSectionHeader(content, i18n.getBundle().get("settingsDisplay"));
        buildResolutionRow(content);
        buildWindowModeRow(content);

        content.add(new Label("", skin)).height(25).row(); // spacer

        // Footer: Apply + Back
        buildFooterRow(content);

        ScrollPane scroll = new ScrollPane(content, skin);
        scroll.setFadeScrollBars(false);
        window.add(scroll).expand().fill();

        stage.addActor(window);
    }

    private void addSectionHeader(Table t, String text) {
        Label lbl = new Label("-- " + text + " --",
                new Label.LabelStyle(
                        FontGenerator.generate(ResourceHandler.FontPath.BOMBARD, 20, false),
                        SECTION));
        t.add(lbl).padBottom(15).row();
    }

    private void buildResolutionRow(Table t) {
        Label lbl = rowLabel(i18n.getBundle().get("settingsResolution") + ":");

        Array<String> items = new Array<>();
        for (Resolution r : Resolution.values()) {
            items.add(r.label);
        }
        resolutionBox = new SelectBox<>(skin);
        resolutionBox.setItems(items);
        resolutionBox.setSelected(pendingResolution.label);

        resolutionBox.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                pendingResolution = resolveByLabel(resolutionBox.getSelected());
            }
        });

        Table row = new Table();
        row.add(lbl).width(200).left();
        row.add(resolutionBox).width(220);
        t.add(row).padBottom(15).row();
    }

    private Resolution resolveByLabel(String label) {
        for (Resolution r : Resolution.values()) {
            if (r.label.equals(label)) return r;
        }
        return Resolution.R_1280x720;
    }

    private void buildWindowModeRow(Table t) {
        Label lbl = rowLabel(i18n.getBundle().get("settingsWindowMode") + ":");

        fullscreenBtn = toggleButton(i18n.getBundle().get("settingsFullscreen"));
        windowedBtn = toggleButton(i18n.getBundle().get("settingsWindowed"));

        windowModeGroup = new ButtonGroup<>(fullscreenBtn, windowedBtn);
        windowModeGroup.setMinCheckCount(1);
        windowModeGroup.setMaxCheckCount(1);

        if (pendingFullscreen) {
            fullscreenBtn.setChecked(true);
        } else {
            windowedBtn.setChecked(true);
        }
        refreshWindowModeColors();

        ChangeListener modeListener = new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                pendingFullscreen = fullscreenBtn.isChecked();
                refreshWindowModeColors();
            }
        };
        fullscreenBtn.addListener(modeListener);
        windowedBtn.addListener(modeListener);

        Table row = new Table();
        row.add(lbl).width(200).left();
        row.add(fullscreenBtn).width(110).padRight(10);
        row.add(windowedBtn).width(110);
        t.add(row).padBottom(15).row();
    }

    private void refreshWindowModeColors() {
        fullscreenBtn.setColor(fullscreenBtn.isChecked() ? ACCENT : Color.WHITE);
        windowedBtn.setColor(windowedBtn.isChecked() ? ACCENT : Color.WHITE);
    }

    private void buildFooterRow(Table t) {
        TextButton applyBtn = new TextButton(i18n.getBundle().get("settingsApply"), skin);
        applyBtn.setColor(ACCENT);
        applyBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                applySettings();
            }
        });

        TextButton backBtn = new TextButton(i18n.getBundle().get("pauseMenuBack"), skin);
        backBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                goBack();
            }
        });

        Table row = new Table();
        row.add(applyBtn).width(150).padRight(20);
        row.add(backBtn).width(200);
        t.add(row).padBottom(10).row();
    }

    private void applySettings() {
        PlayerSettings.setResolution(pendingResolution);
        PlayerSettings.setFullscreen(pendingFullscreen);

        if (pendingFullscreen) {
            Graphics.DisplayMode displayMode = Gdx.graphics.getDisplayMode();
            Gdx.graphics.setFullscreenMode(displayMode);
        } else {
            Gdx.graphics.setWindowedMode(pendingResolution.w, pendingResolution.h);
        }
    }

    private void goBack() {
        BattleRoyaleArenaLite game =
                (BattleRoyaleArenaLite) Gdx.app.getApplicationListener();
        game.setScreen(new MainMenu(game.getBatch()));
    }

    private Label titleLabel(String text) {
        return new Label(text,
                new Label.LabelStyle(
                        FontGenerator.generate(ResourceHandler.FontPath.BOMBARD, 28, false),
                        Color.WHITE));
    }

    private Label rowLabel(String text) {
        return new Label(text,
                new Label.LabelStyle(
                        FontGenerator.generate(ResourceHandler.FontPath.BOMBARD, 20, false),
                        Color.WHITE));
    }

    private TextButton toggleButton(String text) {
        return new TextButton(text, skin);
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float delta) {
        if (AUTOSETTINGS) {
            autoExitElapsed += delta;
            if (autoExitElapsed >= AUTOSETTINGS_SECONDS) {
                PlayerSettings.setFullscreen(false);
                PlayerSettings.setResolution(Resolution.R_1280x720);
                boolean pass = !PlayerSettings.getFullscreen()
                        && PlayerSettings.getResolution() == Resolution.R_1280x720;
                Gdx.app.log("SETTINGS", "settings autotest PASS=" + pass
                        + " (fullscreen=" + PlayerSettings.getFullscreen()
                        + ", resolution=" + PlayerSettings.getResolution() + ")");
                Gdx.app.exit();
            }
        }

        Gdx.gl.glClearColor(0.08f, 0.08f, 0.10f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override public void pause() { }
    @Override public void resume() { }
    @Override public void hide() { }

    @Override
    public void dispose() {
        stage.dispose();
        skin.dispose();
    }

    public Stage getStage() {
        return stage;
    }
}