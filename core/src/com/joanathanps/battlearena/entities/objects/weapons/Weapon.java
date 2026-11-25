package com.joanathanps.battlearena.entities.objects.weapons;

import com.badlogic.gdx.math.Vector2;
import com.joanathanps.battlearena.audio.AudioManager;
import com.joanathanps.battlearena.entities.Soldier;
import com.joanathanps.battlearena.entities.objects.EntityObject;
import com.joanathanps.battlearena.entities.projectiles.Bullet;
import com.joanathanps.battlearena.scenes.Match;

/**
 * A gun with a magazine and unlimited reserve ammo. Fire rate is a cooldown ticked by the
 * owning inventory, so it pauses with the match.
 */
public abstract class Weapon extends EntityObject {

    private int damage;
    private float secondsBetweenShots;
    private float reloadSeconds;
    private int magCapacity;
    private int bullets;
    private float cooldown;

    public Weapon(Match match) {
        super(match);
    }

    /** Reloading finishes by filling the magazine. */
    @Override
    public boolean transformSoldier(Soldier soldier) {
        fillMagazine();
        return true;
    }

    public void fillMagazine() {
        bullets = magCapacity;
    }

    public void tick(float delta) {
        if (cooldown > 0) {
            cooldown -= delta;
        }
    }

    /** Fires one bullet if the magazine has one and the cooldown has passed. */
    public boolean shoot(Soldier soldier) {
        if (bullets <= 0 || cooldown > 0) {
            return false;
        }
        cooldown = secondsBetweenShots;
        bullets--;
        getMatch().addBullet(new Bullet(getMatch(), this, soldier));
        soldier.flashMuzzle();

        Soldier player = getMatch().getPlayer();
        Vector2 listener = player != null ? player.getBody().getPosition() : null;
        AudioManager.playShoot(this, soldier == player, soldier.getBody().getPosition(), listener);
        if (getMatch().getHud() != null) {
            getMatch().getHud().recordGunshot(soldier.getBody().getPosition());
        }
        return true;
    }

    /** How far a bullet travels before it drops, in world units. */
    public abstract float getRange();

    protected void setDamage(int damage) {
        this.damage = damage;
    }

    protected void setSecondsBetweenShots(float seconds) {
        this.secondsBetweenShots = seconds;
    }

    protected void setReloadSeconds(float seconds) {
        this.reloadSeconds = seconds;
    }

    protected void setMagCapacity(int magCapacity) {
        this.magCapacity = magCapacity;
    }

    public void setBullets(int bullets) {
        this.bullets = bullets;
    }

    public int getBullets() {
        return bullets;
    }

    public int getMagCapacity() {
        return magCapacity;
    }

    public int getDamage() {
        return damage;
    }

    public float getSecondsBetweenShots() {
        return secondsBetweenShots;
    }

    public float getReloadSeconds() {
        return reloadSeconds;
    }
}
