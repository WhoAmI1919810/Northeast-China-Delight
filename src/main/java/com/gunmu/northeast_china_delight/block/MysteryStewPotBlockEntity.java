package com.gunmu.northeast_china_delight.block;

import com.gunmu.northeast_china_delight.item.MysteryStewEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * 放下的盆装谜之炖菜：保存这一盆给予的状态效果，方块形态与物品形态共用同一份数据。
 * 放下时从物品上抄过来（{@link MysteryStewPotBlock#setPlacedBy}），拿碗盛一碗就照这个给一碗原版谜之炖菜，
 * 满盆敲下来掉的盆装谜之炖菜也带着它（{@link MysteryStewPotBlock#getDrops}）。客户端用不着，不同步。
 */
public class MysteryStewPotBlockEntity extends BlockEntity {

    private List<MysteryStewEffects.Entry> effects = List.of();

    public MysteryStewPotBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MYSTERY_STEW_POT.get(), pos, state);
    }

    public List<MysteryStewEffects.Entry> effects() {
        return effects;
    }

    public void setEffects(List<MysteryStewEffects.Entry> effects) {
        this.effects = List.copyOf(effects);
        setChanged();
    }

    @Override
    //? if <1.20.5 {
    /*protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Effects", MysteryStewEffects.save(effects));
    }*/
    //?} else {
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Effects", MysteryStewEffects.save(effects));
    }
    //?}

    @Override
    //? if <1.20.5 {
    /*public void load(CompoundTag tag) {
        super.load(tag);
        effects = MysteryStewEffects.load(tag.getList("Effects", Tag.TAG_COMPOUND));
    }*/
    //?} else {
    protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        effects = MysteryStewEffects.load(tag.getList("Effects", Tag.TAG_COMPOUND));
    }
    //?}
}
