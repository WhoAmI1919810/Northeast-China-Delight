package com.gunmu.northeast_china_delight.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
//? if <1.20.5 {
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
//?}

import java.util.List;
import java.util.Locale;

/**
 * 盆装谜之炖菜（2026-11-05 用户）：大锅里凑不上配方时煮出来、用大脸盆盛的一盆。放下、拿碗盛同别的盆装菜，
 * 盛出来是一碗原版谜之炖菜；给什么效果存在物品上（{@link MysteryStewEffects}）。
 *
 * <p>提示框同原版谜之炖菜：只有创造模式能看见效果，生存模式里吃了才知道。</p>
 */
public class MysteryStewItem extends DdFeastItem {

    public MysteryStewItem(Block block, Item.Properties properties) {
        super(block, properties);
    }

    @Override
    //? if <1.20.5 {
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        addEffectLines(stack, tooltip, flag);
    }
    //?} else {
    /*public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        addEffectLines(stack, tooltip, flag);
    }
    *///?}

    private static void addEffectLines(ItemStack stack, List<Component> tooltip, TooltipFlag flag) {
        if (!flag.isCreative()) {
            return;
        }
        for (MysteryStewEffects.Entry entry : MysteryStewEffects.read(stack)) {
            MobEffect effect = entry.effect().value();
            tooltip.add(Component.translatable("tooltip.northeast_china_delight.mystery_stew.effect",
                            Component.translatable(effect.getDescriptionId()), seconds(entry.duration()))
                    .withStyle(effect.getCategory().getTooltipFormatting()));
        }
    }

    /** 刻 → 秒：整秒不带小数（4、6），不是整秒的写两位（饱和 7 刻 = 0.35） */
    private static String seconds(int ticks) {
        return ticks % 20 == 0 ? Integer.toString(ticks / 20) : String.format(Locale.ROOT, "%.2f", ticks / 20.0F);
    }
}
