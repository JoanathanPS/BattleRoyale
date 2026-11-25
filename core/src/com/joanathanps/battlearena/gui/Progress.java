package com.joanathanps.battlearena.gui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.joanathanps.battlearena.entities.objects.PlayerInventory;
import com.joanathanps.battlearena.graphics.FontGenerator;
import com.joanathanps.battlearena.graphics.ResourceHandler;
import com.joanathanps.battlearena.scenes.Match;

import static com.joanathanps.battlearena.scheme.PhysicsAdapter.pCenter;
import static com.joanathanps.battlearena.scheme.PlayerSettings.GAME_HEIGHT;
import static com.joanathanps.battlearena.scheme.PlayerSettings.GAME_WIDTH;

/** The timed bar shown while the player reloads or heals. */
public class Progress implements Disposable {

    private static final float BAR_WIDTH = 120;
    private static final float BAR_HEIGHT = 10;

    private final Match match;
    private final Stage stage;
    private Image progressBar;
    private Label activity;
    private float duration;
    private float elapsed;
    private boolean active;

    public Progress(Match match) {
        this.match = match;
        stage = new Stage(new FitViewport(GAME_WIDTH, GAME_HEIGHT, new OrthographicCamera()), match.getBatch());
        float x = pCenter(GAME_WIDTH) - BAR_WIDTH / 2;

        Image emptyBar = new Image(match.getResources().getTexture(ResourceHandler.TexturePath.EMPTY_BAR));
        emptyBar.setBounds(x, 120, BAR_WIDTH, BAR_HEIGHT);
        stage.addActor(emptyBar);

        progressBar = new Image(match.getResources().getTexture(ResourceHandler.TexturePath.GENERIC_PROGRESS_BAR));
        progressBar.setBounds(x, 120, 0, BAR_HEIGHT);
        stage.addActor(progressBar);

        activity = new Label("", new Label.LabelStyle(
                FontGenerator.generate(ResourceHandler.FontPath.BOMBARD, 18, true), Color.WHITE));
        Table table = new Table();
        table.bottom();
        table.setFillParent(true);
        table.add(activity).padBottom(136);
        stage.addActor(table);
    }

    public void start(String activityKey, float seconds) {
        activity.setText(match.getI18n().getBundle().get(activityKey));
        duration = Math.max(0.1f, seconds);
        elapsed = 0;
        active = true;
        progressBar.setWidth(0);
    }

    public void cancel() {
        if (active) {
            active = false;
            ((PlayerInventory) match.getPlayer().getInventory()).cancelAction();
        }
    }

    public void update(float delta) {
        if (!active) {
            return;
        }
        elapsed += delta;
        progressBar.setWidth(BAR_WIDTH * Math.min(1f, elapsed / duration));
        if (elapsed >= duration) {
            active = false;
            ((PlayerInventory) match.getPlayer().getInventory()).finishAction();
        }
    }

    public boolean isActive() {
        return active;
    }

    public void draw() {
        if (active) {
            stage.getViewport().apply();
            stage.draw();
        }
    }

    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        stage.dispose();
    }
}
