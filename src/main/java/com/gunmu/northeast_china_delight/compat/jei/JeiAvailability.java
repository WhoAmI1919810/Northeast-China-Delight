package com.gunmu.northeast_china_delight.compat.jei;

import com.gunmu.chinese_regional_culinary_ingredients.region.CrciAvailability;
import com.gunmu.chinese_regional_culinary_ingredients.region.CrciUseSite;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Keeps disabled CRCI content out of every recipe page and ingredient list. */
final class JeiAvailability {
    static boolean available(ItemStack stack) {
        return !stack.isEmpty() && CrciAvailability.isAvailable(stack, CrciUseSite.JEI);
    }

    static boolean available(Item item) {
        return CrciAvailability.isAvailable(item, CrciUseSite.JEI);
    }

    static List<ItemStack> filter(Collection<ItemStack> stacks) {
        List<ItemStack> result = new ArrayList<>();
        for (ItemStack stack : stacks) {
            if (available(stack)) {
                result.add(stack);
            }
        }
        return List.copyOf(result);
    }

    static boolean anyAvailable(List<List<ItemStack>> slots) {
        for (List<ItemStack> slot : slots) {
            if (slot.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private JeiAvailability() {
    }
}
