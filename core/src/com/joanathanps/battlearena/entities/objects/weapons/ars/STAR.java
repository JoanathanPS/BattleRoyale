package com.joanathanps.battlearena.entities.objects.weapons.ars;

import com.joanathanps.battlearena.graphics.ResourceHandler;
import com.joanathanps.battlearena.scenes.Match;

public class STAR extends AssaultRifle {

    public STAR(Match match) {
        super(match);
        setAttributes();
    }

    private void setAttributes() {
        setName(getI18n().getBundle().get("star"));
        setIcon(getResources().getTexture(ResourceHandler.TexturePath.STAR));
        setDamage(5);
        setRateOfFire(.1f);
        setReloadTime(3);
        setMagCapacity(20);
        setAccuracy(5);
        setRarity(5);
    }
}