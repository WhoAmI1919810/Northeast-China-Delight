package com.gunmu.dongbei_delight.block;

import com.gunmu.dongbei_delight.DongbeiDelight;
import com.gunmu.dongbei_delight.fluid.ModFluids;
import com.gunmu.dongbei_delight.item.ModItems;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ModBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(DongbeiDelight.MODID);

    // ===== 方块：箱装 / 袋装 =====
    // 9 个材料压成 1 个方块，方块也能拆回 9 个（配方见 tools\generate_food_storage.ps1 生成的文件）。
    // 贴图、方块状态、模型、掉落表都由那个脚本按下面的清单生成。

    /** 箱装：蔬菜与腌菜，木头质感 */
    public static final List<String> CRATE_IDS = List.of(
            "napa_cabbage_crate", "cucumber_crate", "green_radish_crate", "green_onion_crate",
            "eggplant_crate", "green_pepper_crate", "red_chili_crate", "green_beans_crate",
            "corn_crate", "sweet_potato_crate", "sour_cabbage_crate", "spicy_cabbage_crate",
            "pickled_cucumber_crate", "pickled_carrot_crate", "pickled_green_radish_crate"
    );

    /** 袋装：谷物与山珍，麻袋质感 */
    public static final List<String> SACK_IDS = List.of(
            "corn_seeds_sack", "buckwheat_sack", "soybean_sack", "red_bean_sack", "peanut_sack",
            "hazelnut_sack", "hazel_mushroom_sack", "wood_ear_sack", "ginseng_sack"
    );

    public static final Map<String, DeferredBlock<Block>> CRATES = storageBlocks(CRATE_IDS, SoundType.WOOD);
    public static final Map<String, DeferredBlock<Block>> SACKS = storageBlocks(SACK_IDS, SoundType.WOOL);

    /** 这些方块都是普通方块：只负责装东西，没有方块实体 */
    private static Map<String, DeferredBlock<Block>> storageBlocks(List<String> ids, SoundType sound) {
        Map<String, DeferredBlock<Block>> map = new LinkedHashMap<>();
        for (String id : ids) {
            map.put(id, BLOCKS.registerBlock(id, Block::new,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.WOOD)
                            .strength(1.0F)
                            .sound(sound)));
        }
        return Collections.unmodifiableMap(map);
    }

    // ===== 作物 =====
    // 种子用延迟取值的方式传入，避免 ModItems / ModBlocks 之间的类初始化死循环

    public static final DeferredBlock<Block> SOYBEAN_CROP =
            crop("soybean", () -> ModItems.SOYBEAN.get());
    public static final DeferredBlock<Block> EGGPLANT_CROP =
            fruitCrop("eggplant", () -> ModItems.EGGPLANT_SEEDS.get(), () -> ModItems.EGGPLANT.get());
    public static final DeferredBlock<Block> GREEN_PEPPER_CROP =
            fruitCrop("green_pepper", () -> ModItems.GREEN_PEPPER_SEEDS.get(), () -> ModItems.GREEN_PEPPER.get());
    public static final DeferredBlock<Block> CORN_CROP = BLOCKS.registerBlock(
            "corn_crop",
            properties -> new CornCropBlock(properties, () -> ModItems.CORN_SEEDS.get()),
            cropProperties()
    );
    /** 玉米上方的茎秆，没有物品，由作物自己长出来 */
    public static final DeferredBlock<Block> CORN_STALK = BLOCKS.registerBlock(
            "corn_stalk",
            CornStalkBlock::new,
            plantProperties()
    );
    public static final DeferredBlock<Block> GREEN_BEANS_CROP =
            fruitCrop("green_beans", () -> ModItems.GREEN_BEANS_SEEDS.get(), () -> ModItems.GREEN_BEANS.get());
    public static final DeferredBlock<Block> BUCKWHEAT_CROP =
            crop("buckwheat", () -> ModItems.BUCKWHEAT.get());
    /** 大白菜：整棵长在地上，形状和模型照农夫乐事的卷心菜 */
    public static final DeferredBlock<Block> NAPA_CABBAGE_CROP =
            cabbageCrop("napa_cabbage", () -> ModItems.NAPA_CABBAGE_SEEDS.get());
    public static final DeferredBlock<Block> CUCUMBER_CROP =
            fruitCrop("cucumber", () -> ModItems.CUCUMBER_SEEDS.get(), () -> ModItems.CUCUMBER.get());
    /** 红薯：自身即种子，没有单独的种子物品 */
    public static final DeferredBlock<Block> SWEET_POTATO_CROP =
            crop("sweet_potato", () -> ModItems.SWEET_POTATO.get());
    /** 红辣椒：可以用剪刀采摘，植株继续生长 */
    public static final DeferredBlock<Block> RED_CHILI_CROP =
            fruitCrop("red_chili", () -> ModItems.RED_CHILI_SEEDS.get(), () -> ModItems.RED_CHILI.get());
    /** 花生、红豆：自身即种子 */
    public static final DeferredBlock<Block> PEANUT_CROP =
            crop("peanut", () -> ModItems.PEANUT.get());
    public static final DeferredBlock<Block> RED_BEAN_CROP =
            crop("red_bean", () -> ModItems.RED_BEAN.get());
    /** 大葱：整株收获，需要单独的种子物品；**只有 4 个生长阶段**，所以用 4 阶段作物那套 */
    public static final DeferredBlock<Block> GREEN_ONION_CROP =
            smallCrop("green_onion", () -> ModItems.GREEN_ONION_SEEDS.get());
    /** 青萝卜：整株收获，需要单独的种子物品 */
    public static final DeferredBlock<Block> GREEN_RADISH_CROP =
            crop("green_radish", () -> ModItems.GREEN_RADISH_SEEDS.get());

    // ===== 野生植物 =====

    /**
     * 榛子丛：像甜浆果丛一样长在地上，右键采摘榛子、不会伤害玩家。
     *
     * 没有对应的物品（不掉落方块本身，也不进创造模式物品栏），
     * 想种就在地上右键榛子 —— 榛子自己就是种子。
     */
    public static final DeferredBlock<Block> HAZELNUT_BUSH = BLOCKS.registerBlock(
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

    // ===== 功能方块 =====

    // registerBlock 会自动把注册名绑定为方块的 id（1.21.4 起方块必须带 id）
    public static final DeferredBlock<Block> VAT = BLOCKS.registerBlock(
            "vat",
            Vat::new,
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
    public static final DeferredBlock<Block> GRILL_CAMPFIRE = BLOCKS.registerBlock(
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
    public static final DeferredBlock<Block> ANIMAL_OIL_BLOCK = BLOCKS.registerBlock(
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
    // 做法见 DdFeastBlock：两个方块状态属性（朝向 + 剩余份数），没有方块实体。

    public static final DeferredBlock<Block> DA_FENG_SHOU_POT =
            feastBlock("da_feng_shou", () -> ModItems.DA_FENG_SHOU_BOWL.get());
    public static final DeferredBlock<Block> DI_GUO_JI_POT =
            feastBlock("di_guo_ji", () -> ModItems.DI_GUO_JI_BOWL.get());
    public static final DeferredBlock<Block> DI_GUO_PAI_GU_POT =
            feastBlock("di_guo_pai_gu", () -> ModItems.DI_GUO_PAI_GU_BOWL.get());
    public static final DeferredBlock<Block> SHA_ZHU_CAI_POT =
            feastBlock("sha_zhu_cai", () -> ModItems.SHA_ZHU_CAI_BOWL.get());
    public static final DeferredBlock<Block> ZHU_ROU_DUN_FEN_TIAO_POT =
            feastBlock("zhu_rou_dun_fen_tiao", () -> ModItems.ZHU_ROU_DUN_FEN_TIAO_BOWL.get());
    public static final DeferredBlock<Block> SUAN_CAI_DUN_GU_TOU_POT =
            feastBlock("suan_cai_dun_gu_tou", () -> ModItems.SUAN_CAI_DUN_GU_TOU_BOWL.get());
    public static final DeferredBlock<Block> XIAO_JI_DUN_MO_GU_POT =
            feastBlock("xiao_ji_dun_mo_gu", () -> ModItems.XIAO_JI_DUN_MO_GU_BOWL.get());
    public static final DeferredBlock<Block> SUAN_CAI_HAI_XIAN_GUO_POT =
            feastBlock("suan_cai_hai_xian_guo", () -> ModItems.SUAN_CAI_HAI_XIAN_GUO_BOWL.get());

    /**
     * 注册一口大盆菜。
     *
     * 注意第二个参数**必须**传「取一份给什么」—— 之前漏了它，方块里的 servingItem 一直是 null，
     * 结果拿着碗右键毫无反应。
     */
    private static DeferredBlock<Block> feastBlock(String name, Supplier<? extends net.minecraft.world.item.Item> serving) {
        return BLOCKS.registerBlock(name + "_pot",
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
    private static DeferredBlock<Block> crop(String name, Supplier<? extends ItemLike> seed) {
        return BLOCKS.registerBlock(name + "_crop", properties -> new DdCropBlock(properties, seed), cropProperties());
    }

    /** 4 个生长阶段的小作物（AGE_3，目前只有大葱用） */
    private static DeferredBlock<Block> smallCrop(String name, Supplier<? extends ItemLike> seed) {
        return BLOCKS.registerBlock(name + "_crop",
                properties -> new DdSmallCropBlock(properties, seed), cropProperties());
    }

    /** 整棵长在地上的菜（大白菜）：形状照农夫乐事的卷心菜 */
    private static DeferredBlock<Block> cabbageCrop(String name, Supplier<? extends ItemLike> seed) {
        return BLOCKS.registerBlock(name + "_crop",
                properties -> new DdCabbageCropBlock(properties, seed), cropProperties());
    }

    /** 果菜：成熟后可以用剪刀只摘果实，植株继续生长 */
    private static DeferredBlock<Block> fruitCrop(String name, Supplier<? extends ItemLike> seed,
                                                  Supplier<? extends ItemLike> fruit) {
        return BLOCKS.registerBlock(name + "_crop",
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

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
