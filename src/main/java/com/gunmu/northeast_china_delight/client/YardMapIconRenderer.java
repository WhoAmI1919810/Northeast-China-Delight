package com.gunmu.northeast_china_delight.client;

import com.gunmu.northeast_china_delight.NortheastChinaDelight;
import com.gunmu.northeast_china_delight.util.DdIds;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import org.joml.Matrix4f;

import java.util.List;

/**
 * 1.20.1 客户端：在地图画布上绘制东北小院专属小屋图标（{@code northeast_yard.png}），
 * 视觉表现与 1.21.1 的数据驱动地图装饰对齐。
 */
public final class YardMapIconRenderer
{
    private static final ResourceLocation YARD_ICON_TEXTURE =
            DdIds.of(NortheastChinaDelight.MODID, "textures/map/decorations/northeast_yard.png");
    private static final RenderType YARD_ICON_RENDER_TYPE = RenderType.text(YARD_ICON_TEXTURE);

    private YardMapIconRenderer()
    {
    }

    //? if <1.20.5 {
    /*public static void render(PoseStack poseStack, MultiBufferSource bufferSource,
                              List<MapDecoration> decorations, boolean active, int packedLight, int startIndex)
    {
        int index = startIndex;
        for (MapDecoration dec : decorations)
        {
            if (active && !dec.renderOnFrame())
            {
                continue;
            }
            poseStack.pushPose();
            poseStack.translate((float) dec.getX() / 2.0F + 64.0F, (float) dec.getY() / 2.0F + 64.0F, -0.02F);
            poseStack.scale(4.0F, 4.0F, 3.0F);
            poseStack.translate(0.125F, -0.125F, 0.0F);
            Matrix4f matrix = poseStack.last().pose();
            float z = (float) index * -0.001F;
            VertexConsumer consumer = bufferSource.getBuffer(YARD_ICON_RENDER_TYPE);
            consumer.vertex(matrix, -1.0F, 1.0F, z).color(255, 255, 255, 255).uv(0.0F, 1.0F).uv2(packedLight).endVertex();
            consumer.vertex(matrix, 1.0F, 1.0F, z).color(255, 255, 255, 255).uv(1.0F, 1.0F).uv2(packedLight).endVertex();
            consumer.vertex(matrix, 1.0F, -1.0F, z).color(255, 255, 255, 255).uv(1.0F, 0.0F).uv2(packedLight).endVertex();
            consumer.vertex(matrix, -1.0F, -1.0F, z).color(255, 255, 255, 255).uv(0.0F, 0.0F).uv2(packedLight).endVertex();
            poseStack.popPose();
            index++;
        }
    }
    *///?}
}
