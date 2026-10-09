package com.gunmu.northeast_china_delight.util;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;

/**
 * 存档 API 的收敛层，用来抹平 1.20.4 与 1.21.1 的差异。
 *
 * <p>1.20.5 起 {@code ContainerHelper} / {@code ItemStack} 的存取都多了一个
 * {@code HolderLookup.Provider}（数据包注册表）参数，1.20.4 没有这一层。
 * 本类统一成同一种调用形式：1.20.4 分支把这个参数直接丢掉。</p>
 */
public final class DdNbt {

    private DdNbt() {
    }

    /** 把一整个物品列表写进 tag */
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

    /** 读单个物品；空 tag / 解析失败都给 {@code ItemStack.EMPTY} */
    public static ItemStack parseItem(CompoundTag tag, HolderLookup.Provider registries) {
        //? if <1.20.5 {
        /*return ItemStack.of(tag);*/
        //?} else {
        return ItemStack.parse(registries, tag).orElse(ItemStack.EMPTY);
        //?}
    }

    /** 写单个物品，返回序列化出来的 tag */
    public static Tag saveItem(ItemStack stack, HolderLookup.Provider registries) {
        //? if <1.20.5 {
        /*return stack.save(new CompoundTag());*/
        //?} else {
        return stack.save(registries);
        //?}
    }
}
