package com.gunmu.northeast_china_delight.gametest;

import com.gunmu.northeast_china_delight.NortheastChinaDelight;
import com.gunmu.northeast_china_delight.advancement.ModAdvancements;
import com.gunmu.northeast_china_delight.block.FacingDecorBlock;
import com.gunmu.northeast_china_delight.block.ModBlocks;
import com.gunmu.northeast_china_delight.event.SleepComfort;
import com.gunmu.northeast_china_delight.util.DdIds;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BedPart;
//? if <1.20.2 {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?} else {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?}

import java.util.UUID;

@GameTestHolder(NortheastChinaDelight.MODID)
@PrefixGameTestTemplate(false)
public final class SleepComfortGameTests
{
    private SleepComfortGameTests()
    {
    }

    private static ServerPlayer serverPlayer(GameTestHelper helper)
    {
        ServerLevel level = helper.getLevel();
        //? if <1.20.2 {
        ServerPlayer player = new ServerPlayer(level.getServer(), level,
                new GameProfile(UUID.randomUUID(), "sleep-comfort-test-player"));
        //?} else {
        /*ServerPlayer player = new ServerPlayer(level.getServer(), level,
                new GameProfile(UUID.randomUUID(), "sleep-comfort-test-player"),
                net.minecraft.server.level.ClientInformation.createDefault());
        *///?}
        player.setGameMode(GameType.CREATIVE);
        return player;
    }

    private static void setBiome(GameTestHelper helper, BlockPos center, String biome)
    {
        BlockPos min = center.offset(-4, -4, -4);
        BlockPos max = center.offset(4, 8, 4);
        String command = "fillbiome %d %d %d %d %d %d %s".formatted(
                min.getX(), min.getY(), min.getZ(), max.getX(), max.getY(), max.getZ(), biome);
        helper.getLevel().getServer().getCommands().performPrefixedCommand(
                helper.getLevel().getServer().createCommandSourceStack().withLevel(helper.getLevel()), command);
    }

    private static void placeBed(ServerLevel level, BlockPos foot, Direction facing)
    {
        BlockPos head = foot.relative(facing);
        level.setBlockAndUpdate(foot.below(), Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(head.below(), Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(foot.above(), Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(head.above(), Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(foot, Blocks.RED_BED.defaultBlockState()
                .setValue(BedBlock.FACING, facing).setValue(BedBlock.PART, BedPart.FOOT));
        level.setBlockAndUpdate(head, Blocks.RED_BED.defaultBlockState()
                .setValue(BedBlock.FACING, facing).setValue(BedBlock.PART, BedPart.HEAD));
    }

    private static void placeRadiator(ServerLevel level, BlockPos pos)
    {
        level.setBlockAndUpdate(pos, ModBlocks.RADIATOR.get().defaultBlockState()
                .setValue(FacingDecorBlock.FACING, Direction.WEST));
    }

    private static ServerPlayer sleepOnBed(GameTestHelper helper, BlockPos foot, Direction facing)
    {
        ServerLevel level = helper.getLevel();
        setBiome(helper, foot, "minecraft:snowy_taiga");
        placeBed(level, foot, facing);
        ServerPlayer player = serverPlayer(helper);
        player.moveTo(foot.getX() + 0.5, foot.getY(), foot.getZ() + 0.5, 0.0F, 0.0F);
        level.setDayTime(18000L);
        level.updateSkyBrightness();
        player.startSleepInBed(foot);
        helper.assertTrue(player.isSleeping(), "测试玩家应能在床上入睡");
        return player;
    }

    @GameTest(template = "empty")
    public static void bedSleepNearRadiatorGrantsSixSeconds(GameTestHelper helper)
    {
        ServerLevel level = helper.getLevel();
        BlockPos foot = helper.absolutePos(new BlockPos(4, 3, 4));
        placeRadiator(level, foot.east(3));
        ServerPlayer player = sleepOnBed(helper, foot, Direction.NORTH);

        player.stopSleepInBed(true, false);

        MobEffectInstance regeneration = player.getEffect(MobEffects.REGENERATION);
        helper.assertTrue(regeneration != null, "寒夜里挨着暖气片睡床应获得生命恢复");
        helper.assertTrue(regeneration.getDuration() == SleepComfort.BED_REGENERATION_TICKS,
                "睡床应获得 6 秒生命恢复");

        //? if <1.20.2 {
        var advancement = level.getServer().getAdvancements().getAdvancement(ModAdvancements.COZY_SLEEP);
        //?} else {
        /*var advancement = level.getServer().getAdvancements().get(ModAdvancements.COZY_SLEEP);
        *///?}
        helper.assertTrue(advancement != null, "应加载睡舒坦了进度");
        helper.assertTrue(!player.getAdvancements().getOrStartProgress(advancement).isDone(),
                "只有枕头睡觉才发睡舒坦了进度");
    }

    @GameTest(template = "empty")
    public static void pillowSleepNearRadiatorGrantsTwelveSecondsAndAdvancement(GameTestHelper helper)
    {
        ServerLevel level = helper.getLevel();
        BlockPos pillow = helper.absolutePos(new BlockPos(4, 3, 4));
        setBiome(helper, pillow, "minecraft:snowy_taiga");
        level.setBlockAndUpdate(pillow.below(), Blocks.WHITE_WOOL.defaultBlockState());
        level.setBlockAndUpdate(pillow, ModBlocks.PILLOW.get().defaultBlockState()
                .setValue(FacingDecorBlock.FACING, Direction.NORTH));
        placeRadiator(level, pillow.west(4));

        ServerPlayer player = serverPlayer(helper);
        player.moveTo(pillow.getX() + 0.5, pillow.getY(), pillow.getZ() + 0.5, 0.0F, 0.0F);
        level.setDayTime(18000L);
        level.updateSkyBrightness();
        player.startSleepInBed(pillow);
        helper.assertTrue(player.isSleeping(), "测试玩家应能在枕头上入睡");

        player.stopSleepInBed(true, false);

        MobEffectInstance regeneration = player.getEffect(MobEffects.REGENERATION);
        helper.assertTrue(regeneration != null, "寒夜里挨着暖气片睡枕头应获得生命恢复");
        helper.assertTrue(regeneration.getDuration() == SleepComfort.PILLOW_REGENERATION_TICKS,
                "睡枕头应获得 12 秒生命恢复");

        //? if <1.20.2 {
        var advancement = level.getServer().getAdvancements().getAdvancement(ModAdvancements.COZY_SLEEP);
        helper.assertTrue(advancement != null, "应加载睡舒坦了进度");
        helper.assertTrue(advancement.getCriteria().containsKey("sleep"), "睡舒坦了进度应有 sleep 判据");
        helper.assertTrue(player.getAdvancements().getOrStartProgress(advancement).isDone(),
                "枕头加暖气片条件应发睡舒坦了进度");
        var homeVoice = level.getServer().getAdvancements().getAdvancement(
                DdIds.of(NortheastChinaDelight.MODID, "home_voice"));
        helper.assertTrue(homeVoice != null && homeVoice.getParent() != null
                        && homeVoice.getParent().getId().equals(ModAdvancements.COZY_SLEEP),
                "家乡之声应以睡舒坦了为前置");
        //?} else {
        /*var advancement = level.getServer().getAdvancements().get(ModAdvancements.COZY_SLEEP);
        helper.assertTrue(advancement != null, "应加载睡舒坦了进度");
        helper.assertTrue(advancement.value().criteria().containsKey("sleep"), "睡舒坦了进度应有 sleep 判据");
        helper.assertTrue(player.getAdvancements().getOrStartProgress(advancement).isDone(),
                "枕头加暖气片条件应发睡舒坦了进度");
        var homeVoice = level.getServer().getAdvancements().get(
                DdIds.of(NortheastChinaDelight.MODID, "home_voice"));
        helper.assertTrue(homeVoice != null && homeVoice.value().parent()
                        .filter(ModAdvancements.COZY_SLEEP::equals).isPresent(),
                "家乡之声应以睡舒坦了为前置");
        *///?}
    }

    @GameTest(template = "empty")
    public static void sleepComfortRequiresColdAndReachableRadiator(GameTestHelper helper)
    {
        ServerLevel level = helper.getLevel();
        BlockPos foot = helper.absolutePos(new BlockPos(4, 3, 4));
        placeBed(level, foot, Direction.NORTH);

        setBiome(helper, foot, "minecraft:plains");
        helper.assertTrue(level.getBiome(foot).value().getBaseTemperature() > SleepComfort.MAX_COLD_TEMPERATURE,
                "温暖群系测试前提应成立");
        placeRadiator(level, foot.east(3));
        ServerPlayer warmPlayer = serverPlayer(helper);
        warmPlayer.moveTo(foot.getX() + 0.5, foot.getY(), foot.getZ() + 0.5, 0.0F, 0.0F);
        level.setDayTime(18000L);
        level.updateSkyBrightness();
        warmPlayer.startSleepInBed(foot);
        warmPlayer.stopSleepInBed(true, false);
        helper.assertTrue(warmPlayer.getEffect(MobEffects.REGENERATION) == null,
                "群系温度高于 0.2 时不应获得生命恢复");

        setBiome(helper, foot, "minecraft:snowy_taiga");
        level.setBlockAndUpdate(foot.east(3), Blocks.AIR.defaultBlockState());
        ServerPlayer noRadiatorPlayer = serverPlayer(helper);
        noRadiatorPlayer.moveTo(foot.getX() + 0.5, foot.getY(), foot.getZ() + 0.5, 0.0F, 0.0F);
        noRadiatorPlayer.startSleepInBed(foot);
        noRadiatorPlayer.stopSleepInBed(true, false);
        helper.assertTrue(noRadiatorPlayer.getEffect(MobEffects.REGENERATION) == null,
                "附近没有暖气片时不应获得生命恢复");

        encloseSleepArea(level, foot);
        placeRadiator(level, foot.east(3));
        ServerPlayer blockedPlayer = serverPlayer(helper);
        blockedPlayer.moveTo(foot.getX() + 0.5, foot.getY(), foot.getZ() + 0.5, 0.0F, 0.0F);
        blockedPlayer.startSleepInBed(foot);
        helper.assertTrue(blockedPlayer.isSleeping(), "封闭房间里的床仍可入睡");
        helper.assertTrue(!SleepComfort.hasReachableRadiator(level, blockedPlayer.blockPosition()),
                "被实体方块隔开时不应把暖气片算作可达");
        blockedPlayer.stopSleepInBed(true, false);
        helper.assertTrue(blockedPlayer.getEffect(MobEffects.REGENERATION) == null,
                "暖气片与床之间没有通路时不应获得生命恢复");
        helper.succeed();
    }

    private static void encloseSleepArea(ServerLevel level, BlockPos center)
    {
        BlockPos min = center.offset(-2, -1, -2);
        BlockPos max = center.offset(2, 2, 2);
        for (BlockPos cursor : BlockPos.betweenClosed(min, max))
        {
            boolean boundary = cursor.getX() == min.getX() || cursor.getX() == max.getX()
                    || cursor.getY() == min.getY() || cursor.getY() == max.getY()
                    || cursor.getZ() == min.getZ() || cursor.getZ() == max.getZ();
            level.setBlockAndUpdate(cursor, boundary
                    ? Blocks.STONE.defaultBlockState()
                    : Blocks.AIR.defaultBlockState());
        }
        placeBed(level, center, Direction.NORTH);
    }
}
