package com.joanathanps.battlearena.events;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.Viewport;

/**
 * Match input. Keeps two views of every action:
 * held (true while the key is down) and pressed (true for the frame after a key-down, even if
 * the key was released before that frame ran). Call {@link #endFrame()} once per frame after
 * everything has read the input, so a quick tap is never lost.
 */
public final class InputTracker extends InputAdapter {

    public static final int UP = 0;
    public static final int DOWN = 1;
    public static final int LEFT = 2;
    public static final int RIGHT = 3;
    public static final int ESC = 4;
    public static final int M = 5;
    public static final int E = 6;
    public static final int NUM_1 = 7;
    public static final int NUM_2 = 8;
    public static final int NUM_3 = 9;
    public static final int NUM_4 = 10;
    public static final int NUM_5 = 11;
    public static final int NUM_6 = 12;
    public static final int R = 13;
    private static final int COUNT = 14;

    private static final boolean[] held = new boolean[COUNT];
    private static final boolean[] pressed = new boolean[COUNT];
    private static final Vector2 mouseWorld = new Vector2();

    private final Viewport viewport;
    private int scroll;

    public InputTracker(Viewport viewport) {
        this.viewport = viewport;
        resetAllKeys();
    }

    private static int actionFor(int keycode) {
        switch (keycode) {
            case Input.Keys.W: return UP;
            case Input.Keys.S: return DOWN;
            case Input.Keys.A: return LEFT;
            case Input.Keys.D: return RIGHT;
            case Input.Keys.ESCAPE: return ESC;
            case Input.Keys.M: return M;
            case Input.Keys.E: return E;
            case Input.Keys.NUM_1: return NUM_1;
            case Input.Keys.NUM_2: return NUM_2;
            case Input.Keys.NUM_3: return NUM_3;
            case Input.Keys.NUM_4: return NUM_4;
            case Input.Keys.NUM_5: return NUM_5;
            case Input.Keys.NUM_6: return NUM_6;
            case Input.Keys.R: return R;
            default: return -1;
        }
    }

    @Override
    public boolean keyDown(int keycode) {
        int action = actionFor(keycode);
        if (action < 0) {
            return false;
        }
        held[action] = true;
        pressed[action] = true;
        return true;
    }

    @Override
    public boolean keyUp(int keycode) {
        int action = actionFor(keycode);
        if (action < 0) {
            return false;
        }
        held[action] = false;
        return true;
    }

    @Override
    public boolean scrolled(int amount) {
        scroll += amount;
        return true;
    }

    /** True while the key is down. */
    public static boolean isHeld(int action) {
        return held[action];
    }

    /** True once per key-down, until the end of the frame that reads it. */
    public static boolean wasPressed(int action) {
        return pressed[action];
    }

    public static boolean isShiftHeld() {
        return Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT);
    }

    /** Cursor offset from the window centre in window pixels, x to the right and y downwards. */
    public static float getRelativeX() {
        return Gdx.input.getX() - Gdx.graphics.getWidth() / 2f;
    }

    public static float getRelativeY() {
        return Gdx.input.getY() - Gdx.graphics.getHeight() / 2f;
    }

    /** Cursor position in world units, refreshed by {@link #update()}. */
    public static Vector2 getMousePos() {
        return mouseWorld;
    }

    /** Scroll steps since the last frame: negative is up, positive is down. */
    public int getScroll() {
        return scroll;
    }

    public void update() {
        mouseWorld.set(Gdx.input.getX(), Gdx.input.getY());
        viewport.unproject(mouseWorld);
    }

    public void endFrame() {
        for (int i = 0; i < COUNT; i++) {
            pressed[i] = false;
        }
        scroll = 0;
    }

    public void resetAllKeys() {
        for (int i = 0; i < COUNT; i++) {
            held[i] = false;
            pressed[i] = false;
        }
        scroll = 0;
    }
}
