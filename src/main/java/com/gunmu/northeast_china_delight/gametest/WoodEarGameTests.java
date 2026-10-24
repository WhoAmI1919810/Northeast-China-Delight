package com.gunmu.northeast_china_delight.gametest;

import com.gunmu.northeast_china_delight.NortheastChinaDelight;
import com.gunmu.northeast_china_delight.block.ModBlocks;
import com.gunmu.northeast_china_delight.block.WildWoodEarBlock;
import com.gunmu.northeast_china_delight.block.WoodEarCropBlock;
import com.gunmu.northeast_china_delight.block.WoodEarRules;
import com.gunmu.northeast_china_delight.item.ModItems;
import com.gunmu.northeast_china_delight.worldgen.WildWoodEarFeature;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
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
 * 木耳：段木木耳的种、长、收，野生木耳只长在树上，概率和刮树皮一致。
 */
@GameTestHolder(NortheastChinaDelight.MODID)
@PrefixGameTestTemplate(false)
public final class WoodEarGameTests {

    private static final BlockPos LOG = new BlockPos(1, 2, 1);
    /** 原木南边那一格：木耳挂在原木的南面，facing 指回原木（北） */
    private static final BlockPos EAR = LOG.south();

    private WoodEarGameTests() {
    }

    private static Player survivalPlayer(GameTestHelper helper) {
        //? if <1.20.5 {
        /*return helper.makeMockSurvivalPlayer();
        *///?} else {
        return helper.makeMockPlayer(GameType.SURVIVAL);
        //?}
    }

    private static BlockState crop(int age) {
        return ModBlocks.WOOD_EAR_CROP.get().defaultBlockState()
                .setValue(WoodEarCropBlock.FACING, Direction.NORTH)
                .setValue(WoodEarCropBlock.AGE, age);
    }

    @GameTest(template = "empty")
    public static void woodEarGrowsOnlyOnLogsWithBark(GameTestHelper helper) {
        BlockPos ear = helper.absolutePos(EAR);
        for (var log : new net.minecraft.world.level.block.Block[] {Blocks.OAK_LOG, Blocks.BIRCH_LOG, Blocks.DARK_OAK_WOOD}) {
            helper.setBlock(LOG, log);
            helper.assertTrue(crop(0).canSurvive(helper.getLevel(), ear), "木耳应能挂在带皮的原木上：" + log);
        }
        for (var log : new net.minecraft.world.level.block.Block[] {Blocks.STRIPPED_OAK_LOG, Blocks.SPRUCE_LOG, Blocks.STONE}) {
            helper.setBlock(LOG, log);
            helper.assertTrue(!crop(0).canSurvive(helper.getLevel(), ear), "木耳不能挂在这上面：" + log);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void woodEarIsPlantedOnTheSideOfALog(GameTestHelper helper) {
        helper.setBlock(LOG, Blocks.OAK_LOG);
        Player player = survivalPlayer(helper);
        ItemStack ears = new ItemStack(ModItems.WOOD_EAR.get(), 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, ears);
        BlockPos log = helper.absolutePos(LOG);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(log).add(0.0, 0.0, 0.5), Direction.SOUTH, log, false);
        ears.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
        BlockState planted = helper.getLevel().getBlockState(helper.absolutePos(EAR));
        helper.assertTrue(planted.is(ModBlocks.WOOD_EAR_CROP.get())
                        && planted.getValue(WoodEarCropBlock.FACING) == Direction.NORTH
                        && planted.getValue(WoodEarCropBlock.AGE) == 0
                        && player.getMainHandItem().getCount() == 1,
                "对着原木侧面用木耳应种下段木木耳，朝向原木");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void matureWoodEarHarvestsAndRegrows(GameTestHelper helper) {
        helper.setBlock(LOG, Blocks.OAK_LOG);
        helper.setBlock(EAR, crop(WoodEarCropBlock.MAX_AGE));
        helper.useBlock(EAR, survivalPlayer(helper));
        BlockState after = helper.getLevel().getBlockState(helper.absolutePos(EAR));
        int dropped = helper.getLevel().getEntitiesOfClass(ItemEntity.class,
                        new AABB(helper.absolutePos(EAR)).inflate(2.0),
                        entity -> entity.getItem().is(ModItems.WOOD_EAR.get()))
                .stream().mapToInt(entity -> entity.getItem().getCount()).sum();
        helper.assertTrue(after.is(ModBlocks.WOOD_EAR_CROP.get())
                        && after.getValue(WoodEarCropBlock.AGE) == WoodEarCropBlock.REGROW_AGE
                        && dropped >= 2 && dropped <= 3,
                "成熟木耳右键应收 2~3 个，并退回小耳接着长");
        helper.setBlock(LOG, Blocks.AIR);
        helper.assertTrue(helper.getLevel().getBlockState(helper.absolutePos(EAR)).isAir(),
                "原木没了，木耳应跟着掉下来");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void wildWoodEarGrowsOnlyOnTrees(GameTestHelper helper) {
        BlockPos tree = new BlockPos(2, 1, 2);
        for (int y = 0; y < 3; y++) {
            helper.setBlock(tree.above(y), Blocks.OAK_LOG);
        }
        helper.setBlock(tree.above(3), Blocks.OAK_LEAVES);
        boolean grew = WildWoodEarFeature.grow(helper.getLevel(), helper.absolutePos(tree), RandomSource.create(7));
        boolean found = false;
        for (Direction side : Direction.Plane.HORIZONTAL) {
            BlockState state = helper.getLevel().getBlockState(helper.absolutePos(tree.relative(side)));
            if (state.is(ModBlocks.WILD_WOOD_EAR.get()) && state.getValue(WildWoodEarBlock.FACING) == side.getOpposite()) {
                found = true;
            }
        }
        helper.assertTrue(grew && found, "树上的橡木应能长出一朵朝向原木的野生木耳");
        // 把树叶拿掉，同一根原木就当成木屋框架：不再长野生木耳
        helper.setBlock(tree.above(3), Blocks.AIR);
        helper.assertTrue(!WildWoodEarFeature.grow(helper.getLevel(), helper.absolutePos(tree.above()), RandomSource.create(7)),
                "头顶没有树叶的原木（比如木屋框架）不长野生木耳");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void wildWoodEarUsesTheBarkScrapingChance(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(LOG);
        helper.assertTrue(WoodEarRules.chance(helper.getLevel(), pos, Blocks.OAK_LOG.defaultBlockState()) == WoodEarRules.OAK_CHANCE
                        && WoodEarRules.chance(helper.getLevel(), pos, Blocks.SPRUCE_LOG.defaultBlockState()) == 0.0F
                        && WoodEarRules.chance(helper.getLevel(), pos, Blocks.STRIPPED_OAK_LOG.defaultBlockState()) == 0.0F,
                "橡木 5%，针叶树和去了皮的原木不出");
        helper.succeed();
    }
}
