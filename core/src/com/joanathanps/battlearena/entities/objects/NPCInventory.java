package com.joanathanps.battlearena.entities.objects;

import com.joanathanps.battlearena.entities.Enemy;
import com.joanathanps.battlearena.entities.Soldier;
import com.joanathanps.battlearena.entities.objects.weapons.Weapon;
import com.joanathanps.battlearena.scenes.Match;

public class NPCInventory extends Inventory{

    public NPCInventory(Match match, Soldier soldier) {
        super(match, soldier);
    }

    @Override
    public void useSelectedItem(Soldier soldier) {

    }

    @Override
    public void reloadSelectedWeapon() {
        transferAmmo(0);
    }

    @Override
    public void applyAction() {

    }

    @Override
    public void transferAmmo(int bulletsInWeapon) {
        ((Weapon)getItem(((Enemy)getSoldier()).getAi().getSelectedInventorySlot())).fillMagazine();
    }

    @Override
    public void update(float delta) {
        setArmorPoints(delta);
    }

    @Override
    public int getBulletsInMagazine() {
        if (Weapon.class.isAssignableFrom(getItem(((Enemy)getSoldier()).getAi().getSelectedInventorySlot()).getClass())) {
            return ((Weapon)getItem(((Enemy)getSoldier()).getAi().getSelectedInventorySlot())).getBullets();
        } else {
            return -1;
        }
    }

    @Override
    public int getBulletsInAmmoBoxes() {
        return -1;
    }
}
