package com.gunmu.northeast_china_delight.client;

import com.gunmu.northeast_china_delight.block.GrillBlockEntity;
import com.gunmu.northeast_china_delight.item.BottleColors;
import com.gunmu.northeast_china_delight.item.GrillSeasonings;
import com.gunmu.northeast_china_delight.item.ModItems;
import com.gunmu.northeast_china_delight.item.SeasoningBottleItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.CampfireBlock;
import com.gunmu.northeast_china_delight.block.ModBlocks;

import java.util.ArrayList;
import java.util.List;

/**
 * 烤架营火的渲染。所以位置、朝向、透明轮廓和食材完全一致；盐、糖这类则在食材的实心像素上撒几个白点；</li>
 */
public class GrillCampfireRenderer implements BlockEntityRenderer<GrillBlockEntity> {

    private static final float ITEM_Y = 0.99375F;
    private static final float ITEM_OFFSET = 0.3125F;
    private static final float ITEM_SCALE = 0.375F;

    private static final float SEASON_LIFT = 0.0135F;
    private static final float SEASON_ALPHA = 0.32F;

    private static final int SAUCE_TINT = 0xB5321C;
    private static final float SAUCE_ALPHA = 0.55F;
    private static final int SAUCE_FLECK_COLOR = 0x7A1A0C;
    private static final float SAUCE_FLECK_ALPHA = 0.9F;
    private static final int SAUCE_FLECK_COUNT = 7;
    private static final float SAUCE_FLECK_HALF_PX = 0.9F;

    private static final int SPECK_COUNT = 4;
    private static final float SPECK_HALF_PX = 0.6F;
    private static final float SPECK_ALPHA = 0.9F;
    /** 只有足够实的像素才撒点，避免点在半透明的边缘上 */
    private static final int SPECK_MIN_ALPHA = 200;

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
            List<Item> applied = grill.seasoningsOf(slot);

            pose.pushPose();
            translateToItem(pose, direction);
            drawItem(stack, buffer, pose, packedLight, packedOverlay, grill, seed + slot);
            pose.popPose();

            for (Item seasoning : applied) {
                if (!(seasoning instanceof SeasoningBottleItem)) {
                    continue;
                }
                int argb = BottleColors.of(seasoning);
                MultiBufferSource tinted = new TintBufferSource(buffer,
                        ((argb >> 16) & 0xFF) / 255.0F,
                        ((argb >> 8) & 0xFF) / 255.0F,
                        (argb & 0xFF) / 255.0F,
                        SEASON_ALPHA);
                pose.pushPose();
                pose.translate(0.0F, SEASON_LIFT, 0.0F);
                translateToItem(pose, direction);
                drawItem(stack, tinted, pose, packedLight, packedOverlay, grill, seed + slot);
                pose.popPose();
            }

            boolean hasSauce = applied.stream().anyMatch(GrillSeasonings::isSauce);
            if (hasSauce) {
                MultiBufferSource sauce = new TintBufferSource(buffer,
                        ((SAUCE_TINT >> 16) & 0xFF) / 255.0F,
                        ((SAUCE_TINT >> 8) & 0xFF) / 255.0F,
                        (SAUCE_TINT & 0xFF) / 255.0F,
                        SAUCE_ALPHA);
                pose.pushPose();
                pose.translate(0.0F, SEASON_LIFT, 0.0F);
                translateToItem(pose, direction);
                drawItem(stack, sauce, pose, packedLight, packedOverlay, grill, seed + slot);
                pose.popPose();
                renderSpecks(pose, buffer, stack, direction, grill.getLevel(), seed + slot,
                        packedLight, packedOverlay,
                        SAUCE_FLECK_COLOR, SAUCE_FLECK_ALPHA, SAUCE_FLECK_HALF_PX, SAUCE_FLECK_COUNT);
            }

            boolean hasDrySeasoning = applied.stream().anyMatch(item -> !GrillSeasonings.needsBrush(new ItemStack(item)));
            if (hasDrySeasoning) {
                renderSpecks(pose, buffer, stack, direction, grill.getLevel(), seed + slot,
                        packedLight, packedOverlay,
                        0xFFFFFF, SPECK_ALPHA, SPECK_HALF_PX, SPECK_COUNT);
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

    private static void translateToItemLocal(PoseStack pose, Direction direction) {
        pose.translate(0.0F, SEASON_LIFT, 0.0F);
        translateToItem(pose, direction);
    }

    private void renderSpecks(PoseStack pose, MultiBufferSource buffer, ItemStack stack, Direction direction,
                              net.minecraft.world.level.Level level, int seed,
                              int packedLight, int packedOverlay,
                              int color, float alpha, float halfPx, int count) {
        BakedModel model = this.itemRenderer.getModel(stack, level, null, seed);
        RandomSource random = RandomSource.create(42L);
        List<BakedQuad> quads = model.getQuads(null, null, random);
        if (quads.isEmpty()) {
            return;
        }
        TextureAtlasSprite sprite = quads.get(0).getSprite();
        int width = sprite.contents().width();
        int height = sprite.contents().height();

        List<int[]> candidates = new ArrayList<>();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if ((sprite.getPixelRGBA(0, x, y) >>> 24) >= SPECK_MIN_ALPHA) {
                    candidates.add(new int[] {x, y});
                }
            }
        }
        if (candidates.isEmpty()) {
            return;
        }

        VertexConsumer consumer = buffer.getBuffer(RenderType.debugQuads());
        float red = ((color >> 16) & 0xFF) / 255.0F;
        float green = ((color >> 8) & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;
        pose.pushPose();
        translateToItemLocal(pose, direction);
        PoseStack.Pose last = pose.last();
        for (int i = 0; i < count; i++) {
            int[] pixel = candidates.get((int) ((long) i * candidates.size() / count) % candidates.size());
            float x0 = (pixel[0] + 0.5F - halfPx) / width - 0.5F;
            float x1 = (pixel[0] + 0.5F + halfPx) / width - 0.5F;
            float y0 = (pixel[1] + 0.5F - halfPx) / height - 0.5F;
            float y1 = (pixel[1] + 0.5F + halfPx) / height - 0.5F;
            addSpeckQuad(consumer, last, x0, y0, x1, y1, red, green, blue, alpha);
        }
        pose.popPose();
    }

    private static void addSpeckQuad(VertexConsumer consumer, PoseStack.Pose pose,
                                     float x0, float y0, float x1, float y1,
                                     float red, float green, float blue, float alpha) {
        //? if <1.20.5 {
        /*consumer.vertex(pose.pose(), x0, y0, 0.0F).color(red, green, blue, alpha).endVertex();
        consumer.vertex(pose.pose(), x1, y0, 0.0F).color(red, green, blue, alpha).endVertex();
        consumer.vertex(pose.pose(), x1, y1, 0.0F).color(red, green, blue, alpha).endVertex();
        consumer.vertex(pose.pose(), x0, y1, 0.0F).color(red, green, blue, alpha).endVertex();*/
        //?} else {
        consumer.addVertex(pose, x0, y0, 0.0F).setColor(red, green, blue, alpha);
        consumer.addVertex(pose, x1, y0, 0.0F).setColor(red, green, blue, alpha);
        consumer.addVertex(pose, x1, y1, 0.0F).setColor(red, green, blue, alpha);
        consumer.addVertex(pose, x0, y1, 0.0F).setColor(red, green, blue, alpha);
        //?}
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
        /*@Override
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
        }*/
        //?} else {
        @Override
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
        //?}
    }
}
