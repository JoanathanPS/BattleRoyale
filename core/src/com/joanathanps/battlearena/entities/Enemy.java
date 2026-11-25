package com.joanathanps.battlearena.entities;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.joanathanps.battlearena.entities.objects.LootPickup;
import com.joanathanps.battlearena.entities.objects.NPCInventory;
import com.joanathanps.battlearena.events.AI;
import com.joanathanps.battlearena.graphics.AnimationRegion;
import com.joanathanps.battlearena.scenes.Match;

import static com.joanathanps.battlearena.scheme.PhysicsAdapter.*;

public class Enemy extends Soldier implements Lootable {

    private static final int SPEED = 8;

    private AI ai;
    private final Vector2 force = new Vector2();
    private Vector2 moveTarget;
    private Vector2 aimTarget;
    private boolean playerIsColliding;
    private float pursuitMultiplier = 1f;
    private float ghostTime;

    public Enemy(Match match, Vector2 position, String name) {
        super(match, position, 35, 5, SPEED, AnimationRegion.SOLDIER, ENEMY_TAG,
                (short) (DEFAULT_TAG | BUILDING_TAG | LOOT_TAG | WATER_TAG | PLAYER_TAG | ENEMY_TAG | BULLET_TAG),
                null, name);
        setColor(1.0f, 0.4f, 0.3f, 1.0f);
        getFixture().setUserData(this);
    }

    @Override
    protected void init() {
        super.init();
        setInventory(new NPCInventory(getMatch(), this));
        ai = new AI(getMatch(), this);
    }

    @Override
    public void update(float delta) {
        if (isDead()) {
            if (playerIsColliding && LootPickup.hasItems(getInventory().getItems())) {
                LootPickup.collect(getMatch().getPlayer().getInventory(), getInventory().getItems());
            }
            return;
        }
        super.update(delta);
        getInventory().update(delta);
        ai.update(delta);
        updateRotation();
        updateGhost(delta);
    }

    /** Faces the aim target while fighting, otherwise the direction of travel. */
    private void updateRotation() {
        Vector2 lookAt = aimTarget != null ? aimTarget : moveTarget;
        if (lookAt == null) {
            return;
        }
        Vector2 position = getBody().getPosition();
        float angle = MathUtils.atan2(lookAt.y - position.y, lookAt.x - position.x) * MathUtils.radiansToDegrees;
        setRotation(angle - 90);
    }

    /** Pushes the bot toward a point. */
    public void seek(Vector2 target) {
        moveTarget = target;
        Vector2 position = getBody().getPosition();
        force.set(target.x - position.x, target.y - position.y).nor().scl(getSpeed() * pursuitMultiplier);
        getBody().applyForceToCenter(force, true);
    }

    /** Point to aim at, or null to look where it walks. */
    public void aimAt(Vector2 target) {
        aimTarget = target;
    }

    public void setPursuit(boolean pursuing) {
        pursuitMultiplier = pursuing ? 1.5f : 1f;
    }

    /**
     * Lets a bot that is stuck on scenery pass through it for a moment. Only used off-screen,
     * so the player never sees a bot walk through a wall.
     */
    public void ghost(float seconds) {
        ghostTime = seconds;
        getFixture().setSensor(true);
    }

    private void updateGhost(float delta) {
        if (ghostTime > 0) {
            ghostTime -= delta;
            if (ghostTime <= 0) {
                getFixture().setSensor(false);
            }
        }
    }

    @Override
    public void draw(Batch batch) {
        if (ghostTime <= 0 || isDead()) {
            super.draw(batch);
        }
    }

    @Override
    protected void verifyItems() {
        Object selectedObjectClass = getInventory().getSelectedItem().getClass();
        Object selectedArmorClass = getInventory().getEquipmentItems().get(1).getClass();
        Object selectedHelmetClass = getInventory().getEquipmentItems().get(0).getClass();

        if (selectedObjectClass != getLastSelectedObjectClass() || selectedArmorClass != getLastEquippedArmorClass() ||
                selectedHelmetClass != getLastEquippedHelmetClass()) {
            getAnimator().verify(selectedArmorClass, selectedHelmetClass, selectedObjectClass);
        }
        setLastSelectedObjectClass(selectedObjectClass);
        setLastEquippedArmorClass(selectedArmorClass);
        setLastEquippedHelmetClass(selectedHelmetClass);
    }

    @Override
    protected void die() {
        ghostTime = 0;
        getBody().setLinearVelocity(0, 0);
        getFixture().setSensor(true);
    }

    public AI getAi() {
        return ai;
    }

    public void onPlayerEnter() {
        playerIsColliding = true;
    }

    public void onPlayerLeave() {
        playerIsColliding = false;
    }

    /** Another bot walked over this corpse. */
    public void onEnemyEnter(Enemy enemy) {
        if (isDead()) {
            enemy.getAi().searchForItems(getInventory().getItems());
        }
    }
}
