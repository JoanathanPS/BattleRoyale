package com.joanathanps.battlearena.entities.objects.weapons.srs;

import com.joanathanps.battlearena.graphics.ResourceHandler;
import com.joanathanps.battlearena.scenes.Match;

public class M20 extends SniperRifle {

    public M20(Match match) {
        super(match);
        setAttributes();
    }

    private void setAttributes() {
        setName(getI18n().getBundle().get("m20"));
        setIcon(getResources().getTexture(ResourceHandler.TexturePath.M20));
        setDamage(70);
        setRateOfFire(3);
        setReloadTime(4);
        setMagCapacity(5);
        setAccuracy(1);
        setRarity(2);
    }
}