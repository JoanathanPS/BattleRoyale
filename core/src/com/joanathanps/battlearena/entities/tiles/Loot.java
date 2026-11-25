package com.joanathanps.battlearena.entities.tiles;

import com.badlogic.gdx.math.Rectangle;
import com.joanathanps.battlearena.entities.Enemy;
import com.joanathanps.battlearena.entities.Lootable;
import com.joanathanps.battlearena.entities.objects.Empty;
import com.joanathanps.battlearena.entities.objects.EntityObject;
import com.joanathanps.battlearena.entities.objects.LootPickup;
import com.joanathanps.battlearena.entities.objects.Medkit;
import com.joanathanps.battlearena.entities.objects.equipment.armor.MilitaryVest;
import com.joanathanps.battlearena.entities.objects.equipment.armor.SoftVest;
import com.joanathanps.battlearena.entities.objects.equipment.helmets.CombatHelmet;
import com.joanathanps.battlearena.entities.objects.equipment.helmets.HalfHelmet;
import com.joanathanps.battlearena.entities.objects.weapons.Weapon;
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

    public boolean isPlayerColliding() {
        return playerIsColliding;
    }

    public ArrayList<EntityObject> getItems() {
        return items;
    }

    public boolean hasItems() {
        for (EntityObject item : items) {
            if (!(item instanceof Empty)) {
                return true;
            }
        }
        return false;
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

    public EntityObject getRandomItemWithoutDuplicateWeapon(ArrayList<EntityObject> existingItems) {
        for (int attempts = 0; attempts < 20; attempts++) {
            EntityObject candidate = rollItem();
            if (candidate instanceof Weapon && containsWeaponType(existingItems, candidate.getClass())) {
                continue;
            }
            return candidate;
        }
        return null;
    }

    private boolean containsWeaponType(ArrayList<EntityObject> existingItems, Class<?> weaponClass) {
        for (EntityObject item : existingItems) {
            if (item instanceof Weapon && item.getClass() == weaponClass) {
                return true;
            }
        }
        return false;
    }

    /**
     * Drop table, out of 100: medkit 13, soft vest 12, half helmet 9, military vest 6,
     * combat helmet 3, G21 25, P26 12, STAR 10, W16A 5, M20 3, AW3 2.
     */
    private EntityObject rollItem() {
        int roll = MathUtils.randomRange(1, 100);
        Match match = getMatch();
        if (roll <= 13) return new Medkit(match);
        if (roll <= 25) return new SoftVest(match);
        if (roll <= 34) return new HalfHelmet(match);
        if (roll <= 40) return new MilitaryVest(match);
        if (roll <= 43) return new CombatHelmet(match);
        if (roll <= 68) return new G21(match);
        if (roll <= 80) return new P26(match);
        if (roll <= 90) return new STAR(match);
        if (roll <= 95) return new W16A(match);
        if (roll <= 98) return new M20(match);
        return new AW3(match);
    }
}
