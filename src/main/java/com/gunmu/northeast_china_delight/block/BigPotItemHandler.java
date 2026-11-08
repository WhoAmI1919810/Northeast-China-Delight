package com.gunmu.northeast_china_delight.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
//? if <1.20.2 {
import net.minecraftforge.items.IItemHandler;
//?} else {
/*import net.neoforged.neoforge.items.IItemHandler;
*///?}

/**
 * 大灶从上面送料的物品能力（2026-11-08 用户报：机械动力的溜槽没法把物品投入大铁锅）。
 *
 * <p>原版漏斗认的是 {@link BigStoveBlock#getContainer}（WorldlyContainerHolder），可机械动力的溜槽、漏斗口只认加载器的物品能力，
 * NeoForge 也只替原版堆肥桶做了转换，所以以前溜槽眼里大灶是块实心方块。这里把同一套规矩包成物品能力：
 * 一次收一个，收什么见 {@link BigPotCooking#dropIn}（同掉进锅里、漏斗往下送）；水桶、水盆倒完的空桶、空盆从锅口蹦出来；
 * 不往外给东西。只认从上面来（side 为 UP 或者不分面），四格哪一格都行。</p>
 *
 * <p>1.21.1 按方块注册（NortheastChinaDelight#registerCapabilities）；1.20.1 的能力只能挂在方块实体上，
 * 西北那格挂在 {@link BigPotBlockEntity}，另外三格挂在 {@link BigStovePartBlockEntity}。</p>
 */
public final class BigPotItemHandler implements IItemHandler {

    private final Level level;
    private final BlockPos pos;

    public BigPotItemHandler(Level level, BlockPos pos) {
        this.level = level;
        this.pos = pos;
    }

    @Override
    public int getSlots() {
        return 1;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (stack.isEmpty() || this.level.isClientSide) {
            return stack;
        }
        BlockState state = this.level.getBlockState(this.pos);
        if (!(state.getBlock() instanceof BigStoveBlock)) {
            return stack;
        }
        BigPotBlockEntity pot = BigStoveBlock.potAt(this.level, this.pos, state);
        if (pot == null) {
            return stack;
        }
        ItemStack back = BigPotCooking.dropIn(pot, state, this.level, this.pos, stack, null, simulate);
        if (back == null) {
            return stack;
        }
        if (!simulate && !back.isEmpty()) {
            Vec3 mouth = BigPotCooking.center(this.pos, state);
            BigPotCooking.popOut(this.level, mouth.x, mouth.y, mouth.z, back);
        }
        return stack.copyWithCount(stack.getCount() - 1);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return ItemStack.EMPTY;
    }

    @Override
    public int getSlotLimit(int slot) {
        return 64;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return insertItem(slot, stack, true).getCount() < stack.getCount();
    }
}
