package com.gunmu.northeast_china_delight.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.PushReaction;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 可被一名玩家选作盖被的叠放被子。占用时仍保留原方块、碰撞箱和掉落物，只把模型切为空模型。
 */
public class FoldedQuiltBlock extends FacingDecorBlock implements EntityBlock
{
    private static final float OCCUPIED_EXPLOSION_RESISTANCE = 3_600_000.0F;

    public static final BooleanProperty OCCUPIED = BlockStateProperties.OCCUPIED;

    public FoldedQuiltBlock(BlockBehaviour.Properties properties)
    {
        super(properties, Block.box(1, 0, 1, 15, 7, 15), false);
        registerDefaultState(defaultBlockState().setValue(OCCUPIED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        super.createBlockStateDefinition(builder);
        builder.add(OCCUPIED);
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state)
    {
        return isOccupied(state) ? PushReaction.BLOCK : PushReaction.DESTROY;
    }

    @Override
    public float getExplosionResistance(BlockState state, BlockGetter level, BlockPos pos, Explosion explosion)
    {
        return isOccupied(state) ? OCCUPIED_EXPLOSION_RESISTANCE : super.getExplosionResistance();
    }

    @Override
    public boolean isFlammable(BlockState state, BlockGetter level, BlockPos pos, Direction direction)
    {
        return !isOccupied(state) && super.isFlammable(state, level, pos, direction);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
    {
        return new FoldedQuiltBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type)
    {
        if (level.isClientSide || type != ModBlockEntities.FOLDED_QUILT.get() || !isOccupied(state))
        {
            return null;
        }
        return (tickLevel, tickPos, tickState, blockEntity) -> {
            if (blockEntity instanceof FoldedQuiltBlockEntity quilt)
            {
                quilt.serverTick((ServerLevel) tickLevel, tickPos, tickState);
            }
        };
    }

    public static boolean isOccupied(BlockState state)
    {
        return state.hasProperty(OCCUPIED) && state.getValue(OCCUPIED);
    }

    public static boolean select(ServerLevel level, BlockPos pos, UUID playerId)
    {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof FoldedQuiltBlock) || isOccupied(state))
        {
            return false;
        }
        if (!(level.getBlockEntity(pos) instanceof FoldedQuiltBlockEntity quilt) || !quilt.select(playerId))
        {
            return false;
        }
        if (!level.setBlock(pos, state.setValue(OCCUPIED, true), Block.UPDATE_CLIENTS))
        {
            quilt.clearSelection();
            return false;
        }
        return true;
    }

    public static boolean release(ServerLevel level, BlockPos pos, UUID playerId)
    {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof FoldedQuiltBlock)
                || !(level.getBlockEntity(pos) instanceof FoldedQuiltBlockEntity quilt)
                || !quilt.isSelectedBy(playerId))
        {
            return false;
        }
        if (isOccupied(state) && !level.setBlock(pos, state.setValue(OCCUPIED, false), Block.UPDATE_CLIENTS))
        {
            return false;
        }
        quilt.clearSelection();
        return true;
    }

    public static void clearStaleOccupation(ServerLevel level, BlockPos pos)
    {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof FoldedQuiltBlock))
        {
            return;
        }
        if (level.getBlockEntity(pos) instanceof FoldedQuiltBlockEntity quilt)
        {
            quilt.clearSelection();
        }
        if (isOccupied(state))
        {
            level.setBlock(pos, state.setValue(OCCUPIED, false), Block.UPDATE_CLIENTS);
        }
    }
}
