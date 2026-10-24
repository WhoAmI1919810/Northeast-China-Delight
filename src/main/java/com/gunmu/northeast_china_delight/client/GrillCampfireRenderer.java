package com.gunmu.northeast_china_delight.client;

import com.gunmu.northeast_china_delight.block.GrillBlockEntity;
import com.gunmu.northeast_china_delight.block.ModBlocks;
import com.gunmu.northeast_china_delight.crafting.GrillRecipes;
import com.gunmu.northeast_china_delight.crafting.GrillVisual;
import com.gunmu.northeast_china_delight.item.GrillSeasonings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CampfireBlock;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 烤架营火的渲染：4 份食材按原版营火的位置平躺在铁条上，加过的调料按配方里写的视觉
 * （没写就按调料本身：调料瓶和厚酱刷、鸡蛋打、其余撒）叠在食材上面，最后画烤架。
 */
public class GrillCampfireRenderer implements BlockEntityRenderer<GrillBlockEntity> {

    private static final float ITEM_Y = 0.99375F;
    private static final float ITEM_OFFSET = 0.3125F;
    private static final float ITEM_SCALE = 0.375F;

    /** 3D 物品模型画不了逐像素的调料，退回把整件食材再画一遍并染色 */
    private static final float FALLBACK_LIFT = 0.0135F;
    private static final int FALLBACK_EGG_COLOR = 0xF2C65A;

    private final ItemRenderer itemRenderer;

    public GrillCampfireRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(GrillBlockEntity grill, float partialTick, PoseStack pose, MultiBufferSource buffer,
                       int packedLight, int packedOverlay) {
        Direction facing = grill.getBlockState().getValue(CampfireBlock.FACING);
        NonNullList<ItemStack> items = grill.getItems();
        int seed = (int) grill.getBlockPos().asLong();

        for (int slot = 0; slot < items.size(); slot++) {
            ItemStack stack = items.get(slot);
            if (stack.isEmpty()) {
                continue;
            }
            Direction direction = Direction.from2DDataValue(Math.floorMod(slot + facing.get2DDataValue(), 4));

            pose.pushPose();
            translateToItem(pose, direction);
            drawItem(stack, buffer, pose, packedLight, packedOverlay, grill, seed + slot);
            pose.popPose();

            List<GrillSeasoningOverlay.Layer> layers = seasoningLayers(grill, slot);
            if (layers.isEmpty()) {
                continue;
            }
            BakedModel model = this.itemRenderer.getModel(stack, grill.getLevel(), null, seed + slot);
            pose.pushPose();
            translateToItem(pose, direction);
            boolean drawn = GrillSeasoningOverlay.draw(pose, buffer, model, layers, slot, packedLight, packedOverlay);
            pose.popPose();
            if (!drawn) {
                drawTinted(stack, layers, direction, pose, buffer, packedLight, packedOverlay, grill, seed + slot);
            }
        }

        BlockPos pos = grill.getBlockPos();
        Level level = grill.getLevel();
        GrillRackGeometry.render(pose, buffer, packedLight, packedOverlay,
                linked(level, pos, Direction.EAST),
                linked(level, pos, Direction.WEST),
                linked(level, pos, Direction.NORTH),
                linked(level, pos, Direction.SOUTH));
    }

    /**
     * 这份食材上已加调料的样子，按打蛋 → 刷酱 → 撒料从下往上叠（不管玩家先加的哪样）。
     * 客户端也有同步过来的配方，按绑定的配方 ID 找每份调料占的条目，读条目里写的视觉。
     */
    private static List<GrillSeasoningOverlay.Layer> seasoningLayers(GrillBlockEntity grill, int slot) {
        List<Item> applied = grill.seasoningsOf(slot);
        if (applied.isEmpty()) {
            return List.of();
        }
        List<GrillVisual> declared = null;
        ResourceLocation id = grill.recipeIdOf(slot);
        Level level = grill.getLevel();
        if (id != null && level != null) {
            GrillRecipes.Recipe recipe = GrillRecipes.byId(level.getRecipeManager(), id);
            if (recipe != null) {
                declared = recipe.visualsFor(applied);
            }
        }
        List<GrillSeasoningOverlay.Layer> layers = new ArrayList<>();
        for (int i = 0; i < applied.size(); i++) {
            GrillVisual visual = GrillSeasonings.resolve(declared == null ? GrillVisual.AUTO : declared.get(i),
                    applied.get(i));
            if (visual.style() != GrillVisual.Style.NONE) {
                layers.add(new GrillSeasoningOverlay.Layer(visual.style(), visual.color(), visual.opacity()));
            }
        }
        layers.sort(Comparator.comparingInt(GrillCampfireRenderer::layerOrder));
        return layers;
    }

    private static int layerOrder(GrillSeasoningOverlay.Layer layer) {
        return switch (layer.style()) {
            case EGG -> 0;
            case BRUSH -> 1;
            default -> 2;
        };
    }

    private void drawTinted(ItemStack stack, List<GrillSeasoningOverlay.Layer> layers, Direction direction,
                            PoseStack pose, MultiBufferSource buffer, int packedLight, int packedOverlay,
                            GrillBlockEntity grill, int seed) {
        for (GrillSeasoningOverlay.Layer layer : layers) {
            if (layer.style() == GrillVisual.Style.SPRINKLE) {
                continue;
            }
            boolean egg = layer.style() == GrillVisual.Style.EGG;
            int rgb = egg ? FALLBACK_EGG_COLOR : layer.color();
            MultiBufferSource tinted = new TintBufferSource(buffer,
                    ((rgb >> 16) & 0xFF) / 255.0F,
                    ((rgb >> 8) & 0xFF) / 255.0F,
                    (rgb & 0xFF) / 255.0F,
                    layer.opacity() * (egg ? 0.75F : 0.5F));
            pose.pushPose();
            pose.translate(0.0F, FALLBACK_LIFT, 0.0F);
            translateToItem(pose, direction);
            drawItem(stack, tinted, pose, packedLight, packedOverlay, grill, seed);
            pose.popPose();
        }
    }

    private void drawItem(ItemStack stack, MultiBufferSource buffer, PoseStack pose,
                          int packedLight, int packedOverlay, GrillBlockEntity grill, int seed) {
        this.itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED, packedLight, packedOverlay,
                pose, buffer, grill.getLevel(), seed);
    }

    private static boolean linked(BlockGetter level, BlockPos pos, Direction direction) {
        return level != null && level.getBlockState(pos.relative(direction)).is(ModBlocks.GRILL_CAMPFIRE.get());
    }

    private static void translateToItem(PoseStack pose, Direction direction) {
        pose.translate(0.5F, ITEM_Y, 0.5F);
        pose.mulPose(Axis.YP.rotationDegrees(-direction.toYRot()));
        pose.mulPose(Axis.XP.rotationDegrees(90.0F));
        pose.translate(-ITEM_OFFSET, -ITEM_OFFSET, 0.0F);
        pose.scale(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE);
    }

    private record TintBufferSource(MultiBufferSource delegate,
                                    float red, float green, float blue, float alpha) implements MultiBufferSource {
        @Override
        public VertexConsumer getBuffer(RenderType renderType) {
            return new TintVertexConsumer(this.delegate.getBuffer(renderType), this.red, this.green, this.blue, this.alpha);
        }
    }

    private static final class TintVertexConsumer implements VertexConsumer {
        private final VertexConsumer delegate;
        private final float red;
        private final float green;
        private final float blue;
        private final float alpha;

        private TintVertexConsumer(VertexConsumer delegate, float red, float green, float blue, float alpha) {
            this.delegate = delegate;
            this.red = red;
            this.green = green;
            this.blue = blue;
            this.alpha = alpha;
        }

        //? if <1.20.5 {
        @Override
        public VertexConsumer vertex(double x, double y, double z) {
            this.delegate.vertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer color(int red, int green, int blue, int alpha) {
            this.delegate.color(this.red, this.green, this.blue, this.alpha);
            return this;
        }

        @Override
        public VertexConsumer uv(float u, float v) {
            this.delegate.uv(u, v);
            return this;
        }

        @Override
        public VertexConsumer overlayCoords(int u, int v) {
            this.delegate.overlayCoords(u, v);
            return this;
        }

        @Override
        public VertexConsumer uv2(int u, int v) {
            this.delegate.uv2(u, v);
            return this;
        }

        @Override
        public VertexConsumer normal(float x, float y, float z) {
            this.delegate.normal(x, y, z);
            return this;
        }

        @Override
        public void endVertex() {
            this.delegate.endVertex();
        }

        @Override
        public void defaultColor(int red, int green, int blue, int alpha) {
            this.delegate.defaultColor(red, green, blue, alpha);
        }

        @Override
        public void unsetDefaultColor() {
            this.delegate.unsetDefaultColor();
        }
        //?} else {
        /*@Override
        public VertexConsumer addVertex(float x, float y, float z) {
            this.delegate.addVertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            this.delegate.setColor(this.red, this.green, this.blue, this.alpha);
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            this.delegate.setUv(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            this.delegate.setUv1(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            this.delegate.setUv2(u, v);
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            this.delegate.setNormal(x, y, z);
            return this;
        }
        *///?}
    }
}
