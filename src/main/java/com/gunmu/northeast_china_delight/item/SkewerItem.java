package com.gunmu.northeast_china_delight.item;

import com.gunmu.northeast_china_delight.block.SkewerBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class SkewerItem extends BlockItem
{
    public SkewerItem(SkewerBlock block, Properties properties)
    {
        super(block, properties);
    }

    @Override
    public InteractionResult place(BlockPlaceContext context)
    {
        if (context.getClickedFace() != Direction.DOWN || !(this.getBlock() instanceof SkewerBlock block))
        {
            return InteractionResult.FAIL;
        }

        Level level = context.getLevel();
        BlockPos topPos = context.getClickedPos();
        BlockPos supportPos = topPos.above();
        BlockPos bottomPos = topPos.below();
        BlockState support = level.getBlockState(supportPos);
        if (!SkewerBlock.isValidSupport(block, support, level, supportPos)
                || !level.getBlockState(topPos).canBeReplaced(context)
                || !level.getBlockState(bottomPos).canBeReplaced(context))
        {
            return InteractionResult.FAIL;
        }

        BlockState topState = block.defaultBlockState().setValue(SkewerBlock.PART, SkewerBlock.Part.TOP);
        BlockState bottomState = block.defaultBlockState().setValue(SkewerBlock.PART, SkewerBlock.Part.BOTTOM);
        if (!topState.canSurvive(level, topPos))
        {
            return InteractionResult.FAIL;
        }

        if (!level.setBlock(topPos, topState, 11) || !level.setBlock(bottomPos, bottomState, 11))
        {
            return InteractionResult.FAIL;
        }

        Player player = context.getPlayer();
        level.playSound(player, topPos, topState.getSoundType(level, topPos, player).getPlaceSound(),
                net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 0.8F);
        level.gameEvent(net.minecraft.world.level.gameevent.GameEvent.BLOCK_PLACE,
                topPos, net.minecraft.world.level.gameevent.GameEvent.Context.of(player, topState));
        context.getItemInHand().consume(1, player);
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
