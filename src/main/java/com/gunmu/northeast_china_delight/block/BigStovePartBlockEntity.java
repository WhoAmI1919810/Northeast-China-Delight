package com.gunmu.northeast_china_delight.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 大灶除西北那格以外三格上的方块实体，不存任何数据，只为在 1.20.1（Forge）上挂物品能力：
 * Forge 的能力只能挂在方块实体上，机械动力 1.20.1 的溜槽也是先找下面那格的方块实体再要能力。
 * 1.20.2 起能力按方块注册，这三格不放方块实体（{@link BigStoveBlock#newBlockEntity}）。
 */
public class BigStovePartBlockEntity extends BlockEntity {

    //? if <1.20.2 {
    private net.minecraftforge.common.util.LazyOptional<net.minecraftforge.items.IItemHandler> itemHandler =
            net.minecraftforge.common.util.LazyOptional.empty();
    //?}

    public BigStovePartBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BIG_STOVE_PART.get(), pos, state);
    }

    //? if <1.20.2 {
    @Override
    public <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(
            net.minecraftforge.common.capabilities.Capability<T> capability,
            @org.jetbrains.annotations.Nullable net.minecraft.core.Direction side) {
        if (capability == net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER
                && (side == null || side == net.minecraft.core.Direction.UP) && this.level != null) {
            if (!this.itemHandler.isPresent()) {
                this.itemHandler = net.minecraftforge.common.util.LazyOptional.of(
                        () -> new BigPotItemHandler(this.level, this.worldPosition));
            }
            return this.itemHandler.cast();
        }
        return super.getCapability(capability, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        this.itemHandler.invalidate();
    }
    //?}
}
