package com.gunmu.northeast_china_delight.client;

import com.gunmu.northeast_china_delight.NortheastChinaDelight;
import com.gunmu.northeast_china_delight.block.WoodenMortarBlock;
import com.gunmu.northeast_china_delight.block.WoodenMortarBlockEntity;
import com.gunmu.northeast_china_delight.crafting.PoundingRecipes;
import com.gunmu.northeast_china_delight.util.DdIds;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 按当前配方阶段动态绘制木臼模型。
 *
 * <p>配方 {@code stage_model_mode} 为 overlay 时，阶段模型只有臼里的内容物：先画一只空木臼，
 * 再在同一个朝向上叠画内容物。内容物模型缺失时只画空臼，不退回整只木臼的旧模型（否则会叠出两层臼身）。</p>
 */
public class WoodenMortarRenderer implements BlockEntityRenderer<WoodenMortarBlockEntity> {

    private static final ResourceLocation FALLBACK_MODEL =
            DdIds.of(NortheastChinaDelight.MODID, "block/wooden_mortar_1");
    /** overlay 模式下垫在内容物底下的空木臼 */
    public static final ResourceLocation EMPTY_MORTAR_MODEL =
            DdIds.of(NortheastChinaDelight.MODID, "block/wooden_mortar");

    public WoodenMortarRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(WoodenMortarBlockEntity mortar, float partialTick, PoseStack pose,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        BlockState state = mortar.getBlockState();
        int stage = state.getValue(WoodenMortarBlock.STATE);
        if (stage <= 0 || mortar.getLevel() == null) {
            return;
        }

        PoundingRecipes.Recipe recipe = mortar.resolve(mortar.getLevel());
        boolean overlay = recipe != null && recipe.stageModelMode() == PoundingRecipes.StageModelMode.OVERLAY;
        ResourceLocation modelId = recipe == null ? FALLBACK_MODEL : recipe.stageModel(stage);
        BakedModel model = bakedModel(modelId);
        if (model == Minecraft.getInstance().getModelManager().getMissingModel()) {
            model = overlay ? null : bakedModel(FALLBACK_MODEL);
        }

        pose.pushPose();
        rotateToFacing(pose, state.getValue(WoodenMortarBlock.FACING));
        if (overlay) {
            renderBakedModel(pose, buffer, state, bakedModel(EMPTY_MORTAR_MODEL), packedLight, packedOverlay);
        }
        if (model != null) {
            renderBakedModel(pose, buffer, state, model, packedLight, packedOverlay);
        }
        pose.popPose();
    }

    public static BakedModel bakedModel(ResourceLocation id) {
        //? if <1.20.2 {
        return Minecraft.getInstance().getModelManager().getModel(id);
        //?} else {
        /*return Minecraft.getInstance().getModelManager().getModel(ModelResourceLocation.standalone(id));
        *///?}
    }

    public static void renderBakedModel(PoseStack pose, MultiBufferSource buffer,
                                        BlockState state, BakedModel model,
                                        int packedLight, int packedOverlay) {
        Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(
                pose.last(), buffer.getBuffer(RenderType.cutout()), state, model,
                1.0F, 1.0F, 1.0F, packedLight, packedOverlay);
    }

    private static void rotateToFacing(PoseStack pose, Direction facing) {
        float degrees = switch (facing) {
            case SOUTH -> 0.0F;
            case WEST -> 90.0F;
            case NORTH -> 180.0F;
            case EAST -> 270.0F;
            default -> 0.0F;
        };
        if (degrees == 0.0F) {
            return;
        }
        pose.translate(0.5F, 0.5F, 0.5F);
        pose.mulPose(Axis.YP.rotationDegrees(degrees));
        pose.translate(-0.5F, -0.5F, -0.5F);
    }
}
