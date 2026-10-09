package com.gunmu.northeast_china_delight.block;

import com.mojang.serialization.MapCodec;
import com.gunmu.northeast_china_delight.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
//? if >=1.20.5 {
/*import net.minecraft.world.ItemInteractionResult;
*///?}
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * 「一大盆菜」的方块形式 —— 做法照农夫乐事的 {@code FeastBlock}（牧羊人派、填馅南瓜那套）。
 *
 * <p>**没有方块实体**，整道菜的状态只有两个方块状态属性：
 * <ul>
 *     <li>{@link #FACING}：朝向，放置时按玩家面朝的方向；</li>
 *     <li>{@link #SERVINGS}：还剩几份（0~4），取一份减一。</li>
 * </ul>
 *
 * <p>右键取一份：给一份「碗装XX」，份数减一；**取走最后一份时方块直接消失**，
 * 并把大脸盆还给玩家（所以盆不会丢）。
 *
 * <p>方块物品（一放就是一整盆）和碗装单品都在 {@link ModItems} 里登记。
 */
public class DdFeastBlock extends Block {

    //? if >=1.20.2 {
    /*public static final MapCodec<DdFeastBlock> CODEC = simpleCodec(DdFeastBlock::new);
    *///?}

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final int MAX_SERVINGS = 4;
    public static final IntegerProperty SERVINGS = IntegerProperty.create("servings", 0, MAX_SERVINGS);

    /**
     * 还有菜时的高度：盆身 6 像素 + 冒尖的食材最多到 7 像素。
     *
     * <p>（2026-09-24 去掉"漏斗"下两层之后重新量的：40 个模型里 38 个是 6 像素，
     * 只有 2 个冒尖的到 7 像素，所以取 7 就正好包住整道菜。）
     */
    private static final VoxelShape SHAPE_FULL = Block.box(1.0, 0.0, 1.0, 15.0, 7.0, 15.0);
    /** 空盆 / 残渣：只剩盆身，6 像素高 */
    private static final VoxelShape SHAPE_EMPTY = Block.box(1.0, 0.0, 1.0, 15.0, 6.0, 15.0);

    /** 取一份给什么（碗装XX）；只有 codec 反序列化出来的实例才会是 null */
    @Nullable
    private final Supplier<Item> servingItem;

    public DdFeastBlock(BlockBehaviour.Properties properties) {
        this(properties, null);
    }

    public DdFeastBlock(BlockBehaviour.Properties properties, @Nullable Supplier<Item> servingItem) {
        super(properties);
        this.servingItem = servingItem;
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(SERVINGS, MAX_SERVINGS));
    }

    //? if >=1.20.2 {
    /*@Override
    public MapCodec<DdFeastBlock> codec() {
        return CODEC;
    }
    *///?}

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        // 选中轮廓 = 碰撞箱 = 这道菜的真实高度（6~7 像素），不再是整格 16 像素。
        // 模型里最高的食材也只到 7 像素，所以右键照样点得到，不会出现"点高了没反应"。
        return state.getValue(SERVINGS) > 0 ? SHAPE_FULL : SHAPE_EMPTY;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(SERVINGS) > 0 ? SHAPE_FULL : SHAPE_EMPTY;
    }

    public int servings(BlockState state) {
        return state.getValue(SERVINGS);
    }

    /** 一放下去就是满满一盆 */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(SERVINGS, MAX_SERVINGS);
    }

    @Override
    //? if <1.20.5 {
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        ItemStack stack = player.getItemInHand(hand);
        int servings = state.getValue(SERVINGS);
        if (this.servingItem == null) {
            return InteractionResult.PASS;
        }
        // 必须拿**碗**来盛：右键一次盛走一份，碗被消耗，取一份「碗装XX」
        if (!stack.is(net.minecraft.world.item.Items.BOWL)) {
            if (!level.isClientSide) {
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                        servings > 0 ? "message.northeast_china_delight.pot_need_bowl" : "message.northeast_china_delight.pot_empty"), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (servings <= 0) {
            if (!level.isClientSide) {
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                        "message.northeast_china_delight.pot_empty"), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!level.isClientSide) {
            stack.shrink(1);
            popResource(level, pos, new ItemStack(this.servingItem.get()));
            if (servings == 1) {
                // 最后一份：连方块一起收走，把大脸盆还给玩家
                level.removeBlock(pos, false);
                popResource(level, pos, new ItemStack(ModItems.LARGE_BASIN.get()));
            } else {
                level.setBlock(pos, state.setValue(SERVINGS, servings - 1), Block.UPDATE_ALL);
            }
            level.playSound(null, pos, SoundEvents.WOOD_BREAK, SoundSource.PLAYERS,
                    0.8F, 0.8F + level.random.nextFloat() * 0.4F);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    //?} else {
    /*protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        int servings = state.getValue(SERVINGS);
        if (this.servingItem == null) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        // 必须拿**碗**来盛：右键一次盛走一份，碗被消耗，取一份「碗装XX」
        if (!stack.is(net.minecraft.world.item.Items.BOWL)) {
            if (!level.isClientSide) {
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                        servings > 0 ? "message.northeast_china_delight.pot_need_bowl" : "message.northeast_china_delight.pot_empty"), true);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (servings <= 0) {
            if (!level.isClientSide) {
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                        "message.northeast_china_delight.pot_empty"), true);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!level.isClientSide) {
            stack.shrink(1);
            popResource(level, pos, new ItemStack(this.servingItem.get()));
            if (servings == 1) {
                // 最后一份：连方块一起收走，把大脸盆还给玩家
                level.removeBlock(pos, false);
                popResource(level, pos, new ItemStack(ModItems.LARGE_BASIN.get()));
            } else {
                level.setBlock(pos, state.setValue(SERVINGS, servings - 1), Block.UPDATE_ALL);
            }
            level.playSound(null, pos, SoundEvents.WOOD_BREAK, SoundSource.PLAYERS,
                    0.8F, 0.8F + level.random.nextFloat() * 0.4F);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }
    *///?}

    /** 必须放在实心方块上（和蛋糕一样） */
    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                     LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (direction == Direction.DOWN && !state.canSurvive(level, pos)) {
            return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, SERVINGS);
    }

    /** 比较器按剩余份数输出（和蛋糕一样） */
    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return state.getValue(SERVINGS);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }
}
