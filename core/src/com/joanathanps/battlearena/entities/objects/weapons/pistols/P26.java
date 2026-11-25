package com.joanathanps.battlearena.entities.objects.weapons.pistols;

import com.joanathanps.battlearena.graphics.ResourceHandler;
import com.joanathanps.battlearena.scenes.Match;

public class P26 extends Pistol {

    public P26(Match match) {
        super(match);
        setName(getI18n().getBundle().get("p26"));
        setIcon(getResources().getTexture(ResourceHandler.TexturePath.P26));
        setDamage(18);
        setSecondsBetweenShots(.4f);
        setReloadSeconds(1);
        setMagCapacity(17);
        setTier(2);
    }
}
