package com.joanathanps.battlearena.entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.utils.Align;
import com.joanathanps.battlearena.entities.objects.Empty;
import com.joanathanps.battlearena.entities.objects.Inventory;
import com.joanathanps.battlearena.entities.objects.equipment.armor.Armor;
import com.joanathanps.battlearena.entities.objects.equipment.helmets.Helmet;
import com.joanathanps.battlearena.entities.projectiles.Bullet;
import com.joanathanps.battlearena.forge.BodyBuilder;
import com.joanathanps.battlearena.forge.WorldBuilder;
import com.joanathanps.battlearena.graphics.AnimationRegion;
import com.joanathanps.battlearena.graphics.Animator;
import com.joanathanps.battlearena.graphics.FontGenerator;
import com.joanathanps.battlearena.graphics.ResourceHandler;
import com.joanathanps.battlearena.scenes.Match;

import com.joanathanps.battlearena.scheme.PhysicsAdapter;
import static com.joanathanps.battlearena.scheme.PhysicsAdapter.*;

public abstract class Soldier extends Entity {

    private final float radius;
    private final float linearDamping;
    private int speed;
    private final short categoryBits;
    private final short maskBits;
    private final Object userData;
    private AnimationRegion animationRegion;
    private Object lastSelectedObjectClass;
    private Object lastEquippedArmorClass;
    private Object lastEquippedHelmetClass;

    private String name;

    public String getName() {
        return name;
    }

    protected static BitmapFont labelFont;
    protected static GlyphLayout labelGlyphLayout = new GlyphLayout();

    private Fixture fixture;

    private Animator animator;
    private int health = 100;
    private int armor = 0;
    private Inventory inventory;

    private Texture pointlight;

    private boolean isFiring;
    private int kills;

    private boolean isDead;

    private static final int MELEE_DAMAGE = 5;
    private static final float MELEE_RANGE = 1.5f;
    private static final float PUNCH_INTERVAL = 0.7f;
    private static final float PUNCH_SWING_TIME = 0.18f;
    private static final float PUNCH_IMPACT_TIME = 0.12f;
    private static final float PUNCH_SWEEP_START = -40f;
    private static final float PUNCH_SWEEP_END = 40f;
    private static Texture punchSwipeTexture;
    private Sprite punchSwipe;
    private Sprite punchImpact;
    private float punchCooldownCount;
    private float punchSwingCount;
    private float punchSwingAngle;
    private Soldier punchHitTarget;
    private float punchImpactCount;

    Soldier(Match match, Vector2 position, float radius, float linearDamping, int speed,
            AnimationRegion animationRegion, short categoryBits, short maskBits, Object userData,
            String name) {
        super(match, position, animationRegion, ResourceHandler.TextureAtlasPath.SOLDIER_ATLAS);

        this.name = name;
        this.radius = radius;
        this.linearDamping = linearDamping;
        this.speed = speed;
        this.animationRegion = animationRegion;
        this.categoryBits = categoryBits;
        this.userData = userData;
        this.maskBits = maskBits;
        init();
        buildBody();
    }

    protected void init() {
        animator = new Animator(this, animationRegion);
        setOrigin(pScaleCenter(animationRegion.getFrameWidth()), pScaleCenter(animationRegion.getFrameHeight()));
        setBounds(0, 0, pScale(animationRegion.getFrameWidth()), pScale(animationRegion.getFrameHeight()));
        setRegion(animator.getIdleTexture());
        pointlight = getMatch().getResources().getTexture(ResourceHandler.TexturePath.POINTLIGHT);
        if (punchSwipeTexture == null) {
            Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
            pixmap.setColor(1, 1, 1, 1);
            pixmap.fill();
            punchSwipeTexture = new Texture(pixmap);
            pixmap.dispose();
        }
        if (labelFont == null) {
            // Clean white text with minimal border and no shadow
            labelFont = FontGenerator.generateWithBorder(
                    ResourceHandler.FontPath.BOMBARD,
                    32,
                    1.0f,
                    new Color(0f, 0f, 0f, 0.9f),
                    0,
                    0,
                    Color.CLEAR
            );
            labelFont.setUseIntegerPositions(false);
            labelFont.getData().setScale(PhysicsAdapter.getScale() * 0.42f);
        }
        punchSwipe = new Sprite(punchSwipeTexture);
        punchImpact = new Sprite(punchSwipeTexture);
    }

    @Override
    protected void buildBody() {
        setBody(new BodyBuilder(getMatch().getWorld(), getPosition())
                .withBodyType(BodyDef.BodyType.DynamicBody)
                .withRadius(pScale(radius))
                .withLinearDamping(linearDamping)
                .withCategoryBits(categoryBits)
                .withMaskBits(maskBits)
                .withUserData(userData)
                .build());
        fixture = getBody().getFixtureList().first();
    }

    public void update(float delta) {
        setPosition(
                getBody().getPosition().x - pCenter(getWidth()),
                getBody().getPosition().y - pCenter(getHeight()));
        setRegion(animator.getFrame(delta, .7f));
        verifyItems();
        verifyHealth();
        if (punchCooldownCount > 0) {
            punchCooldownCount -= delta;
        }
        if (punchSwingCount > 0) {
            punchSwingCount -= delta;
        }
        if (punchImpactCount > 0) {
            punchImpactCount -= delta;
        }
    }

    protected abstract void verifyItems();

    private void verifyHealth() {
        if (health <= 0) {
            health = 0;
            die();
        }
    }

    protected abstract void die();

    public void takeDangerZoneDamage() {
        changeHealth(-5);
    }

    public void punch() {
        if (punchCooldownCount > 0) {
            return;
        }
        punchCooldownCount = PUNCH_INTERVAL;
        punchSwingCount = PUNCH_SWING_TIME;
        punchSwingAngle = getRotation() + 90;

        if (getMatch().getWorldBuilder() == null) {
            return;
        }
        Soldier nearest = null;
        float nearestDistance = MELEE_RANGE;
        Vector2 position = getBody().getPosition();
        for (Soldier combatant : getMatch().getWorldBuilder().getSoldiers()) {
            if (combatant == this || combatant.isDead()) {
                continue;
            }
            float distance = position.dst(combatant.getBody().getPosition());
            if (distance <= nearestDistance) {
                nearestDistance = distance;
                nearest = combatant;
            }
        }
        if (nearest != null) {
            nearest.takeMeleeDamage(MELEE_DAMAGE, this);
            punchHitTarget = nearest;
            punchImpactCount = PUNCH_IMPACT_TIME;
        }
    }

    public void takeMeleeDamage(int damage, Soldier attacker) {
        if (armor > 0) {
            if (getInventory().getEquipmentItems().get(0).getClass() != Empty.class) {
                ((Helmet)getInventory().getEquipmentItems().get(0)).takeDamage(damage / 2);
                ((Helmet)getInventory().getEquipmentItems().get(0)).updateName();
            }
            if (getInventory().getEquipmentItems().get(1).getClass() != Empty.class) {
                ((Armor)getInventory().getEquipmentItems().get(1)).takeDamage(damage / 2);
                ((Armor)getInventory().getEquipmentItems().get(1)).updateName();
            }
        } else {
            health -= damage;
            if (health <= 0) {
                attacker.accountKill();
                Gdx.app.log("COMBAT", (attacker instanceof Player ? "PLAYER" : "BOT")
                        + " KILLED " + (this instanceof Player ? "PLAYER" : "BOT"));
            }
        }
    }

    @Override
    public void draw(Batch batch) {
        if (!isDead) {
            if (isFiring) {
                batch.draw(pointlight,
                        getBody().getPosition().x - pScaleCenter(pointlight.getWidth()),
                        getBody().getPosition().y - pScaleCenter(pointlight.getHeight()),
                        pScale(pointlight.getWidth()),
                        pScale(pointlight.getHeight()));
            }
            if (punchSwingCount > 0) {
                float p = 1f - (punchSwingCount / PUNCH_SWING_TIME);
                float sweepDeg = PUNCH_SWEEP_START + (PUNCH_SWEEP_END - PUNCH_SWEEP_START) * p;
                float swingRad = (punchSwingAngle + sweepDeg) * MathUtils.degreesToRadians;
                float dirX = MathUtils.cos(swingRad);
                float dirY = MathUtils.sin(swingRad);
                float reach = MELEE_RANGE * Math.min(1f, p * 2.2f);
                punchSwipe.setSize(pScale(reach * 1.35f), pScale(reach * 0.32f));
                punchSwipe.setOrigin(punchSwipe.getWidth() / 2, punchSwipe.getHeight() / 2);
                punchSwipe.setCenter(
                        getBody().getPosition().x + dirX * (reach * 0.62f),
                        getBody().getPosition().y + dirY * (reach * 0.62f));
                punchSwipe.setRotation(punchSwingAngle + sweepDeg);
                punchSwipe.setColor(0.95f, 0.9f, 0.5f, 1f - p);
                punchSwipe.draw(batch);
            }
            if (punchImpactCount > 0 && punchHitTarget != null && !punchHitTarget.isDead()) {
                float p = 1f - (punchImpactCount / PUNCH_IMPACT_TIME);
                float radius = pScale(0.45f * (0.5f + p * 0.8f));
                punchImpact.setSize(radius * 2, radius * 2);
                punchImpact.setOrigin(radius, radius);
                punchImpact.setCenter(punchHitTarget.getBody().getPosition().x,
                        punchHitTarget.getBody().getPosition().y);
                punchImpact.setRotation(0);
                punchImpact.setColor(1f, 0.95f, 0.65f, 1f - p);
                punchImpact.draw(batch);
            }
            super.draw(batch);
            if (name != null && labelFont != null) {
                labelGlyphLayout.setText(labelFont, name);
                float textX = getX() + (getWidth() - labelGlyphLayout.width) / 2f;
                float textY = getY() + getHeight() + labelGlyphLayout.height + 0.15f;

                labelFont.setColor(Color.WHITE);
                labelFont.draw(batch, name, textX, textY);
            }
        }
    }

    public void setFiring(boolean firing) {
        isFiring = firing;
    }

    public void switchAnimation(AnimationRegion animationRegion, ResourceHandler.TextureAtlasPath textureAtlasPath) {
        this.animationRegion = animationRegion;
        setRegion(getMatch().getResources().getTextureAtlas(textureAtlasPath).findRegion(animationRegion.getRegion()));
        animator = new Animator(this, animationRegion);
        setBounds(0, 0, pScale(animationRegion.getFrameWidth()), pScale(animationRegion.getFrameHeight()));
        setOrigin(pScaleCenter(animationRegion.getFrameWidth()), pScaleCenter(animationRegion.getFrameHeight()));
    }

    public void changeHealth(int difference) {
        health = health + difference < 0 ? 0 : health + difference > 100 ? 100 : health + difference;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public int getSpeed() {
        return speed;
    }

    public int getHealth() {
        return health;
    }

    public int getArmor() {
        return armor;
    }

    public void setArmor(int armor) {
        this.armor = armor;
    }

    public void setSpeed(int speed) {
        this.speed = speed;
    }

    public Fixture getFixture() {
        return fixture;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public Object getLastSelectedObjectClass() {
        return lastSelectedObjectClass;
    }

    public Object getLastEquippedArmorClass() {
        return lastEquippedArmorClass;
    }

    public Object getLastEquippedHelmetClass() {
        return lastEquippedHelmetClass;
    }

    public void setLastSelectedObjectClass(Object lastSelectedObjectClass) {
        this.lastSelectedObjectClass = lastSelectedObjectClass;
    }

    public void setLastEquippedArmorClass(Object lastEquippedArmorClass) {
        this.lastEquippedArmorClass = lastEquippedArmorClass;
    }

    public void setLastEquippedHelmetClass(Object lastEquippedHelmetClass) {
        this.lastEquippedHelmetClass = lastEquippedHelmetClass;
    }

    public Animator getAnimator() {
        return animator;
    }

    public void getShot(Bullet bullet) {
        if (armor > 0) {
            if (getInventory().getEquipmentItems().get(0).getClass() != Empty.class) {
                ((Helmet)getInventory().getEquipmentItems().get(0)).takeDamage(bullet.getWeapon().getDamage() / 2);
                ((Helmet)getInventory().getEquipmentItems().get(0)).updateName();
            }
            if (getInventory().getEquipmentItems().get(1).getClass() != Empty.class) {
                ((Armor)getInventory().getEquipmentItems().get(1)).takeDamage(bullet.getWeapon().getDamage() / 2);
                ((Armor)getInventory().getEquipmentItems().get(1)).updateName();
            }
        } else {
            health -= bullet.getWeapon().getDamage();
            if (health <= 0) {
                Soldier killer = bullet.getWeapon().getLastSoldierToShoot();
                killer.accountKill();
                Gdx.app.log("COMBAT", (killer instanceof Player ? "PLAYER" : "BOT")
                        + " KILLED " + (this instanceof Player ? "PLAYER" : "BOT"));
            }
        }
    }

    public void setDead(boolean dead) {
        isDead = dead;
    }

    public boolean isDead() {
        return isDead;
    }

    private void accountKill() {
        kills++;
    }

    public int getKills() {
        return kills;
    }
}
