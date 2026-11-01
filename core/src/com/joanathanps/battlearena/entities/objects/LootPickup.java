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
            if (item instanceof Helmet) {
                if (tryEquip(inventory, 0, item)) {
                    items.set(i, new Empty(inventory.getMatch()));
                }
            } else if (item instanceof Armor) {
                if (tryEquip(inventory, 1, item)) {
                    items.set(i, new Empty(inventory.getMatch()));
                }
            } else if (item instanceof Weapon
                    && tryRefillDuplicateWeapon(inventory, (Weapon) item)) {
                // Same weapon already held — ammo topped up, consume the ground copy
                items.set(i, new Empty(inventory.getMatch()));
            } else if (tryAddToFreeSlot(inventory, item)) {
                items.set(i, new Empty(inventory.getMatch()));
            } else if (item instanceof Weapon && trySwapWeapon(inventory, item)) {
                items.set(i, new Empty(inventory.getMatch()));
            }
        }
    }

    /**
     * Collects a single specific item chosen by the player (PUBG style).
     * Transfers item to player's inventory/equipment and clears it from sourceList.
     */
    public static boolean collectSingle(Inventory inventory, EntityObject item, ArrayList<EntityObject> sourceList) {
        if (item == null || item.getClass() == Empty.class) {
            return false;
        }

        boolean collected = false;
        if (item instanceof Helmet) {
            collected = tryEquip(inventory, 0, item);
        } else if (item instanceof Armor) {
            collected = tryEquip(inventory, 1, item);
        } else if (item instanceof Weapon
                && tryRefillDuplicateWeapon(inventory, (Weapon) item)) {
            collected = true;
        } else if (tryAddToFreeSlot(inventory, item)) {
            collected = true;
        } else if (item instanceof Weapon) {
            // Swap with selected slot if inventory is full
            int selectedSlot = inventory.getMatch().getHud().getSelectedSlot();
            EntityObject oldItem = inventory.getItems().get(selectedSlot);
            inventory.getItems().set(selectedSlot, item);
            identifyItem(item);
            // Replace ground item with swapped item or empty
            int idx = sourceList.indexOf(item);
            if (idx != -1) {
                sourceList.set(idx, oldItem);
            }
            return true;
        }

        if (collected) {
            int idx = sourceList.indexOf(item);
            if (idx != -1) {
                sourceList.set(idx, new Empty(inventory.getMatch()));
            }
            return true;
        }
        return false;
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

    /**
     * If the inventory already contains the same weapon class as {@code incoming},
     * top up that weapon's magazine to full capacity and return true.
     * The incoming ground item is consumed by the caller.
     */
    private static boolean tryRefillDuplicateWeapon(Inventory inventory, Weapon incoming) {
        for (EntityObject held : inventory.getItems()) {
            if (held.getClass() == incoming.getClass() && held instanceof Weapon) {
                Weapon heldWeapon = (Weapon) held;
                heldWeapon.setBullets(heldWeapon.getMagCapacity());
                return true;
            }
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
            if (held instanceof Weapon && item.getRarity() < held.getRarity()) {
                inventory.getItems().set(i, item);
                identifyItem(item);
                return true;
            }
        }
        return false;
    }

    private static void identifyItem(EntityObject item) {
        if (item instanceof Weapon) {
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