package com.gunmu.northeast_china_delight.util;

import net.minecraft.world.entity.player.Player;

/**
 * 玩家相关的小工具，抹平 1.20.4 与 1.21.1 的 API 差异。
 */
public final class DdPlayers {

    private DdPlayers() {
    }

    /** 「创造模式那一类不会消耗东西」的玩家（1.21 起的 {@code hasInfiniteMaterials}）。 */
    public static boolean hasInfiniteMaterials(Player player) {
        //? if <1.20.5 {
        /*return player.isCreative();*/
        //?} else {
        return player.hasInfiniteMaterials();
        //?}
    }
}
