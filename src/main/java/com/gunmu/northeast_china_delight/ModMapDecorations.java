package com.gunmu.northeast_china_delight;

import com.gunmu.northeast_china_delight.util.DdIds;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
//? if >=1.20.5 {
import net.minecraft.core.Holder;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;
//?}
//? if <1.20.2 {
/*import net.minecraftforge.eventbus.api.IEventBus;
*///?} else {
import net.neoforged.bus.api.IEventBus;
//?}
//? if <1.20.2 {
/*import net.minecraftforge.registries.DeferredRegister;
*///?} else {
import net.neoforged.neoforge.registries.DeferredRegister;
//?}
import java.util.function.Supplier;

/**
 * 自定义地图标识：制图师卖的「小院地图」上标目的地用的图标。
 *
 * <p>贴图放在 {@code assets/northeast_china_delight/textures/map/decorations/northeast_yard.png}（8×8）。</p>
 *
 * <p>1.20.4 没有 {@code MapDecorationType} 注册表（1.20.5 才加），只能退回原版
 * {@code MapDecoration.Type} 枚举，这里用红叉（原版探险家地图同款图标）。</p>
 */
public final class ModMapDecorations
{
    //? if <1.20.5 {
    /*// 1.20.4：没有自定义地图标识的注册表，退回原版红叉
    public static final MapDecoration.Type DONGBEI_YARD = MapDecoration.Type.TARGET_X;
    *///?} else {
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
    public static final Supplier<MapDecorationType> DONGBEI_YARD =
            DECORATION_TYPES.register("northeast_yard", () -> new MapDecorationType(
                    DdIds.of(NortheastChinaDelight.MODID, "northeast_yard"),
                    false, -1, true, true));
    //?}

    private ModMapDecorations()
    {
    }

    /**
     * 小院图标的 Holder（1.21 的 {@code addTargetDecoration} 要 Holder）。
     *
     * <p>字段本身声明成 Supplier 是为了 <1.20.2 也能共用（Forge 的 RegistryObject 只是 Supplier），
     * 而 NeoForge 的 DeferredHolder 两种身份都有，所以这里直接转一下即可。</p>
     */
    //? if >=1.20.5 {
    @SuppressWarnings("unchecked")
    public static Holder<MapDecorationType> dongbeiYardHolder()
    {
        return (Holder<MapDecorationType>) DONGBEI_YARD;
    }
    //?}

    public static void register(IEventBus eventBus)
    {
        //? if >=1.20.5 {
        DECORATION_TYPES.register(eventBus);
        //?}
    }
}
