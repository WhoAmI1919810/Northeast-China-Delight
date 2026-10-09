package com.gunmu.northeast_china_delight.util;

import net.minecraft.resources.ResourceLocation;

/**
 * 版本无关的 ResourceLocation 工厂。
 *
 * <p>1.20.4 的 ResourceLocation 只能通过构造器创建（{@code new ResourceLocation(...)}），
 * 1.20.5 起 Mojang 把构造器收成私有、改用静态工厂（{@code fromNamespaceAndPath} / {@code parse}）。
 * 全工程统一从这里取 id，版本差异只保留在本类里。
 */
public final class DdIds {

    private DdIds() {
    }

    /** 等价于 1.21+ 的 ResourceLocation.fromNamespaceAndPath。 */
    public static ResourceLocation of(String namespace, String path) {
        //? if <1.20.5 {
        return new ResourceLocation(namespace, path);
        //?} else {
        /*return ResourceLocation.fromNamespaceAndPath(namespace, path);
        *///?}
    }

    /** 等价于 1.21+ 的 ResourceLocation.parse。 */
    public static ResourceLocation parse(String id) {
        //? if <1.20.5 {
        return new ResourceLocation(id);
        //?} else {
        /*return ResourceLocation.parse(id);
        *///?}
    }
}
