package com.rgs.galaxyinvaders.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.rgs.galaxyinvaders.GalaxyInvadersGame;
import com.rgs.galaxyinvaders.config.Constants;
import com.rgs.galaxyinvaders.ui.UiFactory;

public class MenuScreen implements Screen {
    private final GalaxyInvadersGame game;
    private final OrthographicCamera cam = new OrthographicCamera();
    private final Viewport viewport = new FitViewport(Constants.W, Constants.H, cam);

    private Stage stage;
    private Skin skin;
    private Label highScoreLabel;
    private CheckBox muteCb;

    public MenuScreen(GalaxyInvadersGame game) { this.game = game; }

    @Override
    public void show() {
        cam.position.set(Constants.W/2f, Constants.H/2f, 0);
        viewport.apply(true);

        skin = UiFactory.createBasicSkin();
        stage = new Stage(viewport);
        Gdx.input.setInputProcessor(stage);

        // Root layout near top
        Table root = new Table();
        root.setFillParent(true);
        root.top().padTop(28);
        stage.addActor(root);

        // Title + High score
        Label title = UiFactory.title(skin, "Galaxy Invaders", 3.2f);
        title.setAlignment(Align.center);

        highScoreLabel = new Label("High Score: " + game.getHighScore(), skin);
        highScoreLabel.setAlignment(Align.center);
        highScoreLabel.setFontScale(1.2f);

        // Buttons
        TextButton startBtn = UiFactory.button(skin, "Start");
        TextButton quitBtn  = UiFactory.button(skin, "Quit");
        muteCb = UiFactory.checkbox(skin, "Mute", game.isMuted());
        muteCb.getLabel().setFontScale(1.15f);
        muteCb.getLabelCell().padRight(12f);   // a little spacing
        muteCb.clearChildren();                 // re-order children
        muteCb.add(muteCb.getLabel()).padRight(12f).left();
        muteCb.add(muteCb.getImage()).size(24f, 24f).right();

        startBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new GameScreen(game));
            }
        });
        quitBtn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                Gdx.app.exit();
            }
        });
        muteCb.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                game.setMuted(muteCb.isChecked());
            }
        });

        // Layout
        root.add(title).width(Constants.W).padBottom(8).row();
        root.add(highScoreLabel).width(Constants.W).padBottom(28).row();
        root.add(startBtn).width(240).height(56).padBottom(14).row();
        root.add(quitBtn).width(240).height(56).padBottom(14).row();    // Quit
        root.add(muteCb).width(240).height(52);  // transparent background checkbox

        game.assets.playMenuMusic();
        // reflect current mute state on music right away
        game.assets.applyCurrentVolumes();
    }

    @Override
    public void render(float delta) {
        // Keyboard shortcuts still work
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            game.setScreen(new GameScreen(game)); return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) { Gdx.app.exit(); return; }

        // Keep labels fresh
        highScoreLabel.setText("High Score: " + game.getHighScore());
        // keep checkbox synced if state changed elsewhere
        if (muteCb.isChecked() != game.isMuted()) muteCb.setChecked(game.isMuted());

        Gdx.gl.glClearColor(0.02f, 0.02f, 0.05f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        stage.act(delta);
        stage.draw();
    }

    @Override public void resize(int width, int height) { viewport.update(width, height, true); }
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
    @Override public void dispose() { if (stage!=null) stage.dispose(); if (skin!=null) skin.dispose(); }
}
