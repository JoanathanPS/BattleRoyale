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

    public void update(float delta) {
        // Player interacts via the NearbyLootWidget when in proximity
    }

    public boolean isPlayerColliding() {
        return playerIsColliding;
    }

    public ArrayList<EntityObject> getItems() {
        return items;
    }

    public boolean hasItems() {
        for (EntityObject item : items) {
            if (!(item instanceof com.joanathanps.battlearena.entities.objects.Empty)) {
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

    public EntityObject getRandomItem() {
        return rollItem();
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

    private EntityObject rollItem() {
        int randomNumber = MathUtils.randomRange(1, 100);

        if (randomNumber <= 13 ) {
            Medkit item = new Medkit(getMatch());
            item.setRarity(1);
            return item;
        } else if (randomNumber <= 25 ) {
            SoftVest item = new SoftVest(getMatch());
            item.setRarity(2);
            return item;
        } else if (randomNumber <= 34 ) {
            HalfHelmet item = new HalfHelmet(getMatch());
            item.setRarity(2);
            return item;
        } else if (randomNumber <= 40 ) {
            MilitaryVest item = new MilitaryVest(getMatch());
            item.setRarity(3);
            return item;
        } else if (randomNumber <= 43 ) {
            CombatHelmet item = new CombatHelmet(getMatch());
            item.setRarity(4);
            return item;
        } else if (randomNumber <= 68 ) {
            G21 item = new G21(getMatch());
            item.setRarity(5);
            return item;
        } else if (randomNumber <= 80 ) {
            P26 item = new P26(getMatch());
            item.setRarity(5);
            return item;
        } else if (randomNumber <= 90 ) {
            STAR item = new STAR(getMatch());
            item.setRarity(5);
            return item;
        } else if (randomNumber <= 95 ) {
            W16A item = new W16A(getMatch());
            item.setRarity(5);
            return item;
        } else if (randomNumber <= 98 ) {
            M20 item = new M20(getMatch());
            item.setRarity(5);
            return item;
        } else if (randomNumber <= 100 ) {
            AW3 item = new AW3(getMatch());
            item.setRarity(6);
            return item;
        }

        return null;
    }
}
