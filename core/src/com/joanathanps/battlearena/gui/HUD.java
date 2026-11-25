package com.joanathanps.battlearena.gui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.joanathanps.battlearena.entities.Player;
import com.joanathanps.battlearena.entities.Soldier;
import com.joanathanps.battlearena.entities.objects.Empty;
import com.joanathanps.battlearena.entities.objects.EntityObject;
import com.joanathanps.battlearena.entities.objects.Inventory;
import com.joanathanps.battlearena.entities.objects.weapons.Weapon;
import com.joanathanps.battlearena.events.SafeZoneController;
import com.joanathanps.battlearena.graphics.FontGenerator;
import com.joanathanps.battlearena.graphics.ResourceHandler;
import com.joanathanps.battlearena.scenes.Match;

import java.util.ArrayList;

import static com.joanathanps.battlearena.scheme.PhysicsAdapter.formatSeconds;
import static com.joanathanps.battlearena.scheme.PlayerSettings.GAME_HEIGHT;
import static com.joanathanps.battlearena.scheme.PlayerSettings.GAME_WIDTH;

/**
 * Everything drawn over the match: zone banner, alive and kill counters, kill feed,
 * minimap, health/armor/stamina bars, ammo, inventory slots and the nearby-loot panel.
 * Laid out on a fixed 1280 x 720 canvas that scales with the window.
 */
public class HUD implements Disposable {

    /** Minimap pixels per world unit, matching the minimap texture. */
    private static final float MINIMAP_SCALE = 7.2f;
    /** Ocean margin, in minimap pixels, around the island in the minimap texture. */
    private static final float MINIMAP_MARGIN = 182f;
    private static final float MINIMAP_X = 10f;
    private static final float MINIMAP_Y = 10f;
    private static final float MINIMAP_SIZE = 200f;

    private static final float SLOT_X = GAME_WIDTH - 370;
    private static final float SLOT_STEP = 55.5f;
    private static final float BAR_WIDTH = 400f;

    private static final Color ZONE_RED = new Color(0.86f, 0.22f, 0.18f, 1f);
    private static final Color MUTED = new Color(0.69f, 0.69f, 0.69f, 1f);
    private static final Color STAMINA = new Color(1f, 0.84f, 0f, 1f);

    private final Match match;
    private final Stage stage;
    private final ShapeRenderer shapes = new ShapeRenderer();
    private final GlyphLayout layout = new GlyphLayout();
    private final Rectangle minimapClip = new Rectangle(MINIMAP_X, MINIMAP_Y, MINIMAP_SIZE, MINIMAP_SIZE);
    private final Rectangle scissor = new Rectangle();
    private final Texture pixel;

    private final BitmapFont small = FontGenerator.generate(ResourceHandler.FontPath.BOMBARD, 12, true);
    private final BitmapFont body = FontGenerator.generate(ResourceHandler.FontPath.BOMBARD, 14, true);
    private final BitmapFont medium = FontGenerator.generate(ResourceHandler.FontPath.BOMBARD, 18, true);
    private final BitmapFont large = FontGenerator.generate(ResourceHandler.FontPath.BOMBARD, 22, true);
    private final BitmapFont counter = FontGenerator.generate(ResourceHandler.FontPath.BOMBARD, 26, false);
    private final BitmapFont callout = FontGenerator.generate(ResourceHandler.FontPath.BOMBARD, 28, true);

    private final Texture minimap;
    private final Texture minimapFrame;
    private final Texture playerMark;
    private final Texture emptyBar;
    private final Texture healthBar;
    private final Texture armorBar;
    private final Texture healthIcon;
    private final Texture armorIcon;
    private final Texture vignette;
    private final Texture eventBackground;
    private final Texture counterBackground;

    private final Image selectionMarker;
    private final Image[] slotImages = new Image[Inventory.SLOTS];
    private final EntityObject[] shownItems = new EntityObject[Inventory.SLOTS];
    private int selectedSlot;

    private final NearbyLootWidget nearbyLootWidget;

    private final ArrayList<FeedEntry> feed = new ArrayList<>();
    private final ArrayList<Gunshot> gunshots = new ArrayList<>();
    private String calloutText = "";
    private float calloutTime;
    private float damageFlash;

    private static final class FeedEntry {
        final String text;
        final boolean involvesPlayer;
        float time = 5f;

        FeedEntry(String text, boolean involvesPlayer) {
            this.text = text;
            this.involvesPlayer = involvesPlayer;
        }
    }

    private static final class Gunshot {
        final Vector2 position;
        float time = 1.8f;

        Gunshot(Vector2 position) {
            this.position = new Vector2(position);
        }
    }

    public HUD(Match match) {
        this.match = match;
        ResourceHandler resources = match.getResources();
        stage = new Stage(new FitViewport(GAME_WIDTH, GAME_HEIGHT, new OrthographicCamera()), match.getBatch());

        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        pixel = new Texture(pixmap);
        pixmap.dispose();

        minimap = resources.getTexture(ResourceHandler.TexturePath.MATCH_MINIMAP);
        minimapFrame = resources.getTexture(ResourceHandler.TexturePath.MATCH_MINIMAP_FRAME);
        playerMark = resources.getTexture(ResourceHandler.TexturePath.PLAYER_MARK);
        emptyBar = resources.getTexture(ResourceHandler.TexturePath.EMPTY_BAR);
        healthBar = resources.getTexture(ResourceHandler.TexturePath.HEALTH_BAR);
        armorBar = resources.getTexture(ResourceHandler.TexturePath.ARMOR_BAR);
        healthIcon = resources.getTexture(ResourceHandler.TexturePath.HEALTH_ICON);
        armorIcon = resources.getTexture(ResourceHandler.TexturePath.ARMOR_ICON);
        vignette = resources.getTexture(ResourceHandler.TexturePath.VIGNETTE);
        eventBackground = resources.getTexture(ResourceHandler.TexturePath.EVENT_BACKGROUND);
        counterBackground = resources.getTexture(ResourceHandler.TexturePath.ALIVE_INDICATOR_BG);

        Image inventoryBackground = new Image(resources.getTexture(ResourceHandler.TexturePath.INVENTORY));
        inventoryBackground.setPosition(GAME_WIDTH - 366, 10);
        stage.addActor(inventoryBackground);

        selectionMarker = new Image(resources.getTexture(ResourceHandler.TexturePath.SELECTED_SLOT));
        stage.addActor(selectionMarker);
        for (int i = 0; i < Inventory.SLOTS; i++) {
            slotImages[i] = new Image(resources.getTexture(ResourceHandler.TexturePath.INVISIBLE_SLOT));
            slotImages[i].setPosition(SLOT_X + i * SLOT_STEP, 15);
            stage.addActor(slotImages[i]);
        }
        switchSelectedSlot(0);

        nearbyLootWidget = new NearbyLootWidget(match);
    }

    public void update(float delta) {
        refreshSlots();
        for (int i = feed.size() - 1; i >= 0; i--) {
            feed.get(i).time -= delta;
            if (feed.get(i).time <= 0) {
                feed.remove(i);
            }
        }
        for (int i = gunshots.size() - 1; i >= 0; i--) {
            gunshots.get(i).time -= delta;
            if (gunshots.get(i).time <= 0) {
                gunshots.remove(i);
            }
        }
        if (calloutTime > 0) {
            calloutTime -= delta;
        }
        if (damageFlash > 0) {
            damageFlash = Math.max(0, damageFlash - delta * 1.8f);
        }
        if (match.getState() == Match.GameState.RUNNING) {
            nearbyLootWidget.update(delta);
        }
    }

    public void draw(boolean showMinimap, boolean showLoot) {
        Batch batch = match.getBatch();
        stage.getViewport().apply();
        batch.setProjectionMatrix(stage.getCamera().combined);

        batch.begin();
        batch.setColor(Color.WHITE);
        batch.draw(vignette, 0, 0, GAME_WIDTH, GAME_HEIGHT);
        if (damageFlash > 0) {
            batch.setColor(0.85f, 0.05f, 0.05f, Math.min(0.55f, damageFlash));
            batch.draw(vignette, 0, 0, GAME_WIDTH, GAME_HEIGHT);
            batch.setColor(Color.WHITE);
        }
        drawZoneBanner(batch);
        drawCounters(batch);
        drawFeed(batch);
        drawBars(batch);
        drawAmmo(batch);
        drawCallout(batch);
        batch.end();

        stage.draw();

        if (showMinimap) {
            drawMinimap(batch);
        }
        if (showLoot) {
            nearbyLootWidget.draw(batch);
        }
    }

    private void drawZoneBanner(Batch batch) {
        SafeZoneController zone = match.getSafeZoneController();
        float x = (GAME_WIDTH - eventBackground.getWidth()) / 2f;
        float y = GAME_HEIGHT - eventBackground.getHeight();
        batch.draw(eventBackground, x, y);

        String title;
        String time = formatSeconds(zone.getSecondsLeft(), false);
        switch (zone.getStage()) {
            case WAITING:
                title = text("zoneShrinksIn");
                break;
            case SHRINKING:
                title = text("zoneShrinking");
                break;
            default:
                title = text("zoneClosed");
                time = "";
                break;
        }
        small.setColor(Color.WHITE);
        drawCentered(batch, small, title + "  " + zone.getPhaseNumber() + "/" + SafeZoneController.PHASES,
                GAME_WIDTH / 2f, GAME_HEIGHT - 6);
        medium.setColor(Color.WHITE);
        drawCentered(batch, medium, time, GAME_WIDTH / 2f, GAME_HEIGHT - 20);

        Player player = match.getPlayer();
        if (!player.isDead() && zone.isOutside(player.getBody().getPosition())) {
            body.setColor(ZONE_RED);
            drawCentered(batch, body, text("outsideZone") + "  -" + zone.getDamagePerSecond() + " HP/s",
                    GAME_WIDTH / 2f, GAME_HEIGHT - eventBackground.getHeight() - 8);
        }
    }

    private void drawCounters(Batch batch) {
        float top = GAME_HEIGHT - counterBackground.getHeight();
        batch.draw(counterBackground, 0, top);
        batch.draw(counterBackground, GAME_WIDTH, top, -counterBackground.getWidth(), counterBackground.getHeight());

        counter.setColor(MUTED);
        counter.draw(batch, String.valueOf(match.getAliveCount()), 8, GAME_HEIGHT - 8);
        layout.setText(counter, String.valueOf(match.getPlayer().getKills()));
        counter.draw(batch, layout, GAME_WIDTH - 8 - layout.width, GAME_HEIGHT - 8);

        small.setColor(MUTED);
        small.draw(batch, text("alive"), 50, GAME_HEIGHT - 16);
        layout.setText(small, text("kills"));
        small.draw(batch, layout, GAME_WIDTH - 50 - layout.width, GAME_HEIGHT - 16);
    }

    private void drawFeed(Batch batch) {
        float y = GAME_HEIGHT - 56;
        for (int i = feed.size() - 1; i >= 0; i--) {
            FeedEntry entry = feed.get(i);
            float alpha = Math.min(1f, entry.time);
            layout.setText(body, entry.text);
            float x = GAME_WIDTH - 12 - layout.width;
            batch.setColor(0f, 0f, 0f, 0.45f * alpha);
            batch.draw(pixel, x - 6, y - layout.height - 5, layout.width + 12, layout.height + 10);
            batch.setColor(Color.WHITE);
            body.setColor(entry.involvesPlayer ? STAMINA.r : 1f, entry.involvesPlayer ? STAMINA.g : 1f,
                    entry.involvesPlayer ? STAMINA.b : 1f, alpha);
            body.draw(batch, entry.text, x, y);
            y -= layout.height + 12;
        }
    }

    private void drawBars(Batch batch) {
        Player player = match.getPlayer();
        float x = (GAME_WIDTH - BAR_WIDTH) / 2f;
        float stamina = MathUtils.clamp(player.getStamina() / player.getMaxStamina(), 0f, 1f);

        batch.draw(emptyBar, x, 51);
        batch.setColor(STAMINA);
        batch.draw(armorBar, x, 51, stamina * BAR_WIDTH, 15);
        batch.setColor(Color.WHITE);

        batch.draw(emptyBar, x, 33);
        batch.draw(emptyBar, x, 15);
        if (player.getArmor() > 0) {
            batch.draw(armorBar, x, 33, Math.min(player.getArmor() * 4, BAR_WIDTH), 15);
        }
        if (player.getHealth() > 0) {
            batch.draw(healthBar, x, 15, Math.min(player.getHealth() * 4, BAR_WIDTH), 15);
        }
        batch.draw(armorIcon, x + 3, 35);
        batch.draw(healthIcon, x + 3, 17);
        body.setColor(Color.WHITE);
        body.draw(batch, String.valueOf(player.getArmor()), x + 18, 45);
        body.draw(batch, String.valueOf(player.getHealth()), x + 18, 27);
    }

    private void drawAmmo(Batch batch) {
        EntityObject held = match.getPlayer().getInventory().getSelectedItem();
        if (!(held instanceof Weapon)) {
            return;
        }
        Weapon weapon = (Weapon) held;
        String ammo = weapon.getBullets() + " / " + weapon.getMagCapacity();
        large.setColor(weapon.getBullets() == 0 ? ZONE_RED : Color.WHITE);
        drawCentered(batch, large, ammo, GAME_WIDTH / 2f, 96);
        if (weapon.getBullets() == 0 && !match.getPlayer().getInventory().isSelectedBeingUsed()) {
            small.setColor(Color.WHITE);
            drawCentered(batch, small, text("reloadHint"), GAME_WIDTH / 2f, 112);
        }
    }

    private void drawCallout(Batch batch) {
        if (calloutTime <= 0) {
            return;
        }
        callout.setColor(ZONE_RED.r, ZONE_RED.g, ZONE_RED.b, Math.min(1f, calloutTime));
        drawCentered(batch, callout, calloutText, GAME_WIDTH / 2f, GAME_HEIGHT / 2f - 110);
    }

    private void drawMinimap(Batch batch) {
        Vector2 player = match.getPlayer().getBody().getPosition();
        float centerX = MINIMAP_X + MINIMAP_SIZE / 2f;
        float centerY = MINIMAP_Y + MINIMAP_SIZE / 2f;

        ScissorStack.calculateScissors(stage.getCamera(), batch.getTransformMatrix(), minimapClip, scissor);
        batch.begin();
        batch.flush();
        if (ScissorStack.pushScissors(scissor)) {
            batch.draw(minimap, centerX - MINIMAP_MARGIN - player.x * MINIMAP_SCALE,
                    centerY - MINIMAP_MARGIN - player.y * MINIMAP_SCALE);
            batch.end();

            SafeZoneController zone = match.getSafeZoneController();
            shapes.setProjectionMatrix(stage.getCamera().combined);
            Gdx.gl.glEnable(com.badlogic.gdx.graphics.GL20.GL_BLEND);
            shapes.begin(ShapeRenderer.ShapeType.Filled);
            float zx = centerX + (zone.getCenter().x - player.x) * MINIMAP_SCALE;
            float zy = centerY + (zone.getCenter().y - player.y) * MINIMAP_SCALE;
            float zr = zone.getRadius() * MINIMAP_SCALE;
            SafeZoneController.drawRing(shapes, zx, zy, zr, zr + 400, 0.78f, 0.17f, 0.13f, 0.35f);
            SafeZoneController.drawRing(shapes, zx, zy, zr - 1f, zr + 1f, 0.86f, 0.22f, 0.18f, 1f);
            if (zone.hasNextCircle()) {
                float nx = centerX + (zone.getNextCenter().x - player.x) * MINIMAP_SCALE;
                float ny = centerY + (zone.getNextCenter().y - player.y) * MINIMAP_SCALE;
                float nr = zone.getNextRadius() * MINIMAP_SCALE;
                SafeZoneController.drawRing(shapes, nx, ny, nr - 0.8f, nr + 0.8f, 1f, 1f, 1f, 0.9f);
            }
            for (Gunshot shot : gunshots) {
                shapes.setColor(1f, 0.15f, 0.15f, MathUtils.clamp(shot.time / 1.8f, 0.2f, 1f));
                shapes.circle(centerX + (shot.position.x - player.x) * MINIMAP_SCALE,
                        centerY + (shot.position.y - player.y) * MINIMAP_SCALE, 3.5f, 10);
            }
            shapes.end();

            batch.begin();
            batch.draw(minimapFrame, MINIMAP_X, MINIMAP_Y);
            batch.draw(playerMark, centerX - playerMark.getWidth() / 2f, centerY - playerMark.getHeight() / 2f);
            batch.flush();
            ScissorStack.popScissors();
        }
        batch.end();
    }

    public void onElimination(Soldier victim, Soldier killer) {
        Player player = match.getPlayer();
        String killerName = killer == null ? text("zone") : killer.getName();
        feed.add(new FeedEntry(killerName + "  >  " + victim.getName(), victim == player || killer == player));
        if (feed.size() > 5) {
            feed.remove(0);
        }
        if (killer == player && victim != player) {
            calloutText = text("eliminated") + "  " + victim.getName();
            calloutTime = 2.2f;
        }
    }

    public void flashDamage(int amount) {
        damageFlash = Math.min(0.9f, damageFlash + 0.25f + amount / 100f);
    }

    public void recordGunshot(Vector2 position) {
        if (position != null && match.getPlayer() != null
                && position.dst2(match.getPlayer().getBody().getPosition()) > 0.01f) {
            gunshots.add(new Gunshot(position));
        }
    }

    private void refreshSlots() {
        ArrayList<EntityObject> items = match.getPlayer().getInventory().getItems();
        for (int i = 0; i < Inventory.SLOTS; i++) {
            EntityObject item = items.get(i);
            if (item != shownItems[i]) {
                shownItems[i] = item;
                Texture icon = item instanceof Empty
                        ? match.getResources().getTexture(ResourceHandler.TexturePath.INVISIBLE_SLOT)
                        : item.getIcon();
                slotImages[i].setDrawable(new TextureRegionDrawable(new com.badlogic.gdx.graphics.g2d.TextureRegion(icon)));
            }
        }
    }

    public void switchSelectedSlot(int slotIndex) {
        selectedSlot = (slotIndex % Inventory.SLOTS + Inventory.SLOTS) % Inventory.SLOTS;
        selectionMarker.setPosition(SLOT_X + selectedSlot * SLOT_STEP, 6);
    }

    public void switchSelectedSlot(boolean forward) {
        switchSelectedSlot(selectedSlot + (forward ? 1 : -1));
    }

    public int getSelectedSlot() {
        return selectedSlot;
    }

    public boolean isPointerOverLootPanel() {
        return nearbyLootWidget.isPointerOver();
    }

    private void drawCentered(Batch batch, BitmapFont font, String value, float centerX, float top) {
        layout.setText(font, value);
        font.draw(batch, layout, centerX - layout.width / 2f, top);
    }

    private String text(String key) {
        return match.getI18n().getBundle().get(key);
    }

    public Stage getStage() {
        return stage;
    }

    @Override
    public void dispose() {
        stage.dispose();
        shapes.dispose();
        pixel.dispose();
        nearbyLootWidget.dispose();
    }
}
