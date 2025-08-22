package com.rgs.galaxyinvaders;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Preferences;
import com.rgs.galaxyinvaders.assets.Assets;
import com.rgs.galaxyinvaders.screens.MenuScreen;

public class GalaxyInvadersGame extends Game {
    public Preferences prefs;
    public Assets assets;

    @Override
    public void create() {
        prefs = com.badlogic.gdx.Gdx.app.getPreferences("galaxy_invaders_prefs");
        if (!prefs.contains("highscore")) prefs.putInteger("highscore", 0).flush();

        assets = new Assets();
        assets.loadAll();

        setScreen(new MenuScreen(this));
    }

    public int getHighScore() { return prefs.getInteger("highscore", 0); }
    public void maybeSetHighScore(int score) {
        if (score > getHighScore()) { prefs.putInteger("highscore", score).flush(); }
    }

    @Override
    public void dispose() {
        super.dispose();
        if (assets != null) assets.dispose();
    }
}
