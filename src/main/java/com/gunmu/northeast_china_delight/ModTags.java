package com.gunmu.northeast_china_delight;

import com.gunmu.northeast_china_delight.util.DdIds;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/**
 * 矿物词典（c: 通用标签）键。 NeoForge 1.21.1 用 {@code c:} 命名空间的标签当"矿物词典"：别家模组的青椒、黄瓜、大豆这些东西只要把物品塞进同一个标
 * 签，我们的配方就能直接认。
 */
public final class ModTags {

    private ModTags() {
    }

    private static TagKey<Item> item(String path) {
        //? if <1.20.5 {
        return TagKey.create(Registries.ITEM, DdIds.of("forge", legacyPath(path)));
        //?} else {
        /*return TagKey.create(Registries.ITEM, DdIds.of("c", path));
        *///?}
    }

    /**
     * 1.20.1 用的标签路径：通用标签在 {@code forge:} 命名空间下，而且有几条在 1.20.1 的名字
     * 和 1.21 的 {@code c:} 不一样（跟着农夫乐事的 1.20.1 版本走）。
     *
     * <p>这张表要和 {@code gradle/legacy-data-port.groovy} 里的 {@code legacyTagPathRenames} 对齐。</p>
     */
    //? if <1.20.5 {
    private static String legacyPath(String path) {
        return switch (path) {
            case "foods/dough/wheat" -> "dough/wheat";
            case "foods/raw_fish" -> "raw_fishes";
            case "foods/raw_beef" -> "raw_beef";
            case "foods/raw_chicken" -> "raw_chicken";
            case "foods/raw_mutton" -> "raw_mutton";
            case "foods/raw_pork" -> "raw_pork";
            case "foods/bread" -> "bread";
            case "tools/knife" -> "tools/knives";
            case "foods/fruits/pear" -> "fruits/pear";
            default -> path;
        };
    }
    //?}

    public static final TagKey<Item> CROPS_WHEAT = item("crops/wheat");
    public static final TagKey<Item> CROPS_CARROT = item("crops/carrot");
    public static final TagKey<Item> FOODS_FRUITS_PEAR = item("foods/fruits/pear");
    public static final TagKey<Item> FOODS_RAW_PORK = item("foods/raw_pork");
    public static final TagKey<Item> FOODS_BREAD = item("foods/bread");
    public static final TagKey<Item> FOODS_SOY_PASTE = item("foods/soy_paste");
    public static final TagKey<Item> EGGS = item("eggs");
    public static final TagKey<Item> CROPS_CORN = item("crops/corn");
    public static final TagKey<Item> SEEDS_CORN = item("seeds/corn");
    public static final TagKey<Item> CROPS_SOYBEAN = item("crops/soybean");
    public static final TagKey<Item> CROPS_BUCKWHEAT = item("crops/buckwheat");
    public static final TagKey<Item> CROPS_CUCUMBER = item("crops/cucumber");
    public static final TagKey<Item> CROPS_EGGPLANT = item("crops/eggplant");
    public static final TagKey<Item> CROPS_GREEN_PEPPER = item("crops/green_pepper");
    public static final TagKey<Item> CROPS_CHILE_PEPPER = item("crops/chile_pepper");
    public static final TagKey<Item> CROPS_GREEN_BEAN = item("crops/green_bean");
    public static final TagKey<Item> CROPS_NAPA_CABBAGE = item("crops/napa_cabbage");
    public static final TagKey<Item> CROPS_CABBAGE = item("crops/cabbage");
    public static final TagKey<Item> CROPS_GREEN_ONION = item("crops/green_onion");
    public static final TagKey<Item> CROPS_RADISH = item("crops/radish");
    public static final TagKey<Item> CROPS_SWEET_POTATO = item("crops/sweet_potato");
    public static final TagKey<Item> CROPS_PEANUT = item("crops/peanut");
    public static final TagKey<Item> CROPS_RED_BEAN = item("crops/red_bean");
    public static final TagKey<Item> CROPS_GINSENG = item("crops/ginseng");
    public static final TagKey<Item> MUSHROOMS = item("mushrooms");
    public static final TagKey<Item> FOODS_SHRIMP = item("foods/shrimp");
    public static final TagKey<Item> FOODS_OYSTER = item("foods/oyster");
    public static final TagKey<Item> FOODS_SEA_CUCUMBER = item("foods/sea_cucumber");
    public static final TagKey<Item> FOODS_RAW_FISH = item("foods/raw_fish");
    public static final TagKey<Item> FOODS_SALT = item("foods/seasonings/salt");
    public static final TagKey<Item> FOODS_SOY_SAUCE = item("foods/seasonings/soy_sauce");
    public static final TagKey<Item> FOODS_VINEGAR = item("foods/seasonings/vinegar");
    public static final TagKey<Item> FOODS_WHITE_VINEGAR = item("foods/seasonings/white_vinegar");
    public static final TagKey<Item> FOODS_FISH_SAUCE = item("foods/seasonings/fish_sauce");
    public static final TagKey<Item> FOODS_SHRIMP_PASTE = item("foods/seasonings/shrimp_paste");
    public static final TagKey<Item> FOODS_CHILI_OIL = item("foods/seasonings/chili_oil");
    public static final TagKey<Item> FOODS_CHILI_SAUCE = item("foods/seasonings/chili_sauce");
    public static final TagKey<Item> FOODS_COOKING_OIL = item("foods/seasonings/cooking_oil");
    public static final TagKey<Item> FOODS_ANIMAL_OIL = item("foods/seasonings/animal_oil");
    public static final TagKey<Item> FOODS_PEANUT_BUTTER = item("foods/seasonings/peanut_butter");
    public static final TagKey<Item> FLOURS_CORN = item("flours/corn");
    public static final TagKey<Item> FOODS_STARCH = item("foods/starch");
    public static final TagKey<Item> FOODS_SOY_MILK = item("foods/soy_milk");
    public static final TagKey<Item> FOODS_TOFU = item("foods/tofu");
    public static final TagKey<Item> FOODS_DRIED_TOFU = item("foods/dried_tofu");
    public static final TagKey<Item> FOODS_VERMICELLI = item("foods/vermicelli");
    public static final TagKey<Item> FOODS_BEAN_SPROUTS = item("foods/bean_sprouts");
    public static final TagKey<Item> FOODS_SAUERKRAUT = item("foods/sauerkraut");
    public static final TagKey<Item> FOODS_KIMCHI = item("foods/kimchi");
    public static final TagKey<Item> FOODS_PICKLES = item("foods/pickles");
    public static final TagKey<Item> FOODS_SALTED_MEAT = item("foods/salted_meat");
    public static final TagKey<Item> FOODS_SALTED_FISH = item("foods/salted_fish");
    public static final TagKey<Item> FOODS_CRACKLINGS = item("foods/cracklings");
    public static final TagKey<Item> SUGAR = item("sugar");
    public static final TagKey<Item> DRIED_KELP = item("dried_kelp");
    public static final TagKey<Item> PLATES_IRON = item("plates/iron");
    public static final TagKey<Item> PLATES_BRASS = item("plates/brass");
}
