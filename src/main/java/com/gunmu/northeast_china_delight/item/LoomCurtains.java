package com.gunmu.northeast_china_delight.item;

import com.gunmu.northeast_china_delight.block.ModBlocks;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 织布机染门帘：普通门帘放进织布机的旗帜格、染料放进染料格，结果格出染好的门帘
 * （蓝色染料 → 棉门帘，红色染料 → 大花布门帘，绿色染料 → 军绿棉门帘）。取走结果时原版织布机自己会扣掉一个门帘和一份染料。
 *
 * <p>织布机原版只认旗帜，这里的判断由三个 mixin 调用：
 * {@code LoomBannerSlotMixin}（旗帜格放得进普通门帘）、{@code LoomMenuMixin}（算结果）、
 * {@code LoomScreenMixin}（客户端别把门帘当旗帜画预览）。</p>
 */
public final class LoomCurtains {

    private LoomCurtains() {
    }

    public static boolean isPlainCurtain(ItemStack stack) {
        return stack.is(ModBlocks.PLAIN_DOOR_CURTAIN.get().asItem());
    }

    /** 染料对应的门帘；不认识的染料给空 */
    public static ItemStack dyedCurtain(ItemStack dye) {
        if (dye.is(Items.BLUE_DYE)) {
            return new ItemStack(ModBlocks.COTTON_DOOR_CURTAIN.get());
        }
        if (dye.is(Items.RED_DYE)) {
            return new ItemStack(ModBlocks.FLORAL_DOOR_CURTAIN.get());
        }
        if (dye.is(Items.GREEN_DYE)) {
            return new ItemStack(ModBlocks.ARMY_GREEN_DOOR_CURTAIN.get());
        }
        return ItemStack.EMPTY;
    }
}
