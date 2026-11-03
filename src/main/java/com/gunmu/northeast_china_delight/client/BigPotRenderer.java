package com.gunmu.northeast_china_delight.client;

import com.gunmu.northeast_china_delight.block.BigPotBlockEntity;
import com.gunmu.northeast_china_delight.block.BigStoveBlock;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

import java.util.ArrayList;
import java.util.List;

/**
 * 画大铁锅里的东西（锅盖揭着的时候）：
 *
 * <ul>
 *   <li>没煮好：液面（流体颜色，倒进去的调料按份数往调料的颜色染）+ 放进去的食材，按 {@link BigPotVisuals} 的六个格子摆；</li>
 *   <li>煮好了：成品在锅里的模型（{@code block/big_pot/dish/<成品>}，一锅汤铺满锅，菜泡在汤里）；没有就把成品物品平放在锅里。</li>
 * </ul>
 *
 * <p>方块实体在灶西北那一格（朝北时）上，画的时候先挪到整口灶的西北角，再按朝向绕锅心转（和方块状态里模型的 y 转法一样）。
 * 画得出这一格，渲染范围见 {@link BigPotBlockEntity#renderBounds}。</p>
 */
public class BigPotRenderer implements BlockEntityRenderer<BigPotBlockEntity> {

    public BigPotRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(BigPotBlockEntity pot, float partialTick, PoseStack pose, MultiBufferSource buffer,
                       int packedLight, int packedOverlay) {
        Level level = pot.getLevel();
        BlockState state = pot.getBlockState();
        if (level == null || !(state.getBlock() instanceof BigStoveBlock)
                || state.getValue(BigStoveBlock.POT) != BigStoveBlock.Pot.POT || pot.isEmpty()) {
            return;
        }
        BlockPos pos = pot.getBlockPos();
        BlockPos origin = BigStoveBlock.origin(pos, state);
        int light = LevelRenderer.getLightColor(level, origin.offset(1, 1, 1));

        pose.pushPose();
        pose.translate(origin.getX() - pos.getX(), origin.getY() - pos.getY(), origin.getZ() - pos.getZ());
        // 朝向：和方块状态一样（north 0、east 90、south 180、west 270，顺时针），绕锅心转
        pose.translate(1.0F, 0.0F, 1.0F);
        pose.mulPose(Axis.YP.rotationDegrees(-modelYRot(state)));
        pose.translate(-1.0F, 0.0F, -1.0F);

        if (pot.hasResult()) {
            renderDish(pose, buffer, state, pot.result(), light, packedOverlay, level);
        } else {
            List<ItemStack> ingredients = new ArrayList<>();
            for (ItemStack stack : pot.contents()) {
                if (!BigPotVisuals.isSeasoning(stack)) {
                    ingredients.add(stack);
                }
            }
            renderIngredients(pose, buffer, state, ingredients, light, packedOverlay, level);
            if (pot.liquidMb() > 0) {
                renderLiquid(pose, buffer, pot.liquid(), BigPotVisuals.liquidY(pot.liquidMb()),
                        BigPotVisuals.liquidColor(pot.liquid(), pot.contents()), light, packedOverlay);
            }
        }
        pose.popPose();
    }

    private static float modelYRot(BlockState state) {
        return switch (state.getValue(BigStoveBlock.FACING)) {
            case EAST -> 90.0F;
            case SOUTH -> 180.0F;
            case WEST -> 270.0F;
            default -> 0.0F;
        };
    }

    private static void renderDish(PoseStack pose, MultiBufferSource buffer, BlockState state, ItemStack result,
                                   int light, int overlay, Level level) {
        BakedModel model = BigPotVisuals.dishModel(result);
        if (model != null) {
            renderModel(pose, buffer, state, model, light, overlay);
            return;
        }
        // 数据包加的菜没有锅里的模型：把成品平放在水面那么高
        pose.pushPose();
        pose.translate(1.0F, BigPotVisuals.liquidY(1000) / 16.0F + 0.02F, 1.0F);
        pose.mulPose(Axis.XP.rotationDegrees(90.0F));
        pose.scale(0.9F, 0.9F, 0.9F);
        Minecraft.getInstance().getItemRenderer().renderStatic(result, ItemDisplayContext.FIXED, light, overlay,
                pose, buffer, level, 0);
        pose.popPose();
    }

    private static void renderIngredients(PoseStack pose, MultiBufferSource buffer, BlockState state,
                                          List<ItemStack> ingredients, int light, int overlay, Level level) {
        for (int i = 0; i < ingredients.size() && i < BigPotVisuals.SLOTS; i++) {
            ItemStack stack = ingredients.get(i);
            BakedModel model = BigPotVisuals.ingredientModel(stack);
            int copies = Math.min(stack.getCount(), BigPotVisuals.MAX_COPIES);
            for (int k = 0; k < copies; k++) {
                float[] at = BigPotVisuals.slot(i, k);
                pose.pushPose();
                pose.translate(at[0] / 16.0F, at[1] / 16.0F, at[2] / 16.0F);
                pose.mulPose(Axis.YP.rotationDegrees(at[3]));
                if (model != null) {
                    pose.scale(BigPotVisuals.SCALE, BigPotVisuals.SCALE, BigPotVisuals.SCALE);
                    pose.translate(-0.5F, 0.0F, -0.5F);
                    renderModel(pose, buffer, state, model, light, overlay);
                } else {
                    // 没有专门模型的食材（别的模组的）：平躺的物品
                    pose.translate(0.0F, 0.03F, 0.0F);
                    pose.mulPose(Axis.XP.rotationDegrees(90.0F));
                    pose.scale(0.4F, 0.4F, 0.4F);
                    Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, light,
                            overlay, pose, buffer, level, i * 3 + k);
                }
                pose.popPose();
            }
        }
    }

    /** 用实体式的方块图集缓冲画（按面的法线分明暗，同大缸里的展示模型） */
    private static void renderModel(PoseStack pose, MultiBufferSource buffer, BlockState state, BakedModel model,
                                    int light, int overlay) {
        Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(
                pose.last(), buffer.getBuffer(Sheets.cutoutBlockSheet()), state, model, 1.0F, 1.0F, 1.0F,
                light, overlay);
    }

    /** 液面：按锅里这一圈的内半径一行一行铺满（{@link BigPotVisuals#surfaceRows}），半透明 */
    public static void renderLiquid(PoseStack pose, MultiBufferSource buffer, Fluid fluid, float yPx, int argb,
                             int light, int overlay) {
        TextureAtlasSprite sprite = VatRenderer.fluidSprite(fluid);
        if (sprite == null) {
            return;
        }
        float red = ((argb >> 16) & 0xFF) / 255.0F;
        float green = ((argb >> 8) & 0xFF) / 255.0F;
        float blue = (argb & 0xFF) / 255.0F;
        float alpha = ((argb >>> 24) & 0xFF) / 255.0F;
        VertexConsumer consumer = buffer.getBuffer(RenderType.translucent());
        float y = yPx / 16.0F;
        for (int[] row : BigPotVisuals.surfaceRows(yPx)) {
            float z0 = row[0] / 16.0F;
            float z1 = (row[0] + 1) / 16.0F;
            float x0 = row[1] / 16.0F;
            float x1 = row[2] / 16.0F;
            VatRenderer.quad(consumer, pose.last(), light, overlay, red, green, blue, alpha,
                    x0, y, z0, u(sprite, x0), v(sprite, z0),
                    x0, y, z1, u(sprite, x0), v(sprite, z1),
                    x1, y, z1, u(sprite, x1), v(sprite, z1),
                    x1, y, z0, u(sprite, x1), v(sprite, z0),
                    0, 1, 0);
        }
    }

    /** 贴图铺满两格宽的灶面（不用 getU：1.20.1 和 1.21.1 的参数单位不一样） */
    private static float u(TextureAtlasSprite sprite, float blocks) {
        return sprite.getU0() + (sprite.getU1() - sprite.getU0()) * blocks / 2.0F;
    }

    private static float v(TextureAtlasSprite sprite, float blocks) {
        return sprite.getV0() + (sprite.getV1() - sprite.getV0()) * blocks / 2.0F;
    }

    //? if >=1.20.2 {
    /*@Override
    public net.minecraft.world.phys.AABB getRenderBoundingBox(BigPotBlockEntity pot) {
        return pot.renderBounds();
    }
    *///?}
}
