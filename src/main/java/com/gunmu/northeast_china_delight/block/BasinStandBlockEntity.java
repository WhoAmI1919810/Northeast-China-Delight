package com.gunmu.northeast_china_delight.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 脸盆架的占位方块实体。
 *
 * <p>治疗冷却已经改存到玩家的持久数据中，由所有脸盆架共享；这个类型继续保留，
 * 只用于兼容已经放置的脸盆架和旧存档，不再保存独立的冷却字段。</p>
 */
public class BasinStandBlockEntity extends BlockEntity {

    public BasinStandBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BASIN_STAND.get(), pos, state);
    }
}