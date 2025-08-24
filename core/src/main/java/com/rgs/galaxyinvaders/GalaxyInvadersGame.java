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

    @Override
    public void create() {
        assets = new Assets();
        assets.loadAll();
        assets.finishLoading();

        prefs = Gdx.app.getPreferences("GalaxyInvadersPrefs");
        highScore = prefs.getInteger("highscore", 0);

        setScreen(new MenuScreen(this));
    }

    public int getHighScore() { return highScore; }
    public void maybeSetHighScore(int s) {
        if (s > highScore) {
            highScore = s;
            prefs.putInteger("highscore", highScore);
            prefs.flush();
        }
    }

    @Override public void dispose() {
        super.dispose();
        if (assets != null) assets.dispose();
    }
}
