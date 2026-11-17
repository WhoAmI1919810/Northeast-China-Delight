package com.gunmu.northeast_china_delight.client;

import com.gunmu.northeast_china_delight.NortheastChinaDelight;
import com.gunmu.northeast_china_delight.block.ModBlockTags;
import com.gunmu.northeast_china_delight.block.SleepingPillowBlock;
import com.gunmu.northeast_china_delight.entity.QuiltSleepData;
import com.gunmu.northeast_china_delight.util.DdIds;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

/**
 * 玩家用枕头入睡、且附近选中的被子仍有效时，在身体上绘制一床花被。
 */
public class QuiltSleepLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>>
{
    private static final ResourceLocation TOP_TEXTURE =
            DdIds.of(NortheastChinaDelight.MODID, "textures/block/quilt_red.png");
    private static final ResourceLocation SIDE_TEXTURE =
            DdIds.of(NortheastChinaDelight.MODID, "textures/block/quilt_red_side.png");

    private static final float MIN_X = -10.0F;
    private static final float MAX_X = 10.0F;
    private static final float MIN_Y = -3.0F;
    private static final float MAX_Y = 27.0F;
    private static final float TOP_Z = -4.0F;
    private static final float DRAPE_BOTTOM_Z = 10.0F;
    private static final int COLUMNS = 2;
    private static final int ROWS = 4;
    private static final float MODEL_PIXEL = 1.0F / 16.0F;

    public QuiltSleepLayer(PlayerRenderer renderer)
    {
        super(renderer);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch)
    {
        if (!hasVisibleQuilt(player))
        {
            return;
        }

        pose.pushPose();
        pose.scale(MODEL_PIXEL, MODEL_PIXEL, MODEL_PIXEL);
        PoseStack.Pose last = pose.last();
        drawTop(last, buffer.getBuffer(RenderType.entityCutoutNoCull(TOP_TEXTURE)), packedLight);
        VertexConsumer sides = buffer.getBuffer(RenderType.entityCutoutNoCull(SIDE_TEXTURE));
        drawLongSides(last, sides, packedLight);
        drawEnds(last, sides, packedLight);
        pose.popPose();
    }

    private static boolean hasVisibleQuilt(AbstractClientPlayer player)
    {
        if (!player.isSleeping() || player.isInvisible())
        {
            return false;
        }

        BlockPos pillowPos = player.getSleepingPos().orElse(null);
        if (pillowPos == null
                || !(player.level().getBlockState(pillowPos).getBlock() instanceof SleepingPillowBlock))
        {
            return false;
        }

        return ((QuiltSleepData) player).ncd$getQuiltPos()
                .filter(pos -> SleepingPillowBlock.isQuiltInRange(pillowPos, pos))
                .filter(pos -> player.level().getBlockState(pos).is(ModBlockTags.QUILTS))
                .or(() -> SleepingPillowBlock.findNearbyQuilt(player.level(), pillowPos))
                .isPresent();
    }

    private static void drawTop(PoseStack.Pose pose, VertexConsumer consumer, int light)
    {
        float width = (MAX_X - MIN_X) / COLUMNS;
        float length = (MAX_Y - MIN_Y) / ROWS;
        for (int column = 0; column < COLUMNS; column++)
        {
            float x0 = MIN_X + column * width;
            float x1 = x0 + width;
            for (int row = 0; row < ROWS; row++)
            {
                float y0 = MIN_Y + row * length;
                float y1 = y0 + length;
                VatRenderer.quad(consumer, pose, light, OverlayTexture.NO_OVERLAY,
                        1.0F, 1.0F, 1.0F, 1.0F,
                        x0, y0, TOP_Z, 0.0F, 1.0F,
                        x1, y0, TOP_Z, 1.0F, 1.0F,
                        x1, y1, TOP_Z, 1.0F, 0.0F,
                        x0, y1, TOP_Z, 0.0F, 0.0F,
                        0.0F, 0.0F, -1.0F);
            }
        }
    }

    private static void drawLongSides(PoseStack.Pose pose, VertexConsumer consumer, int light)
    {
        float length = (MAX_Y - MIN_Y) / ROWS;
        for (int row = 0; row < ROWS; row++)
        {
            float y0 = MIN_Y + row * length;
            float y1 = y0 + length;
            quad(consumer, pose, light, MIN_X, y0, y1, -1.0F, 0.0F, 0.0F);
            quad(consumer, pose, light, MAX_X, y0, y1, 1.0F, 0.0F, 0.0F);
        }
    }

    private static void quad(VertexConsumer consumer, PoseStack.Pose pose, int light, float x,
                             float y0, float y1, float normalX, float normalY, float normalZ)
    {
        VatRenderer.quad(consumer, pose, light, OverlayTexture.NO_OVERLAY,
                1.0F, 1.0F, 1.0F, 1.0F,
                x, y0, TOP_Z, 0.0F, 1.0F,
                x, y1, TOP_Z, 1.0F, 1.0F,
                x, y1, DRAPE_BOTTOM_Z, 1.0F, 0.0F,
                x, y0, DRAPE_BOTTOM_Z, 0.0F, 0.0F,
                normalX, normalY, normalZ);
    }

    private static void drawEnds(PoseStack.Pose pose, VertexConsumer consumer, int light)
    {
        float width = (MAX_X - MIN_X) / COLUMNS;
        for (int column = 0; column < COLUMNS; column++)
        {
            float x0 = MIN_X + column * width;
            float x1 = x0 + width;
            endQuad(consumer, pose, light, MIN_Y, x0, x1, -1.0F);
            endQuad(consumer, pose, light, MAX_Y, x0, x1, 1.0F);
        }
    }

    private static void endQuad(VertexConsumer consumer, PoseStack.Pose pose, int light, float y,
                                float x0, float x1, float normalY)
    {
        VatRenderer.quad(consumer, pose, light, OverlayTexture.NO_OVERLAY,
                1.0F, 1.0F, 1.0F, 1.0F,
                x0, y, TOP_Z, 0.0F, 1.0F,
                x1, y, TOP_Z, 1.0F, 1.0F,
                x1, y, DRAPE_BOTTOM_Z, 1.0F, 0.0F,
                x0, y, DRAPE_BOTTOM_Z, 0.0F, 0.0F,
                0.0F, normalY, 0.0F);
    }
}
