package com.gunmu.northeast_china_delight.block;

import com.gunmu.northeast_china_delight.NortheastChinaDelight;
import com.gunmu.northeast_china_delight.fluid.ModFluids;
import com.gunmu.northeast_china_delight.item.ModItems;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.VoxelShape;
//? if <1.20.2 {
/*import net.minecraftforge.eventbus.api.IEventBus;
*///?} else {
import net.neoforged.bus.api.IEventBus;
//?}
//? if <1.20.2 {
/*import net.minecraftforge.registries.DeferredRegister;
*///?} else {
import net.neoforged.neoforge.registries.DeferredRegister;
//?}

import java.util.function.Function;
import java.util.function.Supplier;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ModBlocks {

    //? if <1.20.2 {
    /*public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(net.minecraftforge.registries.ForgeRegistries.BLOCKS, NortheastChinaDelight.MODID);
    *///?} else {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(NortheastChinaDelight.MODID);
    //?}

    /**
     * 用「属性 → 方块」的工厂注册一个方块。
     *
     * <p>1.20.1（Forge）的 DeferredRegister 只有 register(name, supplier)，
     * 所以把属性在这里套进工厂；1.20.2 起 NeoForge 的 registerBlock 直接把属性传进去，
     * 方块本身也能拿到注册名（1.21.4 起必须带 id）。</p>
     */
    private static <B extends Block> Supplier<B> block(String name,
                                                       Function<BlockBehaviour.Properties, ? extends B> factory,
                                                       BlockBehaviour.Properties properties) {
        //? if <1.20.2 {
        /*return BLOCKS.register(name, () -> factory.apply(properties));
        *///?} else {
        return BLOCKS.registerBlock(name, factory, properties);
        //?}
    }

    // ===== 方块：箱装 / 袋装 =====
    // 9 个材料压成 1 个方块，方块也能拆回 9 个（配方见 tools\generate_food_storage.ps1 生成的文件）。
    // 贴图、方块状态、模型、掉落表都由那个脚本按下面的清单生成。

    /** 箱装：蔬菜与腌菜，木头质感 */
    public static final List<String> CRATE_IDS = List.of(
            "napa_cabbage_crate", "cucumber_crate", "green_radish_crate", "green_onion_crate",
            "eggplant_crate", "green_pepper_crate", "red_chili_crate", "green_beans_crate",
            "corn_crate", "sweet_potato_crate", "sour_cabbage_crate", "spicy_cabbage_crate",
            "pickled_cucumber_crate", "pickled_carrot_crate", "pickled_green_radish_crate", "frozen_pear_crate"
    );

    /** 袋装：谷物与山珍，麻袋质感 */
    public static final List<String> SACK_IDS = List.of(
            "corn_seeds_sack", "buckwheat_sack", "soybean_sack", "red_bean_sack", "peanut_sack",
            "hazelnut_sack", "hazel_mushroom_sack", "wood_ear_sack", "ginseng_sack"
    );

    public static final Map<String, Supplier<Block>> CRATES = storageBlocks(CRATE_IDS, SoundType.WOOD);
    public static final Map<String, Supplier<Block>> SACKS = storageBlocks(SACK_IDS, SoundType.WOOL);

    /** 这些方块都是普通方块：只负责装东西，没有方块实体 */
    private static Map<String, Supplier<Block>> storageBlocks(List<String> ids, SoundType sound) {
        Map<String, Supplier<Block>> map = new LinkedHashMap<>();
        for (String id : ids) {
            map.put(id, block(id, Block::new,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.WOOD)
                            .strength(1.0F)
                            .sound(sound)));
        }
        return Collections.unmodifiableMap(map);
    }

    // ===== 作物 =====
    // 种子用延迟取值的方式传入，避免 ModItems / ModBlocks 之间的类初始化死循环

    public static final Supplier<Block> SOYBEAN_CROP =
            crop("soybean", () -> ModItems.SOYBEAN.get());
    public static final Supplier<Block> EGGPLANT_CROP =
            fruitCrop("eggplant", () -> ModItems.EGGPLANT_SEEDS.get(), () -> ModItems.EGGPLANT.get());
    public static final Supplier<Block> GREEN_PEPPER_CROP =
            fruitCrop("green_pepper", () -> ModItems.GREEN_PEPPER_SEEDS.get(), () -> ModItems.GREEN_PEPPER.get());
    public static final Supplier<Block> CORN_CROP = block(
            "corn_crop",
            properties -> new CornCropBlock(properties, () -> ModItems.CORN_SEEDS.get()),
            cropProperties()
    );
    /** 玉米上方的茎秆，没有物品，由作物自己长出来 */
    public static final Supplier<Block> CORN_STALK = block(
            "corn_stalk",
            CornStalkBlock::new,
            plantProperties()
    );
    public static final Supplier<Block> GREEN_BEANS_CROP =
            fruitCrop("green_beans", () -> ModItems.GREEN_BEANS_SEEDS.get(), () -> ModItems.GREEN_BEANS.get());
    public static final Supplier<Block> BUCKWHEAT_CROP =
            crop("buckwheat", () -> ModItems.BUCKWHEAT.get());
    /** 大白菜：整棵长在地上，形状和模型照农夫乐事的卷心菜 */
    public static final Supplier<Block> NAPA_CABBAGE_CROP =
            cabbageCrop("napa_cabbage", () -> ModItems.NAPA_CABBAGE_SEEDS.get());
    public static final Supplier<Block> CUCUMBER_CROP =
            fruitCrop("cucumber", () -> ModItems.CUCUMBER_SEEDS.get(), () -> ModItems.CUCUMBER.get());
    /** 红薯：自身即种子，没有单独的种子物品 */
    public static final Supplier<Block> SWEET_POTATO_CROP =
            crop("sweet_potato", () -> ModItems.SWEET_POTATO.get());
    /** 红辣椒：可以用剪刀采摘，植株继续生长 */
    public static final Supplier<Block> RED_CHILI_CROP =
            fruitCrop("red_chili", () -> ModItems.RED_CHILI_SEEDS.get(), () -> ModItems.RED_CHILI.get());
    /** 花生、红豆：自身即种子 */
    public static final Supplier<Block> PEANUT_CROP =
            crop("peanut", () -> ModItems.PEANUT.get());
    public static final Supplier<Block> RED_BEAN_CROP =
            crop("red_bean", () -> ModItems.RED_BEAN.get());
    /** 大葱：整株收获，需要单独的种子物品；**只有 4 个生长阶段**，所以用 4 阶段作物那套 */
    public static final Supplier<Block> GREEN_ONION_CROP =
            smallCrop("green_onion", () -> ModItems.GREEN_ONION_SEEDS.get());
    /** 青萝卜：整株收获，需要单独的种子物品 */
    public static final Supplier<Block> GREEN_RADISH_CROP =
            crop("green_radish", () -> ModItems.GREEN_RADISH_SEEDS.get());

    // ===== 野生植物 =====

    /**
     * 榛子丛：像甜浆果丛一样长在地上，右键采摘榛子、不会伤害玩家。
     *
     * 没有对应的物品（不掉落方块本身，也不进创造模式物品栏），
     * 想种就在地上右键榛子 —— 榛子自己就是种子。
     */
    public static final Supplier<Block> HAZELNUT_BUSH = block(
            "hazelnut_bush",
            HazelnutBushBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .randomTicks()
                    .noCollission()
                    .instabreak()
                    .sound(SoundType.SWEET_BERRY_BUSH)
                    .pushReaction(PushReaction.DESTROY)
    );

    /** 榛蘑（单株）：行为照抄原版蘑菇 */
    public static final Supplier<Block> HAZEL_MUSHROOM = block(
            "hazel_mushroom",
            HazelMushroomBlock::new,
            mushroomProperties()
    );

    /** 榛蘑簇：行为照抄农夫乐事的蘑菇菌簇 */
    public static final Supplier<Block> HAZEL_MUSHROOM_COLONY = block(
            "hazel_mushroom_colony",
            HazelMushroomColonyBlock::new,
            mushroomProperties()
    );

    /** 人参：8 个生长阶段，自带覆雪状态 */
    public static final Supplier<Block> GINSENG_CROP = block(
            "ginseng_crop",
            GinsengCropBlock::new,
            plantProperties().randomTicks()
    );

    public static final Supplier<SkewerBlock> CORN_SKEWER = block(
            "corn_skewer",
            properties -> new SkewerBlock(properties, () -> ModItems.CORN.get()),
            skewerProperties()
    );
    public static final Supplier<SkewerBlock> CHILI_SKEWER = block(
            "chili_skewer",
            properties -> new SkewerBlock(properties, () -> ModItems.RED_CHILI.get()),
            skewerProperties()
    );

    // ===== 功能方块 =====

    // registerBlock 会自动把注册名绑定为方块的 id（1.21.4 起方块必须带 id）
    public static final Supplier<Block> VAT = block(
            "vat",
            Vat::new,
            BlockBehaviour.Properties.of().strength(3.5f)
    );

    /** 创造大缸：同外观、同交互，条件凑齐就瞬间完成（没有合成配方） */
    public static final Supplier<Block> CREATIVE_VAT = block(
            "creative_vat",
            CreativeVat::new,
            BlockBehaviour.Properties.of().strength(3.5f)
    );

    /**
     * 架上烧烤架的营火。
     *
     * 注意这里是**另一个方块**，而不是给原版营火加方块状态属性 ——
     * 给原版方块加属性会改变全局方块状态 id，老存档里的方块会错位。
     * 这个方块直接继承原版营火，所以点燃、熄灭、伤害、粒子、渲染都和营火一样，
     * 只是多了一个烤架，并且把「放上去烤」的流程换成了本模组的烧烤配方。
     */
    public static final Supplier<Block> GRILL_CAMPFIRE = block(
            "grill_campfire",
            GrillCampfireBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PODZOL)
                    .instrument(NoteBlockInstrument.BASS)
                    .strength(2.0F)
                    .sound(SoundType.WOOD)
                    .lightLevel(state -> state.getValue(CampfireBlock.LIT) ? 15 : 0)
                    .noOcclusion()
                    .ignitedByLava()
    );

    /**
     * 动物油块：4 瓶动物油在工作台压成一块。
     * 特性照搬原版蜂蜜块（{@link AnimalOilBlock} 直接继承 {@link net.minecraft.world.level.block.HoneyBlock}），
     * 只是贴图更白。
     */
    public static final Supplier<Block> ANIMAL_OIL_BLOCK = block(
            "animal_oil_block",
            AnimalOilBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_YELLOW)
                    .speedFactor(0.4F)
                    .jumpFactor(0.5F)
                    .noOcclusion()
                    .sound(SoundType.HONEY_BLOCK)
    );

    // ===== 流体方块 =====

    // ===== 大盆菜的方块形式（一放就是一整盆，右键取一份）=====

    // ===== 单份菜肴的方块形式（右键吃、潜行右键摆）=====
    // 容器几何与 display-delight 一致：碗 8×8 高 4、大盘 14×14 高 2。
    // 这些方块只负责承载渲染和碰撞箱，食用/放置由 ModGameplayEvents 统一处理，
    // 这样普通 Item 的 craftRemainder / FoodProperties 不会因为换成 BlockItem 而改变。

    /** 需要单份方块模型的料理。大盆整锅料理不在这里，它们已有独立的 DdFeastBlock。 */
    public static final List<String> DISH_BLOCK_IDS = List.of(
            "new_style_crispy_pork", "orange_crispy_pork", "crispy_pork_strips", "three_fresh_veggies",
            "braised_pork_ribs", "ground_pot_chicken_bowl", "ground_pot_ribs_bowl", "slaughter_feast_bowl",
            "old_style_crispy_pork", "braised_pork_bone_sauce", "braised_beef_sauce", "braised_pork_hock",
            "braised_pork_strips", "candied_sweet_potato", "smoked_meat_platter", "snowy_bean_paste",
            "candied_peanuts", "grilled_oil_edge", "potato_pancake", "corn_fritter", "salted_fish_flatbread",
            "kimchi_pancake", "sauerkraut_fried_vermicelli", "pepper_dried_tofu",
            "cabbage_tofu_vermicelli_stew", "kimchi_tofu_soup", "pickled_cucumber_pork_stirfry",
            "pickled_veggie_salad", "mixed_vegetable_salad", "stuffed_cucumber_pickle",
            "dip_veggie_platter", "shredded_pollack", "buckwheat_cold_noodles", "sour_noodle_soup",
            "crushed_corn_porridge", "sweet_potato_porridge", "soy_paste_soup", "tiger_salad",
            "steamed_egg_soy_paste", "sauerkraut_bone_stew_bowl", "pork_vermicelli_stew_bowl",
            "spicy_beef_soup", "kimchi_fried_rice", "shrimp_paste_scrambled_egg",
            "shrimp_paste_tofu_stew", "seafood_tofu_soup", "mung_bean_sheet_salad",
            "braised_sea_cucumber_scallion", "sea_cucumber_tofu_soup", "cured_pork_bean_stew",
            "harvest_stew_bowl", "demoli_fish_stew", "borscht", "ginseng_chicken_soup",
            "chicken_mushroom_stew_bowl", "sauerkraut_seafood_stew_bowl", "bibimbap",
            "grilled_cold_noodles", "baked_sweet_potato", "northeast_rice_wrap", "sticky_bean_bun",
            "grilled_chicken_frame", "grilled_corn", "sauerkraut_crackling_dumpling",
            "shrimp_pork_dumpling", "three_delicacy_dumpling", "dry_fried_hairtail",
            "braised_hairtail", "hazelnut_sugar_fire_bun"
    );

    /** 真实以餐盘盛装的单份料理。 */
    public static final List<String> TRAY_DISH_IDS = List.of(
            "new_style_crispy_pork", "orange_crispy_pork", "crispy_pork_strips", "three_fresh_veggies",
            "braised_pork_ribs", "old_style_crispy_pork", "braised_pork_bone_sauce", "braised_beef_sauce",
            "braised_pork_hock", "braised_pork_strips", "candied_sweet_potato", "smoked_meat_platter",
            "snowy_bean_paste", "candied_peanuts", "potato_pancake", "corn_fritter", "salted_fish_flatbread",
            "kimchi_pancake", "sauerkraut_fried_vermicelli", "pepper_dried_tofu", "pickled_cucumber_pork_stirfry",
            "pickled_veggie_salad", "mixed_vegetable_salad", "stuffed_cucumber_pickle", "dip_veggie_platter",
            "shredded_pollack", "tiger_salad", "shrimp_paste_scrambled_egg", "mung_bean_sheet_salad",
            "braised_sea_cucumber_scallion", "cured_pork_bean_stew", "dry_fried_hairtail",
            "braised_hairtail"
    );

    /** 本身不带独立碗或盘；模型只借用展示餐盘承载体积，不能归入盘装菜。 */
    public static final List<String> NO_CONTAINER_DISH_IDS = List.of(
            "grilled_oil_edge", "grilled_cold_noodles", "baked_sweet_potato", "northeast_rice_wrap",
            "sticky_bean_bun", "grilled_chicken_frame", "grilled_corn", "sauerkraut_crackling_dumpling",
            "shrimp_pork_dumpling", "three_delicacy_dumpling", "hazelnut_sugar_fire_bun"
    );

    /** 黄铜碗料理必须复用 brass_bowl 的 8×8×4 像素边界。 */
    public static final List<String> BRASS_BOWL_DISH_IDS = List.of(
            "buckwheat_cold_noodles", "spicy_beef_soup", "kimchi_fried_rice",
            "kimchi_tofu_soup", "soy_paste_soup", "ginseng_chicken_soup", "bibimbap"
    );

    public static final Map<String, Supplier<Block>> DISH_BLOCKS = registerDishBlocks();

    /** 保留这个公开字段，兼容旧存档、测试和现有数据生成脚本。 */
    public static final Supplier<Block> OLD_STYLE_GUO_BAO_ROU =
            DISH_BLOCKS.get("old_style_crispy_pork");

    private static Map<String, Supplier<Block>> registerDishBlocks()
    {
        Map<String, Supplier<Block>> map = new LinkedHashMap<>();
        for (String id : DISH_BLOCK_IDS)
        {
            VoxelShape shape = TRAY_DISH_IDS.contains(id)
                    ? DishBlock.TRAY_SHAPE
                    : NO_CONTAINER_DISH_IDS.contains(id)
                    ? DishBlock.TRAY_SHAPE
                    : DishBlock.BOWL_SHAPE;
            map.put(id, dishBlock(id, shape));
        }
        return Collections.unmodifiableMap(map);
    }

    private static Supplier<Block> dishBlock(String name, net.minecraft.world.phys.shapes.VoxelShape shape) {
        return block(name,
                properties -> new DishBlock(properties, shape),
                BlockBehaviour.Properties.of()
                        .mapColor(MapColor.WOOD)
                        .strength(0.3F)
                        .sound(SoundType.WOOD)
                        .noOcclusion()
                        .instabreak()
                        .pushReaction(PushReaction.DESTROY));
    }

    // 做法见 DdFeastBlock：两个方块状态属性（朝向 + 剩余份数），没有方块实体。

    public static final Supplier<Block> DA_FENG_SHOU_POT =
            feastBlock("harvest_stew", () -> ModItems.DA_FENG_SHOU_BOWL.get());
    public static final Supplier<Block> DI_GUO_JI_POT =
            feastBlock("ground_pot_chicken", () -> ModItems.DI_GUO_JI_BOWL.get());
    public static final Supplier<Block> DI_GUO_PAI_GU_POT =
            feastBlock("ground_pot_ribs", () -> ModItems.DI_GUO_PAI_GU_BOWL.get());
    public static final Supplier<Block> SHA_ZHU_CAI_POT =
            feastBlock("slaughter_feast", () -> ModItems.SHA_ZHU_CAI_BOWL.get());
    public static final Supplier<Block> ZHU_ROU_DUN_FEN_TIAO_POT =
            feastBlock("pork_vermicelli_stew", () -> ModItems.ZHU_ROU_DUN_FEN_TIAO_BOWL.get());
    public static final Supplier<Block> SUAN_CAI_DUN_GU_TOU_POT =
            feastBlock("sauerkraut_bone_stew", () -> ModItems.SUAN_CAI_DUN_GU_TOU_BOWL.get());
    public static final Supplier<Block> XIAO_JI_DUN_MO_GU_POT =
            feastBlock("chicken_mushroom_stew", () -> ModItems.XIAO_JI_DUN_MO_GU_BOWL.get());
    public static final Supplier<Block> SUAN_CAI_HAI_XIAN_GUO_POT =
            feastBlock("sauerkraut_seafood_stew", () -> ModItems.SUAN_CAI_HAI_XIAN_GUO_BOWL.get());

    /**
     * 注册一口大盆菜。
     *
     * 注意第二个参数**必须**传「取一份给什么」—— 之前漏了它，方块里的 servingItem 一直是 null，
     * 结果拿着碗右键毫无反应。
     */
    private static Supplier<Block> feastBlock(String name, Supplier<? extends net.minecraft.world.item.Item> serving) {
        return block(name + "_pot",
                properties -> new DdFeastBlock(properties, () -> serving.get()),
                BlockBehaviour.Properties.of()
                        .mapColor(MapColor.WOOD)
                        .strength(0.5F)
                        .sound(SoundType.WOOD)
                        .noOcclusion());
    }



    /**
     * 调料液体**不再有方块形态**（2026-09-24 用户要求）：
     * 它们只作为真正的流体存在 —— 大缸、管道、机械动力储罐、玻璃瓶都能用，
     * 但没法被倒到地上变成液体方块。
     * NeoForge 的 createLegacyBlock 在没有方块时返回空气，所以删掉注册不会崩。
     */
    private static final boolean FLUIDS_HAVE_NO_BLOCK_FORM = true;
    private static Supplier<Block> crop(String name, Supplier<? extends ItemLike> seed) {
        return block(name + "_crop", properties -> new DdCropBlock(properties, seed), cropProperties());
    }

    /** 4 个生长阶段的小作物（AGE_3，目前只有大葱用） */
    private static Supplier<Block> smallCrop(String name, Supplier<? extends ItemLike> seed) {
        return block(name + "_crop",
                properties -> new DdSmallCropBlock(properties, seed), cropProperties());
    }

    /** 整棵长在地上的菜（大白菜）：形状照农夫乐事的卷心菜 */
    private static Supplier<Block> cabbageCrop(String name, Supplier<? extends ItemLike> seed) {
        return block(name + "_crop",
                properties -> new DdCabbageCropBlock(properties, seed), cropProperties());
    }

    /** 果菜：成熟后可以用剪刀只摘果实，植株继续生长 */
    private static Supplier<Block> fruitCrop(String name, Supplier<? extends ItemLike> seed,
                                                  Supplier<? extends ItemLike> fruit) {
        return block(name + "_crop",
                properties -> new DdCropBlock(properties, seed, fruit), cropProperties());
    }

    /** 与原版作物一致的方块属性 */
    private static BlockBehaviour.Properties cropProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.PLANT)
                .noCollission()
                .randomTicks()
                .instabreak()
                .sound(SoundType.CROP)
                .pushReaction(PushReaction.DESTROY);
    }

    /** 不会随机刻的植物方块 */
    private static BlockBehaviour.Properties plantProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.PLANT)
                .noCollission()
                .instabreak()
                .sound(SoundType.CROP)
                .pushReaction(PushReaction.DESTROY);
    }

    /** 蘑菇类方块：和原版的蘑菇、农夫乐事的菌簇同一套属性（怕光、一碰就碎、会随机刻） */
    private static BlockBehaviour.Properties mushroomProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.PLANT)
                .randomTicks()
                .noCollission()
                .instabreak()
                .sound(SoundType.GRASS)
                .pushReaction(PushReaction.DESTROY);
    }

    private static BlockBehaviour.Properties skewerProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.PLANT)
                .noCollission()
                .noOcclusion()
                .instabreak()
                .sound(SoundType.CROP)
                .pushReaction(PushReaction.DESTROY)
                .noLootTable();
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
