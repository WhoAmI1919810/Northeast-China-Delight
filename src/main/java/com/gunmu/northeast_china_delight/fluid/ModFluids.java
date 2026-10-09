package com.gunmu.northeast_china_delight.fluid;

import com.gunmu.northeast_china_delight.NortheastChinaDelight;
import com.gunmu.northeast_china_delight.block.ModBlocks;
import com.gunmu.northeast_china_delight.util.DdIds;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
//? if <1.20.2 {
import net.minecraftforge.eventbus.api.IEventBus;
//?} else {
/*import net.neoforged.bus.api.IEventBus;
*///?}
//? if <1.20.2 {
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
//?} else {
/*import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
*///?}
//? if >=1.20.2 {
/*import net.neoforged.neoforge.fluids.BaseFlowingFluid;
*///?}
//? if <1.20.2 {
import net.minecraftforge.fluids.FluidType;
//?} else {
/*import net.neoforged.neoforge.fluids.FluidType;
*///?}
//? if <1.20.2 {
import net.minecraftforge.registries.DeferredRegister;
//?} else {
/*import net.neoforged.neoforge.registries.DeferredRegister;
*///?}
//? if <1.20.2 {
import net.minecraftforge.registries.ForgeRegistries;
//?} else {
/*import net.neoforged.neoforge.registries.NeoForgeRegistries;
*///?}

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 大缸酿出的两种液体：酱油、大酱。
 *
 * 关于「懒加载」：{@code Fluid} 的构造函数会往流体注册表里挂一个 intrusive holder，
 * 这要求注册表处于「解冻」状态 —— 也就是只能在 RegisterEvent 期间创建。
 * 而方块注册事件不一定排在流体注册事件之前，所以这里两个实例都做成按需创建：
 * 谁先用到（方块工厂或流体注册）就先创建，另一个直接复用同一个实例。
 */
public final class ModFluids {

    private ModFluids() {
    }

    public static final DeferredRegister<FluidType> FLUID_TYPES =
            //? if <1.20.2 {
            DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES, NortheastChinaDelight.MODID);
            //?} else {
            /*DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, NortheastChinaDelight.MODID);
            *///?}
    public static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(Registries.FLUID, NortheastChinaDelight.MODID);

    // ===== 酱油 =====

    public static final FluidType SOY_SAUCE_TYPE = new DdFluidType(FluidType.Properties.create()
            .descriptionId("fluid_type.northeast_china_delight.soy_sauce")
            .density(1100)
            .viscosity(1200)
            .motionScale(0.006D), "block/soy_sauce_still", "block/soy_sauce_flow", 0xFFFFFFFF);

    private static final BaseFlowingFluid.Properties SOY_SAUCE_PROPERTIES =
            new BaseFlowingFluid.Properties(() -> SOY_SAUCE_TYPE,
                    ModFluids::soySauceSource, ModFluids::soySauceFlowing)
                    .slopeFindDistance(4)
                    .levelDecreasePerBlock(1)
                    .tickRate(6);

    private static BaseFlowingFluid.Source soySauceSource;
    private static BaseFlowingFluid.Flowing soySauceFlowing;

    /** 静止的酱油（注册与液体方块共用这一个实例） */
    public static BaseFlowingFluid.Source soySauceSource() {
        if (soySauceSource == null) {
            soySauceSource = new BaseFlowingFluid.Source(SOY_SAUCE_PROPERTIES);
        }
        return soySauceSource;
    }

    /** 流动的酱油 */
    public static BaseFlowingFluid.Flowing soySauceFlowing() {
        if (soySauceFlowing == null) {
            soySauceFlowing = new BaseFlowingFluid.Flowing(SOY_SAUCE_PROPERTIES);
        }
        return soySauceFlowing;
    }

    public static final Supplier<FluidType> SOY_SAUCE_TYPE_HOLDER =
            FLUID_TYPES.register("soy_sauce", () -> SOY_SAUCE_TYPE);
    public static final Supplier<BaseFlowingFluid.Source> SOY_SAUCE =
            FLUIDS.register("soy_sauce", ModFluids::soySauceSource);
    public static final Supplier<BaseFlowingFluid.Flowing> FLOWING_SOY_SAUCE =
            FLUIDS.register("flowing_soy_sauce", ModFluids::soySauceFlowing);

    // ===== 大酱 =====

    public static final FluidType SOY_PASTE_TYPE = new DdFluidType(FluidType.Properties.create()
            .descriptionId("fluid_type.northeast_china_delight.soy_paste")
            .density(1250)
            .viscosity(3200)
            .motionScale(0.004D), "block/soy_paste_still", "block/soy_paste_still", 0xFF9A672E);

    private static final BaseFlowingFluid.Properties SOY_PASTE_PROPERTIES =
            new BaseFlowingFluid.Properties(() -> SOY_PASTE_TYPE,
                    ModFluids::soyPasteSource, ModFluids::soyPasteFlowing)
                    .slopeFindDistance(2)
                    .levelDecreasePerBlock(2)
                    .tickRate(20);

    private static BaseFlowingFluid.Source soyPasteSource;
    private static BaseFlowingFluid.Flowing soyPasteFlowing;

    /** 静止的大酱（罐里那缸酱的「液体」形态） */
    public static BaseFlowingFluid.Source soyPasteSource() {
        if (soyPasteSource == null) {
            soyPasteSource = new BaseFlowingFluid.Source(SOY_PASTE_PROPERTIES);
        }
        return soyPasteSource;
    }

    /** 流动的大酱 */
    public static BaseFlowingFluid.Flowing soyPasteFlowing() {
        if (soyPasteFlowing == null) {
            soyPasteFlowing = new BaseFlowingFluid.Flowing(SOY_PASTE_PROPERTIES);
        }
        return soyPasteFlowing;
    }

    public static final Supplier<FluidType> SOY_PASTE_TYPE_HOLDER =
            FLUID_TYPES.register("soy_paste", () -> SOY_PASTE_TYPE);
    public static final Supplier<BaseFlowingFluid.Source> SOY_PASTE =
            FLUIDS.register("soy_paste", ModFluids::soyPasteSource);
    public static final Supplier<BaseFlowingFluid.Flowing> FLOWING_SOY_PASTE =
            FLUIDS.register("flowing_soy_paste", ModFluids::soyPasteFlowing);

    // ===== 豆浆 =====

    public static final FluidType SOY_MILK_TYPE = new DdFluidType(FluidType.Properties.create()
            .descriptionId("fluid_type.northeast_china_delight.soy_milk")
            .density(1030)
            .viscosity(1100)
            .motionScale(0.008D), "block/soy_milk_still", "block/soy_milk_flow", 0xFFFFFFFF);

    private static final BaseFlowingFluid.Properties SOY_MILK_PROPERTIES =
            new BaseFlowingFluid.Properties(() -> SOY_MILK_TYPE,
                    ModFluids::soyMilkSource, ModFluids::soyMilkFlowing)
                    .slopeFindDistance(4)
                    .levelDecreasePerBlock(1)
                    .tickRate(6);

    private static BaseFlowingFluid.Source soyMilkSource;
    private static BaseFlowingFluid.Flowing soyMilkFlowing;

    /** 静止的豆浆（动力搅拌器在工作盆里加热搅拌出来的） */
    public static BaseFlowingFluid.Source soyMilkSource() {
        if (soyMilkSource == null) {
            soyMilkSource = new BaseFlowingFluid.Source(SOY_MILK_PROPERTIES);
        }
        return soyMilkSource;
    }

    /** 流动的豆浆 */
    public static BaseFlowingFluid.Flowing soyMilkFlowing() {
        if (soyMilkFlowing == null) {
            soyMilkFlowing = new BaseFlowingFluid.Flowing(SOY_MILK_PROPERTIES);
        }
        return soyMilkFlowing;
    }

    public static final Supplier<FluidType> SOY_MILK_TYPE_HOLDER =
            FLUID_TYPES.register("soy_milk", () -> SOY_MILK_TYPE);
    public static final Supplier<BaseFlowingFluid.Source> SOY_MILK =
            FLUIDS.register("soy_milk", ModFluids::soyMilkSource);
    public static final Supplier<BaseFlowingFluid.Flowing> FLOWING_SOY_MILK =
            FLUIDS.register("flowing_soy_milk", ModFluids::soyMilkFlowing);

    // ===== 醋 =====

    public static final FluidType VINEGAR_TYPE = new DdFluidType(FluidType.Properties.create()
            .descriptionId("fluid_type.northeast_china_delight.vinegar")
            .density(1010)
            .viscosity(900)
            .motionScale(0.008D), "block/vinegar_still", "block/vinegar_flow", 0xFFFFFFFF);

    private static final BaseFlowingFluid.Properties VINEGAR_PROPERTIES =
            new BaseFlowingFluid.Properties(() -> VINEGAR_TYPE,
                    ModFluids::vinegarSource, ModFluids::vinegarFlowing)
                    .slopeFindDistance(4)
                    .levelDecreasePerBlock(1)
                    .tickRate(6);

    private static BaseFlowingFluid.Source vinegarSource;
    private static BaseFlowingFluid.Flowing vinegarFlowing;

    /** 静止的醋（酱渣 + 玉米粒 / 荞麦二次发酵出来的） */
    public static BaseFlowingFluid.Source vinegarSource() {
        if (vinegarSource == null) {
            vinegarSource = new BaseFlowingFluid.Source(VINEGAR_PROPERTIES);
        }
        return vinegarSource;
    }

    /** 流动的醋 */
    public static BaseFlowingFluid.Flowing vinegarFlowing() {
        if (vinegarFlowing == null) {
            vinegarFlowing = new BaseFlowingFluid.Flowing(VINEGAR_PROPERTIES);
        }
        return vinegarFlowing;
    }

    public static final Supplier<FluidType> VINEGAR_TYPE_HOLDER =
            FLUID_TYPES.register("vinegar", () -> VINEGAR_TYPE);
    public static final Supplier<BaseFlowingFluid.Source> VINEGAR =
            FLUIDS.register("vinegar", ModFluids::vinegarSource);
    public static final Supplier<BaseFlowingFluid.Flowing> FLOWING_VINEGAR =
            FLUIDS.register("flowing_vinegar", ModFluids::vinegarFlowing);

    // ===== 酸引水（泡菜腌完后缸里的那缸水） =====

    public static final FluidType SOUR_WATER_TYPE = new DdFluidType(FluidType.Properties.create()
            .descriptionId("fluid_type.northeast_china_delight.sour_water")
            .density(1020)
            .viscosity(1000)
            .motionScale(0.008D), "block/sour_water_still", "block/sour_water_flow", 0xFFFFFFFF);

    private static final BaseFlowingFluid.Properties SOUR_WATER_PROPERTIES =
            new BaseFlowingFluid.Properties(() -> SOUR_WATER_TYPE,
                    ModFluids::sourWaterSource, ModFluids::sourWaterFlowing)
                    .slopeFindDistance(4)
                    .levelDecreasePerBlock(1)
                    .tickRate(6);

    private static BaseFlowingFluid.Source sourWaterSource;
    private static BaseFlowingFluid.Flowing sourWaterFlowing;

    /** 静止的酸引水（泡菜发酵完成后由缸里的盐水转化而来） */
    public static BaseFlowingFluid.Source sourWaterSource() {
        if (sourWaterSource == null) {
            sourWaterSource = new BaseFlowingFluid.Source(SOUR_WATER_PROPERTIES);
        }
        return sourWaterSource;
    }

    /** 流动的酸引水 */
    public static BaseFlowingFluid.Flowing sourWaterFlowing() {
        if (sourWaterFlowing == null) {
            sourWaterFlowing = new BaseFlowingFluid.Flowing(SOUR_WATER_PROPERTIES);
        }
        return sourWaterFlowing;
    }

    public static final Supplier<FluidType> SOUR_WATER_TYPE_HOLDER =
            FLUID_TYPES.register("sour_water", () -> SOUR_WATER_TYPE);
    public static final Supplier<BaseFlowingFluid.Source> SOUR_WATER =
            FLUIDS.register("sour_water", ModFluids::sourWaterSource);
    public static final Supplier<BaseFlowingFluid.Flowing> FLOWING_SOUR_WATER =
            FLUIDS.register("flowing_sour_water", ModFluids::sourWaterFlowing);

    // ===== 白醋（酸引水 + 谷物二次发酵） =====

    public static final FluidType WHITE_VINEGAR_TYPE = new DdFluidType(FluidType.Properties.create()
            .descriptionId("fluid_type.northeast_china_delight.white_vinegar")
            .density(1005)
            .viscosity(850)
            .motionScale(0.008D), "block/white_vinegar_still", "block/white_vinegar_flow", 0xFFFFFFFF);

    private static final BaseFlowingFluid.Properties WHITE_VINEGAR_PROPERTIES =
            new BaseFlowingFluid.Properties(() -> WHITE_VINEGAR_TYPE,
                    ModFluids::whiteVinegarSource, ModFluids::whiteVinegarFlowing)
                    .slopeFindDistance(4)
                    .levelDecreasePerBlock(1)
                    .tickRate(6);

    private static BaseFlowingFluid.Source whiteVinegarSource;
    private static BaseFlowingFluid.Flowing whiteVinegarFlowing;

    /** 静止的白醋 */
    public static BaseFlowingFluid.Source whiteVinegarSource() {
        if (whiteVinegarSource == null) {
            whiteVinegarSource = new BaseFlowingFluid.Source(WHITE_VINEGAR_PROPERTIES);
        }
        return whiteVinegarSource;
    }

    /** 流动的白醋 */
    public static BaseFlowingFluid.Flowing whiteVinegarFlowing() {
        if (whiteVinegarFlowing == null) {
            whiteVinegarFlowing = new BaseFlowingFluid.Flowing(WHITE_VINEGAR_PROPERTIES);
        }
        return whiteVinegarFlowing;
    }

    public static final Supplier<FluidType> WHITE_VINEGAR_TYPE_HOLDER =
            FLUID_TYPES.register("white_vinegar", () -> WHITE_VINEGAR_TYPE);
    public static final Supplier<BaseFlowingFluid.Source> WHITE_VINEGAR =
            FLUIDS.register("white_vinegar", ModFluids::whiteVinegarSource);
    public static final Supplier<BaseFlowingFluid.Flowing> FLOWING_WHITE_VINEGAR =
            FLUIDS.register("flowing_white_vinegar", ModFluids::whiteVinegarFlowing);

    // ===== 鱼露 =====

    public static final FluidType FISH_SAUCE_TYPE = new DdFluidType(FluidType.Properties.create()
            .descriptionId("fluid_type.northeast_china_delight.fish_sauce")
            .density(1150)
            .viscosity(1000)
            .motionScale(0.006D), "block/fish_sauce_still", "block/fish_sauce_flow", 0xFFFFFFFF);

    private static final BaseFlowingFluid.Properties FISH_SAUCE_PROPERTIES =
            new BaseFlowingFluid.Properties(() -> FISH_SAUCE_TYPE,
                    ModFluids::fishSauceSource, ModFluids::fishSauceFlowing)
                    .slopeFindDistance(4)
                    .levelDecreasePerBlock(1)
                    .tickRate(6);

    private static BaseFlowingFluid.Source fishSauceSource;
    private static BaseFlowingFluid.Flowing fishSauceFlowing;

    /** 静止的鱼露 */
    public static BaseFlowingFluid.Source fishSauceSource() {
        if (fishSauceSource == null) {
            fishSauceSource = new BaseFlowingFluid.Source(FISH_SAUCE_PROPERTIES);
        }
        return fishSauceSource;
    }

    /** 流动的鱼露 */
    public static BaseFlowingFluid.Flowing fishSauceFlowing() {
        if (fishSauceFlowing == null) {
            fishSauceFlowing = new BaseFlowingFluid.Flowing(FISH_SAUCE_PROPERTIES);
        }
        return fishSauceFlowing;
    }

    public static final Supplier<FluidType> FISH_SAUCE_TYPE_HOLDER =
            FLUID_TYPES.register("fish_sauce", () -> FISH_SAUCE_TYPE);
    public static final Supplier<BaseFlowingFluid.Source> FISH_SAUCE =
            FLUIDS.register("fish_sauce", ModFluids::fishSauceSource);
    public static final Supplier<BaseFlowingFluid.Flowing> FLOWING_FISH_SAUCE =
            FLUIDS.register("flowing_fish_sauce", ModFluids::fishSauceFlowing);

    // ===== 虾酱 =====

    public static final FluidType SHRIMP_PASTE_TYPE = new DdFluidType(FluidType.Properties.create()
            .descriptionId("fluid_type.northeast_china_delight.shrimp_paste")
            .density(1300)
            .viscosity(3000)
            .motionScale(0.005D), "block/shrimp_paste_still", "block/shrimp_paste_still", 0xFF8A4A34);

    private static final BaseFlowingFluid.Properties SHRIMP_PASTE_PROPERTIES =
            new BaseFlowingFluid.Properties(() -> SHRIMP_PASTE_TYPE,
                    ModFluids::shrimpPasteSource, ModFluids::shrimpPasteFlowing)
                    .slopeFindDistance(2)
                    .levelDecreasePerBlock(2)
                    .tickRate(20);

    private static BaseFlowingFluid.Source shrimpPasteSource;
    private static BaseFlowingFluid.Flowing shrimpPasteFlowing;

    /** 静止的虾酱 */
    public static BaseFlowingFluid.Source shrimpPasteSource() {
        if (shrimpPasteSource == null) {
            shrimpPasteSource = new BaseFlowingFluid.Source(SHRIMP_PASTE_PROPERTIES);
        }
        return shrimpPasteSource;
    }

    /** 流动的虾酱 */
    public static BaseFlowingFluid.Flowing shrimpPasteFlowing() {
        if (shrimpPasteFlowing == null) {
            shrimpPasteFlowing = new BaseFlowingFluid.Flowing(SHRIMP_PASTE_PROPERTIES);
        }
        return shrimpPasteFlowing;
    }

    public static final Supplier<FluidType> SHRIMP_PASTE_TYPE_HOLDER =
            FLUID_TYPES.register("shrimp_paste", () -> SHRIMP_PASTE_TYPE);
    public static final Supplier<BaseFlowingFluid.Source> SHRIMP_PASTE =
            FLUIDS.register("shrimp_paste", ModFluids::shrimpPasteSource);
    public static final Supplier<BaseFlowingFluid.Flowing> FLOWING_SHRIMP_PASTE =
            FLUIDS.register("flowing_shrimp_paste", ModFluids::shrimpPasteFlowing);

    // ===== 植物油 =====

    public static final FluidType VEGETABLE_OIL_TYPE = new DdFluidType(FluidType.Properties.create()
            .descriptionId("fluid_type.northeast_china_delight.vegetable_oil")
            .density(920)
            .viscosity(1400)
            .motionScale(0.006D), "block/vegetable_oil_still", "block/vegetable_oil_flow", 0xFFFFFFFF);

    private static final BaseFlowingFluid.Properties VEGETABLE_OIL_PROPERTIES =
            new BaseFlowingFluid.Properties(() -> VEGETABLE_OIL_TYPE,
                    ModFluids::vegetableOilSource, ModFluids::vegetableOilFlowing)
                    .slopeFindDistance(4)
                    .levelDecreasePerBlock(1)
                    .tickRate(6);

    private static BaseFlowingFluid.Source vegetableOilSource;
    private static BaseFlowingFluid.Flowing vegetableOilFlowing;

    /** 静止的植物油（动力冲压机压熟花生米挤出来的） */
    public static BaseFlowingFluid.Source vegetableOilSource() {
        if (vegetableOilSource == null) {
            vegetableOilSource = new BaseFlowingFluid.Source(VEGETABLE_OIL_PROPERTIES);
        }
        return vegetableOilSource;
    }

    /** 流动的植物油 */
    public static BaseFlowingFluid.Flowing vegetableOilFlowing() {
        if (vegetableOilFlowing == null) {
            vegetableOilFlowing = new BaseFlowingFluid.Flowing(VEGETABLE_OIL_PROPERTIES);
        }
        return vegetableOilFlowing;
    }

    public static final Supplier<FluidType> VEGETABLE_OIL_TYPE_HOLDER =
            FLUID_TYPES.register("vegetable_oil", () -> VEGETABLE_OIL_TYPE);
    public static final Supplier<BaseFlowingFluid.Source> VEGETABLE_OIL =
            FLUIDS.register("vegetable_oil", ModFluids::vegetableOilSource);
    public static final Supplier<BaseFlowingFluid.Flowing> FLOWING_VEGETABLE_OIL =
            FLUIDS.register("flowing_vegetable_oil", ModFluids::vegetableOilFlowing);

    // ===== 动物油 =====

    public static final FluidType ANIMAL_OIL_TYPE = new DdFluidType(FluidType.Properties.create()
            .descriptionId("fluid_type.northeast_china_delight.animal_oil")
            .density(900)
            .viscosity(1800)
            .motionScale(0.005D), "block/animal_oil_still", "block/animal_oil_flow", 0xFFFFFFFF);

    private static final BaseFlowingFluid.Properties ANIMAL_OIL_PROPERTIES =
            new BaseFlowingFluid.Properties(() -> ANIMAL_OIL_TYPE,
                    ModFluids::animalOilSource, ModFluids::animalOilFlowing)
                    .slopeFindDistance(3)
                    .levelDecreasePerBlock(1)
                    .tickRate(8);

    private static BaseFlowingFluid.Source animalOilSource;
    private static BaseFlowingFluid.Flowing animalOilFlowing;

    /** 静止的动物油（动力搅拌器把肥肉熬出来的液态油） */
    public static BaseFlowingFluid.Source animalOilSource() {
        if (animalOilSource == null) {
            animalOilSource = new BaseFlowingFluid.Source(ANIMAL_OIL_PROPERTIES);
        }
        return animalOilSource;
    }

    /** 流动的动物油 */
    public static BaseFlowingFluid.Flowing animalOilFlowing() {
        if (animalOilFlowing == null) {
            animalOilFlowing = new BaseFlowingFluid.Flowing(ANIMAL_OIL_PROPERTIES);
        }
        return animalOilFlowing;
    }

    public static final Supplier<FluidType> ANIMAL_OIL_TYPE_HOLDER =
            FLUID_TYPES.register("animal_oil", () -> ANIMAL_OIL_TYPE);
    public static final Supplier<BaseFlowingFluid.Source> ANIMAL_OIL =
            FLUIDS.register("animal_oil", ModFluids::animalOilSource);
    public static final Supplier<BaseFlowingFluid.Flowing> FLOWING_ANIMAL_OIL =
            FLUIDS.register("flowing_animal_oil", ModFluids::animalOilFlowing);

    // ===== 花生酱 =====
    // 注：这瓶「酱」是能流动的液体 —— 大缸里虾酱 6 虾 3 盐出一瓶，
    // 花生酱就是把花生熬出油的稠酱，物理上跟油差不多。

    public static final FluidType PEANUT_BUTTER_TYPE = new DdFluidType(FluidType.Properties.create()
            .descriptionId("fluid_type.northeast_china_delight.peanut_butter")
            .density(1400)
            .viscosity(6000)
            .motionScale(0.004D), "block/peanut_butter_still", "block/peanut_butter_still", 0xFFC9A05A);

    private static final BaseFlowingFluid.Properties PEANUT_BUTTER_PROPERTIES =
            new BaseFlowingFluid.Properties(() -> PEANUT_BUTTER_TYPE,
                    ModFluids::peanutButterSource, ModFluids::peanutButterFlowing)
                    .slopeFindDistance(2)
                    .levelDecreasePerBlock(2)
                    .tickRate(12);

    private static BaseFlowingFluid.Source peanutButterSource;
    private static BaseFlowingFluid.Flowing peanutButterFlowing;

    public static BaseFlowingFluid.Source peanutButterSource() {
        if (peanutButterSource == null) {
            peanutButterSource = new BaseFlowingFluid.Source(PEANUT_BUTTER_PROPERTIES);
        }
        return peanutButterSource;
    }

    public static BaseFlowingFluid.Flowing peanutButterFlowing() {
        if (peanutButterFlowing == null) {
            peanutButterFlowing = new BaseFlowingFluid.Flowing(PEANUT_BUTTER_PROPERTIES);
        }
        return peanutButterFlowing;
    }

    public static final Supplier<FluidType> PEANUT_BUTTER_TYPE_HOLDER =
            FLUID_TYPES.register("peanut_butter", () -> PEANUT_BUTTER_TYPE);
    public static final Supplier<BaseFlowingFluid.Source> PEANUT_BUTTER =
            FLUIDS.register("peanut_butter", ModFluids::peanutButterSource);
    public static final Supplier<BaseFlowingFluid.Flowing> FLOWING_PEANUT_BUTTER =
            FLUIDS.register("flowing_peanut_butter", ModFluids::peanutButterFlowing);

    // ===== 辣椒油 =====

    public static final FluidType CHILI_OIL_TYPE = new DdFluidType(FluidType.Properties.create()
            .descriptionId("fluid_type.northeast_china_delight.chili_oil")
            .density(920)
            .viscosity(1600)
            .motionScale(0.006D), "block/chili_oil_still", "block/chili_oil_flow", 0xFFFFFFFF);

    private static final BaseFlowingFluid.Properties CHILI_OIL_PROPERTIES =
            new BaseFlowingFluid.Properties(() -> CHILI_OIL_TYPE,
                    ModFluids::chiliOilSource, ModFluids::chiliOilFlowing)
                    .slopeFindDistance(3)
                    .levelDecreasePerBlock(1)
                    .tickRate(7);

    private static BaseFlowingFluid.Source chiliOilSource;
    private static BaseFlowingFluid.Flowing chiliOilFlowing;

    public static BaseFlowingFluid.Source chiliOilSource() {
        if (chiliOilSource == null) {
            chiliOilSource = new BaseFlowingFluid.Source(CHILI_OIL_PROPERTIES);
        }
        return chiliOilSource;
    }

    public static BaseFlowingFluid.Flowing chiliOilFlowing() {
        if (chiliOilFlowing == null) {
            chiliOilFlowing = new BaseFlowingFluid.Flowing(CHILI_OIL_PROPERTIES);
        }
        return chiliOilFlowing;
    }

    public static final Supplier<FluidType> CHILI_OIL_TYPE_HOLDER =
            FLUID_TYPES.register("chili_oil", () -> CHILI_OIL_TYPE);
    public static final Supplier<BaseFlowingFluid.Source> CHILI_OIL =
            FLUIDS.register("chili_oil", ModFluids::chiliOilSource);
    public static final Supplier<BaseFlowingFluid.Flowing> FLOWING_CHILI_OIL =
            FLUIDS.register("flowing_chili_oil", ModFluids::chiliOilFlowing);

    // ===== 辣椒酱 =====
    // 碗装的稠调料：2 个红辣椒 + 盐捣出来的糊，稠度跟虾酱一个量级。

    public static final FluidType CHILI_SAUCE_TYPE = new DdFluidType(FluidType.Properties.create()
            .descriptionId("fluid_type.northeast_china_delight.chili_sauce")
            .density(1350)
            .viscosity(4000)
            .motionScale(0.005D), "block/chili_sauce_still", "block/chili_sauce_still", 0xFFE04A1F);

    private static final BaseFlowingFluid.Properties CHILI_SAUCE_PROPERTIES =
            new BaseFlowingFluid.Properties(() -> CHILI_SAUCE_TYPE,
                    ModFluids::chiliSauceSource, ModFluids::chiliSauceFlowing)
                    .slopeFindDistance(2)
                    .levelDecreasePerBlock(2)
                    .tickRate(18);

    private static BaseFlowingFluid.Source chiliSauceSource;
    private static BaseFlowingFluid.Flowing chiliSauceFlowing;

    public static BaseFlowingFluid.Source chiliSauceSource() {
        if (chiliSauceSource == null) {
            chiliSauceSource = new BaseFlowingFluid.Source(CHILI_SAUCE_PROPERTIES);
        }
        return chiliSauceSource;
    }

    public static BaseFlowingFluid.Flowing chiliSauceFlowing() {
        if (chiliSauceFlowing == null) {
            chiliSauceFlowing = new BaseFlowingFluid.Flowing(CHILI_SAUCE_PROPERTIES);
        }
        return chiliSauceFlowing;
    }

    public static final Supplier<FluidType> CHILI_SAUCE_TYPE_HOLDER =
            FLUID_TYPES.register("chili_sauce", () -> CHILI_SAUCE_TYPE);
    public static final Supplier<BaseFlowingFluid.Source> CHILI_SAUCE =
            FLUIDS.register("chili_sauce", ModFluids::chiliSauceSource);
    public static final Supplier<BaseFlowingFluid.Flowing> FLOWING_CHILI_SAUCE =
            FLUIDS.register("flowing_chili_sauce", ModFluids::chiliSauceFlowing);

    // ===== 格瓦斯 =====
    // 大缸里发酵出来的饮料，比水稠一点点，颜色像焦糖。

    public static final FluidType KVASS_TYPE = new DdFluidType(FluidType.Properties.create()
            .descriptionId("fluid_type.northeast_china_delight.kvass")
            .density(1050)
            .viscosity(1100)
            .motionScale(0.007D), "block/kvass_still", "block/kvass_flow", 0xFFFFFFFF);

    private static final BaseFlowingFluid.Properties KVASS_PROPERTIES =
            new BaseFlowingFluid.Properties(() -> KVASS_TYPE,
                    ModFluids::kvassSource, ModFluids::kvassFlowing)
                    .slopeFindDistance(4)
                    .levelDecreasePerBlock(1)
                    .tickRate(5);

    private static BaseFlowingFluid.Source kvassSource;
    private static BaseFlowingFluid.Flowing kvassFlowing;

    public static BaseFlowingFluid.Source kvassSource() {
        if (kvassSource == null) {
            kvassSource = new BaseFlowingFluid.Source(KVASS_PROPERTIES);
        }
        return kvassSource;
    }

    public static BaseFlowingFluid.Flowing kvassFlowing() {
        if (kvassFlowing == null) {
            kvassFlowing = new BaseFlowingFluid.Flowing(KVASS_PROPERTIES);
        }
        return kvassFlowing;
    }

    public static final Supplier<FluidType> KVASS_TYPE_HOLDER =
            FLUID_TYPES.register("kvass", () -> KVASS_TYPE);
    public static final Supplier<BaseFlowingFluid.Source> KVASS =
            FLUIDS.register("kvass", ModFluids::kvassSource);
    public static final Supplier<BaseFlowingFluid.Flowing> FLOWING_KVASS =
            FLUIDS.register("flowing_kvass", ModFluids::kvassFlowing);

    public static void register(IEventBus eventBus) {
        FLUID_TYPES.register(eventBus);
        FLUIDS.register(eventBus);
    }

    /**
     * 本模组的流体类型。
     *
     * <p>「这个流体在客户端长什么样」（静/流贴图、染色）两个版本的挂法不同：
     * 1.20.1 没有 {@code RegisterClientExtensionsEvent}，只能覆写 {@link FluidType#initializeClient}
     * —— 好在它只在客户端会被调用（构造器里带 Dist 判断），服务端不会加载这段；
     * 1.21 起由客户端事件统一注册，这个类就只是个普通 FluidType。</p>
     */
    private static class DdFluidType extends FluidType {

        private final String stillTexture;
        private final String flowingTexture;
        private final int tintColor;

        DdFluidType(Properties properties, String stillTexture, String flowingTexture, int tintColor) {
            super(properties);
            this.stillTexture = stillTexture;
            this.flowingTexture = flowingTexture;
            this.tintColor = tintColor;
        }

        //? if <1.20.5 {
        @Override
        public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
            consumer.accept(new IClientFluidTypeExtensions() {
                @Override
                public ResourceLocation getStillTexture() {
                    return DdIds.of(NortheastChinaDelight.MODID, DdFluidType.this.stillTexture);
                }

                @Override
                public ResourceLocation getFlowingTexture() {
                    return DdIds.of(NortheastChinaDelight.MODID, DdFluidType.this.flowingTexture);
                }

                @Override
                public int getTintColor() {
                    return DdFluidType.this.tintColor;
                }
            });
        }
        //?}
    }
}
