package com.joanathanps.battlearena.entities.tiles;

import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.joanathanps.battlearena.entities.Enemy;
import com.joanathanps.battlearena.forge.WorldBuilder;
import com.joanathanps.battlearena.scenes.Match;

import java.util.ArrayList;

import static com.joanathanps.battlearena.scheme.PhysicsAdapter.BUILDING_TAG;
import static com.joanathanps.battlearena.scheme.PhysicsAdapter.pUnscale;

public class Building extends InteractiveTile {

    private enum TraverseMode {
        STORE, HIDE, SHOW
    }

    private int[][] ceilingTileIds;
    private final int CEILING_LAYER = WorldBuilder.Layer.CEILING.index;
    private final int TRANSPARENT_TILE = 40;
    private final int CEILING_AREA = 31;

    /** Half-extents in Box2D world units, stored at construction from the tile bounds. */
    private float halfW;
    private float halfH;

    public Building(Rectangle bounds, Match match) {
        super(bounds, match, true, BUILDING_TAG);
        // Store half-extents in Box2D units (bounds are in pixel units, divide by 100).
        halfW = (bounds.getWidth()  / 2f) / 100f;
        halfH = (bounds.getHeight() / 2f) / 100f;
        setUserData();
        storeCeilingTiles();
    }

    private void setUserData() {
        getFixture().setUserData(this);
    }

    private void storeCeilingTiles() {
        ceilingTileIds = new int[CEILING_AREA][CEILING_AREA];
        traverseCeilingTiles(TraverseMode.STORE);
    }

    private void traverseCeilingTiles(TraverseMode mode) {
        TiledMapTileLayer layer = (TiledMapTileLayer) getMap().getLayers().get(CEILING_LAYER);
        for (int i = -((CEILING_AREA / 2) - 1); i <= ((CEILING_AREA / 2) - 1); i++) {
            for (int j = -((CEILING_AREA / 2) - 1); j <= ((CEILING_AREA / 2) - 1); j++) {
                if (layer.getCell((int)((pUnscale(getBody().getPosition().x) / (CEILING_AREA + 1)) - i),
                        (int)((pUnscale(getBody().getPosition().y) / (CEILING_AREA + 1)) - j)) != null) {
                    switch (mode) {
                        case STORE:
                            ceilingTileIds[i + ((CEILING_AREA / 2) - 1)][j + ((CEILING_AREA / 2) - 1)] =
                                    layer.getCell(
                                            (int)((pUnscale(getBody().getPosition().x) / (CEILING_AREA + 1)) - i),
                                            (int)((pUnscale(getBody().getPosition().y) / (CEILING_AREA + 1)) - j))
                                            .getTile().getId();
                            break;
                        case HIDE:
                            layer.getCell(
                                    (int)((pUnscale(getBody().getPosition().x) / (CEILING_AREA + 1)) - i),
                                    (int)((pUnscale(getBody().getPosition().y) / (CEILING_AREA + 1)) - j))
                                    .setTile(getMap().getTileSets().getTile(TRANSPARENT_TILE));
                            break;
                        case SHOW:
                            layer.getCell(
                                    (int)((pUnscale(getBody().getPosition().x) / (CEILING_AREA + 1)) - i),
                                    (int)((pUnscale(getBody().getPosition().y) / (CEILING_AREA + 1)) - j))
                                    .setTile(getMap().getTileSets().getTile(
                                            ceilingTileIds[i + ((CEILING_AREA / 2) - 1)]
                                                    [j + ((CEILING_AREA / 2) - 1)]));
                            break;
                    }
                }
            }
        }
    }

    @Override
    public void onPlayerEnter() {
        traverseCeilingTiles(TraverseMode.HIDE);
    }

    @Override
    public void onPlayerLeave() {
        traverseCeilingTiles(TraverseMode.SHOW);
    }

    @Override
    public void onEnemyEnter(Enemy enemy) {

    }

    @Override
    public void onEnemyLeave(Enemy enemy) {

    }

    /**
     * Returns four entry-point positions just outside each side of this
     * building (North, South, East, West).  Bots navigate to one of these
     * before attempting to reach loot that is inside the building, so they
     * approach from a valid opening rather than walking through the wall.
     *
     * <p>The padding (1.0 world unit) places the waypoint just beyond the
     * building's physical fixture so the bot is clearly outside before entering.
     */
    public ArrayList<Vector2> getEntryPoints() {
        ArrayList<Vector2> points = new ArrayList<>();
        Vector2 center = getBody().getPosition();
        float pad = 1.0f; // world-unit padding beyond the wall

        points.add(new Vector2(center.x,              center.y + halfH + pad)); // North
        points.add(new Vector2(center.x,              center.y - halfH - pad)); // South
        points.add(new Vector2(center.x + halfW + pad, center.y));              // East
        points.add(new Vector2(center.x - halfW - pad, center.y));              // West

        return points;
    }
}
