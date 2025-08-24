package com.rgs.galaxyinvaders.assets;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;

public class Assets implements Disposable {

    public static class ExplosionSeq {
        public final Array<TextureRegion> frames = new Array<>();
        public float frameDuration = 0.05f;
    }

    public final AssetManager manager = new AssetManager();

    // Textures
    public TextureRegion playerShip, bossShip, white1x1;
    public final Array<TextureRegion> enemyShips = new Array<>();
    public final Array<ExplosionSeq> explosions = new Array<>();

    // Audio
    public Music menuMusic, gameMusic;
    public Sound pickup, hit;

    // Global audio state
    private boolean muted = false;
    private float musicVolume = 1f; // base music volume (when unmuted)
    private float sfxVolume   = 1f; // base SFX volume   (when unmuted)

    public void loadAll() {
        // Sprites
        manager.load("player1.png", Texture.class);
        manager.load("enemy1.png", Texture.class);
        manager.load("boss1.png",  Texture.class);

        // Audio
        manager.load("music1.mp3", Music.class);
        manager.load("music2.mp3", Music.class);
        manager.load("pickup.mp3", Sound.class);
        manager.load("hit.mp3",    Sound.class);
    }

    public void finishLoading() {
        manager.finishLoading();

        if (manager.isLoaded("player1.png")) playerShip = tr("player1.png");
        if (manager.isLoaded("enemy1.png"))  enemyShips.add(tr("enemy1.png"));
        if (manager.isLoaded("boss1.png"))   bossShip = tr("boss1.png");

        // 1x1 fallback
        Pixmap pm = new Pixmap(1,1, Pixmap.Format.RGBA8888);
        pm.setColor(Color.WHITE); pm.fill();
        white1x1 = new TextureRegion(new Texture(pm));
        pm.dispose();

        // Music
        if (manager.isLoaded("music1.mp3")) { menuMusic = manager.get("music1.mp3", Music.class); menuMusic.setLooping(true); }
        if (manager.isLoaded("music2.mp3")) { gameMusic = manager.get("music2.mp3", Music.class); gameMusic.setLooping(true); }
        // SFX
        if (manager.isLoaded("pickup.mp3")) pickup = manager.get("pickup.mp3", Sound.class);
        if (manager.isLoaded("hit.mp3"))    hit    = manager.get("hit.mp3", Sound.class);

        applyCurrentVolumes();
    }

    private TextureRegion tr(String path) { return new TextureRegion(manager.get(path, Texture.class)); }

    // --- Music control ---
    public void playMenuMusic() {
        if (gameMusic != null) gameMusic.stop();
        if (menuMusic != null && !menuMusic.isPlaying()) {
            menuMusic.setVolume(muted ? 0f : musicVolume);
            menuMusic.play();
        }
    }
    public void playGameMusic() {
        if (menuMusic != null) menuMusic.stop();
        if (gameMusic != null && !gameMusic.isPlaying()) {
            gameMusic.setVolume(muted ? 0f : musicVolume);
            gameMusic.play();
        }
    }
    public void stopAllMusic() { if (menuMusic != null) menuMusic.stop(); if (gameMusic != null) gameMusic.stop(); }

    // --- Global audio state ---
    public boolean isMuted() { return muted; }
    public void setMuted(boolean m) { muted = m; applyCurrentVolumes(); }
    public void setMusicVolume(float v) { musicVolume = clamp01(v); applyCurrentVolumes(); }
    public void setSfxVolume(float v)   { sfxVolume   = clamp01(v); }

    public void applyCurrentVolumes() {
        float mv = muted ? 0f : musicVolume;
        if (menuMusic != null) menuMusic.setVolume(mv);
        if (gameMusic != null) gameMusic.setVolume(mv);
    }

    /** Helper for SFX callers: returns effective volume respecting mute and SFX volume. */
    public float sfx(float requested) { return muted ? 0f : clamp01(requested) * sfxVolume; }

    @Override public void dispose() { stopAllMusic(); manager.dispose(); }

    private static float clamp01(float v){ return v < 0f ? 0f : (v > 1f ? 1f : v); }
}
