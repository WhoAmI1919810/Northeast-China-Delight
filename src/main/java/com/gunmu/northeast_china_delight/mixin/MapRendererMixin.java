package com.gunmu.northeast_china_delight.mixin;

import net.minecraft.client.gui.MapRenderer;
//? if <1.20.5 {
/*import com.gunmu.northeast_china_delight.client.YardMapIconRenderer;
import com.gunmu.northeast_china_delight.villager.YardMapIconMarker;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
*///?}
import org.spongepowered.asm.mixin.Mixin;

/**
 * 1.20.1 客户端地图渲染钩子：
 * 拦截顶层 {@link MapRenderer#render}，在原版绘制期间隐藏东北小院的红叉占位标记，
 * 随后改画 {@code textures/map/decorations/northeast_yard.png} 专属小屋图标。
 */
@Mixin(MapRenderer.class)
public abstract class MapRendererMixin
{
    //? if <1.20.5 {
    /*@Unique
    private List<MapDecoration> ncd$pendingYardIcons;

    @Inject(method = "render", at = @At("HEAD"))
    private void ncd$beforeRender(PoseStack poseStack, MultiBufferSource bufferSource, int mapId,
                                  MapItemSavedData data, boolean active, int packedLight, CallbackInfo ci)
    {
        List<MapDecoration> found = null;
        for (MapDecoration dec : data.getDecorations())
        {
            if (YardMapIconMarker.isYardDecoration(dec))
            {
                if (found == null)
                {
                    found = new ArrayList<>(1);
                }
                found.add(dec);
            }
        }
        this.ncd$pendingYardIcons = found;
        if (found != null)
        {
            YardMapIconMarker.setSuppressVanillaDraw(true);
        }
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void ncd$afterRender(PoseStack poseStack, MultiBufferSource bufferSource, int mapId,
                                 MapItemSavedData data, boolean active, int packedLight, CallbackInfo ci)
    {
        List<MapDecoration> found = this.ncd$pendingYardIcons;
        this.ncd$pendingYardIcons = null;
        if (found == null)
        {
            return;
        }
        YardMapIconMarker.setSuppressVanillaDraw(false);
        int index = 0;
        for (MapDecoration dec : data.getDecorations())
        {
            if (!YardMapIconMarker.isYardDecoration(dec) && (!active || dec.renderOnFrame()))
            {
                index++;
            }
        }
        YardMapIconRenderer.render(poseStack, bufferSource, found, active, packedLight, index);
    }
    *///?}
}
