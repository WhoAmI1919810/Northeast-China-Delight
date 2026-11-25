package com.gunmu.northeast_china_delight.gametest;

import com.gunmu.northeast_china_delight.NortheastChinaDelight;
import com.gunmu.northeast_china_delight.block.FacingDecorBlock;
import com.gunmu.northeast_china_delight.block.FoldedQuiltBlock;
import com.gunmu.northeast_china_delight.block.FoldedQuiltBlockEntity;
import com.gunmu.northeast_china_delight.block.ModBlocks;
import com.gunmu.northeast_china_delight.block.SleepingPillowBlock;
import com.gunmu.northeast_china_delight.entity.QuiltSleepData;
import com.gunmu.northeast_china_delight.event.ModGameplayEvents;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.Objects;
import java.util.UUID;

@GameTestHolder(NortheastChinaDelight.MODID)
@PrefixGameTestTemplate(false)
public final class PillowSleepGameTests
{
    private PillowSleepGameTests()
    {
    }

    private static ServerPlayer serverPlayer(GameTestHelper helper)
    {
        ServerLevel level = helper.getLevel();
        return new ServerPlayer(level.getServer(), level,
                new GameProfile(UUID.randomUUID(), "pillow-test-player"), ClientInformation.createDefault());
    }

    private static void presetRespawn(ServerPlayer player, BlockPos pos)
    {
        player.setRespawnPosition(player.serverLevel().dimension(), pos, 0.0F, false, false);
    }

    private static BlockState pillow(Direction facing)
    {
        return ModBlocks.PILLOW.get().defaultBlockState().setValue(FacingDecorBlock.FACING, facing);
    }

    private static InteractionResult usePillow(ServerLevel level, BlockPos pos, Player player)
    {
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        return level.getBlockState(pos).useWithoutItem(level, player, hit);
    }

    private static void setTime(ServerLevel level, long time)
    {
        level.setDayTime(time);
        level.updateSkyBrightness();
    }

    private static SleepingPillowBlock.MissingRequirement missingRequirement(ServerLevel level, BlockPos pos)
    {
        return SleepingPillowBlock.evaluateSleep(level, level.getBlockState(pos), pos).missingRequirement();
    }

    private static void preparePillowLayout(ServerLevel level, BlockPos pos, BlockPos quiltPos)
    {
        level.setBlockAndUpdate(pos.below(), Blocks.WHITE_WOOL.defaultBlockState());
        level.setBlockAndUpdate(pos, pillow(Direction.NORTH));
        level.setBlockAndUpdate(pos.north(), Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(pos.north().above(), Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(pos.north().below(), Blocks.WHITE_WOOL.defaultBlockState());
        level.setBlockAndUpdate(pos.above(), Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(pos.south(), Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(pos.south().above(), Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(pos.south().below(), Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(quiltPos, ModBlocks.FOLDED_QUILT.get().defaultBlockState());
    }

    private static void beginPillowSleep(GameTestHelper helper, ServerLevel level, ServerPlayer player,
                                         BlockPos pos, BlockPos quiltPos)
    {
        preparePillowLayout(level, pos, quiltPos);
        player.moveTo(pos.getX() + 2.5, pos.getY(), pos.getZ() + 0.5, 0.0F, 0.0F);
        setTime(level, 18000L);
        presetRespawn(player, pos);
        usePillow(level, pos, player);
        helper.assertTrue(player.isSleeping(), "完整布局下应能开始枕头睡眠");
        helper.assertTrue(((QuiltSleepData) player).ncd$getSleepSide().orElse(null) == Direction.NORTH,
                "应记录本次选中的睡眠方向 D");
    }

    private static void assertPillowSleepCleared(GameTestHelper helper, ServerLevel level, ServerPlayer player,
                                                 BlockPos quiltPos, String reason)
    {
        QuiltSleepData data = (QuiltSleepData) player;
        helper.assertFalse(player.isSleeping(), reason + " 时应立即起床");
        helper.assertTrue(data.ncd$getQuiltPos().isEmpty(), reason + " 后应清空被子坐标");
        helper.assertTrue(data.ncd$getSleepSide().isEmpty(), reason + " 后应清空睡眠方向");
        BlockState quiltState = level.getBlockState(quiltPos);
        helper.assertFalse(FoldedQuiltBlock.isOccupied(quiltState), reason + " 后被子不应仍处于占用状态");
    }

    @GameTest(template = "empty")
    public static void pillowSleepSidePrefersFacingThenOtherAirSide(GameTestHelper helper)
    {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(4, 3, 4));
        level.setBlockAndUpdate(pos.below(), Blocks.WHITE_WOOL.defaultBlockState());
        level.setBlockAndUpdate(pos, pillow(Direction.EAST));
        level.setBlockAndUpdate(pos.east(), Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(pos.west(), Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(pos.above(), Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(pos.east().above(), Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(pos.west().above(), Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(pos.east().below(), Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(pos.west().below(), Blocks.WHITE_WOOL.defaultBlockState());

        helper.assertTrue(SleepingPillowBlock.sleepSide(level.getBlockState(pos), level, pos) == Direction.WEST,
                "枕头朝向侧不满足羊毛条件时应该换到相对方向");

        level.setBlockAndUpdate(pos.east().below(), Blocks.WHITE_WOOL.defaultBlockState());
        helper.assertTrue(SleepingPillowBlock.sleepSide(level.getBlockState(pos), level, pos) == Direction.EAST,
                "两侧都满足条件时应优先选择枕头朝向的一侧");

        level.setBlockAndUpdate(pos.west().below(), Blocks.STONE.defaultBlockState());
        helper.assertTrue(SleepingPillowBlock.sleepSide(level.getBlockState(pos), level, pos) == Direction.EAST,
                "相对方向不满足羊毛条件时应保留枕头朝向侧");

        level.setBlockAndUpdate(pos.east().below(), Blocks.STONE.defaultBlockState());
        helper.assertTrue(SleepingPillowBlock.sleepSide(level.getBlockState(pos), level, pos) == null,
                "两侧都不满足条件时不应选择 D 方向");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void pillowQuiltSearchUsesInclusiveNineByFiveByNineBox(GameTestHelper helper)
    {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(4, 4, 4));
        level.setBlockAndUpdate(pos, pillow(Direction.NORTH));

        BlockPos boundary = pos.offset(4, 2, 4);
        level.setBlockAndUpdate(boundary, ModBlocks.FOLDED_QUILT.get().defaultBlockState());
        helper.assertTrue(SleepingPillowBlock.findNearbyQuilt(level, pos).orElse(null).equals(boundary),
                "x/z=4、y=2 的边界应算在被子范围内");

        level.setBlockAndUpdate(boundary, Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(pos.offset(5, 2, 0), ModBlocks.FOLDED_QUILT.get().defaultBlockState());
        helper.assertTrue(SleepingPillowBlock.findNearbyQuilt(level, pos).isEmpty(),
                "x=5 不应算在被子范围内");

        level.setBlockAndUpdate(pos.offset(5, 2, 0), Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(pos.offset(0, 3, 0), ModBlocks.FOLDED_QUILT.get().defaultBlockState());
        helper.assertTrue(SleepingPillowBlock.findNearbyQuilt(level, pos).isEmpty(),
                "y=3 不应算在被子范围内");

        level.setBlockAndUpdate(pos.offset(0, 3, 0), Blocks.AIR.defaultBlockState());
        BlockPos nearest = pos.offset(1, 0, 1);
        level.setBlockAndUpdate(pos.offset(4, 0, 4), ModBlocks.FOLDED_QUILT.get().defaultBlockState());
        level.setBlockAndUpdate(nearest, ModBlocks.FOLDED_QUILT.get().defaultBlockState());
        helper.assertTrue(SleepingPillowBlock.findNearbyQuilt(level, pos).orElse(null).equals(nearest),
                "多块被子时应选距离枕头最近的一块");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void pillowRequiresVanillaRulesAndFullBeddingLayout(GameTestHelper helper)
    {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(4, 3, 4));
        BlockPos quiltPos = pos.east(3);
        long oldTime = level.getDayTime();
        try
        {
            level.setBlockAndUpdate(pos.below(), Blocks.WHITE_WOOL.defaultBlockState());
            level.setBlockAndUpdate(pos.north().below(), Blocks.WHITE_WOOL.defaultBlockState());
            level.setBlockAndUpdate(pos.south(), Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(pos.south().above(), Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(pos, pillow(Direction.NORTH));
            level.setBlockAndUpdate(quiltPos, ModBlocks.FOLDED_QUILT.get().defaultBlockState());

            ServerPlayer player = serverPlayer(helper);
            QuiltSleepData quiltData = (QuiltSleepData) player;
            player.moveTo(pos.getX() + 2.5, pos.getY(), pos.getZ() + 0.5, 0.0F, 0.0F);
            BlockPos oldRespawn = player.getRespawnPosition();
            helper.assertTrue(missingRequirement(level, pos) == null,
                    "完整布局不应缺少睡眠条件");

            setTime(level, 6000L);
            usePillow(level, pos, player);
            helper.assertTrue(!player.isSleeping(), "白天不应该入睡");
            helper.assertTrue(Objects.equals(oldRespawn, player.getRespawnPosition()),
                    "原版条件失败时不应提前写入重生点");

            setTime(level, 18000L);

            level.setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
            usePillow(level, pos, player);
            helper.assertTrue(!player.isSleeping(), "枕头下方不是羊毛时不应入睡");
            helper.assertTrue(missingRequirement(level, pos) == SleepingPillowBlock.MissingRequirement.WOOL,
                    "枕头下方不是羊毛时应提示缺羊毛");
            level.setBlockAndUpdate(pos.below(), Blocks.WHITE_WOOL.defaultBlockState());

            level.setBlockAndUpdate(pos.north(), Blocks.STONE.defaultBlockState());
            usePillow(level, pos, player);
            helper.assertTrue(!player.isSleeping(), "枕头两侧都不通时不应入睡");
            helper.assertTrue(missingRequirement(level, pos) == SleepingPillowBlock.MissingRequirement.SPACE,
                    "两侧都被挡住时应提示缺空间");
            level.setBlockAndUpdate(pos.north(), Blocks.AIR.defaultBlockState());

            level.setBlockAndUpdate(pos.north().below(), Blocks.STONE.defaultBlockState());
            usePillow(level, pos, player);
            helper.assertTrue(!player.isSleeping(), "一侧缺羊毛而另一侧缺空间时不应入睡");
            helper.assertTrue(missingRequirement(level, pos) == SleepingPillowBlock.MissingRequirement.SPACE,
                    "一侧缺羊毛而另一侧缺空间时应优先提示缺空间");
            level.setBlockAndUpdate(pos.north().below(), Blocks.WHITE_WOOL.defaultBlockState());

            level.setBlockAndUpdate(quiltPos, Blocks.AIR.defaultBlockState());
            usePillow(level, pos, player);
            helper.assertTrue(!player.isSleeping(), "附近没有被子时不应入睡");
            helper.assertTrue(quiltData.ncd$getQuiltPos().isEmpty(), "没有被子时不应记录被子坐标");
            helper.assertTrue(missingRequirement(level, pos) == SleepingPillowBlock.MissingRequirement.QUILT,
                    "附近没有被子时应提示缺被子");
            level.setBlockAndUpdate(quiltPos, ModBlocks.FOLDED_QUILT.get().defaultBlockState());

            presetRespawn(player, pos);
            usePillow(level, pos, player);
            helper.assertTrue(player.isSleeping(), "完整布局且夜晚时应该入睡");
            helper.assertTrue(player.getSleepingPos().orElse(null).equals(pos),
                    "玩家应睡在枕头方块上");
            helper.assertTrue(player.getBedOrientation() == Direction.SOUTH,
                    "D 为北时镜头应朝北，对应的原版床方向应为南");
            helper.assertTrue(pos.equals(player.getRespawnPosition()), "成功入睡后重生点应设为枕头");
            helper.assertTrue(quiltData.ncd$getQuiltPos().orElse(null).equals(quiltPos),
                    "成功入睡后应记录选中的被子坐标");
            helper.assertTrue(FoldedQuiltBlock.isOccupied(level.getBlockState(quiltPos)),
                    "成功入睡后被子应进入占用外观");
            helper.assertTrue(level.getBlockEntity(quiltPos) instanceof FoldedQuiltBlockEntity quilt
                            && quilt.isSelectedBy(player.getUUID()),
                    "被子实体应记录选中它的玩家 UUID");

            var respawn = level.getBlockState(pos).getRespawnPosition(
                    net.minecraft.world.entity.EntityType.PLAYER, level, pos, 0.0F);
            helper.assertTrue(respawn.isPresent(), "枕头应该能作为重生位置");

            player.stopSleepInBed(true, false);
            helper.assertTrue(quiltData.ncd$getQuiltPos().isEmpty(), "醒来后应清空被子坐标");
            helper.assertTrue(!FoldedQuiltBlock.isOccupied(level.getBlockState(quiltPos)),
                    "醒来后被子应恢复正常外观");
            helper.assertTrue(level.getBlockEntity(quiltPos) instanceof FoldedQuiltBlockEntity releasedQuilt
                            && !releasedQuilt.isSelected(),
                    "醒来后被子实体应清除占用玩家");
        }
        finally
        {
            level.setDayTime(oldTime);
            level.updateSkyBrightness();
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void pillowReportsSpaceWhenWoolAndSpaceProblemsCoexist(GameTestHelper helper)
    {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(4, 3, 4));
        BlockPos quiltPos = pos.east(3);
        long oldTime = level.getDayTime();
        ServerPlayer player = serverPlayer(helper);
        try
        {
            level.setBlockAndUpdate(pos.below(), Blocks.WHITE_WOOL.defaultBlockState());
            level.setBlockAndUpdate(pos.north(), Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(pos.north().below(), Blocks.WHITE_WOOL.defaultBlockState());
            level.setBlockAndUpdate(pos.south(), Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(pos.south().below(), Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(pos, pillow(Direction.SOUTH));
            level.setBlockAndUpdate(quiltPos, ModBlocks.FOLDED_QUILT.get().defaultBlockState());
            setTime(level, 18000L);

            helper.assertTrue(missingRequirement(level, pos) == SleepingPillowBlock.MissingRequirement.SPACE,
                    "朝向侧缺羊毛、相对侧缺空间时应提示缺空间");

            player.moveTo(pos.getX() + 2.5, pos.getY(), pos.getZ() + 0.5, 0.0F, 0.0F);
            presetRespawn(player, pos);
            usePillow(level, pos, player);
            helper.assertTrue(!player.isSleeping(), "两侧分别缺羊毛和空间时不应入睡");
        }
        finally
        {
            level.setDayTime(oldTime);
            level.updateSkyBrightness();
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void pillowReportsWoolWhenBothSidesHaveSpaceButNoWool(GameTestHelper helper)
    {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(4, 3, 4));
        level.setBlockAndUpdate(pos.below(), Blocks.WHITE_WOOL.defaultBlockState());
        level.setBlockAndUpdate(pos.north(), Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(pos.south(), Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(pos.above(), Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(pos.north().above(), Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(pos.south().above(), Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(pos.north().below(), Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(pos.south().below(), Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(pos, pillow(Direction.NORTH));
        level.setBlockAndUpdate(pos.east(3), ModBlocks.FOLDED_QUILT.get().defaultBlockState());

        helper.assertTrue(missingRequirement(level, pos) == SleepingPillowBlock.MissingRequirement.WOOL,
                "两侧都有空间但前下方都不是羊毛时应提示缺羊毛");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void pillowReportsWoolBeforeQuiltWhenPillowNotOnWool(GameTestHelper helper)
    {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(4, 3, 4));
        level.setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(pos.north(), Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(pos.south(), Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(pos, pillow(Direction.NORTH));

        helper.assertTrue(missingRequirement(level, pos) == SleepingPillowBlock.MissingRequirement.WOOL,
                "枕头下方没有羊毛时应先提示缺羊毛，而不是缺被子或缺空间");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void pillowFallsBackWhenPreferredHeadspaceBlocked(GameTestHelper helper)
    {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(4, 3, 4));
        BlockPos quiltPos = pos.north(3);
        long oldTime = level.getDayTime();
        ServerPlayer player = serverPlayer(helper);
        try
        {
            level.setBlockAndUpdate(pos.below(), Blocks.WHITE_WOOL.defaultBlockState());
            level.setBlockAndUpdate(pos.east().below(), Blocks.WHITE_WOOL.defaultBlockState());
            level.setBlockAndUpdate(pos.west().below(), Blocks.WHITE_WOOL.defaultBlockState());
            level.setBlockAndUpdate(pos.east(), Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(pos.west(), Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(pos.above(), Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(pos.east().above(), Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(pos.west().above(), Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(pos, pillow(Direction.EAST));
            level.setBlockAndUpdate(quiltPos, ModBlocks.FOLDED_QUILT.get().defaultBlockState());
            setTime(level, 18000L);

            SleepingPillowBlock.SleepEvaluation evaluation =
                    SleepingPillowBlock.evaluateSleep(level, level.getBlockState(pos), pos);
            helper.assertTrue(evaluation.canSleep(), "相对侧空间完整时应能睡眠");
            helper.assertTrue(evaluation.side() == Direction.WEST,
                    "朝向侧头部空间不足时应回退到相对侧");

            player.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 2.5, 0.0F, 0.0F);
            presetRespawn(player, pos);
            usePillow(level, pos, player);
            helper.assertTrue(player.isSleeping(), "朝向侧头部空间不足但相对侧可用时应入睡");
            helper.assertTrue(player.getBedOrientation() == Direction.EAST,
                    "D 为西时镜头应朝西，对应的原版床方向应为东");
        }
        finally
        {
            if (player.isSleeping())
            {
                player.stopSleepInBed(true, false);
            }
            level.setDayTime(oldTime);
            level.updateSkyBrightness();
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void selectedQuiltCannotBeTakenBySecondPlayer(GameTestHelper helper)
    {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(4, 3, 4));
        BlockPos quiltPos = pos.east(3);
        long oldTime = level.getDayTime();
        ServerPlayer first = serverPlayer(helper);
        ServerPlayer second = serverPlayer(helper);
        try
        {
            level.setBlockAndUpdate(pos.below(), Blocks.WHITE_WOOL.defaultBlockState());
            level.setBlockAndUpdate(pos.north().below(), Blocks.WHITE_WOOL.defaultBlockState());
            level.setBlockAndUpdate(pos.south(), Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(pos.south().above(), Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(pos, pillow(Direction.NORTH));
            level.setBlockAndUpdate(quiltPos, ModBlocks.FOLDED_QUILT.get().defaultBlockState());
            setTime(level, 18000L);

            first.moveTo(pos.getX() + 2.5, pos.getY(), pos.getZ() + 0.5, 0.0F, 0.0F);
            presetRespawn(first, pos);
            usePillow(level, pos, first);
            helper.assertTrue(first.isSleeping(), "第一名玩家应能选中空闲被子并入睡");

            second.moveTo(pos.getX() + 2.5, pos.getY(), pos.getZ() + 0.5, 0.0F, 0.0F);
            helper.assertTrue(missingRequirement(level, pos) == SleepingPillowBlock.MissingRequirement.QUILT,
                    "只能找到已占用被子时应提示缺被子");
            usePillow(level, pos, second);
            helper.assertTrue(!second.isSleeping(), "已被选中的被子不能让第二名玩家再次选中");
            helper.assertTrue(level.getBlockEntity(quiltPos) instanceof FoldedQuiltBlockEntity quilt
                            && quilt.isSelectedBy(first.getUUID()),
                    "被子的占用者应保持为第一名玩家");
            helper.assertTrue(!FoldedQuiltBlock.release(level, quiltPos, second.getUUID()),
                    "非占用者不能释放被子");

            first.stopSleepInBed(true, false);
            helper.assertTrue(!FoldedQuiltBlock.isOccupied(level.getBlockState(quiltPos)),
                    "占用者醒来后被子应重新可选");
            helper.assertTrue(SleepingPillowBlock.findNearbyQuilt(level, pos).orElse(null).equals(quiltPos),
                    "被子释放后应重新进入候选列表");
        }
        finally
        {
            if (first.isSleeping())
            {
                first.stopSleepInBed(true, false);
            }
            level.setDayTime(oldTime);
            level.updateSkyBrightness();
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void occupiedQuiltKeepsCollisionAndRejectsOtherPlayerBreaking(GameTestHelper helper)
    {
        ServerLevel level = helper.getLevel();
        BlockPos quiltPos = helper.absolutePos(new BlockPos(4, 3, 4));
        ServerPlayer owner = serverPlayer(helper);
        ServerPlayer breaker = serverPlayer(helper);
        level.setBlockAndUpdate(quiltPos, ModBlocks.FOLDED_QUILT.get().defaultBlockState());

        helper.assertFalse(level.getBlockState(quiltPos).getCollisionShape(level, quiltPos).isEmpty(),
                "空闲被子应保留碰撞箱");
        helper.assertTrue(level.getBlockState(quiltPos).getPistonPushReaction() == PushReaction.DESTROY,
                "空闲被子应保持原版的活塞破坏行为");
        helper.assertTrue(FoldedQuiltBlock.select(level, quiltPos, owner.getUUID()), "应能占用空闲被子");

        BlockState occupied = level.getBlockState(quiltPos);
        helper.assertTrue(FoldedQuiltBlock.isOccupied(occupied), "被子应处于占用状态");
        helper.assertFalse(occupied.getCollisionShape(level, quiltPos).isEmpty(),
                "占用中的被子仍应保留碰撞箱");
        helper.assertTrue(occupied.getPistonPushReaction() == PushReaction.BLOCK,
                "占用中的被子不能被活塞推动或销毁");
        helper.assertTrue(occupied.getExplosionResistance(level, quiltPos, null) >= 3_600_000.0F,
                "占用中的被子应能抵挡常规爆炸");
        helper.assertFalse(occupied.isFlammable(level, quiltPos, Direction.UP),
                "占用中的被子不能被点燃");

        BlockEvent.BreakEvent blocked = new BlockEvent.BreakEvent(level, quiltPos, occupied, breaker);
        ModGameplayEvents.onBlockBreak(blocked);
        helper.assertTrue(blocked.isCanceled(), "其他玩家不能拆除占用中的被子");

        helper.assertTrue(FoldedQuiltBlock.release(level, quiltPos, owner.getUUID()), "占用者应能释放被子");
        BlockState released = level.getBlockState(quiltPos);
        helper.assertTrue(released.getPistonPushReaction() == PushReaction.DESTROY,
                "释放后的被子应恢复可被活塞破坏");
        BlockEvent.BreakEvent allowed = new BlockEvent.BreakEvent(level, quiltPos, released, breaker);
        ModGameplayEvents.onBlockBreak(allowed);
        helper.assertFalse(allowed.isCanceled(), "释放后的被子应恢复可拆除状态");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void pillowFallsBackToOppositeWoolSide(GameTestHelper helper)
    {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(4, 3, 4));
        long oldTime = level.getDayTime();
        ServerPlayer player = serverPlayer(helper);
        try
        {
            level.setBlockAndUpdate(pos.below(), Blocks.WHITE_WOOL.defaultBlockState());
            level.setBlockAndUpdate(pos.west().below(), Blocks.WHITE_WOOL.defaultBlockState());
            level.setBlockAndUpdate(pos.east().below(), Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(pos, pillow(Direction.EAST));
            level.setBlockAndUpdate(pos.north(3), ModBlocks.FOLDED_QUILT.get().defaultBlockState());

            player.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 2.5, 0.0F, 0.0F);
            setTime(level, 18000L);
            presetRespawn(player, pos);
            usePillow(level, pos, player);

            helper.assertTrue(player.isSleeping(), "羊毛向西铺时应该回退到相对方向并入睡");
            helper.assertTrue(player.getBedOrientation() == Direction.EAST,
                    "D 为西时镜头应朝西，对应的原版床方向应为东");
        }
        finally
        {
            if (player.isSleeping())
            {
                player.stopSleepInBed(true, false);
            }
            level.setDayTime(oldTime);
            level.updateSkyBrightness();
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void pillowSleepLayoutIsValidatedUntilWakeUp(GameTestHelper helper)
    {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(4, 3, 4));
        BlockPos quiltPos = pos.east(3);
        long oldTime = level.getDayTime();
        ServerPlayer player = serverPlayer(helper);
        try
        {
            beginPillowSleep(helper, level, player, pos, quiltPos);
            helper.assertTrue(SleepingPillowBlock.isSleepingLayoutValid(player), "完整布局应持续有效");
            SleepingPillowBlock.validateSleepingLayout(player);
            helper.assertTrue(player.isSleeping(), "完整布局未被改动时不应被叫醒");
            usePillow(level, pos, player);
            helper.assertTrue(player.isSleeping(), "睡眠中再次右键枕头不应打断本次睡眠");
            helper.assertTrue(((QuiltSleepData) player).ncd$getQuiltPos().isPresent(),
                    "睡眠中再次右键枕头不应清掉被子坐标");

            level.setBlockAndUpdate(pos.north().below(), Blocks.STONE.defaultBlockState());
            SleepingPillowBlock.validateSleepingLayout(player);
            assertPillowSleepCleared(helper, level, player, quiltPos, "所选侧羊毛被破坏");

            beginPillowSleep(helper, level, player, pos, quiltPos);
            level.setBlockAndUpdate(pos.north(), Blocks.STONE.defaultBlockState());
            SleepingPillowBlock.validateSleepingLayout(player);
            assertPillowSleepCleared(helper, level, player, quiltPos, "所选侧空位被堵住");

            beginPillowSleep(helper, level, player, pos, quiltPos);
            level.setBlockAndUpdate(pos.north().above(), Blocks.STONE.defaultBlockState());
            SleepingPillowBlock.validateSleepingLayout(player);
            assertPillowSleepCleared(helper, level, player, quiltPos, "头部空间被堵住");

            beginPillowSleep(helper, level, player, pos, quiltPos);
            level.setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
            SleepingPillowBlock.validateSleepingLayout(player);
            assertPillowSleepCleared(helper, level, player, quiltPos, "枕头下羊毛被破坏");

            beginPillowSleep(helper, level, player, pos, quiltPos);
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            SleepingPillowBlock.validateSleepingLayout(player);
            assertPillowSleepCleared(helper, level, player, quiltPos, "枕头被移除");

            beginPillowSleep(helper, level, player, pos, quiltPos);
            level.setBlockAndUpdate(quiltPos, Blocks.AIR.defaultBlockState());
            SleepingPillowBlock.validateSleepingLayout(player);
            assertPillowSleepCleared(helper, level, player, quiltPos, "被子被移除");

            beginPillowSleep(helper, level, player, pos, quiltPos);
            level.setBlockAndUpdate(quiltPos, ModBlocks.FOLDED_QUILT.get().defaultBlockState());
            SleepingPillowBlock.validateSleepingLayout(player);
            assertPillowSleepCleared(helper, level, player, quiltPos, "被子被替换为空闲被子");

            beginPillowSleep(helper, level, player, pos, quiltPos);
            level.setBlockAndUpdate(pos.south().below(), Blocks.WHITE_WOOL.defaultBlockState());
            level.setBlockAndUpdate(pos.north().below(), Blocks.STONE.defaultBlockState());
            helper.assertFalse(SleepingPillowBlock.isSleepingLayoutValid(player),
                    "另一侧变得合法也不能替代入睡时选定的方向");
            SleepingPillowBlock.validateSleepingLayout(player);
            assertPillowSleepCleared(helper, level, player, quiltPos, "原睡眠方向的羊毛被破坏");
        }
        finally
        {
            if (player.isSleeping())
            {
                player.stopSleepInBed(true, false);
            }
            level.setDayTime(oldTime);
            level.updateSkyBrightness();
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void pillowSleepLowersPlayerToPillowHeight(GameTestHelper helper)
    {
        ServerLevel level = helper.getLevel();
        BlockPos pillowPos = helper.absolutePos(new BlockPos(2, 1, 2));
        BlockPos quiltPos = pillowPos.east(1);
        BlockPos bedFoot = helper.absolutePos(new BlockPos(1, 1, 3));
        BlockPos bedHead = bedFoot.north();
        long oldTime = level.getDayTime();
        ServerPlayer player = serverPlayer(helper);
        try
        {
            beginPillowSleep(helper, level, player, pillowPos, quiltPos);
            double expectedPillowY = pillowPos.getY() + SleepingPillowBlock.SLEEP_POS_Y_OFFSET;
            helper.assertTrue(Math.abs(player.getY() - expectedPillowY) < 1.0E-6D,
                    "枕头入睡高度应与枕头高度匹配，期望 " + expectedPillowY + "，实际 " + player.getY());
            helper.assertTrue(player.blockPosition().equals(pillowPos),
                    "枕头入睡后玩家所在方块仍应是枕头位置");
            player.stopSleepInBed(true, false);

            level.setBlockAndUpdate(bedFoot.below(), Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(bedHead.below(), Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(bedFoot, Blocks.RED_BED.defaultBlockState()
                    .setValue(net.minecraft.world.level.block.BedBlock.FACING, Direction.NORTH)
                    .setValue(net.minecraft.world.level.block.BedBlock.PART,
                            net.minecraft.world.level.block.state.properties.BedPart.FOOT));
            level.setBlockAndUpdate(bedHead, Blocks.RED_BED.defaultBlockState()
                    .setValue(net.minecraft.world.level.block.BedBlock.FACING, Direction.NORTH)
                    .setValue(net.minecraft.world.level.block.BedBlock.PART,
                            net.minecraft.world.level.block.state.properties.BedPart.HEAD));
            player.startSleeping(bedHead);
            double expectedBedY = bedHead.getY() + 0.6875D;
            helper.assertTrue(Math.abs(player.getY() - expectedBedY) < 1.0E-6D,
                    "原版床入睡高度不应被枕头逻辑改动，期望 " + expectedBedY + "，实际 " + player.getY());
        }
        finally
        {
            if (player.isSleeping())
            {
                player.stopSleepInBed(true, false);
            }
            level.setDayTime(oldTime);
            level.updateSkyBrightness();
        }
        helper.succeed();
    }
}
