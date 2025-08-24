package com.rgs.galaxyinvaders.entities;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;

public abstract class GameObject {
    protected float x,y,w,h;
    protected TextureRegion sprite;

    public abstract void update(float dt);
    public void render(SpriteBatch sb) { if (sprite != null) sb.draw(sprite, x, y, w, h); }
    public Rectangle rect() { return new Rectangle(x, y, w, h); }

    // helpers
    public float centerX() { return x + w/2f; }
    public float centerY() { return y + h/2f; }
    public float getX() { return x; }
    public float getY() { return y; }
    public float getW() { return w; }
    public float getH() { return h; }
}
