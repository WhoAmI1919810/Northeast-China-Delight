package com.gunmu.northeast_china_delight.block;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 脸盆架的方块实体。
 *
 * <p>治疗冷却存在玩家的持久数据里，由所有脸盆架共享，不在这里。</p>
 *
 * <p>这里记架上这盆水被洗过几次脸（2026-11-07 用户：「给脸盆架加一个字段，记录当上面有水盆时，被玩家右键使用过几次。
 * 当字段=2时，如果玩家再使用，正常使用一次，并将上面的水盆变成空脸盆」），见 {@link BasinStandBlock}。
 * 架上的盆一不是水盆（拿走、舀干、换成空盆），次数就清零：新倒满的一盆水从 0 数起，不管是谁倒的（压水井也一样）。</p>
 */
public class BasinStandBlockEntity extends BlockEntity {

    /** 用到第几次把水用完：已经用过这么多次以后再洗一次，水盆就变空盆 */
    public static final int USES_BEFORE_EMPTY = 2;

    private int uses;

    public BasinStandBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BASIN_STAND.get(), pos, state);
    }

    /** 这盆水已经洗过几次 */
    public int uses() {
        return this.uses;
    }

    public void setUses(int uses) {
        this.uses = uses;
        setChanged();
    }

    /** 方块状态变了（盆放上、拿走、倒水、舀水都会走这里）：架上不是水盆就清零 */
    @Override
    public void setBlockState(BlockState state) {
        super.setBlockState(state);
        if (this.uses != 0 && state.hasProperty(BasinStandBlock.BASIN)
                && state.getValue(BasinStandBlock.BASIN) != BasinStandBlock.Basin.WATER) {
            this.uses = 0;
            setChanged();
        }
    }

    @Override
    //? if <1.20.5 {
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("WashUses", this.uses);
    }
    //?} else {
    /*protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("WashUses", this.uses);
    }
    *///?}

    @Override
    //? if <1.20.5 {
    public void load(CompoundTag tag) {
        super.load(tag);
        this.uses = tag.getInt("WashUses");
    }
    //?} else {
    /*protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.uses = tag.getInt("WashUses");
    }
    *///?}
}
