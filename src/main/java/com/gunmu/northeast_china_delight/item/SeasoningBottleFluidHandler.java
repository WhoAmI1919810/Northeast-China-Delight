package com.gunmu.northeast_china_delight.item;

import com.gunmu.northeast_china_delight.fluid.ModFluids;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
//? if <1.20.2 {
/*import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
*///?} else {
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
//?}
import org.jetbrains.annotations.Nullable;

/**
 * 调料瓶的流体能力：让机械动力的分液池能倒出瓶子里实际剩下的液体。
 *
 * <p>余量存在原版耐久上（damage = 已用掉的 mB，见 {@link SeasoningBottleItem}），
 * 所以倒空时按 {@link SeasoningBottleItem#remainingMb} 给量，而不是固定 250 mB。
 * 倒空之后容器变成空玻璃瓶。
 */
public final class SeasoningBottleFluidHandler implements IFluidHandlerItem {

    private ItemStack stack;

    private SeasoningBottleFluidHandler(ItemStack stack) {
        this.stack = stack;
    }

    /** 这种调料瓶对应的流体；没有对应流体的瓶子（食用油）返回 null，不暴露能力 */
    @Nullable
    public static Fluid fluidOf(Item item) {
        if (item == ModItems.SOY_SAUCE.get()) return ModFluids.soySauceSource();
        if (item == ModItems.VINEGAR.get()) return ModFluids.vinegarSource();
        if (item == ModItems.WHITE_VINEGAR.get()) return ModFluids.whiteVinegarSource();
        if (item == ModItems.FISH_SAUCE.get()) return ModFluids.fishSauceSource();
        if (item == ModItems.SHRIMP_PASTE.get()) return ModFluids.shrimpPasteSource();
        if (item == ModItems.KVASS.get()) return ModFluids.kvassSource();
        if (item == ModItems.SOUR_WATER.get()) return ModFluids.sourWaterSource();
        if (item == ModItems.PEANUT_BUTTER.get()) return ModFluids.peanutButterSource();
        if (item == ModItems.COOKING_OIL.get()) return ModFluids.vegetableOilSource();
        if (item == ModItems.ANIMAL_OIL.get()) return ModFluids.animalOilSource();
        if (item == ModItems.CHILI_OIL.get()) return ModFluids.chiliOilSource();
        return null;
    }

    /** 给能力注册用：这种物品没有对应流体时返回 null */
    @Nullable
    public static SeasoningBottleFluidHandler of(ItemStack stack) {
        return fluidOf(stack.getItem()) == null ? null : new SeasoningBottleFluidHandler(stack);
    }

    //? if <1.20.2 {
    /*@Nullable
    public static net.minecraftforge.common.capabilities.ICapabilityProvider provider(ItemStack stack) {
        // 1.20.1（Forge）的物品能力入口：initCapabilities 要返回 ICapabilityProvider，
        // 这里用 LazyOptional 把 FLUID_HANDLER_ITEM 包成 provider。没有对应流体的瓶子（食用油）返回 null
        SeasoningBottleFluidHandler handler = of(stack);
        if (handler == null) {
            return null;
        }
        net.minecraftforge.common.util.LazyOptional<IFluidHandlerItem> opt =
                net.minecraftforge.common.util.LazyOptional.of(() -> handler);
        return new net.minecraftforge.common.capabilities.ICapabilityProvider() {
            @Override
            public <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(
                    net.minecraftforge.common.capabilities.Capability<T> capability,
                    @Nullable net.minecraft.core.Direction side) {
                return capability == net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER_ITEM
                        ? opt.cast() : net.minecraftforge.common.util.LazyOptional.empty();
            }
        };
    }*/
    //?}

    @Override
    public ItemStack getContainer() {
        return this.stack;
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        Fluid fluid = fluidOf(this.stack.getItem());
        int amount = SeasoningBottleItem.remainingMb(this.stack);
        return fluid == null || amount <= 0 ? FluidStack.EMPTY : new FluidStack(fluid, amount);
    }

    @Override
    public int getTankCapacity(int tank) {
        return SeasoningBottleItem.CAPACITY_MB;
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack resource) {
        Fluid fluid = fluidOf(this.stack.getItem());
        return fluid != null && resource.getFluid().isSame(fluid);
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (!isFluidValid(0, resource)) {
            return 0;
        }
        int room = SeasoningBottleItem.usedMb(this.stack);
        int moved = Math.min(room, resource.getAmount());
        if (moved > 0 && action.execute()) {
            this.stack.setDamageValue(this.stack.getDamageValue() - moved);
        }
        return moved;
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        Fluid fluid = fluidOf(this.stack.getItem());
        if (fluid == null || !resource.getFluid().isSame(fluid)) {
            return FluidStack.EMPTY;
        }
        return drain(resource.getAmount(), action);
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        Fluid fluid = fluidOf(this.stack.getItem());
        int have = SeasoningBottleItem.remainingMb(this.stack);
        if (fluid == null || have <= 0 || maxDrain <= 0) {
            return FluidStack.EMPTY;
        }
        int moved = Math.min(have, maxDrain);
        if (action.execute()) {
            int used = SeasoningBottleItem.usedMb(this.stack) + moved;
            if (used >= SeasoningBottleItem.CAPACITY_MB) {
                this.stack = new ItemStack(Items.GLASS_BOTTLE);
            } else {
                this.stack.setDamageValue(used);
            }
        }
        return new FluidStack(fluid, moved);
    }
}
