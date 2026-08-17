package com.joanathanps.battlearena.entities.objects.weapons.srs;

import com.joanathanps.battlearena.graphics.ResourceHandler;
import com.joanathanps.battlearena.scenes.Match;

public class AW3 extends SniperRifle {

    public AW3(Match match) {
        super(match);
        setAttributes();
    }

    private void setAttributes() {
        setName(getI18n().getBundle().get("aW3"));
        setIcon(getResources().getTexture(ResourceHandler.TexturePath.AW3));
        setDamage(90);
        setRateOfFire(5);
        setReloadTime(5);
        setMagCapacity(1);
        setAccuracy(1);
        setRarity(1);
    }
}