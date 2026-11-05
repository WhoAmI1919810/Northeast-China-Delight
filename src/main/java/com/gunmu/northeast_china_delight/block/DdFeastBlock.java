package com.gunmu.northeast_china_delight.block;

import com.mojang.serialization.MapCodec;
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
 * 放法同碗装菜、盘装菜：潜行右键放下（{@code item/DdFeastItem}），放在哪都行（2026-11-05 用户）。
 */
public class DdFeastBlock extends Block {

    //? if >=1.20.2 {
    /*public static final MapCodec<DdFeastBlock> CODEC = simpleCodec(DdFeastBlock::new);
    *///?}

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final int MAX_SERVINGS = 4;
    public static final IntegerProperty SERVINGS = IntegerProperty.create("servings", 0, MAX_SERVINGS);

    private static final VoxelShape SHAPE_FULL = Block.box(1.0, 0.0, 1.0, 15.0, 7.0, 15.0);
    private static final VoxelShape SHAPE_EMPTY = Block.box(1.0, 0.0, 1.0, 15.0, 6.0, 15.0);

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
        return state.getValue(SERVINGS) > 0 ? SHAPE_FULL : SHAPE_EMPTY;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(SERVINGS) > 0 ? SHAPE_FULL : SHAPE_EMPTY;
    }

    public int servings(BlockState state) {
        return state.getValue(SERVINGS);
    }

    /**
     * 拿碗盛走的那一份。一般就是「碗装XX」；盆装谜之炖菜按盆里存的效果给一碗原版谜之炖菜（{@link MysteryStewPotBlock}）。
     * 在最后一份把方块换回大脸盆之前调，方块实体还在
     */
    protected ItemStack servingStack(Level level, BlockPos pos) {
        return new ItemStack(this.servingItem.get());
    }

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
        // 必须拿碗来盛：右键一次盛走一份，碗被消耗，取一份「碗装XX」
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
            popResource(level, pos, this.servingStack(level, pos));
            if (servings == 1) {
                // 最后一份：盆不倒空也不掉出来 —— 原地变成普通的大脸盆方块（想收走就把它敲下来）
                level.setBlock(pos, ModBlocks.LARGE_BASIN.get().defaultBlockState(), Block.UPDATE_ALL);
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
            popResource(level, pos, this.servingStack(level, pos));
            if (servings == 1) {
                // 最后一份：盆不倒空也不掉出来 —— 原地变成普通的大脸盆方块（想收走就把它敲下来）
                level.setBlock(pos, ModBlocks.LARGE_BASIN.get().defaultBlockState(), Block.UPDATE_ALL);
            } else {
                level.setBlock(pos, state.setValue(SERVINGS, servings - 1), Block.UPDATE_ALL);
            }
            level.playSound(null, pos, SoundEvents.WOOD_BREAK, SoundSource.PLAYERS,
                    0.8F, 0.8F + level.random.nextFloat() * 0.4F);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }
    *///?}

    // 放在哪都行、底下的方块拆了也不掉，同碗装菜、盘装菜（DishBlock）。以前要放在实心面上、底下没了就碎（和蛋糕一样），
    // 2026-11-05 用户：盆装菜的放置逻辑和碗装菜、盘装菜统一

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, SERVINGS);
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return state.getValue(SERVINGS);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }
}
