package com.rgs.galaxyinvaders.config;

public final class Constants {
    private Constants() {}

    // Virtual world size
    public static final float W = 800f;
    public static final float H = 480f;

    // Gameplay knobs
    public static final int   BOSS_BULLET_LIMIT   = 60;
    public static final float BULLET_HITBOX_SCALE = 0.60f;
    public static final float BOSS_MISS_DEG       = 10f;
    public static final float ATTACK_SLOW_FACTOR  = 1.50f;

    public static final int   BOSS_HIT_SCORE      = 10;
    public static final float BOSS_HIT_POWERUP_CHANCE = 0.20f;
}
