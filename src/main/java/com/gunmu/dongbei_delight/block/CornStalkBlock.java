package com.gunmu.dongbei_delight.block;

import com.gunmu.dongbei_delight.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 玉米上方的茎秆。
 *
 * 玉米「上半部分」的成长阶段记在这个方块上（美术上是上部的 3 张图）：
 * <ul>
 *     <li>{@link #STAGE_YOUNG}：刚抽出来的上格（第 6 阶段 / 年龄 5）；</li>
 *     <li>{@link #STAGE_GROWN}：长成但没结穗（第 7 阶段 / 年龄 6）；</li>
 *     <li>{@link #STAGE_RIPE}：结穗成熟（第 8 阶段 / 年龄 7），可以空手掰或者用剪刀剪下 2 个玉米。</li>
 * </ul>
 *
 * 没有对应的物品，只能由下方的玉米作物长出来；下方作物被破坏时会自动消失。
 */
public class CornStalkBlock extends Block {

    /** 上格的成长阶段：0 刚抽出、1 长成未结穗、2 结穗成熟 —— 和下方作物的年龄 5/6/7 一一对应 */
    public static final IntegerProperty STAGE = IntegerProperty.create("stage", 0, 2);
    public static final int STAGE_YOUNG = 0;
    public static final int STAGE_GROWN = 1;
    public static final int STAGE_RIPE = 2;

    private static final VoxelShape SHAPE = Block.box(2.0D, 0.0D, 2.0D, 14.0D, 16.0D, 14.0D);

    public CornStalkBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(STAGE, STAGE_YOUNG));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(STAGE);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /**
     * 空手右键掰玉米，或用剪刀剪下玉米：每次 2 个，植株退回茎秆阶段等待重新结穗。
     *
     * 注意：空手收获要求两只手都空着，避免主手拿骨粉时副手空手误触发。
     */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (state.getValue(STAGE) != STAGE_RIPE) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        boolean withShears = stack.is(Items.SHEARS);
        boolean emptyHand = stack.isEmpty() && player.getOffhandItem().isEmpty();
        if (!withShears && !emptyHand) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (!level.isClientSide) {
            popResource(level, pos, new ItemStack(ModItems.CORN.get(), 2));
            // 上格回到"刚抽出"，下方作物退回第 6 阶段（年龄 5）—— 上下一起重新长一遍
            level.setBlock(pos, state.setValue(STAGE, STAGE_YOUNG), 2);
            BlockPos belowPos = pos.below();
            if (level.getBlockState(belowPos).getBlock() instanceof CornCropBlock crop) {
                level.setBlock(belowPos, crop.getStateForAge(CornCropBlock.STALK_AGE), 2);
            }
            if (withShears) {
                stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).getBlock() instanceof CornCropBlock;
    }

    /**
     * 上半格**被打掉**时：下方作物退回「单格最后一阶」（年龄 5）。
     *
     * <p>不这么做的话，下格还停在年龄 6/7，下一个随机刻就把上格又长回来了 —— 等于没打掉。
     * 退回年龄 5 之后要再长一次（随机刻）才回到年龄 6，那时才会重新冒出上格。
     *
     * <p>注意和「收获玉米」区分开：右键 / 剪刀收获只是把上格换成未结穗的茎秆、
     * 下格保持年龄 6（仍然是两格高，等它自己重新结穗）；只有把上格打掉才退回年龄 5。
     */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!level.isClientSide && !newState.is(this)) {
            BlockPos belowPos = pos.below();
            BlockState below = level.getBlockState(belowPos);
            if (below.getBlock() instanceof CornCropBlock crop
                    && crop.getAge(below) >= CornCropBlock.STALK_AGE) {
                level.setBlock(belowPos, crop.getStateForAge(CornCropBlock.STALK_AGE - 1), 3);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                     LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (!state.canSurvive(level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }
}
