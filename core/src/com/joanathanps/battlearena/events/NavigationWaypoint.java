package com.joanathanps.battlearena.events;

import com.badlogic.gdx.math.Vector2;

/**
 * A named position in the world that the AI uses as an intermediate
 * routing point — typically a building entry/door location.
 */
public class NavigationWaypoint {

    private final String name;
    private final Vector2 position;

    public NavigationWaypoint(String name, Vector2 position) {
        this.name = name;
        this.position = new Vector2(position);
    }

    public String getName() {
        return name;
    }

    public Vector2 getPosition() {
        return position;
    }
}
