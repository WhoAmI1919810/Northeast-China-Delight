package com.gunmu.northeast_china_delight.villager;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.level.saveddata.maps.MapDecoration;

/**
 * 1.20.1 小院地图专属图标的标识工具（仅公共类，不含任何客户端渲染引用）。
 *
 * <p>1.20.1 的 {@link MapDecoration.Type} 是封闭枚举，网络包按枚举序号同步。
 * 这里继续借用原版 {@link MapDecoration.Type#TARGET_X} 承载边界钳制与展示框隐藏行为，
 * 通过唯一的装饰键与 {@link #MARKER_COMPONENT} 经原版地图同步包传到客户端，
 * 再由客户端 {@code MapRendererMixin} 替换为 {@code northeast_yard.png} 小屋图标。</p>
 */
public final class YardMapIconMarker
{
    public static final String DECORATION_ID = "northeast_china_delight:northeast_yard";
    public static final String MARKER_TRANSLATION_KEY = "map_decoration.northeast_china_delight.northeast_yard";
    public static final Component MARKER_COMPONENT = Component.translatable(MARKER_TRANSLATION_KEY);

    private static final ThreadLocal<Boolean> SUPPRESS_VANILLA_DRAW = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private YardMapIconMarker()
    {
    }

    public static boolean isSuppressingVanillaDraw()
    {
        return Boolean.TRUE.equals(SUPPRESS_VANILLA_DRAW.get());
    }

    public static void setSuppressVanillaDraw(boolean suppress)
    {
        SUPPRESS_VANILLA_DRAW.set(suppress);
    }

    //? if <1.20.5 {
    public static boolean isYardDecoration(MapDecoration decoration)
    {
        if (decoration == null || decoration.getType() != MapDecoration.Type.TARGET_X)
        {
            return false;
        }
        Component name = decoration.getName();
        return name != null
                && name.getContents() instanceof TranslatableContents contents
                && MARKER_TRANSLATION_KEY.equals(contents.getKey());
    }
    //?}
}
