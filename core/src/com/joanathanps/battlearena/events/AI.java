package com.joanathanps.battlearena.events;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.RayCastCallback;
import com.badlogic.gdx.utils.Timer;
import com.joanathanps.battlearena.entities.Enemy;
import com.joanathanps.battlearena.entities.Lootable;
import com.joanathanps.battlearena.entities.Soldier;
import com.joanathanps.battlearena.entities.objects.Empty;
import com.joanathanps.battlearena.entities.objects.EntityObject;
import com.joanathanps.battlearena.entities.objects.equipment.armor.Armor;
import com.joanathanps.battlearena.entities.objects.equipment.helmets.Helmet;
import com.joanathanps.battlearena.entities.objects.weapons.Weapon;
import com.joanathanps.battlearena.entities.objects.weapons.ars.AssaultRifle;
import com.joanathanps.battlearena.entities.objects.weapons.pistols.Pistol;
import com.joanathanps.battlearena.entities.objects.weapons.srs.SniperRifle;
import com.joanathanps.battlearena.entities.tiles.Loot;
import com.joanathanps.battlearena.forge.WorldBuilder;
import com.joanathanps.battlearena.scenes.Match;
import com.joanathanps.battlearena.scheme.MathUtils;

import java.util.ArrayList;
import java.util.Collections;

import static com.joanathanps.battlearena.scheme.PhysicsAdapter.BUILDING_TAG;

public class AI {

    private Match match;
    private WorldBuilder worldBuilder;
    private Soldier soldier;
    private Vector2 target;
    private Vector2 altTarget;
    private ArrayList<Vector2> spatialMemory;
    private WaypointNavigator navigator;

    private float decisionInterval;
    private float aggression;
    private float decisionTimeCount;

    private boolean isLooting;
    private boolean isGoingByAltPath;
    private boolean isAltPathTimedOut;
    private float stamina;
    private float maxStamina;
    private float sprintCooldown;
    private boolean isSprinting;

    private int selectedInventorySlot;
    private Soldier enemyOnTarget;

    private static final float MELEE_CHASE_RANGE = 1.5f;
    private static final float SPRINT_CHASE_RANGE = 4f;

    private enum State {
        IDLE, CALCULATING_ROUTE, SEEKING_LOOT, LOOTING, FINDING_PATH, GOING_TO_SAFE_ZONE, CHASING
    }

    private State state;
    private State previousState;

    public AI(Match match, Soldier soldier) {
        state = State.IDLE;
        this.match = match;
        this.soldier = soldier;
        init();
    }

    private void init() {
        spatialMemory = new ArrayList<>();
        selectedInventorySlot = 0;
        decisionInterval = match.getDifficulty().getReactionSeconds();
        aggression = match.getDifficulty().getAggressionMultiplier();
        
        // Set stamina based on difficulty
        switch (match.getDifficulty()) {
            case EASY:
                maxStamina = 50f;
                sprintCooldown = 2f;
                break;
            case MEDIUM:
                maxStamina = 75f;
                sprintCooldown = 1f;
                break;
            case HARD:
                maxStamina = 100f;
                sprintCooldown = 0.5f;
                break;
        }
        stamina = maxStamina;
        isSprinting = false;
    }

    public void update(float delta) {
        switch (state) {
            case CALCULATING_ROUTE:
                calculateRoute();
                break;
            case SEEKING_LOOT:
                seekLoot();
                break;
            case LOOTING:
                loot();
                break;
            case FINDING_PATH:
                findPath();
                break;
            case GOING_TO_SAFE_ZONE:
                goToSafeZone();
                break;
            case CHASING:
                chase();
                break;
        }

        decisionTimeCount += delta;
        if (decisionTimeCount >= decisionInterval) {
            decisionTimeCount = 0;
            checkAvailableWeapons();
            checkZoneTime();
            if (state != State.CHASING && state != State.GOING_TO_SAFE_ZONE) {
                checkSoldiersAround();
            }
        }
    }

    public void wakeUp(WorldBuilder worldBuilder) {
        this.worldBuilder = worldBuilder;
        this.navigator = worldBuilder.getWaypointNavigator();
        setState(State.CALCULATING_ROUTE);
    }

    private void calculateRoute() {
        ArrayList<Lootable> loot = worldBuilder.getAllLoot();
        double nearestLootDistance = 0;
        Lootable nearestLoot = null;

        for (int i = 0; i < loot.size(); i++) {
            // Skip loot already in spatial memory (recently visited)
            if (spatialMemory.contains(loot.get(i).getBody().getPosition())) {
                continue;
            }
            // Skip empty loot containers — no point walking to them
            if (loot.get(i) instanceof Loot && !((Loot) loot.get(i)).hasItems()) {
                continue;
            }
            // Skip dead enemies with nothing left on them
            if (loot.get(i) instanceof Enemy) {
                Enemy deadEnemy = (Enemy) loot.get(i);
                if (!com.joanathanps.battlearena.entities.objects.LootPickup.hasItems(
                        deadEnemy.getInventory().getItems())) {
                    continue;
                }
            }
            double distanceToLoot = MathUtils.distance(
                    soldier.getBody().getPosition(), loot.get(i).getBody().getPosition());

            if (nearestLoot == null || distanceToLoot < nearestLootDistance) {
                nearestLoot = loot.get(i);
                nearestLootDistance = distanceToLoot;
            }
        }

        if (nearestLoot != null) {
            target = nearestLoot.getBody().getPosition();
            // Add to spatial memory if we've been close enough to consider it visited
            if (nearestLootDistance < 3) {
                spatialMemory.add(nearestLoot.getBody().getPosition());
                if (spatialMemory.size() > 5) {
                    spatialMemory.remove(0);
                }
            }
        }
        setState(State.SEEKING_LOOT);
    }

    private void seekLoot() {
        ((Enemy)soldier).setPursuit(false);
        if (MathUtils.distance(soldier.getBody().getPosition(), target) < 1) {
            spatialMemory.add(target);
            soldier.getBody().getFixtureList().first().setSensor(false);
            setState(State.LOOTING);
        } else {
            // Use the waypoint navigator so the bot routes around buildings
            // rather than pushing straight through the wall toward loot.
            Vector2 nextStep = (navigator != null)
                    ? navigator.getNextWaypoint(soldier.getBody().getPosition(), target)
                    : target;

            if (((SteeringBehavior)soldier).seek(nextStep)) {
                if (soldier.getBody().getLinearVelocity().x == 0 && soldier.getBody().getLinearVelocity().y == 0) {
                    if (!match.getCamera().frustum.pointInFrustum(
                            soldier.getBody().getPosition().x, soldier.getBody().getPosition().y, 0
                    )) {
                        soldier.getBody().getFixtureList().first().setSensor(true);
                        ((Enemy)soldier).setVisible(false);
                        Timer.schedule(new Timer.Task(){
                            @Override
                            public void run() {
                                ((Enemy)soldier).setVisible(true);
                                soldier.getBody().getFixtureList().first().setSensor(false);
                            }
                        }, 1);
                    } else {
                        setState(State.FINDING_PATH);
                    }
                }
            }
        }
    }

    private void loot() {
        if (!isLooting) {
            isLooting = true;
            Timer.schedule(new Timer.Task(){
                @Override
                public void run() {
                    isLooting = false;
                    setState(State.CALCULATING_ROUTE);
                }
            }, 3);
        }
    }

    private void findPath() {
        ((Enemy)soldier).setPursuit(false);
        if (!isGoingByAltPath) {
            // Cast several rays in a fan ahead of the bot and pick the
            // clearest direction — much smarter than a random offset.
            altTarget = pickClearDirection();
            isGoingByAltPath = true;
            Timer.schedule(new Timer.Task(){
                @Override
                public void run() {
                    isAltPathTimedOut = true;
                }
            }, 4);
        } else {
            if (MathUtils.distance(soldier.getBody().getPosition(), altTarget) < 1.5 || isAltPathTimedOut) {
                setState(State.CALCULATING_ROUTE);
                isGoingByAltPath = false;
                isAltPathTimedOut = false;
            } else {
                ((SteeringBehavior)soldier).seek(altTarget);
            }
        }
    }

    /**
     * Fans out rays from the bot's current position in 8 cardinal/diagonal
     * directions and returns a point 6 units away in the direction with the
     * longest clear path (no building wall hit).
     *
     * <p>If every direction is blocked (unlikely in an open arena), falls
     * back to a short random offset as before.
     */
    private Vector2 pickClearDirection() {
        Vector2 pos = soldier.getBody().getPosition();
        float probeLen = 6f;
        float bestLen = -1f;
        float bestAngle = 0f;

        // 8-direction fan (every 45°)
        for (int i = 0; i < 8; i++) {
            float angle = (float)(i * Math.PI / 4);
            float dx = (float) Math.cos(angle);
            float dy = (float) Math.sin(angle);
            Vector2 probe = new Vector2(pos.x + dx * probeLen, pos.y + dy * probeLen);

            // Measure how far we can go in this direction before hitting a building
            final float[] clearDist = {probeLen};
            if (navigator != null) {
                match.getWorld().rayCast(new RayCastCallback() {
                    @Override
                    public float reportRayFixture(Fixture fixture, Vector2 point,
                                                  Vector2 normal, float fraction) {
                        if ((fixture.getFilterData().categoryBits & BUILDING_TAG) != 0) {
                            clearDist[0] = fraction * probeLen;
                            return fraction; // keep closest hit
                        }
                        return -1;
                    }
                }, pos, probe);
            }

            if (clearDist[0] > bestLen) {
                bestLen = clearDist[0];
                bestAngle = angle;
            }
        }

        if (bestLen > 0.5f) {
            return new Vector2(
                    pos.x + (float) Math.cos(bestAngle) * bestLen * 0.9f,
                    pos.y + (float) Math.sin(bestAngle) * bestLen * 0.9f
            );
        }

        // Total fallback — short random offset (original behaviour)
        return new Vector2(
                pos.x + MathUtils.randomRange(-10, 10),
                pos.y + MathUtils.randomRange(-10, 10)
        );
    }

    private void goToSafeZone() {
        ((Enemy)soldier).setPursuit(false);
        target = new Vector2(41 + match.getSafeZoneController()
                .getSafezoneOffsets()[match.getSafeZoneController().getCurrentOffset()].x,
                41 + match.getSafeZoneController()
                        .getSafezoneOffsets()[match.getSafeZoneController().getCurrentOffset()].y);

        // Route around buildings so the bot doesn't get blocked by a wall
        // while urgently running to the safe zone.
        Vector2 nextStep = (navigator != null)
                ? navigator.getNextWaypoint(soldier.getBody().getPosition(), target)
                : target;
        ((SteeringBehavior)soldier).seek(nextStep);

        if (!match.getSafeZoneController().soldierIsInDangerZone(soldier)) {
            setState(State.SEEKING_LOOT);
        }
    }

    private void chase() {
        float distanceToTarget = (float) MathUtils.distance(soldier.getBody().getPosition(), enemyOnTarget.getBody().getPosition());
        boolean canSprint = isSprinting && distanceToTarget <= SPRINT_CHASE_RANGE;
        ((Enemy)soldier).setPursuit(canSprint);
        checkAvailableWeapons();
        boolean armed = hasWeapon(soldier);

        if (enemyOnTarget.isDead()) {
            setState(State.CALCULATING_ROUTE);
            return;
        }

        Vector2 enemyPos = enemyOnTarget.getBody().getPosition();

        if (armed) {
            // Armed: close in and shoot
            if (distanceToTarget > 4) {
                if (distanceToTarget < 7 * aggression) {
                    // Route around walls to reach the enemy position
                    Vector2 nextStep = (navigator != null)
                            ? navigator.getNextWaypoint(soldier.getBody().getPosition(), enemyPos)
                            : enemyPos;
                    ((SteeringBehavior)soldier).seek(nextStep);
                } else {
                    setState(State.CALCULATING_ROUTE);
                }
            }
            if (soldier.getInventory().getBulletsInMagazine() < 1) {
                soldier.getInventory().reloadSelectedWeapon();
            } else {
                ((Weapon)soldier.getInventory().getItem(selectedInventorySlot)).shoot(soldier);
            }
        } else {
            // Unarmed: only chase if target is within melee range, then punch
            if (distanceToTarget <= MELEE_CHASE_RANGE) {
                ((SteeringBehavior)soldier).seek(enemyPos);
                soldier.punch();
            } else if (distanceToTarget < 3f) {
                // Close but not melee range yet — keep closing in (no wall routing needed at this range)
                ((SteeringBehavior)soldier).seek(enemyPos);
            } else {
                // Too far for unarmed engagement — go loot instead
                setState(State.CALCULATING_ROUTE);
            }
        }

        // Stamina/sprint management
        if (isSprinting) {
            stamina -= 0.1f * sprintCooldown;
            if (stamina <= 0) {
                isSprinting = false;
                stamina = 0;
            }
        } else {
            stamina = Math.min(maxStamina, stamina + 0.05f);
        }
    }

    private void setState(State state) {
        previousState = this.state;
        this.state = state;
    }

    public void searchForItems(ArrayList<EntityObject> items) {
        Collections.sort(items);
        for (int i = 0; i < items.size(); i++) {
            exchangeItem(items, i);
        }
        // Print items of this enemy
//        System.out.println(" = = = = = = = = = = = = =");
//        for (EntityObject item : soldier.getInventory().getItems()) {
//            System.out.println(item.getName());
//        }
//        System.out.println("- - -");
//        for (EntityObject item : soldier.getInventory().getEquipmentItems()) {
//            System.out.println(item.getName());
//        }
    }

    private void exchangeItem(ArrayList<EntityObject> items, int index) {
        if (!hasItem(items.get(index))) {
            if (items.get(index) instanceof Helmet) {
                if (!hasHelmet()) {
                    if (soldier.getInventory().getEquipmentItems().get(0).getClass() == Empty.class) {
                        soldier.getInventory().getEquipmentItems().set(0, items.get(index));
                        items.set(index, new Empty(match));
                    }
                } else if (isItemWorthSwapping(items.get(index))) {
                    EntityObject escrow = soldier.getInventory().getEquipmentItems().get(0);
                    soldier.getInventory().getEquipmentItems().set(0, items.get(index));
                    items.set(index, escrow);
                }
            } else if (items.get(index) instanceof Armor) {
                if (!hasArmor()) {
                    if (soldier.getInventory().getEquipmentItems().get(1).getClass() == Empty.class) {
                        soldier.getInventory().getEquipmentItems().set(1, items.get(index));
                        items.set(index, new Empty(match));
                    }
                } else if (isItemWorthSwapping(items.get(index))) {
                    EntityObject escrow = soldier.getInventory().getEquipmentItems().get(1);
                    soldier.getInventory().getEquipmentItems().set(1, items.get(index));
                    items.set(index, escrow);
                }
            } else {
                if (!isInventoryFull()) {
                    if (!(items.get(index) instanceof Weapon) || !hasWeapon(soldier)) {
                        for (int j = 0; j < soldier.getInventory().getItems().size(); j++) {
                            if (soldier.getInventory().getItems().get(j).getClass() == Empty.class) {
                                soldier.getInventory().getItems().set(j, items.get(index));
                                items.set(index, new Empty(match));
                                readyItem(soldier.getInventory().getItems().get(j));
                            }
                        }
                    }
                } else {
                    for (int i = 0; i < soldier.getInventory().getItems().size(); i++) {
                        if (isItemWorthSwapping(items.get(index), soldier.getInventory().getItems().get(i))) {
                            EntityObject escrow = soldier.getInventory().getItems().get(i);
                            soldier.getInventory().getItems().set(i, items.get(index));
                            items.set(index, escrow);
                            readyItem(soldier.getInventory().getItems().get(i));
                        }
                    }
                }
            }
        }
    }

    private void readyItem(EntityObject item) {
        if (item instanceof Weapon) {
            ((Weapon) item).fillMagazine();
        }
    }

    private boolean hasItem(EntityObject lootItem) {
        if (lootItem instanceof Armor ||
                lootItem instanceof Helmet) {
            for (EntityObject item : soldier.getInventory().getEquipmentItems()) {
                if (lootItem.getClass() == item.getClass()) {
                    return true;
                }
            }
        } else {
            for (EntityObject item : soldier.getInventory().getItems()) {
                if (lootItem.getClass() == item.getClass()) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isItemWorthSwapping(EntityObject lootItem) {
        if (lootItem instanceof Helmet) {
            return lootItem.getRarity() < soldier.getInventory().getEquipmentItems().get(0).getRarity();
        } else if (lootItem instanceof Armor) {
            return lootItem.getRarity() < soldier.getInventory().getEquipmentItems().get(1).getRarity();
        }
        return false;
    }

    private boolean isItemWorthSwapping(EntityObject lootItem, EntityObject inventoryItem) {
         if (lootItem instanceof Weapon && inventoryItem instanceof Weapon) {
             return lootItem.getRarity() < inventoryItem.getRarity();
        }
        return false;
    }

    private boolean isInventoryFull() {
        int capacity = soldier.getInventory().getItems().size();

        for (EntityObject item : soldier.getInventory().getItems()) {
            if (item.getClass() != Empty.class) {
                capacity--;
            }
        }

        return capacity == 1;
    }

    private boolean hasHelmet() {
        return soldier.getInventory().getEquipmentItems().get(0).getClass() != Empty.class;
    }

    private boolean hasArmor() {
        return soldier.getInventory().getEquipmentItems().get(1).getClass() != Empty.class;
    }

    public int getSelectedInventorySlot() {
        return selectedInventorySlot;
    }

    private void checkAvailableWeapons() {
        ArrayList<EntityObject> items = soldier.getInventory().getItems();
        for (int i = 0; i < soldier.getInventory().getItems().size(); i++) {
            if (items.get(i) instanceof SniperRifle) {
                selectedInventorySlot = i;
                return;
            }
        }
        for (int i = 0; i < soldier.getInventory().getItems().size(); i++) {
             if (items.get(i) instanceof AssaultRifle) {
                 selectedInventorySlot = i;
                 return;
            }
        }
        for (int i = 0; i < soldier.getInventory().getItems().size(); i++) {
            if (items.get(i) instanceof Pistol) {
                selectedInventorySlot = i;
                return;
            }
        }
    }

    private void checkZoneTime() {
        if (match.getSafeZoneController().getCurrentEvent() >= 2 &&
                match.getSafeZoneController().soldierIsInDangerZone(soldier)) {
            setState(State.GOING_TO_SAFE_ZONE);
        } else if (match.getSafeZoneController().getSecondsToNextEvent() <
                match.getSafeZoneController().getSECONDS_NEXT() / 2 &&
                !match.getSafeZoneController().isInZoneEvent()) {
            setState(State.GOING_TO_SAFE_ZONE);
        }
    }

    private void checkSoldiersAround() {
        Soldier nearestVisible = null;
        double nearestVisibleDistance = Float.MAX_VALUE;
        Soldier nearestMeleeTarget = null;
        double nearestMeleeDistance = Float.MAX_VALUE;
        float acquisitionRange = 5 * aggression;

        Vector2 position = soldier.getBody().getPosition();
        for (Soldier combatant : worldBuilder.getSoldiers()) {
            if (combatant == soldier || combatant.isDead()) {
                continue;
            }
            double distanceToCombatant = MathUtils.distance(position, combatant.getBody().getPosition());

            // Only engage targets that are within acquisition range AND visible (no wall between)
            if (distanceToCombatant > acquisitionRange) {
                continue;
            }
            if (!hasLineOfSight(position, combatant.getBody().getPosition())) {
                continue;
            }

            // Track nearest within melee range separately for unarmed bots
            if (distanceToCombatant <= MELEE_CHASE_RANGE && distanceToCombatant < nearestMeleeDistance) {
                nearestMeleeTarget = combatant;
                nearestMeleeDistance = distanceToCombatant;
            }

            // Track nearest visible target for armed engagement
            if (distanceToCombatant < nearestVisibleDistance) {
                nearestVisible = combatant;
                nearestVisibleDistance = distanceToCombatant;
            }
        }

        boolean selfArmed = hasWeapon(soldier);

        if (selfArmed && nearestVisible != null) {
            // Armed bot with a visible target → engage
            enemyOnTarget = nearestVisible;
            setState(State.CHASING);
        } else if (!selfArmed && nearestMeleeTarget != null) {
            // Unarmed bot and someone is already within melee range → punch them
            enemyOnTarget = nearestMeleeTarget;
            setState(State.CHASING);
        } else {
            // No immediate threat / no weapon → keep looting
            setState(State.CALCULATING_ROUTE);
        }
    }

    /**
     * Returns true if there is no building fixture blocking the straight line
     * from {@code from} to {@code to} in the Box2D world.
     */
    private boolean hasLineOfSight(Vector2 from, Vector2 to) {
        // Tiny epsilon guard — same position counts as visible
        if (from.dst(to) < 0.01f) return true;

        final boolean[] blocked = {false};
        match.getWorld().rayCast(new RayCastCallback() {
            @Override
            public float reportRayFixture(Fixture fixture, Vector2 point, Vector2 normal, float fraction) {
                if ((fixture.getFilterData().categoryBits & BUILDING_TAG) != 0) {
                    blocked[0] = true;
                    return 0; // stop ray immediately
                }
                return -1; // ignore other fixtures, continue ray
            }
        }, from, to);

        return !blocked[0];
    }

    private boolean hasWeapon(Soldier s) {
        for (EntityObject item : s.getInventory().getItems()) {
            if (item instanceof Weapon) {
                return true;
            }
        }
        return false;
    }
}
