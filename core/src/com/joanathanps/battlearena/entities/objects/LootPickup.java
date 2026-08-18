package com.joanathanps.battlearena.entities.objects;

import com.joanathanps.battlearena.entities.objects.equipment.armor.Armor;
import com.joanathanps.battlearena.entities.objects.equipment.helmets.Helmet;
import com.joanathanps.battlearena.entities.objects.weapons.Weapon;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Instant, frictionless pickup helper (Section 3/2 of the V6 gameplay redesign).
 * Walks over a pile of ground items and places every collectable item straight
 * into a soldier's inventory - no interaction key, no timed transform.
 * Weapons are prepared instantly: full magazine, no ammo boxes required.
 */
public final class LootPickup {

    private LootPickup() { }

    public static void collect(Inventory inventory, ArrayList<EntityObject> items) {
        Collections.sort(items);
        for (int i = 0; i < items.size(); i++) {
            EntityObject item = items.get(i);
            if (item.getClass() == Empty.class) {
                continue;
            }
            if (Helmet.class.isAssignableFrom(item.getClass())) {
                if (tryEquip(inventory, 0, item)) {
                    items.set(i, new Empty(inventory.getMatch()));
                }
            } else if (Armor.class.isAssignableFrom(item.getClass())) {
                if (tryEquip(inventory, 1, item)) {
                    items.set(i, new Empty(inventory.getMatch()));
                }
            } else if (tryAddToFreeSlot(inventory, item)) {
                items.set(i, new Empty(inventory.getMatch()));
            } else if (Weapon.class.isAssignableFrom(item.getClass()) && trySwapWeapon(inventory, item)) {
                items.set(i, new Empty(inventory.getMatch()));
            }
        }
    }

    private static boolean tryEquip(Inventory inventory, int equipmentSlot, EntityObject item) {
        ArrayList<EntityObject> equipment = inventory.getEquipmentItems();
        if (equipment.get(equipmentSlot).getClass() == Empty.class) {
            equipment.set(equipmentSlot, item);
            identifyItem(item);
            return true;
        }
        if (item.getRarity() < equipment.get(equipmentSlot).getRarity()) {
            equipment.set(equipmentSlot, item);
            identifyItem(item);
            return true;
        }
        return false;
    }

    private static boolean tryAddToFreeSlot(Inventory inventory, EntityObject item) {
        for (int i = 0; i < inventory.getItems().size(); i++) {
            if (inventory.getItems().get(i).getClass() == Empty.class) {
                inventory.getItems().set(i, item);
                identifyItem(item);
                return true;
            }
        }
        return false;
    }

    private static boolean trySwapWeapon(Inventory inventory, EntityObject item) {
        for (int i = 0; i < inventory.getItems().size(); i++) {
            EntityObject held = inventory.getItems().get(i);
            if (Weapon.class.isAssignableFrom(held.getClass()) && item.getRarity() < held.getRarity()) {
                inventory.getItems().set(i, item);
                identifyItem(item);
                return true;
            }
        }
        return false;
    }

    private static void identifyItem(EntityObject item) {
        if (Weapon.class.isAssignableFrom(item.getClass())) {
            ((Weapon) item).setBullets(((Weapon) item).getMagCapacity());
        }
    }

    public static boolean hasItems(ArrayList<EntityObject> items) {
        for (EntityObject item : items) {
            if (item.getClass() != Empty.class) {
                return true;
            }
        }
        return false;
    }
}