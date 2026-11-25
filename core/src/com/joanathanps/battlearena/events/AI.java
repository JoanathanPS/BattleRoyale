package com.joanathanps.battlearena.events;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.RayCastCallback;
import com.joanathanps.battlearena.entities.Enemy;
import com.joanathanps.battlearena.entities.Lootable;
import com.joanathanps.battlearena.entities.Soldier;
import com.joanathanps.battlearena.entities.objects.Empty;
import com.joanathanps.battlearena.entities.objects.EntityObject;
import com.joanathanps.battlearena.entities.objects.Inventory;
import com.joanathanps.battlearena.entities.objects.LootPickup;
import com.joanathanps.battlearena.entities.objects.Medkit;
import com.joanathanps.battlearena.entities.objects.equipment.armor.Armor;
import com.joanathanps.battlearena.entities.objects.equipment.helmets.Helmet;
import com.joanathanps.battlearena.entities.objects.weapons.Weapon;
import com.joanathanps.battlearena.entities.objects.weapons.srs.SniperRifle;
import com.joanathanps.battlearena.entities.tiles.Loot;
import com.joanathanps.battlearena.forge.WorldBuilder;
import com.joanathanps.battlearena.scenes.Match;
import com.joanathanps.battlearena.scheme.Difficulty;

import java.util.ArrayList;
import java.util.Collections;

import static com.joanathanps.battlearena.scheme.PhysicsAdapter.BUILDING_TAG;

/**
 * Bot brain. Movement and shooting run every frame; decisions (what to do next, who to fight)
 * run every {@code reactionSeconds}, which is what makes Hard bots react faster than Easy ones.
 *
 * <pre>
 * ROUTING -> SEEKING_LOOT -> LOOTING -> ROUTING
 *    |            |  stuck
 *    |            +-------> UNSTICKING -> ROUTING
 *    +-- any decision tick: enemy in sight -> FIGHTING;  zone closing -> TO_ZONE
 * </pre>
 */
public class AI {

    private enum State { ROUTING, SEEKING_LOOT, LOOTING, UNSTICKING, TO_ZONE, FIGHTING }

    private static final float MELEE_RANGE = 1.5f;
    private static final float LOOT_REACH = 1f;
    private static final float LOOT_SECONDS = 1.5f;
    private static final float UNSTICK_SECONDS = 3f;
    private static final float STUCK_SPEED = 0.05f;
    private static final float STUCK_SECONDS = 0.6f;
    private static final float LOSE_TARGET_SECONDS = 2f;
    private static final int MEMORY = 6;

    private final Match match;
    private final Enemy bot;
    private final float reactionSeconds;
    private final float aggression;

    private WorldBuilder world;
    private WaypointNavigator navigator;

    private State state = State.ROUTING;
    private float stateTime;
    private float decisionTimer;
    private float stuckTime;
    private float unseenTime;

    private final Vector2 goal = new Vector2();
    private final ArrayList<Vector2> visited = new ArrayList<>();
    private Soldier target;
    private int selectedInventorySlot;

    public AI(Match match, Enemy bot) {
        this.match = match;
        this.bot = bot;
        this.reactionSeconds = match.getDifficulty().getReactionSeconds();
        this.aggression = match.getDifficulty().getAggressionMultiplier();
        // Spread decisions over frames so twenty bots do not all think at once.
        this.decisionTimer = MathUtils.random(reactionSeconds);
    }

    public void wakeUp(WorldBuilder world) {
        this.world = world;
        this.navigator = world.getWaypointNavigator();
        setState(State.ROUTING);
    }

    public void update(float delta) {
        if (world == null) {
            return;
        }
        stateTime += delta;
        decisionTimer -= delta;
        if (decisionTimer <= 0) {
            decisionTimer = reactionSeconds;
            decide();
        }

        switch (state) {
            case ROUTING:
                pickLootGoal();
                break;
            case SEEKING_LOOT:
                moveTo(goal, delta);
                if (bot.getBody().getPosition().dst(goal) < LOOT_REACH) {
                    remember(goal);
                    setState(State.LOOTING);
                }
                break;
            case LOOTING:
                bot.aimAt(null);
                if (stateTime >= LOOT_SECONDS) {
                    setState(State.ROUTING);
                }
                break;
            case UNSTICKING:
                bot.seek(goal);
                if (stateTime >= UNSTICK_SECONDS || bot.getBody().getPosition().dst(goal) < 1f) {
                    setState(State.ROUTING);
                }
                break;
            case TO_ZONE:
                goal.set(zone().hasNextCircle() ? zone().getNextCenter() : zone().getCenter());
                moveTo(goal, delta);
                if (!zone().isOutside(bot.getBody().getPosition()) && !zone().isOutsideNext(bot.getBody().getPosition())) {
                    setState(State.ROUTING);
                }
                break;
            case FIGHTING:
                fight(delta);
                break;
        }
    }

    /** Runs every reaction tick. */
    private void decide() {
        selectBestWeapon();
        Vector2 position = bot.getBody().getPosition();

        // Standing in the zone always wins: leave first, fight later.
        if (zone().isOutside(position)) {
            setState(State.TO_ZONE);
            return;
        }
        if (state != State.FIGHTING) {
            Soldier seen = findTarget();
            if (seen != null) {
                target = seen;
                unseenTime = 0;
                setState(State.FIGHTING);
                return;
            }
        }
        // Head in early when the next circle excludes us and the shrink is near.
        if (state != State.FIGHTING && state != State.TO_ZONE && zone().isOutsideNext(position)
                && zone().getSecondsUntilShrink() < 15f) {
            setState(State.TO_ZONE);
        }
    }

    private void pickLootGoal() {
        Vector2 position = bot.getBody().getPosition();
        Lootable nearest = null;
        float nearestDistance = Float.MAX_VALUE;
        for (Lootable loot : world.getAllLoot()) {
            Vector2 lootPosition = loot.getBody().getPosition();
            if (isRemembered(lootPosition) || !hasItems(loot) || zone().isOutsideNext(lootPosition)) {
                continue;
            }
            float distance = position.dst(lootPosition);
            if (distance < nearestDistance) {
                nearest = loot;
                nearestDistance = distance;
            }
        }
        if (nearest != null) {
            goal.set(nearest.getBody().getPosition());
            setState(State.SEEKING_LOOT);
        } else {
            // Nothing left worth looting: wander toward the safe zone.
            goal.set(zone().hasNextCircle() ? zone().getNextCenter() : zone().getCenter());
            goal.add(MathUtils.random(-4f, 4f), MathUtils.random(-4f, 4f));
            setState(State.UNSTICKING);
        }
    }

    private static boolean hasItems(Lootable loot) {
        if (loot instanceof Loot) {
            return ((Loot) loot).hasItems();
        }
        if (loot instanceof Enemy) {
            return LootPickup.hasItems(((Enemy) loot).getInventory().getItems());
        }
        return false;
    }

    private void fight(float delta) {
        if (target == null || target.isDead()) {
            target = null;
            bot.aimAt(null);
            setState(State.ROUTING);
            return;
        }
        Vector2 position = bot.getBody().getPosition();
        Vector2 enemy = target.getBody().getPosition();
        float distance = position.dst(enemy);
        boolean visible = hasLineOfSight(position, enemy);

        unseenTime = visible ? 0 : unseenTime + delta;
        if (unseenTime > LOSE_TARGET_SECONDS || distance > acquisitionRange() * 1.6f) {
            target = null;
            bot.aimAt(null);
            setState(State.ROUTING);
            return;
        }

        bot.aimAt(enemy);
        Inventory inventory = bot.getInventory();
        if (inventory.getSelectedItem() instanceof Weapon) {
            float preferred = inventory.getSelectedItem() instanceof SniperRifle ? 9f : 4f;
            bot.setPursuit(distance > preferred + 3f && match.getDifficulty() != Difficulty.EASY);
            if (distance > preferred || !visible) {
                moveTo(enemy, delta);
            }
            if (visible) {
                inventory.useSelectedItem(bot);
            }
        } else {
            bot.setPursuit(false);
            if (distance < 3f) {
                bot.seek(enemy);
                if (distance <= MELEE_RANGE) {
                    bot.punch();
                }
            } else {
                target = null;
                bot.aimAt(null);
                setState(State.ROUTING);
            }
        }
    }

    /** Walks toward a point, routing around buildings, and switches to UNSTICKING when blocked. */
    private void moveTo(Vector2 destination, float delta) {
        Vector2 position = bot.getBody().getPosition();
        Vector2 step = navigator != null ? navigator.getNextWaypoint(position, destination) : destination;
        bot.seek(step);

        if (bot.getBody().getLinearVelocity().len2() < STUCK_SPEED * STUCK_SPEED) {
            stuckTime += delta;
        } else {
            stuckTime = 0;
        }
        if (stuckTime > STUCK_SECONDS && state != State.FIGHTING) {
            stuckTime = 0;
            boolean onScreen = match.getCamera().frustum.pointInFrustum(position.x, position.y, 0);
            if (!onScreen) {
                bot.ghost(1f);
            } else {
                goal.set(pickClearDirection());
                setState(State.UNSTICKING);
            }
        }
    }

    /** Casts eight rays and returns a point along the longest clear one. */
    private Vector2 pickClearDirection() {
        Vector2 position = bot.getBody().getPosition();
        float probe = 6f;
        float bestLength = -1f;
        float bestAngle = 0f;
        Vector2 end = new Vector2();
        for (int i = 0; i < 8; i++) {
            float angle = i * MathUtils.PI / 4;
            end.set(position.x + MathUtils.cos(angle) * probe, position.y + MathUtils.sin(angle) * probe);
            float clear = clearDistance(position, end, probe);
            if (clear > bestLength) {
                bestLength = clear;
                bestAngle = angle;
            }
        }
        if (bestLength < 0.5f) {
            return new Vector2(position.x + MathUtils.random(-6f, 6f), position.y + MathUtils.random(-6f, 6f));
        }
        return new Vector2(position.x + MathUtils.cos(bestAngle) * bestLength * 0.9f,
                position.y + MathUtils.sin(bestAngle) * bestLength * 0.9f);
    }

    private float clearDistance(Vector2 from, Vector2 to, final float length) {
        final float[] clear = {length};
        match.getWorld().rayCast(new RayCastCallback() {
            @Override
            public float reportRayFixture(Fixture fixture, Vector2 point, Vector2 normal, float fraction) {
                if (fixture.isSensor() && (fixture.getFilterData().categoryBits & BUILDING_TAG) == 0) {
                    return -1;
                }
                if (fixture.getBody() == bot.getBody()) {
                    return -1;
                }
                clear[0] = fraction * length;
                return fraction;
            }
        }, from, to);
        return clear[0];
    }

    private float acquisitionRange() {
        boolean sniper = bot.getInventory().getSelectedItem() instanceof SniperRifle;
        return (sniper ? 9f : 5f) * aggression;
    }

    /** Nearest living soldier in range and in sight. Unarmed bots only notice someone in arm's reach. */
    private Soldier findTarget() {
        boolean armed = bot.getInventory().hasWeapon();
        float range = armed ? acquisitionRange() : MELEE_RANGE;
        Vector2 position = bot.getBody().getPosition();
        Soldier best = null;
        float bestDistance = range;
        for (Soldier other : world.getSoldiers()) {
            if (other == bot || other.isDead()) {
                continue;
            }
            float distance = position.dst(other.getBody().getPosition());
            if (distance <= bestDistance && hasLineOfSight(position, other.getBody().getPosition())) {
                best = other;
                bestDistance = distance;
            }
        }
        return best;
    }

    private boolean hasLineOfSight(Vector2 from, Vector2 to) {
        return navigator == null || navigator.hasLineOfSight(from, to);
    }

    private void selectBestWeapon() {
        ArrayList<EntityObject> items = bot.getInventory().getItems();
        int best = -1;
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i) instanceof Weapon && (best < 0 || items.get(i).getTier() > items.get(best).getTier())) {
                best = i;
            }
        }
        if (best >= 0) {
            selectedInventorySlot = best;
        }
    }

    /** Called when the bot walks onto a loot pile or a corpse. Takes what improves its kit. */
    public void searchForItems(ArrayList<EntityObject> pile) {
        Collections.sort(pile);
        Inventory inventory = bot.getInventory();
        for (int i = 0; i < pile.size(); i++) {
            EntityObject item = pile.get(i);
            if (item instanceof Empty) {
                continue;
            }
            if (item instanceof Helmet) {
                pile.set(i, swapEquipment(inventory, Inventory.HELMET, item));
            } else if (item instanceof Armor) {
                pile.set(i, swapEquipment(inventory, Inventory.VEST, item));
            } else if (item instanceof Weapon) {
                pile.set(i, takeWeapon(inventory, (Weapon) item));
            } else if (item instanceof Medkit) {
                int free = freeSlot(inventory);
                if (free >= 0) {
                    inventory.getItems().set(free, item);
                    pile.set(i, new Empty(match));
                }
            }
        }
    }

    /** Equips the item if it has more armor left than what is worn; returns what goes back on the pile. */
    private EntityObject swapEquipment(Inventory inventory, int slot, EntityObject item) {
        EntityObject worn = inventory.getEquipmentItems().get(slot);
        if (armorPoints(item) > armorPoints(worn)) {
            inventory.getEquipmentItems().set(slot, item);
            return worn;
        }
        return item;
    }

    private static int armorPoints(EntityObject item) {
        if (item instanceof Helmet) {
            return ((Helmet) item).getArmorPoints();
        }
        if (item instanceof Armor) {
            return ((Armor) item).getArmorPoints();
        }
        return 0;
    }

    /** Bots carry one weapon of each kind and replace the weakest when they find better. */
    private EntityObject takeWeapon(Inventory inventory, Weapon weapon) {
        for (EntityObject held : inventory.getItems()) {
            if (held.getClass() == weapon.getClass()) {
                return weapon;
            }
        }
        weapon.fillMagazine();
        int free = freeSlot(inventory);
        if (free >= 0) {
            inventory.getItems().set(free, weapon);
            return new Empty(match);
        }
        int weakest = -1;
        for (int i = 0; i < inventory.getItems().size(); i++) {
            EntityObject held = inventory.getItems().get(i);
            if (held instanceof Weapon && (weakest < 0 || held.getTier() < inventory.getItems().get(weakest).getTier())) {
                weakest = i;
            }
        }
        if (weakest >= 0 && inventory.getItems().get(weakest).getTier() < weapon.getTier()) {
            EntityObject dropped = inventory.getItems().get(weakest);
            inventory.getItems().set(weakest, weapon);
            return dropped;
        }
        return weapon;
    }

    private static int freeSlot(Inventory inventory) {
        for (int i = 0; i < inventory.getItems().size(); i++) {
            if (inventory.getItems().get(i) instanceof Empty) {
                return i;
            }
        }
        return -1;
    }

    private void remember(Vector2 position) {
        visited.add(new Vector2(position));
        if (visited.size() > MEMORY) {
            visited.remove(0);
        }
    }

    private boolean isRemembered(Vector2 position) {
        for (Vector2 seen : visited) {
            if (seen.epsilonEquals(position, 0.01f)) {
                return true;
            }
        }
        return false;
    }

    private SafeZoneController zone() {
        return match.getSafeZoneController();
    }

    private void setState(State next) {
        state = next;
        stateTime = 0;
        stuckTime = 0;
        if (next != State.FIGHTING) {
            bot.setPursuit(false);
        }
    }

    public int getSelectedInventorySlot() {
        return selectedInventorySlot;
    }
}
