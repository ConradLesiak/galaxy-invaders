package com.rgs.galaxyinvaders.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.rgs.galaxyinvaders.GalaxyInvadersGame;
import com.rgs.galaxyinvaders.assets.Assets;

public class GameScreen implements Screen {
    private static final float W = 800, H = 480;

    // Boss hit rewards
    private static final int BOSS_HIT_SCORE = 10;
    private static final float BOSS_HIT_POWERUP_CHANCE = 0.20f;

    // Ease knobs
    private static final int   BOSS_BULLET_LIMIT   = 60;    // cap total enemy bullets when boss present
    private static final float BULLET_HITBOX_SCALE = 0.60f; // smaller hitboxes (easier to dodge)
    private static final float BOSS_MISS_DEG       = 10f;   // add aim jitter to make shots dodgeable
    private static final float ATTACK_SLOW_FACTOR  = 1.50f; // all bosses attack slower (bigger cooldowns)

    private final GalaxyInvadersGame game;
    private final OrthographicCamera cam;
    private final Viewport viewport;
    private final SpriteBatch batch;
    private final ShapeRenderer shapes;
    private final BitmapFont font;
    private final GlyphLayout layout = new GlyphLayout();

    private final Starfield stars;

    private final Pool<Bullet> bulletPool = new Pool<Bullet>() {
        @Override protected Bullet newObject() { return new Bullet(); }
    };
    private final Array<Bullet> playerBullets = new Array<>();
    private final Array<Bullet> enemyBullets = new Array<>();
    private final Array<Enemy> enemies = new Array<>();
    private final Array<PowerUp> powerups = new Array<>();
    private final Array<Explosion> explosions = new Array<>();

    private final Player player;
    private Boss boss = null;
    private int wave = 0;
    private int score = 0;
    private boolean paused = false;
    private boolean gameOver = false;

    // Dynamic difficulty (easier start, ramps as bosses are defeated)
    private int bossesDefeated = 0;
    private float diffEnemySpeed = 0.8f;
    private float diffEnemyFire  = 0.75f;
    private float diffSpawn      = 0.75f;
    private float diffBossHP     = 0.7f;
    private float diffBossFire   = 0.8f;

    private float shakeTime = 0f, shakeStrength = 0f;
    private float flashTime = 0f; // boss-kill flash only
    private float slowmo = 0f;

    public GameScreen(GalaxyInvadersGame game) {
        this.game = game;
        cam = new OrthographicCamera();
        viewport = new FitViewport(W, H, cam);
        viewport.apply(true);
        cam.position.set(W/2f, H/2f, 0);

        batch = new SpriteBatch();
        shapes = new ShapeRenderer();
        font = new BitmapFont();
        stars = new Starfield(220);
        player = new Player(W / 2f, 40);

        recalcDifficulty();
        nextWave();
    }

    private void recalcDifficulty() {
        float tier = 1f + bossesDefeated * 0.35f;
        diffEnemySpeed = MathUtils.clamp(0.80f * tier, 0.80f, 3.0f);
        diffEnemyFire  = MathUtils.clamp(0.75f * tier, 0.60f, 3.0f);
        diffSpawn      = MathUtils.clamp(0.75f * tier, 0.60f, 2.5f);
        diffBossHP     = MathUtils.clamp(0.70f * tier, 0.70f, 3.0f);
        diffBossFire   = MathUtils.clamp(0.80f * tier, 0.70f, 3.0f);
    }

    @Override public void render(float delta) {
        float dt = paused ? 0f : delta * (slowmo > 0 ? 0.25f : 1f);
        update(dt);

        if (shakeTime > 0f) {
            shakeTime -= delta;
            cam.position.x = W/2f + MathUtils.random(-shakeStrength, shakeStrength);
            cam.position.y = H/2f + MathUtils.random(-shakeStrength, shakeStrength);
        } else {
            cam.position.set(W/2f, H/2f, 0);
        }

        viewport.apply();
        Gdx.gl.glClearColor(0.02f, 0.02f, 0.05f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        cam.update();

        shapes.setProjectionMatrix(cam.combined);
        batch.setProjectionMatrix(cam.combined);

        // Background
        stars.render(shapes, delta);

        // ===== SPRITES =====
        batch.begin();
        player.render(batch);
        for (Enemy e : enemies) e.render(batch);
        if (boss != null) boss.render(batch);
        for (int i = explosions.size - 1; i >= 0; i--) {
            if (explosions.get(i).render(batch, dt)) explosions.removeIndex(i);
        }
        batch.end();

        // ===== SHAPES (circle bullets, powerups, 50% shield) =====
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapes.begin(ShapeRenderer.ShapeType.Filled);

        shapes.setColor(Color.LIME);
        for (Bullet b : playerBullets) shapes.circle(b.x + b.w/2f, b.y + b.h/2f, b.w/2f, 16);

        shapes.setColor(Color.RED);
        for (Bullet b : enemyBullets) shapes.circle(b.x + b.w/2f, b.y + b.h/2f, b.w/2f, 16);

        for (PowerUp p : powerups) p.render(shapes);

        if (player.shieldTimer > 0) {
            shapes.setColor(Color.VIOLET.r, Color.VIOLET.g, Color.VIOLET.b, 0.5f);
            shapes.circle(player.cx(), player.cy(), 26, 24);
        }

        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        // boss-kill white flash
        if (flashTime > 0f) {
            flashTime -= delta;
            shapes.begin(ShapeRenderer.ShapeType.Filled);
            shapes.setColor(1, 1, 1, MathUtils.clamp(flashTime, 0, 0.5f));
            shapes.rect(0, 0, W, H);
            shapes.end();
        }

        // ===== HUD =====
        batch.begin();
        font.setColor(Color.WHITE);
        font.draw(batch, "Score: " + score, 10, H - 10);
        font.draw(batch, "Lives: " + player.lives, 10, H - 35);
        font.draw(batch, "Wave: " + wave + "   Bosses: " + bossesDefeated, 10, H - 60);
        font.draw(batch, "High: " + game.getHighScore(), 10, H - 85);

        if (player.rapidTimer > 0) font.setColor(Color.YELLOW);
        font.draw(batch, "Rapid", W - 240, H - 10);
        font.setColor(Color.CYAN);
        if (player.spreadTimer > 0) font.setColor(Color.CYAN);
        font.draw(batch, "Spread", W - 180, H - 10);
        font.setColor(Color.VIOLET);
        if (player.shieldTimer > 0) font.setColor(Color.VIOLET);
        font.draw(batch, "Shield", W - 120, H - 10);
        font.setColor(Color.WHITE);

        if (paused) {
            layout.setText(font, "PAUSED  •  [ENTER]/[P] Resume  •  [ESC] Menu");
            font.setColor(Color.LIME);
            font.draw(batch, layout, (W - layout.width)/2f, H/2f);
            font.setColor(Color.WHITE);
        }

        if (gameOver) {
            layout.setText(font, "GAME OVER  •  [ENTER] Retry  •  [ESC] Menu");
            font.setColor(Color.SALMON);
            font.draw(batch, layout, (W - layout.width)/2f, H/2f);
            font.setColor(Color.WHITE);
        }
        batch.end();
    }

    private void update(float dt) {
        // === Input: Pause/Menu ===
        if (!gameOver) {
            // ESC: if playing -> pause; if paused -> Main Menu
            if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
                if (!paused) {
                    paused = true;
                    return; // stop updates this frame while showing pause
                } else {
                    game.setScreen(new MenuScreen(game));
                    return;
                }
            }
            // P toggles pause
            if (Gdx.input.isKeyJustPressed(Input.Keys.P)) {
                paused = !paused;
                if (paused) return;
            }
            // ENTER resumes from pause
            if (paused && Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
                paused = false;
            }
        }

        // Game over behavior
        if (gameOver) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) game.setScreen(new GameScreen(game));
            if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) game.setScreen(new MenuScreen(game));
            return;
        }

        // If paused, skip updates
        if (paused) return;

        if (slowmo > 0) slowmo -= dt;

        stars.update(dt);
        player.update(dt);

        if (boss == null && enemies.size == 0) nextWave();

        if (boss != null) {
            boss.update(dt);
            if (boss.hp <= 0) {
                score += 1000;
                boss = null;
                bossesDefeated++;
                recalcDifficulty();
                flash(0.4f);
                slowmo = 1.2f;
                shake(0.4f, 8f);
                Explosion ex = Explosion.big(player.x, player.y, game.assets);
                if (ex != null) explosions.add(ex);
            }
        }

        // Enemies
        for (int i = enemies.size - 1; i >= 0; i--) {
            Enemy e = enemies.get(i);
            e.update(dt);
            if (e.y + e.h < 0) enemies.removeIndex(i);
            else if (e.shootTimer <= 0 && MathUtils.randomBoolean(e.fireChance)) {
                e.shootTimer = e.reload;
                fireEnemy(e.x + e.w/2f, e.y);
            }
            if (overlap(e.rect(), player.rect())) {
                enemies.removeIndex(i);
                spawnExplosion(e.x + e.w/2f, e.y + e.h/2f);
                playerHit();
            }
        }

        // Player bullets
        for (int i = playerBullets.size - 1; i >= 0; i--) {
            Bullet b = playerBullets.get(i);
            b.x += b.vx * dt;
            b.y += b.vy * dt;
            if (b.y > H) { freePlayerBullet(i); continue; }

            boolean hit = false;
            for (int j = enemies.size - 1; j >= 0; j--) {
                Enemy e = enemies.get(j);
                if (overlap(b.rect(), e.rect())) {
                    e.hp--;
                    e.hitTimer = 0.12f;
                    freePlayerBullet(i);
                    if (e.hp <= 0) {
                        maybeDrop(e.x + e.w/2f, e.y + e.h/2f);
                        score += e.scoreValue;
                        spawnExplosion(e.x + e.w/2f, e.y + e.h/2f);
                        enemies.removeIndex(j);
                        shake(0.08f, 2.2f);
                    }
                    hit = true;
                    break;
                }
            }
            if (!hit && boss != null && overlap(b.rect(), boss.rect())) {
                boss.hp--;
                boss.hitTimer = 0.12f;
                score += BOSS_HIT_SCORE;
                if (MathUtils.random() < BOSS_HIT_POWERUP_CHANCE) {
                    maybeDrop(boss.x + boss.w/2f, boss.y + boss.h/2f);
                }
                freePlayerBullet(i);
                shake(0.02f, 1.2f);
                spawnExplosion(boss.x + boss.w/2f, boss.y + boss.h/2f, 0.03f, 0.25f);
            }
        }

        // Enemy bullets
        for (int i = enemyBullets.size - 1; i >= 0; i--) {
            Bullet b = enemyBullets.get(i);
            b.x += b.vx * dt;
            b.y += b.vy * dt;
            if (b.y + b.h < 0) { freeEnemyBullet(i); continue; }
            if (overlap(b.rect(), player.rect())) {
                freeEnemyBullet(i);
                spawnExplosion(player.x + player.w/2f, player.y + player.h/2f);
                playerHit();
            }
        }

        // Powerups
        for (int i = powerups.size - 1; i >= 0; i--) {
            PowerUp p = powerups.get(i);
            p.update(dt);
            if (p.y + p.size < 0) { powerups.removeIndex(i); continue; }
            if (overlap(p.rect(), player.rect())) {
                p.apply(player);
                powerups.removeIndex(i);
            }
        }
    }

    private void playerHit() {
        if (player.shieldTimer > 0) {
            player.shieldTimer = 0;
            shake(0.15f, 4f);
            return;
        }
        player.lives--;
        shake(0.25f, 6f);
        if (player.lives <= 0) {
            gameOver = true;
            game.maybeSetHighScore(score);
        } else {
            player.blinkTimer = 1.2f;
        }
    }

    private void nextWave() {
        wave++;
        if (wave % 5 == 0) {
            boss = new Boss(W/2f - 60, H - 120, bossesDefeated + 1); // pass boss level (1-based)
            return;
        }

        int base = 4 + Math.round(Math.min(wave, 6) * 0.6f);
        int count = Math.max(4, Math.round(base * diffSpawn));
        for (int i = 0; i < count; i++) {
            int type = MathUtils.random(0, 2); // 0 sine, 1 shooter, 2 chaser
            float x = MathUtils.random(40, W - 80);
            float y = H + MathUtils.random(30, 200);
            TextureRegion sprite = pickEnemySprite();
            enemies.add(new Enemy(type, x, y, wave, sprite));
        }
    }

    private TextureRegion pickEnemySprite() {
        if (game.assets.enemyShips.size > 0) {
            return game.assets.enemyShips.get(MathUtils.random(0, game.assets.enemyShips.size-1));
        }
        return game.assets.playerShip != null ? game.assets.playerShip : game.assets.white1x1;
    }

    private void maybeDrop(float x, float y) {
        float r = MathUtils.random();
        if (r < 0.10f) powerups.add(PowerUp.rapid(x, y));
        else if (r < 0.18f) powerups.add(PowerUp.spread(x, y));
        else if (r < 0.24f) powerups.add(PowerUp.shield(x, y));
        else if (r < 0.28f) powerups.add(PowerUp.life(x, y));
    }

    // ==== Bullets (drawn as circles) ====
    private void fireEnemy(float x, float y) {
        Bullet b = bulletPool.obtain().set(x - 4f, y - 4f, 8f, 8f, -260f);
        enemyBullets.add(b);
    }
    private void firePlayer(float x, float y) {
        Bullet b = bulletPool.obtain().set(x - 3.5f, y, 7f, 7f, 420f);
        playerBullets.add(b);
    }

    private void freePlayerBullet(int idx) { Bullet b = playerBullets.removeIndex(idx); bulletPool.free(b); }
    private void freeEnemyBullet(int idx) { Bullet b = enemyBullets.removeIndex(idx); bulletPool.free(b); }
    private static boolean overlap(Rectangle a, Rectangle b) { return Intersector.overlaps(a, b); }

    private void shake(float time, float strength) {
        shakeTime = Math.max(shakeTime, time);
        shakeStrength = Math.max(shakeStrength, strength);
    }
    private void flash(float t) { flashTime = Math.max(flashTime, t); }

    @Override public void show() {}
    @Override public void resize(int width, int height) { viewport.update(width, height, true); }
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
    @Override public void dispose() { batch.dispose(); shapes.dispose(); font.dispose(); }

    // ===================== Entities =====================

    class Player {
        float x, y, w, h;
        float speed = 320f;
        float cooldown = 0.22f;
        float cdTimer = 0f;
        int lives = 3;

        float rapidTimer = 0f, spreadTimer = 0f, shieldTimer = 0f;
        float blinkTimer = 0f;

        TextureRegion sprite;

        Player(float cx, float y) {
            this.sprite = (game.assets.playerShip != null) ? game.assets.playerShip : game.assets.white1x1;
            float targetW = 36f;
            float scale = targetW / sprite.getRegionWidth();
            this.w = sprite.getRegionWidth() * scale;
            this.h = sprite.getRegionHeight() * scale;
            this.x = cx - w/2f;
            this.y = y;
        }

        void update(float dt) {
            float dir = 0;
            if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) dir -= 1f;
            if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) dir += 1f;
            x += dir * speed * dt;
            x = MathUtils.clamp(x, 6, W - w - 6);

            cdTimer -= dt;
            boolean autoFire = rapidTimer > 0;
            if ((Gdx.input.isKeyPressed(Input.Keys.SPACE) && cdTimer <= 0) || (autoFire && cdTimer <= 0)) {
                shoot();
                cdTimer = (rapidTimer > 0) ? 0.07f : cooldown;
            }

            if (rapidTimer > 0) rapidTimer -= dt;
            if (spreadTimer > 0) spreadTimer -= dt;
            if (shieldTimer > 0) shieldTimer -= dt;
            if (blinkTimer > 0) blinkTimer -= dt;
        }

        void shoot() {
            float cx = x + w/2f, top = y + h;
            firePlayer(cx, top);
            if (spreadTimer > 0) {
                Bullet l = bulletPool.obtain().set(cx - 3.5f, top, 7f, 7f, 420f); l.vx = -120f;
                Bullet r = bulletPool.obtain().set(cx - 3.5f, top, 7f, 7f, 420f); r.vx = 120f;
                playerBullets.add(l); playerBullets.add(r);
            }
        }

        void render(SpriteBatch sb) {
            if (blinkTimer > 0 && ((int)(blinkTimer * 20) % 2 == 0)) return;
            sb.draw(sprite, x, y, w, h);
        }

        Rectangle rect() { return new Rectangle(x, y, w, h); }
        float cx() { return x + w/2f; }
        float cy() { return y + h/2f; }
    }

    static class Bullet implements Pool.Poolable {
        float x, y, w, h, vy, vx;
        Bullet set(float x, float y, float w, float h, float vy) {
            this.x = x; this.y = y; this.w = w; this.h = h; this.vy = vy; this.vx = 0f;
            return this;
        }
        Rectangle rect() {
            // smaller, centered hitbox for fairness
            float sw = w * BULLET_HITBOX_SCALE;
            float sh = h * BULLET_HITBOX_SCALE;
            float sx = x + (w - sw) / 2f;
            float sy = y + (h - sh) / 2f;
            return new Rectangle(sx, sy, sw, sh);
        }
        @Override public void reset() { x = y = w = h = 0f; vy = vx = 0f; }
    }

    class Enemy {
        float x, y, w, h;
        int type; // 0 sine, 1 shooter, 2 chaser
        int hp = 1;
        float t = 0f;
        float speed = 60f;
        float shootTimer = MathUtils.random(0.3f, 2f);
        float reload = 1.1f;                           // slower baseline
        float fireChance = 0.03f;                      // easier baseline
        int scoreValue = 20;
        TextureRegion sprite;

        float hitTimer = 0f;

        Enemy(int type, float cx, float y, int wave, TextureRegion sprite) {
            this.type = type; this.sprite = sprite != null ? sprite : game.assets.white1x1;
            float targetW = 28f;
            float scale = targetW / this.sprite.getRegionWidth();
            this.w = this.sprite.getRegionWidth() * scale;
            this.h = this.sprite.getRegionHeight() * scale;
            this.x = cx - w/2f; this.y = y;

            this.hp = (type==1 && wave>6) ? 2 : 1;
            this.speed = (45f + wave * 3f) * diffEnemySpeed;
            this.reload = (1.1f - Math.min(0.5f, wave * 0.02f)) / Math.max(0.6f, diffEnemyFire);
            this.fireChance = (0.03f + wave * 0.0015f) * diffEnemyFire;
            this.scoreValue = 20 + wave * 3;
            if (type==2) this.reload *= 0.85f;
        }

        void update(float dt) {
            t += dt;
            shootTimer -= dt;
            if (hitTimer > 0f) hitTimer -= dt;

            switch (type) {
                case 0: y -= speed * dt; x += MathUtils.sin(t * 2.4f) * 80f * dt; break;
                case 1: y -= speed * 0.8f * dt; x += MathUtils.sin(t * 1.6f) * 50f * dt; break;
                case 2: float dir = Math.signum((player.x + player.w/2f) - (x + w/2f));
                    x += dir * (speed * 1.3f) * dt; y -= speed * 1.1f * dt; break;
            }
            x = MathUtils.clamp(x, 0, W - w);
        }

        void render(SpriteBatch sb) {
            sb.draw(sprite, x, y, w, h);
            if (hitTimer > 0f) {
                float a = Math.min(0.6f, hitTimer / 0.12f);
                sb.setColor(1f, 0f, 0f, a);
                sb.draw(sprite, x, y, w, h);
                sb.setColor(Color.WHITE);
            }
        }

        Rectangle rect() { return new Rectangle(x, y, w, h); }
    }

    class Boss {
        float x, y, w, h;
        int hp, maxHp;
        float t = 0f, dir = 1f;
        float fireTimer = 0f;
        TextureRegion sprite;
        float hitTimer = 0f;
        final int level; // 1 = first boss, 2 = second, ...

        Boss(float x, float y, int level) {
            this.level = level;
            this.sprite = (game.assets.bossShip != null) ? game.assets.bossShip : game.assets.white1x1;
            float targetW = 140f;
            float scale = targetW / sprite.getRegionWidth();
            this.w = sprite.getRegionWidth() * scale;
            this.h = sprite.getRegionHeight() * scale;
            this.x = x; this.y = y;

            // half of base HP, then scale with diffBossHP
            int base = 120 + wave * 30;
            this.hp = Math.max(1, Math.round((base / 2f) * diffBossHP));
            this.maxHp = this.hp;
        }

        void update(float dt) {
            t += dt;
            if (hitTimer > 0f) hitTimer -= dt;

            x += dir * 100f * dt;
            if (x < 20 || x + w > W - 20) dir *= -1f;

            // respect global bullet cap
            if (enemyBullets.size >= BOSS_BULLET_LIMIT) {
                fireTimer = Math.max(fireTimer, 0.15f);
                fireTimer -= dt;
                return;
            }

            // Allowed pattern count by boss level:
            // level 1 -> 2 patterns (aimed + fan), level 2+ -> 3 patterns (adds spiral)
            int patternSlots = Math.min(2 + (level - 1), 3);
            boolean allowAimed  = true;
            boolean allowFan    = patternSlots >= 2;
            boolean allowSpiral = patternSlots >= 3;

            // Projectile counts scale gently with level (Boss 1 stays at 2/5)
            int earlyVolley = (level == 1) ? 2 : Math.min(2 + level, 6);
            int midFan      = (level == 1) ? 5 : Math.min(5 + (level - 1), 10);
            int lateSpiral  = (level == 1) ? 5 : Math.min(6 + (level - 1), 12);

            // Slower fire for all bosses
            float intervalScale = ATTACK_SLOW_FACTOR / Math.max(1f, diffBossFire);

            float hpPct = (float)hp / maxHp;
            fireTimer -= dt;
            if (fireTimer <= 0f) {
                if (hpPct > 0.66f) {
                    // Early phase: prefer aimed; fallback to fan if needed
                    if (allowAimed) aimedVolley(earlyVolley, 140f);
                    else if (allowFan) fan(-45, 45, earlyVolley, 160f);
                    fireTimer = 1.4f * intervalScale;
                } else if (hpPct > 0.33f) {
                    // Mid phase: prefer fan; fallback to aimed
                    if (allowFan)   fan(-45, 45, midFan, 180f);
                    else if (allowAimed) aimedVolley(Math.max(2, midFan/2), 150f);
                    fireTimer = 1.2f * intervalScale;
                } else {
                    // Late phase: prefer spiral; fallback to fan/aimed
                    if (allowSpiral)      spiral(lateSpiral, 160f);
                    else if (allowFan)    fan(-50, 50, Math.max(5, lateSpiral), 170f);
                    else if (allowAimed)  aimedVolley(Math.max(3, earlyVolley + 1), 160f);
                    fireTimer = 1.0f * intervalScale;
                }
            }
        }

        void aimedVolley(int count, float speed) {
            if (enemyBullets.size >= BOSS_BULLET_LIMIT) return;
            float cx = x + w/2f, cy = y;
            for (int i = 0; i < count; i++) {
                if (enemyBullets.size >= BOSS_BULLET_LIMIT) break;
                Bullet b = bulletPool.obtain().set(cx - 4f, cy - 4f, 8f, 8f, -speed);
                float dx = (player.x + player.w/2f) - cx;
                float dy = (player.y + player.h/2f) - cy;
                float ang = MathUtils.atan2(dy, dx) + MathUtils.degreesToRadians * MathUtils.random(-BOSS_MISS_DEG, BOSS_MISS_DEG);
                b.vx = MathUtils.cos(ang) * 120f;
                enemyBullets.add(b);
            }
        }

        void fan(float a0, float a1, int n, float speed) {
            if (enemyBullets.size >= BOSS_BULLET_LIMIT) return;
            float cx = x + w/2f, cy = y;
            for (int i = 0; i < n; i++) {
                if (enemyBullets.size >= BOSS_BULLET_LIMIT) break;
                float a = MathUtils.lerp(a0, a1, i/(float)(n-1));
                float rad = (a + 90f) * MathUtils.degreesToRadians;
                Bullet b = bulletPool.obtain().set(cx - 4f, cy - 4f, 8f, 8f, -speed);
                b.vx = MathUtils.cos(rad) * 160f;
                enemyBullets.add(b);
            }
        }

        void spiral(int n, float speed) {
            if (enemyBullets.size >= BOSS_BULLET_LIMIT) return;
            float cx = x + w/2f, cy = y;
            for (int i = 0; i < n; i++) {
                if (enemyBullets.size >= BOSS_BULLET_LIMIT) break;
                float a = (t * 180f + i * (360f/n)) * MathUtils.degreesToRadians; // slower spiral
                Bullet b = bulletPool.obtain().set(cx - 4f, cy - 4f, 8f, 8f, -speed);
                b.vx = MathUtils.cos(a) * 150f;
                enemyBullets.add(b);
            }
        }

        void render(SpriteBatch sb) {
            sb.draw(sprite, x, y, w, h);
            if (hitTimer > 0f) {
                float a = Math.min(0.6f, hitTimer / 0.12f);
                sb.setColor(1f, 0f, 0f, a);
                sb.draw(sprite, x, y, w, h);
                sb.setColor(Color.WHITE);
            }
            // HP bar
            float pct = Math.max(0, (float)hp / maxHp);
            float barW = 400f, barX = 200f, barY = H - 26f, barH = 8f;
            sb.setColor(Color.DARK_GRAY); sb.draw(game.assets.white1x1, barX, barY, barW, barH);
            sb.setColor(Color.RED);       sb.draw(game.assets.white1x1, barX, barY, barW * pct, barH);
            sb.setColor(Color.WHITE);
        }

        Rectangle rect() { return new Rectangle(x, y, w, h); }
    }

    static class PowerUp {
        enum Type { RAPID, SPREAD, SHIELD, LIFE }
        Type type;
        float x, y, size = 14, vy = -65f;

        static PowerUp rapid(float x, float y) { PowerUp p=new PowerUp(); p.type=Type.RAPID; p.x=x; p.y=y; return p; }
        static PowerUp spread(float x, float y) { PowerUp p=new PowerUp(); p.type=Type.SPREAD; p.x=x; p.y=y; return p; }
        static PowerUp shield(float x, float y) { PowerUp p=new PowerUp(); p.type=Type.SHIELD; p.x=x; p.y=y; return p; }
        static PowerUp life(float x, float y) { PowerUp p=new PowerUp(); p.type=Type.LIFE; p.x=x; p.y=y; return p; }

        void update(float dt) { y += vy * dt; }

        void apply(Player pl) {
            switch (type) {
                case RAPID:  pl.rapidTimer = Math.max(pl.rapidTimer, 8f); break;
                case SPREAD: pl.spreadTimer = Math.max(pl.spreadTimer, 8f); break;
                case SHIELD: pl.shieldTimer = Math.max(pl.shieldTimer, 10f); break;
                case LIFE:   pl.lives = Math.min(pl.lives + 1, 5); break;
            }
        }

        void render(ShapeRenderer sr) {
            switch (type) {
                case RAPID:  sr.setColor(Color.YELLOW); break;
                case SPREAD: sr.setColor(Color.CYAN); break;
                case SHIELD: sr.setColor(Color.VIOLET); break;
                case LIFE:   sr.setColor(Color.LIME); break;
            }
            sr.triangle(x, y + size, x - size, y, x + size, y);
            sr.triangle(x, y - size, x - size, y, x + size, y);
        }

        Rectangle rect() { return new Rectangle(x - size, y - size, size * 2, size * 2); }
    }

    static class Starfield {
        static class Star { float x,y,spd,size; Color c; }
        final Array<Star> stars = new Array<>();

        Starfield(int count) {
            for (int i=0;i<count;i++) {
                Star s = new Star();
                s.x = MathUtils.random(0, W);
                s.y = MathUtils.random(0, H);
                s.spd = MathUtils.random(30f, 180f);
                s.size = MathUtils.random(1.2f, 2.8f);
                float g = MathUtils.random(0.6f, 1f);
                s.c = new Color(g, g, 1f, 1f);
                stars.add(s);
            }
        }

        void update(float dt) {
            for (Star s : stars) {
                s.y -= s.spd * dt;
                if (s.y < 0) { s.y = H; s.x = MathUtils.random(0, W); }
            }
        }

        void render(ShapeRenderer sr, float dt) {
            update(dt);
            sr.begin(ShapeRenderer.ShapeType.Filled);
            for (Star s : stars) {
                sr.setColor(s.c);
                sr.circle(s.x, s.y, s.size, 8);
            }
            sr.end();
        }
    }

    static class Explosion {
        final Array<TextureRegion> frames;
        float x, y, t=0, frameDuration, scale;
        Explosion(float cx, float cy, Assets.ExplosionSeq seq, float frameDuration, float scale) {
            this.x=cx; this.y=cy; this.frames=new Array<>(seq.frames);
            this.frameDuration = frameDuration;
            this.scale = scale;
        }
        static Explosion big(float cx, float cy, Assets assets) {
            if (assets.explosions.size == 0) return null;
            Assets.ExplosionSeq seq = assets.explosions.get(0);
            return new Explosion(cx, cy, seq, 0.04f, 1.0f);
        }
        boolean render(SpriteBatch sb, float dt) {
            if (frames.size == 0) return true;
            t += dt;
            int idx = (int)(t / frameDuration);
            if (idx >= frames.size) return true;
            TextureRegion r = frames.get(idx);
            float w = r.getRegionWidth() * scale;
            float h = r.getRegionHeight() * scale;
            sb.draw(r, x - w/2f, y - h/2f, w, h);
            return false;
        }
    }

    private void spawnExplosion(float cx, float cy) { spawnExplosion(cx, cy, 0.05f, 0.7f); }
    private void spawnExplosion(float cx, float cy, float frameDur, float scale) {
        if (game.assets.explosions.size > 0) {
            Assets.ExplosionSeq seq = game.assets.explosions.get(MathUtils.random(0, game.assets.explosions.size - 1));
            explosions.add(new Explosion(cx, cy, seq, frameDur > 0 ? frameDur : seq.frameDuration, scale));
        }
    }
}
