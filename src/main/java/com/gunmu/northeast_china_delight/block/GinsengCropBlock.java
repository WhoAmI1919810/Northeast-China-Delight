package com.gunmu.northeast_china_delight.block;

import com.gunmu.northeast_china_delight.item.ModItems;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 人参：8 个生长阶段的药材植株，长在灰化土／泥土／苔藓块上。
 *
 * <p>和普通作物不一样的地方：</p>
 * <ul>
 *   <li>必须长在<b>寒冷生物群系</b>里，头顶要有树叶遮阴，附近要有雪（雪层、雪块、细雪都算）；</li>
 *   <li>长得极慢（随机刻 1/30 的进度）；</li>
 *   <li>直接破坏植株<b>只掉参籽</b>，想拿到人参得空手刨它脚下的土（见 {@code GinsengHarvestEvents}）；</li>
 *   <li>覆雪状态（{@link BlockStateProperties#SNOWY}）交给游戏自己维护：头顶盖上雪就是覆雪形态，
 *       雪化了自动变回来 —— 和草方块那套一模一样。</li>
 * </ul>
 */
public class GinsengCropBlock extends BushBlock implements BonemealableBlock
{
    //? if >=1.20.2 {
    /*public static final MapCodec<GinsengCropBlock> CODEC = simpleCodec(GinsengCropBlock::new);
    *///?}

    public static final int MAX_AGE = 7;
    public static final IntegerProperty AGE = BlockStateProperties.AGE_7;
    public static final BooleanProperty SNOWY = BlockStateProperties.SNOWY;

    private static final int GROWTH_DENOMINATOR = 30;

    private static final int SHADE_HEIGHT = 6;
    private static final int SNOW_RANGE = 3;

    private static final VoxelShape[] SHAPE_BY_AGE = new VoxelShape[]{
            Block.box(5.0, 0.0, 5.0, 11.0, 3.0, 11.0),
            Block.box(5.0, 0.0, 5.0, 11.0, 5.0, 11.0),
            Block.box(5.0, 0.0, 5.0, 11.0, 7.0, 11.0),
            Block.box(4.0, 0.0, 4.0, 11.0, 9.0, 11.0),
            Block.box(3.0, 0.0, 3.0, 12.0, 11.0, 12.0),
            Block.box(3.0, 0.0, 3.0, 12.0, 13.0, 12.0),
            Block.box(3.0, 0.0, 3.0, 12.0, 15.0, 12.0),
            Block.box(3.0, 0.0, 3.0, 12.0, 15.0, 12.0)
    };

    public GinsengCropBlock(BlockBehaviour.Properties properties)
    {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(AGE, 0).setValue(SNOWY, false));
    }

    //? if >=1.20.2 {
    /*@Override
    public MapCodec<GinsengCropBlock> codec()
    {
        return CODEC;
    }
    *///?}

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
    {
        return SHAPE_BY_AGE[state.getValue(AGE)];
    }

    @Override
    //? if <1.20.2 {
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state)
    //?} else {
    /*public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state)
    *///?}
    {
        return new ItemStack(ModItems.GINSENG_SEEDS.get());
    }

    /**
     * 参籽种下去：必须「寒冷群系 + 头顶有树荫」，另外它和野山参一样可以踩在雪片上长
     * —— 雪地里地表是一层雪片，参苗长在雪面才不会把底下的土露出来。
     */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context)
    {
        BlockPos pos = context.getClickedPos();
        if (!this.canSurvive(this.defaultBlockState(), context.getLevel(), pos)
                || !hasShade(context.getLevel(), pos)
                || !hasSnowNearby(context.getLevel(), pos))
        {
            return null;
        }
        return this.defaultBlockState().setValue(SNOWY, isSnowyAt(context.getLevel(), pos));
    }

    private static boolean isSnow(BlockState state)
    {
        return state.is(Blocks.SNOW) || state.is(Blocks.SNOW_BLOCK);
    }

    /**
     * 这一株该不该是覆雪形态：只有实际占用的种植格或脚下土块有雪。
     * 生物群系气候不能作为判定，否则在无雪地面种植也会错误显示覆雪。
     */
    public static boolean isSnowyAt(LevelReader level, BlockPos pos)
    {
        return isSnow(level.getBlockState(pos))
                || isSnow(level.getBlockState(pos.below()));
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos)
    {
        if (state.is(ModBlockTags.GINSENG_PLANTABLE_ON))
        {
            return true;
        }
        return (state.is(Blocks.SNOW) || state.is(Blocks.SNOW_BLOCK))
                && level.getBlockState(pos.below()).is(ModBlockTags.GINSENG_PLANTABLE_ON);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos)
    {
        if (!this.mayPlaceOn(level.getBlockState(pos.below()), level, pos.below()))
        {
            return false;
        }
        // 只要求「寒冷群系」：野山参是按「这块地会落雪」长出来的，
        // 那时候地上还没有雪片（落雪那一步排在植被之后），不能拿「附近有雪」卡它。
        // 「头顶要遮阴 + 附近要有雪」是种参籽的规矩，放在 getStateForPlacement 里。
        return hasColdBiome(level, pos);
    }

    private static boolean hasColdBiome(LevelReader level, BlockPos pos)
    {
        // 世界生成阶段（WorldGenLevel 里的 canSurvive 检查）不能走 getBiome ——
        // 它会去算"贴壁"的邻块，触发邻近 chunk 的懒加载，正在生成的 chunk 又会反过来要这块，
        // 最终抛出 Requested chunk unavailable during world generation 把整个服务器打挂。
        // 改用 getUncachedNoiseBiome：这条路径直接问 BiomeSource，不读已生成的方块。
        var biome = level instanceof net.minecraft.world.level.WorldGenLevel
                ? level.getUncachedNoiseBiome(pos.getX() >> 2, pos.getY() >> 2, pos.getZ() >> 2)
                : level.getBiome(pos);
        return biome.value().getBaseTemperature() < 0.15F;
    }

    private static boolean hasShade(LevelReader level, BlockPos pos)
    {
        BlockPos.MutableBlockPos cursor = pos.mutable();
        for (int i = 0; i < SHADE_HEIGHT; i++)
        {
            cursor.move(Direction.UP);
            if (level.getBlockState(cursor).is(ModBlockTags.GINSENG_SHADE))
            {
                return true;
            }
        }
        return false;
    }

    private static boolean hasSnowNearby(LevelReader level, BlockPos pos)
    {
        for (BlockPos around : BlockPos.betweenClosed(
                pos.offset(-SNOW_RANGE, -2, -SNOW_RANGE), pos.offset(SNOW_RANGE, 2, SNOW_RANGE)))
        {
            if (level.getBlockState(around).is(ModBlockTags.GINSENG_SNOW_NEARBY))
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isRandomlyTicking(BlockState state)
    {
        return state.getValue(AGE) < MAX_AGE;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random)
    {
        if (state.getValue(AGE) >= MAX_AGE || !level.isAreaLoaded(pos, 1))
        {
            return;
        }
        if (!this.canSurvive(state, level, pos) || !hasShade(level, pos))
        {
            return;
        }
        if (cropGrowPre(level, pos, state, random.nextInt(GROWTH_DENOMINATOR) == 0))
        {
            level.setBlock(pos, state.setValue(AGE, state.getValue(AGE) + 1), 2);
            cropGrowPost(level, pos, state);
        }
    }

    private static boolean cropGrowPre(ServerLevel level, BlockPos pos, BlockState state, boolean def)
    {
        //? if <1.20.2 {
        return net.minecraftforge.common.ForgeHooks.onCropsGrowPre(level, pos, state, def);
        //?} else if <1.20.5 {
        /*return net.neoforged.neoforge.common.CommonHooks.onCropsGrowPre(level, pos, state, def);*/
        //?} else {
        /*return net.neoforged.neoforge.common.CommonHooks.canCropGrow(level, pos, state, def);
        *///?}
    }

    private static void cropGrowPost(ServerLevel level, BlockPos pos, BlockState state)
    {
        //? if <1.20.2 {
        net.minecraftforge.common.ForgeHooks.onCropsGrowPost(level, pos, state);
        //?} else if <1.20.5 {
        /*net.neoforged.neoforge.common.CommonHooks.onCropsGrowPost(level, pos, state);*/
        //?} else {
        /*net.neoforged.neoforge.common.CommonHooks.fireCropGrowPost(level, pos, state);
        *///?}
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        builder.add(AGE, SNOWY);
    }

    @Override
    //? if <1.20.2 {
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state, boolean isClient)
    //?} else {
    /*public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state)
    *///?}
    {
        return state.getValue(AGE) < MAX_AGE;
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state)
    {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state)
    {
        level.setBlock(pos, state.setValue(AGE, Math.min(MAX_AGE, state.getValue(AGE) + 1)), 2);
    }
}
