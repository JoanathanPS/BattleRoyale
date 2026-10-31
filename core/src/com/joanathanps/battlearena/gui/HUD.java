package com.joanathanps.battlearena.gui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack;
import com.badlogic.gdx.scenes.scene2d.utils.SpriteDrawable;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.joanathanps.battlearena.entities.Enemy;
import com.joanathanps.battlearena.entities.objects.Empty;
import com.joanathanps.battlearena.entities.objects.Inventory;
import com.joanathanps.battlearena.events.SafeZoneController;
import com.joanathanps.battlearena.forge.WorldBuilder;
import com.joanathanps.battlearena.graphics.FontGenerator;
import com.joanathanps.battlearena.graphics.ResourceHandler;
import com.joanathanps.battlearena.scenes.Match;

import java.util.ArrayList;

import static com.joanathanps.battlearena.scheme.PhysicsAdapter.pCenter;
import static com.joanathanps.battlearena.scheme.PlayerSettings.GAME_HEIGHT;
import static com.joanathanps.battlearena.scheme.PlayerSettings.GAME_WIDTH;

public class HUD implements Disposable {

    private Stage stage;
    private Viewport viewport;

    private Label event;
    private Label timeToNextEvent;

    private Texture minimap;
    private Texture minimapFrame;
    private Texture minimapPlayerMark;
    private Rectangle minimapMask;
    private int minimapOffset = 10;

    // Safe zone and radar minimap elements
    private Sprite minimapZoneDot;
    private Sprite minimapZoneArrow;
    private Texture minimapZoneCircleTex;
    private Texture minimapArrowTex;
    private Texture minimapGunshotTex;
    private Sprite minimapGunshotDot;

    public static class GunshotMarker {
        public final Vector2 pos;
        public float timeRemaining;

        public GunshotMarker(Vector2 pos) {
            this.pos = new Vector2(pos);
            this.timeRemaining = 1.8f;
        }
    }
    private final ArrayList<GunshotMarker> gunshotMarkers = new ArrayList<>();

    // PUBG-style Nearby Loot Widget
    private NearbyLootWidget nearbyLootWidget;

    private Image eventBackground;

    private Texture emptyBar;
    private Texture healthbar;
    private Texture armorBar;
    private Texture healthIcon;
    private Texture armorIcon;
    private Label healthPoints;
    private Label armorPoints;
    private Match match;

    private Image inventoryBackground;
    private Image selectionMarker;
    private ArrayList<Image> items;
    private int selectedSlot;

    private Texture vignette;

    private Label ammoIndicator;

    private Label aliveIndicator;
    private Label killsIndicator;

    private Label killFeedback;

    private int kills;

    public HUD(Match match) {
        this.match = match;
        init();
        setupStage();
        forgeEventDisplay();
        forgeMinimap();
        forgeHealthDisplay();
        forgeInventory();
        forgeAmmoIndicator();
        forgeEnemyStatus();
        forgeKillfeedback();
    }

    private void init() {
        kills = 0;
    }

    private void setupStage() {
        viewport = new FitViewport(GAME_WIDTH, GAME_HEIGHT, new OrthographicCamera());
        stage = new Stage(viewport, match.getBatch());
        vignette = match.getResources().getTexture(ResourceHandler.TexturePath.VIGNETTE);
    }

    private void forgeEventDisplay() {
        eventBackground = new Image(match.getResources().getTexture(ResourceHandler.TexturePath.EVENT_BACKGROUND));
        eventBackground.setPosition(pCenter(GAME_WIDTH) - pCenter( eventBackground.getWidth()),
                GAME_HEIGHT - eventBackground.getHeight());
        stage.addActor(eventBackground);

        Table table = new Table();
        table.top();
        table.setFillParent(true);

        event = new Label("",
                new Label.LabelStyle(
                        FontGenerator.generate(ResourceHandler.FontPath.BOMBARD, 12, false),
                        Color.WHITE
                )
        );
        table.add(event).fillX().padTop(3).row();

        timeToNextEvent = new Label("",
                new Label.LabelStyle(
                        FontGenerator.generate(ResourceHandler.FontPath.BOMBARD, 22, false),
                        Color.WHITE
                )
        );
        table.add(timeToNextEvent).padTop(1).row();

        stage.addActor(table);
    }

    private void forgeMinimap() {
        minimap = match.getResources().getTexture(ResourceHandler.TexturePath.MATCH_MINIMAP);
        minimapFrame = match.getResources().getTexture(ResourceHandler.TexturePath.MATCH_MINIMAP_FRAME);
        minimapPlayerMark = match.getResources().getTexture(ResourceHandler.TexturePath.PLAYER_MARK);
        minimapMask = new Rectangle();
        Rectangle clipBounds = new Rectangle(minimapOffset, minimapOffset, 200, 200);
        ScissorStack.calculateScissors(stage.getCamera(), stage.getBatch().getTransformMatrix(), clipBounds, minimapMask);

        // 1. Safe zone center dot
        Pixmap dotPixmap = new Pixmap(12, 12, Pixmap.Format.RGBA8888);
        dotPixmap.setColor(0, 0, 0, 0);
        dotPixmap.fill();
        dotPixmap.setColor(0.35f, 0.92f, 1f, 1f);
        dotPixmap.fillCircle(6, 6, 5);
        Texture dotTex = new Texture(dotPixmap);
        dotPixmap.dispose();
        minimapZoneDot = new Sprite(dotTex);
        minimapZoneDot.setSize(8, 8);

        // 2. High-resolution smooth anti-aliased safe zone circle boundary ring
        Pixmap circlePixmap = new Pixmap(512, 512, Pixmap.Format.RGBA8888);
        circlePixmap.setColor(0, 0, 0, 0);
        circlePixmap.fill();

        float center = 256f;
        float ringRadius = 248f;
        float ringThickness = 4.5f;

        for (int y = 0; y < 512; y++) {
            for (int x = 0; x < 512; x++) {
                float dx = x - center;
                float dy = y - center;
                float dist = (float) Math.sqrt(dx * dx + dy * dy);
                float diff = Math.abs(dist - ringRadius);
                if (diff <= ringThickness + 2.5f) {
                    float alpha = Math.max(0f, Math.min(1f, 1f - (diff - (ringThickness - 1f)) / 3.5f));
                    circlePixmap.setColor(0.35f, 0.92f, 1f, alpha * 0.92f);
                    circlePixmap.drawPixel(x, y);
                }
            }
        }
        minimapZoneCircleTex = new Texture(circlePixmap);
        minimapZoneCircleTex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        circlePixmap.dispose();

        // 3. Directional arrow nudge on minimap edge
        Pixmap arrowPixmap = new Pixmap(20, 20, Pixmap.Format.RGBA8888);
        arrowPixmap.setColor(0, 0, 0, 0);
        arrowPixmap.fill();
        arrowPixmap.setColor(0.25f, 0.95f, 1f, 1f);
        arrowPixmap.fillTriangle(10, 0, 2, 18, 18, 18);
        minimapArrowTex = new Texture(arrowPixmap);
        minimapArrowTex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        arrowPixmap.dispose();
        minimapZoneArrow = new Sprite(minimapArrowTex);
        minimapZoneArrow.setSize(12, 12);

        // 4. Solid clean red gunshot marker (only for active shooting)
        Pixmap gunshotPixmap = new Pixmap(16, 16, Pixmap.Format.RGBA8888);
        gunshotPixmap.setColor(0, 0, 0, 0);
        gunshotPixmap.fill();
        gunshotPixmap.setColor(0.95f, 0.15f, 0.15f, 1f);
        gunshotPixmap.fillCircle(8, 8, 6);
        gunshotPixmap.setColor(1f, 0.8f, 0.8f, 0.9f);
        gunshotPixmap.fillCircle(8, 8, 2);
        minimapGunshotTex = new Texture(gunshotPixmap);
        minimapGunshotTex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        gunshotPixmap.dispose();
        minimapGunshotDot = new Sprite(minimapGunshotTex);
        minimapGunshotDot.setSize(8, 8);

        // 5. Nearby loot widget
        nearbyLootWidget = new NearbyLootWidget(match);
    }

    private void forgeHealthDisplay() {
        emptyBar = match.getResources().getTexture(ResourceHandler.TexturePath.EMPTY_BAR);
        healthbar = match.getResources().getTexture(ResourceHandler.TexturePath.HEALTH_BAR);
        armorBar = match.getResources().getTexture(ResourceHandler.TexturePath.ARMOR_BAR);
        healthIcon = match.getResources().getTexture(ResourceHandler.TexturePath.HEALTH_ICON);
        armorIcon = match.getResources().getTexture(ResourceHandler.TexturePath.ARMOR_ICON);

        Table table = new Table();
        table.bottom();
        table.setFillParent(true);

        armorPoints = new Label("",
                new Label.LabelStyle(
                        FontGenerator.generate(ResourceHandler.FontPath.BOMBARD, 14, false), Color.WHITE
                )
        );
        table.add(armorPoints).padBottom(3).padLeft(-350).row();
        healthPoints = new Label("",
                new Label.LabelStyle(
                        FontGenerator.generate(ResourceHandler.FontPath.BOMBARD, 14, false), Color.WHITE
                )
        );
        table.add(healthPoints).padBottom(15).padLeft(-350);

        stage.addActor(table);
    }

    private void forgeInventory() {
        items = new ArrayList<>();
        inventoryBackground = new Image(match.getResources().getTexture(ResourceHandler.TexturePath.INVENTORY));
        inventoryBackground.setPosition(GAME_WIDTH - 366, 10);
        stage.addActor(inventoryBackground);

        selectedSlot = 0;

        selectionMarker = new Image(match.getResources().getTexture(ResourceHandler.TexturePath.SELECTED_SLOT));
        Vector2 markerPos = getSelectionMarkerPosition(selectedSlot);
        selectionMarker.setPosition(markerPos.x, markerPos.y);
        stage.addActor(selectionMarker);

        float itemPos = GAME_WIDTH - 370;
        for (int i = 0; i < 6; i++) {
            items.add(new Image(match.getResources().getTexture(ResourceHandler.TexturePath.INVISIBLE_SLOT)));
            items.get(i).setPosition(itemPos, 15);
            itemPos += 55.5f;
            stage.addActor(items.get(i));
        }
    }

    private void forgeAmmoIndicator() {
        Table table = new Table();
        table.bottom();
        table.setFillParent(true);

        ammoIndicator = new Label("",
                new Label.LabelStyle(
                        FontGenerator.generate(ResourceHandler.FontPath.BOMBARD, 22, false), Color.WHITE
                )
        );
        table.add(ammoIndicator).padBottom(60).row();

        stage.addActor(table);
    }

    private void forgeEnemyStatus() {
        Image aliveIndicatorBackground = new Image(match.getResources().getTexture(ResourceHandler.TexturePath.ALIVE_INDICATOR_BG));
        aliveIndicatorBackground.setPosition(0, GAME_HEIGHT - aliveIndicatorBackground.getHeight());
        stage.addActor(aliveIndicatorBackground);

        Table table = new Table();
        table.top().left();
        table.setFillParent(true);
        aliveIndicator = new Label("",
                new Label.LabelStyle(
                        FontGenerator.generate(ResourceHandler.FontPath.BOMBARD, 26, false), Color.valueOf("afafaf")
                )
        );
        table.add(aliveIndicator).padTop(6).padLeft(6).row();
        stage.addActor(table);

        Image aliveArrow = new Image(match.getResources().getTexture(ResourceHandler.TexturePath.THIN_ARROW));
        aliveArrow.setPosition(30, GAME_HEIGHT - aliveArrow.getHeight() - 10);
        stage.addActor(aliveArrow);

        Image killsIndicatorBackground = new Image(match.getResources().getTexture(ResourceHandler.TexturePath.ALIVE_INDICATOR_BG));
        killsIndicatorBackground.setPosition(GAME_WIDTH, GAME_HEIGHT - killsIndicatorBackground.getHeight());
        killsIndicatorBackground.setSize(-killsIndicatorBackground.getWidth(), killsIndicatorBackground.getHeight());
        stage.addActor(killsIndicatorBackground);

        table = new Table();
        table.top().right();
        table.setFillParent(true);

        killsIndicator = new Label("",
                new Label.LabelStyle(
                        FontGenerator.generate(ResourceHandler.FontPath.BOMBARD, 26, false), Color.valueOf("afafaf")
                )
        );
        table.add(killsIndicator).padTop(6).padRight(6).row();
        stage.addActor(table);

        Image killsArrow = new Image(match.getResources().getTexture(ResourceHandler.TexturePath.THIN_ARROW));
        killsArrow.setPosition(GAME_WIDTH - 65, GAME_HEIGHT - killsArrow.getHeight() + 8);
        killsArrow.setSize(killsArrow.getWidth(), -killsArrow.getHeight());
        stage.addActor(killsArrow);
    }

    private void forgeKillfeedback() {
        Table table = new Table();
        table.center();
        table.setFillParent(true);
        killFeedback = new Label( "",
                new Label.LabelStyle(
                        FontGenerator.generate(ResourceHandler.FontPath.BOMBARD, 26, false), Color.valueOf("ce3535")
                )
        );
        table.add(killFeedback).padTop(250);
        stage.addActor(table);
        killFeedback.setVisible(false);
    }

    private Vector2 getSelectionMarkerPosition(int index) {
        switch (index) {
            case 0: return new Vector2(GAME_WIDTH - 370, 6);
            case 1: return new Vector2(GAME_WIDTH - 314.5f, 6);
            case 2: return new Vector2(GAME_WIDTH - 259, 6);
            case 3: return new Vector2(GAME_WIDTH - 203.5f, 6);
            case 4: return new Vector2(GAME_WIDTH - 148, 6);
            case 5: return new Vector2(GAME_WIDTH - 92.5f, 6);
            default: return new Vector2(GAME_WIDTH - 37, 6);
        }
    }

    public void drawVignette(float delta) {
        match.getBatch().begin();
        match.getBatch().draw(vignette, 0, 0, GAME_WIDTH, GAME_HEIGHT);
        match.getBatch().end();
    }

    public void drawMinimap(float delta, Vector2 playerPosition) {
        match.getBatch().begin();
        match.getBatch().flush();
        ScissorStack.pushScissors(minimapMask);
        match.getBatch().draw(minimap, - playerPosition.x * 7.2f - 82 + minimapOffset,
                - playerPosition.y * 7.2f - 82 + minimapOffset);
        match.getBatch().draw(minimapFrame, minimapOffset, minimapOffset);
        match.getBatch().draw(minimapPlayerMark,
                pCenter(minimapFrame.getWidth()) - pCenter(minimapPlayerMark.getWidth()) + minimapOffset,
                pCenter(minimapFrame.getHeight()) - pCenter(minimapPlayerMark.getHeight()) + minimapOffset);

        // Minimap center in screen coords (player is always here)
        float mapCenterX = pCenter(minimapFrame.getWidth()) + minimapOffset; // 110
        float mapCenterY = pCenter(minimapFrame.getHeight()) + minimapOffset; // 110

        // Minimap frame bounds (screen coords)
        float frameLeft   = minimapOffset + 4;
        float frameRight  = minimapOffset + minimapFrame.getWidth() - 4;
        float frameBottom = minimapOffset + 4;
        float frameTop    = minimapOffset + minimapFrame.getHeight() - 4;

        // --- 1. Draw Safe Zone Boundary Circle & Directional Arrow on Minimap ---
        SafeZoneController szc = match.getSafeZoneController();
        if (szc != null) {
            Vector2[] offsets = szc.getSafezoneOffsets();
            int offsetIndex = szc.getCurrentOffset();
            Vector2 offset = offsets[offsetIndex];

            // Safe zone world-center
            float szWorldX = offset.x + 41f;
            float szWorldY = offset.y + 41f;

            // Relative offset from player to safe zone, scaled to minimap pixels
            float relX = (szWorldX - playerPosition.x) * 7.2f;
            float relY = (szWorldY - playerPosition.y) * 7.2f;

            float dotScreenX = mapCenterX + relX;
            float dotScreenY = mapCenterY + relY;

            // Radius in world units -> minimap pixels
            float szWorldRadius = 30.5f;
            int event = szc.getCurrentEvent();
            if (event >= 3 && event <= 4) szWorldRadius = 21f;
            else if (event >= 5 && event <= 6) szWorldRadius = 13f;
            else if (event >= 7 && event <= 8) szWorldRadius = 6.5f;
            float szPixelRadius = szWorldRadius * 7.2f;

            // Draw circular safe zone boundary ring on minimap
            match.getBatch().setColor(0.3f, 0.9f, 1f, 0.75f);
            match.getBatch().draw(minimapZoneCircleTex,
                    dotScreenX - szPixelRadius, dotScreenY - szPixelRadius,
                    szPixelRadius * 2f, szPixelRadius * 2f);
            match.getBatch().setColor(Color.WHITE);

            // Draw center dot
            minimapZoneDot.setCenter(dotScreenX, dotScreenY);
            minimapZoneDot.draw(match.getBatch());

            // Directional arrow nudge on minimap edge pointing to safe zone
            float dx = relX;
            float dy = relY;
            float len = (float) Math.sqrt(dx * dx + dy * dy);
            if (len > 0) {
                dx /= len;
                dy /= len;
            }
            float edgeMargin = 8f;
            float arrowX = MathUtils.clamp(mapCenterX + dx * 86f, frameLeft + edgeMargin, frameRight - edgeMargin);
            float arrowY = MathUtils.clamp(mapCenterY + dy * 86f, frameBottom + edgeMargin, frameTop - edgeMargin);
            float angleDeg = MathUtils.radiansToDegrees * MathUtils.atan2(dy, dx) - 90f;
            minimapZoneArrow.setCenter(arrowX, arrowY);
            minimapZoneArrow.setOrigin(minimapZoneArrow.getWidth() / 2f, minimapZoneArrow.getHeight() / 2f);
            minimapZoneArrow.setRotation(angleDeg);
            minimapZoneArrow.draw(match.getBatch());
        }

        // --- 2. Draw Solid Red Gunshot Indicators on Minimap (ONLY when gunshots occur) ---
        for (int i = gunshotMarkers.size() - 1; i >= 0; i--) {
            GunshotMarker gm = gunshotMarkers.get(i);
            gm.timeRemaining -= delta;
            if (gm.timeRemaining <= 0) {
                gunshotMarkers.remove(i);
                continue;
            }

            float relEx = (gm.pos.x - playerPosition.x) * 7.2f;
            float relEy = (gm.pos.y - playerPosition.y) * 7.2f;
            float eScreenX = mapCenterX + relEx;
            float eScreenY = mapCenterY + relEy;

            boolean inBounds = eScreenX >= frameLeft && eScreenX <= frameRight
                    && eScreenY >= frameBottom && eScreenY <= frameTop;

            if (inBounds) {
                // Solid clean red (no blinking)
                float alpha = MathUtils.clamp(gm.timeRemaining / 1.8f, 0.2f, 1f);
                minimapGunshotDot.setColor(1f, 0.15f, 0.15f, alpha);
                minimapGunshotDot.setCenter(eScreenX, eScreenY);
                minimapGunshotDot.draw(match.getBatch());
            }
        }

        match.getBatch().flush();
        ScissorStack.popScissors();
        match.getBatch().end();
    }

    public void drawNearbyLoot(float delta) {
        if (nearbyLootWidget != null) {
            nearbyLootWidget.update(delta);
            nearbyLootWidget.draw(match.getBatch());
        }
    }

    public void drawHealthBars(float delta, int health, int armor) {
        match.getBatch().begin();
        match.getBatch().draw(emptyBar, pCenter(GAME_WIDTH) - pCenter(emptyBar.getWidth()), 33);
        match.getBatch().draw(emptyBar, pCenter(GAME_WIDTH) - pCenter(emptyBar.getWidth()), 15);

        match.getBatch().draw(armorBar,
                pCenter(GAME_WIDTH) - pCenter(emptyBar.getWidth()),
                33,
                armor * 4,
                15);
        match.getBatch().draw(healthbar,
                pCenter(GAME_WIDTH) - pCenter(emptyBar.getWidth()),
                15,
                health * 4,
                15);

        match.getBatch().draw(armorIcon,
                pCenter(GAME_WIDTH) - pCenter(emptyBar.getWidth()) + 2, 35);
        match.getBatch().draw(healthIcon,
                pCenter(GAME_WIDTH) - pCenter(emptyBar.getWidth()) + 2, 17);

        match.getBatch().end();
    }

    public void updateHealthBars(float delta, int health, int armor) {
        armorPoints.setText(String.valueOf(armor));
        healthPoints.setText(String.valueOf(health));
    }

    public void drawStaminaBar(float delta, float stamina, float maxStamina) {
        float ratio = MathUtils.clamp(stamina / maxStamina, 0f, 1f);
        match.getBatch().begin();
        match.getBatch().draw(emptyBar, pCenter(GAME_WIDTH) - pCenter(emptyBar.getWidth()), 51);
        Color previousColor = match.getBatch().getColor();
        match.getBatch().setColor(Color.valueOf("ffd700"));
        match.getBatch().draw(armorBar,
                pCenter(GAME_WIDTH) - pCenter(emptyBar.getWidth()),
                51,
                ratio * 400,
                15);
        match.getBatch().setColor(previousColor);
        match.getBatch().end();
    }

    public void updateAmmoIndicator(float delta, Inventory inventory) {
        int bulletsInMagazine = inventory.getBulletsInMagazine();
        if (bulletsInMagazine != -1) {
            ammoIndicator.setText(bulletsInMagazine + " | INF");
        } else {
            ammoIndicator.setText("");
        }
    }

    public void updateEnemyStatus(float delta, WorldBuilder worldBuilder) {
        int alive = worldBuilder.getEnemies().size() + 1;
        for (Enemy enemy : worldBuilder.getEnemies()) {
            if (enemy.isDead()) {
                alive--;
            }
        }
        aliveIndicator.setText(String.valueOf(alive));
        killsIndicator.setText(String.valueOf(match.getPlayer().getKills()));
    }

    public void updateInventory(float delta) {
        if (match.getPlayer() != null) {
            for (int i = 0; i < 6; i++) {
                items.get(i).setDrawable(
                        new SpriteDrawable(new Sprite(
                                match.getPlayer().getInventory().getItems().get(i).getClass() == Empty.class ?
                                        match.getResources().getTexture(ResourceHandler.TexturePath.INVISIBLE_SLOT) :
                                        match.getPlayer().getInventory().getItems().get(i).getIcon()
                        ))
                );
            }
        }
    }

    public void switchSelectedSlot(int slotIndex) {
        Vector2 newPos = getSelectionMarkerPosition(slotIndex);
        selectedSlot = slotIndex;
        selectionMarker.setPosition(newPos.x, newPos.y);
    }

    public void switchSelectedSlot(boolean isGoingForward) {
        if (!isGoingForward) {
            Vector2 newPos = getSelectionMarkerPosition(selectedSlot - 1 < 0 ? 5 : selectedSlot - 1);
            selectedSlot = selectedSlot - 1 < 0 ? 5 : selectedSlot - 1;
            selectionMarker.setPosition(newPos.x, newPos.y);
        } else {
            Vector2 newPos = getSelectionMarkerPosition(selectedSlot + 1 > 5 ? 0 : selectedSlot + 1);
            selectedSlot = selectedSlot + 1 > 5 ? 0 : selectedSlot + 1;
            selectionMarker.setPosition(newPos.x, newPos.y);
        }
    }

    public void updateTimeLabel(String time) {
        timeToNextEvent.setText(time);
    }

    public void updateEventLabel(String eventLabel, boolean isInZoneEvent) {
        event.setText(eventLabel);
        if (isInZoneEvent) {
            timeToNextEvent.setVisible(false);
            event.setStyle(new Label.LabelStyle(
                    FontGenerator.generate(ResourceHandler.FontPath.BOMBARD, 26, false), Color.WHITE
            ));
        } else {
            timeToNextEvent.setVisible(true);
            event.setStyle(new Label.LabelStyle(
                    FontGenerator.generate(ResourceHandler.FontPath.BOMBARD, 12, false), Color.WHITE
            ));
        }
    }

    public void notifyKill(int kills) {
        killFeedback.setVisible(true);
        killFeedback.setText(kills + " " + match.getI18n().getBundle().get(kills > 1 ? "kills" : "kill"));
        this.kills = kills;
        Timer.schedule(new Timer.Task(){
            @Override
            public void run() {
                killFeedback.setText("");
                killFeedback.setVisible(false);
            }
        }, 2);
    }

    public void recordGunshot(Vector2 position) {
        if (position != null) {
            gunshotMarkers.add(new GunshotMarker(position));
        }
    }

    @Override
    public void dispose() {
        stage.dispose();
        if (nearbyLootWidget != null) nearbyLootWidget.dispose();
        if (minimapZoneCircleTex != null) minimapZoneCircleTex.dispose();
        if (minimapArrowTex != null) minimapArrowTex.dispose();
        if (minimapGunshotTex != null) minimapGunshotTex.dispose();
    }

    public Stage getStage() {
        return stage;
    }

    public int getSelectedSlot() {
        return selectedSlot;
    }

    public int getKills() {
        return kills;
    }
}
