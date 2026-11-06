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
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

//? if <1.20.2 {
import net.minecraftforge.client.model.data.ModelData;
//?} else {
/*import net.neoforged.neoforge.client.model.data.ModelData;
*///?}

/**
 * 按当前配方阶段动态绘制木臼模型。
 *
 * <p>配方 {@code stage_model_mode} 为 overlay 时，阶段模型只有臼里的内容物：先画一只空木臼，
 * 再在同一个朝向上叠画内容物。内容物模型缺失时只画空臼，不退回整只木臼的旧模型（否则会叠出两层臼身）。</p>
 *
 * <p>亮度要和地上的静态木臼一样：空臼直接用 blockstate 里按朝向转好的那个方块模型、走原版方块的画法
 * （面明暗 + 平滑光照 + 遮挡剔除），内容物自己按面打明暗，见 {@link #renderShaded}。
 * 以前整个用 {@code renderModel} 画，它不做面明暗，侧面都跟顶面一样亮，一装料木臼就比旁边亮一截。</p>
 *
 * <p>倒进去的液体（配方 {@code inputs.liquid}，开舂之前）和舂出来的液体（{@code finished_liquid}）不走模型，
 * 按流体贴图直接画一层液面，见 {@link #renderLiquid}（2026-11-06 用户）。</p>
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
        if (level == null) {
            return;
        }
        if (stage <= 0) {
            renderDeposited(mortar, level, state, pose, buffer, packedLight, packedOverlay);
            return;
        }

        PoundingRecipes.Recipe recipe = mortar.resolve(level);
        boolean overlay = recipe != null && recipe.stageModelMode() == PoundingRecipes.StageModelMode.OVERLAY;
        boolean done = recipe != null && stage >= recipe.doneState();
        ResourceLocation modelId = recipe == null ? FALLBACK_MODEL : recipe.stageModel(stage);
        BakedModel model = bakedModel(modelId);
        if (model == Minecraft.getInstance().getModelManager().getMissingModel()) {
            model = overlay ? null : bakedModel(FALLBACK_MODEL);
        }
        if (done && recipe.finishedLiquid() != null && recipe.finishedModel() == null) {
            // 成品是液体、又没另给成品模型：只画液面，不沿用最后一步的模型
            model = null;
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
        if (recipe != null) {
            PoundingRecipes.Liquid liquid = done ? recipe.finishedLiquid()
                    : mortar.started() ? null : recipe.depositedLiquid(mortar::inputCount);
            if (liquid != null) {
                renderLiquid(pose, buffer, level, pos, liquid, packedLight);
            }
        }
    }

    /**
     * 料还没放齐（臼还是空臼的样子，方块模型照常画）：放进去了的那几样各画各的模型（配方 inputs 里的 model，
     * 2026-11-05 用户：红薯和水得都放进去才有模型）。没写 model 的、不是 overlay 的配方什么都不多画。
     *
     * <p>只在还没舂过（进度 0）时画：取成品时服务端清空方块实体、把臼改回空臼是同一刻的事，客户端却先收到「改回空臼」、
     * 后收到「清空」，中间那一下方块实体里还留着放进去的料，不拦就闪回放料那一步（同日用户报）。
     * 舂过的那份旧数据进度一定大于 0；放最后一样料时进度是 0，照画，和接下来放齐那一步一个样，不闪。</p>
     */
    private static void renderDeposited(WoodenMortarBlockEntity mortar, Level level, BlockState state, PoseStack pose,
                                        MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (!mortar.hasDepositedInputs() || mortar.progress() > 0 || mortar.hasPendingResults()) {
            return;
        }
        PoundingRecipes.Recipe recipe = mortar.resolve(level);
        if (recipe == null) {
            return;
        }
        // 倒进去的液体：料没放齐也先画上（同一样东西放进去就看得见）
        PoundingRecipes.Liquid liquid = recipe.depositedLiquid(mortar::inputCount);
        if (liquid != null) {
            renderLiquid(pose, buffer, level, mortar.getBlockPos(), liquid, packedLight);
        }
        if (recipe.stageModelMode() != PoundingRecipes.StageModelMode.OVERLAY) {
            return;
        }
        VertexConsumer consumer = null;
        Direction facing = state.getValue(WoodenMortarBlock.FACING);
        for (int i = 0; i < recipe.inputs().size(); i++) {
            ResourceLocation id = recipe.inputs().get(i).model();
            if (id == null || mortar.inputCount(i) <= 0) {
                continue;
            }
            BakedModel model = bakedModel(id);
            if (model == Minecraft.getInstance().getModelManager().getMissingModel()) {
                continue;
            }
            if (consumer == null) {
                consumer = buffer.getBuffer(RenderType.cutout());
            }
            pose.pushPose();
            rotateToFacing(pose, facing);
            renderShaded(pose, consumer, level, mortar.getBlockPos(), state, model, facing, packedLight, packedOverlay);
            pose.popPose();
        }
    }

    /** 臼里的液面：臼底 y 10、内腔是 2..14 的八角形，液面顶在 10.5（同以前那层水 / 油模型），五块拼起来，只画顶面 */
    private static final float LIQUID_Y = 10.5F;
    private static final float[][] LIQUID_RECTS = {
            {4, 2, 12, 3}, {3, 3, 13, 4}, {2, 4, 14, 12}, {3, 12, 13, 13}, {4, 13, 12, 14}};

    /**
     * 画一层液面（2026-11-06 用户：配方写明哪样是液体，直接画对应的液面）。流体按它自己的静止贴图和颜色（水按群系水色，
     * 同原版炼药锅；JEI 里没有世界时用原版默认水色）；只给了贴图的按贴图和 color。用实体半透明的固定缓冲，
     * 液面下的花生、红薯块隔着液面看得见（同大锅、大缸，见 BigPotRenderer#renderLiquid）
     */
    public static void renderLiquid(PoseStack pose, MultiBufferSource buffer, @Nullable Level level,
                                    @Nullable BlockPos pos, PoundingRecipes.Liquid liquid, int light) {
        TextureAtlasSprite sprite = null;
        int color = liquid.color();
        Fluid fluid = liquid.fluid() == null ? Fluids.EMPTY : BuiltInRegistries.FLUID.get(liquid.fluid());
        if (fluid != Fluids.EMPTY) {
            sprite = VatRenderer.fluidSprite(fluid);
            if (color == -1) {
                color = fluid.isSame(Fluids.WATER) && level != null && pos != null
                        ? 0xFF000000 | BiomeColors.getAverageWaterColor(level, pos)
                        : VatRenderer.fluidTint(fluid);
            }
        }
        if (sprite == null && liquid.texture() != null) {
            sprite = Minecraft.getInstance()
                    .getTextureAtlas(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS).apply(liquid.texture());
        }
        if (sprite == null) {
            return;
        }
        float red = ((color >> 16) & 0xFF) / 255.0F;
        float green = ((color >> 8) & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;
        float alpha = ((color >>> 24) & 0xFF) / 255.0F;
        VertexConsumer consumer = buffer.getBuffer(Sheets.translucentCullBlockSheet());
        float y = LIQUID_Y / 16.0F;
        for (float[] r : LIQUID_RECTS) {
            float x0 = r[0] / 16.0F;
            float z0 = r[1] / 16.0F;
            float x1 = r[2] / 16.0F;
            float z1 = r[3] / 16.0F;
            VatRenderer.quad(consumer, pose.last(), light, OverlayTexture.NO_OVERLAY, red, green, blue, alpha,
                    x0, y, z0, u(sprite, x0), v(sprite, z0),
                    x0, y, z1, u(sprite, x0), v(sprite, z1),
                    x1, y, z1, u(sprite, x1), v(sprite, z1),
                    x1, y, z0, u(sprite, x1), v(sprite, z0),
                    0, 1, 0);
        }
    }

    /** 贴图铺满一格（不用 getU：1.20.1 和 1.21.1 的参数单位不一样） */
    private static float u(TextureAtlasSprite sprite, float blocks) {
        return sprite.getU0() + (sprite.getU1() - sprite.getU0()) * blocks;
    }

    private static float v(TextureAtlasSprite sprite, float blocks) {
        return sprite.getV0() + (sprite.getV1() - sprite.getV0()) * blocks;
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
            consumer.putBulkData(pose.last(), quad, shade, shade, shade, packedLight, packedOverlay);
            //?} else {
            /*consumer.putBulkData(pose.last(), quad, shade, shade, shade, 1.0F, packedLight, packedOverlay);
            *///?}
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
