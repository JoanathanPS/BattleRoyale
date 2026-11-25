package com.joanathanps.battlearena.entities.objects.weapons.srs;

import com.joanathanps.battlearena.graphics.ResourceHandler;
import com.joanathanps.battlearena.scenes.Match;

public class AW3 extends SniperRifle {

    public AW3(Match match) {
        super(match);
        setName(getI18n().getBundle().get("aW3"));
        setIcon(getResources().getTexture(ResourceHandler.TexturePath.AW3));
        setDamage(135);
        setSecondsBetweenShots(5);
        setReloadSeconds(5);
        setMagCapacity(1);
        setTier(6);
    }
}
