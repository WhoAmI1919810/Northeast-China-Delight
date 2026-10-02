package com.gunmu.northeast_china_delight.block;

import com.gunmu.northeast_china_delight.item.ModItems;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.util.TriState;

/**
 * 榛蘑（单株）：行为照抄原版蘑菇。
 *
 * <ul>
 *   <li>只能在「固体方块」上落脚，而且要**暗**（亮度 13 以下）才活得下去，和我们认识的真蘑菇一样；</li>
 *   <li>随机刻会像原版蘑菇那样慢慢往旁边「蹿」：附近同类超过 5 株就不长了，</li>
 *   <li>撒骨粉会原地长成一丛 {@link HazelMushroomColonyBlock 榛蘑簇}（这是玩家想种出菌簇的唯一办法，
 *       自然生成出来的永远都是单株）。</li>
 * </ul>
 */
public class HazelMushroomBlock extends BushBlock implements BonemealableBlock
{
    public static final MapCodec<HazelMushroomBlock> CODEC = simpleCodec(HazelMushroomBlock::new);

    /** 原版蘑菇的规矩：亮度到 13 就不长了 */
    public static final int MAX_LIGHT = 13;

    protected static final VoxelShape SHAPE = Block.box(5.0, 0.0, 5.0, 11.0, 6.0, 11.0);

    public HazelMushroomBlock(BlockBehaviour.Properties properties)
    {
        super(properties);
    }

    @Override
    public MapCodec<HazelMushroomBlock> codec()
    {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
    {
        return SHAPE;
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state)
    {
        return new ItemStack(ModItems.HAZEL_MUSHROOM.get());
    }

    /**
     * 原版蘑菇的扩散逻辑，一字不差地搬过来。
     *
     * <p>唯一的差别：原版蘑菇把「亮度 13 以上」写死在 {@code canSurvive} 里，亮处会直接被方块更新清掉。
     * 榛蘑是按「榛子丛周围每格 3%」撒出去的，开阔地（天光 15）很常见，照原版写法撒完就会当场消失，
     * 所以这里把「怕光」挪到扩散这一步：<b>亮处的榛蘑能活，但不会蔓延</b>。</p>
     */
    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random)
    {
        // 种在沃土上的榛蘑有概率原地长成榛蘑菌落（对齐农夫乐事：蘑菇菌落不是撒骨粉变出来的，
        // 是蘑菇种在沃土上一段时间后自动变成的）
        if (level.getBlockState(pos.below()).is(ModBlockTags.RICH_SOIL_CONVERTS_TO_COLONY)
                && random.nextInt(4) == 0)
        {
            if (ModBlocks.HAZEL_MUSHROOM_COLONY.get().defaultBlockState().canSurvive(level, pos))
            {
                level.setBlock(pos, ModBlocks.HAZEL_MUSHROOM_COLONY.get().defaultBlockState(), 2);
            }
            return;
        }
        // 太亮就不蔓延（原版蘑菇的「怕光」）
        if (level.getRawBrightness(pos, 0) >= MAX_LIGHT)
        {
            return;
        }
        if (random.nextInt(25) != 0)
        {
            return;
        }

        int nearbyQuota = 5;
        for (BlockPos around : BlockPos.betweenClosed(pos.offset(-4, -1, -4), pos.offset(4, 1, 4)))
        {
            if (level.getBlockState(around).is(this) && --nearbyQuota <= 0)
            {
                return;
            }
        }

        BlockPos target = pos.offset(random.nextInt(3) - 1, random.nextInt(2) - random.nextInt(2), random.nextInt(3) - 1);
        for (int attempt = 0; attempt < 4; attempt++)
        {
            if (level.isEmptyBlock(target) && state.canSurvive(level, target))
            {
                pos = target;
            }
            target = pos.offset(random.nextInt(3) - 1, random.nextInt(2) - random.nextInt(2), random.nextInt(3) - 1);
        }

        if (level.isEmptyBlock(target) && state.canSurvive(level, target))
        {
            level.setBlock(target, state, 2);
        }
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos)
    {
        return state.isSolidRender(level, pos);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos)
    {
        BlockPos below = pos.below();
        BlockState belowState = level.getBlockState(below);
        TriState soil = belowState.canSustainPlant(level, below, Direction.UP, state);
        if (belowState.is(BlockTags.MUSHROOM_GROW_BLOCK))
        {
            return true;
        }
        // 只挑地面，不挑亮度：亮处的榛蘑照活，只是不会蔓延（见 randomTick）
        return soil.isDefault() ? this.mayPlaceOn(belowState, level, below) : soil.isTrue();
    }

    // 榛蘑不吃骨粉 —— 原版蘑菇也是不能撒骨粉催大的。
    // 想拿到榛蘑菌落：把它种在农夫乐事的沃土（rich_soil）上，等随机刻。
    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state)
    {
        return false;
    }

    @Override
    public boolean isBonemealSuccess(net.minecraft.world.level.Level level, RandomSource random, BlockPos pos, BlockState state)
    {
        return false;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state)
    {
    }
}
