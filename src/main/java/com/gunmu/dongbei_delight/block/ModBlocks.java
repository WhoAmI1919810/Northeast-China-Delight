package com.gunmu.dongbei_delight.block;

import com.gunmu.dongbei_delight.DongbeiDelight;
import com.gunmu.dongbei_delight.fluid.ModFluids;
import com.gunmu.dongbei_delight.item.ModItems;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(DongbeiDelight.MODID);

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
    public static final DeferredBlock<Block> NAPA_CABBAGE_CROP =
            crop("napa_cabbage", () -> ModItems.NAPA_CABBAGE_SEEDS.get());
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
    /** 大葱：整株收获，需要单独的种子物品 */
    public static final DeferredBlock<Block> GREEN_ONION_CROP =
            crop("green_onion", () -> ModItems.GREEN_ONION_SEEDS.get());

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

    // ===== 流体方块 =====

    /**
     * 酱油 / 大酱的液体方块。
     * 直接传入液体实例（而不是从注册表里取），避免液体与方块两个注册事件的先后顺序问题。
     * 没有对应的桶物品，正常情况下玩家不会把它倒出来。
     */
    public static final DeferredBlock<LiquidBlock> SOY_SAUCE_FLUID = BLOCKS.registerBlock(
            "soy_sauce",
            properties -> new LiquidBlock(ModFluids.soySauceSource(), properties),
            fluidProperties()
    );
    public static final DeferredBlock<LiquidBlock> SOY_PASTE_FLUID = BLOCKS.registerBlock(
            "soy_paste",
            properties -> new LiquidBlock(ModFluids.soyPasteSource(), properties),
            fluidProperties()
    );
    public static final DeferredBlock<LiquidBlock> SOY_MILK_FLUID = BLOCKS.registerBlock(
            "soy_milk",
            properties -> new LiquidBlock(ModFluids.soyMilkSource(), properties),
            fluidProperties()
    );
    public static final DeferredBlock<LiquidBlock> VINEGAR_FLUID = BLOCKS.registerBlock(
            "vinegar",
            properties -> new LiquidBlock(ModFluids.vinegarSource(), properties),
            fluidProperties()
    );
    public static final DeferredBlock<LiquidBlock> SOUR_WATER_FLUID = BLOCKS.registerBlock(
            "sour_water",
            properties -> new LiquidBlock(ModFluids.sourWaterSource(), properties),
            fluidProperties()
    );
    public static final DeferredBlock<LiquidBlock> WHITE_VINEGAR_FLUID = BLOCKS.registerBlock(
            "white_vinegar",
            properties -> new LiquidBlock(ModFluids.whiteVinegarSource(), properties),
            fluidProperties()
    );
    public static final DeferredBlock<LiquidBlock> FISH_SAUCE_FLUID = BLOCKS.registerBlock(
            "fish_sauce",
            properties -> new LiquidBlock(ModFluids.fishSauceSource(), properties),
            fluidProperties()
    );
    public static final DeferredBlock<LiquidBlock> SHRIMP_PASTE_FLUID = BLOCKS.registerBlock(
            "shrimp_paste",
            properties -> new LiquidBlock(ModFluids.shrimpPasteSource(), properties),
            fluidProperties()
    );
    public static final DeferredBlock<LiquidBlock> VEGETABLE_OIL_FLUID = BLOCKS.registerBlock(
            "vegetable_oil",
            properties -> new LiquidBlock(ModFluids.vegetableOilSource(), properties),
            fluidProperties()
    );

    private static DeferredBlock<Block> crop(String name, Supplier<? extends ItemLike> seed) {
        return BLOCKS.registerBlock(name + "_crop", properties -> new DdCropBlock(properties, seed), cropProperties());
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

    /** 与原版水一致的液体方块属性 */
    private static BlockBehaviour.Properties fluidProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.WATER)
                .replaceable()
                .noCollission()
                .strength(100.0F)
                .pushReaction(PushReaction.DESTROY)
                .noLootTable()
                .liquid()
                .sound(SoundType.EMPTY);
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
