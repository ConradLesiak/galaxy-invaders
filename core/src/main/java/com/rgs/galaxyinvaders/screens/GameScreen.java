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
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.rgs.galaxyinvaders.GalaxyInvadersGame;
import com.rgs.galaxyinvaders.config.Constants;
import com.rgs.galaxyinvaders.entities.Bullet;
import com.rgs.galaxyinvaders.entities.PowerUp;
import com.rgs.galaxyinvaders.input.TouchControls;
import com.rgs.galaxyinvaders.ui.UiFactory;
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
    private final TouchControls controls = new TouchControls();
    private final boolean showTouchUi = true;

    private boolean paused = false;
    private boolean gameOver = false;

    // Pause icon (top-right)
    private final Vector2 pauseCenter = new Vector2(Constants.W - 28f, Constants.H - 28f);
    private final float pauseRadius  = 20f;

    // UI stages & skin
    private Stage pauseStage, overStage;
    private Skin skin;

    public GameScreen(GalaxyInvadersGame game) {
        this.game = game;
        viewport.apply(true);
        cam.position.set(Constants.W/2f, Constants.H/2f, 0);
        world = new GameWorld(game);
    }

    @Override public void show() {
        game.assets.playGameMusic();
        // Build UI once
        if (skin == null) skin = UiFactory.createBasicSkin();
        if (pauseStage == null) buildPauseStage();
        if (overStage == null)  buildOverStage();
        // Ensure gameplay has input focus initially
        Gdx.input.setInputProcessor(null);
    }

    private void buildPauseStage() {
        pauseStage = new Stage(viewport);
        Table root = new Table(); root.setFillParent(true); pauseStage.addActor(root);

        TextButton resume = UiFactory.button(skin, "Resume");
        TextButton menu   = UiFactory.button(skin, "Main Menu");

        resume.addListener(new ClickListener(){ @Override public void clicked(InputEvent e, float x,float y){ paused = false; Gdx.input.setInputProcessor(null);} });
        menu.addListener(new ClickListener(){ @Override public void clicked(InputEvent e, float x,float y){ game.setScreen(new MenuScreen(game)); } });

        root.center().pad(20);
        root.add(resume).width(240).height(56).pad(6).row();
        root.add(menu).width(240).height(56).pad(6);
    }

    private void buildOverStage() {
        overStage = new Stage(viewport);
        Table root = new Table(); root.setFillParent(true); overStage.addActor(root);

        TextButton retry = UiFactory.button(skin, "Retry");
        TextButton menu  = UiFactory.button(skin, "Main Menu");

        retry.addListener(new ClickListener(){ @Override public void clicked(InputEvent e, float x,float y){ game.setScreen(new GameScreen(game)); } });
        menu.addListener(new ClickListener(){ @Override public void clicked(InputEvent e, float x,float y){ game.setScreen(new MenuScreen(game)); } });

        root.center().pad(20);
        root.add(retry).width(240).height(56).pad(6).row();
        root.add(menu).width(240).height(56).pad(6);
    }

    @Override public void render(float delta) {
        handleInputPauseMenu(); // keys & pause icon

        // Touch controls only when actively playing
        if (!paused && !gameOver) {
            controls.updateFromInput(viewport);
            world.getPlayer().setExternalMoveAxis(controls.getMoveAxis());
            world.getPlayer().setExternalFire(controls.isFiring());
        } else {
            world.getPlayer().setExternalMoveAxis(0f);
            world.getPlayer().setExternalFire(false);
        }

        // Freeze on pause or game over
        float dt = (!paused && !gameOver) ? delta * world.getTimeScale() : 0f;

        world.update(dt);
        boolean newGameOver = world.isGameOver();
        if (!gameOver && newGameOver) {
            gameOver = true;
            Gdx.input.setInputProcessor(overStage);
        } else if (gameOver && !newGameOver) {
            gameOver = newGameOver;
        }

        // Camera & clear
        world.applyCameraShake(cam, delta);
        viewport.apply();
        Gdx.gl.glClearColor(0.02f,0.02f,0.05f,1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        cam.update();

        // Background
        world.getStarfield().render(shapes, dt, cam.combined);

        // Sprites
        batch.setProjectionMatrix(cam.combined);
        batch.begin();
        world.renderSprites(batch);
        batch.end();

        // Shapes: bullets, powerups, shield
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapes.setProjectionMatrix(cam.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);

        shapes.setColor(Color.LIME);
        for (com.rgs.galaxyinvaders.entities.Bullet b : world.getPlayerBullets()) shapes.circle(b.cx(), b.cy(), b.radius(), 16);

        shapes.setColor(Color.RED);
        for (com.rgs.galaxyinvaders.entities.Bullet b : world.getEnemyBullets()) shapes.circle(b.cx(), b.cy(), b.radius(), 16);

        for (PowerUp p : world.getPowerUps()) p.render(shapes);

        world.renderShield(shapes);
        shapes.end();

        // Flash overlay (freezes with dt=0)
        world.renderFlash(shapes, dt);

        // On-screen joystick (hidden when paused or game over)
        if (showTouchUi && !paused && !gameOver) {
            Gdx.gl.glEnable(GL20.GL_BLEND);
            controls.render(shapes);
        }

        // Pause icon (always visible during play & pause; optional during game over)
        drawPauseButton();

        // Dim + UI stages
        if (paused) {
            drawDimOverlay(0.5f);
            pauseStage.act(delta);
            pauseStage.draw();
        } else if (gameOver) {
            drawDimOverlay(0.55f);
            overStage.act(delta);
            overStage.draw();
        }

        Gdx.gl.glDisable(GL20.GL_BLEND);

        // HUD (you already draw score/lives in world.renderHud)
        batch.begin();
        world.renderHud(batch, font);
        batch.end();
    }

    private void handleInputPauseMenu() {
        if (gameOver) {
            // Stage handles buttons; keep Esc for quick menu
            if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) game.setScreen(new MenuScreen(game));
            // ensure stage is input processor
            Gdx.input.setInputProcessor(overStage);
            return;
        }

        // Pause via icon tap
        if (!paused && Gdx.input.justTouched() && touchInCircle(pauseCenter.x, pauseCenter.y, pauseRadius)) {
            paused = true;
            Gdx.input.setInputProcessor(pauseStage);
            return;
        }

        // Keyboard pause / resume
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if (!paused) { paused = true; Gdx.input.setInputProcessor(pauseStage); return; }
            else { game.setScreen(new MenuScreen(game)); return; }
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.P)) {
            paused = !paused;
            Gdx.input.setInputProcessor(paused ? pauseStage : null);
            if (paused) return;
        }
    }

    private boolean touchInCircle(float cx, float cy, float r) {
        for (int p = 0; p < 20; p++) {
            if (!Gdx.input.isTouched(p)) continue;
            Vector2 w = viewport.unproject(new Vector2(Gdx.input.getX(p), Gdx.input.getY(p)));
            float dx = w.x - cx, dy = w.y - cy;
            if (dx*dx + dy*dy <= r*r) return true;
        }
        return false;
    }

    private void drawPauseButton() {
        shapes.setProjectionMatrix(cam.combined);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.15f, 0.15f, 0.2f, paused ? 0.9f : 0.55f);
        shapes.circle(pauseCenter.x, pauseCenter.y, pauseRadius + 4f, 28);
        shapes.setColor(0.25f, 0.25f, 0.35f, paused ? 0.95f : 0.75f);
        shapes.circle(pauseCenter.x, pauseCenter.y, pauseRadius, 28);
        // pause bars
        shapes.setColor(Color.WHITE);
        float barW = 4f, barH = 14f, gap = 6f;
        shapes.rect(pauseCenter.x - gap - barW, pauseCenter.y - barH/2f, barW, barH);
        shapes.rect(pauseCenter.x + gap,        pauseCenter.y - barH/2f, barW, barH);
        shapes.end();
    }

    private void drawDimOverlay(float alpha) {
        shapes.setProjectionMatrix(cam.combined);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0f, 0f, 0f, alpha);
        shapes.rect(0, 0, Constants.W, Constants.H);
        shapes.end();
    }

    @Override public void resize(int width, int height) {
        viewport.update(width, height, true);
        if (pauseStage != null) pauseStage.getViewport().update(width, height, true);
        if (overStage  != null) overStage.getViewport().update(width, height, true);
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
    @Override public void dispose() {
        batch.dispose(); shapes.dispose(); font.dispose();
        if (pauseStage!=null) pauseStage.dispose();
        if (overStage!=null)  overStage.dispose();
        if (skin!=null) skin.dispose();
    }
}
