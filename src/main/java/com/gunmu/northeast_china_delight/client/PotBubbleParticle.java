package com.gunmu.northeast_china_delight.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * 大铁锅汤面上的泡：贴图就是原版气泡（particles/pot_bubble.json 指向
 * minecraft:bubble），大小同原版气泡那一档往大挑一点（锅有两格宽）。贴着汤面待半秒到一秒多、轻轻晃一下，
 * 到时候破掉，原地冒一个原版的 bubble_pop（破泡那几帧）。
 *
 * <p>原版气泡（BubbleParticle）每刻看自己是不是在水里，不在就当场消失，锅里的汤不是真的水方块，所以不能直接用。
 * 不跟方块碰撞（hasPhysics 关掉）：泡在灶这一格里面，开着碰撞会被整格的碰撞箱卡住。</p>
 */
public class PotBubbleParticle extends TextureSheetParticle {

    PotBubbleParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
        super(level, x, y, z);
        pickSprite(sprites);
        setSize(0.02F, 0.02F);
        // 原版气泡乘 0.2～0.8，这里 0.4～0.8
        quadSize *= random.nextFloat() * 0.4F + 0.4F;
        lifetime = 10 + random.nextInt(16);
        gravity = 0.0F;
        hasPhysics = false;
        xd = (random.nextDouble() - 0.5) * 0.004;
        yd = 0.0;
        zd = (random.nextDouble() - 0.5) * 0.004;
    }

    @Override
    public void tick() {
        xo = x;
        yo = y;
        zo = z;
        if (age++ >= lifetime) {
            remove();
            level.addParticle(ParticleTypes.BUBBLE_POP, x, y, z, 0.0, 0.0, 0.0);
            return;
        }
        move(xd, yd, zd);
        xd *= 0.9;
        zd *= 0.9;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                       double xSpeed, double ySpeed, double zSpeed) {
            return new PotBubbleParticle(level, x, y, z, sprites);
        }
    }
}
