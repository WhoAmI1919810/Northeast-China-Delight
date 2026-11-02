package com.gunmu.northeast_china_delight.block;

import com.gunmu.northeast_china_delight.NortheastChinaDelight;
import com.gunmu.northeast_china_delight.compat.displaydelight.DisplayDelightCompat;
import com.gunmu.northeast_china_delight.fluid.ModFluids;
import com.gunmu.northeast_china_delight.item.ModItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
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

    public static final List<String> CRATE_IDS = List.of(
            "napa_cabbage_crate", "cucumber_crate", "green_radish_crate", "green_onion_crate",
            "eggplant_crate", "green_pepper_crate", "red_chili_crate", "green_beans_crate",
            "corn_crate", "sweet_potato_crate", "sour_cabbage_crate", "spicy_cabbage_crate",
            "pickled_cucumber_crate", "pickled_carrot_crate", "pickled_green_radish_crate", "frozen_pear_crate",
            "bellflower_crate", "snow_mustard_crate", "pickled_snow_mustard_crate"
    );

    public static final List<String> SACK_IDS = List.of(
            "corn_seeds_sack", "buckwheat_sack", "soybean_sack", "red_bean_sack", "peanut_sack",
            "hazelnut_sack", "hazel_mushroom_sack", "wood_ear_sack", "ginseng_sack"
    );

    public static final Map<String, Supplier<Block>> CRATES = storageBlocks(CRATE_IDS, SoundType.WOOD);
    public static final Map<String, Supplier<Block>> SACKS = storageBlocks(SACK_IDS, SoundType.WOOL);

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
    public static final Supplier<Block> CORN_STALK = block(
            "corn_stalk",
            CornStalkBlock::new,
            plantProperties()
    );
    public static final Supplier<Block> GREEN_BEANS_CROP =
            fruitCrop("green_beans", () -> ModItems.GREEN_BEANS_SEEDS.get(), () -> ModItems.GREEN_BEANS.get());
    public static final Supplier<Block> BUCKWHEAT_CROP =
            crop("buckwheat", () -> ModItems.BUCKWHEAT.get());
    public static final Supplier<Block> NAPA_CABBAGE_CROP =
            cabbageCrop("napa_cabbage", () -> ModItems.NAPA_CABBAGE_SEEDS.get());
    public static final Supplier<Block> SNOW_MUSTARD_CROP =
            cabbageCrop("snow_mustard", () -> ModItems.SNOW_MUSTARD_SEEDS.get());
    public static final Supplier<Block> CUCUMBER_CROP =
            fruitCrop("cucumber", () -> ModItems.CUCUMBER_SEEDS.get(), () -> ModItems.CUCUMBER.get());
    public static final Supplier<Block> SWEET_POTATO_CROP =
            crop("sweet_potato", () -> ModItems.SWEET_POTATO.get());
    public static final Supplier<Block> RED_CHILI_CROP =
            fruitCrop("red_chili", () -> ModItems.RED_CHILI_SEEDS.get(), () -> ModItems.RED_CHILI.get());
    public static final Supplier<Block> PEANUT_CROP =
            crop("peanut", () -> ModItems.PEANUT.get());
    public static final Supplier<Block> RED_BEAN_CROP =
            crop("red_bean", () -> ModItems.RED_BEAN.get());
    public static final Supplier<Block> GREEN_ONION_CROP =
            smallCrop("green_onion", () -> ModItems.GREEN_ONION_SEEDS.get());
    public static final Supplier<Block> GREEN_RADISH_CROP =
            crop("green_radish", () -> ModItems.GREEN_RADISH_SEEDS.get());
    public static final Supplier<Block> BELLFLOWER_CROP = block(
            "bellflower_crop",
            properties -> new BellflowerCropBlock(properties, () -> ModItems.BELLFLOWER_SEEDS.get()),
            cropProperties()
    );

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

    public static final Supplier<Block> HAZEL_MUSHROOM = block(
            "hazel_mushroom",
            HazelMushroomBlock::new,
            mushroomProperties()
    );

    public static final Supplier<Block> HAZEL_MUSHROOM_COLONY = block(
            "hazel_mushroom_colony",
            HazelMushroomColonyBlock::new,
            mushroomProperties()
    );

    /** 段木木耳：木耳贴在原木侧面种下，四个阶段，成熟后右键收 */
    public static final Supplier<Block> WOOD_EAR_CROP = block(
            "wood_ear_crop",
            WoodEarCropBlock::new,
            woodEarProperties().randomTicks()
    );

    /** 野生木耳：树上自己长的一朵，不会长大 */
    public static final Supplier<Block> WILD_WOOD_EAR = block(
            "wild_wood_ear",
            WildWoodEarBlock::new,
            woodEarProperties()
    );

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

    public static final Supplier<Block> UNFIRED_VAT_BLANK = block(
            "unfired_vat_blank",
            Block::new,
            BlockBehaviour.Properties.of().strength(3.5f)
    );

    // registerBlock 会自动把注册名绑定为方块的 id（1.21.4 起方块必须带 id）
    public static final Supplier<Block> VAT = block(
            "vat",
            Vat::new,
            BlockBehaviour.Properties.of().strength(3.5f)
    );

    public static final Supplier<Block> CREATIVE_VAT = block(
            "creative_vat",
            CreativeVat::new,
            BlockBehaviour.Properties.of().strength(3.5f)
    );

    /**
     * 架上烧烤架的营火。
     *
     * 注意这里是另一个方块，而不是给原版营火加方块状态属性 ——
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

    public static final Supplier<Block> WOODEN_MORTAR = block(
            "wooden_mortar",
            WoodenMortarBlock::new,
            // 硬度、爆炸抗性照木板；斧子挖得快（data/minecraft/tags/block/mineable/axe.json）
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .instrument(NoteBlockInstrument.BASS)
                    .strength(2.0F, 3.0F)
                    .sound(SoundType.WOOD)
                    .noOcclusion()
    );

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

    // ===== 东北建材（小院用；暂时没有配方，只能从创造模式拿） =====

    /** 青砖：数值照原版红砖 */
    public static final Supplier<Block> BLUE_BRICKS = block("blue_bricks", Block::new, blueBrickProperties());
    public static final Supplier<StairBlock> BLUE_BRICK_STAIRS = block(
            "blue_brick_stairs",
            //? if <1.20.2 {
            /*properties -> new StairBlock(() -> BLUE_BRICKS.get().defaultBlockState(), properties),
            *///?} else {
            properties -> new StairBlock(BLUE_BRICKS.get().defaultBlockState(), properties),
            //?}
            blueBrickProperties()
    );
    public static final Supplier<SlabBlock> BLUE_BRICK_SLAB =
            block("blue_brick_slab", SlabBlock::new, blueBrickProperties());
    public static final Supplier<WallBlock> BLUE_BRICK_WALL =
            block("blue_brick_wall", WallBlock::new, blueBrickProperties().forceSolidOn());

    /** 木格窗：糊纸的、镶玻璃的，窗框都是橡木原木 */
    public static final Supplier<Block> PAPER_LATTICE_WINDOW =
            block("paper_lattice_window", PaperLatticeWindowBlock::new, windowProperties(SoundType.WOOD));
    public static final Supplier<Block> GLASS_LATTICE_WINDOW =
            block("glass_lattice_window", LatticeWindowBlock::new, windowProperties(SoundType.GLASS));

    /** 障子：高粱秆用柳条勒成的、木板钉成的 */
    public static final Supplier<Block> SORGHUM_STALK_FENCE = block(
            "sorghum_stalk_fence",
            FenceScreenBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.SAND)
                    .strength(1.0F)
                    .sound(SoundType.BAMBOO)
                    .noOcclusion()
                    .ignitedByLava()
    );
    public static final Supplier<Block> BOARD_FENCE = block(
            "board_fence",
            FenceScreenBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .instrument(NoteBlockInstrument.BASS)
                    .strength(2.0F, 3.0F)
                    .sound(SoundType.WOOD)
                    .noOcclusion()
                    .ignitedByLava()
    );

    /** 门帘：普通门帘（本白，合成出来的）；放进织布机用蓝 / 红染料染成棉门帘 / 大花布门帘（见 LoomCurtains） */
    public static final Supplier<Block> PLAIN_DOOR_CURTAIN =
            block("plain_door_curtain", DoorCurtainBlock::new, curtainProperties(MapColor.WOOL));
    /** 门帘：藏蓝棉门帘、大花布门帘 */
    public static final Supplier<Block> COTTON_DOOR_CURTAIN =
            block("cotton_door_curtain", DoorCurtainBlock::new, curtainProperties(MapColor.COLOR_BLUE));
    public static final Supplier<Block> FLORAL_DOOR_CURTAIN =
            block("floral_door_curtain", DoorCurtainBlock::new, curtainProperties(MapColor.COLOR_RED));

    // ===== 东北建材第二批（2026-11-02；同样先不加配方） =====

    /**
     * 彩钢瓦：只做房盖，没有整块 —— 平屋顶（台阶形）、斜屋顶（楼梯形），红 / 蓝 / 绿三色。
     * 斜屋顶的「底座」用同色的平屋顶（原版楼梯要一个基础方块来借爆炸抗性、踩踏这些行为）。
     */
    public static final Supplier<SlabBlock> RED_STEEL_ROOF_SLAB =
            block("red_steel_roof_slab", SlabBlock::new, steelRoofProperties(MapColor.COLOR_RED));
    public static final Supplier<StairBlock> RED_STEEL_ROOF_STAIRS =
            stairs("red_steel_roof_stairs", RED_STEEL_ROOF_SLAB, steelRoofProperties(MapColor.COLOR_RED));
    public static final Supplier<SlabBlock> BLUE_STEEL_ROOF_SLAB =
            block("blue_steel_roof_slab", SlabBlock::new, steelRoofProperties(MapColor.COLOR_BLUE));
    public static final Supplier<StairBlock> BLUE_STEEL_ROOF_STAIRS =
            stairs("blue_steel_roof_stairs", BLUE_STEEL_ROOF_SLAB, steelRoofProperties(MapColor.COLOR_BLUE));
    public static final Supplier<SlabBlock> GREEN_STEEL_ROOF_SLAB =
            block("green_steel_roof_slab", SlabBlock::new, steelRoofProperties(MapColor.COLOR_GREEN));
    public static final Supplier<StairBlock> GREEN_STEEL_ROOF_STAIRS =
            stairs("green_steel_roof_stairs", GREEN_STEEL_ROOF_SLAB, steelRoofProperties(MapColor.COLOR_GREEN));

    /** 瓷砖：白瓷砖贴墙面，蓝瓷砖做墙裙 */
    public static final Supplier<Block> WHITE_TILES = block("white_tiles", Block::new, tileProperties(MapColor.SNOW));
    public static final Supplier<Block> BLUE_TILES = block("blue_tiles", Block::new, tileProperties(MapColor.COLOR_BLUE));

    /** 红漆铁大门（铁门的声音，但能用手推开）、糊纸木格门 */
    public static final Supplier<Block> RED_IRON_GATE = block(
            "red_iron_gate",
            properties -> new NeDoorBlock(properties, BlockSetType.IRON, true),
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .requiresCorrectToolForDrops()
                    .strength(5.0F)
                    .noOcclusion()
                    .pushReaction(PushReaction.DESTROY)
    );
    public static final Supplier<Block> KOREAN_PAPER_DOOR = block(
            "korean_paper_door",
            properties -> new NeDoorBlock(properties, BlockSetType.BIRCH, false),
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .instrument(NoteBlockInstrument.BASS)
                    .strength(3.0F)
                    .noOcclusion()
                    .ignitedByLava()
                    .pushReaction(PushReaction.DESTROY)
    );

    /** 被子：叠好放着的一床花被（三折摞起来） */
    public static final Supplier<Block> FOLDED_QUILT = block(
            "folded_quilt",
            properties -> new FacingDecorBlock(properties, Block.box(1, 0, 1, 15, 7, 15), false),
            beddingProperties(MapColor.COLOR_RED)
    );
    /** 枕头：白布枕头，两头绣花的枕头顶 */
    public static final Supplier<Block> PILLOW = block(
            "pillow",
            properties -> new FacingDecorBlock(properties, Block.box(2, 0, 2, 14, 4.5, 14), false),
            beddingProperties(MapColor.WOOL)
    );

    /** 小板凳：右键坐上去（见 StoolBlock） */
    public static final Supplier<Block> SMALL_STOOL = block(
            "small_stool",
            StoolBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .instrument(NoteBlockInstrument.BASS)
                    .strength(1.0F)
                    .sound(SoundType.WOOD)
                    .noOcclusion()
                    .ignitedByLava()
    );

    /** 压水井：出水嘴朝着放的人；空桶 / 大脸盆 / 玻璃瓶右键压水（见 HandPumpBlock） */
    public static final Supplier<Block> HAND_PUMP = block(
            "hand_pump",
            HandPumpBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GREEN)
                    .requiresCorrectToolForDrops()
                    .strength(3.0F, 6.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion()
    );

    /** 红灯笼：亮度 15 */
    public static final Supplier<Block> RED_LANTERN = block(
            "red_lantern",
            properties -> new DishBlock(properties, Block.box(2, 0, 2, 14, 16, 14)),
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(0.5F)
                    .sound(SoundType.WOOL)
                    .lightLevel(state -> 15)
                    .noOcclusion()
                    .pushReaction(PushReaction.DESTROY)
    );

    public static final List<String> DISH_BLOCK_IDS = List.of(
            "new_style_crispy_pork", "orange_crispy_pork", "crispy_pork_strips", "three_fresh_veggies",
            "braised_pork_ribs", "ground_pot_chicken_bowl", "ground_pot_ribs_bowl", "slaughter_feast_bowl",
            "old_style_crispy_pork", "braised_pork_bone_sauce", "braised_beef_sauce", "braised_pork_hock",
            "braised_pork_strips", "candied_sweet_potato", "smoked_meat_platter", "snowy_bean_paste",
            "candied_peanuts", "grilled_oil_edge", "potato_pancake", "corn_fritter", "salted_fish_flatbread",
            "kimchi_pancake", "sauerkraut_fried_vermicelli", "pepper_dried_tofu",
            "cabbage_tofu_vermicelli_stew", "kimchi_tofu_soup", "pickled_cucumber_pork_stirfry",
            "pickled_veggie_salad", "peanut_potato_salad", "stuffed_cucumber_pickle",
            "dip_veggie_platter", "shredded_pollack", "buckwheat_cold_noodles", "sour_noodle_soup",
            "crushed_corn_porridge", "sweet_potato_porridge", "soy_paste_soup", "tiger_salad",
            "steamed_egg_soy_paste", "sauerkraut_bone_stew_bowl", "pork_vermicelli_stew_bowl",
            "spicy_beef_soup", "kimchi_fried_rice", "shrimp_paste_scrambled_egg",
            "shrimp_paste_tofu_stew", "seafood_tofu_soup", "mung_bean_sheet_salad",
            "braised_sea_cucumber_scallion", "sea_cucumber_tofu_soup", "cured_pork_bean_stew",
            "harvest_stew_bowl", "demoli_fish_stew", "borscht", "ginseng_chicken_soup",
            "chicken_mushroom_stew_bowl", "sauerkraut_seafood_stew_bowl", "bibimbap",
            "grilled_cold_noodles", "baked_sweet_potato", "northeast_rice_wrap", "sticky_bean_bun",
            "red_bean_songpyeon", "peanut_songpyeon",
            "grilled_chicken_frame", "grilled_corn", "sauerkraut_crackling_dumpling",
            "shrimp_pork_dumpling", "three_delicacy_dumpling", "dry_fried_hairtail",
            "braised_hairtail", "hazelnut_sugar_fire_bun",
            "soy_milk", "snow_mustard_tofu_stew", "dog_bao_pickles", "pounded_rice_cake"
    );

    public static final List<String> TRAY_DISH_IDS = List.of(
            "new_style_crispy_pork", "orange_crispy_pork", "crispy_pork_strips", "three_fresh_veggies",
            "braised_pork_ribs", "old_style_crispy_pork",
            "braised_pork_hock", "braised_pork_strips", "candied_sweet_potato", "smoked_meat_platter",
            "snowy_bean_paste", "candied_peanuts", "potato_pancake", "corn_fritter", "salted_fish_flatbread",
            "kimchi_pancake", "sauerkraut_fried_vermicelli", "pepper_dried_tofu", "pickled_cucumber_pork_stirfry",
            "pickled_veggie_salad", "peanut_potato_salad", "stuffed_cucumber_pickle", "dip_veggie_platter",
            "shredded_pollack", "tiger_salad", "shrimp_paste_scrambled_egg", "mung_bean_sheet_salad",
            "braised_sea_cucumber_scallion", "cured_pork_bean_stew", "dry_fried_hairtail",
            "braised_hairtail"
    );

    /** 本身不带独立碗或盘；模型只借用展示餐盘承载体积，不能归入盘装菜。 */
    public static final List<String> NO_CONTAINER_DISH_IDS = List.of(
            "grilled_oil_edge", "grilled_cold_noodles", "baked_sweet_potato", "northeast_rice_wrap",
            "sticky_bean_bun", "red_bean_songpyeon", "peanut_songpyeon",
            "grilled_chicken_frame", "grilled_corn", "sauerkraut_crackling_dumpling",
            "shrimp_pork_dumpling", "three_delicacy_dumpling", "hazelnut_sugar_fire_bun",
            "braised_beef_sauce", "braised_pork_bone_sauce", "pounded_rice_cake"
    );

    /** 无器具菜肴里单个 / 少量的放小餐盘，其余仍放餐盘。 */
    public static final List<String> SMALL_PLATE_DISH_IDS = List.of(
            "baked_sweet_potato", "northeast_rice_wrap", "sticky_bean_bun", "red_bean_songpyeon",
            "peanut_songpyeon", "pounded_rice_cake"
    );

    /** 黄铜碗料理统一用 block/brass_bowl 的碗体，边界见 DishBlock.BRASS_BOWL_SHAPE。 */
    public static final List<String> BRASS_BOWL_DISH_IDS = List.of(
            "buckwheat_cold_noodles", "spicy_beef_soup", "kimchi_fried_rice",
            "kimchi_tofu_soup", "soy_paste_soup", "ginseng_chicken_soup", "bibimbap", "dog_bao_pickles"
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
                    : SMALL_PLATE_DISH_IDS.contains(id)
                    ? DishBlock.SMALL_PLATE_SHAPE
                    : BRASS_BOWL_DISH_IDS.contains(id)
                    ? DishBlock.BRASS_BOWL_SHAPE
                    : NO_CONTAINER_DISH_IDS.contains(id)
                    ? DishBlock.TRAY_SHAPE
                    : DishBlock.BOWL_SHAPE;
            if (NO_CONTAINER_DISH_IDS.contains(id))
            {
                ResourceLocation plate = SMALL_PLATE_DISH_IDS.contains(id)
                        ? DisplayDelightCompat.SMALL_PLATE : DisplayDelightCompat.PLATE;
                map.put(id, dishBlock(id, properties -> new PlatedDishBlock(properties, shape, plate)));
            }
            else
            {
                map.put(id, dishBlock(id, properties -> new DishBlock(properties, shape)));
            }
        }
        return Collections.unmodifiableMap(map);
    }

    /** 能不能潜行右键放下。无器具菜肴不行：要右键展示乐事的餐盘 / 小餐盘摆上去（见 {@link PlatedDishBlock}）。 */
    public static boolean canPlaceDish(String id)
    {
        return !NO_CONTAINER_DISH_IDS.contains(id);
    }

    private static Supplier<Block> dishBlock(String name,
                                             Function<BlockBehaviour.Properties, Block> factory) {
        return block(name,
                factory,
                BlockBehaviour.Properties.of()
                        .mapColor(MapColor.WOOD)
                        .strength(0.3F)
                        .sound(SoundType.WOOD)
                        .noOcclusion()
                        .instabreak()
                        .pushReaction(PushReaction.DESTROY));
    }

    /**
     * 空的大脸盆：能放在地上的盆体（模型 block/large_basin 由 临时素材/1.json 转写，盆沿 5.5 像素）。
     * 不影响物品图标 —— 物品模型仍旧走 models/item/large_basin.json 里的 2D 贴图。
     * 拿水桶右键倒满就变成 {@link #WATER_BASIN}（见 {@link BasinBlock}）。
     */
    public static final Supplier<Block> LARGE_BASIN =
            block("large_basin", properties -> new BasinBlock(properties, false), basinProperties());

    /** 装满水的大脸盆：模型是空盆加一层水面（水面按群系水色染色），空桶右键舀走变回空盆。 */
    public static final Supplier<Block> WATER_BASIN =
            block("water_basin", properties -> new BasinBlock(properties, true), basinProperties());

    private static BlockBehaviour.Properties basinProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.WOOD)
                .strength(0.5F)
                .sound(SoundType.WOOD)
                .noOcclusion();
    }

    /** 黄桃罐头放下后的方块形态：玻璃瓶装饰，可回收。 */
    public static final Supplier<Block> CANNED_YELLOW_PEACH =
            block("canned_yellow_peach",
                    properties -> new DishBlock(properties, DishBlock.CANNED_YELLOW_PEACH_SHAPE),
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_YELLOW)
                            .strength(0.2F)
                            .sound(SoundType.GLASS)
                            .noOcclusion());
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
     * 注意第二个参数必须传「取一份给什么」—— 之前漏了它，方块里的 servingItem 一直是 null，
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
     * 调料液体不再有方块形态（2026-09-24 用户要求）：
     * 它们只作为真正的流体存在 —— 大缸、管道、机械动力储罐、玻璃瓶都能用，
     * 但没法被倒到地上变成液体方块。
     * NeoForge 的 createLegacyBlock 在没有方块时返回空气，所以删掉注册不会崩。
     */
    private static final boolean FLUIDS_HAVE_NO_BLOCK_FORM = true;
    private static Supplier<Block> crop(String name, Supplier<? extends ItemLike> seed) {
        return block(name + "_crop", properties -> new DdCropBlock(properties, seed), cropProperties());
    }

    private static Supplier<Block> smallCrop(String name, Supplier<? extends ItemLike> seed) {
        return block(name + "_crop",
                properties -> new DdSmallCropBlock(properties, seed), cropProperties());
    }

    private static Supplier<Block> cabbageCrop(String name, Supplier<? extends ItemLike> seed) {
        return block(name + "_crop",
                properties -> new DdCabbageCropBlock(properties, seed), cropProperties());
    }

    private static Supplier<Block> fruitCrop(String name, Supplier<? extends ItemLike> seed,
                                                  Supplier<? extends ItemLike> fruit) {
        return block(name + "_crop",
                properties -> new DdCropBlock(properties, seed, fruit), cropProperties());
    }

    /** 台阶：1.20.1 用 Forge 的 Supplier 构造（那时整块还没注册完，取不到 defaultBlockState） */
    private static Supplier<StairBlock> stairs(String name, Supplier<? extends Block> base, BlockBehaviour.Properties properties) {
        //? if <1.20.2 {
        /*return block(name, p -> new StairBlock(() -> base.get().defaultBlockState(), p), properties);
        *///?} else {
        return block(name, p -> new StairBlock(base.get().defaultBlockState(), p), properties);
        //?}
    }

    private static BlockBehaviour.Properties beddingProperties(MapColor color) {
        return BlockBehaviour.Properties.of()
                .mapColor(color)
                .strength(0.5F)
                .sound(SoundType.WOOL)
                .noOcclusion()
                .ignitedByLava()
                .pushReaction(PushReaction.DESTROY);
    }

    private static BlockBehaviour.Properties steelRoofProperties(MapColor color) {
        return BlockBehaviour.Properties.of()
                .mapColor(color)
                .requiresCorrectToolForDrops()
                .strength(2.0F, 6.0F)
                .sound(SoundType.METAL);
    }

    private static BlockBehaviour.Properties tileProperties(MapColor color) {
        return BlockBehaviour.Properties.of()
                .mapColor(color)
                .instrument(NoteBlockInstrument.BASEDRUM)
                .requiresCorrectToolForDrops()
                .strength(1.5F, 6.0F);
    }

    private static BlockBehaviour.Properties blueBrickProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_GRAY)
                .instrument(NoteBlockInstrument.BASEDRUM)
                .requiresCorrectToolForDrops()
                .strength(2.0F, 6.0F);
    }

    private static BlockBehaviour.Properties windowProperties(SoundType sound) {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.WOOD)
                .instrument(NoteBlockInstrument.BASS)
                .strength(1.0F)
                .sound(sound)
                .noOcclusion()
                .isSuffocating((state, level, pos) -> false)
                .isViewBlocking((state, level, pos) -> false)
                .ignitedByLava();
    }

    private static BlockBehaviour.Properties curtainProperties(MapColor color) {
        return BlockBehaviour.Properties.of()
                .mapColor(color)
                .strength(0.8F)
                .sound(SoundType.WOOL)
                .noCollission()
                .noOcclusion()
                .ignitedByLava()
                .pushReaction(PushReaction.DESTROY);
    }

    private static BlockBehaviour.Properties cropProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.PLANT)
                .noCollission()
                .randomTicks()
                .instabreak()
                .sound(SoundType.CROP)
                .pushReaction(PushReaction.DESTROY);
    }

    private static BlockBehaviour.Properties plantProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.PLANT)
                .noCollission()
                .instabreak()
                .sound(SoundType.CROP)
                .pushReaction(PushReaction.DESTROY);
    }

    private static BlockBehaviour.Properties woodEarProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_BROWN)
                .noCollission()
                .noOcclusion()
                .strength(0.2F)
                .sound(SoundType.FUNGUS)
                .pushReaction(PushReaction.DESTROY);
    }

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
