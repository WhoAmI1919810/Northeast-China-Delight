package com.gunmu.northeast_china_delight;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 自定义地图标识：制图师卖的「小院地图」上标目的地用的图标。
 *
 * <p>贴图放在 {@code assets/northeast_china_delight/textures/map/decorations/northeast_yard.png}（8×8）。</p>
 */
public final class ModMapDecorations
{
    public static final DeferredRegister<MapDecorationType> DECORATION_TYPES =
            DeferredRegister.create(Registries.MAP_DECORATION_TYPE, NortheastChinaDelight.MODID);

    /**
     * 东北小院：红屋顶小屋那一类图标。
     *
     * <p>参数对齐原版 {@code MapDecorationTypes.TARGET_X}：
     * {@code showOnItemFrame=false}（不显示在展示框上）、{@code mapColor=-1}（不染色）、
     * {@code explorationMapElement=true}（跟随地图边缘，像红叉那样指示方向）、
     * {@code trackCount=true}（计入地图标记数）。
     */
    public static final DeferredHolder<MapDecorationType, MapDecorationType> DONGBEI_YARD =
            DECORATION_TYPES.register("northeast_yard", () -> new MapDecorationType(
                    ResourceLocation.fromNamespaceAndPath(NortheastChinaDelight.MODID, "northeast_yard"),
                    false, -1, true, true));

    private ModMapDecorations()
    {
    }

    public static void register(IEventBus eventBus)
    {
        DECORATION_TYPES.register(eventBus);
    }
}
