package com.gunmu.northeast_china_delight;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * 模组配置。
 *
 * <p>目前只有一个开关：**菜肴能不能摆成方块**（右键吃、潜行右键摆）。
 * 万一方块建模的观感不尽如人意，把这个开关关掉即可——关掉之后潜行右键不会放置，
 * 也不会有任何文字提示（玩家感觉不到这个功能存在）。</p>
 */
public final class NortheastChinaConfig
{
    public static final ModConfigSpec SPEC;

    /** 菜肴方块形态总开关 */
    public static final ModConfigSpec.BooleanValue DISH_PLACEMENT_ENABLED;

    static
    {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.comment("菜肴方块形态").push("dishes");
        DISH_PLACEMENT_ENABLED = builder
                .comment("允许玩家潜行右键把菜肴摆成方块。",
                        "设为 false 时彻底关闭这个功能：潜行右键不会放置，也不会有任何提示文字。")
                .define("dish_placement_enabled", false);
        builder.pop();
        SPEC = builder.build();
    }

    private NortheastChinaConfig()
    {
    }
}
