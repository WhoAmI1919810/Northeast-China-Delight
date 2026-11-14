package com.gunmu.northeast_china_delight.particle;

import com.gunmu.northeast_china_delight.NortheastChinaDelight;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
//? if <1.20.2 {
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
//?} else {
/*import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
*///?}

import java.util.function.Supplier;

/**
 * 本模组的粒子种类。粒子长什么样在客户端注册（NortheastChinaDelight.ClientModEvents#onRegisterParticles），
 * 贴图写在 assets/northeast_china_delight/particles/ 里。
 */
public final class ModParticles {

    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, NortheastChinaDelight.MODID);

    /**
     * 大铁锅汤面上冒的泡：原版气泡（bubble）的样子，
     * 但原版那个离了水当场就没了，锅里的汤不是真的水，所以另做一个，冒一会儿破掉（client/PotBubbleParticle）。
     * 原版 SimpleParticleType 的构造器是 protected，开个匿名子类
     */
    public static final Supplier<SimpleParticleType> POT_BUBBLE = PARTICLES.register("pot_bubble",
            () -> new SimpleParticleType(false) {
            });

    private ModParticles() {
    }

    public static void register(IEventBus eventBus) {
        PARTICLES.register(eventBus);
    }
}
