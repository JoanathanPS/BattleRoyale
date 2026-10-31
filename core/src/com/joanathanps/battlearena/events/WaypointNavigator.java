package com.joanathanps.battlearena.events;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.RayCastCallback;
import com.badlogic.gdx.physics.box2d.World;

import java.util.ArrayList;

import static com.joanathanps.battlearena.scheme.PhysicsAdapter.BUILDING_TAG;

/**
 * Lightweight waypoint-based navigator for AI bots.
 *
 * <p>Instead of steering straight toward a target (which causes bots to
 * walk through building walls), this navigator:
 * <ol>
 *   <li>Raycasts from the bot to the target.</li>
 *   <li>If the path is clear, returns the target directly.</li>
 *   <li>If a wall is hit, finds the closest registered building
 *       entry-point that is roughly on the way to the target and
 *       returns that as the next step instead.</li>
 * </ol>
 *
 * <p>Building entry-points are registered at map-load time by
 * {@link com.joanathanps.battlearena.forge.WorldBuilder}.
 */
public class WaypointNavigator {

    /** Minimum distance before we consider a waypoint "reached". */
    private static final float WAYPOINT_REACH_DIST = 1.5f;

    private final World world;
    private final ArrayList<NavigationWaypoint> waypoints;

    public WaypointNavigator(World world) {
        this.world = world;
        this.waypoints = new ArrayList<>();
    }

    // -----------------------------------------------------------------------
    // Registration
    // -----------------------------------------------------------------------

    /** Register a single entry-point waypoint. */
    public void addWaypoint(NavigationWaypoint waypoint) {
        waypoints.add(waypoint);
    }

    /** Register multiple entry-point waypoints at once. */
    public void addWaypoints(ArrayList<NavigationWaypoint> points) {
        waypoints.addAll(points);
    }

    // -----------------------------------------------------------------------
    // Navigation
    // -----------------------------------------------------------------------

    /**
     * Returns the position the bot should move toward next.
     *
     * <p>If the straight-line path from {@code from} to {@code to} is
     * unobstructed, returns {@code to} so the bot moves directly there.
     * Otherwise returns the nearest registered waypoint that lies
     * between {@code from} and {@code to}, giving the bot a door/opening
     * to aim for first.
     *
     * @param from  current bot position (Box2D world units)
     * @param to    ultimate target position (Box2D world units)
     * @return      the next position to {@code seek()} toward
     */
    public Vector2 getNextWaypoint(Vector2 from, Vector2 to) {
        if (from == null || to == null) return to;

        // Short-circuit: already close to target
        if (from.dst(to) < WAYPOINT_REACH_DIST) return to;

        // If the direct path is clear, no detour needed
        if (hasLineOfSight(from, to)) return to;

        // Path is blocked — find the best entry-point waypoint
        NavigationWaypoint best = findBestWaypoint(from, to);
        if (best != null) {
            // If the bot has already reached the best waypoint, fall
            // through and try the target directly (we are at the door)
            if (from.dst(best.getPosition()) < WAYPOINT_REACH_DIST) {
                return to;
            }
            return best.getPosition();
        }

        // No waypoints registered or none helpful — fall back to target
        return to;
    }

    /**
     * Finds the waypoint that minimises: distance(bot?waypoint) +
     * distance(waypoint?target), i.e. the waypoint that adds the least
     * overhead to the overall journey.  We also require that the waypoint
     * is roughly in the direction of the target (dot-product filter) to
     * avoid the bot doubling back.
     */
    private NavigationWaypoint findBestWaypoint(Vector2 from, Vector2 to) {
        NavigationWaypoint best = null;
        float bestCost = Float.MAX_VALUE;

        // Direction vector from bot to target (normalised)
        Vector2 toDir = new Vector2(to).sub(from).nor();

        for (NavigationWaypoint wp : waypoints) {
            Vector2 wpPos = wp.getPosition();

            // Dot-product check: skip waypoints that are behind us
            Vector2 toWp = new Vector2(wpPos).sub(from).nor();
            float dot = toDir.dot(toWp);
            if (dot < -0.2f) continue; // more than ~100° off — skip

            float cost = from.dst(wpPos) + wpPos.dst(to);
            if (cost < bestCost) {
                bestCost = cost;
                best = wp;
            }
        }

        return best;
    }

    /**
     * Raycasts from {@code from} to {@code to} and returns {@code true}
     * if no building fixture is in between.
     */
    public boolean hasLineOfSight(Vector2 from, Vector2 to) {
        if (from.dst(to) < 0.01f) return true;

        final boolean[] blocked = {false};
        world.rayCast(new RayCastCallback() {
            @Override
            public float reportRayFixture(Fixture fixture, Vector2 point,
                                          Vector2 normal, float fraction) {
                if ((fixture.getFilterData().categoryBits & BUILDING_TAG) != 0) {
                    blocked[0] = true;
                    return 0; // stop ray
                }
                return -1; // ignore, continue
            }
        }, from, to);

        return !blocked[0];
    }

    // -----------------------------------------------------------------------
    // Accessors
    // -----------------------------------------------------------------------

    public ArrayList<NavigationWaypoint> getWaypoints() {
        return waypoints;
    }
}
