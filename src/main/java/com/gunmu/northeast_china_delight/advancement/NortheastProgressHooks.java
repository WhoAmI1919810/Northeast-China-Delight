package com.gunmu.northeast_china_delight.advancement;

import com.gunmu.chinese_regional_culinary_ingredients.util.CrciProgressHooks;
import net.minecraft.server.level.ServerPlayer;

/** 把东北乐事的进度挂到 CRCI 公共器具的触发点上。 */
public final class NortheastProgressHooks {

    private static boolean registered;

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        CrciProgressHooks.register(new CrciProgressHooks.Listener() {
            @Override
            public void onStove(ServerPlayer player) {
                ModAdvancements.award(player, ModAdvancements.STOKE_STOVE, "stoke");
            }

            @Override
            public void onWashed(ServerPlayer player) {
                ModAdvancements.award(player, ModAdvancements.WASH_FACE, "wash");
            }

            @Override
            public void onMysteryStewServed(ServerPlayer player) {
                ModAdvancements.award(player, ModAdvancements.MYSTERY_STEW_FEAST, "cook");
            }
        });
    }

    private NortheastProgressHooks() {
    }
}
