package com.joanathanps.battlearena.entities.objects;

import com.joanathanps.battlearena.entities.Enemy;
import com.joanathanps.battlearena.entities.Soldier;
import com.joanathanps.battlearena.entities.objects.weapons.Weapon;
import com.joanathanps.battlearena.scenes.Match;

/** Bots pick their slot through their AI and reload instantly. */
public class NPCInventory extends Inventory {

    private final Enemy enemy;

    public NPCInventory(Match match, Enemy enemy) {
        super(match, enemy);
        this.enemy = enemy;
    }

    @Override
    public int getSelectedIndex() {
        return enemy.getAi().getSelectedInventorySlot();
    }

    @Override
    public void useSelectedItem(Soldier soldier) {
        EntityObject selected = getSelectedItem();
        if (selected instanceof Weapon) {
            if (((Weapon) selected).getBullets() == 0) {
                reloadSelectedWeapon();
            } else {
                ((Weapon) selected).shoot(soldier);
            }
        } else {
            soldier.punch();
        }
    }

    @Override
    public void reloadSelectedWeapon() {
        EntityObject selected = getSelectedItem();
        if (selected instanceof Weapon) {
            ((Weapon) selected).fillMagazine();
        }
    }
}
