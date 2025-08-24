package com.rgs.galaxyinvaders.world;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.rgs.galaxyinvaders.assets.Assets;
import com.rgs.galaxyinvaders.config.Constants;
import com.rgs.galaxyinvaders.entities.Enemy;

public final class Spawner {
    private Spawner() {}
    public static void spawnEnemies(GameWorld world, Assets assets, int count, int wave, float diffEnemySpeed, float diffEnemyFire) {
        for (int i = 0; i < count; i++) {
            int type = MathUtils.random(0, 2); // 0 sine, 1 shooter, 2 chaser
            float x = MathUtils.random(40, Constants.W - 80);
            float y = Constants.H + MathUtils.random(30, 200);
            TextureRegion sprite = (assets.enemyShips.size > 0) ? assets.enemyShips.random() :
                (assets.playerShip != null ? assets.playerShip : assets.white1x1);
            world.spawnEnemy(new Enemy(world, sprite, type, x, y, wave, diffEnemySpeed, diffEnemyFire));
        }
    }
}
