package com.rgs.galaxyinvaders.entities;

import com.badlogic.gdx.math.Rectangle;
import com.rgs.galaxyinvaders.config.Constants;

public class Bullet {
    public float x,y,w,h,vy,vx;

    public Bullet set(float x, float y, float w, float h, float vy) {
        this.x=x; this.y=y; this.w=w; this.h=h; this.vy=vy; this.vx=0f; return this;
    }
    public Rectangle rect() {
        float sw = w * Constants.BULLET_HITBOX_SCALE;
        float sh = h * Constants.BULLET_HITBOX_SCALE;
        float sx = x + (w - sw)/2f;
        float sy = y + (h - sh)/2f;
        return new Rectangle(sx, sy, sw, sh);
    }

    public float cx() { return x + w/2f; }
    public float cy() { return y + h/2f; }
    public float radius() { return w/2f; }
}
