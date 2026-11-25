package com.joanathanps.battlearena.entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.joanathanps.battlearena.entities.objects.PlayerInventory;
import com.joanathanps.battlearena.events.InputTracker;
import com.joanathanps.battlearena.graphics.AnimationRegion;
import com.joanathanps.battlearena.scenes.Match;
import com.joanathanps.battlearena.scheme.PlayerSettings;

import static com.joanathanps.battlearena.scheme.PhysicsAdapter.*;

public class Player extends Soldier {

    private static final int WALK_SPEED = 8;
    private static final int RELOAD_SPEED = 7;
    private static final float SPRINT_MULTIPLIER = 1.7f;
    private static final float STAMINA_DRAIN_RATE = 22f;
    private static final float STAMINA_REGEN_RATE = 13f;
    private static final float MAX_STAMINA = 100f;

    private final Vector2 force = new Vector2();
    private boolean actionsBlocked;
    private boolean actionsSemiBlocked;
    private float stamina = MAX_STAMINA;
    private boolean isSprinting;

    public Player(Match match, Vector2 position) {
        super(match, position, 35, 5, WALK_SPEED, AnimationRegion.SOLDIER, PLAYER_TAG,
                (short) (DEFAULT_TAG | BUILDING_TAG | LOOT_TAG | WATER_TAG | ENEMY_TAG | BULLET_TAG), "player",
                PlayerSettings.getPlayerName());
        setColor(0.4f, 0.6f, 1.0f, 1.0f);
    }

    @Override
    protected void init() {
        super.init();
        setInventory(new PlayerInventory(getMatch(), this));
    }

    @Override
    public void update(float delta) {
        super.update(delta);
        if (isDead()) {
            return;
        }
        getInventory().update(delta);
        handleKeyboardInput(delta);
        handleMouseInput();
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
        getMatch().getProgress().cancel();
        getBody().setLinearVelocity(0, 0);
        getBody().getFixtureList().first().setSensor(true);
    }

    @Override
    protected void onDamaged(int amount) {
        getMatch().getHud().flashDamage(amount);
    }

    private void handleKeyboardInput(float delta) {
        if (actionsBlocked) {
            isSprinting = false;
            updateStamina(delta);
            return;
        }
        float directionX = 0;
        float directionY = 0;
        if (InputTracker.isHeld(InputTracker.LEFT)) {
            directionX -= 1;
        }
        if (InputTracker.isHeld(InputTracker.RIGHT)) {
            directionX += 1;
        }
        if (InputTracker.isHeld(InputTracker.UP)) {
            directionY += 1;
        }
        if (InputTracker.isHeld(InputTracker.DOWN)) {
            directionY -= 1;
        }

        boolean isMoving = directionX != 0 || directionY != 0;
        isSprinting = isMoving && !actionsSemiBlocked && InputTracker.isShiftHeld() && stamina > 0;
        updateStamina(delta);

        if (isMoving) {
            float multiplier = isSprinting ? SPRINT_MULTIPLIER : 1f;
            force.set(directionX, directionY).nor().scl(getSpeed() * multiplier);
            getBody().applyForceToCenter(force, true);
        }

        if (InputTracker.wasPressed(InputTracker.R) && isPlaying()) {
            getInventory().reloadSelectedWeapon();
        }
    }

    private void handleMouseInput() {
        Vector2 mouse = InputTracker.getMousePos();
        Vector2 position = getBody().getPosition();
        float angle = MathUtils.atan2(mouse.y - position.y, mouse.x - position.x) * MathUtils.radiansToDegrees;
        setRotation(angle - 90);

        if (Gdx.input.isButtonPressed(Input.Buttons.LEFT) && isPlaying()
                && !getMatch().getHud().isPointerOverLootPanel()) {
            getInventory().useSelectedItem(this);
        }
    }

    private boolean isPlaying() {
        Match.GameState state = getMatch().getState();
        return state == Match.GameState.RUNNING || state == Match.GameState.TACTICAL;
    }

    private void updateStamina(float delta) {
        if (isSprinting) {
            stamina = Math.max(0, stamina - STAMINA_DRAIN_RATE * delta);
        } else {
            stamina = Math.min(MAX_STAMINA, stamina + STAMINA_REGEN_RATE * delta);
        }
    }

    public float getStamina() {
        return stamina;
    }

    public float getMaxStamina() {
        return MAX_STAMINA;
    }

    public boolean isSprinting() {
        return isSprinting;
    }

    /** Healing: no moving, no shooting. */
    public void blockActions() {
        actionsBlocked = true;
    }

    /** Reloading: slower walk, no sprinting. */
    public void slowDown() {
        setSpeed(RELOAD_SPEED);
        actionsSemiBlocked = true;
    }

    public void unblockActions() {
        actionsBlocked = false;
        actionsSemiBlocked = false;
        setSpeed(WALK_SPEED);
    }

    public boolean isActionsBlocked() {
        return actionsBlocked || actionsSemiBlocked;
    }
}
