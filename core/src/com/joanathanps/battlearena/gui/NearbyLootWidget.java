package com.joanathanps.battlearena.gui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Disposable;
import com.joanathanps.battlearena.audio.AudioManager;
import com.joanathanps.battlearena.entities.Enemy;
import com.joanathanps.battlearena.entities.objects.Empty;
import com.joanathanps.battlearena.entities.objects.EntityObject;
import com.joanathanps.battlearena.entities.objects.LootPickup;
import com.joanathanps.battlearena.entities.objects.weapons.Weapon;
import com.joanathanps.battlearena.entities.tiles.Loot;
import com.joanathanps.battlearena.graphics.FontGenerator;
import com.joanathanps.battlearena.graphics.ResourceHandler;
import com.joanathanps.battlearena.scenes.Match;
import com.joanathanps.battlearena.scheme.MathUtils;

import java.util.ArrayList;

import static com.joanathanps.battlearena.scheme.PlayerSettings.GAME_HEIGHT;
import static com.joanathanps.battlearena.scheme.PlayerSettings.GAME_WIDTH;

/**
 * PUBG-Style Nearby Loot interactive UI panel.
 * Appears on the right side of the screen when standing near crates, chests, or dead bodies.
 * Allows navigating items with Up/Down and pressing ENTER (or clicking) to pick up.
 */
public class NearbyLootWidget implements Disposable {

    private final Match match;
    private final BitmapFont titleFont;
    private final BitmapFont itemFont;
    private final BitmapFont tipFont;
    private final GlyphLayout layout = new GlyphLayout();

    private final Texture pixelTex;
    private int selectedIndex = 0;
    private boolean isVisible = false;
    private boolean wasMouseDown = false;

    // Cache of nearby items and their parent containers
    private final ArrayList<EntityObject> nearbyItems = new ArrayList<>();
    private final ArrayList<ArrayList<EntityObject>> parentContainers = new ArrayList<>();

    private static final float PANEL_W = 280f;
    private static final float ROW_H = 46f;
    private static final float PANEL_X = GAME_WIDTH - PANEL_W - 20f;
    private static final float PROXIMITY_DIST = 1.25f;

    public NearbyLootWidget(Match match) {
        this.match = match;

        titleFont = FontGenerator.generateWithBorder(
                ResourceHandler.FontPath.BOMBARD, 20, 1.2f, Color.BLACK, 0, 0, Color.CLEAR
        );
        itemFont = FontGenerator.generateWithBorder(
                ResourceHandler.FontPath.BOMBARD, 16, 1.0f, Color.BLACK, 0, 0, Color.CLEAR
        );
        tipFont = FontGenerator.generate(ResourceHandler.FontPath.BOMBARD, 12, false);

        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(1, 1, 1, 1);
        pixmap.fill();
        pixelTex = new Texture(pixmap);
        pixmap.dispose();
    }

    public void update(float delta) {
        if (match.getPlayer() == null || match.getPlayer().isDead()) {
            isVisible = false;
            return;
        }

        Vector2 playerPos = match.getPlayer().getBody().getPosition();
        nearbyItems.clear();
        parentContainers.clear();

        // 1. Check crates and chests (close proximity + no walls in between)
        if (match.getWorldBuilder() != null) {
            com.joanathanps.battlearena.events.WaypointNavigator nav = match.getWorldBuilder().getWaypointNavigator();

            for (Loot loot : match.getWorldBuilder().getLoot()) {
                Vector2 lootPos = loot.getBody().getPosition();
                float dist = (float) MathUtils.distance(playerPos, lootPos);
                boolean hasLOS = (nav == null) || nav.hasLineOfSight(playerPos, lootPos);

                if (loot.isPlayerColliding() || (dist <= PROXIMITY_DIST && hasLOS)) {
                    for (EntityObject item : loot.getItems()) {
                        if (item != null && item.getClass() != Empty.class) {
                            nearbyItems.add(item);
                            parentContainers.add(loot.getItems());
                        }
                    }
                }
            }

            // 2. Check dead enemies
            for (Enemy enemy : match.getWorldBuilder().getEnemies()) {
                if (enemy.isDead()) {
                    Vector2 enemyPos = enemy.getBody().getPosition();
                    float dist = (float) MathUtils.distance(playerPos, enemyPos);
                    boolean hasLOS = (nav == null) || nav.hasLineOfSight(playerPos, enemyPos);

                    if (dist <= PROXIMITY_DIST && hasLOS) {
                        for (EntityObject item : enemy.getInventory().getItems()) {
                            if (item != null && item.getClass() != Empty.class) {
                                nearbyItems.add(item);
                                parentContainers.add(enemy.getInventory().getItems());
                            }
                        }
                    }
                }
            }
        }

        isVisible = !nearbyItems.isEmpty();
        if (isVisible) {
            if (selectedIndex >= nearbyItems.size()) {
                selectedIndex = Math.max(0, nearbyItems.size() - 1);
            }
            handleInput();
        } else {
            selectedIndex = 0;
        }
    }

    private void handleInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
            selectedIndex = (selectedIndex - 1 + nearbyItems.size()) % nearbyItems.size();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) {
            selectedIndex = (selectedIndex + 1) % nearbyItems.size();
        }

        // ENTER or F or E to collect selected item
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) ||
                Gdx.input.isKeyJustPressed(Input.Keys.F) ||
                Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            collectSelected();
        }

        // Mouse click on items
        boolean isMouseDown = Gdx.input.isButtonPressed(Input.Buttons.LEFT);
        if (isMouseDown && !wasMouseDown) {
            float mouseX = Gdx.input.getX() * (GAME_WIDTH / (float) Gdx.graphics.getWidth());
            float mouseY = (Gdx.graphics.getHeight() - Gdx.input.getY()) * (GAME_HEIGHT / (float) Gdx.graphics.getHeight());

            float panelH = 50f + nearbyItems.size() * ROW_H + 25f;
            float panelY = GAME_HEIGHT / 2f - panelH / 2f;

            if (mouseX >= PANEL_X && mouseX <= PANEL_X + PANEL_W) {
                float startY = panelY + panelH - 50f - ROW_H;
                for (int i = 0; i < nearbyItems.size(); i++) {
                    float rowY = startY - i * ROW_H;
                    if (mouseY >= rowY && mouseY <= rowY + ROW_H) {
                        selectedIndex = i;
                        collectSelected();
                        break;
                    }
                }
            }
        }
        wasMouseDown = isMouseDown;
    }

    private void collectSelected() {
        if (selectedIndex >= 0 && selectedIndex < nearbyItems.size()) {
            EntityObject item = nearbyItems.get(selectedIndex);
            ArrayList<EntityObject> container = parentContainers.get(selectedIndex);
            LootPickup.collectSingle(match.getPlayer().getInventory(), item, container);
            AudioManager.playLoot();
        }
    }

    public void draw(Batch batch) {
        if (!isVisible || nearbyItems.isEmpty()) return;

        float panelH = 50f + nearbyItems.size() * ROW_H + 25f;
        float panelY = GAME_HEIGHT / 2f - panelH / 2f;

        batch.begin();

        // 1. Dark panel background
        batch.setColor(0.08f, 0.08f, 0.10f, 0.88f);
        batch.draw(pixelTex, PANEL_X, panelY, PANEL_W, panelH);

        // Header background
        batch.setColor(0.15f, 0.15f, 0.18f, 0.95f);
        batch.draw(pixelTex, PANEL_X, panelY + panelH - 36f, PANEL_W, 36f);

        // Gold top accent line
        batch.setColor(1f, 0.84f, 0f, 1f);
        batch.draw(pixelTex, PANEL_X, panelY + panelH - 2f, PANEL_W, 2f);

        // Header Title
        titleFont.setColor(Color.WHITE);
        titleFont.draw(batch, "NEARBY LOOT", PANEL_X + 12f, panelY + panelH - 12f);

        // Tip text
        tipFont.setColor(new Color(0.7f, 0.7f, 0.7f, 1f));
        tipFont.draw(batch, "[Up/Down] Select  [Enter/E] Loot", PANEL_X + 12f, panelY + 16f);

        // 2. Draw Item Rows
        float startY = panelY + panelH - 46f - ROW_H;
        for (int i = 0; i < nearbyItems.size(); i++) {
            EntityObject item = nearbyItems.get(i);
            float rowY = startY - i * ROW_H;
            boolean isSelected = (i == selectedIndex);

            // Row background
            if (isSelected) {
                // PUBG yellow/gold highlighted row
                batch.setColor(1f, 0.8f, 0.1f, 0.85f);
                batch.draw(pixelTex, PANEL_X + 4f, rowY + 2f, PANEL_W - 8f, ROW_H - 4f);

                // Dark border inside selection
                batch.setColor(0.1f, 0.1f, 0.1f, 1f);
                batch.draw(pixelTex, PANEL_X + 5f, rowY + 3f, PANEL_W - 10f, 1f);
                batch.draw(pixelTex, PANEL_X + 5f, rowY + ROW_H - 3f, PANEL_W - 10f, 1f);
            } else {
                batch.setColor(0.18f, 0.18f, 0.22f, 0.7f);
                batch.draw(pixelTex, PANEL_X + 4f, rowY + 2f, PANEL_W - 8f, ROW_H - 4f);
            }

            // Item Icon
            if (item.getIcon() != null) {
                batch.setColor(Color.WHITE);
                batch.draw(item.getIcon(), PANEL_X + 10f, rowY + 6f, 34f, 34f);
            }

            // Item Name & Description
            itemFont.setColor(isSelected ? Color.BLACK : Color.WHITE);
            String name = item.getName();
            if (item instanceof Weapon) {
                Weapon w = (Weapon) item;
                name += " (" + w.getMagCapacity() + " rnds)";
            }
            itemFont.draw(batch, name, PANEL_X + 52f, rowY + 28f);
        }

        batch.setColor(Color.WHITE);
        batch.end();
    }

    @Override
    public void dispose() {
        titleFont.dispose();
        itemFont.dispose();
        tipFont.dispose();
        pixelTex.dispose();
    }
}