package com.gunmu.northeast_china_delight.effect;

import com.gunmu.northeast_china_delight.NortheastChinaDelight;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 本模组的两个"口味"状态效果。具体机制都在 {@link DishEffectEvents} 里实现，
 * 这里的 MobEffect 只是用来记录等级（等级决定百分比）。
 */
public class ModEffects {

    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, NortheastChinaDelight.MODID);

    /** 油腻：吃完油腻菜获得。进食变慢，但饱食度与饱和度掉得慢 */
    public static final DeferredHolder<MobEffect, MobEffect> GREASY =
            EFFECTS.register("greasy", () -> new SimpleEffect(MobEffectCategory.NEUTRAL, 0x8A5A2B));

    /** 爽口：吃完爽口的菜获得。进食变慢，但回血更快 */
    public static final DeferredHolder<MobEffect, MobEffect> REFRESHING =
            EFFECTS.register("refreshing", () -> new SimpleEffect(MobEffectCategory.BENEFICIAL, 0x7AC74F));

    /**
     * 暖身：汤类料理的效果。在寒冷覆雪的生物群系里移动速度 +5%×等级，并且不会陷进细雪。
     * 具体机制在 {@link DishEffectEvents} 里实现（按等级挂移动速度修饰符），
     * 「不会陷细雪」由 mixin 打在原版细雪方块上。
     */
    public static final DeferredHolder<MobEffect, MobEffect> WARMTH =
            EFFECTS.register("warmth", () -> new SimpleEffect(MobEffectCategory.BENEFICIAL, 0xFF8A3D));

    /** 养生：参鸡汤的效果，按当前血量分档回血（见 {@link NourishingEffect}） */
    public static final DeferredHolder<MobEffect, MobEffect> NOURISHING =
            EFFECTS.register("nourishing", NourishingEffect::new);

    public static void register(IEventBus eventBus) {
        EFFECTS.register(eventBus);
    }

    /** MobEffect 的构造器是 protected，包外想 new 就得先继承一层 */
    public static class SimpleEffect extends MobEffect {
        public SimpleEffect(MobEffectCategory category, int color) {
            super(category, color);
        }
    }
}
