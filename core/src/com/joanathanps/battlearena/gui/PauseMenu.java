package com.joanathanps.battlearena.gui;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.joanathanps.battlearena.audio.AudioManager;
import com.joanathanps.battlearena.graphics.FontGenerator;
import com.joanathanps.battlearena.graphics.ResourceHandler;
import com.joanathanps.battlearena.scenes.Match;

import static com.joanathanps.battlearena.scheme.PlayerSettings.GAME_HEIGHT;
import static com.joanathanps.battlearena.scheme.PlayerSettings.GAME_WIDTH;

/** Esc menu. The match is frozen while it is open; Esc again resumes. */
public class PauseMenu implements Disposable {

    private final Match match;
    private final Stage stage;

    public PauseMenu(final Match match) {
        this.match = match;
        stage = new Stage(new FitViewport(GAME_WIDTH, GAME_HEIGHT, new OrthographicCamera()), match.getBatch());
        Skin skin = match.getResources().getSkin(ResourceHandler.SkinPaths.BUTTON_SKIN);

        Image scrim = new Image(match.getResources().getTexture(ResourceHandler.TexturePath.BLACK_BACKGROUND));
        scrim.setBounds(0, 0, GAME_WIDTH, GAME_HEIGHT);
        scrim.setColor(0, 0, 0, 0.6f);
        stage.addActor(scrim);

        Table table = new Table();
        table.setFillParent(true);
        table.center();

        Label title = new Label(text("paused"), new Label.LabelStyle(
                FontGenerator.generate(ResourceHandler.FontPath.BOMBARD, 42, true), Color.WHITE));
        table.add(title).padBottom(24).row();

        addButton(table, skin, text("pauseMenuResume"), new Runnable() {
            public void run() {
                match.resumeMatch();
            }
        });
        addButton(table, skin, text("pauseMenuBack"), new Runnable() {
            public void run() {
                match.backToMenu();
            }
        });
        if (Gdx.app.getType() != Application.ApplicationType.WebGL) {
            addButton(table, skin, text("quit"), new Runnable() {
                public void run() {
                    Gdx.app.exit();
                }
            });
        }
        stage.addActor(table);
    }

    static void addButton(Table table, Skin skin, String label, final Runnable action) {
        TextButton button = new TextButton(label, skin, "white-button");
        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                AudioManager.playClick();
                action.run();
            }
        });
        table.add(button).left().padTop(-4).row();
    }

    private String text(String key) {
        return match.getI18n().getBundle().get(key);
    }

    public void update(float delta) {
        stage.act(delta);
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            match.resumeMatch();
        }
    }

    public void draw() {
        stage.getViewport().apply();
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
