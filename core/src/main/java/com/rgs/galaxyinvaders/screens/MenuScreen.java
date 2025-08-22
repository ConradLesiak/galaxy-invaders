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

public class MenuScreen implements Screen {

    private static final float W = 800, H = 480;

    private final GalaxyInvadersGame game;
    private final OrthographicCamera cam;
    private final Viewport viewport;
    private final SpriteBatch batch;
    private final BitmapFont font;
    private final GlyphLayout layout = new GlyphLayout();

    public MenuScreen(GalaxyInvadersGame game) {
        this.game = game;
        cam = new OrthographicCamera();
        viewport = new FitViewport(W, H, cam);
        viewport.apply(true);
        cam.position.set(W/2f, H/2f, 0);

        batch = new SpriteBatch();
        font = new BitmapFont();
    }

    @Override public void show() {
        // Start menu music (stops game music if it was playing)
        game.assets.playMenuMusic();
    }

    @Override public void render(float delta) {
        Gdx.gl.glClearColor(0.02f,0.02f,0.05f,1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // input
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            game.setScreen(new GameScreen(game));
            return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            Gdx.app.exit();
        }

        viewport.apply();
        cam.update();
        batch.setProjectionMatrix(cam.combined);

        batch.begin();
        font.setColor(Color.WHITE);
        layout.setText(font, "Galaxy Invaders");
        font.getData().setScale(1.3f);
        font.draw(batch, layout, (W - layout.width)/2f, H - 140);

        font.getData().setScale(1.0f);
        String line1 = "Press [ENTER] or [SPACE] to Start";
        String line2 = "Move: A/D or \u2190/\u2192   •   Fire: SPACE   •   Pause: P/ENTER   •   Menu: ESC";
        layout.setText(font, line1);
        font.setColor(Color.LIME);
        font.draw(batch, layout, (W - layout.width)/2f, H/2f + 10);
        font.setColor(Color.GRAY);
        layout.setText(font, line2);
        font.draw(batch, layout, (W - layout.width)/2f, H/2f - 24);

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
