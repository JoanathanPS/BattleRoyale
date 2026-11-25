package com.joanathanps.battlearena.entities;

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
import com.joanathanps.battlearena.entities.objects.EntityObject;
import com.joanathanps.battlearena.entities.objects.Inventory;
import com.joanathanps.battlearena.entities.objects.equipment.armor.Armor;
import com.joanathanps.battlearena.entities.objects.equipment.helmets.Helmet;
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

    private static final float MUZZLE_FLASH_TIME = 0.05f;
    private float muzzleFlashTime;
    private int kills;

    private boolean isDead;
    private Soldier killer;

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
            // A dedicated size, because this font is scaled down to world units and fonts are shared.
            labelFont = FontGenerator.generateOutlined(ResourceHandler.FontPath.BOMBARD, 33, 2f);
            labelFont.setUseIntegerPositions(false);
            labelFont.getData().setScale(labelFont.getData().scaleX * PhysicsAdapter.getScale() * 0.5f);
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
        if (punchCooldownCount > 0) {
            punchCooldownCount -= delta;
        }
        if (punchSwingCount > 0) {
            punchSwingCount -= delta;
        }
        if (punchImpactCount > 0) {
            punchImpactCount -= delta;
        }
        if (muzzleFlashTime > 0) {
            muzzleFlashTime -= delta;
        }
    }

    protected abstract void verifyItems();

    protected abstract void die();

    /** Zone damage ignores armor, like in most battle royales. */
    public void takeZoneDamage(int amount) {
        applyDamage(amount, null, false);
    }

    /** Bullet and melee damage. Armor takes the hit first; whatever it cannot absorb reaches health. */
    public void takeDamage(int amount, Soldier attacker) {
        applyDamage(amount, attacker, true);
    }

    private void applyDamage(int amount, Soldier attacker, boolean armorCanAbsorb) {
        if (isDead || amount <= 0) {
            return;
        }
        int remaining = armorCanAbsorb ? absorbWithArmor(amount) : amount;
        health = Math.max(0, health - remaining);
        onDamaged(amount);
        if (health == 0) {
            isDead = true;
            killer = attacker;
            if (attacker != null && attacker != this) {
                attacker.kills++;
            }
            die();
            getMatch().onElimination(this, attacker);
        }
    }

    /** Hook for feedback when this soldier is hit, such as the player's red flash. */
    protected void onDamaged(int amount) {
    }

    /**
     * Splits the hit between helmet and vest, each capped by the points it has left,
     * removes pieces that reach zero and returns the damage that gets through.
     */
    private int absorbWithArmor(int amount) {
        EntityObject helmetItem = getInventory().getEquipmentItems().get(0);
        EntityObject vestItem = getInventory().getEquipmentItems().get(1);
        Helmet helmet = helmetItem instanceof Helmet ? (Helmet) helmetItem : null;
        Armor vest = vestItem instanceof Armor ? (Armor) vestItem : null;
        int helmetPoints = helmet != null ? Math.max(0, helmet.getArmorPoints()) : 0;
        int vestPoints = vest != null ? Math.max(0, vest.getArmorPoints()) : 0;

        int absorbed = Math.min(amount, helmetPoints + vestPoints);
        if (absorbed == 0) {
            return amount;
        }
        int fromVest = Math.min(vestPoints, (absorbed + 1) / 2);
        int fromHelmet = Math.min(helmetPoints, absorbed - fromVest);
        fromVest = absorbed - fromHelmet;

        if (helmet != null && fromHelmet > 0) {
            helmet.takeDamage(fromHelmet);
            helmet.updateName();
            if (helmet.getArmorPoints() <= 0) {
                getInventory().getEquipmentItems().set(0, new Empty(getMatch()));
            }
        }
        if (vest != null && fromVest > 0) {
            vest.takeDamage(fromVest);
            vest.updateName();
            if (vest.getArmorPoints() <= 0) {
                getInventory().getEquipmentItems().set(1, new Empty(getMatch()));
            }
        }
        armor = Math.max(0, armor - absorbed);
        return amount - absorbed;
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
            punchHitTarget = nearest;
            punchImpactCount = PUNCH_IMPACT_TIME;
            nearest.takeDamage(MELEE_DAMAGE, this);
        }
    }

    @Override
    public void draw(Batch batch) {
        if (!isDead) {
            if (muzzleFlashTime > 0) {
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

    public void flashMuzzle() {
        muzzleFlashTime = MUZZLE_FLASH_TIME;
    }

    public void switchAnimation(AnimationRegion animationRegion, ResourceHandler.TextureAtlasPath textureAtlasPath) {
        this.animationRegion = animationRegion;
        setRegion(getMatch().getResources().getTextureAtlas(textureAtlasPath).findRegion(animationRegion.getRegion()));
        animator = new Animator(this, animationRegion);
        setBounds(0, 0, pScale(animationRegion.getFrameWidth()), pScale(animationRegion.getFrameHeight()));
        setOrigin(pScaleCenter(animationRegion.getFrameWidth()), pScaleCenter(animationRegion.getFrameHeight()));
    }

    public void heal(int amount) {
        if (!isDead) {
            changeHealth(amount);
        }
    }

    private void changeHealth(int difference) {
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

    public boolean isDead() {
        return isDead;
    }

    /** Who eliminated this soldier, or null for the zone. */
    public Soldier getKiller() {
        return killer;
    }

    public int getKills() {
        return kills;
    }
}
