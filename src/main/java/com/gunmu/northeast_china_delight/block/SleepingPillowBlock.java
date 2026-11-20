package com.gunmu.northeast_china_delight.block;

import com.gunmu.chinese_regional_culinary_ingredients.block.FacingDecorBlock;

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
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
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
    public static final BooleanProperty OCCUPIED = BlockStateProperties.OCCUPIED;
    public static final BooleanProperty REVERSED = BooleanProperty.create("reversed");

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

    /**
     * 一次完整的枕头睡眠条件评估。睡眠尝试和失败提示都使用这一份结果，避免两套判定逐渐分叉。
     */
    public record SleepEvaluation(@Nullable Direction side, @Nullable BlockPos quiltPos,
                                  @Nullable MissingRequirement missingRequirement)
    {
        public boolean canSleep()
        {
            return this.missingRequirement == null;
        }
    }

    private record SideEvaluation(@Nullable Direction side, @Nullable MissingRequirement missingRequirement)
    {
    }

    public SleepingPillowBlock(Properties properties)
    {
        super(properties, Block.box(2, 0, 2, 14, 4.5, 14), false);
        this.registerDefaultState(this.defaultBlockState()
                .setValue(OCCUPIED, false)
                .setValue(REVERSED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        super.createBlockStateDefinition(builder);
        builder.add(OCCUPIED, REVERSED);
    }

    private static boolean hasSleepingOccupant(ServerLevel level, BlockPos pos)
    {
        AABB area = new AABB(pos).inflate(2.0);
        return !level.getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity.isSleeping() && pos.equals(entity.getSleepingPos().orElse(null))).isEmpty();
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
        if (!serverPlayer.isSleeping())
        {
            quiltData.ncd$setQuiltPos(null);
            quiltData.ncd$setSleepSide(null);
        }

        Player.BedSleepingProblem basicProblem = basicSleepProblem(serverPlayer, state, pos);
        if (basicProblem != null)
        {
            showProblem(serverPlayer, basicProblem);
            return InteractionResult.SUCCESS;
        }

        ServerLevel serverLevel = (ServerLevel) level;
        if (state.getValue(OCCUPIED) || hasSleepingOccupant(serverLevel, pos))
        {
            if (hasSleepingOccupant(serverLevel, pos))
            {
                if (serverPlayer.connection != null)
                {
                    serverPlayer.displayClientMessage(Component.translatable("block.minecraft.bed.occupied"), true);
                }
                return InteractionResult.SUCCESS;
            }
            state = state.setValue(OCCUPIED, false).setValue(REVERSED, false);
            level.setBlock(pos, state, Block.UPDATE_ALL);
        }

        SleepEvaluation evaluation = evaluateSleep(level, state, pos);
        if (!evaluation.canSleep())
        {
            showMissingRequirement(serverPlayer, evaluation.missingRequirement());
            return InteractionResult.SUCCESS;
        }

        Player.BedSleepingProblem environmentProblem = environmentSleepProblem(serverPlayer, pos);
        if (environmentProblem != null)
        {
            showProblem(serverPlayer, environmentProblem);
            return InteractionResult.SUCCESS;
        }

        BlockPos quiltPos = evaluation.quiltPos();
        if (!FoldedQuiltBlock.select(serverLevel, quiltPos, serverPlayer.getUUID()))
        {
            showMissingRequirement(serverPlayer, MissingRequirement.QUILT);
            return InteractionResult.SUCCESS;
        }

        boolean reversed = evaluation.side() == state.getValue(FACING).getOpposite();
        BlockState prepared = state.setValue(REVERSED, reversed);
        if (prepared != state)
        {
            level.setBlock(pos, prepared, Block.UPDATE_ALL);
        }
        var result = serverPlayer.startSleepInBed(pos);
        if (result.left().isPresent())
        {
            if (prepared != state)
            {
                level.setBlock(pos, state, Block.UPDATE_ALL);
            }
            showProblem(serverPlayer, result.left().get());
            FoldedQuiltBlock.release(serverLevel, quiltPos, serverPlayer.getUUID());
            quiltData.ncd$setQuiltPos(null);
            quiltData.ncd$setSleepSide(null);
            return InteractionResult.SUCCESS;
        }
        quiltData.ncd$setQuiltPos(quiltPos);
        quiltData.ncd$setSleepSide(evaluation.side());
        return InteractionResult.SUCCESS;
    }

    /**
     * 返回本次右键选中的 D：先看枕头朝向的一侧，再看反向。候选方向必须同时满足
     * 该侧是空气、头部空间未被阻挡且前下方是羊毛；两侧都不满足时返回 null。
     */
    @Nullable
    public static Direction sleepSide(BlockState state, BlockGetter level, BlockPos pos)
    {
        return evaluateSleepSide(level, state, pos).side();
    }

    private static SideEvaluation evaluateSleepSide(BlockGetter level, BlockState state, BlockPos pos)
    {
        if (!level.getBlockState(pos.below()).is(BlockTags.WOOL))
        {
            return new SideEvaluation(null, MissingRequirement.WOOL);
        }

        Direction facing = state.getValue(FACING);
        MissingRequirement facingProblem = sideProblem(level, pos, facing);
        if (facingProblem == null)
        {
            return new SideEvaluation(facing, null);
        }

        Direction opposite = facing.getOpposite();
        MissingRequirement oppositeProblem = sideProblem(level, pos, opposite);
        if (oppositeProblem == null)
        {
            return new SideEvaluation(opposite, null);
        }

        MissingRequirement reason = facingProblem == MissingRequirement.SPACE
                || oppositeProblem == MissingRequirement.SPACE
                ? MissingRequirement.SPACE
                : MissingRequirement.WOOL;
        return new SideEvaluation(null, reason);
    }

    @Nullable
    private static MissingRequirement sideProblem(BlockGetter level, BlockPos pos, Direction side)
    {
        BlockPos sidePos = pos.relative(side);
        if (!level.getBlockState(sidePos).isAir() || isHeadspaceBlocked(level, pos, side))
        {
            return MissingRequirement.SPACE;
        }
        return level.getBlockState(sidePos.below()).is(BlockTags.WOOL) ? null : MissingRequirement.WOOL;
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

    /**
     * 枕头本体、睡眠侧空间和羊毛先于被子评估；空间问题优先于羊毛问题，避免逐项补查形成两套规则。
     */
    public static SleepEvaluation evaluateSleep(Level level, BlockState state, BlockPos pos)
    {
        SideEvaluation side = evaluateSleepSide(level, state, pos);
        if (side.side() == null)
        {
            return new SleepEvaluation(null, null, side.missingRequirement());
        }

        Optional<BlockPos> quiltPos = findNearbyQuilt(level, pos);
        if (quiltPos.isEmpty())
        {
            return new SleepEvaluation(null, null, MissingRequirement.QUILT);
        }
        return new SleepEvaluation(side.side(), quiltPos.get(), null);
    }

    /** 被子坐标是否仍在枕头对应的搜索范围内。 */
    public static boolean isQuiltInRange(BlockPos pillowPos, BlockPos quiltPos)
    {
        return Math.abs(quiltPos.getX() - pillowPos.getX()) <= QUILT_RADIUS_XZ
                && Math.abs(quiltPos.getY() - pillowPos.getY()) <= QUILT_RADIUS_Y
                && Math.abs(quiltPos.getZ() - pillowPos.getZ()) <= QUILT_RADIUS_XZ;
    }

    /**
     * 睡眠期间重新核对入睡时选定的完整布局：枕头、方向 D 的空位与头部空间、
     * 枕头和 D 前下方的羊毛，以及本人占用中的那床被子。
     */
    public static boolean isSleepingLayoutValid(ServerPlayer player)
    {
        if (!player.isSleeping())
        {
            return false;
        }
        BlockPos pillowPos = player.getSleepingPos().orElse(null);
        if (pillowPos == null)
        {
            return false;
        }

        ServerLevel level = player.serverLevel();
        BlockState pillowState = level.getBlockState(pillowPos);
        if (!(pillowState.getBlock() instanceof SleepingPillowBlock))
        {
            return false;
        }

        QuiltSleepData quiltData = (QuiltSleepData) player;
        Direction side = quiltData.ncd$getSleepSide().orElse(null);
        Direction facing = pillowState.getValue(FACING);
        if (side == null || (side != facing && side != facing.getOpposite()))
        {
            return false;
        }
        if (!level.getBlockState(pillowPos.below()).is(BlockTags.WOOL) || sideProblem(level, pillowPos, side) != null)
        {
            return false;
        }

        BlockPos quiltPos = quiltData.ncd$getQuiltPos().orElse(null);
        if (quiltPos == null || !isQuiltInRange(pillowPos, quiltPos))
        {
            return false;
        }
        BlockState quiltState = level.getBlockState(quiltPos);
        return quiltState.is(ModBlockTags.QUILTS)
                && FoldedQuiltBlock.isOccupied(quiltState)
                && level.getBlockEntity(quiltPos) instanceof FoldedQuiltBlockEntity quilt
                && quilt.isSelectedBy(player.getUUID());
    }

    /**
     * 枕头睡眠期间定期校验整套布局；被子被指令替换、羊毛被破坏或空位被堵住时立即起床。
     */
    public static void validateSleepingLayout(ServerPlayer player)
    {
        if (!player.isSleeping())
        {
            return;
        }

        QuiltSleepData quiltData = (QuiltSleepData) player;
        BlockPos pillowPos = player.getSleepingPos().orElse(null);
        boolean pillow = pillowPos != null
                && player.serverLevel().getBlockState(pillowPos).getBlock() instanceof SleepingPillowBlock;
        boolean hadPillowSelection = quiltData.ncd$getSleepSide().isPresent();
        if (!pillow && !hadPillowSelection)
        {
            return;
        }
        if (pillow && isSleepingLayoutValid(player))
        {
            return;
        }

        quiltData.ncd$getQuiltPos().ifPresent(quiltPos ->
                FoldedQuiltBlock.release(player.serverLevel(), quiltPos, player.getUUID()));
        quiltData.ncd$setQuiltPos(null);
        quiltData.ncd$setSleepSide(null);
        player.stopSleepInBed(true, true);
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

    /** 睡觉时间与附近怪物等原版环境限制。 */
    @Nullable
    private static Player.BedSleepingProblem environmentSleepProblem(ServerPlayer player, BlockPos pos)
    {
        ServerLevel level = player.serverLevel();
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
    /*@Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 net.minecraft.world.InteractionHand hand, BlockHitResult hitResult)
    {
        return trySleep(state, level, pos, player);
    }
    *///?} else {
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hitResult)
    {
        return trySleep(state, level, pos, player);
    }
    //?}

    //? if <1.20.2 {
    /*@Override
    public boolean isBed(BlockState state, BlockGetter level, BlockPos pos, @Nullable Entity sleeper)
    {
        return true;
    }
    *///?} else {
    @Override
    public boolean isBed(BlockState state, BlockGetter level, BlockPos pos, LivingEntity sleeper)
    {
        return true;
    }
    //?}

    @Override
    public void setBedOccupied(BlockState state, Level level, BlockPos pos, LivingEntity sleeper, boolean occupied)
    {
        BlockState next = occupied
                ? state.setValue(OCCUPIED, true)
                : state.setValue(OCCUPIED, false).setValue(REVERSED, false);
        if (next != state)
        {
            level.setBlock(pos, next, Block.UPDATE_ALL);
        }
    }

    @Override
    public Direction getBedDirection(BlockState state, LevelReader level, BlockPos pos)
    {
        if (state.getValue(OCCUPIED) || state.getValue(REVERSED))
        {
            Direction facing = state.getValue(FACING);
            return state.getValue(REVERSED) ? facing : facing.getOpposite();
        }
        Direction chosen = sleepSide(state, level, pos);
        return (chosen == null ? state.getValue(FACING) : chosen).getOpposite();
    }

    //? if <1.20.2 {
    /*@Override
    public Optional<Vec3> getRespawnPosition(BlockState state, EntityType<?> type,
                                             LevelReader levelReader, BlockPos pos,
                                             float orientation, @Nullable LivingEntity entity)
    {
        if (!(levelReader instanceof Level level) || !BedBlock.canSetSpawn(level))
        {
            return Optional.empty();
        }
        return BedBlock.findStandUpPosition(type, level, pos, state.getValue(FACING), orientation);
    }
    *///?} else {
    @Override
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
    //?}

}
