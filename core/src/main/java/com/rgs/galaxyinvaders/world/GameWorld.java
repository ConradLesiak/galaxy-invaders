package com.rgs.galaxyinvaders.world;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import com.rgs.galaxyinvaders.GalaxyInvadersGame;
import com.rgs.galaxyinvaders.assets.Assets;
import com.rgs.galaxyinvaders.config.Constants;
import com.rgs.galaxyinvaders.entities.*;
import com.rgs.galaxyinvaders.fx.Explosion;
import com.rgs.galaxyinvaders.fx.Starfield;

public class GameWorld {
    private final GalaxyInvadersGame game;

    // Entities & pools
    private final Pool<Bullet> bulletPool = new Pool<Bullet>() { @Override protected Bullet newObject() { return new Bullet(); } };
    private final Array<Bullet> playerBullets = new Array<>();
    private final Array<Bullet> enemyBullets = new Array<>();
    private final Array<Enemy> enemies = new Array<>();
    private final Array<PowerUp> powerups = new Array<>();
    private final Array<Explosion> explosions = new Array<>();
    private final Player player;

    private Boss boss = null;
    private int wave = 0;
    private int score = 0;
    private int bossesDefeated = 0;

    // Difficulty scalars (start easy, ramp with bosses)
    private float diffEnemySpeed = 0.8f, diffEnemyFire = 0.75f, diffSpawn = 0.75f, diffBossHP = 0.7f, diffBossFire = 0.8f;

    // Camera feedback
    private float shakeTime = 0f, shakeStrength = 0f;
    private float flashTime = 0f; // boss-kill flash
    private float slowmo = 0f;

    private boolean gameOver = false;

    // Background
    private final Starfield starfield = new Starfield(220);

    public GameWorld(GalaxyInvadersGame game) {
        this.game = game;
        player = new Player(this, game.assets);
        nextWave();
    }

    // ---------- Update / Render orchestration ----------
    public void update(float dt) {
        if (gameOver) return; // <<< freeze everything on subsequent frames
        if (slowmo > 0f) slowmo -= dt;
        starfield.update(dt);
        player.update(dt);

        if (boss == null && enemies.size == 0) nextWave();

        if (boss != null) {
            boss.update(dt);
            if (boss.getHp() <= 0) {
                score += 1000;
                boss = null;
                bossesDefeated++;
                recalcDifficulty();
                flash(0.4f);
                slowmo = 1.2f;
                shake(0.4f, 8f);
                Explosion ex = Explosion.big(player.getX(), player.getY(), game.assets);
                if (ex != null) explosions.add(ex);
            }
        }

        // Enemies
        for (int i = enemies.size - 1; i >= 0; i--) {
            Enemy e = enemies.get(i);
            e.update(dt);
            if (e.getY() + e.getH() < 0) enemies.removeIndex(i);
            else if (e.readyToShoot() && MathUtils.randomBoolean(e.getFireChance())) {
                e.resetShootTimer();
                fireEnemy(e.centerX(), e.getY());
            }
            if (e.rect().overlaps(player.rect())) {
                enemies.removeIndex(i);
                spawnExplosion(e.centerX(), e.centerY());
                hitPlayer();
            }
        }

        // Player bullets VS enemies / boss
        for (int i = playerBullets.size - 1; i >= 0; i--) {
            Bullet b = playerBullets.get(i);
            b.x += b.vx * dt; b.y += b.vy * dt;
            if (b.y > Constants.H) { freePlayerBullet(i); continue; }

            boolean hit = false;
            for (int j = enemies.size - 1; j >= 0; j--) {
                Enemy e = enemies.get(j);
                if (b.rect().overlaps(e.rect())) {
                    e.damage(1);
                    freePlayerBullet(i);
                    playHitSfx(0.45f);
                    if (e.isDead()) {
                        maybeDrop(e.centerX(), e.centerY());
                        score += e.getScoreValue();
                        spawnExplosion(e.centerX(), e.centerY());
                        enemies.removeIndex(j);
                        shake(0.08f, 2.2f);
                    }
                    hit = true; break;
                }
            }
            if (!hit && boss != null && b.rect().overlaps(boss.rect())) {
                boss.damage(1);
                score += Constants.BOSS_HIT_SCORE;
                playHitSfx(0.55f);
                if (MathUtils.random() < Constants.BOSS_HIT_POWERUP_CHANCE) maybeDrop(boss.centerX(), boss.centerY());
                freePlayerBullet(i);
                shake(0.02f, 1.2f);
                spawnExplosion(boss.centerX(), boss.centerY(), 0.03f, 0.25f);
            }
        }

        // Enemy bullets VS player
        for (int i = enemyBullets.size - 1; i >= 0; i--) {
            Bullet b = enemyBullets.get(i);
            b.x += b.vx * dt; b.y += b.vy * dt;
            if (b.y + b.h < 0) { freeEnemyBullet(i); continue; }
            if (b.rect().overlaps(player.rect())) {
                freeEnemyBullet(i);
                spawnExplosion(player.centerX(), player.centerY());
                playHitSfx(0.8f);
                hitPlayer();
            }
        }

        // Powerups
        for (int i = powerups.size - 1; i >= 0; i--) {
            PowerUp p = powerups.get(i);
            p.update(dt);
            if (p.getY() + p.getSize() < 0) { powerups.removeIndex(i); continue; }
            if (p.rect().overlaps(player.rect())) {
                p.apply(player);
                powerups.removeIndex(i);
                playPickupSfx(0.9f);
            }
        }

        // Explosions
        for (int i = explosions.size - 1; i >= 0; i--) {
            if (explosions.get(i).tick(dt)) explosions.removeIndex(i);
        }
    }

    public void renderSprites(SpriteBatch sb) {
        player.render(sb);
        for (Enemy e : enemies) e.render(sb);
        if (boss != null) boss.render(sb);
        for (Explosion ex : explosions) ex.render(sb);
    }

    public void renderHud(SpriteBatch sb, BitmapFont font) {
        font.setColor(Color.WHITE);
        font.draw(sb, "Score: " + score, 10, Constants.H - 10);
        font.draw(sb, "Lives: " + player.getLives(), 10, Constants.H - 35);
        font.draw(sb, "Wave: " + wave + "   Bosses: " + bossesDefeated, 10, Constants.H - 60);
    }

    public void renderShield(com.badlogic.gdx.graphics.glutils.ShapeRenderer sr) {
        if (player.getShieldTimer() > 0) {
            sr.setColor(1f, 0f, 1f, 0.5f);
            sr.circle(player.centerX(), player.centerY(), 26, 24);
        }
    }

    // NEW: time-aware version (freezes when dt = 0)
    public void renderFlash(ShapeRenderer sr, float dt) {
        if (flashTime <= 0f) return;
        flashTime -= dt;
        sr.begin(ShapeRenderer.ShapeType.Filled);
        sr.setColor(1f, 1f, 1f, MathUtils.clamp(flashTime, 0f, 0.5f));
        sr.rect(0, 0, Constants.W, Constants.H);
        sr.end();
    }

    public void applyCameraShake(OrthographicCamera cam, float delta) {
        if (shakeTime > 0f) {
            shakeTime -= delta;
            cam.position.x = Constants.W/2f + MathUtils.random(-shakeStrength, shakeStrength);
            cam.position.y = Constants.H/2f + MathUtils.random(-shakeStrength, shakeStrength);
        } else cam.position.set(Constants.W/2f, Constants.H/2f, 0);
    }

    // ---------- Spawning / Difficulty ----------
    private void recalcDifficulty() {
        float tier = 1f + bossesDefeated * 0.35f;
        diffEnemySpeed = MathUtils.clamp(0.80f * tier, 0.80f, 3.0f);
        diffEnemyFire  = MathUtils.clamp(0.75f * tier, 0.60f, 3.0f);
        diffSpawn      = MathUtils.clamp(0.75f * tier, 0.60f, 2.5f);
        diffBossHP     = MathUtils.clamp(0.70f * tier, 0.70f, 3.0f);
        diffBossFire   = MathUtils.clamp(0.80f * tier, 0.70f, 3.0f);
    }

    public void nextWave() {
        wave++;
        if (wave % 5 == 0) {
            boss = new Boss(this, game.assets, bossesDefeated + 1, diffBossHP, diffBossFire);
            return;
        }
        int base = 4 + Math.round(Math.min(wave, 6) * 0.6f);
        int count = Math.max(4, Math.round(base * diffSpawn));
        Spawner.spawnEnemies(this, game.assets, count, wave, diffEnemySpeed, diffEnemyFire);
    }

    public void spawnEnemy(Enemy e) { enemies.add(e); }

    // ---------- Events ----------
    private void hitPlayer() {
        if (player.consumeShieldIfAny()) { shake(0.15f, 4f); return; }
        player.loseLife();
        shake(0.25f, 6f);
        if (player.getLives() <= 0) {
            gameOver = true;
            game.maybeSetHighScore(score);
        } else {
            player.blink(1.2f);
        }
    }

    public void maybeDrop(float x, float y) {
        float r = MathUtils.random();
        if (r < 0.10f) powerups.add(PowerUp.rapid(x, y));
        else if (r < 0.18f) powerups.add(PowerUp.spread(x, y));
        else if (r < 0.24f) powerups.add(PowerUp.shield(x, y));
        else if (r < 0.28f) powerups.add(PowerUp.life(x, y));
    }

    public void spawnExplosion(float cx, float cy) { spawnExplosion(cx, cy, 0.05f, 0.7f); }
    public void spawnExplosion(float cx, float cy, float frameDur, float scale) {
        if (game.assets.explosions.size > 0) {
            Assets.ExplosionSeq seq = game.assets.explosions.random();
            explosions.add(new Explosion(cx, cy, seq, frameDur > 0 ? frameDur : seq.frameDuration, scale));
        }
    }

    // ---------- Shooting ----------
    public void firePlayer(float x, float y) {
        Bullet b = bulletPool.obtain().set(x - 3.5f, y, 7f, 7f, 420f);
        playerBullets.add(b);
    }
    public void fireEnemy(float x, float y) {
        Bullet b = bulletPool.obtain().set(x - 4f, y - 4f, 8f, 8f, -260f);
        enemyBullets.add(b);
    }
    public void addPlayerBullet(Bullet b) { playerBullets.add(b); }

    public boolean bossBulletCapReached() { return enemyBullets.size >= Constants.BOSS_BULLET_LIMIT; }

    public void freePlayerBullet(int idx) { Bullet b = playerBullets.removeIndex(idx); bulletPool.free(b); }
    public void freeEnemyBullet(int idx) { Bullet b = enemyBullets.removeIndex(idx); bulletPool.free(b); }

    // ---------- Feedback ----------
    public void shake(float t, float s) { shakeTime = Math.max(shakeTime, t); shakeStrength = Math.max(shakeStrength, s); }
    public void flash(float t) { flashTime = Math.max(flashTime, t); }
    public float getTimeScale() { return slowmo > 0 ? 0.25f : 1f; }

    private void playPickupSfx(float vol) {
        if (game.assets.pickup != null) game.assets.pickup.play(game.assets.sfx(vol));
    }
    private void playHitSfx(float vol) {
        if (game.assets.hit != null) game.assets.hit.play(game.assets.sfx(vol));
    }

    // ---------- Getters ----------
    public Player getPlayer() { return player; }
    public Boss getBoss() { return boss; }
    public Array<Enemy> getEnemies() { return enemies; }
    public Array<Bullet> getPlayerBullets() { return playerBullets; }
    public Array<Bullet> getEnemyBullets() { return enemyBullets; }
    public Array<PowerUp> getPowerUps() { return powerups; }
    public Starfield getStarfield() { return starfield; }
    public boolean isGameOver() { return gameOver; }
}
