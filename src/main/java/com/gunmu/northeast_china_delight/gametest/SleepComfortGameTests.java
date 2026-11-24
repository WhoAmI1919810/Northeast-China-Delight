package com.gunmu.northeast_china_delight.gametest;

import com.gunmu.northeast_china_delight.NortheastChinaDelight;
import com.gunmu.northeast_china_delight.advancement.ModAdvancements;
import com.gunmu.northeast_china_delight.block.FacingDecorBlock;
import com.gunmu.northeast_china_delight.block.ModBlocks;
import com.gunmu.northeast_china_delight.event.ModGameplayEvents;
import com.gunmu.northeast_china_delight.event.SleepComfort;
import com.gunmu.northeast_china_delight.util.DdIds;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
//? if <1.20.2 {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?} else {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
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

    private static final class TestServerPlayer extends ServerPlayer
    {
        private int sleepTimer = SleepComfort.MINIMUM_SLEEP_TICKS;

        private TestServerPlayer(ServerLevel level, GameProfile profile)
        {
            super(level.getServer(), level, profile);
        }

        @Override
        public int getSleepTimer()
        {
            return sleepTimer;
        }

        private void setSleepTimer(int ticks)
        {
            sleepTimer = ticks;
        }

        @Override
        protected void onEffectAdded(MobEffectInstance effectInstance, net.minecraft.world.entity.Entity source)
        {
        }

        @Override
        protected void onEffectUpdated(MobEffectInstance effectInstance, boolean forced,
                                       net.minecraft.world.entity.Entity source)
        {
        }

        @Override
        protected void onEffectRemoved(MobEffectInstance effectInstance)
        {
        }
    }

    private static TestServerPlayer serverPlayer(GameTestHelper helper)
    {
        ServerLevel level = helper.getLevel();
        //? if <1.20.2 {
        TestServerPlayer player = new TestServerPlayer(level,
                new GameProfile(UUID.randomUUID(), "sleep-comfort-test-player"));
        //?} else {
        /*TestServerPlayer player = new TestServerPlayer(level,
                new GameProfile(UUID.randomUUID(), "sleep-comfort-test-player"));
        *///?}
        CompoundTag gameTypes = new CompoundTag();
        gameTypes.putInt("playerGameType", GameType.CREATIVE.getId());
        player.loadGameTypes(gameTypes);
        return player;
    }

    private static void presetRespawn(ServerPlayer player, BlockPos pos)
    {
        player.setRespawnPosition(player.serverLevel().dimension(), pos, 0.0F, false, false);
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
        for (Direction direction : Direction.Plane.HORIZONTAL)
        {
            level.setBlockAndUpdate(foot.above().relative(direction), Blocks.AIR.defaultBlockState());
        }
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

    private static TestServerPlayer sleepOnBed(GameTestHelper helper, BlockPos foot, Direction facing)
    {
        ServerLevel level = helper.getLevel();
        setBiome(helper, foot, "minecraft:snowy_taiga");
        placeBed(level, foot, facing);
        TestServerPlayer player = serverPlayer(helper);
        player.moveTo(foot.getX() + 0.5, foot.getY(), foot.getZ() + 0.5, 0.0F, 0.0F);
        level.setDayTime(18000L);
        level.updateSkyBrightness();
        presetRespawn(player, foot);
        player.startSleeping(foot);
        helper.assertTrue(player.isSleeping(), "测试玩家应能进入睡眠状态");
        return player;
    }

    @GameTest(template = "empty")
    public static void bedSleepNearRadiatorGrantsSixSeconds(GameTestHelper helper)
    {
        ServerLevel level = helper.getLevel();
        BlockPos foot = helper.absolutePos(new BlockPos(4, 3, 4));
        placeRadiator(level, foot.east(3));
        ServerPlayer player = sleepOnBed(helper, foot, Direction.NORTH);

        SleepComfort.applyAfterWakeUp(player);

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
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void interruptedSleepBeforeMinimumDoesNotGrantRegeneration(GameTestHelper helper)
    {
        ServerLevel level = helper.getLevel();
        BlockPos foot = helper.absolutePos(new BlockPos(4, 3, 4));
        placeRadiator(level, foot.east(3));
        TestServerPlayer player = sleepOnBed(helper, foot, Direction.NORTH);

        player.setSleepTimer(SleepComfort.MINIMUM_SLEEP_TICKS - 1);
        player.stopSleepInBed(true, false);

        helper.assertTrue(player.getEffect(MobEffects.REGENERATION) == null,
                "未睡满原版深睡阈值时不应获得生命恢复");
        helper.assertTrue(!SleepComfort.isCooldownActive(player),
                "未发奖励时不应开始冷却");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void manualWakeAfterMinimumSleepGrantsRegeneration(GameTestHelper helper)
    {
        ServerLevel level = helper.getLevel();
        BlockPos foot = helper.absolutePos(new BlockPos(4, 3, 4));
        placeRadiator(level, foot.east(3));
        TestServerPlayer player = sleepOnBed(helper, foot, Direction.NORTH);

        player.setSleepTimer(SleepComfort.MINIMUM_SLEEP_TICKS);
        player.stopSleepInBed(true, false);

        MobEffectInstance regeneration = player.getEffect(MobEffects.REGENERATION);
        helper.assertTrue(regeneration != null, "睡满阈值后主动起床也应获得生命恢复");
        helper.assertTrue(regeneration.getDuration() == SleepComfort.BED_REGENERATION_TICKS,
                "主动起床仍应按床发放 6 秒生命恢复");
        helper.assertTrue(SleepComfort.isCooldownActive(player), "发奖励后应进入个人冷却");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void sleepComfortCooldownBlocksRepeatedReward(GameTestHelper helper)
    {
        ServerLevel level = helper.getLevel();
        BlockPos foot = helper.absolutePos(new BlockPos(4, 3, 4));
        placeRadiator(level, foot.east(3));
        TestServerPlayer player = sleepOnBed(helper, foot, Direction.NORTH);

        player.setSleepTimer(SleepComfort.MINIMUM_SLEEP_TICKS);
        player.stopSleepInBed(true, false);
        helper.assertTrue(player.getEffect(MobEffects.REGENERATION) != null,
                "第一次睡满阈值应获得生命恢复");

        player.removeEffect(MobEffects.REGENERATION);
        player.startSleeping(foot);
        player.setSleepTimer(SleepComfort.MINIMUM_SLEEP_TICKS);
        player.stopSleepInBed(true, false);

        helper.assertTrue(player.getEffect(MobEffects.REGENERATION) == null,
                "冷却期内再次睡满阈值不应再次获得生命恢复");
        helper.assertTrue(SleepComfort.isCooldownActive(player), "冷却应继续生效");
        TestServerPlayer respawned = serverPlayer(helper);
        ModGameplayEvents.onPlayerClone(new PlayerEvent.Clone(respawned, player, true));
        helper.assertTrue(SleepComfort.isCooldownActive(respawned), "死亡重生后应保留暖气奖励冷却");
        helper.succeed();
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
        level.setBlockAndUpdate(pillow.above(), Blocks.AIR.defaultBlockState());
        for (Direction direction : Direction.Plane.HORIZONTAL)
        {
            level.setBlockAndUpdate(pillow.above().relative(direction), Blocks.AIR.defaultBlockState());
        }
        placeRadiator(level, pillow.west(4));

        ServerPlayer player = serverPlayer(helper);
        player.moveTo(pillow.getX() + 0.5, pillow.getY(), pillow.getZ() + 0.5, 0.0F, 0.0F);
        level.setDayTime(18000L);
        level.updateSkyBrightness();
        presetRespawn(player, pillow);
        player.startSleeping(pillow);
        helper.assertTrue(player.isSleeping(), "测试玩家应能进入睡眠状态");

        SleepComfort.applyAfterWakeUp(player);

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
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void sleepComfortRequiresColdAndReachableRadiator(GameTestHelper helper)
    {
        ServerLevel level = helper.getLevel();
        BlockPos foot = helper.absolutePos(new BlockPos(4, 3, 4));
        placeBed(level, foot, Direction.NORTH);

        setBiome(helper, foot, "minecraft:plains");
        helper.assertTrue(level.getBiome(foot).value().getBaseTemperature() > SleepComfort.MAX_RADIATOR_TEMPERATURE,
                "温暖群系测试前提应成立");
        placeRadiator(level, foot.east(3));
        ServerPlayer warmPlayer = serverPlayer(helper);
        warmPlayer.moveTo(foot.getX() + 0.5, foot.getY(), foot.getZ() + 0.5, 0.0F, 0.0F);
        level.setDayTime(18000L);
        level.updateSkyBrightness();
        presetRespawn(warmPlayer, foot);
        warmPlayer.startSleepInBed(foot);
        warmPlayer.stopSleepInBed(true, false);
        helper.assertTrue(warmPlayer.getEffect(MobEffects.REGENERATION) == null,
                "群系温度高于 0.2 时不应获得生命恢复");

        setBiome(helper, foot, "minecraft:snowy_taiga");
        level.setBlockAndUpdate(foot.east(3), Blocks.AIR.defaultBlockState());
        ServerPlayer noRadiatorPlayer = serverPlayer(helper);
        noRadiatorPlayer.moveTo(foot.getX() + 0.5, foot.getY(), foot.getZ() + 0.5, 0.0F, 0.0F);
        presetRespawn(noRadiatorPlayer, foot);
        noRadiatorPlayer.startSleepInBed(foot);
        noRadiatorPlayer.stopSleepInBed(true, false);
        helper.assertTrue(noRadiatorPlayer.getEffect(MobEffects.REGENERATION) == null,
                "附近没有暖气片时不应获得生命恢复");

        encloseSleepArea(level, foot);
        placeRadiator(level, foot.east(3));
        ServerPlayer blockedPlayer = serverPlayer(helper);
        blockedPlayer.moveTo(foot.getX() + 0.5, foot.getY(), foot.getZ() + 0.5, 0.0F, 0.0F);
        presetRespawn(blockedPlayer, foot);
        blockedPlayer.startSleepInBed(foot);
        helper.assertTrue(blockedPlayer.isSleeping(), "封闭房间里的床仍可入睡");
        helper.assertTrue(!SleepComfort.hasReachableRadiator(level, blockedPlayer.blockPosition()),
                "被实体方块隔开时不应把暖气片算作可达");
        blockedPlayer.stopSleepInBed(true, false);
        helper.assertTrue(blockedPlayer.getEffect(MobEffects.REGENERATION) == null,
                "暖气片与床之间没有通路时不应获得生命恢复");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void radiatorRangeIncludesNineBlocksButNotTen(GameTestHelper helper)
    {
        ServerLevel level = helper.getLevel();
        BlockPos playerPos = helper.absolutePos(new BlockPos(4, 3, 4));

        BlockPos eightBlocksAway = playerPos.east(8);
        placeRadiator(level, eightBlocksAway);
        helper.assertTrue(SleepComfort.hasReachableRadiator(level, playerPos),
                "8 格内的暖气片应有效");
        level.setBlockAndUpdate(eightBlocksAway, Blocks.AIR.defaultBlockState());

        BlockPos nineBlocksAway = playerPos.east(9);
        placeRadiator(level, nineBlocksAway);
        helper.assertTrue(SleepComfort.hasReachableRadiator(level, playerPos),
                "9 格边界上的暖气片应有效");
        level.setBlockAndUpdate(nineBlocksAway, Blocks.AIR.defaultBlockState());

        placeRadiator(level, playerPos.east(10));
        helper.assertTrue(!SleepComfort.hasReachableRadiator(level, playerPos),
                "10 格外的暖气片不应有效");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void radiatorTemperatureBoundaryIncludesTaigaButNotPlains(GameTestHelper helper)
    {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(4, 3, 4));

        setBiome(helper, pos, "minecraft:old_growth_pine_taiga");
        helper.assertTrue(SleepComfort.isRadiatorTemperatureSuitable(level, pos),
                "基础温度 0.3 的群系应满足暖气片温度条件");

        setBiome(helper, pos, "minecraft:taiga");
        helper.assertTrue(SleepComfort.isRadiatorTemperatureSuitable(level, pos),
                "基础温度 0.25 的针叶林应满足暖气片温度条件");

        setBiome(helper, pos, "minecraft:plains");
        helper.assertTrue(!SleepComfort.isRadiatorTemperatureSuitable(level, pos),
                "基础温度高于 0.3 的群系不应满足暖气片温度条件");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void radiatorWarmthRequiresSuitableClimateAndVisibleRadiator(GameTestHelper helper)
    {
        ServerLevel level = helper.getLevel();
        // empty.nbt 为 5x5x5（相对坐标 0..4），1.21.1 会在 x=5 等外沿自动围屏障墙，
        // 玩家和东侧暖气片都放在模板内部，避免暖气片上方撞到外围屏障。
        BlockPos playerPos = helper.absolutePos(new BlockPos(2, 1, 2));
        BlockPos radiatorPos = playerPos.east(1);
        placeRadiator(level, radiatorPos);

        ServerPlayer player = serverPlayer(helper);
        player.moveTo(playerPos.getX() + 0.5, playerPos.getY(), playerPos.getZ() + 0.5, 0.0F, 0.0F);

        setBiome(helper, playerPos, "minecraft:old_growth_pine_taiga");
        helper.assertTrue(level.getBlockState(radiatorPos).is(ModBlocks.RADIATOR.get()), "暖气片应已放置");
        helper.assertTrue(SleepComfort.isRadiatorTemperatureSuitable(level, playerPos),
                "测试群系温度应满足暖气片条件");
        Vec3 source = Vec3.atCenterOf(radiatorPos).add(-0.51D, 0.0D, 0.0D);
        Vec3 target = player.getBoundingBox().getCenter();
        HitResult hit = level.clip(new ClipContext(source, target,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        String hitInfo = hit instanceof BlockHitResult blockHit
                ? blockHit.getBlockPos() + " " + level.getBlockState(blockHit.getBlockPos())
                : hit.getLocation().toString();
        helper.assertTrue(hit.getType() == HitResult.Type.MISS,
                "视线应无遮挡，实际命中 " + hit.getType() + " 于 " + hitInfo);
        helper.assertTrue(ModGameplayEvents.hasVisibleRadiator(player),
                "玩家与暖气片之间应无遮挡，位置 " + player.blockPosition()
                        + "，眼睛 " + player.getEyePosition() + "，暖气片 " + radiatorPos);
        helper.assertTrue(ModGameplayEvents.grantsRadiatorWarmth(player),
                "基础温度 0.3 且看得见暖气片时应获得温暖效果");

        setBiome(helper, playerPos, "minecraft:plains");
        helper.assertTrue(!ModGameplayEvents.grantsRadiatorWarmth(player),
                "温暖群系里不应持续获得暖气片温暖效果");

        setBiome(helper, playerPos, "minecraft:snowy_taiga");
        level.setBlockAndUpdate(radiatorPos, Blocks.AIR.defaultBlockState());
        helper.assertTrue(!ModGameplayEvents.grantsRadiatorWarmth(player),
                "附近没有暖气片时不应获得温暖效果");
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
