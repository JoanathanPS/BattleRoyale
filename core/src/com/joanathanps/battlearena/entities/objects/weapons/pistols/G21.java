package com.joanathanps.battlearena.entities.objects.weapons.pistols;

import com.joanathanps.battlearena.graphics.ResourceHandler;
import com.joanathanps.battlearena.scenes.Match;

public class G21 extends Pistol {

    public G21(Match match) {
        super(match);
        setName(getI18n().getBundle().get("g21"));
        setIcon(getResources().getTexture(ResourceHandler.TexturePath.G21));
        setDamage(15);
        setSecondsBetweenShots(.6f);
        setReloadSeconds(2);
        setMagCapacity(15);
        setTier(1);
    }
}
