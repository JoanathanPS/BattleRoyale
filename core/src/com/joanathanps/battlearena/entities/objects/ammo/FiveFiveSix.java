package com.joanathanps.battlearena.entities.objects.ammo;

import com.joanathanps.battlearena.graphics.ResourceHandler;
import com.joanathanps.battlearena.scenes.Match;

public class FiveFiveSix extends Ammo{

    public FiveFiveSix(Match match) {
        super(match);
        setAttributes();
    }

    @Override
    public void updateName() {
        setName(getI18n().getBundle().get("fiveFiveSix") + " (" + getAmount() + "x)");
    }

    private void setAttributes() {
        setAmount(80);
        updateName();
        setIcon(getResources().getTexture(ResourceHandler.TexturePath.FIVE_FIVE_SIX));
        setRarity(9);
    }
}
