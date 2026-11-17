package com.gunmu.northeast_china_delight.block;

import com.gunmu.northeast_china_delight.entity.QuiltSleepData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * 可睡的枕头：枕头必须垫在羊毛上，沿枕头朝向的两端至少有一格空气；
 * 选定的一侧前方下方也要是羊毛，附近还要有被子方块。
 *
 * <p>原版睡眠入口在 {@code ServerPlayer#startSleepInBed}，这里只负责补齐床的接口和枕头自己的条件。
 * 原版摄像机看到的是床朝向的反面，所以 {@link #getBedDirection} 返回选定的 D 的反向，镜头实际朝向 D。</p>
 */
public class SleepingPillowBlock extends FacingDecorBlock
{
    private static final int QUILT_RADIUS_XZ = 4;
    private static final int QUILT_RADIUS_Y = 2;

    /** 枕头缺少的睡眠条件；诊断顺序为被子、空间、羊毛。 */
    public enum MissingRequirement
    {
        QUILT("message.northeast_china_delight.pillow.need_quilt"),
        SPACE("message.northeast_china_delight.pillow.need_space"),
        WOOL("message.northeast_china_delight.pillow.need_wool");

        private final String translationKey;

        MissingRequirement(String translationKey)
        {
            this.translationKey = translationKey;
        }

        public Component message()
        {
            return Component.translatable(this.translationKey);
        }
    }

    public SleepingPillowBlock(Properties properties)
    {
        super(properties, Block.box(2, 0, 2, 14, 4.5, 14), false);
    }

    private static InteractionResult trySleep(BlockState state, Level level, BlockPos pos, Player player)
    {
        if (level.isClientSide)
        {
            return InteractionResult.CONSUME;
        }
        if (!(player instanceof ServerPlayer serverPlayer))
        {
            return InteractionResult.PASS;
        }

        QuiltSleepData quiltData = (QuiltSleepData) serverPlayer;
        quiltData.ncd$setQuiltPos(null);

        Player.BedSleepingProblem basicProblem = basicSleepProblem(serverPlayer, state, pos);
        if (basicProblem != null)
        {
            showProblem(serverPlayer, basicProblem);
            return InteractionResult.SUCCESS;
        }

        MissingRequirement missingRequirement = missingRequirement(level, state, pos);
        if (missingRequirement != null)
        {
            showMissingRequirement(serverPlayer, missingRequirement);
            return InteractionResult.SUCCESS;
        }

        Player.BedSleepingProblem environmentProblem = environmentSleepProblem(serverPlayer, state, pos);
        if (environmentProblem != null)
        {
            showProblem(serverPlayer, environmentProblem);
            return InteractionResult.SUCCESS;
        }

        Optional<BlockPos> quiltPos = findNearbyQuilt(level, pos);
        if (quiltPos.isEmpty())
        {
            showMissingRequirement(serverPlayer, MissingRequirement.QUILT);
            return InteractionResult.SUCCESS;
        }

        if (!FoldedQuiltBlock.select((ServerLevel) level, quiltPos.get(), serverPlayer.getUUID()))
        {
            showMissingRequirement(serverPlayer, MissingRequirement.QUILT);
            return InteractionResult.SUCCESS;
        }

        var result = serverPlayer.startSleepInBed(pos);
        if (result.left().isPresent())
        {
            showProblem(serverPlayer, result.left().get());
            FoldedQuiltBlock.release((ServerLevel) level, quiltPos.get(), serverPlayer.getUUID());
            quiltData.ncd$setQuiltPos(null);
            return InteractionResult.SUCCESS;
        }
        quiltData.ncd$setQuiltPos(quiltPos.get());
        return InteractionResult.SUCCESS;
    }

    /**
     * 返回本次右键选中的 D：先看枕头朝向的一侧，再看反向。候选方向必须同时满足
     * 该侧是空气且前下方是羊毛；两侧都不满足时返回 null。
     */
    @Nullable
    public static Direction sleepSide(BlockState state, BlockGetter level, BlockPos pos)
    {
        Direction facing = state.getValue(FACING);
        if (isUsableSleepSide(level, pos, facing))
        {
            return facing;
        }
        Direction opposite = facing.getOpposite();
        return isUsableSleepSide(level, pos, opposite) ? opposite : null;
    }

    private static boolean isUsableSleepSide(BlockGetter level, BlockPos pos, Direction side)
    {
        BlockPos sidePos = pos.relative(side);
        return level.getBlockState(sidePos).isAir()
                && level.getBlockState(sidePos.below()).is(BlockTags.WOOL);
    }

    @Nullable
    private static Direction firstAirSide(BlockGetter level, BlockPos pos, Direction facing)
    {
        if (level.getBlockState(pos.relative(facing)).isAir())
        {
            return facing;
        }
        Direction opposite = facing.getOpposite();
        return level.getBlockState(pos.relative(opposite)).isAir() ? opposite : null;
    }

    /** 以枕头为中心，x/z 各 ±4、y 各 ±2 的闭区间内是否有被子方块。 */
    public static boolean hasNearbyQuilt(Level level, BlockPos pos)
    {
        return findNearbyQuilt(level, pos).isPresent();
    }

    /**
     * 在枕头周围选一块被子；多块时取距离最近的一块，距离相同则保留遍历到的第一块。
     */
    public static Optional<BlockPos> findNearbyQuilt(Level level, BlockPos pos)
    {
        BlockPos min = pos.offset(-QUILT_RADIUS_XZ, -QUILT_RADIUS_Y, -QUILT_RADIUS_XZ);
        BlockPos max = pos.offset(QUILT_RADIUS_XZ, QUILT_RADIUS_Y, QUILT_RADIUS_XZ);
        BlockPos nearest = null;
        long nearestDistance = Long.MAX_VALUE;
        for (BlockPos cursor : BlockPos.betweenClosed(min, max))
        {
            BlockState cursorState = level.getBlockState(cursor);
            if (cursorState.is(ModBlockTags.QUILTS) && !FoldedQuiltBlock.isOccupied(cursorState))
            {
                long dx = (long) cursor.getX() - pos.getX();
                long dy = (long) cursor.getY() - pos.getY();
                long dz = (long) cursor.getZ() - pos.getZ();
                long distance = dx * dx + dy * dy + dz * dz;
                if (distance < nearestDistance)
                {
                    nearest = cursor.immutable();
                    nearestDistance = distance;
                }
            }
        }
        return Optional.ofNullable(nearest);
    }

    @Nullable
    public static MissingRequirement missingRequirement(Level level, BlockState state, BlockPos pos)
    {
        if (findNearbyQuilt(level, pos).isEmpty())
        {
            return MissingRequirement.QUILT;
        }

        Direction facing = state.getValue(FACING);
        Direction chosenSide = sleepSide(state, level, pos);
        Direction headspaceSide = chosenSide != null ? chosenSide : firstAirSide(level, pos, facing);
        if (headspaceSide == null || isHeadspaceBlocked(level, pos, headspaceSide))
        {
            return MissingRequirement.SPACE;
        }

        boolean woolUnderPillow = level.getBlockState(pos.below()).is(BlockTags.WOOL);
        boolean woolUnderSide = level.getBlockState(pos.relative(facing).below()).is(BlockTags.WOOL)
                || level.getBlockState(pos.relative(facing.getOpposite()).below()).is(BlockTags.WOOL);
        if (!woolUnderPillow || !woolUnderSide || chosenSide == null)
        {
            return MissingRequirement.WOOL;
        }
        return null;
    }

    /** 被子坐标是否仍在枕头对应的搜索范围内。 */
    public static boolean isQuiltInRange(BlockPos pillowPos, BlockPos quiltPos)
    {
        return Math.abs(quiltPos.getX() - pillowPos.getX()) <= QUILT_RADIUS_XZ
                && Math.abs(quiltPos.getY() - pillowPos.getY()) <= QUILT_RADIUS_Y
                && Math.abs(quiltPos.getZ() - pillowPos.getZ()) <= QUILT_RADIUS_XZ;
    }

    /**
     * 原版睡眠入口会检查床尾上方是否可站立；枕头只有一格，按 D 方向检查上方空间。
     */
    public static boolean isHeadspaceBlocked(BlockGetter level, BlockPos pos, @Nullable Direction sleepSide)
    {
        if (sleepSide == null)
        {
            return true;
        }
        return isSuffocating(level, pos.above()) || isSuffocating(level, pos.relative(sleepSide).above());
    }

    private static boolean isSuffocating(BlockGetter level, BlockPos pos)
    {
        return level.getBlockState(pos).isSuffocating(level, pos);
    }

    /** 玩家状态、维度和距离等基础限制。 */
    @Nullable
    private static Player.BedSleepingProblem basicSleepProblem(ServerPlayer player, BlockState state, BlockPos pos)
    {
        ServerLevel level = player.serverLevel();
        Direction facing = state.getValue(FACING);
        if (player.isSleeping() || !player.isAlive())
        {
            return Player.BedSleepingProblem.OTHER_PROBLEM;
        }
        if (!level.dimensionType().natural() || !BedBlock.canSetSpawn(level))
        {
            return Player.BedSleepingProblem.NOT_POSSIBLE_HERE;
        }
        if (!bedInRange(player, pos, facing))
        {
            return Player.BedSleepingProblem.TOO_FAR_AWAY;
        }
        return null;
    }

    /** 睡觉时间、附近怪物与头部空间等原版环境限制。 */
    @Nullable
    private static Player.BedSleepingProblem environmentSleepProblem(ServerPlayer player, BlockState state, BlockPos pos)
    {
        ServerLevel level = player.serverLevel();
        Direction facing = state.getValue(FACING);
        Direction chosen = sleepSide(state, level, pos);
        if (isHeadspaceBlocked(level, pos, chosen == null ? facing : chosen))
        {
            return Player.BedSleepingProblem.OBSTRUCTED;
        }
        if (level.isDay())
        {
            return Player.BedSleepingProblem.NOT_POSSIBLE_NOW;
        }
        if (!player.isCreative())
        {
            Vec3 center = Vec3.atBottomCenterOf(pos);
            AABB area = new AABB(center.x - 8.0, center.y - 5.0, center.z - 8.0,
                    center.x + 8.0, center.y + 5.0, center.z + 8.0);
            if (!level.getEntitiesOfClass(Monster.class, area, monster -> monster.isPreventingPlayerRest(player)).isEmpty())
            {
                return Player.BedSleepingProblem.NOT_SAFE;
            }
        }
        return null;
    }

    private static boolean bedInRange(ServerPlayer player, BlockPos pos, Direction direction)
    {
        return isReachable(player, pos) || isReachable(player, pos.relative(direction.getOpposite()));
    }

    private static boolean isReachable(ServerPlayer player, BlockPos pos)
    {
        Vec3 center = Vec3.atBottomCenterOf(pos);
        return Math.abs(player.getX() - center.x) <= 3.0
                && Math.abs(player.getY() - center.y) <= 2.0
                && Math.abs(player.getZ() - center.z) <= 3.0;
    }

    private static void showProblem(ServerPlayer player, Player.BedSleepingProblem problem)
    {
        Component message = problem.getMessage();
        if (message != null && player.connection != null)
        {
            player.displayClientMessage(message, true);
        }
    }

    private static void showMissingRequirement(ServerPlayer player, MissingRequirement requirement)
    {
        if (player.connection != null)
        {
            player.displayClientMessage(requirement.message(), true);
        }
    }

    //? if <1.20.5 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 net.minecraft.world.InteractionHand hand, BlockHitResult hitResult)
    {
        return trySleep(state, level, pos, player);
    }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hitResult)
    {
        return trySleep(state, level, pos, player);
    }
    *///?}

    //? if <1.20.2 {
    @Override
    public boolean isBed(BlockState state, BlockGetter level, BlockPos pos, @Nullable Entity sleeper)
    {
        return true;
    }
    //?} else {
    /*@Override
    public boolean isBed(BlockState state, BlockGetter level, BlockPos pos, LivingEntity sleeper)
    {
        return true;
    }
    *///?}

    @Override
    public void setBedOccupied(BlockState state, Level level, BlockPos pos, LivingEntity sleeper, boolean occupied)
    {
        // 枕头没有 BedBlock.OCCUPIED 状态，不能走原版的属性写入。
    }

    @Override
    public Direction getBedDirection(BlockState state, LevelReader level, BlockPos pos)
    {
        Direction chosen = sleepSide(state, level, pos);
        return (chosen == null ? state.getValue(FACING) : chosen).getOpposite();
    }

    //? if >=1.20.2 {
    /*@Override
    public Optional<ServerPlayer.RespawnPosAngle> getRespawnPosition(BlockState state, EntityType<?> type,
                                                                      LevelReader levelReader, BlockPos pos,
                                                                      float orientation)
    {
        if (!(levelReader instanceof Level level) || !BedBlock.canSetSpawn(level))
        {
            return Optional.empty();
        }
        return BedBlock.findStandUpPosition(type, level, pos, state.getValue(FACING), orientation)
                .map(standPosition -> ServerPlayer.RespawnPosAngle.of(standPosition, pos));
    }
    *///?}

}
