package com.gunmu.northeast_china_delight.block;

import com.gunmu.northeast_china_delight.entity.QuiltSleepData;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 被子占用信息：记录是否已被选中，以及选中它的玩家 UUID。
 */
public class FoldedQuiltBlockEntity extends BlockEntity
{
    private static final String TAG_SELECTED = "Selected";
    private static final String TAG_SELECTED_BY = "SelectedBy";
    private static final int OWNER_CHECK_INTERVAL = 20;

    private boolean selected;
    @Nullable
    private UUID selectedBy;

    public FoldedQuiltBlockEntity(BlockPos pos, BlockState state)
    {
        super(ModBlockEntities.FOLDED_QUILT.get(), pos, state);
    }

    public boolean isSelected()
    {
        return this.selected && this.selectedBy != null;
    }

    @Nullable
    public UUID selectedBy()
    {
        return this.selectedBy;
    }

    public boolean isSelectedBy(UUID playerId)
    {
        return isSelected() && this.selectedBy.equals(playerId);
    }

    public boolean select(UUID playerId)
    {
        if (isSelected())
        {
            return false;
        }
        this.selected = true;
        this.selectedBy = playerId;
        setChanged();
        return true;
    }

    public void clearSelection()
    {
        if (!this.selected && this.selectedBy == null)
        {
            return;
        }
        this.selected = false;
        this.selectedBy = null;
        setChanged();
    }

    public void serverTick(ServerLevel level, BlockPos pos, BlockState state)
    {
        if (!FoldedQuiltBlock.isOccupied(state) || Math.floorMod(level.getGameTime() + pos.asLong(), OWNER_CHECK_INTERVAL) != 0)
        {
            return;
        }
        UUID ownerId = this.selectedBy;
        if (ownerId == null)
        {
            FoldedQuiltBlock.clearStaleOccupation(level, pos);
            return;
        }
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(ownerId);
        boolean stillUsed = owner != null && owner.isSleeping()
                && ((QuiltSleepData) owner).ncd$getQuiltPos().filter(pos::equals).isPresent();
        if (!stillUsed)
        {
            FoldedQuiltBlock.release(level, pos, ownerId);
        }
    }

    @Override
    public void setBlockState(BlockState state)
    {
        super.setBlockState(state);
        if (state.hasProperty(FoldedQuiltBlock.OCCUPIED) && !state.getValue(FoldedQuiltBlock.OCCUPIED))
        {
            clearSelection();
        }
    }

    @Override
    //? if <1.20.5 {
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        writeSelection(tag);
    }
    //?} else {
    /*protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        writeSelection(tag);
    }
    *///?}

    @Override
    //? if <1.20.5 {
    public void load(CompoundTag tag) {
        super.load(tag);
        readSelection(tag);
    }
    //?} else {
    /*protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        readSelection(tag);
    }
    *///?}

    private void writeSelection(CompoundTag tag)
    {
        tag.putBoolean(TAG_SELECTED, this.selected);
        if (this.selectedBy != null)
        {
            tag.putString(TAG_SELECTED_BY, this.selectedBy.toString());
        }
    }

    private void readSelection(CompoundTag tag)
    {
        this.selected = tag.getBoolean(TAG_SELECTED);
        this.selectedBy = null;
        if (tag.contains(TAG_SELECTED_BY))
        {
            try
            {
                this.selectedBy = UUID.fromString(tag.getString(TAG_SELECTED_BY));
            }
            catch (IllegalArgumentException ignored)
            {
                this.selected = false;
            }
        }
        if (this.selectedBy == null)
        {
            this.selected = false;
        }
    }
}
