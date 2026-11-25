package com.joanathanps.battlearena.entities.projectiles;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.joanathanps.battlearena.entities.Soldier;
import com.joanathanps.battlearena.entities.objects.weapons.Weapon;
import com.joanathanps.battlearena.forge.BodyBuilder;
import com.joanathanps.battlearena.graphics.ResourceHandler;
import com.joanathanps.battlearena.scenes.Match;
import com.joanathanps.battlearena.scheme.Difficulty;

import static com.joanathanps.battlearena.scheme.PhysicsAdapter.*;

/**
 * A sensor body that flies straight until it hits something or runs out of range.
 * Contacts only mark it as spent; {@link Match} removes its body after the physics step,
 * because Box2D does not allow destroying bodies inside a contact callback.
 */
public class Bullet extends Sprite {

    private static final float SPEED = 10f;
    /** Hide the sprite for its first moments so it appears at the muzzle, not inside the shooter. */
    private static final float HIDDEN_TIME = 0.05f;

    private final Match match;
    private final Soldier shooter;
    private final int damage;
    private final float lifetime;
    private final Body body;
    private final float directionX;
    private final float directionY;
    private float age;
    private boolean spent;

    public Bullet(Match match, Weapon weapon, Soldier shooter) {
        super(match.getResources().getTexture(ResourceHandler.TexturePath.BULLET));
        this.match = match;
        this.shooter = shooter;
        this.damage = weapon.getDamage();
        this.lifetime = weapon.getRange() / SPEED;

        setBounds(0, 0, pScale(getWidth()), pScale(getHeight()));
        setOrigin(pScaleCenter(getWidth()) + .1f, pScaleCenter(getHeight()) + .1f);
        setRotation(shooter.getRotation() + spreadFor(shooter));

        float angle = (getRotation() + 90) * MathUtils.degreesToRadians;
        directionX = MathUtils.cos(angle);
        directionY = MathUtils.sin(angle);

        body = new BodyBuilder(match.getWorld(), shooter.getBody().getPosition())
                .withBodyType(BodyDef.BodyType.DynamicBody)
                .withRadius(pScale(5))
                .withCategoryBits(BULLET_TAG)
                .withMaskBits((short) (DEFAULT_TAG | LOOT_TAG | PLAYER_TAG | ENEMY_TAG))
                .withSensor(true)
                .withUserData(this)
                .build();
        body.setBullet(true);
        body.setLinearVelocity(directionX * SPEED, directionY * SPEED);
    }

    /** Bots miss more on lower difficulties. The player's aim is never altered. */
    private float spreadFor(Soldier shooter) {
        if (shooter == match.getPlayer()) {
            return 0;
        }
        Difficulty difficulty = match.getDifficulty();
        if (MathUtils.random() < difficulty.getAccuracy()) {
            return 0;
        }
        float spreadDegrees = 10f + 30f * (1f - difficulty.getAccuracy());
        return MathUtils.random(-spreadDegrees, spreadDegrees);
    }

    public void update(float delta) {
        age += delta;
        if (age >= lifetime) {
            spent = true;
        }
        setPosition(body.getPosition().x - .1f, body.getPosition().y - .1f);
    }

    @Override
    public void draw(Batch batch) {
        if (age >= HIDDEN_TIME && !spent) {
            super.draw(batch);
        }
    }

    /** Called on impact. The body is removed by the match after the physics step. */
    public void spend() {
        spent = true;
    }

    public boolean isSpent() {
        return spent;
    }

    public Body getBody() {
        return body;
    }

    public Soldier getShooter() {
        return shooter;
    }

    public int getDamage() {
        return damage;
    }
}
