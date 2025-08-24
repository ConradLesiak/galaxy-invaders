package com.rgs.galaxyinvaders.fx;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.math.Matrix4;
import com.rgs.galaxyinvaders.config.Constants;

public class Starfield {
    static class Star { float x,y,spd,size; Color c; }
    private final Array<Star> stars = new Array<>();

    public Starfield(int count) {
        for (int i=0;i<count;i++) {
            Star s = new Star();
            s.x = MathUtils.random(0, Constants.W);
            s.y = MathUtils.random(0, Constants.H);
            s.spd = MathUtils.random(30f, 180f);
            s.size = MathUtils.random(1.2f, 2.8f);
            float g = MathUtils.random(0.6f, 1f);
            s.c = new Color(g, g, 1f, 1f);
            stars.add(s);
        }
    }

    public void update(float dt) {
        for (Star s : stars) {
            s.y -= s.spd * dt;
            if (s.y < 0) { s.y = Constants.H; s.x = MathUtils.random(0, Constants.W); }
        }
    }

    public void render(ShapeRenderer sr, float dt, Matrix4 proj) {
        update(dt);
        sr.setProjectionMatrix(proj);
        sr.begin(ShapeRenderer.ShapeType.Filled);
        for (Star s : stars) {
            sr.setColor(s.c);
            sr.circle(s.x, s.y, s.size, 8);
        }
        sr.end();
    }
}
