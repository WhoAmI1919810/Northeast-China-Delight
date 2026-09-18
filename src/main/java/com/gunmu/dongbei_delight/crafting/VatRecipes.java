package com.gunmu.dongbei_delight.crafting;

import com.gunmu.dongbei_delight.item.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * 大缸的全部规则集中在这里。
 *
 * 目前仍然写在代码里，但结构已经和将来的数据包配方一一对应，
 * 之后要做成自定义配方类型时把这张表换成 JSON 读取即可。
 */
public final class VatRecipes {

    private VatRecipes() {
    }

    /** 大缸当前在处理什么 */
    public enum Kind {
        /** 空缸 */
        NONE,
        /** 泡菜：水 + 盐 + 蔬菜，压石头 */
        PICKLE,
        /** 腊肉：肉与盐交替层叠，压石头，不加水 */
        MEAT,
        /** 大酱：大酱块 + 盐 + 水，蒙地毯 */
        PASTE,
        /** 酱油：大酱块 + 小麦 + 盐 + 满水，蒙地毯 */
        SOY_SAUCE,
        /** 水面团：碎玉米粒泡水 */
        DOUGH
    }

    /** 每一份水能泡几份蔬菜（一份水配两份菜） */
    public static final int VEGETABLES_PER_WATER = 2;
    /** 每一份水配几份盐 */
    public static final int SALT_PER_WATER = 1;
    /** 一缸最多放几块肉 */
    public static final int MAX_MEATS = 5;
    /** 一缸大酱能装几碗 */
    public static final int PASTE_SERVINGS = 10;
    /** 一缸酱油能装几瓶 */
    public static final int SOY_SAUCE_SERVINGS = 10;
    /** 大酱 / 酱油需要的酱块与盐的数量 */
    public static final int PASTE_CHUNKS = 3;
    public static final int PASTE_SALT = 3;

    /** 酱油额外需要的麦子 */
    public static Item wheatInput() {
        return Items.WHEAT;
    }

    /** 一个腌制单位 = 2 秒 = 40 tick；水量几层就是几个单位 */
    public static final int UNIT_TICKS = 40;

    private static Map<Item, Item> pickles;

    /** 泡菜：投入的蔬菜 → 腌制成品 */
    public static Map<Item, Item> pickles() {
        if (pickles == null) {
            Map<Item, Item> map = new HashMap<>();
            map.put(ModItems.NAPA_CABBAGE.get(), ModItems.SOUR_CABBAGE.get());
            map.put(ModItems.CUCUMBER.get(), ModItems.PICKLED_CUCUMBER.get());
            map.put(Items.CARROT, ModItems.PICKLED_CARROT.get());
            pickles = Map.copyOf(map);
        }
        return pickles;
    }

    public static boolean isPickleIngredient(ItemStack stack) {
        return pickles().containsKey(stack.getItem());
    }

    /** 腌制腊肉用的原料与成品 */
    public static Item meatInput() {
        return Items.PORKCHOP;
    }

    public static Item meatResult() {
        return ModItems.SALTED_PORK.get();
    }

    /** 水面团 */
    public static Item doughInput() {
        return ModItems.CORN_SEEDS.get();
    }

    public static Item doughResult() {
        return ModItems.WATER_DOUGH.get();
    }

    /**
     * 腌制/发酵完成后，缸里应该显示成品的模型而不是原料的模型。
     * 没有对应成品的物品（比如盐）返回 null。
     */
    @Nullable
    public static Item resultFor(Item input) {
        Item pickle = pickles().get(input);
        if (pickle != null) {
            return pickle;
        }
        if (input == meatInput()) {
            return meatResult();
        }
        if (input == doughInput()) {
            return doughResult();
        }
        return null;
    }
}
