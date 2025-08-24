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
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.rgs.galaxyinvaders.GalaxyInvadersGame;
import com.rgs.galaxyinvaders.config.Constants;
import com.rgs.galaxyinvaders.entities.Bullet;
import com.rgs.galaxyinvaders.entities.PowerUp;
import com.rgs.galaxyinvaders.world.GameWorld;

public class GameScreen implements Screen {
    private final GalaxyInvadersGame game;
    private final OrthographicCamera cam = new OrthographicCamera();
    private final Viewport viewport = new FitViewport(Constants.W, Constants.H, cam);
    private final SpriteBatch batch = new SpriteBatch();
    private final ShapeRenderer shapes = new ShapeRenderer();
    private final BitmapFont font = new BitmapFont();
    private final GlyphLayout layout = new GlyphLayout();

    private final GameWorld world;

    private boolean paused = false;
    private boolean gameOver = false;

    public GameScreen(GalaxyInvadersGame game) {
        this.game = game;
        viewport.apply(true);
        cam.position.set(Constants.W/2f, Constants.H/2f, 0);
        world = new GameWorld(game);
    }

    @Override public void show() { game.assets.playGameMusic(); }

    @Override public void render(float delta) {
        handleInput();
        float dt = paused ? 0f : delta * world.getTimeScale();

        world.update(dt);
        gameOver = world.isGameOver();

        // Camera shake
        world.applyCameraShake(cam, delta);
        viewport.apply();
        Gdx.gl.glClearColor(0.02f,0.02f,0.05f,1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        cam.update();

        // Background stars
        world.getStarfield().render(shapes, delta, cam.combined);

        // Sprites
        batch.setProjectionMatrix(cam.combined);
        batch.begin();
        world.renderSprites(batch);
        batch.end();

        // Shapes: bullets, powerups, shield overlay, flashes
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapes.setProjectionMatrix(cam.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);

        // Player bullets (light green)
        shapes.setColor(Color.LIME);
        for (Bullet b : world.getPlayerBullets()) shapes.circle(b.cx(), b.cy(), b.radius(), 16);

        // Enemy bullets (red)
        shapes.setColor(Color.RED);
        for (Bullet b : world.getEnemyBullets()) shapes.circle(b.cx(), b.cy(), b.radius(), 16);

        // Powerups
        for (PowerUp p : world.getPowerUps()) p.render(shapes);

        // 50% shield ring
        world.renderShield(shapes);

        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        // Flash overlay (boss kill)
        world.renderFlash(shapes);

        // HUD
        batch.begin();
        world.renderHud(batch, font);
        if (paused) {
            layout.setText(font, "PAUSED  •  [ENTER]/[P] Resume  •  [ESC] Menu");
            font.setColor(Color.LIME);
            font.draw(batch, layout, (Constants.W - layout.width)/2f, Constants.H/2f);
            font.setColor(Color.WHITE);
        }
        if (gameOver) {
            layout.setText(font, "GAME OVER  •  [ENTER] Retry  •  [ESC] Menu");
            font.setColor(Color.SALMON);
            font.draw(batch, layout, (Constants.W - layout.width)/2f, Constants.H/2f);
            font.setColor(Color.WHITE);
        }
        batch.end();
    }

    private void handleInput() {
        // ESC: if playing -> pause; if paused -> Main Menu
        if (!gameOver) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
                if (!paused) { paused = true; return; }
                else { game.setScreen(new MenuScreen(game)); return; }
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.P)) {
                paused = !paused; if (paused) return;
            }
            if (paused && Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) paused = false;
        } else {
            if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) game.setScreen(new GameScreen(game));
            if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) game.setScreen(new MenuScreen(game));
        }
    }

    @Override public void resize(int width, int height) { viewport.update(width, height, true); }
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
    @Override public void dispose() { batch.dispose(); shapes.dispose(); font.dispose(); }
}
