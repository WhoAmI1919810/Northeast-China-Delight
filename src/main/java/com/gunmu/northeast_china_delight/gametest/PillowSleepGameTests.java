package com.gunmu.northeast_china_delight.gametest;

import com.gunmu.northeast_china_delight.NortheastChinaDelight;
import com.gunmu.chinese_regional_culinary_ingredients.block.FacingDecorBlock;
import com.gunmu.northeast_china_delight.block.ModBlocks;
import com.gunmu.northeast_china_delight.block.SleepingPillowBlock;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

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

    @GameTest(template = "empty")
    public static void pillowSleepSidePrefersFacingThenOtherAirSide(GameTestHelper helper)
    {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(4, 3, 4));
        level.setBlockAndUpdate(pos.below(), Blocks.WHITE_WOOL.defaultBlockState());
        level.setBlockAndUpdate(pos, pillow(Direction.EAST));
        level.setBlockAndUpdate(pos.east(), Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(pos.west(), Blocks.AIR.defaultBlockState());
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

        level.setBlockAndUpdate(pos.offset(4, 2, 4), ModBlocks.FOLDED_QUILT.get().defaultBlockState());
        helper.assertTrue(SleepingPillowBlock.hasNearbyQuilt(level, pos),
                "x/z=4、y=2 的边界应算在被子范围内");

        level.setBlockAndUpdate(pos.offset(4, 2, 4), Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(pos.offset(5, 2, 0), ModBlocks.FOLDED_QUILT.get().defaultBlockState());
        helper.assertTrue(!SleepingPillowBlock.hasNearbyQuilt(level, pos),
                "x=5 不应算在被子范围内");

        level.setBlockAndUpdate(pos.offset(5, 2, 0), Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(pos.offset(0, 3, 0), ModBlocks.FOLDED_QUILT.get().defaultBlockState());
        helper.assertTrue(!SleepingPillowBlock.hasNearbyQuilt(level, pos),
                "y=3 不应算在被子范围内");
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
            player.moveTo(pos.getX() + 2.5, pos.getY(), pos.getZ() + 0.5, 0.0F, 0.0F);
            BlockPos oldRespawn = player.getRespawnPosition();

            setTime(level, 6000L);
            usePillow(level, pos, player);
            helper.assertTrue(!player.isSleeping(), "白天不应该入睡");
            helper.assertTrue(Objects.equals(oldRespawn, player.getRespawnPosition()),
                    "原版条件失败时不应提前写入重生点");

            setTime(level, 18000L);

            level.setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
            usePillow(level, pos, player);
            helper.assertTrue(!player.isSleeping(), "枕头下方不是羊毛时不应入睡");
            level.setBlockAndUpdate(pos.below(), Blocks.WHITE_WOOL.defaultBlockState());

            level.setBlockAndUpdate(pos.north(), Blocks.STONE.defaultBlockState());
            usePillow(level, pos, player);
            helper.assertTrue(!player.isSleeping(), "枕头两侧都不通时不应入睡");
            level.setBlockAndUpdate(pos.north(), Blocks.AIR.defaultBlockState());

            level.setBlockAndUpdate(pos.north().below(), Blocks.STONE.defaultBlockState());
            usePillow(level, pos, player);
            helper.assertTrue(!player.isSleeping(), "D 方向前下方不是羊毛时不应入睡");
            level.setBlockAndUpdate(pos.north().below(), Blocks.WHITE_WOOL.defaultBlockState());

            level.setBlockAndUpdate(quiltPos, Blocks.AIR.defaultBlockState());
            usePillow(level, pos, player);
            helper.assertTrue(!player.isSleeping(), "附近没有被子时不应入睡");
            level.setBlockAndUpdate(quiltPos, ModBlocks.FOLDED_QUILT.get().defaultBlockState());

            presetRespawn(player, pos);
            usePillow(level, pos, player);
            helper.assertTrue(player.isSleeping(), "完整布局且夜晚时应该入睡");
            helper.assertTrue(player.getSleepingPos().orElse(null).equals(pos),
                    "玩家应睡在枕头方块上");
            helper.assertTrue(player.getBedOrientation() == Direction.SOUTH,
                    "D 为北时镜头应朝北，对应的原版床方向应为南");
            helper.assertTrue(pos.equals(player.getRespawnPosition()), "成功入睡后重生点应设为枕头");

            var respawn = level.getBlockState(pos).getRespawnPosition(
                    net.minecraft.world.entity.EntityType.PLAYER, level, pos, 0.0F);
            helper.assertTrue(respawn.isPresent(), "枕头应该能作为重生位置");

            player.stopSleepInBed(true, false);
        }
        finally
        {
            level.setDayTime(oldTime);
            level.updateSkyBrightness();
        }
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
}
