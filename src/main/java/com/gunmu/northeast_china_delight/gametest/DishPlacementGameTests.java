package com.gunmu.northeast_china_delight.gametest;

import com.gunmu.northeast_china_delight.block.ModBlocks;
import com.gunmu.northeast_china_delight.block.PlacedDishBlock;
import com.gunmu.northeast_china_delight.event.ModGameplayEvents;
import com.gunmu.northeast_china_delight.item.ModItems;
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
/*import net.minecraftforge.gametest.GameTestHolder;
*///?} else {
import net.neoforged.neoforge.gametest.GameTestHolder;
//?}
//? if <1.20.2 {
/*import net.minecraftforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
//?}

/**
 * 菜肴方块形态的规矩：不潜行右键 = 吃（不摆）、潜行右键 = 摆成方块。
 */
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
        /*return gameType == GameType.CREATIVE ? helper.makeMockPlayer() : helper.makeMockSurvivalPlayer();
        *///?} else {
        return helper.makeMockPlayer(gameType);
        //?}
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
            clickDish(player, ground);
            if (ModBlocks.canPlaceDish(id))
            {
                helper.assertTrue(level.getBlockState(target).is(dishBlock.get()),
                        "料理未沿用统一的潜行右键放置路径: " + id);
            }
            else
            {
                helper.assertTrue(level.getBlockState(target).isAir(),
                        "无器具料理不能潜行放下，只能摆到展示乐事的餐盘上: " + id);
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void kvassDrinksPlacesAndPicksUp(GameTestHelper helper)
    {
        ServerLevel level = helper.getLevel();
        BlockPos ground = helper.absolutePos(new BlockPos(1, 1, 1));
        BlockPos target = ground.above();
        level.setBlockAndUpdate(ground, Blocks.STONE.defaultBlockState());
        Player player = mockPlayer(helper, GameType.SURVIVAL);
        var kvass = ModItems.KVASS.get();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(kvass));

        player.setShiftKeyDown(false);
        clickDish(player, ground);
        helper.assertTrue(level.getBlockState(target).isAir() && player.isUsingItem(),
                "普通右键应该喝格瓦斯");
        player.stopUsingItem();

        player.setShiftKeyDown(true);
        clickDish(player, ground);
        var state = level.getBlockState(target);
        helper.assertTrue(state.is(ModBlocks.DISH_BLOCKS.get("kvass").get()), "潜行右键应该放下格瓦斯");
        helper.assertTrue(player.getMainHandItem().isEmpty(), "放下格瓦斯应消耗手里的一瓶");

        var outline = state.getShape(level, target);
        var collision = state.getCollisionShape(level, target);
        helper.assertTrue(outline.toAabbs().size() == 1 && outline.bounds().equals(collision.bounds()),
                "格瓦斯应使用贴合瓶身外沿的单个方框");
        helper.assertTrue(collision.toAabbs().size() > 1, "格瓦斯应保留瓶身的实体碰撞形状");

        helper.assertTrue(PlacedDishBlock.pickUp(state, level, target, player).consumesAction(),
                "潜行空手右键应该收回格瓦斯");
        helper.assertTrue(level.getBlockState(target).isAir() && player.getInventory().countItem(kvass) == 1,
                "收回应该返还一整瓶格瓦斯");
        helper.succeed();
    }

    private static void clickDish(Player player, BlockPos ground)
    {
        BlockHitResult hit = new BlockHitResult(
                Vec3.atCenterOf(ground).add(0.0, 0.5, 0.0), Direction.UP, ground, false);

        ModGameplayEvents.onDishRightClick(
                new
                        //? if <1.20.2 {
                        /*net.minecraftforge.event.entity.player.PlayerInteractEvent
                        *///?} else {
                        net.neoforged.neoforge.event.entity.player.PlayerInteractEvent
                        //?}
                        .RightClickBlock(
                        player, InteractionHand.MAIN_HAND, ground, hit));
    }

    private static void useOn(ServerLevel level, Player player, BlockPos pos)
    {
        ItemStack stack = new ItemStack(ModItems.OLD_STYLE_GUO_BAO_ROU.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos).add(0.0, 0.5, 0.0), Direction.UP, pos.below(), false);
        stack.getItem().useOn(new UseOnContext(level, player, InteractionHand.MAIN_HAND, stack, hit));
    }
}
