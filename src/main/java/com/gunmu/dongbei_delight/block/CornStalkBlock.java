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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 玉米上方的茎秆。
 *
 * 玉米「上半部分成熟」——结穗状态记在这个方块上：
 * <ul>
 *     <li>{@link #HAS_CORN} = false：茎秆还没结穗（下方作物年龄 6），或者玉米刚被掰走；</li>
 *     <li>{@link #HAS_CORN} = true：结穗成熟，可以空手掰或者用剪刀剪下 2 个玉米。</li>
 * </ul>
 *
 * 没有对应的物品，只能由下方的玉米作物长出来；下方作物被破坏时会自动消失。
 */
public class CornStalkBlock extends Block {

    /** 上半部分是否已经结穗（成熟） */
    public static final BooleanProperty HAS_CORN = BooleanProperty.create("has_corn");

    private static final VoxelShape SHAPE = Block.box(2.0D, 0.0D, 2.0D, 14.0D, 16.0D, 14.0D);

    public CornStalkBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(HAS_CORN, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HAS_CORN);
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
        if (!state.getValue(HAS_CORN)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        boolean withShears = stack.is(Items.SHEARS);
        boolean emptyHand = stack.isEmpty() && player.getOffhandItem().isEmpty();
        if (!withShears && !emptyHand) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (!level.isClientSide) {
            popResource(level, pos, new ItemStack(ModItems.CORN.get(), 2));
            level.setBlock(pos, state.setValue(HAS_CORN, false), 2);
            // 下方作物退回茎秆阶段，过一会儿重新结穗
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

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                     LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (!state.canSurvive(level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }
}
