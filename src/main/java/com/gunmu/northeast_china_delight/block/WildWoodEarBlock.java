package com.gunmu.northeast_china_delight.block;

import com.gunmu.northeast_china_delight.item.ModItems;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 野生木耳：树上自己长出来的一朵（见 {@code WildWoodEarFeature}），{@code facing} 是原木在哪一边。
 * 不会长大；空手右键摘下或者挖掉都给 1 个木耳，原木没了就跟着掉下来。
 */
public class WildWoodEarBlock extends HorizontalDirectionalBlock
{
    //? if >=1.20.2 {
    public static final MapCodec<WildWoodEarBlock> CODEC = simpleCodec(WildWoodEarBlock::new);
    //?}

    private static final double[] NORTH_BOX = {4.5, 4.5, 0.0, 11.5, 11.5, 3.0};
    private static final VoxelShape[] SHAPES = new VoxelShape[4];

    static
    {
        for (Direction facing : Direction.Plane.HORIZONTAL)
        {
            SHAPES[facing.get2DDataValue()] = WoodEarCropBlock.attachedShape(NORTH_BOX, facing);
        }
    }

    public WildWoodEarBlock(Properties properties)
    {
        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    //? if >=1.20.2 {
    @Override
    public MapCodec<WildWoodEarBlock> codec()
    {
        return CODEC;
    }
    //?}

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        builder.add(FACING);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
    {
        return SHAPES[state.getValue(FACING).get2DDataValue()];
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

    //? if <1.20.5 {
    /*@Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit)
    {
        return hand == InteractionHand.MAIN_HAND && player.getMainHandItem().isEmpty()
                ? pick(level, pos, player) : InteractionResult.PASS;
    }
    *///?} else {
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit)
    {
        return player.getMainHandItem().isEmpty() ? pick(level, pos, player) : InteractionResult.PASS;
    }
    //?}

    private static InteractionResult pick(Level level, BlockPos pos, Player player)
    {
        if (!level.isClientSide)
        {
            ItemStack ear = new ItemStack(ModItems.WOOD_EAR.get());
            if (!player.addItem(ear))
            {
                player.drop(ear, false);
            }
            level.playSound(null, pos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.BLOCKS, 1.0F, 1.0F);
            level.removeBlock(pos, false);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
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
