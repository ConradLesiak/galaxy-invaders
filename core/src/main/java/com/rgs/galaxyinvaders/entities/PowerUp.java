package com.rgs.galaxyinvaders.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;

public class PowerUp {
    private final PowerUpType type;
    private float x, y, size = 14f, vy = -65f;

    private PowerUp(PowerUpType t, float x, float y){ this.type=t; this.x=x; this.y=y; }

    public static PowerUp rapid(float x,float y){ return new PowerUp(PowerUpType.RAPID,x,y); }
    public static PowerUp spread(float x,float y){ return new PowerUp(PowerUpType.SPREAD,x,y); }
    public static PowerUp shield(float x,float y){ return new PowerUp(PowerUpType.SHIELD,x,y); }
    public static PowerUp life(float x,float y){ return new PowerUp(PowerUpType.LIFE,x,y); }

    public void update(float dt){ y += vy * dt; }
    public void apply(Player p){ p.applyPower(type); }

    public void render(ShapeRenderer sr){
        switch (type){
            case RAPID: sr.setColor(Color.YELLOW); break;
            case SPREAD: sr.setColor(Color.CYAN); break;
            case SHIELD: sr.setColor(Color.VIOLET); break;
            case LIFE: sr.setColor(Color.LIME); break;
        }
        sr.triangle(x, y + size, x - size, y, x + size, y);
        sr.triangle(x, y - size, x - size, y, x + size, y);
    }

    public Rectangle rect(){ return new Rectangle(x - size, y - size, size*2, size*2); }
    public float getY(){ return y; }
    public float getSize(){ return size; }
}
