package com.rgs.galaxyinvaders.assets;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;

/** Minimal asset loader: player1.png (player), enemy1.png (enemies), boss1.png (boss) from assets/sprites/. */
public class Assets implements Disposable {

    /** Kept so GameScreen's explosion code compiles even if we don't use it. */
    public static class ExplosionSeq {
        public final String key;
        public final Array<TextureRegion> frames = new Array<>();
        public float frameDuration = 0.05f;
        public ExplosionSeq(String key) { this.key = key; }
    }

    // Sprites the game uses
    public TextureRegion playerShip;
    public TextureRegion bossShip;
    public final Array<TextureRegion> enemyShips = new Array<>();
    public TextureRegion playerProjectile;
    public TextureRegion enemyProjectile;
    public final Array<ExplosionSeq> explosions = new Array<>(); // optional; can stay empty

    // Utility/fallback
    public TextureRegion white1x1;

    // Track textures to dispose
    private final Array<Texture> allTextures = new Array<>();

    /** Call once from Game.create() */
    public void loadAll() {
        // --- 1x1 white fallback ---
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(1, 1, 1, 1);
        pm.fill();
        Texture white = new Texture(pm);
        pm.dispose();
        white.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        white1x1 = new TextureRegion(white);
        allTextures.add(white);

        // --- Hardwired file paths (under assets/) ---
        // Player
        playerShip = loadRegionOrFallback("sprites/player1.png", "player1.png");

        // Enemies (all use enemy1.png)
        TextureRegion enemy = loadRegionOrFallback("sprites/enemy1.png", "enemy1.png");
        enemyShips.add(enemy);

        // Boss (prefer boss1.png; if missing, fall back to enemy1.png; else to white1x1)
        bossShip = loadRegionOrFallback("sprites/boss1.png", "boss1.png");
        if (bossShip == white1x1 && enemy != white1x1) bossShip = enemy;

        // Projectiles: simple circles (so use white fallback; drawing handled in GameScreen)
        playerProjectile = white1x1;
        enemyProjectile  = white1x1;

        // Log what we ended up using
        Gdx.app.log("Assets", "Player:  sprites/player1.png " + (playerShip == white1x1 ? "(MISSING → white1x1)" : "OK"));
        Gdx.app.log("Assets", "Enemy:   sprites/enemy1.png  " + (enemy == white1x1 ? "(MISSING → white1x1)" : "OK"));
        Gdx.app.log("Assets", "Boss:    sprites/boss1.png   " + (bossShip == white1x1 ? "(MISSING → fallback)" : "OK"));
    }

    private TextureRegion loadRegionOrFallback(String internalPath, String label) {
        FileHandle fh = Gdx.files.internal(internalPath);
        if (!fh.exists()) {
            Gdx.app.log("Assets", "WARN: " + label + " not found at " + internalPath + " — using white1x1.");
            return white1x1;
        }
        Texture tex = new Texture(fh);
        tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        allTextures.add(tex);
        return new TextureRegion(tex);
    }

    @Override
    public void dispose() {
        for (Texture t : allTextures) t.dispose();
    }
}
