package com.gunmu.northeast_china_delight.client;

import com.gunmu.northeast_china_delight.NortheastChinaDelight;
import com.gunmu.northeast_china_delight.block.WoodenMortarBlock;
import com.gunmu.northeast_china_delight.block.WoodenMortarBlockEntity;
import com.gunmu.northeast_china_delight.crafting.PoundingRecipes;
import com.gunmu.northeast_china_delight.util.DdIds;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
//? if >=1.20.2 {
import net.minecraft.client.resources.model.ModelResourceLocation;
//?}

//? if <1.20.2 {
/*import net.minecraftforge.client.model.data.ModelData;
*///?} else {
import net.neoforged.neoforge.client.model.data.ModelData;
//?}

/**
 * 按当前配方阶段动态绘制木臼模型。
 *
 * <p>配方 {@code stage_model_mode} 为 overlay 时，阶段模型只有臼里的内容物：先画一只空木臼，
 * 再在同一个朝向上叠画内容物。内容物模型缺失时只画空臼，不退回整只木臼的旧模型（否则会叠出两层臼身）。</p>
 *
 * <p>亮度要和地上的静态木臼一样：空臼直接用 blockstate 里按朝向转好的那个方块模型、走原版方块的画法
 * （面明暗 + 平滑光照 + 遮挡剔除），内容物自己按面打明暗，见 {@link #renderShaded}。
 * 以前整个用 {@code renderModel} 画，它不做面明暗，侧面都跟顶面一样亮，一装料木臼就比旁边亮一截。</p>
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
        Level level = mortar.getLevel();
        if (stage <= 0 || level == null) {
            return;
        }

        PoundingRecipes.Recipe recipe = mortar.resolve(level);
        boolean overlay = recipe != null && recipe.stageModelMode() == PoundingRecipes.StageModelMode.OVERLAY;
        ResourceLocation modelId = recipe == null ? FALLBACK_MODEL : recipe.stageModel(stage);
        BakedModel model = bakedModel(modelId);
        if (model == Minecraft.getInstance().getModelManager().getMissingModel()) {
            model = overlay ? null : bakedModel(FALLBACK_MODEL);
        }

        VertexConsumer consumer = buffer.getBuffer(RenderType.cutout());
        BlockPos pos = mortar.getBlockPos();
        if (overlay) {
            BlockState empty = state.setValue(WoodenMortarBlock.STATE, 0).setValue(WoodenMortarBlock.MALLET, false);
            BakedModel base = Minecraft.getInstance().getBlockRenderer().getBlockModel(empty);
            Minecraft.getInstance().getBlockRenderer().getModelRenderer().tesselateBlock(level, base, empty, pos, pose,
                    consumer, true, RandomSource.create(), empty.getSeed(pos), packedOverlay, ModelData.EMPTY, null);
        }
        if (model != null) {
            Direction facing = state.getValue(WoodenMortarBlock.FACING);
            pose.pushPose();
            rotateToFacing(pose, facing);
            renderShaded(pose, consumer, level, pos, state, model, facing, packedLight, packedOverlay);
            pose.popPose();
        }
    }

    /**
     * 不在 blockstate 里的模型（内容物、数据包的整臼模型）按原版方块的面明暗画：顶 1.0、南北 0.8、东西 0.6、底 0.5，
     * 朝向按转过之后的算；亮度用木臼所在格的光照，跟着场景明暗走。
     */
    private static void renderShaded(PoseStack pose, VertexConsumer consumer, Level level, BlockPos pos,
                                     BlockState state, BakedModel model, Direction facing,
                                     int packedLight, int packedOverlay) {
        RandomSource random = RandomSource.create();
        long seed = state.getSeed(pos);
        for (Direction side : Direction.values()) {
            random.setSeed(seed);
            renderQuads(pose, consumer, level, model.getQuads(state, side, random), facing, packedLight, packedOverlay);
        }
        random.setSeed(seed);
        renderQuads(pose, consumer, level, model.getQuads(state, null, random), facing, packedLight, packedOverlay);
    }

    private static void renderQuads(PoseStack pose, VertexConsumer consumer, Level level, java.util.List<BakedQuad> quads,
                                    Direction facing, int packedLight, int packedOverlay) {
        for (BakedQuad quad : quads) {
            float shade = level.getShade(turned(quad.getDirection(), facing), quad.isShade());
            //? if <1.20.5 {
            /*consumer.putBulkData(pose.last(), quad, shade, shade, shade, packedLight, packedOverlay);
            *///?} else {
            consumer.putBulkData(pose.last(), quad, shade, shade, shade, 1.0F, packedLight, packedOverlay);
            //?}
        }
    }

    /** 模型里朝 {@code side} 的面，按木臼朝向转过之后实际朝哪 */
    private static Direction turned(Direction side, Direction facing) {
        if (side.getAxis() == Direction.Axis.Y) {
            return side;
        }
        int quarterTurns = Math.round(facingDegrees(facing) / 90.0F);
        for (int i = 0; i < quarterTurns; i++) {
            side = side.getCounterClockWise();
        }
        return side;
    }

    public static BakedModel bakedModel(ResourceLocation id) {
        //? if <1.20.2 {
        /*return Minecraft.getInstance().getModelManager().getModel(id);
        *///?} else {
        return Minecraft.getInstance().getModelManager().getModel(ModelResourceLocation.standalone(id));
        //?}
    }

    public static void renderBakedModel(PoseStack pose, MultiBufferSource buffer,
                                        BlockState state, BakedModel model,
                                        int packedLight, int packedOverlay) {
        Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(
                pose.last(), buffer.getBuffer(RenderType.cutout()), state, model,
                1.0F, 1.0F, 1.0F, packedLight, packedOverlay);
    }

    /**
     * 和 blockstate 的转法一致：blockstate 写 {@code "y": N} 等于绕 Y 轴转 -N 度
     * （西 90 → 270，东 270 → 90）。以前东西两向写反了，东西朝向的木臼一装料，模型就和空臼差半圈。
     */
    private static float facingDegrees(Direction facing) {
        return switch (facing) {
            case WEST -> 270.0F;
            case NORTH -> 180.0F;
            case EAST -> 90.0F;
            default -> 0.0F;
        };
    }

    private static void rotateToFacing(PoseStack pose, Direction facing) {
        float degrees = facingDegrees(facing);
        if (degrees == 0.0F) {
            return;
        }
        pose.translate(0.5F, 0.5F, 0.5F);
        pose.mulPose(Axis.YP.rotationDegrees(degrees));
        pose.translate(-0.5F, -0.5F, -0.5F);
    }
}
