package com.gunmu.northeast_china_delight.crafting;

import com.gunmu.northeast_china_delight.item.SeasoningBottleItem;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
//? if <1.20.5 {
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
//?} else {
/*import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.crafting.CraftingInput;
*///?}

import java.util.ArrayList;
import java.util.List;

/**
 * 工作台里把几瓶没装满的同种瓶装调料倒在一起（{@code northeast_china_delight:seasoning_bottle_merge}）。
 *
 * <p>成品是各瓶剩余量之和，最多一满瓶 250 mB，不像原版修理那样多送 5%。其余每个瓶子都退回：
 * 倒空的退空玻璃瓶，一瓶装不下的部分依次留在退回的瓶子里，所以玻璃瓶一个也不会被吞掉。</p>
 *
 * <p>原版修理配方、砂轮、铁砧对瓶装调料都不起作用，见 {@code RepairItemRecipeMixin} 和 {@code ModGameplayEvents}。</p>
 */
public class SeasoningBottleMergeRecipe extends CustomRecipe {

    //? if <1.20.5 {
    public SeasoningBottleMergeRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }
    //?} else {
    /*public SeasoningBottleMergeRecipe(CraftingBookCategory category) {
        super(category);
    }
    *///?}

    @Override
    //? if <1.20.5 {
    public boolean matches(CraftingContainer input, Level level) {
    //?} else {
    /*public boolean matches(CraftingInput input, Level level) {
    *///?}
        return bottleSlots(stacks(input)) != null;
    }

    @Override
    //? if <1.20.5 {
    public ItemStack assemble(CraftingContainer input, RegistryAccess registries) {
    //?} else {
    /*public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
    *///?}
        List<ItemStack> stacks = stacks(input);
        List<Integer> slots = bottleSlots(stacks);
        if (slots == null) {
            return ItemStack.EMPTY;
        }
        return bottleWith(stacks.get(slots.get(0)), Math.min(SeasoningBottleItem.CAPACITY_MB, totalMb(stacks, slots)));
    }

    @Override
    //? if <1.20.5 {
    public NonNullList<ItemStack> getRemainingItems(CraftingContainer input) {
    //?} else {
    /*public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
    *///?}
        List<ItemStack> stacks = stacks(input);
        NonNullList<ItemStack> remaining = NonNullList.withSize(stacks.size(), ItemStack.EMPTY);
        List<Integer> slots = bottleSlots(stacks);
        if (slots == null) {
            return remaining;
        }
        // 第一瓶并进成品，其余每格退一个瓶子；每瓶都不满，所以剩下的量一定装得进退回的这些瓶子
        int leftover = Math.max(0, totalMb(stacks, slots) - SeasoningBottleItem.CAPACITY_MB);
        for (int i = 1; i < slots.size(); i++) {
            int mb = Math.min(leftover, SeasoningBottleItem.CAPACITY_MB);
            leftover -= mb;
            remaining.set(slots.get(i), mb > 0
                    ? bottleWith(stacks.get(slots.get(0)), mb)
                    : new ItemStack(Items.GLASS_BOTTLE));
        }
        return remaining;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModCraftingRecipes.SEASONING_BOTTLE_MERGE.get();
    }

    //? if <1.20.5 {
    private static List<ItemStack> stacks(CraftingContainer input) {
        List<ItemStack> stacks = new ArrayList<>(input.getContainerSize());
        for (int slot = 0; slot < input.getContainerSize(); slot++) {
            stacks.add(input.getItem(slot));
        }
        return stacks;
    }
    //?} else {
    /*private static List<ItemStack> stacks(CraftingInput input) {
        List<ItemStack> stacks = new ArrayList<>(input.size());
        for (int slot = 0; slot < input.size(); slot++) {
            stacks.add(input.getItem(slot));
        }
        return stacks;
    }
    *///?}

    /** 格子里只有同一种、都没装满的瓶装调料且至少两瓶时，返回它们所在的格子；否则 null */
    @Nullable
    private static List<Integer> bottleSlots(List<ItemStack> stacks) {
        List<Integer> slots = new ArrayList<>();
        ItemStack first = null;
        for (int slot = 0; slot < stacks.size(); slot++) {
            ItemStack stack = stacks.get(slot);
            if (stack.isEmpty()) {
                continue;
            }
            if (!SeasoningBottleItem.isBottle(stack) || stack.getCount() != 1 || SeasoningBottleItem.isFull(stack)) {
                return null;
            }
            if (first == null) {
                first = stack;
            } else if (!stack.is(first.getItem())) {
                return null;
            }
            slots.add(slot);
        }
        return slots.size() >= 2 ? slots : null;
    }

    private static int totalMb(List<ItemStack> stacks, List<Integer> slots) {
        int total = 0;
        for (int slot : slots) {
            total += SeasoningBottleItem.remainingMb(stacks.get(slot));
        }
        return total;
    }

    private static ItemStack bottleWith(ItemStack kind, int mb) {
        ItemStack bottle = new ItemStack(kind.getItem());
        bottle.setDamageValue(SeasoningBottleItem.CAPACITY_MB - mb);
        return bottle;
    }
}
