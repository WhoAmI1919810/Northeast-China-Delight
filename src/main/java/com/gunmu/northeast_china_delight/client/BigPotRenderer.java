package com.gunmu.northeast_china_delight.client;

import com.gunmu.northeast_china_delight.block.BigPotBlockEntity;
import com.gunmu.northeast_china_delight.block.BigStoveBlock;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
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
 * 画大铁锅里的东西。盖着锅盖也照画（被锅盖挡着看不见）：不然一盖一揭锅里的东西就一闪没了又冒出来
 * （2026-11-03 用户说的）；锅里的东西最高都不过锅盖底面。
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
                || state.getValue(BigStoveBlock.POT) == BigStoveBlock.Pot.NONE || pot.isEmpty()) {
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
            int spread = 0;
            for (ItemStack stack : pot.contents()) {
                if (BigPotVisuals.isSpread(stack)) {
                    spread += stack.getCount();
                } else if (!BigPotVisuals.isSeasoning(stack)) {
                    ingredients.add(stack);
                }
            }
            if (spread > 0) {
                renderSpread(pose, buffer, BigPotVisuals.spreadY(spread), light, packedOverlay);
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
        List<ItemStack> pieces = BigPotVisuals.pieces(ingredients);
        for (int i = 0; i < pieces.size(); i++) {
            ItemStack stack = pieces.get(i);
            BakedModel model = BigPotVisuals.ingredientModel(stack);
            float[] at = BigPotVisuals.slot(i);
            pose.pushPose();
            pose.translate(at[0] / 16.0F, at[1] / 16.0F, at[2] / 16.0F);
            pose.mulPose(Axis.YP.rotationDegrees(at[3]));
            if (model != null) {
                pose.translate(-0.5F, 0.0F, -0.5F);
                renderModel(pose, buffer, state, model, light, overlay);
            } else {
                // 没有专门模型的食材（别的模组的）：平躺的物品，0.375 格宽，和圆柱差不多大
                pose.translate(0.0F, 0.03F, 0.0F);
                pose.mulPose(Axis.XP.rotationDegrees(90.0F));
                pose.scale(0.375F, 0.375F, 0.375F);
                Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, light,
                        overlay, pose, buffer, level, i);
            }
            pose.popPose();
        }
    }

    /** 用实体式的方块图集缓冲画（按面的法线分明暗，同大缸里的展示模型） */
    private static void renderModel(PoseStack pose, MultiBufferSource buffer, BlockState state, BakedModel model,
                                    int light, int overlay) {
        Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(
                pose.last(), buffer.getBuffer(Sheets.cutoutBlockSheet()), state, model, 1.0F, 1.0F, 1.0F,
                light, overlay);
    }

    /**
     * 生米在锅底均匀铺一层：只画顶面（侧面贴着锅壁、看不见），按那个高度锅的内半径一行行铺满，
     * 贴图每 16 像素一块（跨两格的行在 x 16 那里断开，各取各那格里的 uv）
     */
    private static void renderSpread(PoseStack pose, MultiBufferSource buffer, float yPx, int light, int overlay) {
        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getTextureAtlas(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS).apply(BigPotVisuals.RAW_RICE);
        VertexConsumer consumer = buffer.getBuffer(Sheets.cutoutBlockSheet());
        float y = yPx / 16.0F;
        for (int[] row : BigPotVisuals.surfaceRows(yPx - 0.01F)) {
            int z = row[0];
            float v0 = sprite.getV0() + (sprite.getV1() - sprite.getV0()) * (z % 16) / 16.0F;
            float v1 = sprite.getV0() + (sprite.getV1() - sprite.getV0()) * (z % 16 + 1) / 16.0F;
            for (int[] seg : new int[][]{{row[1], Math.min(row[2], 16)}, {Math.max(row[1], 16), row[2]}}) {
                if (seg[1] <= seg[0]) {
                    continue;
                }
                float u0 = sprite.getU0() + (sprite.getU1() - sprite.getU0()) * (seg[0] % 16) / 16.0F;
                float u1 = sprite.getU0() + (sprite.getU1() - sprite.getU0()) * ((seg[1] - 1) % 16 + 1) / 16.0F;
                float x0 = seg[0] / 16.0F;
                float x1 = seg[1] / 16.0F;
                VatRenderer.quad(consumer, pose.last(), light, overlay, 1.0F, 1.0F, 1.0F, 1.0F,
                        x0, y, z / 16.0F, u0, v0,
                        x0, y, (z + 1) / 16.0F, u0, v1,
                        x1, y, (z + 1) / 16.0F, u1, v1,
                        x1, y, z / 16.0F, u1, v0,
                        0, 1, 0);
            }
        }
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
        // 用实体半透明的固定缓冲：原版在方块实体之后、半透明地形之后才画它，锅里的食材（cutoutBlockSheet）先画好了，
        // 隔着水能看见。以前用 RenderType.translucent()（方块实体里不是固定缓冲，会被提前画掉、写进深度），
        // 水面下的食材在深度测试里全输给水面，一点都看不见（2026-11-03 用户：水面下的食材显示不出来）
        VertexConsumer consumer = buffer.getBuffer(Sheets.translucentCullBlockSheet());
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
