package com.gunmu.northeast_china_delight.item;

import com.gunmu.northeast_china_delight.block.PaperLatticeWindowBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 窗花：对着糊纸木格窗右键贴上去（点中间是团花、点四角是角花，见 {@link PaperLatticeWindowBlock#cutFor}）。
 * 已经贴了的窗户不能再贴，先用剪刀揭下来。
 */
public class PaperCutItem extends Item {

    public PaperCutItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof PaperLatticeWindowBlock)
                || state.getValue(PaperLatticeWindowBlock.PAPER_CUT) != PaperLatticeWindowBlock.PaperCut.NONE) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            PaperLatticeWindowBlock.PaperCut cut = PaperLatticeWindowBlock.cutFor(state, pos, context.getClickLocation());
            level.setBlock(pos, state.setValue(PaperLatticeWindowBlock.PAPER_CUT, cut), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.PAINTING_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
            Player player = context.getPlayer();
            if (player == null || !player.getAbilities().instabuild) {
                context.getItemInHand().shrink(1);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
