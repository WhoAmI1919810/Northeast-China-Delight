package com.gunmu.northeast_china_delight.event;

import com.gunmu.northeast_china_delight.advancement.ModAdvancements;
import com.gunmu.northeast_china_delight.block.ModBlocks;
import com.gunmu.northeast_china_delight.block.SleepingPillowBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

/**
 * 寒冷群系里挨着暖气片睡觉时的起床奖励。
 *
 * <p>{@link #hasReachableRadiator} 不要求直线可见，而是在暖气片和睡眠位置之间做三维连通检查：
 * 中间格必须没有碰撞体积，起点暖气片和终点床/枕头本身不参与可通行判断。</p>
 */
public final class SleepComfort
{
    public static final float MAX_COLD_TEMPERATURE = 0.2F;
    public static final int BED_REGENERATION_TICKS = 6 * 20;
    public static final int PILLOW_REGENERATION_TICKS = 12 * 20;

    private static final int RADIATOR_SCAN_RADIUS = ModGameplayEvents.RADIATOR_SCAN_RADIUS;
    private static final int MAX_PATH_NODES = 4096;
    private static final double RADIATOR_RANGE_SQR =
            ModGameplayEvents.RADIATOR_RANGE * ModGameplayEvents.RADIATOR_RANGE;

    private SleepComfort()
    {
    }

    public static void applyAfterWakeUp(ServerPlayer player)
    {
        if (!player.isSleeping())
        {
            return;
        }
        BlockPos sleepPos = player.getSleepingPos().orElse(null);
        if (sleepPos == null)
        {
            return;
        }

        ServerLevel level = player.serverLevel();
        BlockPos playerPos = player.blockPosition();
        if (!isColdEnough(level, playerPos) || !hasReachableRadiator(level, playerPos))
        {
            return;
        }

        BlockState sleepingState = level.getBlockState(sleepPos);
        boolean pillow = sleepingState.getBlock() instanceof SleepingPillowBlock;
        if (!pillow && !sleepingState.isBed(level, sleepPos, player))
        {
            return;
        }

        int duration = pillow ? PILLOW_REGENERATION_TICKS : BED_REGENERATION_TICKS;
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, duration, 0));
        if (pillow)
        {
            ModAdvancements.award(player, ModAdvancements.COZY_SLEEP, "sleep");
        }
    }

    public static boolean isColdEnough(Level level, BlockPos pos)
    {
        return level.getBiome(pos).value().getBaseTemperature() <= MAX_COLD_TEMPERATURE;
    }

    public static boolean hasReachableRadiator(Level level, BlockPos playerPos)
    {
        Vec3 playerCenter = Vec3.atCenterOf(playerPos);
        BlockPos min = playerPos.offset(-RADIATOR_SCAN_RADIUS, -RADIATOR_SCAN_RADIUS, -RADIATOR_SCAN_RADIUS);
        BlockPos max = playerPos.offset(RADIATOR_SCAN_RADIUS, RADIATOR_SCAN_RADIUS, RADIATOR_SCAN_RADIUS);
        for (BlockPos cursor : BlockPos.betweenClosed(min, max))
        {
            if (!level.getBlockState(cursor).is(ModBlocks.RADIATOR.get()))
            {
                continue;
            }
            if (Vec3.atCenterOf(cursor).distanceToSqr(playerCenter) > RADIATOR_RANGE_SQR)
            {
                continue;
            }
            BlockPos radiator = cursor.immutable();
            if (hasOpenPath(level, radiator, playerPos))
            {
                return true;
            }
        }
        return false;
    }

    public static boolean hasOpenPath(BlockGetter level, BlockPos from, BlockPos to)
    {
        if (from.equals(to))
        {
            return true;
        }

        Set<Long> visited = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        visited.add(from.asLong());
        queue.addLast(from);
        int examined = 0;
        while (!queue.isEmpty() && examined++ < MAX_PATH_NODES)
        {
            BlockPos current = queue.removeFirst();
            for (Direction direction : Direction.values())
            {
                BlockPos next = current.relative(direction);
                if (next.equals(to))
                {
                    return true;
                }
                if (!isPassable(level, next) || !visited.add(next.asLong()))
                {
                    continue;
                }
                queue.addLast(next);
            }
        }
        return false;
    }

    private static boolean isPassable(BlockGetter level, BlockPos pos)
    {
        return level.getBlockState(pos)
                .getCollisionShape(level, pos, CollisionContext.empty())
                .isEmpty();
    }
}
