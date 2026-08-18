package com.joanathanps.battlearena.entities.tiles;

import com.badlogic.gdx.math.Rectangle;
import com.joanathanps.battlearena.entities.Enemy;
import com.joanathanps.battlearena.entities.Lootable;
import com.joanathanps.battlearena.entities.objects.EntityObject;
import com.joanathanps.battlearena.entities.objects.LootPickup;
import com.joanathanps.battlearena.entities.objects.Medkit;
import com.joanathanps.battlearena.entities.objects.equipment.armor.MilitaryVest;
import com.joanathanps.battlearena.entities.objects.equipment.armor.SoftVest;
import com.joanathanps.battlearena.entities.objects.equipment.helmets.CombatHelmet;
import com.joanathanps.battlearena.entities.objects.equipment.helmets.HalfHelmet;
import com.joanathanps.battlearena.entities.objects.weapons.ars.STAR;
import com.joanathanps.battlearena.entities.objects.weapons.ars.W16A;
import com.joanathanps.battlearena.entities.objects.weapons.pistols.G21;
import com.joanathanps.battlearena.entities.objects.weapons.pistols.P26;
import com.joanathanps.battlearena.entities.objects.weapons.srs.AW3;
import com.joanathanps.battlearena.entities.objects.weapons.srs.M20;
import com.joanathanps.battlearena.scenes.Match;
import com.joanathanps.battlearena.scheme.MathUtils;

import java.util.ArrayList;

import static com.joanathanps.battlearena.scheme.PhysicsAdapter.*;

public abstract class Loot extends InteractiveTile implements Lootable {

    private boolean playerIsColliding;

    private ArrayList<EntityObject> items;

    Loot(Rectangle bounds, Match match) {
        super(bounds, match, false, LOOT_TAG);
        init();
    }

    private void init() {
        items = new ArrayList<>();
    }

    public void update(float delta) {
        if (playerIsColliding && LootPickup.hasItems(items)) {
            LootPickup.collect(getMatch().getPlayer().getInventory(), items);
        }
    }

    ArrayList<EntityObject> getItems() {
        return items;
    }

    public Match getMatch() {
        return super.getMatch();
    }

    @Override
    public void onPlayerEnter() {
        playerIsColliding = true;
    }

    @Override
    public void onPlayerLeave() {
        playerIsColliding = false;
    }

    @Override
    public void onEnemyEnter(Enemy enemy) {
        enemy.getAi().searchForItems(items);
    }

    @Override
    public void onEnemyLeave(Enemy enemy) {

    }

    public EntityObject getRandomItem() {
        int randomNumber = MathUtils.randomRange(1, 100);

        if (randomNumber <= 13 ) {
            return new Medkit(getMatch());
        } else if (randomNumber <= 25 ) {
            return new SoftVest(getMatch());
        } else if (randomNumber <= 34 ) {
            return new HalfHelmet(getMatch());
        } else if (randomNumber <= 40 ) {
            return new MilitaryVest(getMatch());
        } else if (randomNumber <= 43 ) {
            return new CombatHelmet(getMatch());
        } else if (randomNumber <= 68 ) {
            return new G21(getMatch());
        } else if (randomNumber <= 80 ) {
            return new P26(getMatch());
        } else if (randomNumber <= 90 ) {
            return new STAR(getMatch());
        } else if (randomNumber <= 95 ) {
            return new W16A(getMatch());
        } else if (randomNumber <= 98 ) {
            return new M20(getMatch());
        } else if (randomNumber <= 100 ) {
            return new AW3(getMatch());
        }

        return null;
    }
}
