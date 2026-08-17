package com.joanathanps.battlearena.entities.objects.weapons.ars;

import com.joanathanps.battlearena.entities.objects.ammo.FiveFiveSix;
import com.joanathanps.battlearena.entities.objects.weapons.Weapon;
import com.joanathanps.battlearena.scenes.Match;

public abstract class AssaultRifle extends Weapon {

    private FiveFiveSix ammo;

    public AssaultRifle(Match match) {
        super(match);
        ammo = new FiveFiveSix(match);
    }

    @Override
    public Object getAmmoType() {
        return ammo.getClass();
    }

    @Override
    public float getTimeToTransform() {
        return .03f;
    }
}
