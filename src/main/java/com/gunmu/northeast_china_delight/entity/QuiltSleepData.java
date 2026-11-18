package com.gunmu.northeast_china_delight.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * 玩家用枕头入睡时选中的睡眠方向和被子方块坐标。
 */
public interface QuiltSleepData
{
    Optional<BlockPos> ncd$getQuiltPos();

    void ncd$setQuiltPos(@Nullable BlockPos pos);

    Optional<Direction> ncd$getSleepSide();

    void ncd$setSleepSide(@Nullable Direction side);
}
