package com.gunmu.northeast_china_delight.util;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;

/**
 * 存档 API 的收敛层，用来抹平 1.20.1 与 1.21.1 的差异。 1.20.5 起 {@code ContainerHelper} /
 * {@code ItemStack} 的存取都多了一个 {@code HolderLookup.Provider}（数据包注册表）参数，1.20.1 没有这一层。本类统一成同一种
 * 调用形式：1.20.1 分支把这个参数直接丢掉。
 */
public final class DdNbt {

    private DdNbt() {
    }

    public static void saveAllItems(CompoundTag tag, NonNullList<ItemStack> list, boolean includeEmpty,
                                   HolderLookup.Provider registries) {
        //? if <1.20.5 {
        /*ContainerHelper.saveAllItems(tag, list, includeEmpty);*/
        //?} else {
        ContainerHelper.saveAllItems(tag, list, includeEmpty, registries);
        //?}
    }

    /** 从 tag 里读回物品列表（与 1.21 一样：只覆盖 NBT 里存在的槽位） */
    public static void loadAllItems(CompoundTag tag, NonNullList<ItemStack> list,
                                   HolderLookup.Provider registries) {
        //? if <1.20.5 {
        /*ContainerHelper.loadAllItems(tag, list);*/
        //?} else {
        ContainerHelper.loadAllItems(tag, list, registries);
        //?}
    }

    public static ItemStack parseItem(CompoundTag tag, HolderLookup.Provider registries) {
        //? if <1.20.5 {
        /*return ItemStack.of(tag);*/
        //?} else {
        return ItemStack.parse(registries, tag).orElse(ItemStack.EMPTY);
        //?}
    }

    public static Tag saveItem(ItemStack stack, HolderLookup.Provider registries) {
        //? if <1.20.5 {
        /*return stack.save(new CompoundTag());*/
        //?} else {
        return stack.save(registries);
        //?}
    }
}
