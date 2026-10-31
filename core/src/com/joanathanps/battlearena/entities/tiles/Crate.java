package com.joanathanps.battlearena.entities.tiles;

import com.badlogic.gdx.math.Rectangle;
import com.joanathanps.battlearena.entities.objects.EntityObject;
import com.joanathanps.battlearena.scenes.Match;

public class Crate extends Loot {

    public Crate(Rectangle bounds, Match match) {
        super(bounds, match);
        setUserData();
        addItems();
    }

    private void setUserData() {
        getFixture().setUserData(this);
    }

    private void addItems() {
        int itemCount = com.joanathanps.battlearena.scheme.MathUtils.randomRange(3, 4);
        for (int i = 0; i < itemCount; i++) {
            EntityObject item = getRandomItemWithoutDuplicateWeapon(getItems());
            if (item != null) {
                getItems().add(item);
            }
        }
    }

    @Override
    public void onPlayerEnter() {
        super.onPlayerEnter();
    }

    @Override
    public void onPlayerLeave() {
        super.onPlayerLeave();
    }
}
