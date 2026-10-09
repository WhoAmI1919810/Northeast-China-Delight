package com.gunmu.northeast_china_delight.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
//? if >=1.20.5 {
/*import net.minecraft.world.ItemInteractionResult;
*///?}
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

public class SkewerBlock extends Block
{
    public enum Part implements StringRepresentable
    {
        TOP("top"),
        BOTTOM("bottom");

        private final String name;

        Part(String name)
        {
            this.name = name;
        }

        @Override
        public String getSerializedName()
        {
            return name;
        }
    }

    public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);
    public static final IntegerProperty AGE = IntegerProperty.create("age", 0, 3);
    public static final int MAX_AGE = 3;

    private static final ThreadLocal<Set<BlockPos>> REMOVING =
            ThreadLocal.withInitial(HashSet::new);

    private final Supplier<? extends Item> harvestItem;

    public SkewerBlock(BlockBehaviour.Properties properties,
                       Supplier<? extends Item> harvestItem)
    {
        super(properties);
        this.harvestItem = harvestItem;
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(PART, Part.TOP)
                .setValue(AGE, 0));
    }

    public static boolean isValidSupport(Block block, BlockState support, LevelReader level, BlockPos pos)
    {
        return support.is(block) || support.isFaceSturdy(level, pos, Direction.DOWN);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        builder.add(PART, AGE);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos)
    {
        BlockPos above = pos.above();
        BlockState support = level.getBlockState(above);
        if (state.getValue(PART) == Part.TOP)
        {
            return isValidSupport(this, support, level, above);
        }
        return support.is(this) && support.getValue(PART) == Part.TOP;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                     LevelAccessor level, BlockPos pos, BlockPos neighborPos)
    {
        if (!state.canSurvive(level, pos))
        {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    //? if <1.20.5 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hitResult)
    {
        advance(state, level, pos);
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult)
    {
        return advance(state, level, pos);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult)
    {
        advance(state, level, pos);
        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }
    *///?}

    private InteractionResult advance(BlockState state, Level level, BlockPos pos)
    {
        int age = state.getValue(AGE);
        if (age >= MAX_AGE)
        {
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        if (!level.isClientSide)
        {
            level.setBlock(pos, state.setValue(AGE, age + 1), 3);
            popResource(level, pos, new ItemStack(harvestItem.get()));
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston)
    {
        if (!level.isClientSide && !newState.is(this))
        {
            Set<BlockPos> removing = REMOVING.get();
            if (removing.add(pos))
            {
                try
                {
                    dropSegment(level, pos, state);
                    BlockPos otherPos = state.getValue(PART) == Part.TOP ? pos.below() : pos.above();
                    BlockState otherState = level.getBlockState(otherPos);
                    if (otherState.is(this))
                    {
                        level.setBlock(otherPos, Blocks.AIR.defaultBlockState(), 3);
                    }
                }
                finally
                {
                    removing.remove(pos);
                    if (removing.isEmpty())
                    {
                        REMOVING.remove();
                    }
                }
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    private void dropSegment(Level level, BlockPos pos, BlockState state)
    {
        popResource(level, pos, new ItemStack(Items.STRING));
        int remainingSkewers = MAX_AGE - state.getValue(AGE);
        if (remainingSkewers > 0)
        {
            popResource(level, pos, new ItemStack(harvestItem.get(), remainingSkewers));
        }
    }
}
