package com.gunmu.northeast_china_delight.fluid;

import com.gunmu.northeast_china_delight.NortheastChinaDelight;
import com.gunmu.northeast_china_delight.util.DdIds;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
//? if <1.20.2 {
/*import net.minecraftforge.eventbus.api.IEventBus;
*///?} else {
import net.neoforged.bus.api.IEventBus;
//?}
//? if <1.20.2 {
/*import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
*///?} else {
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
//?}
//? if >=1.20.2 {
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
//?}
//? if <1.20.2 {
/*import net.minecraftforge.fluids.FluidType;
*///?} else {
import net.neoforged.neoforge.fluids.FluidType;
//?}
//? if <1.20.2 {
/*import net.minecraftforge.registries.DeferredRegister;
*///?} else {
import net.neoforged.neoforge.registries.DeferredRegister;
//?}
//? if <1.20.2 {
/*import net.minecraftforge.registries.ForgeRegistries;
*///?} else {
import net.neoforged.neoforge.registries.NeoForgeRegistries;
//?}

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 东北乐事自有流体：豆浆与格瓦斯。通用调味流体已迁到 CRCI，由 CrciFluids 注册。
 *
 * 关于「懒加载」：{@code Fluid} 的构造函数会往流体注册表里挂一个 intrusive holder，
 * 这要求注册表处于「解冻」状态 —— 也就是只能在 RegisterEvent 期间创建。
 * 所以这里的实例都做成按需创建：谁先用到（流体注册或其它调用）就先创建。
 */
public final class ModFluids {

    private ModFluids() {
    }

    public static final DeferredRegister<FluidType> FLUID_TYPES =
            //? if <1.20.2 {
            /*DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES, NortheastChinaDelight.MODID);
            *///?} else {
            DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, NortheastChinaDelight.MODID);
            //?}
    public static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(Registries.FLUID, NortheastChinaDelight.MODID);

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

    public static BaseFlowingFluid.Source soyMilkSource() {
        if (soyMilkSource == null) {
            soyMilkSource = new BaseFlowingFluid.Source(SOY_MILK_PROPERTIES);
        }
        return soyMilkSource;
    }

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
     * 本模组的流体类型。「这个流体在客户端长什么样」（静/流贴图、染色）两个版本的挂法不同： 1.20.1 没有
     * {@code RegisterClientExtensionsEvent}，只能覆写 {@link FluidType#initializeClient} —— 好在它只在客户
     * 端会被调用（构造器里带 Dist 判断），服务端不会加载这段； 1.21 起由客户端事件统一注册，这个类就只是个普通 FluidType。
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
        /*@Override
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
        }*/
        //?}
    }
}
