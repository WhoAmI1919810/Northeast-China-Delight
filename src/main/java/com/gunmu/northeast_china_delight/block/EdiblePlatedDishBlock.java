package com.gunmu.northeast_china_delight.block;

import com.gunmu.chinese_regional_culinary_ingredients.block.PlatedDishBlock;
import com.gunmu.chinese_regional_culinary_ingredients.compat.displaydelight.DisplayDelightCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
//? if <1.20.5 {
/*import net.minecraft.world.InteractionHand;
*///?}
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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

/**
 * 摆在展示乐事餐盘上的可食用菜肴：大餐盘 3 口吃完、小餐盘 1 口直接吃完。
 *
 * <p>吃完最后一口后原地替换回对应的展示乐事空餐盘；若未安装展示乐事（如通过 {@code /setblock} 放置），
 * 则直接安全移除方块，不报错也不卡残块。</p>
 */
public abstract class EdiblePlatedDishBlock extends PlatedDishBlock
{
    protected EdiblePlatedDishBlock(BlockBehaviour.Properties properties, VoxelShape shape, ResourceLocation plate)
    {
        super(properties, shape, plate);
        IntegerProperty prop = this.bitesProperty();
        if (prop != null)
        {
            this.registerDefaultState(this.stateDefinition.any()
                    .setValue(FACING, Direction.NORTH)
                    .setValue(prop, 0));
        }
    }

    public static EdiblePlatedDishBlock oneBite(BlockBehaviour.Properties properties, VoxelShape shape,
                                                ResourceLocation plate)
    {
        return new OneBite(properties, shape, plate);
    }

    public static EdiblePlatedDishBlock threeBites(BlockBehaviour.Properties properties, VoxelShape shape,
                                                   ResourceLocation plate)
    {
        return new ThreeBite(properties, shape, plate);
    }

    protected abstract IntegerProperty bitesProperty();

    public abstract int maxBites();

    public int bites(BlockState state)
    {
        IntegerProperty prop = this.bitesProperty();
        return prop == null ? 0 : state.getValue(prop);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        super.createBlockStateDefinition(builder);
        IntegerProperty prop = this.bitesProperty();
        if (prop != null)
        {
            builder.add(prop);
        }
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context)
    {
        BlockState state = super.getStateForPlacement(context);
        IntegerProperty prop = this.bitesProperty();
        return prop == null ? state : state.setValue(prop, 0);
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
        int bites = this.bites(state);
        if (player.isSecondaryUseActive())
        {
            return bites == 0 ? takeBackFull(level, pos, player) : InteractionResult.PASS;
        }
        int maxBites = this.maxBites();
        return PlacedDishEating.tryEat(state, level, pos, player, bites, maxBites, () -> {
            IntegerProperty prop = this.bitesProperty();
            if (prop == null || bites + 1 >= maxBites)
            {
                Block plateBlock = DisplayDelightCompat.plateBlock(this.plate());
                if (plateBlock != null)
                {
                    level.setBlock(pos, Block.updateFromNeighbourShapes(plateBlock.defaultBlockState(), level, pos), Block.UPDATE_ALL);
                }
                else
                {
                    level.removeBlock(pos, false);
                    level.gameEvent(player, GameEvent.BLOCK_DESTROY, pos);
                }
            }
            else
            {
                level.setBlock(pos, state.setValue(prop, bites + 1), Block.UPDATE_ALL);
            }
        });
    }

    private InteractionResult takeBackFull(Level level, BlockPos pos, Player player)
    {
        if (!player.isSecondaryUseActive() || !player.getMainHandItem().isEmpty())
        {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide)
        {
            Block plateBlock = DisplayDelightCompat.plateBlock(this.plate());
            ItemStack dish = new ItemStack(PlacedDishEating.dishItemFor(this));
            if (plateBlock != null)
            {
                level.setBlock(pos, Block.updateFromNeighbourShapes(plateBlock.defaultBlockState(), level, pos), Block.UPDATE_ALL);
            }
            else
            {
                level.removeBlock(pos, false);
                level.gameEvent(player, GameEvent.BLOCK_DESTROY, pos);
            }
            if (!dish.isEmpty() && !player.addItem(dish))
            {
                player.drop(dish, false);
            }
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.6F, 1.2F);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state)
    {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos)
    {
        return this.maxBites() - this.bites(state);
    }

    private static final class OneBite extends EdiblePlatedDishBlock
    {
        private OneBite(BlockBehaviour.Properties properties, VoxelShape shape, ResourceLocation plate)
        {
            super(properties, shape, plate);
        }

        @Override
        protected IntegerProperty bitesProperty()
        {
            return null;
        }

        @Override
        public int maxBites()
        {
            return 1;
        }
    }

    private static final class ThreeBite extends EdiblePlatedDishBlock
    {
        private ThreeBite(BlockBehaviour.Properties properties, VoxelShape shape, ResourceLocation plate)
        {
            super(properties, shape, plate);
        }

        @Override
        protected IntegerProperty bitesProperty()
        {
            return EdibleDishBlock.BITES_3;
        }

        @Override
        public int maxBites()
        {
            return 3;
        }
    }
}
