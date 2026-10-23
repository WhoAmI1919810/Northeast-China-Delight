package com.gunmu.northeast_china_delight.crafting;

import com.gunmu.northeast_china_delight.item.ModItems;
import com.gunmu.northeast_china_delight.ModTags;
import com.gunmu.northeast_china_delight.block.VatBlockEntity;
import com.gunmu.northeast_china_delight.util.DdIds;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 大缸的共享辅助规则；配方本体由数据包加载，ID 即资源路径。
 */
public final class VatRecipes {

    private VatRecipes() {
    }

    /**
     * 大缸当前在处理什么
     */
    public enum Kind {
        NONE,
        PICKLE,
        MEAT,
        SALTED_FISH,
        PASTE,
        SOY_SAUCE,
        VINEGAR,
        WHITE_VINEGAR,
        FISH_SAUCE,
        SHRIMP_PASTE,
        SPICY_PICKLE,
        BEAN_SPROUTS,
        SOUR_CORN,
        KVASS,
        FROZEN_PEAR,
        FROZEN_TOFU
    }

    public static final int VEGETABLES_PER_WATER = 2;
    public static final int SALT_PER_WATER = 1;
    public static final int MAX_MEATS = 6;
    public static final int SALTED_FISH_FISH = 3;
    public static final int SALTED_FISH_SALT = 3;
    public static final int SALTED_SALT_MAX = 3;
    public static final int PASTE_SERVINGS = 10;
    public static final int SOY_SAUCE_SERVINGS = 10;
    public static final int VINEGAR_SERVINGS = 10;
    public static final int WHITE_VINEGAR_SERVINGS = 10;
    public static final int WATER_MB_PER_LEVEL = 1000;
    public static final int SERVING_MB = 250;
    /** 白醋要喝的酸引水 = 一整缸（3 层 = 3000 mB） */
    public static final int SOUR_WATER_MB = 3 * WATER_MB_PER_LEVEL;
    public static final int FISH_SAUCE_FISH = 3;
    public static final int FISH_SAUCE_SALT = 6;
    public static final int FISH_SAUCE_SERVINGS = 1;
    public static final int SHRIMP_PASTE_SHRIMP = 6;
    public static final int SHRIMP_PASTE_SALT = 3;
    public static final int SHRIMP_PASTE_SERVINGS = 1;
    public static final int SPICY_SAUCE_MAX = 1;
    public static final int SPICY_SEASONING_MAX = 2;
    public static final int SPROUT_SOYBEAN_MAX = 2;
    public static final int SOUR_CORN_PER_WATER = 2;
    public static int spicySeasoningNeed(int waterLevel) {
        return waterLevel >= 3 ? 2 : 1;
    }
    public static final int KVASS_BREAD = 6;
    public static final int KVASS_WATER_LEVEL = 3;
    public static final int KVASS_SERVINGS = 6;
    public static final int PRODUCT_CAPACITY_MB = PASTE_SERVINGS * SERVING_MB;
    public static final int PASTE_CHUNKS = 3;
    public static final int PASTE_SALT = 3;
    public static final int RESIDUE_COUNT = 3;
    public static final int VINEGAR_GRAIN_COUNT = 3;

    public static boolean isWheat(ItemStack stack) {
        return stack.is(ModTags.CROPS_WHEAT);
    }

    public static boolean isPear(ItemStack stack) {
        return stack.is(ModTags.FOODS_FRUITS_PEAR);
    }

    public static final int PICKLE_SECONDS_PER_LAYER = 60;
    public static final int SPICY_PICKLE_SECONDS_PER_LAYER = 60;
    public static final int MEAT_SECONDS = 120;
    public static final int SALTED_FISH_SECONDS = 120;
    public static final int PASTE_SECONDS = 300;
    public static final int SOY_SAUCE_SECONDS = 300;
    public static final int VINEGAR_SECONDS = 240;
    public static final int WHITE_VINEGAR_SECONDS = 180;
    public static final int FISH_SAUCE_SECONDS = 240;
    public static final int SHRIMP_PASTE_SECONDS = 240;
    public static final int BEAN_SPROUTS_SECONDS = 30;
    public static final int SOUR_CORN_SECONDS = 60;
    public static final int KVASS_SECONDS = 60;
    public static final int FROZEN_PEAR_SECONDS = 30;
    public static final int FROZEN_TOFU_SECONDS = 30;

    public static final int TICKS_PER_SECOND = 20;

    private static Map<Item, Item> pickles;

    public static Map<Item, Item> pickles() {
        if (pickles == null) {
            Map<Item, Item> map = new HashMap<>();
            map.put(ModItems.NAPA_CABBAGE.get(), ModItems.SOUR_CABBAGE.get());
            map.put(ModItems.SNOW_MUSTARD.get(), ModItems.PICKLED_SNOW_MUSTARD.get());
            map.put(ModItems.CUCUMBER.get(), ModItems.PICKLED_CUCUMBER.get());
            map.put(Items.CARROT, ModItems.PICKLED_CARROT.get());
            map.put(ModItems.GREEN_RADISH.get(), ModItems.PICKLED_GREEN_RADISH.get());
            pickles = Map.copyOf(map);
        }
        return pickles;
    }

    /**
     * 泡菜能用的蔬菜：本模组自己的那几样，或者别家塞进对应通用标签的同类菜。
     *
     * <p>{@code c:crops/cabbage}（卷心菜 / 卷心菜叶）特意不收 —— 酸菜是大白菜腌的，
     * 卷心菜腌出来不该叫酸菜；别家的大白菜往 {@code c:crops/napa_cabbage} 里塞就行。
     */
    public static boolean isPickleIngredient(ItemStack stack) {
        return pickles().containsKey(stack.getItem())
                || stack.is(ModTags.CROPS_NAPA_CABBAGE)
                || stack.is(ModTags.CROPS_SNOW_MUSTARD)
                || stack.is(ModTags.CROPS_CUCUMBER)
                || stack.is(ModTags.CROPS_CARROT)
                || stack.is(ModTags.CROPS_RADISH);
    }

    static ItemStack convertPickle(ItemStack stack) {
        Item result = pickles().get(stack.getItem());
        if (result != null) {
            return new ItemStack(result);
        }
        if (stack.is(ModTags.CROPS_SNOW_MUSTARD)) return new ItemStack(ModItems.PICKLED_SNOW_MUSTARD.get());
        if (stack.is(ModTags.CROPS_CUCUMBER)) return new ItemStack(ModItems.PICKLED_CUCUMBER.get());
        if (stack.is(ModTags.CROPS_CARROT)) return new ItemStack(ModItems.PICKLED_CARROT.get());
        if (stack.is(ModTags.CROPS_RADISH)) return new ItemStack(ModItems.PICKLED_GREEN_RADISH.get());
        return new ItemStack(ModItems.SOUR_CABBAGE.get());
    }

    public static boolean isMeatInput(ItemStack stack) {
        return stack.is(ModTags.FOODS_RAW_PORK) && !isBacon(stack);
    }

    private static boolean isBacon(ItemStack stack) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id.getPath().equals("bacon") || id.getPath().endsWith("_bacon");
    }

    public static Item meatResult() {
        return ModItems.SALTED_PORK.get();
    }

    public static Item saltedFishResult() {
        return ModItems.SALTED_FISH.get();
    }

    public static boolean isCornKernels(ItemStack stack) {
        return stack.is(ModTags.SEEDS_CORN);
    }

    public static boolean isVinegarGrain(ItemStack stack) {
        return stack.is(ModTags.SEEDS_CORN) || stack.is(ModTags.CROPS_BUCKWHEAT);
    }

    /** 生鱼：虾 / 生蚝 / 海参不算，切好的鱼片也不算（免得鱼片腌完又变回整条） */
    public static boolean isRawFish(ItemStack stack) {
        return stack.is(ModTags.FOODS_RAW_FISH)
                && !stack.is(ModTags.FOODS_SHRIMP)
                && !stack.is(ModTags.FOODS_OYSTER)
                && !stack.is(ModTags.FOODS_SEA_CUCUMBER)
                && !stack.is(FarmersDelightItems.COD_SLICE)
                && !stack.is(FarmersDelightItems.SALMON_SLICE);
    }

    /**
     * 农夫乐事的两种生鱼片（腌咸鱼不收，免得切好的鱼片拿去腌又变回整条）
     */
    private static final class FarmersDelightItems {
        private static final Item COD_SLICE = item("farmersdelight", "cod_slice");
        private static final Item SALMON_SLICE = item("farmersdelight", "salmon_slice");

        private static Item item(String namespace, String path) {
            return BuiltInRegistries.ITEM.get(DdIds.of(namespace, path));
        }
    }

    public static Item residue() {
        return ModItems.SOY_RESIDUE.get();
    }

    public static List<VatRecipe> all(@Nullable net.minecraft.world.level.Level level) {
        return level == null ? List.of() : all(level.getRecipeManager());
    }

    public static List<VatRecipe> all(@Nullable net.minecraft.world.item.crafting.RecipeManager manager) {
        if (manager == null) {
            return List.of();
        }
        List<VatRecipe> recipes = new ArrayList<>();
        //? if <1.20.5 {
        /*for (VatDataRecipe entry : manager.getAllRecipesFor(ModVatRecipes.TYPE.get())) {
            VatRecipe recipe = entry.rule(entry.getId());
            if (recipe != null) {
                recipes.add(recipe);
            }
        }
        *///?} else {
        for (var entry : manager.getAllRecipesFor(ModVatRecipes.TYPE.get())) {
            VatRecipe recipe = entry.value().rule(entry.id());
            if (recipe != null) {
                recipes.add(recipe);
            }
        }
        //?}
        recipes.sort(java.util.Comparator.comparingInt(VatRecipe::priority).reversed()
                .thenComparing(VatRecipe::id));
        return List.copyOf(recipes);
    }

    @Nullable
    public static VatRecipe recipeOf(Kind kind, @Nullable net.minecraft.world.level.Level level) {
        for (VatRecipe recipe : all(level)) {
            if (recipe.kind() == kind) {
                return recipe;
            }
        }
        return null;
    }

    @Nullable
    public static VatRecipe recipeOf(VatBlockEntity vat) {
        String id = vat.recipeId();
        if (id != null) {
            for (VatRecipe recipe : all(vat.getLevel())) {
                if (id.equals(recipe.id())) {
                    return recipe;
                }
            }
            return null;
        }
        return recipeOf(vat.kind(), vat.getLevel());
    }

    public static boolean isSalt(ItemStack stack) {
        return stack.is(ModTags.FOODS_SALT);
    }

    static ItemStack convertSpicyPickle(ItemStack stack) {
        if (stack.is(ModTags.CROPS_NAPA_CABBAGE)) {
            return new ItemStack(spicyCabbage());
        }
        return convertPickle(stack);
    }

    public static Item vinegarResult() {
        return ModItems.VINEGAR.get();
    }

    public static Item sourWaterResult() {
        return ModItems.SOUR_WATER.get();
    }

    public static Item whiteVinegarResult() {
        return ModItems.WHITE_VINEGAR.get();
    }

    public static Item fishSauceResult() {
        return ModItems.FISH_SAUCE.get();
    }

    public static Item shrimpPasteResult() {
        return ModItems.SHRIMP_PASTE.get();
    }

    public static Item spicyCabbage() {
        return ModItems.SPICY_CABBAGE.get();
    }

    public static Item chiliSauce() {
        return ModItems.CHILI_SAUCE.get();
    }

    public static Item beanSprouts() {
        return ModItems.BEAN_SPROUTS.get();
    }

    public static Item sourCornKernels() {
        return ModItems.SOUR_CORN_KERNELS.get();
    }

    public static Item kvassBread() {
        return Items.BREAD;
    }

    public static boolean isShrimp(ItemStack stack) {
        return stack.is(ModTags.FOODS_SHRIMP);
    }

    public static boolean isSoybean(ItemStack stack) {
        return stack.is(ModTags.CROPS_SOYBEAN);
    }

    public static boolean isBread(ItemStack stack) {
        return stack.is(ModTags.FOODS_BREAD);
    }

    public static Item kvassResult() {
        return ModItems.KVASS.get();
    }

    @Nullable
    public static Item resultFor(VatBlockEntity vat, Item input) {
        Item found = keepResult(recipeOf(vat), input);
        if (found != null || vat.recipeId() != null) {
            return found;
        }
        for (VatRecipe other : all(vat.getLevel())) {
            found = keepResult(other, input);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    @Nullable
    private static Item keepResult(@Nullable VatRecipe recipe, Item input) {
        if (recipe == null) {
            return null;
        }
        ItemStack stack = new ItemStack(input);
        for (VatRecipe.Slot slot : recipe.keepSlots()) {
            if (!slot.matcher().test(stack)) {
                continue;
            }
            if (slot.converter() == null) {
                return null;
            }
            Item result = slot.converter().apply(stack.copyWithCount(1)).getItem();
            return result == input ? null : result;
        }
        return null;
    }

    /**
     * 用容器从缸里取液体：一个「一份」= {@link #SERVING_MB} mB，大酱用碗盛、其它都用玻璃瓶装。
     */
    public record Serving(Item container, Item item) {
    }

    /**
     * 这种流体用什么容器取、取出来是什么。
     * 先看配方里写的（{@code serving_container} / {@code serving_item}），
     * 配方没写才按本模组自己的那几种液体给默认瓶子。
     */
    @Nullable
    public static Serving servingOf(VatBlockEntity vat) {
        VatRecipe recipe = recipeOf(vat);
        if (recipe != null) {
            net.minecraft.world.item.Item container = recipe.liquid().servingContainer();
            net.minecraft.world.item.Item item = recipe.liquid().servingItem();
            if (container != null && item != null) {
                return new Serving(container, item);
            }
        }
        return servingOf(productOf(vat));
    }

    @Nullable
    public static Serving servingOf(net.minecraft.world.level.material.Fluid fluid) {
        if (fluid == null) {
            return null;
        }
        if (fluid.isSame(com.gunmu.northeast_china_delight.fluid.ModFluids.soyPasteSource())) {
            return new Serving(Items.BOWL, ModItems.SOY_PASTE.get());
        }
        if (fluid.isSame(com.gunmu.northeast_china_delight.fluid.ModFluids.soySauceSource())) {
            return new Serving(Items.GLASS_BOTTLE, ModItems.SOY_SAUCE.get());
        }
        if (fluid.isSame(com.gunmu.northeast_china_delight.fluid.ModFluids.vinegarSource())) {
            return new Serving(Items.GLASS_BOTTLE, ModItems.VINEGAR.get());
        }
        if (fluid.isSame(com.gunmu.northeast_china_delight.fluid.ModFluids.whiteVinegarSource())) {
            return new Serving(Items.GLASS_BOTTLE, ModItems.WHITE_VINEGAR.get());
        }
        if (fluid.isSame(com.gunmu.northeast_china_delight.fluid.ModFluids.fishSauceSource())) {
            return new Serving(Items.GLASS_BOTTLE, ModItems.FISH_SAUCE.get());
        }
        if (fluid.isSame(com.gunmu.northeast_china_delight.fluid.ModFluids.shrimpPasteSource())) {
            return new Serving(Items.GLASS_BOTTLE, ModItems.SHRIMP_PASTE.get());
        }
        if (fluid.isSame(com.gunmu.northeast_china_delight.fluid.ModFluids.kvassSource())) {
            return new Serving(Items.GLASS_BOTTLE, ModItems.KVASS.get());
        }
        if (fluid.isSame(com.gunmu.northeast_china_delight.fluid.ModFluids.sourWaterSource())) {
            return new Serving(Items.GLASS_BOTTLE, ModItems.SOUR_WATER.get());
        }
        return null;
    }

    /**
     * 缸里现在的成品液体。优先看缸自己记的（发酵完成时按配方写进去的），
     * 其次看配方声明的产物，最后才按旧的加工类型兜底 —— 老存档没有记流体 id，只能靠 kind 认。
     */
    @Nullable
    public static net.minecraft.world.level.material.Fluid productOf(VatBlockEntity vat) {
        net.minecraft.world.level.material.Fluid brewed = vat.brewedFluid();
        if (brewed != null && vat.brewedMb() > 0) {
            return brewed;
        }
        VatRecipe recipe = recipeOf(vat);
        if (recipe != null && recipe.productFluid() != null) {
            return recipe.productFluid();
        }
        if (vat.sourWaterMb() > 0 && (vat.kind() == Kind.PICKLE || vat.kind() == Kind.SPICY_PICKLE)) {
            return com.gunmu.northeast_china_delight.fluid.ModFluids.sourWaterSource();
        }
        return legacyProduct(vat);
    }

    /** 旧存档里各液体还分字段记，按字段里有没有量认出现在是哪种 */
    @Nullable
    private static net.minecraft.world.level.material.Fluid legacyProduct(VatBlockEntity vat) {
        if (vat.pasteMb() > 0) return com.gunmu.northeast_china_delight.fluid.ModFluids.soyPasteSource();
        if (vat.soySauceMb() > 0) return com.gunmu.northeast_china_delight.fluid.ModFluids.soySauceSource();
        if (vat.vinegarMb() > 0) return com.gunmu.northeast_china_delight.fluid.ModFluids.vinegarSource();
        if (vat.whiteVinegarMb() > 0) return com.gunmu.northeast_china_delight.fluid.ModFluids.whiteVinegarSource();
        if (vat.fishSauceMb() > 0) return com.gunmu.northeast_china_delight.fluid.ModFluids.fishSauceSource();
        if (vat.shrimpPasteMb() > 0) return com.gunmu.northeast_china_delight.fluid.ModFluids.shrimpPasteSource();
        if (vat.kvassMb() > 0) return com.gunmu.northeast_china_delight.fluid.ModFluids.kvassSource();
        return null;
    }

    @Nullable
    public static Item bottleFor(VatBlockEntity vat) {
        Serving serving = servingOf(vat);
        return serving == null ? null : serving.item();
    }
}
