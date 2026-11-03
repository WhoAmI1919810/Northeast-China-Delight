package com.gunmu.northeast_china_delight.block;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 大铁锅里装的东西。只挂在大灶 nw 那一格上（{@link BigStoveBlock#newBlockEntity}），四格共用这一份。
 *
 * <p>现在还没有烹饪功能：先把以后要用的数据存下来、存档读档不丢，功能的入口见 {@link BigPotCooking}。
 * 现在就加上方块实体，是为了以后加功能时，老存档里已经拼好的大灶不用补方块实体。</p>
 *
 * <ul>
 *   <li>{@code water}：锅里的水（mB），以后右键倒水桶 / 水盆进来；</li>
 *   <li>{@code ingredients}：放进去的调料、食材（最多 {@link #MAX_INGREDIENTS} 份，按放进去的先后）；</li>
 *   <li>{@code cookTicks}：点着火、盖着盖已经煮了多久；</li>
 *   <li>{@code result} / {@code servings}：煮好的菜和还剩几份，以后拿大脸盆盛。</li>
 * </ul>
 */
public class BigPotBlockEntity extends BlockEntity {

    public static final int MAX_INGREDIENTS = 9;

    private int waterMb;
    private final List<ItemStack> ingredients = new ArrayList<>();
    private int cookTicks;
    private ItemStack result = ItemStack.EMPTY;
    private int servings;

    public BigPotBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BIG_POT.get(), pos, state);
    }

    public int waterMb() {
        return waterMb;
    }

    public List<ItemStack> ingredients() {
        return Collections.unmodifiableList(ingredients);
    }

    public int cookTicks() {
        return cookTicks;
    }

    public ItemStack result() {
        return result;
    }

    public int servings() {
        return servings;
    }

    /** 锅里什么都没有（没水、没料、没有煮好的菜） */
    public boolean isEmpty() {
        return waterMb <= 0 && ingredients.isEmpty() && (result.isEmpty() || servings <= 0);
    }

    /** 灶被拆时把锅里的料和没盛完的菜掉在 pos（水就没了） */
    public void dropContents(Level level, BlockPos pos) {
        for (ItemStack stack : ingredients) {
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, stack.copy());
        }
        for (int i = 0; i < servings && !result.isEmpty(); i++) {
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, result.copyWithCount(1));
        }
        waterMb = 0;
        ingredients.clear();
        cookTicks = 0;
        result = ItemStack.EMPTY;
        servings = 0;
        setChanged();
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
        tag.putInt("Water", waterMb);
        ListTag list = new ListTag();
        for (ItemStack stack : ingredients) {
            ItemStack.CODEC.encodeStart(NbtOps.INSTANCE, stack).result().ifPresent(list::add);
        }
        tag.put("Ingredients", list);
        tag.putInt("CookTicks", cookTicks);
        if (!result.isEmpty()) {
            ItemStack.CODEC.encodeStart(NbtOps.INSTANCE, result).result().ifPresent(saved -> tag.put("Result", saved));
        }
        tag.putInt("Servings", servings);
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
        waterMb = tag.getInt("Water");
        ingredients.clear();
        ListTag list = tag.getList("Ingredients", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            ItemStack.CODEC.parse(NbtOps.INSTANCE, list.get(i)).result().ifPresent(ingredients::add);
        }
        cookTicks = tag.getInt("CookTicks");
        result = tag.contains("Result")
                ? ItemStack.CODEC.parse(NbtOps.INSTANCE, tag.get("Result")).result().orElse(ItemStack.EMPTY)
                : ItemStack.EMPTY;
        servings = tag.getInt("Servings");
    }
}
