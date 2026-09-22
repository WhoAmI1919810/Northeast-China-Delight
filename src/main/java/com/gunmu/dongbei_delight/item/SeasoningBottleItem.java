package com.gunmu.dongbei_delight.item;

import com.gunmu.dongbei_delight.crafting.VatRecipes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 瓶装调料：一瓶 = {@link #CAPACITY_MB} mB 液体，用一次扣 {@link #DOSE_MB} mB。
 *
 * <p>剩余量直接用原版耐久（{@code DAMAGE} 组件）记：
 * damage = 已用掉的 mB，所以耐久条越用越短，用满 {@link #CAPACITY_MB} 就变成空玻璃瓶。
 *
 * <p><b>厨锅 / 工作台怎么拿到"用过一半的瓶子"</b>：
 * 农夫乐事的厨锅在完成时会调用 {@code ItemStack#getCraftingRemainingItem()}，
 * 那是 NeoForge 的**按 stack 定制**版本，会转发到
 * {@link #getCraftingRemainingItem(ItemStack)}，所以这里能返回扣过量的瓶子。
 * （原版那个不带参数的 {@code Item#getCraftingRemainingItem()} 是 final，改不了，
 * 所以物品属性里仍然保留 {@code craftRemainder(玻璃瓶)}，
 * 一来让 {@code hasCraftingRemainingItem()} 为真，二来给不认 NeoForge 扩展的地方一个兜底。）
 */
public class SeasoningBottleItem extends Item {

    /** 一瓶的容量，和大缸的一份（250 mB）对齐 */
    public static final int CAPACITY_MB = VatRecipes.SERVING_MB;
    /** 每次使用消耗多少 mB */
    public static final int DOSE_MB = 50;
    /** 烧烤时给**一份**食材刷一次只花这么多（比下锅省） */
    public static final int GRILL_DOSE_MB = 10;
    /** 一瓶能用几次 */
    public static final int USES = CAPACITY_MB / DOSE_MB;

    public SeasoningBottleItem(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack getCraftingRemainingItem(ItemStack stack) {
        return useOnce(stack);
    }

    /** 用掉一次：还有剩就返回用过一次的瓶子，正好用空就变成玻璃瓶 */
    public static ItemStack useOnce(ItemStack stack) {
        return use(stack, DOSE_MB);
    }

    /** 用掉 mb：还有剩就返回用过的瓶子，正好用空就变成玻璃瓶 */
    public static ItemStack use(ItemStack stack, int mb) {
        int used = stack.getDamageValue() + Math.max(0, mb);
        if (used >= CAPACITY_MB) {
            return new ItemStack(Items.GLASS_BOTTLE);
        }
        ItemStack left = stack.copyWithCount(1);
        left.setDamageValue(used);
        return left;
    }

    /** 已用掉多少 mB（= 耐久条上掉下去的部分） */
    public static int usedMb(ItemStack stack) {
        return Math.min(CAPACITY_MB, Math.max(0, stack.getDamageValue()));
    }

    /** 还剩多少 mB */
    public static int remainingMb(ItemStack stack) {
        return CAPACITY_MB - usedMb(stack);
    }

    /** 还能用几次 */
    public static int remainingUses(ItemStack stack) {
        return remainingMb(stack) / DOSE_MB;
    }

    public static boolean isFull(ItemStack stack) {
        return usedMb(stack) <= 0;
    }

    public static boolean isBottle(ItemStack stack) {
        return stack.getItem() instanceof SeasoningBottleItem;
    }

    /**
     * 往瓶子里补 mb（不超过容量），返回实际补进去的量。
     * 大缸那边「缺多少补多少、缸里不够就补多少」就是靠这个返回值对齐。
     */
    public static int refill(ItemStack stack, int mb) {
        int room = usedMb(stack);
        int moved = Math.min(room, Math.max(0, mb));
        if (moved > 0) {
            stack.setDamageValue(stack.getDamageValue() - moved);
        }
        return moved;
    }
}
