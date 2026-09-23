package com.gunmu.dongbei_delight.fluid;

import com.gunmu.dongbei_delight.DongbeiDelight;
import com.gunmu.dongbei_delight.block.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

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
            DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, DongbeiDelight.MODID);
    public static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(Registries.FLUID, DongbeiDelight.MODID);

    // ===== 酱油 =====

    public static final FluidType SOY_SAUCE_TYPE = new FluidType(FluidType.Properties.create()
            .descriptionId("fluid_type.dongbei_delight.soy_sauce")
            .density(1100)
            .viscosity(1200)
            .motionScale(0.006D));

    private static final BaseFlowingFluid.Properties SOY_SAUCE_PROPERTIES =
            new BaseFlowingFluid.Properties(() -> SOY_SAUCE_TYPE,
                    ModFluids::soySauceSource, ModFluids::soySauceFlowing)
                    .block(() -> ModBlocks.SOY_SAUCE_FLUID.get())
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

    public static final DeferredHolder<FluidType, FluidType> SOY_SAUCE_TYPE_HOLDER =
            FLUID_TYPES.register("soy_sauce", () -> SOY_SAUCE_TYPE);
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> SOY_SAUCE =
            FLUIDS.register("soy_sauce", ModFluids::soySauceSource);
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FLOWING_SOY_SAUCE =
            FLUIDS.register("flowing_soy_sauce", ModFluids::soySauceFlowing);

    // ===== 大酱 =====

    public static final FluidType SOY_PASTE_TYPE = new FluidType(FluidType.Properties.create()
            .descriptionId("fluid_type.dongbei_delight.soy_paste")
            .density(1250)
            .viscosity(3200)
            .motionScale(0.004D));

    private static final BaseFlowingFluid.Properties SOY_PASTE_PROPERTIES =
            new BaseFlowingFluid.Properties(() -> SOY_PASTE_TYPE,
                    ModFluids::soyPasteSource, ModFluids::soyPasteFlowing)
                    .block(() -> ModBlocks.SOY_PASTE_FLUID.get())
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

    public static final DeferredHolder<FluidType, FluidType> SOY_PASTE_TYPE_HOLDER =
            FLUID_TYPES.register("soy_paste", () -> SOY_PASTE_TYPE);
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> SOY_PASTE =
            FLUIDS.register("soy_paste", ModFluids::soyPasteSource);
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FLOWING_SOY_PASTE =
            FLUIDS.register("flowing_soy_paste", ModFluids::soyPasteFlowing);

    // ===== 豆浆 =====

    public static final FluidType SOY_MILK_TYPE = new FluidType(FluidType.Properties.create()
            .descriptionId("fluid_type.dongbei_delight.soy_milk")
            .density(1030)
            .viscosity(1100)
            .motionScale(0.008D));

    private static final BaseFlowingFluid.Properties SOY_MILK_PROPERTIES =
            new BaseFlowingFluid.Properties(() -> SOY_MILK_TYPE,
                    ModFluids::soyMilkSource, ModFluids::soyMilkFlowing)
                    .block(() -> ModBlocks.SOY_MILK_FLUID.get())
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

    public static final DeferredHolder<FluidType, FluidType> SOY_MILK_TYPE_HOLDER =
            FLUID_TYPES.register("soy_milk", () -> SOY_MILK_TYPE);
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> SOY_MILK =
            FLUIDS.register("soy_milk", ModFluids::soyMilkSource);
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FLOWING_SOY_MILK =
            FLUIDS.register("flowing_soy_milk", ModFluids::soyMilkFlowing);

    // ===== 醋 =====

    public static final FluidType VINEGAR_TYPE = new FluidType(FluidType.Properties.create()
            .descriptionId("fluid_type.dongbei_delight.vinegar")
            .density(1010)
            .viscosity(900)
            .motionScale(0.008D));

    private static final BaseFlowingFluid.Properties VINEGAR_PROPERTIES =
            new BaseFlowingFluid.Properties(() -> VINEGAR_TYPE,
                    ModFluids::vinegarSource, ModFluids::vinegarFlowing)
                    .block(() -> ModBlocks.VINEGAR_FLUID.get())
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

    public static final DeferredHolder<FluidType, FluidType> VINEGAR_TYPE_HOLDER =
            FLUID_TYPES.register("vinegar", () -> VINEGAR_TYPE);
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> VINEGAR =
            FLUIDS.register("vinegar", ModFluids::vinegarSource);
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FLOWING_VINEGAR =
            FLUIDS.register("flowing_vinegar", ModFluids::vinegarFlowing);

    // ===== 酸引水（泡菜腌完后缸里的那缸水） =====

    public static final FluidType SOUR_WATER_TYPE = new FluidType(FluidType.Properties.create()
            .descriptionId("fluid_type.dongbei_delight.sour_water")
            .density(1020)
            .viscosity(1000)
            .motionScale(0.008D));

    private static final BaseFlowingFluid.Properties SOUR_WATER_PROPERTIES =
            new BaseFlowingFluid.Properties(() -> SOUR_WATER_TYPE,
                    ModFluids::sourWaterSource, ModFluids::sourWaterFlowing)
                    .block(() -> ModBlocks.SOUR_WATER_FLUID.get())
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

    public static final DeferredHolder<FluidType, FluidType> SOUR_WATER_TYPE_HOLDER =
            FLUID_TYPES.register("sour_water", () -> SOUR_WATER_TYPE);
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> SOUR_WATER =
            FLUIDS.register("sour_water", ModFluids::sourWaterSource);
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FLOWING_SOUR_WATER =
            FLUIDS.register("flowing_sour_water", ModFluids::sourWaterFlowing);

    // ===== 白醋（酸引水 + 谷物二次发酵） =====

    public static final FluidType WHITE_VINEGAR_TYPE = new FluidType(FluidType.Properties.create()
            .descriptionId("fluid_type.dongbei_delight.white_vinegar")
            .density(1005)
            .viscosity(850)
            .motionScale(0.008D));

    private static final BaseFlowingFluid.Properties WHITE_VINEGAR_PROPERTIES =
            new BaseFlowingFluid.Properties(() -> WHITE_VINEGAR_TYPE,
                    ModFluids::whiteVinegarSource, ModFluids::whiteVinegarFlowing)
                    .block(() -> ModBlocks.WHITE_VINEGAR_FLUID.get())
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

    public static final DeferredHolder<FluidType, FluidType> WHITE_VINEGAR_TYPE_HOLDER =
            FLUID_TYPES.register("white_vinegar", () -> WHITE_VINEGAR_TYPE);
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> WHITE_VINEGAR =
            FLUIDS.register("white_vinegar", ModFluids::whiteVinegarSource);
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FLOWING_WHITE_VINEGAR =
            FLUIDS.register("flowing_white_vinegar", ModFluids::whiteVinegarFlowing);

    // ===== 鱼露 =====

    public static final FluidType FISH_SAUCE_TYPE = new FluidType(FluidType.Properties.create()
            .descriptionId("fluid_type.dongbei_delight.fish_sauce")
            .density(1150)
            .viscosity(1000)
            .motionScale(0.006D));

    private static final BaseFlowingFluid.Properties FISH_SAUCE_PROPERTIES =
            new BaseFlowingFluid.Properties(() -> FISH_SAUCE_TYPE,
                    ModFluids::fishSauceSource, ModFluids::fishSauceFlowing)
                    .block(() -> ModBlocks.FISH_SAUCE_FLUID.get())
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

    public static final DeferredHolder<FluidType, FluidType> FISH_SAUCE_TYPE_HOLDER =
            FLUID_TYPES.register("fish_sauce", () -> FISH_SAUCE_TYPE);
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> FISH_SAUCE =
            FLUIDS.register("fish_sauce", ModFluids::fishSauceSource);
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FLOWING_FISH_SAUCE =
            FLUIDS.register("flowing_fish_sauce", ModFluids::fishSauceFlowing);

    // ===== 虾酱 =====

    public static final FluidType SHRIMP_PASTE_TYPE = new FluidType(FluidType.Properties.create()
            .descriptionId("fluid_type.dongbei_delight.shrimp_paste")
            .density(1300)
            .viscosity(3000)
            .motionScale(0.005D));

    private static final BaseFlowingFluid.Properties SHRIMP_PASTE_PROPERTIES =
            new BaseFlowingFluid.Properties(() -> SHRIMP_PASTE_TYPE,
                    ModFluids::shrimpPasteSource, ModFluids::shrimpPasteFlowing)
                    .block(() -> ModBlocks.SHRIMP_PASTE_FLUID.get())
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

    public static final DeferredHolder<FluidType, FluidType> SHRIMP_PASTE_TYPE_HOLDER =
            FLUID_TYPES.register("shrimp_paste", () -> SHRIMP_PASTE_TYPE);
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> SHRIMP_PASTE =
            FLUIDS.register("shrimp_paste", ModFluids::shrimpPasteSource);
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FLOWING_SHRIMP_PASTE =
            FLUIDS.register("flowing_shrimp_paste", ModFluids::shrimpPasteFlowing);

    // ===== 植物油 =====

    public static final FluidType VEGETABLE_OIL_TYPE = new FluidType(FluidType.Properties.create()
            .descriptionId("fluid_type.dongbei_delight.vegetable_oil")
            .density(920)
            .viscosity(1400)
            .motionScale(0.006D));

    private static final BaseFlowingFluid.Properties VEGETABLE_OIL_PROPERTIES =
            new BaseFlowingFluid.Properties(() -> VEGETABLE_OIL_TYPE,
                    ModFluids::vegetableOilSource, ModFluids::vegetableOilFlowing)
                    .block(() -> ModBlocks.VEGETABLE_OIL_FLUID.get())
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

    public static final DeferredHolder<FluidType, FluidType> VEGETABLE_OIL_TYPE_HOLDER =
            FLUID_TYPES.register("vegetable_oil", () -> VEGETABLE_OIL_TYPE);
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> VEGETABLE_OIL =
            FLUIDS.register("vegetable_oil", ModFluids::vegetableOilSource);
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FLOWING_VEGETABLE_OIL =
            FLUIDS.register("flowing_vegetable_oil", ModFluids::vegetableOilFlowing);

    // ===== 动物油 =====

    public static final FluidType ANIMAL_OIL_TYPE = new FluidType(FluidType.Properties.create()
            .descriptionId("fluid_type.dongbei_delight.animal_oil")
            .density(900)
            .viscosity(1800)
            .motionScale(0.005D));

    private static final BaseFlowingFluid.Properties ANIMAL_OIL_PROPERTIES =
            new BaseFlowingFluid.Properties(() -> ANIMAL_OIL_TYPE,
                    ModFluids::animalOilSource, ModFluids::animalOilFlowing)
                    .block(() -> ModBlocks.ANIMAL_OIL_FLUID.get())
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

    public static final DeferredHolder<FluidType, FluidType> ANIMAL_OIL_TYPE_HOLDER =
            FLUID_TYPES.register("animal_oil", () -> ANIMAL_OIL_TYPE);
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> ANIMAL_OIL =
            FLUIDS.register("animal_oil", ModFluids::animalOilSource);
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FLOWING_ANIMAL_OIL =
            FLUIDS.register("flowing_animal_oil", ModFluids::animalOilFlowing);

    public static void register(IEventBus eventBus) {
        FLUID_TYPES.register(eventBus);
        FLUIDS.register(eventBus);
    }
}
