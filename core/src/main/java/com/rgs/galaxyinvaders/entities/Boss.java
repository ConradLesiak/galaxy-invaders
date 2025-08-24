package com.rgs.galaxyinvaders.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.rgs.galaxyinvaders.assets.Assets;
import com.rgs.galaxyinvaders.config.Constants;
import com.rgs.galaxyinvaders.world.GameWorld;

public class Boss extends GameObject {
    private final GameWorld world;
    private final int level;           // 1 = first boss
    private int hp, maxHp;
    private float t = 0f, dir = 1f;
    private float fireTimer = 0f;
    private float hitTimer = 0f;
    private final float diffBossFire;
    private final TextureRegion white; // 1x1 texture for UI bars

    public Boss(GameWorld world, Assets assets, int level, float diffBossHP, float diffBossFire) {
        this.world = world;
        this.level = level;
        this.diffBossFire = diffBossFire;

        // Sprite for the boss
        this.sprite = (assets.bossShip != null) ? assets.bossShip : assets.white1x1;
        float targetW = 140f;
        float scale = targetW / sprite.getRegionWidth();
        this.w = sprite.getRegionWidth() * scale;
        this.h = sprite.getRegionHeight() * scale;
        this.x = (Constants.W / 2f) - 60f;
        this.y = Constants.H - 120f;

        // 1x1 white for drawing HP bars
        this.white = assets.white1x1;

        // Base HP (halved) then scaled
        int base = 120 + 30;
        this.hp = Math.max(1, Math.round((base / 2f) * diffBossHP));
        this.maxHp = this.hp;
    }

    @Override
    public void update(float dt) {
        t += dt;
        if (hitTimer > 0f) hitTimer -= dt;

        // Simple horizontal sweep
        x += dir * 100f * dt;
        if (x < 20 || x + w > Constants.W - 20) dir *= -1f;

        // Obey global bullet cap
        if (world.bossBulletCapReached()) {
            fireTimer = Math.max(fireTimer, 0.15f);
            fireTimer -= dt;
            return;
        }

        // Pattern slots: boss1=2, boss2+=3 (aimed + fan [+ spiral])
        int patternSlots = Math.min(2 + (level - 1), 3);
        boolean allowAimed  = true;
        boolean allowFan    = patternSlots >= 2;
        boolean allowSpiral = patternSlots >= 3;

        // Projectile counts (boss1 stays 2/5)
        int earlyVolley = (level == 1) ? 2 : Math.min(2 + level, 6);
        int midFan      = (level == 1) ? 5 : Math.min(5 + (level - 1), 10);
        int lateSpiral  = (level == 1) ? 5 : Math.min(6 + (level - 1), 12);

        // Global slower cadence
        float intervalScale = Constants.ATTACK_SLOW_FACTOR / Math.max(1f, diffBossFire);

        float hpPct = (float) hp / maxHp;
        fireTimer -= dt;
        if (fireTimer <= 0f) {
            if (hpPct > 0.66f) {
                if (allowAimed) aimedVolley(earlyVolley, 140f);
                else if (allowFan) fan(-45, 45, earlyVolley, 160f);
                fireTimer = 1.4f * intervalScale;
            } else if (hpPct > 0.33f) {
                if (allowFan) fan(-45, 45, midFan, 180f);
                else if (allowAimed) aimedVolley(Math.max(2, midFan / 2), 150f);
                fireTimer = 1.2f * intervalScale;
            } else {
                if (allowSpiral)      spiral(lateSpiral, 160f);
                else if (allowFan)    fan(-50, 50, Math.max(5, lateSpiral), 170f);
                else if (allowAimed)  aimedVolley(Math.max(3, earlyVolley + 1), 160f);
                fireTimer = 1.0f * intervalScale;
            }
        }
    }

    private void aimedVolley(int count, float speed) {
        float cx = centerX(), cy = y;
        for (int i = 0; i < count; i++) {
            if (world.bossBulletCapReached()) break;
            Bullet b = new Bullet().set(cx - 4f, cy - 4f, 8f, 8f, -speed);
            float dx = world.getPlayer().centerX() - cx;
            float dy = world.getPlayer().centerY() - cy;
            float ang = (float) Math.atan2(dy, dx) + MathUtils.degreesToRadians * MathUtils.random(-Constants.BOSS_MISS_DEG, Constants.BOSS_MISS_DEG);
            b.vx = (float) Math.cos(ang) * 120f;
            world.getEnemyBullets().add(b);
        }
    }

    private void fan(float a0, float a1, int n, float speed) {
        float cx = centerX(), cy = y;
        for (int i = 0; i < n; i++) {
            if (world.bossBulletCapReached()) break;
            float a = MathUtils.lerp(a0, a1, i / (float) (n - 1));
            float rad = (a + 90f) * MathUtils.degreesToRadians;
            Bullet b = new Bullet().set(cx - 4f, cy - 4f, 8f, 8f, -speed);
            b.vx = (float) Math.cos(rad) * 160f;
            world.getEnemyBullets().add(b);
        }
    }

    private void spiral(int n, float speed) {
        float cx = centerX(), cy = y;
        for (int i = 0; i < n; i++) {
            if (world.bossBulletCapReached()) break;
            float a = (t * 180f + i * (360f / n)) * MathUtils.degreesToRadians;
            Bullet b = new Bullet().set(cx - 4f, cy - 4f, 8f, 8f, -speed);
            b.vx = (float) Math.cos(a) * 150f;
            world.getEnemyBullets().add(b);
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        // Boss sprite
        super.render(sb);

        // On-hit red overlay
        if (hitTimer > 0f) {
            float a = Math.min(0.6f, hitTimer / 0.12f);
            sb.setColor(1f, 0f, 0f, a);
            sb.draw(sprite, x, y, w, h);
            sb.setColor(Color.WHITE);
        }

        // HP bar (uses 1x1 white texture)
        if (white != null) {
            float pct = Math.max(0f, (float) hp / maxHp);
            float barW = 400f, barH = 8f;
            float barX = (Constants.W - barW) / 2f;
            float barY = Constants.H - 26f;

            sb.setColor(Color.DARK_GRAY);
            sb.draw(white, barX, barY, barW, barH);

            sb.setColor(Color.RED);
            sb.draw(white, barX, barY, barW * pct, barH);

            sb.setColor(Color.WHITE);
        }
    }

    // --- API ---
    public void damage(int d) { hp -= d; hitTimer = 0.12f; }
    public int getHp() { return hp; }
}
