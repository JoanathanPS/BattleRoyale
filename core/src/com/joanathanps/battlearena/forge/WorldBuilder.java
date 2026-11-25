package com.joanathanps.battlearena.forge;

import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.EllipseMapObject;
import com.badlogic.gdx.maps.objects.PolygonMapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.math.Ellipse;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.utils.Array;
import com.joanathanps.battlearena.entities.BotNames;
import com.joanathanps.battlearena.entities.Enemy;
import com.joanathanps.battlearena.entities.Lootable;
import com.joanathanps.battlearena.entities.Soldier;
import com.joanathanps.battlearena.entities.tiles.Building;
import com.joanathanps.battlearena.entities.tiles.Chest;
import com.joanathanps.battlearena.entities.tiles.Crate;
import com.joanathanps.battlearena.entities.tiles.Loot;
import com.joanathanps.battlearena.events.NavigationWaypoint;
import com.joanathanps.battlearena.events.WaypointNavigator;
import com.joanathanps.battlearena.scenes.Match;

import java.util.ArrayList;


import static com.joanathanps.battlearena.scheme.PhysicsAdapter.*;

public class WorldBuilder {

    public enum Layer {
        OCEAN(0),
        ROCK(1),
        TREE(2),
        BUILDING(3),
        PLAYER_SPAWN_POINTS(4),
        BUILDING_SENSOR(5),
        CHEST(6),
        CRATE(7),
        ENEMY_SPAWN_POINTS(8),
        CEILING(24);
        Layer(int index) { this.index = index; }
        public final int index;
    }

    private ArrayList<Crate> crates;
    private ArrayList<Chest> chests;
    private ArrayList<Enemy> enemies;
    private ArrayList<Building> buildings;
    private ArrayList<Soldier> soldiers;
    private WaypointNavigator waypointNavigator;
    private Match match;

    public WorldBuilder(Match match) {
        this.match = match;
        init();
        buildPolygonalObject(Layer.OCEAN);
        buildEllipticalObject(Layer.ROCK);
        buildEllipticalObject(Layer.TREE);
        buildRectangularObject(Layer.BUILDING);
        buildChests();
        buildCrates();
        buildBuildingSensors();
        buildWaypointNavigator();
        buildEnemies();
    }

    private void init() {
        chests = new ArrayList<>();
        crates = new ArrayList<>();
        enemies = new ArrayList<>();
        buildings = new ArrayList<>();
        waypointNavigator = null; // built after building sensors
    }

    private void buildPolygonalObject(Layer layer) {
        for (MapObject object :
                match.getMap().getLayers().get(layer.index).getObjects().getByType(PolygonMapObject.class)) {
            Polygon pol = ((PolygonMapObject) object).getPolygon();
            new BodyBuilder(match.getWorld(), pScale(
                    pol.getX() + pCenter(pol.getOriginX()),
                    pol.getY() + pCenter(pol.getOriginY())))
                    .withBodyType(BodyDef.BodyType.StaticBody)
                    .withMaskBits((short) (PLAYER_TAG | ENEMY_TAG))
                    .withCategoryBits(WATER_TAG)
                    .withVertices(pScale(pol.getVertices()))
                    .build();
        }
    }

    private void buildEllipticalObject(Layer layer) {
        for (MapObject object :
                match.getMap().getLayers().get(layer.index).getObjects().getByType(EllipseMapObject.class)) {
            Ellipse ell = ((EllipseMapObject) object).getEllipse();
            new BodyBuilder(match.getWorld(), pScale(ell.x + pCenter(ell.width), ell.y + pCenter(ell.width)))
                    .withBodyType(BodyDef.BodyType.StaticBody)
                    .withRadius(pScaleCenter(ell.width))
                    .build();
        }
    }

    private void buildRectangularObject(Layer layer) {
        for (MapObject object :
                match.getMap().getLayers().get(layer.index).getObjects().getByType(RectangleMapObject.class)) {
            Rectangle rect = ((RectangleMapObject) object).getRectangle();
            new BodyBuilder(match.getWorld(), pScale(
                    rect.getX() + pCenter(rect.getWidth()),
                    rect.getY() + pCenter(rect.getHeight())))
                    .withBodyType(BodyDef.BodyType.StaticBody)
                    .withWidth(pScaleCenter(rect.getWidth()))
                    .withHeight(pScaleCenter(rect.getHeight()))
                    .build();
        }
    }

    private void buildChests() {
        for (MapObject object :
                match.getMap().getLayers().get(Layer.CHEST.index).getObjects().getByType(RectangleMapObject.class)) {
            Rectangle rect = ((RectangleMapObject) object).getRectangle();
            chests.add(new Chest(rect, match));
        }
    }

    private void buildCrates() {
        for (MapObject object :
                match.getMap().getLayers().get(Layer.CRATE.index).getObjects().getByType(RectangleMapObject.class)) {
            Rectangle rect = ((RectangleMapObject) object).getRectangle();
            crates.add(new Crate(rect, match));
        }
    }

    private void buildBuildingSensors() {
        for (MapObject object :
                match.getMap().getLayers().get(Layer.BUILDING_SENSOR.index).getObjects()
                        .getByType(RectangleMapObject.class)) {
            Rectangle rect = ((RectangleMapObject) object).getRectangle();
            buildings.add(new Building(rect, match));
        }
    }

    /**
     * Creates the {@link WaypointNavigator} and registers entry-point
     * waypoints for every building sensor on the map.
     *
     * <p>Must be called <em>after</em> {@link #buildBuildingSensors()} so
     * the buildings list is fully populated.
     */
    private void buildWaypointNavigator() {
        waypointNavigator = new WaypointNavigator(match.getWorld());
        int buildingIndex = 0;
        for (Building building : buildings) {
            ArrayList<Vector2> entryPoints = building.getEntryPoints();
            String[] labels = {"N", "S", "E", "W"};
            for (int i = 0; i < entryPoints.size(); i++) {
                String name = "Building_" + buildingIndex + "_" + labels[i];
                waypointNavigator.addWaypoint(new NavigationWaypoint(name, entryPoints.get(i)));
            }
            buildingIndex++;
        }
    }

    private void buildEnemies() {
        Array<Vector2> spawns = new Array<Vector2>();
        for (MapObject object : match.getMap().getLayers().get(Layer.ENEMY_SPAWN_POINTS.index).getObjects()
                .getByType(EllipseMapObject.class)) {
            Ellipse ellipse = ((EllipseMapObject) object).getEllipse();
            spawns.add(new Vector2(ellipse.x, ellipse.y));
        }
        spawns.shuffle();
        BotNames names = new BotNames();
        int count = Math.min(match.getDifficulty().getBotCount(), spawns.size);
        for (int i = 0; i < count; i++) {
            enemies.add(new Enemy(match, spawns.get(i), names.next()));
        }
    }

    public Vector2 getPlayerSpawnPoint() {
        Array<Vector2> spawns = new Array<Vector2>();
        for (MapObject object : match.getMap().getLayers().get(Layer.PLAYER_SPAWN_POINTS.index).getObjects()
                .getByType(EllipseMapObject.class)) {
            Ellipse ellipse = ((EllipseMapObject) object).getEllipse();
            spawns.add(new Vector2(ellipse.x, ellipse.y));
        }
        return spawns.random();
    }

    public ArrayList<Lootable> getAllLoot() {
        ArrayList<Lootable> loot = new ArrayList<>();
        loot.addAll(crates);
        loot.addAll(chests);
        for (Enemy enemy : enemies) {
            if (enemy.isDead()) {
                loot.add(enemy);
            }
        }
        return loot;
    }

    public ArrayList<Loot> getLoot() {
        ArrayList<Loot> loot = new ArrayList<>();
        loot.addAll(crates);
        loot.addAll(chests);
        return loot;
    }

    /** The player and every bot, dead or alive. */
    public ArrayList<Soldier> getSoldiers() {
        if (soldiers == null && match.getPlayer() != null) {
            soldiers = new ArrayList<>();
            soldiers.add(match.getPlayer());
            soldiers.addAll(enemies);
        }
        return soldiers != null ? soldiers : new ArrayList<Soldier>(enemies);
    }

    public ArrayList<Enemy> getEnemies() {
        return enemies;
    }

    public ArrayList<Building> getBuildings() {
        return buildings;
    }

    public WaypointNavigator getWaypointNavigator() {
        return waypointNavigator;
    }
}
