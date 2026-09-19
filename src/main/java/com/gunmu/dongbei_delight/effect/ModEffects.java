package com.gunmu.dongbei_delight.effect;

import com.gunmu.dongbei_delight.DongbeiDelight;
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
            DeferredRegister.create(Registries.MOB_EFFECT, DongbeiDelight.MODID);

    /** 油腻：吃完油腻菜获得。进食变慢，但饱食度与饱和度掉得慢 */
    public static final DeferredHolder<MobEffect, MobEffect> GREASY =
            EFFECTS.register("greasy", () -> new SimpleEffect(MobEffectCategory.NEUTRAL, 0x8A5A2B));

    /** 爽口：吃完爽口的菜获得。进食变慢，但回血更快 */
    public static final DeferredHolder<MobEffect, MobEffect> REFRESHING =
            EFFECTS.register("refreshing", () -> new SimpleEffect(MobEffectCategory.BENEFICIAL, 0x7AC74F));

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
