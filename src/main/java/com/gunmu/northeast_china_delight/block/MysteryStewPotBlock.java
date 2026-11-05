package com.gunmu.northeast_china_delight.block;

import com.gunmu.northeast_china_delight.item.ModItems;
import com.gunmu.northeast_china_delight.item.MysteryStewEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 盆装谜之炖菜放下来的那一盆（2026-11-05 用户）：和别的盆装菜一样拿碗盛、能盛 4 碗、盛完剩个空大脸盆（{@link DdFeastBlock}），
 * 只是盛出来的是一碗原版的谜之炖菜，效果照这一盆存的给（{@link MysteryStewPotBlockEntity}）。
 *
 * <p>不重写 codec()，理由同 {@link AnimalOilBlock}：父类 {@link DdFeastBlock} 的 codec() 写死了自己的类型。</p>
 */
public class MysteryStewPotBlock extends DdFeastBlock implements EntityBlock {

    public MysteryStewPotBlock(BlockBehaviour.Properties properties) {
        super(properties, () -> Items.SUSPICIOUS_STEW);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MysteryStewPotBlockEntity(pos, state);
    }

    /** 盛一碗：原版谜之炖菜，带上这一盆存的效果 */
    @Override
    protected ItemStack servingStack(Level level, BlockPos pos) {
        return MysteryStewEffects.bowl(level.getBlockEntity(pos) instanceof MysteryStewPotBlockEntity pot
                ? pot.effects() : List.of());
    }

    /** 放下时把物品上的效果抄进方块实体 */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.getBlockEntity(pos) instanceof MysteryStewPotBlockEntity pot) {
            pot.setEffects(MysteryStewEffects.read(stack));
        }
    }

    /**
     * 掉落照战利品表（满盆掉一盆谜之炖菜，吃过的掉空大脸盆，同别的盆装菜）；掉出来的那盆谜之炖菜补上这一盆存的效果。
     * 1.21.1 原版这个方法是 protected，这里写成 public 两个版本都能用
     */
    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> drops = super.getDrops(state, params);
        if (params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof MysteryStewPotBlockEntity pot) {
            for (ItemStack stack : drops) {
                if (stack.is(ModItems.MYSTERY_STEW.get())) {
                    MysteryStewEffects.write(stack, pot.effects());
                }
            }
        }
        return drops;
    }
}
