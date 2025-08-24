package com.rgs.galaxyinvaders.entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.rgs.galaxyinvaders.assets.Assets;
import com.rgs.galaxyinvaders.world.GameWorld;

public class Player extends GameObject {
    private final GameWorld world;

    private float speed = 320f;
    private float cooldown = 0.22f, cdTimer = 0f;
    private int lives = 3;

    private float rapidTimer = 0f, spreadTimer = 0f, shieldTimer = 0f, blinkTimer = 0f;

    public Player(GameWorld world, Assets assets) {
        this.world = world;
        TextureRegion tr = assets.playerShip != null ? assets.playerShip : assets.white1x1;
        this.sprite = tr;
        float targetW = 36f;
        float scale = targetW / tr.getRegionWidth();
        this.w = tr.getRegionWidth() * scale;
        this.h = tr.getRegionHeight() * scale;
        this.x = (800f / 2f) - w/2f; // centered
        this.y = 40f;
    }

    @Override public void update(float dt) {
        float dir = 0f;
        if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) dir -= 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) dir += 1f;
        x += dir * speed * dt;
        x = MathUtils.clamp(x, 6, 800f - w - 6);

        cdTimer -= dt;
        boolean autoFire = rapidTimer > 0f;
        if ((Gdx.input.isKeyPressed(Input.Keys.SPACE) && cdTimer <= 0) || (autoFire && cdTimer <= 0)) {
            shoot();
            cdTimer = (rapidTimer > 0) ? 0.07f : cooldown;
        }

        if (rapidTimer > 0)  rapidTimer -= dt;
        if (spreadTimer > 0) spreadTimer -= dt;
        if (shieldTimer > 0) shieldTimer -= dt;
        if (blinkTimer  > 0) blinkTimer  -= dt;
    }

    private void shoot() {
        float cx = x + w/2f, top = y + h;
        world.firePlayer(cx, top);
        if (spreadTimer > 0f) {
            Bullet l = new Bullet().set(cx - 3.5f, top, 7f, 7f, 420f); l.vx = -120f;
            Bullet r = new Bullet().set(cx - 3.5f, top, 7f, 7f, 420f); r.vx =  120f;
            world.addPlayerBullet(l); world.addPlayerBullet(r);
        }
    }

    @Override public void render(SpriteBatch sb) {
        if (blinkTimer > 0 && ((int)(blinkTimer * 20) % 2 == 0)) return;
        super.render(sb);
    }

    public void applyPower(PowerUpType type) {
        switch (type) {
            case RAPID:  rapidTimer  = Math.max(rapidTimer, 8f); break;
            case SPREAD: spreadTimer = Math.max(spreadTimer, 8f); break;
            case SHIELD: shieldTimer = Math.max(shieldTimer, 10f); break;
            case LIFE:   lives = Math.min(lives + 1, 5); break;
        }
    }

    public boolean consumeShieldIfAny() { if (shieldTimer > 0) { shieldTimer = 0; return true; } return false; }
    public void loseLife() { lives--; }
    public void blink(float t) { blinkTimer = t; }

    public int getLives() { return lives; }
    public float getShieldTimer() { return shieldTimer; }
}
