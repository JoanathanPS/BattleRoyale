package com.joanathanps.battlearena.entities.objects.weapons.ars;

import com.joanathanps.battlearena.entities.objects.weapons.Weapon;
import com.joanathanps.battlearena.scenes.Match;

public abstract class AssaultRifle extends Weapon {

    public AssaultRifle(Match match) {
        super(match);
    }

    @Override
    public float getRange() {
        return 20f;
    }
}
