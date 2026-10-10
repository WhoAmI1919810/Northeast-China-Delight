package com.gunmu.northeast_china_delight.gametest;

import com.gunmu.northeast_china_delight.block.ModBlocks;
import com.gunmu.northeast_china_delight.event.ModGameplayEvents;
import com.gunmu.northeast_china_delight.item.ModItems;
import com.gunmu.northeast_china_delight.NortheastChinaConfig;
import com.gunmu.northeast_china_delight.NortheastChinaDelight;
import com.gunmu.northeast_china_delight.util.DdIds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
//? if <1.20.2 {
import net.minecraftforge.gametest.GameTestHolder;
//?} else {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
*///?}
//? if <1.20.2 {
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?} else {
/*import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?}

@GameTestHolder(NortheastChinaDelight.MODID)
@PrefixGameTestTemplate(false)
public final class DishPlacementGameTests
{
    private DishPlacementGameTests()
    {
    }

    private static Player mockPlayer(GameTestHelper helper, GameType gameType)
    {
        //? if <1.20.5 {
        return gameType == GameType.CREATIVE ? helper.makeMockPlayer() : helper.makeMockSurvivalPlayer();
        //?} else {
        /*return helper.makeMockPlayer(gameType);
        *///?}
    }

    @GameTest(template = "empty")
    public static void dishPlacesOnlyWhenSneaking(GameTestHelper helper)
    {
        ServerLevel level = helper.getLevel();
        BlockPos ground = helper.absolutePos(new BlockPos(1, 1, 1));
        BlockPos target = ground.above();
        level.setBlockAndUpdate(ground, Blocks.STONE.defaultBlockState());

        Player player = mockPlayer(helper, GameType.SURVIVAL);

        player.setShiftKeyDown(false);
        useOn(level, player, target);
        helper.assertTrue(level.getBlockState(target).isAir(), "不潜行右键不应该把菜摆出来");

        player.setShiftKeyDown(true);
        useOn(level, player, target);
        helper.assertTrue(level.getBlockState(target).is(ModBlocks.OLD_STYLE_GUO_BAO_ROU.get()),
                "潜行右键应该把菜摆成方块");

        level.setBlockAndUpdate(target, Blocks.AIR.defaultBlockState());
        boolean old = NortheastChinaConfig.DISH_PLACEMENT_ENABLED.get();
        NortheastChinaConfig.DISH_PLACEMENT_ENABLED.set(false);
        try
        {
            useOn(level, player, target);
            helper.assertTrue(level.getBlockState(target).isAir(), "开关关掉后潜行右键也不应该放置");
        }
        finally
        {
            NortheastChinaConfig.DISH_PLACEMENT_ENABLED.set(old);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void everyRegisteredDishUsesTheSamePlacementPath(GameTestHelper helper)
    {
        ServerLevel level = helper.getLevel();
        BlockPos ground = helper.absolutePos(new BlockPos(1, 1, 1));
        BlockPos target = ground.above();
        level.setBlockAndUpdate(ground, Blocks.STONE.defaultBlockState());

        Player player = mockPlayer(helper, GameType.SURVIVAL);
        player.setShiftKeyDown(true);
        boolean oldPlacement = NortheastChinaConfig.DISH_PLACEMENT_ENABLED.get();
        NortheastChinaConfig.DISH_PLACEMENT_ENABLED.set(true);
        try
        {
            for (String id : ModBlocks.DISH_BLOCK_IDS)
            {
                level.setBlockAndUpdate(target, Blocks.AIR.defaultBlockState());
                var dishBlock = ModBlocks.DISH_BLOCKS.get(id);
                helper.assertTrue(dishBlock != null, "料理方块未注册: " + id);

                var item = BuiltInRegistries.ITEM.get(
                        DdIds.of(NortheastChinaDelight.MODID, id));
                helper.assertTrue(item != net.minecraft.world.item.Items.AIR,
                        "料理物品未注册: " + id);
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item));
                BlockHitResult hit = new BlockHitResult(
                        Vec3.atCenterOf(ground).add(0.0, 0.5, 0.0), Direction.UP, ground, false);

                ModGameplayEvents.onDishRightClick(
                        new
                                //? if <1.20.2 {
                                net.minecraftforge.event.entity.player.PlayerInteractEvent
                                //?} else {
                                /*net.neoforged.neoforge.event.entity.player.PlayerInteractEvent
                                *///?}
                                .RightClickBlock(
                                player, InteractionHand.MAIN_HAND, ground, hit));
                helper.assertTrue(level.getBlockState(target).is(dishBlock.get()),
                        "料理未沿用统一的潜行右键放置路径: " + id);
            }
        }
        finally
        {
            NortheastChinaConfig.DISH_PLACEMENT_ENABLED.set(oldPlacement);
        }
        helper.succeed();
    }

    private static void useOn(ServerLevel level, Player player, BlockPos pos)
    {
        ItemStack stack = new ItemStack(ModItems.OLD_STYLE_GUO_BAO_ROU.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos).add(0.0, 0.5, 0.0), Direction.UP, pos.below(), false);
        stack.getItem().useOn(new UseOnContext(level, player, InteractionHand.MAIN_HAND, stack, hit));
    }
}
