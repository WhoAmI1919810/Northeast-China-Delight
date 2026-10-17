package com.gunmu.northeast_china_delight.item;

import com.gunmu.northeast_china_delight.crafting.VatRecipes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
//? if <1.21 {
/*import net.minecraft.world.item.enchantment.Enchantment;
*///?} else {
import net.minecraft.core.Holder;
import net.minecraft.world.item.enchantment.Enchantment;
//?}

/**
 * 瓶装调料：一瓶 = {@link #CAPACITY_MB} mB 液体，用一次扣 {@link #DOSE_MB} mB。剩余量直接用原版耐久（{@code DAMAGE}
 * 组件）记： damage = 已用掉的 mB，所以耐久条越用越短，用满 {@link #CAPACITY_MB} 就变成空玻璃瓶。
 */
public class SeasoningBottleItem extends Item {

    public static final int CAPACITY_MB = VatRecipes.SERVING_MB;
    public static final int DOSE_MB = 50;
    public static final int GRILL_DOSE_MB = 10;
    public static final int USES = CAPACITY_MB / DOSE_MB;

    public SeasoningBottleItem(Properties properties) {
        super(properties);
    }

    //? if <1.20.2 {
    /*@Override
    public net.minecraftforge.common.capabilities.ICapabilityProvider initCapabilities(
            ItemStack stack, net.minecraft.nbt.CompoundTag nbt) {
        // 1.20.1 没有 RegisterCapabilitiesEvent 的物品注册，流体能力从这里挂。
        // 没有对应流体的瓶子（食用油）返回 null，不暴露能力
        return SeasoningBottleFluidHandler.provider(stack);
    }
    *///?}

    /** 耐久只用来记剩余毫升，不是工具，不能附魔。 */
    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }

    @Override
    public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
        return false;
    }

    //? if <1.21 {
    /*@Override
    public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        return false;
    }
    *///?} else {
    @Override
    public boolean isPrimaryItemFor(ItemStack stack, Holder<Enchantment> enchantment) {
        return false;
    }

    @Override
    public boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
        return false;
    }
    //?}

    @Override
    public ItemStack getCraftingRemainingItem(ItemStack stack) {
        return useOnce(stack);
    }

    public static ItemStack useOnce(ItemStack stack) {
        return use(stack, DOSE_MB);
    }

    public static ItemStack use(ItemStack stack, int mb) {
        int used = stack.getDamageValue() + Math.max(0, mb);
        if (used >= CAPACITY_MB) {
            return new ItemStack(Items.GLASS_BOTTLE);
        }
        ItemStack left = stack.copyWithCount(1);
        left.setDamageValue(used);
        return left;
    }

    public static int usedMb(ItemStack stack) {
        return Math.min(CAPACITY_MB, Math.max(0, stack.getDamageValue()));
    }

    public static int remainingMb(ItemStack stack) {
        return CAPACITY_MB - usedMb(stack);
    }

    public static int remainingUses(ItemStack stack) {
        return remainingMb(stack) / DOSE_MB;
    }

    public static boolean isFull(ItemStack stack) {
        return usedMb(stack) <= 0;
    }

    public static boolean isBottle(ItemStack stack) {
        return stack.getItem() instanceof SeasoningBottleItem;
    }

    public static int refill(ItemStack stack, int mb) {
        int room = usedMb(stack);
        int moved = Math.min(room, Math.max(0, mb));
        if (moved > 0) {
            stack.setDamageValue(stack.getDamageValue() - moved);
        }
        return moved;
    }
}
