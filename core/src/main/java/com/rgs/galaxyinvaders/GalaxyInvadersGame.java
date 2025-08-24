package com.rgs.galaxyinvaders;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.rgs.galaxyinvaders.assets.Assets;
import com.rgs.galaxyinvaders.screens.MenuScreen;

public class GalaxyInvadersGame extends Game {

    public Assets assets;
    private Preferences prefs;
    private int highScore = 0;
    private boolean muted = false;

    @Override
    public void create() {
        assets = new Assets();
        assets.loadAll();
        assets.finishLoading();

        prefs = Gdx.app.getPreferences("GalaxyInvadersPrefs");
        highScore = prefs.getInteger("highscore", 0);
        muted = prefs.getBoolean("muted", false);

        assets.setMuted(muted);           // apply to music immediately
        assets.applyCurrentVolumes();     // ensure volumes reflect state

        setScreen(new MenuScreen(this));
    }

    public int getHighScore() { return highScore; }
    public void maybeSetHighScore(int score) {
        if (score > highScore) {
            highScore = score;
            prefs.putInteger("highscore", highScore);
            prefs.flush();
        }
    }

    // --- Mute API for menus ---
    public boolean isMuted() { return muted; }
    public void setMuted(boolean m) {
        muted = m;
        prefs.putBoolean("muted", muted);
        prefs.flush();
        if (assets != null) {
            assets.setMuted(muted);
            assets.applyCurrentVolumes();
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        if (assets != null) assets.dispose();
    }
}
