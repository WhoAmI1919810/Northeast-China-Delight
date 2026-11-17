package com.gunmu.northeast_china_delight.entity;

import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * 玩家用枕头入睡时选中的被子方块坐标。
 */
public interface QuiltSleepData
{
    Optional<BlockPos> ncd$getQuiltPos();

    void ncd$setQuiltPos(@Nullable BlockPos pos);
}
