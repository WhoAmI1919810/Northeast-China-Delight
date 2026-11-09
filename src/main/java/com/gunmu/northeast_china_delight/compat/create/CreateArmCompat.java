package com.gunmu.northeast_china_delight.compat.create;

import com.gunmu.northeast_china_delight.NortheastChinaDelight;
import com.gunmu.northeast_china_delight.block.BigStoveBlock;
import com.gunmu.northeast_china_delight.util.DdIds;
import com.simibubi.create.content.kinetics.mechanicalArm.AllArmInteractionPointTypes;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPoint;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPointType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
//? if <1.20.2 {
import com.simibubi.create.api.registry.CreateRegistries;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.RegisterEvent;
//?} else {
/*import com.simibubi.create.api.registry.CreateRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
*///?}

/**
 * 让机械动力动力臂直接把物品投入大锅。大锅本身没有 Create 的交互点，
 * 只在装了机械动力时注册一个「只进不出」的交互点，实际投料仍走
 * {@link com.gunmu.northeast_china_delight.block.BigPotItemHandler}，
 * 因此瓶装 / 碗装调料和水盆的返还物仍由 {@code BigPotCooking#output} 统一处理。
 */
public final class CreateArmCompat {
    private CreateArmCompat() {
    }

    //? if <1.20.2 {
    public static void register(IEventBus bus) {
        bus.addListener(CreateArmCompat::onRegister);
    }

    private static void onRegister(RegisterEvent event) {
        // Create 的自定义注册表同样在 RegisterEvent 阶段可写；common setup 时已经冻结。
        event.register(CreateRegistries.ARM_INTERACTION_POINT_TYPE,
                DdIds.of(NortheastChinaDelight.MODID, "big_pot"), BigPotPointType::new);
    }
    //?} else {
    /*private static final DeferredRegister<ArmInteractionPointType> TYPES =
            DeferredRegister.create(CreateRegistries.ARM_INTERACTION_POINT_TYPE, NortheastChinaDelight.MODID);

    static {
        TYPES.register("big_pot", BigPotPointType::new);
    }

    public static void register(IEventBus bus) {
        TYPES.register(bus);
        bus.addListener(CreateArmCompat::commonSetup);
    }

    private static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(ArmInteractionPointType::init);
    }
    *///?}

    private static final class BigPotPointType extends ArmInteractionPointType {
        @Override
        public int getPriority() {
            // 只匹配成型的锅，优先级高一些，避免被其他模组的通用方块交互点抢走。
            return 100;
        }

        @Override
        public boolean canCreatePoint(Level level, BlockPos pos, BlockState state) {
            return state.getBlock() instanceof BigStoveBlock
                    && state.getValue(BigStoveBlock.PART) == BigStoveBlock.Part.NW
                    && state.getValue(BigStoveBlock.POT) == BigStoveBlock.Pot.POT;
        }

        @Nullable
        @Override
        public ArmInteractionPoint createPoint(Level level, BlockPos pos, BlockState state) {
            return new BigPotPoint(this, level, pos, state);
        }
    }

    private static final class BigPotPoint extends AllArmInteractionPointTypes.DepositOnlyArmInteractionPoint {
        private BigPotPoint(ArmInteractionPointType type, Level level, BlockPos pos, BlockState state) {
            super(type, level, pos, state);
        }

        @Override
        protected Vec3 getInteractionPositionVector() {
            BlockPos origin = BigStoveBlock.origin(pos, cachedState);
            return new Vec3(origin.getX() + 1.0D, origin.getY() + 1.05D, origin.getZ() + 1.0D);
        }
    }
}
