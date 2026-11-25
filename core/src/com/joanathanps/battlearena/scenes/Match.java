package com.joanathanps.battlearena.scenes;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.joanathanps.battlearena.BattleRoyaleLite;
import com.joanathanps.battlearena.audio.AudioManager;
import com.joanathanps.battlearena.entities.Enemy;
import com.joanathanps.battlearena.entities.Player;
import com.joanathanps.battlearena.entities.Soldier;
import com.joanathanps.battlearena.entities.objects.EntityObject;
import com.joanathanps.battlearena.entities.objects.weapons.ars.AssaultRifle;
import com.joanathanps.battlearena.entities.objects.weapons.pistols.Pistol;
import com.joanathanps.battlearena.entities.objects.weapons.srs.SniperRifle;
import com.joanathanps.battlearena.entities.projectiles.Bullet;
import com.joanathanps.battlearena.entities.tiles.Building;
import com.joanathanps.battlearena.events.InputTracker;
import com.joanathanps.battlearena.events.SafeZoneController;
import com.joanathanps.battlearena.events.WorldContactListener;
import com.joanathanps.battlearena.forge.WorldBuilder;
import com.joanathanps.battlearena.graphics.ResourceHandler;
import com.joanathanps.battlearena.gui.GameOver;
import com.joanathanps.battlearena.gui.HUD;
import com.joanathanps.battlearena.gui.MatchMap;
import com.joanathanps.battlearena.gui.PauseMenu;
import com.joanathanps.battlearena.gui.Progress;
import com.joanathanps.battlearena.languages.Internationalization;
import com.joanathanps.battlearena.scheme.Difficulty;
import com.joanathanps.battlearena.scheme.PhysicsAdapter;

import java.util.ArrayList;
import java.util.Iterator;

import static com.joanathanps.battlearena.scheme.PhysicsAdapter.*;
import static com.joanathanps.battlearena.scheme.PlayerSettings.GAME_HEIGHT;
import static com.joanathanps.battlearena.scheme.PlayerSettings.GAME_WIDTH;

/** One match: the island, the player, the bots, the zone and every overlay on top. */
public class Match implements Screen {

    public enum GameState {
        RUNNING, PAUSED, TACTICAL, GAME_OVER
    }

    private static final float STEP = 1 / 60f;
    private static final int MAX_STEPS = 5;
    /** Time to watch your own death before the results appear. */
    private static final float DEATH_DELAY = 1.2f;

    private final BattleRoyaleLite game;
    private final SpriteBatch batch;
    private final ResourceHandler resources;
    private final Internationalization i18n;
    private final Difficulty difficulty;

    private final OrthographicCamera camera = new OrthographicCamera();
    private final Viewport viewport;
    private final Vector2 cameraTarget = new Vector2();
    private float cameraZoom = 1f;
    private float snappedX;
    private float snappedY;
    private final ShapeRenderer shapes = new ShapeRenderer();
    private final OrthogonalTiledMapRenderer renderer;
    private final OrthogonalTiledMapRenderer overRenderer;
    private final TiledMap map;
    private final TiledMap overMap;
    private final Texture waterBackground;
    private final Texture deadLoot;

    private final World world;
    private final WorldBuilder worldBuilder;
    private final Player player;
    private final SafeZoneController safeZoneController;
    private final ArrayList<Bullet> bullets = new ArrayList<>();
    private float physicsTime;

    private final HUD hud;
    private final Progress progress;
    private final PauseMenu pause;
    private final MatchMap matchMap;
    private final GameOver gameOver;
    private final InputTracker input;

    private GameState state = GameState.RUNNING;
    private float matchTime;
    private float gameOverTimer = -1f;
    private boolean won;
    private int placement;

    public Match(Difficulty difficulty) {
        this.difficulty = difficulty;
        game = BattleRoyaleLite.get();
        batch = game.getBatch();
        resources = game.getResources();
        resources.loadMatchResources();
        i18n = new Internationalization();

        map = resources.getTiledMap(ResourceHandler.TiledMapPath.TILEMAP);
        overMap = resources.getTiledMap(ResourceHandler.TiledMapPath.OVER_TILEMAP);
        renderer = new OrthogonalTiledMapRenderer(map, PhysicsAdapter.getScale(), batch);
        overRenderer = new OrthogonalTiledMapRenderer(overMap, PhysicsAdapter.getScale(), batch);
        waterBackground = resources.getTexture(ResourceHandler.TexturePath.WATER_BACKGROUND);
        deadLoot = resources.getTexture(ResourceHandler.TexturePath.DEAD_LOOT);

        viewport = new FitViewport(pScale(GAME_WIDTH), pScale(GAME_HEIGHT), camera);
        hud = new HUD(this);
        progress = new Progress(this);

        world = new World(GRAVITY, true);
        world.setAutoClearForces(false);
        worldBuilder = new WorldBuilder(this);
        player = new Player(this, worldBuilder.getPlayerSpawnPoint());
        safeZoneController = new SafeZoneController(this);
        for (Enemy enemy : worldBuilder.getEnemies()) {
            enemy.getAi().wakeUp(worldBuilder);
        }
        world.setContactListener(new WorldContactListener(this));

        pause = new PauseMenu(this);
        matchMap = new MatchMap(this);
        gameOver = new GameOver(this);

        input = new InputTracker(viewport);
        Vector2 spawn = player.getBody().getPosition();
        camera.position.set(spawn.x, spawn.y, 0);
        enterState(GameState.RUNNING);
    }

    // ---- state ----

    private void enterState(GameState next) {
        state = next;
        input.resetAllKeys();
        switch (next) {
            case RUNNING:
            case TACTICAL:
                Gdx.input.setInputProcessor(input);
                game.useCursor(next == GameState.RUNNING);
                break;
            case PAUSED:
                Gdx.input.setInputProcessor(pause.getStage());
                game.useCursor(false);
                break;
            case GAME_OVER:
                Gdx.input.setInputProcessor(gameOver.getStage());
                game.useCursor(false);
                break;
        }
    }

    public void resumeMatch() {
        enterState(GameState.RUNNING);
    }

    public void playAgain() {
        game.setScreen(new Match(difficulty));
    }

    public void backToMenu() {
        game.setScreen(new MainMenu());
    }

    private boolean isPlaying() {
        return state == GameState.RUNNING || state == GameState.TACTICAL;
    }

    /** Called by a soldier the moment it dies. */
    public void onElimination(Soldier victim, Soldier killer) {
        hud.onElimination(victim, killer);
        if (killer == player && victim != player) {
            AudioManager.playKill();
        }
        if (gameOverTimer >= 0) {
            return;
        }
        if (victim == player) {
            won = false;
            placement = getAliveCount() + 1;
            gameOverTimer = DEATH_DELAY;
        } else if (!player.isDead() && getAliveCount() == 1) {
            won = true;
            placement = 1;
            gameOverTimer = 0.6f;
        }
    }

    // ---- update ----

    private void update(float delta) {
        if (isPlaying()) {
            handleMatchInput();
            stepWorld(delta);
            player.update(delta);
            for (Enemy enemy : worldBuilder.getEnemies()) {
                enemy.update(delta);
            }
            updateBullets(delta);
            safeZoneController.update(delta);
            hud.update(delta);
            progress.update(delta);
            matchTime += delta;
        } else if (state == GameState.PAUSED) {
            pause.update(delta);
        }
        input.update();
        updateCamera(delta);

        if (gameOverTimer >= 0 && state != GameState.GAME_OVER) {
            gameOverTimer -= delta;
            if (gameOverTimer < 0) {
                gameOver.show(won, placement, player.getKills(), (int) matchTime, player.getKiller());
                enterState(GameState.GAME_OVER);
            }
        }
        input.endFrame();
    }

    /** Fixed 60 Hz physics, so speed does not depend on the frame rate. */
    private void stepWorld(float delta) {
        physicsTime += Math.min(delta, 0.25f);
        int steps = 0;
        while (physicsTime >= STEP && steps < MAX_STEPS) {
            world.step(STEP, 6, 2);
            physicsTime -= STEP;
            steps++;
        }
        if (steps == MAX_STEPS) {
            physicsTime = 0;
        }
        world.clearForces();
    }

    private void handleMatchInput() {
        if (InputTracker.wasPressed(InputTracker.ESC)) {
            if (state == GameState.TACTICAL) {
                enterState(GameState.RUNNING);
            } else {
                enterState(GameState.PAUSED);
            }
            return;
        }
        if (InputTracker.wasPressed(InputTracker.M)) {
            enterState(state == GameState.TACTICAL ? GameState.RUNNING : GameState.TACTICAL);
            return;
        }
        if (player.isDead() || player.isActionsBlocked()) {
            return;
        }
        int[] slots = {InputTracker.NUM_1, InputTracker.NUM_2, InputTracker.NUM_3,
                InputTracker.NUM_4, InputTracker.NUM_5, InputTracker.NUM_6};
        for (int i = 0; i < slots.length; i++) {
            if (InputTracker.wasPressed(slots[i])) {
                hud.switchSelectedSlot(i);
            }
        }
        if (input.getScroll() != 0) {
            hud.switchSelectedSlot(input.getScroll() > 0);
        }
    }

    private void updateBullets(float delta) {
        Iterator<Bullet> iterator = bullets.iterator();
        while (iterator.hasNext()) {
            Bullet bullet = iterator.next();
            bullet.update(delta);
            if (bullet.isSpent()) {
                world.destroyBody(bullet.getBody());
                iterator.remove();
            }
        }
    }

    /** Follows the player, leans toward the cursor and zooms out for longer-range weapons. */
    private void updateCamera(float delta) {
        Vector2 position = player.getBody().getPosition();
        cameraTarget.set(position.x + InputTracker.getRelativeX() / 300f,
                position.y - InputTracker.getRelativeY() / 300f);
        // Same smoothing as lerping 5% per frame at 60 fps, at any frame rate.
        float follow = 1f - (float) Math.pow(0.95f, delta * 60f);
        camera.position.x = MathUtils.lerp(camera.position.x, cameraTarget.x, follow);
        camera.position.y = MathUtils.lerp(camera.position.y, cameraTarget.y, follow);

        EntityObject held = player.getInventory().getSelectedItem();
        float targetZoom = held instanceof SniperRifle ? 2f
                : held instanceof AssaultRifle ? 1.3f
                : held instanceof Pistol ? 1.1f : 1f;
        cameraZoom = MathUtils.lerp(cameraZoom, targetZoom, follow);
        camera.zoom = cameraZoom;
        camera.update();
    }

    /** Snaps the view to whole hundredths of a unit while drawing so unfiltered tiles do not shimmer. */
    private void snapCamera(boolean snap) {
        if (snap) {
            snappedX = camera.position.x;
            snappedY = camera.position.y;
            camera.position.x = Math.round(camera.position.x * 100f) / 100f;
            camera.position.y = Math.round(camera.position.y * 100f) / 100f;
        } else {
            camera.position.x = snappedX;
            camera.position.y = snappedY;
        }
        camera.update();
    }

    // ---- render ----

    @Override
    public void render(float delta) {
        update(delta);

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        snapCamera(true);
        viewport.apply();
        batch.setProjectionMatrix(camera.combined);

        batch.begin();
        batch.draw(waterBackground, camera.position.x - 50, camera.position.y - 50, 100, 100);
        batch.end();

        renderer.setView(camera);
        renderer.render();

        batch.begin();
        for (Soldier soldier : worldBuilder.getSoldiers()) {
            if (soldier.isDead()) {
                Vector2 position = soldier.getBody().getPosition();
                batch.draw(deadLoot, position.x - pScaleCenter(deadLoot.getWidth()),
                        position.y - pScaleCenter(deadLoot.getHeight()),
                        pScale(deadLoot.getWidth()), pScale(deadLoot.getHeight()));
            }
        }
        for (Bullet bullet : bullets) {
            bullet.draw(batch);
        }
        player.draw(batch);
        for (Enemy enemy : worldBuilder.getEnemies()) {
            enemy.draw(batch);
        }
        batch.end();

        overRenderer.setView(camera);
        overRenderer.render();
        safeZoneController.render(shapes, camera.combined);
        snapCamera(false);

        hud.draw(state != GameState.TACTICAL, state == GameState.RUNNING);
        progress.draw();

        if (state == GameState.TACTICAL) {
            matchMap.draw();
        } else if (state == GameState.PAUSED) {
            pause.draw();
        } else if (state == GameState.GAME_OVER) {
            gameOver.draw(delta);
        }
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);
        hud.getStage().getViewport().update(width, height, true);
        progress.resize(width, height);
        pause.getStage().getViewport().update(width, height, true);
        gameOver.getStage().getViewport().update(width, height, true);
        matchMap.resize(width, height);
    }

    @Override
    public void pause() {
        if (isPlaying()) {
            enterState(GameState.PAUSED);
        }
    }

    @Override
    public void show() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
    }

    @Override
    public void dispose() {
        for (Building building : worldBuilder.getBuildings()) {
            building.restoreCeiling();
        }
        world.dispose();
        renderer.dispose();
        overRenderer.dispose();
        shapes.dispose();
        hud.dispose();
        progress.dispose();
        pause.dispose();
        matchMap.dispose();
        gameOver.dispose();
    }

    // ---- accessors ----

    public int getAliveCount() {
        int alive = 0;
        for (Soldier soldier : worldBuilder.getSoldiers()) {
            if (!soldier.isDead()) {
                alive++;
            }
        }
        return alive;
    }

    public void addBullet(Bullet bullet) {
        bullets.add(bullet);
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public ResourceHandler getResources() {
        return resources;
    }

    public World getWorld() {
        return world;
    }

    public TiledMap getMap() {
        return map;
    }

    public TiledMap getOverMap() {
        return overMap;
    }

    public SpriteBatch getBatch() {
        return batch;
    }

    public Internationalization getI18n() {
        return i18n;
    }

    public HUD getHud() {
        return hud;
    }

    public Progress getProgress() {
        return progress;
    }

    public Player getPlayer() {
        return player;
    }

    public SafeZoneController getSafeZoneController() {
        return safeZoneController;
    }

    public WorldBuilder getWorldBuilder() {
        return worldBuilder;
    }

    public OrthographicCamera getCamera() {
        return camera;
    }

    public GameState getState() {
        return state;
    }
}
