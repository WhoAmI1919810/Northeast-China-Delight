package com.gunmu.dongbei_delight.block;

import com.gunmu.dongbei_delight.crafting.VatRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * 大缸里的东西：内容物（蔬菜/肉/盐/酱块）、压着的石头、蒙着的羊毛地毯、剩余酱量。
 *
 * 内容物按放入顺序记录，所以「一层肉一层盐」这种层叠关系会被保留下来。
 */
public class VatBlockEntity extends BlockEntity {

    /** 内容物条目上限（5 份蔬菜 + 1 份盐，或 5 块肉 + 5 份盐） */
    public static final int MAX_ENTRIES = 12;

    private final NonNullList<ItemStack> contents = NonNullList.withSize(MAX_ENTRIES, ItemStack.EMPTY);
    /** 压缸用的石头（保持原样，取出时还给玩家） */
    private ItemStack press = ItemStack.EMPTY;
    /** 蒙缸用的羊毛地毯 */
    private ItemStack cover = ItemStack.EMPTY;
    /** 大酱剩余碗数 */
    private int paste;
    /** 酱油剩余瓶数 */
    private int soySauce;
    private VatRecipes.Kind kind = VatRecipes.Kind.NONE;

    public VatBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.VAT.get(), pos, state);
    }

    // ===== 查询 =====

    public VatRecipes.Kind kind() {
        return this.kind;
    }

    public boolean isEmpty() {
        // 只用盐不算「有东西」：盐已经被吸收了，不该挡住加水或继续操作
        return this.kind == VatRecipes.Kind.NONE && this.paste <= 0 && this.soySauce <= 0
                && this.count(stack -> !stack.is(com.gunmu.dongbei_delight.item.ModItems.SALT.get())) == 0;
    }

    /** 把已经用掉的盐清掉（腌制完成后盐就被吸收了） */
    public void consumeSalt() {
        removeAllMatching(stack -> stack.is(com.gunmu.dongbei_delight.item.ModItems.SALT.get()));
    }

    public void removeAllMatching(Predicate<ItemStack> filter) {
        boolean changed = false;
        for (int i = 0; i < MAX_ENTRIES; i++) {
            ItemStack stack = this.contents.get(i);
            if (!stack.isEmpty() && filter.test(stack)) {
                this.contents.set(i, ItemStack.EMPTY);
                changed = true;
            }
        }
        if (changed) {
            this.sync();
        }
    }

    /** 按放入顺序返回内容物 */
    public List<ItemStack> contents() {
        List<ItemStack> list = new ArrayList<>();
        for (ItemStack stack : this.contents) {
            if (!stack.isEmpty()) {
                list.add(stack);
            }
        }
        return list;
    }

    public int count(Predicate<ItemStack> filter) {
        int n = 0;
        for (ItemStack stack : this.contents) {
            if (!stack.isEmpty() && filter.test(stack)) {
                n++;
            }
        }
        return n;
    }

    public int countOf(Item item) {
        return count(stack -> stack.is(item));
    }

    public int vegetableCount() {
        return count(VatRecipes::isPickleIngredient);
    }

    public int meatCount() {
        return countOf(VatRecipes.meatInput());
    }

    public boolean isPressed() {
        return !this.press.isEmpty();
    }

    public ItemStack press() {
        return this.press;
    }

    public boolean isCovered() {
        return !this.cover.isEmpty();
    }

    public ItemStack cover() {
        return this.cover;
    }

    public int paste() {
        return this.paste;
    }

    public int soySauce() {
        return this.soySauce;
    }

    /** 最后放入的一份内容物（用来判断「肉/盐交替」） */
    public ItemStack lastContent() {
        for (int i = MAX_ENTRIES - 1; i >= 0; i--) {
            if (!this.contents.get(i).isEmpty()) {
                return this.contents.get(i);
            }
        }
        return ItemStack.EMPTY;
    }

    // ===== 修改 =====

    public void setKind(VatRecipes.Kind kind) {
        this.kind = kind;
        this.sync();
    }

    public boolean addContent(ItemStack stack) {
        for (int i = 0; i < MAX_ENTRIES; i++) {
            if (this.contents.get(i).isEmpty()) {
                this.contents.set(i, stack.copyWithCount(1));
                this.sync();
                return true;
            }
        }
        return false;
    }

    /** 取出一份符合条件的内容物；返回是否取到 */
    public boolean removeOneMatching(Predicate<ItemStack> filter) {
        for (int i = 0; i < MAX_ENTRIES; i++) {
            ItemStack stack = this.contents.get(i);
            if (!stack.isEmpty() && filter.test(stack)) {
                this.contents.set(i, ItemStack.EMPTY);
                this.sync();
                return true;
            }
        }
        return false;
    }

    public boolean removeOneOf(Item item) {
        return removeOneMatching(stack -> stack.is(item));
    }

    public void setPress(ItemStack stack) {
        this.press = stack.copyWithCount(1);
        this.sync();
    }

    public ItemStack takePress() {
        ItemStack stack = this.press;
        this.press = ItemStack.EMPTY;
        this.sync();
        return stack;
    }

    public void setCover(ItemStack stack) {
        this.cover = stack.copyWithCount(1);
        this.sync();
    }

    public ItemStack takeCover() {
        ItemStack stack = this.cover;
        this.cover = ItemStack.EMPTY;
        this.sync();
        return stack;
    }

    public void setPaste(int servings) {
        this.paste = servings;
        this.sync();
    }

    public int takeOnePaste() {
        if (this.paste > 0) {
            this.paste--;
            this.sync();
        }
        return this.paste;
    }

    public void setSoySauce(int servings) {
        this.soySauce = servings;
        this.sync();
    }

    public int takeOneSoySauce() {
        if (this.soySauce > 0) {
            this.soySauce--;
            this.sync();
        }
        return this.soySauce;
    }

    /** 清空内容物（但不包括压缸石与地毯，它们由调用方单独处理） */
    public void clearContents() {
        // 逐个置空：既避免 clear() 留下 null 槽位，也保证客户端拿到的是一份干净的数据
        for (int i = 0; i < MAX_ENTRIES; i++) {
            this.contents.set(i, ItemStack.EMPTY);
        }
        this.kind = VatRecipes.Kind.NONE;
        this.paste = 0;
        this.soySauce = 0;
        this.sync();
    }

    public void sync() {
        this.setChanged();
        // 显式把该位置的方块实体标记为「需要重发」，客户端才会拿到最新内容
        if (this.level instanceof net.minecraft.server.level.ServerLevel server) {
            server.getChunkSource().blockChanged(this.worldPosition);
            server.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    // ===== 存档与同步 =====

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, this.contents, registries);
        // 注意：空的 ItemStack 不能存档，必须判空
        if (!this.press.isEmpty()) {
            tag.put("press", this.press.save(registries));
        }
        if (!this.cover.isEmpty()) {
            tag.put("cover", this.cover.save(registries));
        }
        tag.putInt("paste", this.paste);
        tag.putInt("soy_sauce", this.soySauce);
        tag.putString("kind", this.kind.name());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        // 关键：loadAllItems 只会覆盖 NBT 里存在的槽位，不会清空原有数据。
        // 不先清空的话，被取走的物品会残留在客户端那份数据里，表现为「取出了但模型不消失」。
        for (int i = 0; i < MAX_ENTRIES; i++) {
            this.contents.set(i, ItemStack.EMPTY);
        }
        ContainerHelper.loadAllItems(tag, this.contents, registries);
        this.press = ItemStack.parse(registries, tag.getCompound("press")).orElse(ItemStack.EMPTY);
        this.cover = ItemStack.parse(registries, tag.getCompound("cover")).orElse(ItemStack.EMPTY);
        this.paste = tag.getInt("paste");
        this.kind = VatRecipes.Kind.valueOf(tag.getString("kind"));
        // 旧存档里没有 soy_sauce 字段：已经在酿酱油的缸按满缸 10 瓶补上，避免卡住取不出来
        this.soySauce = tag.contains("soy_sauce")
                ? tag.getInt("soy_sauce")
                : (this.kind == VatRecipes.Kind.SOY_SAUCE ? VatRecipes.SOY_SAUCE_SERVINGS : 0);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
