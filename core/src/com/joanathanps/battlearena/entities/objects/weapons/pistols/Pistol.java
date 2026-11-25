package com.joanathanps.battlearena.entities.objects.weapons.pistols;

import com.joanathanps.battlearena.entities.objects.weapons.Weapon;
import com.joanathanps.battlearena.scenes.Match;

public abstract class Pistol extends Weapon {

    public Pistol(Match match) {
        super(match);
    }

    @Override
    public float getRange() {
        return 16f;
    }
}
