package com.rgs.galaxyinvaders.entities;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.rgs.galaxyinvaders.world.GameWorld;

public class Enemy extends GameObject {
    private final GameWorld world;
    private final int type; // 0 sine, 1 shooter, 2 chaser
    private int hp = 1;
    private float t = 0f;
    private float speed = 60f;
    private float shootTimer = MathUtils.random(0.3f, 2f);
    private float reload = 1.1f;
    private float fireChance = 0.03f;
    private int scoreValue = 20;
    private float hitTimer = 0f;

    public Enemy(GameWorld world, TextureRegion sprite, int type, float cx, float y, int wave, float diffEnemySpeed, float diffEnemyFire) {
        this.world = world;
        this.type = type;
        this.sprite = sprite;
        float targetW = 28f;
        float scale = targetW / sprite.getRegionWidth();
        this.w = sprite.getRegionWidth() * scale;
        this.h = sprite.getRegionHeight() * scale;
        this.x = cx - w/2f;
        this.y = y;

        this.hp = (type==1 && wave>6) ? 2 : 1;
        this.speed = (45f + wave * 3f) * diffEnemySpeed;
        this.reload = (1.1f - Math.min(0.5f, wave * 0.02f)) / Math.max(0.6f, diffEnemyFire);
        this.fireChance = (0.03f + wave * 0.0015f) * diffEnemyFire;
        this.scoreValue = 20 + wave * 3;
        if (type==2) this.reload *= 0.85f;
    }

    @Override public void update(float dt) {
        t += dt; shootTimer -= dt; if (hitTimer > 0) hitTimer -= dt;
        switch (type) {
            case 0: y -= speed * dt; x += MathUtils.sin(t * 2.4f) * 80f * dt; break;
            case 1: y -= speed * 0.8f * dt; x += MathUtils.sin(t * 1.6f) * 50f * dt; break;
            case 2: float dir = Math.signum(world.getPlayer().centerX() - (x + w/2f));
                x += dir * (speed * 1.3f) * dt; y -= speed * 1.1f * dt; break;
        }
        x = MathUtils.clamp(x, 0, 800f - w);
    }

    @Override public void render(SpriteBatch sb) {
        super.render(sb);
        if (hitTimer > 0f) {
            float a = Math.min(0.6f, hitTimer / 0.12f);
            sb.setColor(1f, 0f, 0f, a);
            sb.draw(sprite, x, y, w, h);
            sb.setColor(com.badlogic.gdx.graphics.Color.WHITE);
        }
    }

    public boolean readyToShoot() { return shootTimer <= 0f; }
    public void resetShootTimer() { shootTimer = reload; }

    public void damage(int d) { hp -= d; hitTimer = 0.12f; }
    public boolean isDead() { return hp <= 0; }

    public float getFireChance() { return fireChance; }
    public int getScoreValue() { return scoreValue; }
}
