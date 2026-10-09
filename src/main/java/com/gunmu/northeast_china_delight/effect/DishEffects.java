package com.gunmu.northeast_china_delight.effect;

import com.gunmu.northeast_china_delight.util.DdIds;
import com.gunmu.northeast_china_delight.util.DdEffects;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.ArrayList;
import java.util.List;

/**
 * 一道菜吃完到底给哪些状态效果——进食逻辑和食物提示框共用这一份清单，
 * 保证"提示框里写的"和"实际给的"永远一致。
 */
public final class DishEffects {

    /** 农夫乐事的滋养（可选模组，按 id 查） */
    private static final ResourceLocation NOURISHMENT =
            DdIds.of("farmersdelight", "nourishment");
    /** 一条待施加的效果 */
    public record Applied(Holder<MobEffect> effect, int durationTicks, int amplifier) {
        public MobEffectInstance instance() {
            return DdEffects.instance(this.effect, this.durationTicks, this.amplifier);
        }
    }

    private DishEffects() {
    }

    /** 按口味给出吃完的效果列表 */
    public static List<Applied> of(DishFlavors.Info info) {
        int duration = info.durationTicks();
        List<Applied> list = new ArrayList<>();
        switch (info.flavor()) {
            case GREASY -> {
                list.add(new Applied(DdEffects.hold(MobEffects.MOVEMENT_SLOWDOWN), duration, 0));
                list.add(new Applied(DdEffects.hold(MobEffects.HEALTH_BOOST), duration, 1));
                list.add(new Applied(DdEffects.hold(MobEffects.DIG_SPEED), duration, 0));
                list.add(new Applied(DdEffects.hold(ModEffects.GREASY), duration, 1));
            }
            case REFRESHING -> {
                list.add(new Applied(DdEffects.hold(MobEffects.MOVEMENT_SPEED), duration, 0));
                list.add(new Applied(DdEffects.hold(ModEffects.REFRESHING), duration, 0));
            }
            case BALANCED -> DdEffects.holder(NOURISHMENT)
                    .ifPresent(effect -> list.add(new Applied(effect, duration, 0)));
            // 暖身汤：效果只有「暖身」，由汤类名单在进食事件里统一给，这里不加口味效果
            case WARM -> {
            }
        }
        return list;
    }

    /** 新派橙汁锅包肉额外带的效果：橘子汁本身的 5 秒生命恢复（不含解毒） */
    public static List<Applied> orangeJuiceExtras() {
        List<Applied> list = new ArrayList<>();
        list.add(new Applied(DdEffects.hold(MobEffects.REGENERATION), 100, 0));
        return list;
    }

    /** 「养生」短效果：参鸡汤、葱烧海参这类滋补菜额外给 10 秒（200 tick），效果本身按血量分档回血 */
    public static List<Applied> nourishingExtras() {
        List<Applied> list = new ArrayList<>();
        list.add(new Applied(DdEffects.hold(ModEffects.NOURISHING), 200, 0));
        return list;
    }

    /** 额外给的「暖身」：时长由 {@link DishFlavors#warmthTicks} 决定 */
    public static List<Applied> warmth(int durationTicks) {
        List<Applied> list = new ArrayList<>();
        list.add(new Applied(DdEffects.hold(ModEffects.WARMTH), durationTicks, 0));
        return list;
    }
}
