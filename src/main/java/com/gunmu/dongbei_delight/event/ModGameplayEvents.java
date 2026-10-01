package com.gunmu.dongbei_delight.event;

import com.gunmu.dongbei_delight.block.GinsengCropBlock;
import com.gunmu.dongbei_delight.block.ModBlocks;
import com.gunmu.dongbei_delight.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * 两个和小玩法有关的钩子：
 *
 * <ul>
 *   <li><b>木耳</b>：给原木去皮的那一刻按树种／生物群系掷骰子（黑森林的深色橡木、白桦林的白桦 7%，
 *       橡木 5%，针叶树不产）。去皮本身就是「第一次」，已经去过的原木不会再触发；</li>
 *   <li><b>人参</b>：空手刨开成熟人参脚下的土，才会掉出人参和参籽。</li>
 * </ul>
 */
public final class ModGameplayEvents
{
    /** 黑森林／白桦林的去皮出木耳概率 */
    public static final float WOOD_EAR_FOREST_CHANCE = 0.07F;
    /** 橡木的去皮出木耳概率 */
    public static final float WOOD_EAR_OAK_CHANCE = 0.05F;

    private ModGameplayEvents()
    {
    }

    @SubscribeEvent
    public static void onToolModification(BlockEvent.BlockToolModificationEvent event)
    {
        if (event.isSimulated() || event.getItemAbility() != ItemAbilities.AXE_STRIP)
        {
            return;
        }
        LevelAccessor accessor = event.getLevel();
        if (!(accessor instanceof ServerLevel level))
        {
            return;
        }
        BlockPos pos = event.getPos();
        float chance = woodEarChance(level, pos, event.getState());
        if (chance <= 0.0F || level.random.nextFloat() >= chance)
        {
            return;
        }
        Block.popResource(level, pos, new ItemStack(ModItems.WOOD_EAR.get()));
    }

    /** 这一根原木在这个生物群系里去皮出木耳的概率 */
    private static float woodEarChance(ServerLevel level, BlockPos pos, BlockState logState)
    {
        Holder<Biome> biome = level.getBiome(pos);
        if (logState.is(Blocks.DARK_OAK_LOG) || logState.is(Blocks.DARK_OAK_WOOD))
        {
            return biome.is(Biomes.DARK_FOREST) ? WOOD_EAR_FOREST_CHANCE : 0.0F;
        }
        if (logState.is(Blocks.BIRCH_LOG) || logState.is(Blocks.BIRCH_WOOD))
        {
            return biome.is(Biomes.BIRCH_FOREST) || biome.is(Biomes.OLD_GROWTH_BIRCH_FOREST)
                    ? WOOD_EAR_FOREST_CHANCE : 0.0F;
        }
        if (logState.is(Blocks.OAK_LOG) || logState.is(Blocks.OAK_WOOD))
        {
            return WOOD_EAR_OAK_CHANCE;
        }
        return 0.0F;
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event)
    {
        if (!(event.getLevel() instanceof ServerLevel level))
        {
            return;
        }
        Player player = event.getPlayer();
        // 必须空手：拿着工具刨土只会把参苗一起铲掉
        if (!player.getMainHandItem().isEmpty())
        {
            return;
        }
        BlockPos plantPos = event.getPos().above();
        BlockState plant = level.getBlockState(plantPos);
        if (!plant.is(ModBlocks.GINSENG_CROP.get())
                || plant.getValue(GinsengCropBlock.AGE) < GinsengCropBlock.MAX_AGE)
        {
            return;
        }
        level.removeBlock(plantPos, false);
        Block.popResource(level, plantPos, new ItemStack(ModItems.GINSENG.get()));
        Block.popResource(level, plantPos, new ItemStack(ModItems.GINSENG_SEEDS.get(), 1 + level.random.nextInt(2)));
    }
}
