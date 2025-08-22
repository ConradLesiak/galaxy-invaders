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
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.rgs.galaxyinvaders.GalaxyInvadersGame;

public class MenuScreen implements Screen {
    private static final float W = 800f, H = 480f;

    private final GalaxyInvadersGame game;

    private final OrthographicCamera cam;
    private final Viewport viewport;             // <<< FitViewport
    private final SpriteBatch batch;
    private final ShapeRenderer shapes;
    private final BitmapFont font;
    private final GlyphLayout layout = new GlyphLayout();

    private final Starfield stars;
    private float t = 0f; // for subtle title animation

    public MenuScreen(GalaxyInvadersGame game) {
        this.game = game;

        cam = new OrthographicCamera();
        viewport = new FitViewport(W, H, cam);   // <<< letterboxed scaling
        viewport.apply(true);
        cam.position.set(W/2f, H/2f, 0);

        batch = new SpriteBatch();
        shapes = new ShapeRenderer();
        font = new BitmapFont();

        stars = new Starfield(220);
    }

    @Override public void render(float delta) {
        t += delta;

        // apply viewport & clear
        viewport.apply();
        Gdx.gl.glClearColor(0.02f, 0.02f, 0.05f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        cam.update();

        // handle input
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            game.setScreen(new GameScreen(game));
            return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            Gdx.app.exit();
            return;
        }

        // matrices
        shapes.setProjectionMatrix(cam.combined);
        batch.setProjectionMatrix(cam.combined);

        // background stars
        stars.render(shapes, delta);

        // HUD/text
        batch.begin();

        // Title
        String title = "Galaxy Invaders";
        layout.setText(font, title);
        float titleX = (W - layout.width) / 2f;
        float titleY = H - 120 + MathUtils.sin(t * 1.2f) * 4f; // gentle bob
        font.setColor(Color.WHITE);
        font.draw(batch, layout, titleX, titleY);

        // High score
        font.setColor(Color.LIME);
        font.draw(batch, "High Score: " + game.getHighScore(), 10, H - 12);
        font.setColor(Color.WHITE);

        // Press start (blink)
        float alpha = 0.5f + 0.5f * MathUtils.sin(t * 5f);
        font.setColor(1f, 1f, 1f, alpha);
        String press = "Press ENTER or Click to Start";
        layout.setText(font, press);
        font.draw(batch, layout, (W - layout.width)/2f, H * 0.45f);
        font.setColor(Color.WHITE);

        // Controls
        float y = 110;
        font.draw(batch, "Controls:", 30, y + 90);
        font.draw(batch, "Move: A/D or Arrow Keys", 30, y + 65);
        font.draw(batch, "Shoot: SPACE  •  Pause: P", 30, y + 40);
        font.draw(batch, "Menu: ESC", 30, y + 15);

        batch.end();
    }

    @Override public void resize(int width, int height) {
        viewport.update(width, height, true);    // <<< keep camera centered on resize
    }

    @Override public void show() {}
    @Override public void hide() {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void dispose() { batch.dispose(); shapes.dispose(); font.dispose(); }

    // Simple starfield (duplicated here so we don't depend on GameScreen internals)
    static class Starfield {
        static class Star { float x, y, spd, size; Color c; }
        final Array<Star> stars = new Array<>();

        Starfield(int count) {
            for (int i = 0; i < count; i++) {
                Star s = new Star();
                s.x = MathUtils.random(0, W);
                s.y = MathUtils.random(0, H);
                s.spd = MathUtils.random(30f, 180f);
                s.size = MathUtils.random(1.2f, 2.8f);
                float g = MathUtils.random(0.6f, 1f);
                s.c = new Color(g, g, 1f, 1f);
                stars.add(s);
            }
        }

        void render(ShapeRenderer sr, float dt) {
            // update
            for (Star s : stars) {
                s.y -= s.spd * dt;
                if (s.y < 0) { s.y = H; s.x = MathUtils.random(0, W); }
            }
            // draw
            sr.begin(ShapeRenderer.ShapeType.Filled);
            for (Star s : stars) {
                sr.setColor(s.c);
                sr.circle(s.x, s.y, s.size, 8);
            }
            sr.end();
        }
    }
}
