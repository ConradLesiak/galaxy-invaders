package com.rgs.galaxyinvaders.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/**
 * Touch controls:
 * - Joystick appears on first touch on LEFT half; invisible otherwise.
 * - Any touch on RIGHT half triggers firing (no button is drawn).
 */
public class TouchControls {

    // Joystick geometry (only drawn while active)
    private final Vector2 joyCenter = new Vector2(120f, 100f);
    private final float joyOuterR = 56f;
    private final float joyKnobR  = 24f;
    private final Vector2 knobPos = new Vector2(joyCenter);

    private int joyPointer  = -1;
    private boolean joystickActive = false;

    private float moveAxis = 0f;   // -1..1
    private boolean firing = false;

    // Deadzone to prevent drift on small movements
    private final float dead = 0.12f;

    public void updateFromInput(Viewport viewport) {
        moveAxis = 0f;         // recompute each frame from active pointer
        firing   = false;      // recompute based on right-side touches this frame

        float worldW = viewport.getWorldWidth();

        // Scan all pointers for left/right touches
        for (int p = 0; p < 20; p++) {
            if (!Gdx.input.isTouched(p)) continue;

            Vector2 world = viewport.unproject(new Vector2(Gdx.input.getX(p), Gdx.input.getY(p)));

            // LEFT half: capture joystick pointer if none yet
            if (world.x < worldW * 0.5f) {
                if (joyPointer == -1) {
                    joyPointer = p;
                    joyCenter.set(world);   // dynamic re-center on first engage
                    knobPos.set(world);
                    joystickActive = true;  // now visible
                }
            } else {
                // RIGHT half = FIRE (invisible big button)
                firing = true;
            }
        }

        // Update joystick axis from its pointer (if still held)
        if (joyPointer != -1 && Gdx.input.isTouched(joyPointer)) {
            Vector2 w = viewport.unproject(new Vector2(Gdx.input.getX(joyPointer), Gdx.input.getY(joyPointer)));
            float dx = clamp(w.x - joyCenter.x, -joyOuterR, joyOuterR);
            knobPos.set(joyCenter.x + dx, joyCenter.y);
            float raw = dx / joyOuterR; // -1..1
            moveAxis = (Math.abs(raw) < dead) ? 0f : raw;
        } else {
            // released this frame
            joyPointer = -1;
            joystickActive = false; // hide when not touched
            knobPos.set(joyCenter); // reset for next time
            moveAxis = 0f;
        }
    }

    public float getMoveAxis() { return moveAxis; }     // -1..1
    public boolean isFiring()  { return firing; }

    /** Draw only the joystick (and only while active). No fire button is rendered. */
    public void render(ShapeRenderer sr) {
        if (!joystickActive) return;

        sr.begin(ShapeRenderer.ShapeType.Filled);
        // Base (semi-transparent)
        sr.setColor(0.2f, 0.2f, 0.25f, 0.45f);
        sr.circle(joyCenter.x, joyCenter.y, joyOuterR, 28);
        // Knob (bright)
        sr.setColor(0.9f, 0.9f, 1f, 0.85f);
        sr.circle(knobPos.x, knobPos.y, joyKnobR, 24);
        sr.end();
    }

    private static float clamp(float v, float lo, float hi) { return v < lo ? lo : (v > hi ? hi : v); }
}
