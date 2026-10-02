package com.gunmu.dongbei_delight.gametest;

import com.gunmu.dongbei_delight.DongbeiConfig;
import com.gunmu.dongbei_delight.DongbeiDelight;
import com.gunmu.dongbei_delight.block.ModBlocks;
import com.gunmu.dongbei_delight.event.ModGameplayEvents;
import com.gunmu.dongbei_delight.item.ModItems;
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
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 菜肴方块形态的三条规矩：
 * 不潜行右键 = 吃（不摆）、潜行右键 = 摆成方块、开关关掉时潜行右键也不摆（而且没有任何提示）。
 */
@GameTestHolder(DongbeiDelight.MODID)
@PrefixGameTestTemplate(false)
public final class DishPlacementGameTests
{
    private DishPlacementGameTests()
    {
    }

    @GameTest(template = "empty")
    public static void dishPlacesOnlyWhenSneaking(GameTestHelper helper)
    {
        ServerLevel level = helper.getLevel();
        BlockPos ground = helper.absolutePos(new BlockPos(1, 1, 1));
        BlockPos target = ground.above();
        level.setBlockAndUpdate(ground, Blocks.STONE.defaultBlockState());

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);

        // 1) 不潜行：不放置
        player.setShiftKeyDown(false);
        useOn(level, player, target);
        helper.assertTrue(level.getBlockState(target).isAir(), "不潜行右键不应该把菜摆出来");

        // 2) 潜行 + 开关打开：放置
        player.setShiftKeyDown(true);
        useOn(level, player, target);
        helper.assertTrue(level.getBlockState(target).is(ModBlocks.OLD_STYLE_GUO_BAO_ROU.get()),
                "潜行右键应该把菜摆成方块");

        // 3) 开关关掉：不放置（且不会有任何文字提示）
        level.setBlockAndUpdate(target, Blocks.AIR.defaultBlockState());
        boolean old = DongbeiConfig.DISH_PLACEMENT_ENABLED.get();
        DongbeiConfig.DISH_PLACEMENT_ENABLED.set(false);
        try
        {
            useOn(level, player, target);
            helper.assertTrue(level.getBlockState(target).isAir(), "开关关掉后潜行右键也不应该放置");
        }
        finally
        {
            DongbeiConfig.DISH_PLACEMENT_ENABLED.set(old);
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

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setShiftKeyDown(true);
        boolean oldPlacement = DongbeiConfig.DISH_PLACEMENT_ENABLED.get();
        DongbeiConfig.DISH_PLACEMENT_ENABLED.set(true);
        try
        {
            for (String id : ModBlocks.DISH_BLOCK_IDS)
            {
                level.setBlockAndUpdate(target, Blocks.AIR.defaultBlockState());
                var dishBlock = ModBlocks.DISH_BLOCKS.get(id);
                helper.assertTrue(dishBlock != null, "料理方块未注册: " + id);

                var item = BuiltInRegistries.ITEM.get(
                        ResourceLocation.fromNamespaceAndPath(DongbeiDelight.MODID, id));
                helper.assertTrue(item != net.minecraft.world.item.Items.AIR,
                        "料理物品未注册: " + id);
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item));
                BlockHitResult hit = new BlockHitResult(
                        Vec3.atCenterOf(ground).add(0.0, 0.5, 0.0), Direction.UP, ground, false);

                ModGameplayEvents.onDishRightClick(
                        new net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock(
                                player, InteractionHand.MAIN_HAND, ground, hit));
                helper.assertTrue(level.getBlockState(target).is(dishBlock.get()),
                        "料理未沿用统一的潜行右键放置路径: " + id);
            }
        }
        finally
        {
            DongbeiConfig.DISH_PLACEMENT_ENABLED.set(oldPlacement);
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
