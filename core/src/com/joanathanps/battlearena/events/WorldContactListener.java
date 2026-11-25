package com.joanathanps.battlearena.events;

import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.Manifold;
import com.joanathanps.battlearena.audio.AudioManager;
import com.joanathanps.battlearena.entities.Enemy;
import com.joanathanps.battlearena.entities.Soldier;
import com.joanathanps.battlearena.entities.projectiles.Bullet;
import com.joanathanps.battlearena.entities.tiles.InteractiveTile;
import com.joanathanps.battlearena.scenes.Match;

import static com.joanathanps.battlearena.scheme.PhysicsAdapter.*;

/** Routes Box2D contacts to game logic: bullet hits, loot and building triggers, corpse looting. */
public class WorldContactListener implements ContactListener {

    private final Match match;

    public WorldContactListener(Match match) {
        this.match = match;
    }

    @Override
    public void beginContact(Contact contact) {
        handle(contact, true);
    }

    @Override
    public void endContact(Contact contact) {
        handle(contact, false);
    }

    private void handle(Contact contact, boolean begins) {
        Fixture a = contact.getFixtureA();
        Fixture b = contact.getFixtureB();
        short categoryA = a.getFilterData().categoryBits;
        short categoryB = b.getFilterData().categoryBits;

        if (categoryA == BULLET_TAG || categoryB == BULLET_TAG) {
            if (begins) {
                Fixture bullet = categoryA == BULLET_TAG ? a : b;
                Fixture other = bullet == a ? b : a;
                onBulletHit((Bullet) bullet.getUserData(), other);
            }
            return;
        }
        if (categoryA == PLAYER_TAG || categoryB == PLAYER_TAG) {
            Fixture other = categoryA == PLAYER_TAG ? b : a;
            onPlayerTouch(other, begins);
            return;
        }
        if (categoryA == ENEMY_TAG && categoryB == ENEMY_TAG) {
            if (begins) {
                ((Enemy) a.getUserData()).onEnemyEnter((Enemy) b.getUserData());
                ((Enemy) b.getUserData()).onEnemyEnter((Enemy) a.getUserData());
            }
            return;
        }
        if ((categoryA == ENEMY_TAG && categoryB == LOOT_TAG) || (categoryA == LOOT_TAG && categoryB == ENEMY_TAG)) {
            Fixture enemy = categoryA == ENEMY_TAG ? a : b;
            Fixture loot = enemy == a ? b : a;
            if (begins) {
                ((InteractiveTile) loot.getUserData()).onEnemyEnter((Enemy) enemy.getUserData());
            }
        }
    }

    private void onBulletHit(Bullet bullet, Fixture other) {
        if (bullet.isSpent()) {
            return;
        }
        short category = other.getFilterData().categoryBits;
        if (category == PLAYER_TAG || category == ENEMY_TAG) {
            Soldier target = category == PLAYER_TAG ? match.getPlayer() : (Soldier) other.getUserData();
            if (target == bullet.getShooter() || target.isDead()) {
                return;
            }
            bullet.spend();
            target.takeDamage(bullet.getDamage(), bullet.getShooter());
            AudioManager.playHit();
        } else if (!other.isSensor()) {
            bullet.spend();
        }
    }

    private void onPlayerTouch(Fixture other, boolean begins) {
        short category = other.getFilterData().categoryBits;
        if (category == BUILDING_TAG || category == LOOT_TAG) {
            InteractiveTile tile = (InteractiveTile) other.getUserData();
            if (begins) {
                tile.onPlayerEnter();
            } else {
                tile.onPlayerLeave();
            }
        } else if (category == ENEMY_TAG) {
            Enemy enemy = (Enemy) other.getUserData();
            if (begins) {
                enemy.onPlayerEnter();
            } else {
                enemy.onPlayerLeave();
            }
        }
    }

    @Override
    public void preSolve(Contact contact, Manifold oldManifold) {
    }

    @Override
    public void postSolve(Contact contact, ContactImpulse impulse) {
    }
}
