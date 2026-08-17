package com.joanathanps.battlearena.entities.objects;

import com.joanathanps.battlearena.entities.Soldier;
import com.joanathanps.battlearena.graphics.ResourceHandler;
import com.joanathanps.battlearena.scenes.Match;

public class Empty extends EntityObject {

    public Empty(Match match) {
        super(match);
        setAttributes();
    }

    @Override
    public boolean transformSoldier(Soldier soldier) {
        return false;
    }

    private void setAttributes() {
        setName(getI18n().getBundle().get("empty"));
        setIcon(getResources().getTexture(ResourceHandler.TexturePath.EMPTY_SLOT));
        setRarity(14);
    }
}
