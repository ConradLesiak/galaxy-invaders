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
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.rgs.galaxyinvaders.GalaxyInvadersGame;
import com.rgs.galaxyinvaders.config.Constants;

public class MenuScreen implements Screen {
    private final GalaxyInvadersGame game;
    private final OrthographicCamera cam = new OrthographicCamera();
    private final Viewport viewport = new FitViewport(Constants.W, Constants.H, cam);
    private final SpriteBatch batch = new SpriteBatch();
    private final BitmapFont font = new BitmapFont();
    private final GlyphLayout layout = new GlyphLayout();

    public MenuScreen(GalaxyInvadersGame game) {
        this.game = game;
        viewport.apply(true);
        cam.position.set(Constants.W/2f, Constants.H/2f, 0);
    }

    @Override public void show() { game.assets.playMenuMusic(); }

    @Override public void render(float delta) {
        Gdx.gl.glClearColor(0.02f, 0.02f, 0.05f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Input
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            game.setScreen(new GameScreen(game)); return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) Gdx.app.exit();

        viewport.apply();
        cam.update();
        batch.setProjectionMatrix(cam.combined);
        batch.begin();

        font.setColor(Color.WHITE);
        font.getData().setScale(1.25f);
        layout.setText(font, "Galaxy Invaders");
        font.draw(batch, layout, (Constants.W - layout.width)/2f, Constants.H - 140);

        font.getData().setScale(1.0f);
        font.setColor(Color.LIME);
        layout.setText(font, "Press [ENTER] or [SPACE] to Start");
        font.draw(batch, layout, (Constants.W - layout.width)/2f, Constants.H/2f + 10);

        font.setColor(Color.GRAY);
        layout.setText(font, "Move: A/D or ←/→ • Fire: SPACE • Pause: P/ENTER • Menu: ESC");
        font.draw(batch, layout, (Constants.W - layout.width)/2f, Constants.H/2f - 24);

        font.setColor(Color.WHITE);
        font.draw(batch, "High Score: " + game.getHighScore(), 10, 24);

        batch.end();
    }

    @Override public void resize(int width, int height) { viewport.update(width, height, true); }
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
    @Override public void dispose() { batch.dispose(); font.dispose(); }
}
