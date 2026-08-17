package com.joanathanps.battlearena.entities.objects.weapons.pistols;

import com.joanathanps.battlearena.entities.objects.ammo.Nine;
import com.joanathanps.battlearena.entities.objects.weapons.Weapon;
import com.joanathanps.battlearena.scenes.Match;

public abstract class Pistol extends Weapon {

    private Nine ammo;

    public Pistol(Match match) {
        super(match);
        ammo = new Nine(match);
    }

    @Override
    public Object getAmmoType() {
        return ammo.getClass();
    }

    @Override
    public float getTimeToTransform() {
        return .02f;
    }
}
