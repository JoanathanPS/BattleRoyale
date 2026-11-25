package com.joanathanps.battlearena.entities.objects;

import com.joanathanps.battlearena.entities.Soldier;
import com.joanathanps.battlearena.graphics.ResourceHandler;
import com.joanathanps.battlearena.scenes.Match;

public class Medkit extends EntityObject {

    public static final int HEAL = 50;
    public static final float USE_SECONDS = 5f;

    public Medkit(Match match) {
        super(match);
        setAttributes();
    }

    @Override
    public boolean transformSoldier(Soldier soldier) {
        soldier.heal(HEAL);
        return true;
    }

    private void setAttributes() {
        setName(getI18n().getBundle().get("medkit"));
        setIcon(getResources().getTexture(ResourceHandler.TexturePath.MEDKIT_ICON));
    }
}
