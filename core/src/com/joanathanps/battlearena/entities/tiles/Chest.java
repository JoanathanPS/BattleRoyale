package com.joanathanps.battlearena.entities.tiles;

import com.badlogic.gdx.math.Rectangle;
import com.joanathanps.battlearena.entities.objects.EntityObject;
import com.joanathanps.battlearena.scenes.Match;

import static com.joanathanps.battlearena.scheme.MathUtils.randomRange;

public class Chest extends Loot {

    public Chest(Rectangle bounds, Match match) {
        super(bounds, match);
        setUserData();
        addItems();
    }

    private void setUserData() {
        getFixture().setUserData(this);
    }

    private void addItems() {
        int itemCount = randomRange(4, 5);
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
