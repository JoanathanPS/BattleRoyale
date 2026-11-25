package com.joanathanps.battlearena.entities.objects.weapons.srs;

import com.joanathanps.battlearena.entities.objects.weapons.Weapon;
import com.joanathanps.battlearena.scenes.Match;

public abstract class SniperRifle extends Weapon {

    public SniperRifle(Match match) {
        super(match);
    }

    @Override
    public float getRange() {
        return 34f;
    }
}
