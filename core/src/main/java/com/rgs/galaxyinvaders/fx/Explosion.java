package com.rgs.galaxyinvaders.fx;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import com.rgs.galaxyinvaders.assets.Assets;

public class Explosion {
    private final Array<TextureRegion> frames;
    private float x,y,t=0, frameDuration, scale;

    public Explosion(float cx, float cy, Assets.ExplosionSeq seq, float frameDuration, float scale) {
        this.x=cx; this.y=cy; this.frames=new Array<>(seq.frames);
        this.frameDuration = frameDuration; this.scale = scale;
    }

    public static Explosion big(float cx, float cy, com.rgs.galaxyinvaders.assets.Assets assets) {
        if (assets.explosions.size == 0) return null;
        Assets.ExplosionSeq seq = assets.explosions.get(0);
        return new Explosion(cx, cy, seq, 0.04f, 1.0f);
    }

    public boolean tick(float dt) { t += dt; return (int)(t / frameDuration) >= frames.size; }

    public void render(SpriteBatch sb) {
        int idx = (int)(t / frameDuration);
        if (idx >= frames.size) return;
        TextureRegion r = frames.get(idx);
        float w = r.getRegionWidth() * scale;
        float h = r.getRegionHeight() * scale;
        sb.draw(r, x - w/2f, y - h/2f, w, h);
    }
}
