package com.joanathanps.battlearena.gui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.joanathanps.battlearena.events.SafeZoneController;
import com.joanathanps.battlearena.graphics.FontGenerator;
import com.joanathanps.battlearena.graphics.ResourceHandler;
import com.joanathanps.battlearena.scenes.Match;

import static com.joanathanps.battlearena.scheme.PlayerSettings.GAME_HEIGHT;
import static com.joanathanps.battlearena.scheme.PlayerSettings.GAME_WIDTH;

/** The full-screen island map opened with M: current zone, next circle and the player. */
public class MatchMap implements Disposable {

    /** Map pixels per world unit: the 600 px map covers the 82-unit island. */
    private static final float SCALE = 7.3f;

    private final Match match;
    private final Viewport viewport = new FitViewport(GAME_WIDTH, GAME_HEIGHT, new OrthographicCamera());
    private final ShapeRenderer shapes = new ShapeRenderer();
    private final Texture map;
    private final Texture playerMark;
    private final BitmapFont hint = FontGenerator.generate(ResourceHandler.FontPath.BOMBARD, 14, true);
    private final float originX;
    private final float originY;

    public MatchMap(Match match) {
        this.match = match;
        viewport.getCamera().position.set(GAME_WIDTH / 2f, GAME_HEIGHT / 2f, 0);
        map = match.getResources().getTexture(ResourceHandler.TexturePath.MATCH_MAP);
        playerMark = match.getResources().getTexture(ResourceHandler.TexturePath.PLAYER_MARK);
        originX = (GAME_WIDTH - map.getWidth()) / 2f;
        originY = (GAME_HEIGHT - map.getHeight()) / 2f;
    }

    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    public void draw() {
        viewport.apply();
        Batch batch = match.getBatch();
        batch.setProjectionMatrix(viewport.getCamera().combined);

        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapes.setProjectionMatrix(viewport.getCamera().combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0f, 0f, 0f, 0.75f);
        shapes.rect(0, 0, GAME_WIDTH, GAME_HEIGHT);
        shapes.end();

        batch.begin();
        batch.setColor(Color.WHITE);
        batch.draw(map, originX, originY);
        batch.end();

        SafeZoneController zone = match.getSafeZoneController();
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        float zx = originX + zone.getCenter().x * SCALE;
        float zy = originY + zone.getCenter().y * SCALE;
        float zr = zone.getRadius() * SCALE;
        // Clip the danger tint to the map square by drawing it only out to the map's corners.
        SafeZoneController.drawRing(shapes, zx, zy, zr, zr + 900, 0.78f, 0.17f, 0.13f, 0.3f);
        SafeZoneController.drawRing(shapes, zx, zy, zr - 1.2f, zr + 1.2f, 0.86f, 0.22f, 0.18f, 1f);
        if (zone.hasNextCircle()) {
            Vector2 next = zone.getNextCenter();
            float nr = zone.getNextRadius() * SCALE;
            SafeZoneController.drawRing(shapes, originX + next.x * SCALE, originY + next.y * SCALE,
                    nr - 1f, nr + 1f, 1f, 1f, 1f, 0.95f);
        }
        // Cover the tint that spilled past the map edges.
        shapes.setColor(0f, 0f, 0f, 1f);
        shapes.rect(0, 0, originX, GAME_HEIGHT);
        shapes.rect(originX + map.getWidth(), 0, GAME_WIDTH, GAME_HEIGHT);
        shapes.rect(originX, 0, map.getWidth(), originY);
        shapes.rect(originX, originY + map.getHeight(), map.getWidth(), GAME_HEIGHT);
        shapes.end();

        Vector2 player = match.getPlayer().getBody().getPosition();
        batch.begin();
        batch.draw(playerMark, originX + player.x * SCALE - playerMark.getWidth() / 2f,
                originY + player.y * SCALE - playerMark.getHeight() / 2f);
        hint.setColor(Color.WHITE);
        hint.draw(batch, match.getI18n().getBundle().get("mapHint"), originX, originY - 12);
        batch.end();
    }

    @Override
    public void dispose() {
        shapes.dispose();
    }
}
