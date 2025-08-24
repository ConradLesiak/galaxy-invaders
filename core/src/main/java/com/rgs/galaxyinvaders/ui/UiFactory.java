package com.rgs.galaxyinvaders.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox.CheckBoxStyle;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

public final class UiFactory {
    private UiFactory() {}

    public static Skin createBasicSkin() {
        Skin skin = new Skin();

        // Font
        BitmapFont font = new BitmapFont();
        skin.add("default-font", font, BitmapFont.class);

        // --- TextButton styles (same as before) ---
        NinePatchDrawable up   = new NinePatchDrawable(makePatch(0.18f, 0.22f, 0.30f, 0.95f));
        NinePatchDrawable over = new NinePatchDrawable(makePatch(0.22f, 0.26f, 0.36f, 0.95f));
        NinePatchDrawable down = new NinePatchDrawable(makePatch(0.12f, 0.16f, 0.24f, 0.95f));

        TextButtonStyle tbs = new TextButtonStyle();
        tbs.font = font;
        tbs.fontColor = Color.WHITE;
        tbs.up = up; tbs.over = over; tbs.down = down;
        skin.add("default", tbs);

        // --- Label style ---
        Label.LabelStyle ls = new Label.LabelStyle();
        ls.font = font;
        ls.fontColor = Color.WHITE;
        skin.add("default", ls);

        // --- CheckBox style (transparent background; just box + check) ---
        Texture cbOffTex = makeCheckboxTexture(false);
        Texture cbOnTex  = makeCheckboxTexture(true);
        // add textures so Skin disposes them later
        skin.add("cb-off-tex", cbOffTex, Texture.class);
        skin.add("cb-on-tex",  cbOnTex,  Texture.class);

        Drawable cbOff = new TextureRegionDrawable(new TextureRegion(cbOffTex));
        Drawable cbOn  = new TextureRegionDrawable(new TextureRegion(cbOnTex));

        CheckBoxStyle cbs = new CheckBoxStyle();
        cbs.checkboxOff = cbOff;
        cbs.checkboxOn  = cbOn;
        cbs.font = font;
        cbs.fontColor = Color.WHITE;
        skin.add("default", cbs); // same name "default" but different class is OK

        return skin;
    }

    private static NinePatch makePatch(float r, float g, float b, float a) {
        int split = 8;
        Pixmap pm = new Pixmap(64, 64, Pixmap.Format.RGBA8888);
        pm.setColor(r, g, b, a);
        pm.fill();
        Texture tex = new Texture(pm);
        pm.dispose();
        return new NinePatch(tex, split, split, split, split);
    }

    // Draw a 38×38 checkbox with a border; the "on" variant has a white box + green check
    private static Texture makeCheckboxTexture(boolean checked) {
        int S = 38;
        Pixmap pm = new Pixmap(S, S, Pixmap.Format.RGBA8888);
        pm.setColor(0,0,0,0); pm.fill(); // fully transparent background

        // box outline
        pm.setColor(1f, 1f, 1f, 0.95f);
        pm.drawRectangle(1, 1, S-2, S-2);

        if (checked) {
            // green check: two thick lines
            pm.setColor(0.3f, 1f, 0.4f, 1f);
            // arm 1
            for (int i = 0; i < 3; i++) pm.drawLine(8, 20+i, 16, 28+i);
            // arm 2
            for (int i = 0; i < 3; i++) pm.drawLine(16, 28+i, 30, 12+i);
        }
        Texture t = new Texture(pm);
        pm.dispose();
        return t;
    }

    public static TextButton button(Skin skin, String text) {
        return new TextButton(text, skin, "default");
    }

    public static Label title(Skin skin, String text, float scale) {
        Label l = new Label(text, skin);
        l.setFontScale(scale);
        return l;
    }

    public static CheckBox checkbox(Skin skin, String text, boolean checked) {
        CheckBox cb = new CheckBox(text, skin); // uses "default" CheckBoxStyle we added
        cb.setChecked(checked);
        return cb;
    }
}
