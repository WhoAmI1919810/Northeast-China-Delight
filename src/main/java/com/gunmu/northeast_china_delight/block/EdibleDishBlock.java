package com.gunmu.northeast_china_delight.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
//? if <1.20.5 {
/*import net.minecraft.world.InteractionHand;
*///?}
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.function.Supplier;

/**
 * 可分阶段食用的盘装菜（3 口）与碗装/黄铜碗装菜（2 口）。
 *
 * <ul>
 *   <li>非潜行空手右键：吃一口（受个人进食冷却与油腻/爽口影响），最后一口吃完直接移除方块并返还对应容器；</li>
 *   <li>潜行空手右键：仅满态（{@code bites == 0}）可收回整道菜；动过筷子后不可收回，破坏只掉容器。</li>
 * </ul>
 */
public abstract class EdibleDishBlock extends PlacedDishBlock
{
    public static final IntegerProperty BITES_2 = IntegerProperty.create("bites", 0, 1);
    public static final IntegerProperty BITES_3 = IntegerProperty.create("bites", 0, 2);

    private final Supplier<Item> container;

    protected EdibleDishBlock(BlockBehaviour.Properties properties, VoxelShape shape, Supplier<Item> container)
    {
        super(properties, shape);
        this.container = container;
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(this.bitesProperty(), 0));
    }

    public static EdibleDishBlock twoBites(BlockBehaviour.Properties properties, VoxelShape shape, Supplier<Item> container)
    {
        return new TwoBite(properties, shape, container);
    }

    public static EdibleDishBlock threeBites(BlockBehaviour.Properties properties, VoxelShape shape, Supplier<Item> container)
    {
        return new ThreeBite(properties, shape, container);
    }

    protected abstract IntegerProperty bitesProperty();

    public abstract int maxBites();

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        super.createBlockStateDefinition(builder);
        builder.add(this.bitesProperty());
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context)
    {
        return super.getStateForPlacement(context).setValue(this.bitesProperty(), 0);
    }

    //? if <1.20.5 {
    /*@Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit)
    {
        return hand == InteractionHand.MAIN_HAND ? interact(state, level, pos, player) : InteractionResult.PASS;
    }
    *///?} else {
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit)
    {
        return interact(state, level, pos, player);
    }
    //?}

    private InteractionResult interact(BlockState state, Level level, BlockPos pos, Player player)
    {
        IntegerProperty prop = this.bitesProperty();
        int bites = state.getValue(prop);
        if (player.isSecondaryUseActive())
        {
            return bites == 0 ? pickUp(state, level, pos, player) : InteractionResult.PASS;
        }
        int maxBites = this.maxBites();
        return PlacedDishEating.tryEat(state, level, pos, player, bites, maxBites, () -> {
            if (bites + 1 >= maxBites)
            {
                level.removeBlock(pos, false);
                level.gameEvent(player, GameEvent.BLOCK_DESTROY, pos);
                PlacedDishEating.giveContainer(player, this.container.get());
            }
            else
            {
                level.setBlock(pos, state.setValue(prop, bites + 1), Block.UPDATE_ALL);
            }
        });
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state)
    {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos)
    {
        return this.maxBites() - state.getValue(this.bitesProperty());
    }

    private static final class TwoBite extends EdibleDishBlock
    {
        private TwoBite(BlockBehaviour.Properties properties, VoxelShape shape, Supplier<Item> container)
        {
            super(properties, shape, container);
        }

        @Override
        protected IntegerProperty bitesProperty()
        {
            return BITES_2;
        }

        @Override
        public int maxBites()
        {
            return 2;
        }
    }

    private static final class ThreeBite extends EdibleDishBlock
    {
        private ThreeBite(BlockBehaviour.Properties properties, VoxelShape shape, Supplier<Item> container)
        {
            super(properties, shape, container);
        }

        @Override
        protected IntegerProperty bitesProperty()
        {
            return BITES_3;
        }

        @Override
        public int maxBites()
        {
            return 3;
        }
    }
}
