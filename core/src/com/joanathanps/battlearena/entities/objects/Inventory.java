package com.joanathanps.battlearena.entities.objects;

import com.joanathanps.battlearena.entities.Soldier;
import com.joanathanps.battlearena.entities.objects.equipment.armor.Armor;
import com.joanathanps.battlearena.entities.objects.equipment.helmets.Helmet;
import com.joanathanps.battlearena.entities.objects.weapons.Weapon;
import com.joanathanps.battlearena.scenes.Match;

import java.util.ArrayList;

/** Six item slots plus two equipment slots: 0 is the helmet, 1 is the vest. */
public abstract class Inventory {

    public static final int SLOTS = 6;
    public static final int HELMET = 0;
    public static final int VEST = 1;

    private final Match match;
    private final Soldier soldier;
    private final ArrayList<EntityObject> items = new ArrayList<>();
    private final ArrayList<EntityObject> equipmentItems = new ArrayList<>();
    private boolean isSelectedBeingUsed;

    public Inventory(Match match, Soldier soldier) {
        this.match = match;
        this.soldier = soldier;
        for (int i = 0; i < SLOTS; i++) {
            items.add(new Empty(match));
        }
        equipmentItems.add(new Empty(match));
        equipmentItems.add(new Empty(match));
    }

    /** The slot this soldier is holding. */
    public abstract int getSelectedIndex();

    public abstract void useSelectedItem(Soldier soldier);

    public abstract void reloadSelectedWeapon();

    public EntityObject getSelectedItem() {
        return items.get(getSelectedIndex());
    }

    public void update(float delta) {
        for (EntityObject item : items) {
            if (item instanceof Weapon) {
                ((Weapon) item).tick(delta);
            }
        }
        int armorPoints = 0;
        EntityObject helmet = equipmentItems.get(HELMET);
        EntityObject vest = equipmentItems.get(VEST);
        if (helmet instanceof Helmet) {
            armorPoints += Math.max(0, ((Helmet) helmet).getArmorPoints());
        }
        if (vest instanceof Armor) {
            armorPoints += Math.max(0, ((Armor) vest).getArmorPoints());
        }
        soldier.setArmor(armorPoints);
    }

    /** Bullets in the held weapon's magazine, or -1 when no weapon is held. */
    public int getBulletsInMagazine() {
        EntityObject selected = getSelectedItem();
        return selected instanceof Weapon ? ((Weapon) selected).getBullets() : -1;
    }

    public boolean hasWeapon() {
        for (EntityObject item : items) {
            if (item instanceof Weapon) {
                return true;
            }
        }
        return false;
    }

    public boolean isSelectedBeingUsed() {
        return isSelectedBeingUsed;
    }

    public void setSelectedBeingUsed(boolean selectedBeingUsed) {
        isSelectedBeingUsed = selectedBeingUsed;
    }

    public ArrayList<EntityObject> getItems() {
        return items;
    }

    public ArrayList<EntityObject> getEquipmentItems() {
        return equipmentItems;
    }

    public EntityObject getItem(int index) {
        return items.get(index);
    }

    public Match getMatch() {
        return match;
    }

    public Soldier getSoldier() {
        return soldier;
    }
}
