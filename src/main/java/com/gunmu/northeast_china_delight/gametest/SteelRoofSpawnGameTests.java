package com.gunmu.northeast_china_delight.gametest;

import com.gunmu.northeast_china_delight.NortheastChinaDelight;
import com.gunmu.northeast_china_delight.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
//? if <1.20.2 {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?} else {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?}

@GameTestHolder(NortheastChinaDelight.MODID)
@PrefixGameTestTemplate(false)
public final class SteelRoofSpawnGameTests
{
    private SteelRoofSpawnGameTests()
    {
    }

    @GameTest(template = "empty")
    public static void steelRoofSlabsRejectNaturalSpawns(GameTestHelper helper)
    {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(4, 3, 4));
        BlockState[] slabs = {
                ModBlocks.RED_STEEL_ROOF_SLAB.get().defaultBlockState(),
                ModBlocks.BLUE_STEEL_ROOF_SLAB.get().defaultBlockState(),
                ModBlocks.GREEN_STEEL_ROOF_SLAB.get().defaultBlockState()
        };
        for (BlockState slab : slabs)
        {
            for (SlabType type : SlabType.values())
            {
                BlockState state = slab.setValue(net.minecraft.world.level.block.SlabBlock.TYPE, type);
                helper.assertTrue(!state.isValidSpawn(level, pos, EntityType.ZOMBIE),
                        "彩钢瓦平屋顶不应允许自然刷怪：" + type);
            }
        }
        helper.succeed();
    }
}
