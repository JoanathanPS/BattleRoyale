package com.joanathanps.battlearena.events;

import com.badlogic.gdx.math.Vector2;
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
import com.joanathanps.battlearena.forge.WorldBuilder;
import com.joanathanps.battlearena.scenes.Match;
import com.joanathanps.battlearena.scheme.MathUtils;

import java.util.ArrayList;
import java.util.Collections;

public class AI {

    private Match match;
    private WorldBuilder worldBuilder;
    private Soldier soldier;
    private Vector2 target;
    private Vector2 altTarget;
    private ArrayList<Vector2> spatialMemory;

    private float decisionInterval;
    private float aggression;
    private float decisionTimeCount;

    private boolean isLooting;
    private boolean isGoingByAltPath;
    private boolean isAltPathTimedOut;

    private int selectedInventorySlot;
    private Soldier enemyOnTarget;

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
        setState(State.CALCULATING_ROUTE);
    }

    private void calculateRoute() {
        ArrayList<Lootable> loot = worldBuilder.getAllLoot();
        double nearestLootDistance = 0;
        Lootable nearestLoot = null;

        for (int i = 0; i < loot.size(); i++) {
            if (i == 0 || MathUtils.distance(
                    soldier.getBody().getPosition(), loot.get(i).getBody().getPosition()
            ) < nearestLootDistance && !spatialMemory.contains(loot.get(i).getBody().getPosition())) {
                nearestLoot = loot.get(i);
                nearestLootDistance = MathUtils.distance(
                        soldier.getBody().getPosition(), loot.get(i).getBody().getPosition()
                );
            }
        }
        if (nearestLoot != null) {
            target = nearestLoot.getBody().getPosition();
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
            if (((SteeringBehavior)soldier).seek(target)) {
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
            altTarget = new Vector2();
            altTarget.x = soldier.getBody().getPosition().x + MathUtils.randomRange(-30, 30);
            altTarget.y = soldier.getBody().getPosition().y + MathUtils.randomRange(-30, 30);
            isGoingByAltPath = true;
            Timer.schedule(new Timer.Task(){
                @Override
                public void run() {
                    isAltPathTimedOut = true;
                }
            }, 5);
        } else {
            if (MathUtils.distance(soldier.getBody().getPosition(), altTarget) < 1 || isAltPathTimedOut) {
                setState(State.CALCULATING_ROUTE);
                isGoingByAltPath = false;
                isAltPathTimedOut = false;
            } else {
                ((SteeringBehavior)soldier).seek(altTarget);
            }
        }
    }

    private void goToSafeZone() {
        ((Enemy)soldier).setPursuit(false);
        target = new Vector2(41 + match.getSafeZoneController()
                .getSafezoneOffsets()[match.getSafeZoneController().getCurrentOffset()].x,
                41 + match.getSafeZoneController()
                        .getSafezoneOffsets()[match.getSafeZoneController().getCurrentOffset()].y);
        ((SteeringBehavior)soldier).seek(target);

        if (!match.getSafeZoneController().soldierIsInDangerZone(soldier)) {
            setState(State.SEEKING_LOOT);
        }
    }

    private void chase() {
        ((Enemy)soldier).setPursuit(true);
        if (MathUtils.distance(soldier.getBody().getPosition(), enemyOnTarget.getBody().getPosition()) > 4) {
            if (MathUtils.distance(soldier.getBody().getPosition(), enemyOnTarget.getBody().getPosition()) < 7 * aggression) {
                ((SteeringBehavior)soldier).seek(enemyOnTarget.getBody().getPosition());
            } else {
                setState(State.CALCULATING_ROUTE);
            }
        }
        checkAvailableWeapons();
        if (hasWeapon()) {
            if (soldier.getInventory().getBulletsInMagazine() < 1) {
                soldier.getInventory().reloadSelectedWeapon();
            } else {
                ((Weapon)soldier.getInventory().getItem(selectedInventorySlot)).shoot(soldier);
            }
        }
        if (enemyOnTarget.isDead()) {
            setState(State.CALCULATING_ROUTE);
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
            if (Helmet.class.isAssignableFrom(items.get(index).getClass())) {
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
            } else if (Armor.class.isAssignableFrom(items.get(index).getClass())) {
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
                    if (!Weapon.class.isAssignableFrom(items.get(index).getClass()) || !hasWeapon()) {
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
        if (Weapon.class.isAssignableFrom(item.getClass())) {
            ((Weapon) item).fillMagazine();
        }
    }

    private boolean hasItem(EntityObject lootItem) {
        if (Armor.class.isAssignableFrom(lootItem.getClass()) ||
                Helmet.class.isAssignableFrom(lootItem.getClass())) {
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

    private boolean hasWeapon() {
        for (EntityObject item : soldier.getInventory().getItems()) {
            if (Weapon.class.isAssignableFrom(item.getClass())) {
                return true;
            }
        }
        return false;
    }

    private boolean isItemWorthSwapping(EntityObject lootItem) {
        if (Helmet.class.isAssignableFrom(lootItem.getClass())) {
            return lootItem.getRarity() < soldier.getInventory().getEquipmentItems().get(0).getRarity();
        } else if (Armor.class.isAssignableFrom(lootItem.getClass())) {
            return lootItem.getRarity() < soldier.getInventory().getEquipmentItems().get(1).getRarity();
        }
        return false;
    }

    private boolean isItemWorthSwapping(EntityObject lootItem, EntityObject inventoryItem) {
         if (Weapon.class.isAssignableFrom(lootItem.getClass()) && Weapon.class.isAssignableFrom(inventoryItem.getClass())) {
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
            if (SniperRifle.class.isAssignableFrom(items.get(i).getClass())) {
                selectedInventorySlot = i;
                return;
            }
        }
        for (int i = 0; i < soldier.getInventory().getItems().size(); i++) {
             if (AssaultRifle.class.isAssignableFrom(items.get(i).getClass())) {
                 selectedInventorySlot = i;
                 return;
            }
        }
        for (int i = 0; i < soldier.getInventory().getItems().size(); i++) {
            if (Pistol.class.isAssignableFrom(items.get(i).getClass())) {
                selectedInventorySlot = i;
                return;
            }
        }
    }

    private void checkZoneTime() {
        if (match.getSafeZoneController().getSecondsToNextEvent() <
                match.getSafeZoneController().getSECONDS_NEXT() / 2 &&
                !match.getSafeZoneController().isInZoneEvent()) {
            setState(State.GOING_TO_SAFE_ZONE);
        }
    }

    private void checkSoldiersAround() {
        Soldier nearest = null;
        double nearestDistance = Float.MAX_VALUE;
        float acquisitionRange = 5 * aggression;

        if (!hasWeapon()) {
            return;
        }

        Vector2 position = soldier.getBody().getPosition();
        for (Soldier combatant : worldBuilder.getSoldiers()) {
            if (combatant == soldier || combatant.isDead()) {
                continue;
            }
            double distanceToCombatant = MathUtils.distance(position, combatant.getBody().getPosition());
            if (distanceToCombatant < acquisitionRange && distanceToCombatant < nearestDistance) {
                nearest = combatant;
                nearestDistance = distanceToCombatant;
            }
        }
        if (nearest != null) {
            enemyOnTarget = nearest;
            setState(State.CHASING);
        }
    }
}
