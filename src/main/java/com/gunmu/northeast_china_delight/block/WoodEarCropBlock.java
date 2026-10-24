package com.gunmu.northeast_china_delight.block;

import com.gunmu.northeast_china_delight.item.ModItems;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 段木木耳：把木耳贴在带皮的橡木 / 白桦 / 深色橡木（标签 {@link ModBlockTags#WOOD_EAR_LOGS}）侧面种下，
 * 像可可豆一样挂在原木上长。四个阶段：菌芽 → 小耳 → 半大 → 成熟的木耳丛。
 *
 * <p>{@code facing} 是「原木在哪一边」。成熟后右键收 2~3 个木耳，留下小耳接着长（和甜浆果丛一样）；
 * 挖掉时成熟的掉 2~3 个、没熟的掉回 1 个；原木没了就跟着掉下来。</p>
 */
public class WoodEarCropBlock extends HorizontalDirectionalBlock implements BonemealableBlock
{
    //? if >=1.20.2 {
    public static final MapCodec<WoodEarCropBlock> CODEC = simpleCodec(WoodEarCropBlock::new);
    //?}

    public static final int MAX_AGE = 3;
    public static final IntegerProperty AGE = IntegerProperty.create("age", 0, MAX_AGE);
    /** 收完一茬退回到哪个阶段 */
    public static final int REGROW_AGE = 1;

    /** 原木在北面时（模型里木耳从 z = 0 往外长）每个阶段占的范围 */
    private static final double[][] NORTH_BOXES = {
            {5.0, 5.0, 0.0, 11.0, 11.0, 1.5},
            {4.0, 4.0, 0.0, 12.0, 12.0, 2.5},
            {3.0, 3.0, 0.0, 13.0, 13.0, 3.5},
            {1.5, 1.5, 0.0, 14.5, 14.5, 5.0}
    };
    private static final VoxelShape[][] SHAPES = new VoxelShape[4][MAX_AGE + 1];

    static
    {
        for (Direction facing : Direction.Plane.HORIZONTAL)
        {
            for (int age = 0; age <= MAX_AGE; age++)
            {
                SHAPES[facing.get2DDataValue()][age] = attachedShape(NORTH_BOXES[age], facing);
            }
        }
    }

    public WoodEarCropBlock(Properties properties)
    {
        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(AGE, 0));
    }

    //? if >=1.20.2 {
    @Override
    public MapCodec<WoodEarCropBlock> codec()
    {
        return CODEC;
    }
    //?}

    /** 把「原木在北面」时的盒子转到原木所在的那一面（和方块状态里 y 旋转的方向一致） */
    static VoxelShape attachedShape(double[] b, Direction facing)
    {
        double x0 = b[0], y0 = b[1], z0 = b[2], x1 = b[3], y1 = b[4], z1 = b[5];
        return switch (facing)
        {
            case SOUTH -> Block.box(16 - x1, y0, 16 - z1, 16 - x0, y1, 16 - z0);
            case EAST -> Block.box(16 - z1, y0, x0, 16 - z0, y1, x1);
            case WEST -> Block.box(z0, y0, 16 - x1, z1, y1, 16 - x0);
            default -> Block.box(x0, y0, z0, x1, y1, z1);
        };
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        builder.add(FACING, AGE);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
    {
        return SHAPES[state.getValue(FACING).get2DDataValue()][state.getValue(AGE)];
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context)
    {
        BlockState state = this.defaultBlockState();
        for (Direction direction : context.getNearestLookingDirections())
        {
            if (direction.getAxis().isHorizontal())
            {
                state = state.setValue(FACING, direction);
                if (state.canSurvive(context.getLevel(), context.getClickedPos()))
                {
                    return state;
                }
            }
        }
        return null;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos)
    {
        return level.getBlockState(pos.relative(state.getValue(FACING))).is(ModBlockTags.WOOD_EAR_LOGS);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos)
    {
        if (direction == state.getValue(FACING) && !state.canSurvive(level, pos))
        {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    public boolean isRandomlyTicking(BlockState state)
    {
        return state.getValue(AGE) < MAX_AGE;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random)
    {
        int age = state.getValue(AGE);
        // 和可可豆一样：每次随机刻 1/5 的机会长一阶
        if (age < MAX_AGE && cropGrowPre(level, pos, state, random.nextInt(5) == 0))
        {
            BlockState grown = state.setValue(AGE, age + 1);
            level.setBlock(pos, grown, 2);
            cropGrowPost(level, pos, state);
        }
    }

    private static boolean cropGrowPre(ServerLevel level, BlockPos pos, BlockState state, boolean def)
    {
        //? if <1.20.2 {
        /*return net.minecraftforge.common.ForgeHooks.onCropsGrowPre(level, pos, state, def);
        *///?} else if <1.20.5 {
        /*return net.neoforged.neoforge.common.CommonHooks.onCropsGrowPre(level, pos, state, def);*/
        //?} else {
        return net.neoforged.neoforge.common.CommonHooks.canCropGrow(level, pos, state, def);
        //?}
    }

    private static void cropGrowPost(ServerLevel level, BlockPos pos, BlockState state)
    {
        //? if <1.20.2 {
        /*net.minecraftforge.common.ForgeHooks.onCropsGrowPost(level, pos, state);
        *///?} else if <1.20.5 {
        /*net.neoforged.neoforge.common.CommonHooks.onCropsGrowPost(level, pos, state);*/
        //?} else {
        net.neoforged.neoforge.common.CommonHooks.fireCropGrowPost(level, pos, state);
        //?}
    }

    //? if <1.20.5 {
    /*@Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit)
    {
        return harvest(state, level, pos);
    }
    *///?} else {
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit)
    {
        return harvest(state, level, pos);
    }
    //?}

    /** 成熟了才收：掉 2~3 个木耳，退回小耳接着长；没熟返回 PASS，手里的骨粉照常能用 */
    private static InteractionResult harvest(BlockState state, Level level, BlockPos pos)
    {
        if (state.getValue(AGE) < MAX_AGE)
        {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide)
        {
            popResource(level, pos, new ItemStack(ModItems.WOOD_EAR.get(), 2 + level.random.nextInt(2)));
            level.playSound(null, pos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.BLOCKS,
                    1.0F, 0.8F + level.random.nextFloat() * 0.4F);
            level.setBlock(pos, state.setValue(AGE, REGROW_AGE), 2);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    //? if <1.20.2 {
    /*public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state, boolean isClient)
    *///?} else {
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state)
    //?}
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

    @Override
    //? if <1.20.2 {
    /*public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state)
    *///?} else {
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state)
    //?}
    {
        return new ItemStack(ModItems.WOOD_EAR.get());
    }
}
