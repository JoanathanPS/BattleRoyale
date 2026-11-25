package com.joanathanps.battlearena.entities.objects.weapons.ars;

import com.joanathanps.battlearena.graphics.ResourceHandler;
import com.joanathanps.battlearena.scenes.Match;

public class W16A extends AssaultRifle {

    public W16A(Match match) {
        super(match);
        setName(getI18n().getBundle().get("w16A"));
        setIcon(getResources().getTexture(ResourceHandler.TexturePath.W16A));
        setDamage(9);
        setSecondsBetweenShots(.2f);
        setReloadSeconds(4);
        setMagCapacity(20);
        setTier(4);
    }
}
