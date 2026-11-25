package com.joanathanps.battlearena.entities.objects;

import com.joanathanps.battlearena.entities.objects.equipment.armor.Armor;
import com.joanathanps.battlearena.entities.objects.equipment.helmets.Helmet;
import com.joanathanps.battlearena.entities.objects.weapons.Weapon;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Moves items from a pile (a crate, a chest, a corpse) into a soldier's inventory.
 * Anything the soldier swaps out goes back onto the pile, so nothing disappears.
 */
public final class LootPickup {

    private LootPickup() { }

    /** Takes everything useful from the pile, best items first. */
    public static void collect(Inventory inventory, ArrayList<EntityObject> pile) {
        Collections.sort(pile);
        for (int i = 0; i < pile.size(); i++) {
            EntityObject item = pile.get(i);
            if (!(item instanceof Empty)) {
                pile.set(i, take(inventory, item, false));
            }
        }
    }

    /** Takes one item the player picked from the loot panel. Returns true if anything changed. */
    public static boolean collectSingle(Inventory inventory, EntityObject item, ArrayList<EntityObject> pile) {
        int index = pile.indexOf(item);
        if (index < 0 || item instanceof Empty) {
            return false;
        }
        EntityObject left = take(inventory, item, true);
        pile.set(index, left);
        return left != item;
    }

    /**
     * Puts the item into the inventory if it improves it and returns what is left for the pile:
     * an Empty, the replaced item, or the item itself when it was not taken.
     * With {@code forceSwap} a full inventory trades the held slot for the item.
     */
    private static EntityObject take(Inventory inventory, EntityObject item, boolean forceSwap) {
        if (item instanceof Helmet || item instanceof Armor) {
            int slot = item instanceof Helmet ? Inventory.HELMET : Inventory.VEST;
            EntityObject worn = inventory.getEquipmentItems().get(slot);
            if (forceSwap || armorPoints(item) > armorPoints(worn)) {
                inventory.getEquipmentItems().set(slot, item);
                return worn;
            }
            return item;
        }
        if (item instanceof Weapon) {
            for (EntityObject held : inventory.getItems()) {
                if (held.getClass() == item.getClass()) {
                    // A second copy of a held gun is just a full magazine.
                    ((Weapon) held).fillMagazine();
                    return new Empty(inventory.getMatch());
                }
            }
            ((Weapon) item).fillMagazine();
        }
        for (int i = 0; i < inventory.getItems().size(); i++) {
            if (inventory.getItems().get(i) instanceof Empty) {
                inventory.getItems().set(i, item);
                return new Empty(inventory.getMatch());
            }
        }
        if (forceSwap) {
            int selected = inventory.getSelectedIndex();
            EntityObject held = inventory.getItems().get(selected);
            inventory.getItems().set(selected, item);
            return held;
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

    public static boolean hasItems(ArrayList<EntityObject> items) {
        for (EntityObject item : items) {
            if (!(item instanceof Empty)) {
                return true;
            }
        }
        return false;
    }
}
