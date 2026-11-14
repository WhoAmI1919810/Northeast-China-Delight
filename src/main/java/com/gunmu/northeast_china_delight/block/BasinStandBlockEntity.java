package com.gunmu.northeast_china_delight.block;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 脸盆架方块实体：记录当前水盆可继续使用的次数，见 {@link BasinStandBlock}。
 *
 * <p>治疗冷却保存在玩家持久数据中，由所有脸盆架共享。架上不再是水盆时使用次数清零；
 * 重新装满的一盆水从 0 开始计数，与倒水来源无关。</p>
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
    /*protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("WashUses", this.uses);
    }*/
    //?} else {
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("WashUses", this.uses);
    }
    //?}

    @Override
    //? if <1.20.5 {
    /*public void load(CompoundTag tag) {
        super.load(tag);
        this.uses = tag.getInt("WashUses");
    }*/
    //?} else {
    protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.uses = tag.getInt("WashUses");
    }
    //?}
}
