package com.gunmu.northeast_china_delight.compat.create;

import com.simibubi.create.content.fluids.drain.ItemDrainBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;
//? if <1.20.2 {
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
//?} else {
/*import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
*///?}

/**
 * 压水井给机械动力分液池灌水。
 *
 * <p>分液池的方块流体能力默认 {@code forbidInsertion()}，普通 {@code fill} 永远返回 0；
 * 它只允许物品把流体排进来，管道只能抽。压水井是手动倒水，因此这里取 behaviour 的主储罐直接灌，
 * 不改变分液池对管道仍禁止注入的原版规则。只在 {@code OptionalMods.create()} 为 true 时调用。</p>
 */
public final class CreateHandPumpCompat {
    private CreateHandPumpCompat() {
    }

    public static int fillItemDrain(Level level, BlockPos pos, int amount) {
        if (!(level.getBlockEntity(pos) instanceof ItemDrainBlockEntity drain)) {
            return 0;
        }
        SmartFluidTankBehaviour tank = drain.getBehaviour(SmartFluidTankBehaviour.TYPE);
        if (tank == null) {
            return 0;
        }
        return tank.getPrimaryHandler().fill(new FluidStack(Fluids.WATER, amount),
                IFluidHandler.FluidAction.EXECUTE);
    }
}
