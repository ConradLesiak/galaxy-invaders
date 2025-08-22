package com.rgs.galaxyinvaders.assets;

import com.badlogic.gdx.Gdx;
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

    // Sprites
    public TextureRegion playerShip;
    public final Array<TextureRegion> enemyShips = new Array<>();
    public TextureRegion bossShip;
    public final Array<ExplosionSeq> explosions = new Array<>();
    public TextureRegion white1x1;

    // Audio
    public Music menuMusic;
    public Music gameMusic;
    public Sound pickup;
    public Sound hit;

    public void loadAll() {
        // core textures
        manager.load("player1.png", Texture.class);
        manager.load("enemy1.png", Texture.class);
        manager.load("boss1.png",  Texture.class);

        // audio
        manager.load("music1.mp3", Music.class); // menu
        manager.load("music2.mp3", Music.class); // game
        manager.load("pickup.mp3", Sound.class);
        manager.load("hit.mp3",    Sound.class);
    }

    public void finishLoading() {
        manager.finishLoading();

        // textures
        if (manager.isLoaded("player1.png")) playerShip = new TextureRegion(manager.get("player1.png", Texture.class));
        if (manager.isLoaded("enemy1.png"))  enemyShips.add(new TextureRegion(manager.get("enemy1.png", Texture.class)));
        if (manager.isLoaded("boss1.png"))   bossShip = new TextureRegion(manager.get("boss1.png", Texture.class));

        // simple 1x1 fallback
        Pixmap pm = new Pixmap(1,1, Pixmap.Format.RGBA8888);
        pm.setColor(Color.WHITE);
        pm.fill();
        white1x1 = new TextureRegion(new Texture(pm));
        pm.dispose();

        // audio
        if (manager.isLoaded("music1.mp3")) {
            menuMusic = manager.get("music1.mp3", Music.class);
            menuMusic.setLooping(true);
        }
        if (manager.isLoaded("music2.mp3")) {
            gameMusic = manager.get("music2.mp3", Music.class);
            gameMusic.setLooping(true);
        }
        if (manager.isLoaded("pickup.mp3")) pickup = manager.get("pickup.mp3", Sound.class);
        if (manager.isLoaded("hit.mp3"))    hit    = manager.get("hit.mp3", Sound.class);
    }

    public void playMenuMusic() {
        if (gameMusic != null) gameMusic.stop();
        if (menuMusic != null && !menuMusic.isPlaying()) menuMusic.play();
    }

    public void playGameMusic() {
        if (menuMusic != null) menuMusic.stop();
        if (gameMusic != null && !gameMusic.isPlaying()) gameMusic.play();
    }

    public void stopAllMusic() {
        if (menuMusic != null) menuMusic.stop();
        if (gameMusic != null) gameMusic.stop();
    }

    @Override
    public void dispose() {
        stopAllMusic();
        manager.dispose();
    }
}
