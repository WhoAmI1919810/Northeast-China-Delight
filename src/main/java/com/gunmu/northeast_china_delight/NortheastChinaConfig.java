package com.gunmu.northeast_china_delight;

//? if <1.20.2 {
import net.minecraftforge.common.ForgeConfigSpec;
//?} else {
/*import net.neoforged.neoforge.common.ModConfigSpec;
*///?}

/**
 * 模组配置。
 *
 * <p>目前只有一个开关：菜肴能不能摆成方块（右键吃、潜行右键摆）。
 * 万一方块建模的观感不尽如人意，把这个开关关掉即可——关掉之后潜行右键不会放置，
 * 也不会有任何文字提示（玩家感觉不到这个功能存在）。</p>
 */
public final class NortheastChinaConfig
{
    //? if <1.20.2 {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue DISH_PLACEMENT_ENABLED;

    public static final ForgeConfigSpec.BooleanValue YARD_PARTS_ENABLED;
    //?} else {
    /*public static final ModConfigSpec SPEC;

    /^* 菜肴方块形态总开关 ^/
    public static final ModConfigSpec.BooleanValue DISH_PLACEMENT_ENABLED;

    /^* 东北小院里的小零件（灶棚、鸡架、水井、菜窖等）是否生成 ^/
    public static final ModConfigSpec.BooleanValue YARD_PARTS_ENABLED;
    *///?}

    static
    {
        //? if <1.20.2 {
        ForgeConfigSpec.Builder
        //?} else {
        /*ModConfigSpec.Builder
        *///?}
        builder = configBuilder();
        builder.comment("菜肴方块形态").push("dishes");
        DISH_PLACEMENT_ENABLED = builder
                .comment("允许玩家潜行右键把菜肴摆成方块。",
                        "设为 false 时彻底关闭这个功能：潜行右键不会放置，也不会有任何提示文字。")
                .define("dish_placement_enabled", false);
        builder.pop();

        builder.comment("世界生成").push("worldgen");
        YARD_PARTS_ENABLED = builder
                .comment("东北小院里的小建筑（灶棚、鸡架、水井、菜窖、柴垛等）是否生成。",
                        "设为 false 时只生成院子主体：空地、院墙、正房、门楼，不生成零件。")
                .define("yard_parts_enabled", false);
        builder.pop();
        SPEC = builder.build();
    }

    /** 两个平台的 Builder 方法名完全一样，只有类型名不同，所以在这里分版本取。 */
    //? if <1.20.2 {
    private static ForgeConfigSpec.Builder configBuilder() {
        return new ForgeConfigSpec.Builder();
    }
    //?} else {
    /*private static ModConfigSpec.Builder configBuilder() {
        return new ModConfigSpec.Builder();
    }
    *///?}

    private NortheastChinaConfig()
    {
    }
}
