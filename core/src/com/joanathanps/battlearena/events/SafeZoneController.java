package com.joanathanps.battlearena.events;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.QueryCallback;
import com.joanathanps.battlearena.audio.AudioManager;
import com.joanathanps.battlearena.entities.Soldier;
import com.joanathanps.battlearena.scenes.Match;

import static com.joanathanps.battlearena.scheme.PhysicsAdapter.WATER_TAG;

/**
 * The shrinking safe zone.
 *
 * <p>The match runs through {@link #PHASES} phases. Each phase first announces the next circle
 * (WAITING), then moves and shrinks the current circle onto it (SHRINKING). Every circle lies
 * inside the previous one, the last one has radius zero, and anyone outside the current circle
 * loses health every second, more in later phases. Zone damage ignores armor.
 */
public class SafeZoneController {

    public enum Stage { WAITING, SHRINKING, CLOSED }

    /** World units. The island is roughly centred on the 82 x 82 map. */
    private static final float MAP_CENTER = 41f;
    private static final float START_RADIUS = 60f;

    private static final float[] RADII = {30.5f, 21f, 13f, 6.5f, 0f};
    private static final float[] WAIT_SECONDS = {35, 30, 25, 20, 15};
    private static final float[] SHRINK_SECONDS = {25, 20, 15, 12, 15};
    private static final int[] DAMAGE_PER_SECOND = {2, 3, 5, 8, 12};
    public static final int PHASES = RADII.length;

    private final Match match;
    private final Vector2[] centers = new Vector2[PHASES];

    private final Vector2 fromCenter = new Vector2(MAP_CENTER, MAP_CENTER);
    private float fromRadius = START_RADIUS;
    private final Vector2 center = new Vector2(MAP_CENTER, MAP_CENTER);
    private float radius = START_RADIUS;

    private int phase;
    private Stage stage = Stage.WAITING;
    private float stageTime;
    private float damageTick;

    public SafeZoneController(Match match) {
        this.match = match;
        planCircles();
    }

    /** Picks every circle up front: each one random inside the previous, preferring land. */
    private void planCircles() {
        Vector2 previous = new Vector2(MAP_CENTER, MAP_CENTER);
        float previousRadius = 12f;
        for (int i = 0; i < PHASES; i++) {
            float maxShift = Math.max(0f, (i == 0 ? previousRadius : RADII[i - 1] - RADII[i]) * 0.85f);
            Vector2 candidate = new Vector2(previous);
            for (int attempt = 0; attempt < 30; attempt++) {
                float angle = MathUtils.random(MathUtils.PI2);
                float distance = maxShift * (float) Math.sqrt(MathUtils.random());
                candidate.set(previous.x + MathUtils.cos(angle) * distance,
                        previous.y + MathUtils.sin(angle) * distance);
                if (isOnLand(candidate)) {
                    break;
                }
                candidate.set(previous);
            }
            centers[i] = candidate;
            previous = candidate;
        }
    }

    private boolean isOnLand(final Vector2 point) {
        final boolean[] water = {false};
        match.getWorld().QueryAABB(new QueryCallback() {
            @Override
            public boolean reportFixture(Fixture fixture) {
                if ((fixture.getFilterData().categoryBits & WATER_TAG) != 0 && fixture.testPoint(point)) {
                    water[0] = true;
                    return false;
                }
                return true;
            }
        }, point.x - 0.01f, point.y - 0.01f, point.x + 0.01f, point.y + 0.01f);
        return !water[0];
    }

    public void update(float delta) {
        stageTime += delta;
        if (stage == Stage.WAITING && stageTime >= WAIT_SECONDS[phase]) {
            stage = Stage.SHRINKING;
            stageTime = 0;
            AudioManager.playZoneWarning();
        } else if (stage == Stage.SHRINKING) {
            float progress = Math.min(1f, stageTime / SHRINK_SECONDS[phase]);
            float eased = Interpolation.smooth.apply(progress);
            center.set(fromCenter).lerp(centers[phase], eased);
            radius = MathUtils.lerp(fromRadius, RADII[phase], eased);
            if (progress >= 1f) {
                fromCenter.set(centers[phase]);
                fromRadius = RADII[phase];
                stageTime = 0;
                if (phase < PHASES - 1) {
                    phase++;
                    stage = Stage.WAITING;
                } else {
                    stage = Stage.CLOSED;
                }
            }
        }
        dealDamage(delta);
    }

    private void dealDamage(float delta) {
        damageTick += delta;
        if (damageTick < 1f) {
            return;
        }
        damageTick -= 1f;
        int damage = DAMAGE_PER_SECOND[phase];
        for (Soldier soldier : match.getWorldBuilder().getSoldiers()) {
            if (!soldier.isDead() && isOutside(soldier.getBody().getPosition())) {
                soldier.takeZoneDamage(damage);
            }
        }
    }

    public boolean isOutside(Vector2 position) {
        return position.dst2(center) > radius * radius;
    }

    /** True if the position is outside the circle the zone is heading for. */
    public boolean isOutsideNext(Vector2 position) {
        if (stage == Stage.CLOSED) {
            return isOutside(position);
        }
        Vector2 next = centers[phase];
        return position.dst2(next) > RADII[phase] * RADII[phase];
    }

    /**
     * Draws the danger area as a red band from the safe circle outwards, the circle's edge,
     * and the outline of the next circle.
     */
    public void render(ShapeRenderer shapes, Matrix4 projection) {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.setProjectionMatrix(projection);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        drawRing(shapes, center.x, center.y, radius, radius + 140f, 0.78f, 0.17f, 0.13f, 0.3f);
        drawRing(shapes, center.x, center.y, radius - 0.08f, radius + 0.08f, 0.78f, 0.17f, 0.13f, 0.95f);
        if (hasNextCircle()) {
            Vector2 next = getNextCenter();
            float nextRadius = getNextRadius();
            drawRing(shapes, next.x, next.y, nextRadius - 0.05f, nextRadius + 0.05f, 1f, 1f, 1f, 0.85f);
        }
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    /** Filled band between two radii, built from quads so it works on WebGL too. */
    public static void drawRing(ShapeRenderer shapes, float cx, float cy, float inner, float outer,
                                float r, float g, float b, float a) {
        inner = Math.max(0f, inner);
        if (outer <= inner) {
            return;
        }
        shapes.setColor(r, g, b, a);
        int segments = Math.max(48, Math.min(720, (int) (outer * 8)));
        float step = MathUtils.PI2 / segments;
        for (int i = 0; i < segments; i++) {
            float a0 = i * step;
            float a1 = a0 + step;
            float c0 = MathUtils.cos(a0), s0 = MathUtils.sin(a0);
            float c1 = MathUtils.cos(a1), s1 = MathUtils.sin(a1);
            float ix0 = cx + c0 * inner, iy0 = cy + s0 * inner;
            float ix1 = cx + c1 * inner, iy1 = cy + s1 * inner;
            float ox0 = cx + c0 * outer, oy0 = cy + s0 * outer;
            float ox1 = cx + c1 * outer, oy1 = cy + s1 * outer;
            shapes.triangle(ix0, iy0, ox0, oy0, ox1, oy1);
            shapes.triangle(ix0, iy0, ox1, oy1, ix1, iy1);
        }
    }

    /** The next circle is shown while waiting for and during a shrink. */
    public boolean hasNextCircle() {
        return stage != Stage.CLOSED && RADII[phase] > 0;
    }

    public Vector2 getCenter() {
        return center;
    }

    public float getRadius() {
        return radius;
    }

    public Vector2 getNextCenter() {
        return centers[phase];
    }

    public float getNextRadius() {
        return RADII[phase];
    }

    public Stage getStage() {
        return stage;
    }

    /** 1-based phase number for display. */
    public int getPhaseNumber() {
        return phase + 1;
    }

    public int getDamagePerSecond() {
        return DAMAGE_PER_SECOND[phase];
    }

    /** Seconds until the current wait or shrink ends; 0 once the zone has closed. */
    public int getSecondsLeft() {
        if (stage == Stage.WAITING) {
            return (int) Math.ceil(WAIT_SECONDS[phase] - stageTime);
        }
        if (stage == Stage.SHRINKING) {
            return (int) Math.ceil(SHRINK_SECONDS[phase] - stageTime);
        }
        return 0;
    }

    /** Seconds until the zone starts moving again; used by bots to decide when to leave. */
    public float getSecondsUntilShrink() {
        return stage == Stage.WAITING ? WAIT_SECONDS[phase] - stageTime : 0f;
    }
}
