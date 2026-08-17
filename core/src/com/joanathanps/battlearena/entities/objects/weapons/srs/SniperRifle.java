package com.joanathanps.battlearena.entities.objects.weapons.srs;

import com.joanathanps.battlearena.entities.objects.ammo.SevenSixTwo;
import com.joanathanps.battlearena.entities.objects.weapons.Weapon;
import com.joanathanps.battlearena.scenes.Match;

public abstract class SniperRifle extends Weapon {

    private SevenSixTwo ammo;

    public SniperRifle(Match match) {
        super(match);
        ammo = new SevenSixTwo(match);
    }

    @Override
    public Object getAmmoType() {
        return ammo.getClass();
    }

    @Override
    public float getTimeToTransform() {
        return .04f;
    }
}
