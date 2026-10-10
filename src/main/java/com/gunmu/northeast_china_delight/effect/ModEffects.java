package com.gunmu.northeast_china_delight.effect;

import com.gunmu.northeast_china_delight.NortheastChinaDelight;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
//? if <1.20.2 {
import net.minecraftforge.eventbus.api.IEventBus;
//?} else {
/*import net.neoforged.bus.api.IEventBus;
*///?}
//? if <1.20.2 {
import net.minecraftforge.registries.DeferredRegister;
//?} else {
/*import net.neoforged.neoforge.registries.DeferredRegister;
*///?}
import java.util.function.Supplier;

/**
 * 本模组的两个"口味"状态效果。具体机制都在 {@link DishEffectEvents} 里实现，这里的 MobEffect 只是用来记录等级（等级决定百分比）。
 */
public class ModEffects {

    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, NortheastChinaDelight.MODID);

    public static final Supplier<MobEffect> GREASY =
            EFFECTS.register("greasy", () -> new SimpleEffect(MobEffectCategory.NEUTRAL, 0x8A5A2B));

    public static final Supplier<MobEffect> REFRESHING =
            EFFECTS.register("refreshing", () -> new SimpleEffect(MobEffectCategory.BENEFICIAL, 0x7AC74F));

    public static final Supplier<MobEffect> WARMTH =
            EFFECTS.register("warmth", () -> new SimpleEffect(MobEffectCategory.BENEFICIAL, 0xFF8A3D));

    public static final Supplier<MobEffect> NOURISHING =
            EFFECTS.register("nourishing", NourishingEffect::new);

    public static void register(IEventBus eventBus) {
        EFFECTS.register(eventBus);
    }

    /**
     * MobEffect 的构造器是 protected，包外想 new 就得先继承一层
     */
    public static class SimpleEffect extends MobEffect {
        public SimpleEffect(MobEffectCategory category, int color) {
            super(category, color);
        }
    }
}
