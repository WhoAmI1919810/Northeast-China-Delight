package com.gunmu.dongbei_delight.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import com.gunmu.dongbei_delight.item.ModItems;

import java.util.function.Supplier;

/**
 * 玉米作物：比其它作物更高，长到后期会在上方多出一节茎秆（两个方块高）。
 */
public class CornCropBlock extends DdCropBlock {

    /** 长到这个年龄后，上方开始出现茎秆 */
    public static final int STALK_AGE = 6;

    public CornCropBlock(BlockBehaviour.Properties properties, Supplier<? extends ItemLike> seed) {
        super(properties, seed);
    }

    /**
     * 空手右键成熟的玉米：掰下玉米，植株退回茎秆阶段继续生长，可以反复收获。
     * 直接破坏则会连同玉米茎秆一起掉落。
     *
     * 注意：必须两只手都空着才能收获。否则会出现这种情况——
     * 主手拿骨粉右键成熟的玉米时，骨粉因为「已经成熟、无法催熟」返回 PASS，
     * 游戏会接着用空着的副手再走一遍方块交互，从而误触发收获。
     */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (this.isMaxAge(state) && bothHandsEmpty(player)) {
            if (!level.isClientSide) {
                popResource(level, pos, new ItemStack(ModItems.CORN.get()));
                level.setBlock(pos, this.getStateForAge(STALK_AGE), 2);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    /** 两只手都空着（避免副手空手时误触发收获） */
    private static boolean bothHandsEmpty(Player player) {
        return player.getMainHandItem().isEmpty() && player.getOffhandItem().isEmpty();
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        super.randomTick(state, level, pos, random);
        refreshStalk(level, pos);
    }

    @Override
    public void growCrops(Level level, BlockPos pos, BlockState state) {
        super.growCrops(level, pos, state);
        if (level instanceof ServerLevel serverLevel) {
            refreshStalk(serverLevel, pos);
        }
    }

    /** 成熟后在上方补出茎秆，未成熟则清理掉多余的茎秆 */
    private void refreshStalk(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof CornCropBlock)) {
            return;
        }

        BlockPos abovePos = pos.above();
        BlockState aboveState = level.getBlockState(abovePos);
        Block stalk = ModBlocks.CORN_STALK.get();

        if (this.getAge(state) >= STALK_AGE) {
            if (aboveState.isAir()) {
                level.setBlock(abovePos, stalk.defaultBlockState(), 3);
            }
        } else if (aboveState.is(stalk)) {
            level.removeBlock(abovePos, false);
        }
    }
}
