package com.gunmu.northeast_china_delight.block;

import com.gunmu.northeast_china_delight.crafting.PoundingRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 木臼的方块实体：记住装进去的是哪条舂捣配方、每种输入已投入多少、已经捣了几次，
 * 以及最后一次敲击时已经掷好的产物。
 */
public class WoodenMortarBlockEntity extends BlockEntity {

    @Nullable
    private ResourceLocation recipeId;
    private int progress;
    private final List<ItemStack> depositedInputs = new ArrayList<>();

    /** null 表示还没到成品；空列表表示已经掷过并且这次什么都没出。 */
    @Nullable
    private List<ItemStack> pendingResults;

    /** 搁在臼上的那把木槌（连耐久、附魔一起存下来），空手右键取回 */
    private ItemStack mallet = ItemStack.EMPTY;

    public WoodenMortarBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WOODEN_MORTAR.get(), pos, state);
    }

    @Nullable
    public ResourceLocation recipeId() {
        return this.recipeId;
    }

    public int progress() {
        return this.progress;
    }

    public ItemStack mallet() {
        return this.mallet;
    }

    public boolean hasMallet() {
        return !this.mallet.isEmpty();
    }

    public void setMallet(ItemStack stack) {
        this.mallet = stack.copy();
        setChanged();
        sync();
    }

    public ItemStack takeMallet() {
        ItemStack taken = this.mallet;
        this.mallet = ItemStack.EMPTY;
        setChanged();
        sync();
        return taken;
    }

    /** 第一次装料：绑定配方，并按配方输入种类数准备槽位。 */
    public void beginFill(ResourceLocation id, int inputSlots) {
        this.recipeId = id;
        this.progress = 0;
        this.pendingResults = null;
        this.depositedInputs.clear();
        for (int i = 0; i < inputSlots; i++) {
            this.depositedInputs.add(ItemStack.EMPTY);
        }
        setChanged();
        sync();
    }

    public int inputCount(int slot) {
        return slot < 0 || slot >= this.depositedInputs.size() ? 0 : this.depositedInputs.get(slot).getCount();
    }

    public void addInput(int slot, ItemStack stack) {
        if (stack.isEmpty() || slot < 0) {
            return;
        }
        while (this.depositedInputs.size() <= slot) {
            this.depositedInputs.add(ItemStack.EMPTY);
        }
        ItemStack current = this.depositedInputs.get(slot);
        if (current.isEmpty()) {
            this.depositedInputs.set(slot, stack.copy());
        } else {
            current.grow(stack.getCount());
        }
        setChanged();
        sync();
    }

    public boolean hasDepositedInputs() {
        for (ItemStack stack : this.depositedInputs) {
            if (!stack.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public List<ItemStack> takeDepositedInputs() {
        List<ItemStack> taken = new ArrayList<>();
        for (ItemStack stack : this.depositedInputs) {
            if (!stack.isEmpty()) {
                taken.add(stack.copy());
            }
        }
        this.depositedInputs.clear();
        setChanged();
        sync();
        return List.copyOf(taken);
    }

    public boolean inputsSatisfied(PoundingRecipes.Recipe recipe) {
        for (int i = 0; i < recipe.inputs().size(); i++) {
            if (inputCount(i) < recipe.inputs().get(i).count()) {
                return false;
            }
        }
        return true;
    }

    public void poundOnce() {
        this.progress++;
        setChanged();
        sync();
    }

    public void setPendingResults(List<ItemStack> results) {
        List<ItemStack> copy = new ArrayList<>();
        for (ItemStack stack : results) {
            if (!stack.isEmpty()) {
                copy.add(stack.copy());
            }
        }
        this.pendingResults = List.copyOf(copy);
        setChanged();
        sync();
    }

    public boolean hasPendingResults() {
        return this.pendingResults != null;
    }

    public List<ItemStack> pendingResults() {
        return this.pendingResults == null ? List.of() : this.pendingResults;
    }

    @Nullable
    public PoundingRecipes.Recipe resolve(Level level) {
        if (this.recipeId == null) {
            return null;
        }
        RecipeManager manager = level.getRecipeManager();
        return PoundingRecipes.byId(manager, this.recipeId);
    }

    /** 取出成品或取消装料后清空。 */
    public void clear() {
        this.recipeId = null;
        this.progress = 0;
        this.depositedInputs.clear();
        this.pendingResults = null;
        setChanged();
        sync();
    }

    private void sync() {
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    //? if <1.20.5 {
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        this.writeToTag(tag);
    }
    //?} else {
    /*protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        this.writeToTag(tag);
    }
    *///?}

    private void writeToTag(CompoundTag tag) {
        if (this.recipeId != null) {
            tag.putString("PoundRecipe", this.recipeId.toString());
            tag.putInt("PoundProgress", this.progress);
        }

        ListTag inputs = new ListTag();
        for (int i = 0; i < this.depositedInputs.size(); i++) {
            ItemStack stack = this.depositedInputs.get(i);
            if (stack.isEmpty()) {
                continue;
            }
            int slot = i;
            ItemStack.CODEC.encodeStart(NbtOps.INSTANCE, stack).result().ifPresent(saved -> {
                CompoundTag entry = new CompoundTag();
                entry.putInt("Slot", slot);
                entry.put("Item", saved);
                inputs.add(entry);
            });
        }
        tag.put("PoundInputs", inputs);

        if (this.pendingResults != null) {
            tag.putBoolean("PoundResultsRolled", true);
            ListTag results = new ListTag();
            for (ItemStack stack : this.pendingResults) {
                ItemStack.CODEC.encodeStart(NbtOps.INSTANCE, stack).result().ifPresent(saved -> {
                    CompoundTag entry = new CompoundTag();
                    entry.put("Item", saved);
                    results.add(entry);
                });
            }
            tag.put("PoundResults", results);
        }

        if (!this.mallet.isEmpty()) {
            ItemStack.CODEC.encodeStart(NbtOps.INSTANCE, this.mallet)
                    .result().ifPresent(saved -> tag.put("Mallet", saved));
        }
    }

    @Override
    //? if <1.20.5 {
    public void load(CompoundTag tag) {
        super.load(tag);
        this.readFromTag(tag);
    }
    //?} else {
    /*protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.readFromTag(tag);
    }
    *///?}

    private void readFromTag(CompoundTag tag) {
        this.recipeId = tag.contains("PoundRecipe") ? ResourceLocation.tryParse(tag.getString("PoundRecipe")) : null;
        this.progress = tag.getInt("PoundProgress");

        this.depositedInputs.clear();
        ListTag inputs = tag.getList("PoundInputs", Tag.TAG_COMPOUND);
        int slots = 0;
        for (int i = 0; i < inputs.size(); i++) {
            slots = Math.max(slots, inputs.getCompound(i).getInt("Slot") + 1);
        }
        for (int i = 0; i < slots; i++) {
            this.depositedInputs.add(ItemStack.EMPTY);
        }
        for (int i = 0; i < inputs.size(); i++) {
            CompoundTag entry = inputs.getCompound(i);
            int slot = entry.getInt("Slot");
            if (slot < 0 || slot >= this.depositedInputs.size()) {
                continue;
            }
            ItemStack stack = ItemStack.CODEC.parse(NbtOps.INSTANCE, entry.get("Item"))
                    .result().orElse(ItemStack.EMPTY);
            this.depositedInputs.set(slot, stack);
        }

        this.pendingResults = null;
        if (tag.getBoolean("PoundResultsRolled")) {
            List<ItemStack> results = new ArrayList<>();
            ListTag resultTags = tag.getList("PoundResults", Tag.TAG_COMPOUND);
            for (int i = 0; i < resultTags.size(); i++) {
                ItemStack stack = ItemStack.CODEC.parse(NbtOps.INSTANCE, resultTags.getCompound(i).get("Item"))
                        .result().orElse(ItemStack.EMPTY);
                if (!stack.isEmpty()) {
                    results.add(stack);
                }
            }
            this.pendingResults = List.copyOf(results);
        }

        Tag malletTag = tag.get("Mallet");
        this.mallet = malletTag == null ? ItemStack.EMPTY
                : ItemStack.CODEC.parse(NbtOps.INSTANCE, malletTag).result().orElse(ItemStack.EMPTY);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    //? if <1.20.5 {
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        this.writeToTag(tag);
        return tag;
    }
    //?} else {
    /*public CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        this.writeToTag(tag);
        return tag;
    }
    *///?}
}
