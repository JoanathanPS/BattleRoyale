package com.joanathanps.battlearena.entities.objects.weapons.ars;

import com.joanathanps.battlearena.graphics.ResourceHandler;
import com.joanathanps.battlearena.scenes.Match;

public class STAR extends AssaultRifle {

    public STAR(Match match) {
        super(match);
        setName(getI18n().getBundle().get("star"));
        setIcon(getResources().getTexture(ResourceHandler.TexturePath.STAR));
        setDamage(8);
        setSecondsBetweenShots(.1f);
        setReloadSeconds(3);
        setMagCapacity(20);
        setTier(3);
    }
}
