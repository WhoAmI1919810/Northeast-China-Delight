package com.gunmu.northeast_china_delight.client;

import com.gunmu.northeast_china_delight.NortheastChinaDelight;
import com.gunmu.northeast_china_delight.crafting.GrillVisual;
import com.gunmu.northeast_china_delight.util.DdIds;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * 烤架上调料的样子：打蛋、刷酱、撒料都按食材贴图的不透明像素逐格画小方片，贴在食材朝上那一面的上方。
 *
 * <p>平面物品模型（item/generated）朝上那一面就是贴图正面：像素 (u, v) 落在模型坐标
 * x = u/宽 .. (u+1)/宽、y = 1 - (v+1)/高 .. 1 - v/高、z = 8.5/16。所以先套上物品自己的 FIXED 变换再画，
 * 方片才和贴图像素对得上（之前直接按贴图坐标画，物品模型自带的 180° 翻转会让白点落到对角的空白处）。
 * 图案只跟（贴图、样式、颜色、第几种）有关，算一次后缓存。</p>
 *
 * <p>食材由 {@link GrillCampfireRenderer} 通过 {@link Sheets#cutoutBlockSheet()} 先行完整绘制并写入深度；
 * 调料方片取方块图集里一格纯白、靠顶点色上色，统一走 {@link Sheets#translucentCullBlockSheet()}，
 * 在 {@code LevelRenderer} 中严格排在 {@code cutoutBlockSheet} 之后绘制，因此半透明酱层只会正确叠在食材表面，
 * 不会再和食材抢深度或把食材剔除成半透明。同槽位多层整格覆盖（打蛋 + 刷酱）在 CPU 端按源覆盖预合成单层，
 * 避免多层半透明小方片互相排序出错。</p>
 */
final class GrillSeasoningOverlay {

    private static final ResourceLocation WHITE = DdIds.of(NortheastChinaDelight.MODID, "block/grill_overlay");

    /** 物品贴图正面在模型坐标里的 z */
    private static final float FRONT_Z = 8.5F / 16.0F;
    /** 第一层离贴图正面多远、撒料层再抬多少（模型单位，乘 0.375 才是世界单位） */
    private static final float BASE_LIFT = 0.010F;
    private static final float LAYER_LIFT = 0.012F;

    /** 一个格子：x0 y0 x1 y1（贴图坐标 0..1，y 朝下）、r g b a、额外抬高 */
    private static final int STRIDE = 9;

    private static final int EGG_WHITE = 0xF7EFD8;
    private static final int EGG_YOLK = 0xEFC048;
    private static final int EGG_YOLK_LIGHT = 0xF6D772;
    private static final int EGG_BROWNED = 0xDDA04A;

    private static final Map<TextureAtlasSprite, Mask> MASKS = new WeakHashMap<>();
    private static final Map<CompositeKey, float[]> COMPOSITE_PATTERNS = new LinkedHashMap<>(64, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<CompositeKey, float[]> eldest) {
            return this.size() > 512;
        }
    };

    /** 一层调料：样式、颜色、不透明度（酱的稀稠：越稀越透，越稠越实） */
    record Layer(GrillVisual.Style style, int color, float opacity) {
    }

    private record CompositeKey(TextureAtlasSprite sprite, List<Layer> layers, int variant) {
    }

    /**
     * 食材贴图的形状：depth = 离透明处有几格（0 = 透明，1 = 最外一圈）。
     * 主轴角度用来让刷子沿食材的长边刷。cutoutOnly 表示贴图不含半透明像素，可安全走 cutout 通道。
     */
    private record Mask(int width, int height, int[] depth, int maxDepth, float centerX, float centerY,
                        float axisAngle, boolean cutoutOnly) {
    }

    private GrillSeasoningOverlay() {
    }

    /** 平面贴图且不含半透明像素的食材可走 cutoutBlockSheet，避免和半透明调料层互相深度排序。 */
    static boolean isCutoutModel(BakedModel model) {
        if (model.isGui3d()) {
            return false;
        }
        List<BakedQuad> quads = model.getQuads(null, null, RandomSource.create(42L));
        if (quads.isEmpty()) {
            return false;
        }
        TextureAtlasSprite sprite = quads.get(0).getSprite();
        return MASKS.computeIfAbsent(sprite, GrillSeasoningOverlay::mask).cutoutOnly();
    }

    /**
     * 在已经摆到食材位置的 pose 上，按顺序画出各层调料。
     * 物品不是平面贴图模型（3D 模型）时什么都不画，返回 false，由调用方退回整体染色。
     */
    static boolean draw(PoseStack pose, MultiBufferSource buffer, BakedModel model, List<Layer> layers,
                        int variant, int packedLight, int packedOverlay) {
        if (model.isGui3d()) {
            return false;
        }
        List<BakedQuad> quads = model.getQuads(null, null, RandomSource.create(42L));
        if (quads.isEmpty()) {
            return false;
        }
        TextureAtlasSprite sprite = quads.get(0).getSprite();

        pose.pushPose();
        model.getTransforms().getTransform(ItemDisplayContext.FIXED).apply(false, pose);
        pose.translate(-0.5F, -0.5F, -0.5F);
        PoseStack.Pose last = pose.last();
        TextureAtlasSprite white = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(WHITE);
        float u = (white.getU0() + white.getU1()) / 2.0F;
        float v = (white.getV0() + white.getV1()) / 2.0F;
        VertexConsumer consumer = buffer.getBuffer(Sheets.translucentCullBlockSheet());
        float[] cells = compositePattern(sprite, layers, variant);
        float baseZ = FRONT_Z + BASE_LIFT;
        for (int i = 0; i < cells.length; i += STRIDE) {
            float cellZ = baseZ + cells[i + 8];
            float x0 = cells[i];
            float x1 = cells[i + 2];
            float y0 = 1.0F - cells[i + 3];
            float y1 = 1.0F - cells[i + 1];
            float r = cells[i + 4];
            float g = cells[i + 5];
            float b = cells[i + 6];
            float a = cells[i + 7];
            // 逆时针（从上面看），这批是开背面剔除的
            vertex(consumer, last, x0, y0, cellZ, u, v, r, g, b, a, packedLight, packedOverlay);
            vertex(consumer, last, x1, y0, cellZ, u, v, r, g, b, a, packedLight, packedOverlay);
            vertex(consumer, last, x1, y1, cellZ, u, v, r, g, b, a, packedLight, packedOverlay);
            vertex(consumer, last, x0, y1, cellZ, u, v, r, g, b, a, packedLight, packedOverlay);
        }
        pose.popPose();
        return true;
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z,
                               float u, float v, float r, float g, float b, float a,
                               int packedLight, int packedOverlay) {
        //? if <1.20.5 {
        /*consumer.vertex(pose.pose(), x, y, z)
                .color(r, g, b, a)
                .uv(u, v)
                .overlayCoords(packedOverlay)
                .uv2(packedLight)
                .normal(pose.normal(), 0.0F, 0.0F, 1.0F)
                .endVertex();*/
        //?} else {
        consumer.addVertex(pose, x, y, z)
                .setColor(r, g, b, a)
                .setUv(u, v)
                .setOverlay(packedOverlay)
                .setLight(packedLight)
                .setNormal(pose, 0.0F, 0.0F, 1.0F);
        //?}
    }

    /**
     * 把同一槽位的所有调料层合成为最终待画方片：
     * 打蛋 / 刷酱都是整像素网格，按顺序用源覆盖（source-over）合并为单层，避免半透明多层互抢深度；
     * 撒料颗粒叠在合成层上方。
     */
    private static float[] compositePattern(TextureAtlasSprite sprite, List<Layer> layers, int variant) {
        return COMPOSITE_PATTERNS.computeIfAbsent(new CompositeKey(sprite, List.copyOf(layers), variant), key -> {
            Mask mask = MASKS.computeIfAbsent(sprite, GrillSeasoningOverlay::mask);
            int total = mask.width() * mask.height();
            float[] coatR = new float[total];
            float[] coatG = new float[total];
            float[] coatB = new float[total];
            float[] coatA = new float[total];
            boolean hasCoat = false;

            Cells sprinkles = new Cells();
            int sprinkleIndex = 0;
            for (int idx = 0; idx < layers.size(); idx++) {
                Layer layer = layers.get(idx);
                int layerVariant = variant * 4 + idx;
                if (layer.style() == GrillVisual.Style.EGG || layer.style() == GrillVisual.Style.BRUSH) {
                    float[] cells = layer.style() == GrillVisual.Style.EGG
                            ? egg(mask, layer.opacity(), layerVariant)
                            : brush(mask, layer.color(), layer.opacity(), layerVariant);
                    for (int i = 0; i < cells.length; i += STRIDE) {
                        int px = Math.min(mask.width() - 1, Math.max(0, Math.round(cells[i] * mask.width())));
                        int py = Math.min(mask.height() - 1, Math.max(0, Math.round(cells[i + 1] * mask.height())));
                        int cellIdx = py * mask.width() + px;
                        float sr = cells[i + 4];
                        float sg = cells[i + 5];
                        float sb = cells[i + 6];
                        float sa = cells[i + 7];
                        float da = coatA[cellIdx];
                        float outA = sa + da * (1.0F - sa);
                        if (outA > 1.0E-4F) {
                            float keep = da * (1.0F - sa);
                            coatR[cellIdx] = (sr * sa + coatR[cellIdx] * keep) / outA;
                            coatG[cellIdx] = (sg * sa + coatG[cellIdx] * keep) / outA;
                            coatB[cellIdx] = (sb * sa + coatB[cellIdx] * keep) / outA;
                            coatA[cellIdx] = outA;
                            hasCoat = true;
                        }
                    }
                } else if (layer.style() == GrillVisual.Style.SPRINKLE) {
                    float extraLift = (hasCoat ? LAYER_LIFT : 0.0F) + sprinkleIndex * (LAYER_LIFT * 0.5F);
                    float[] cells = sprinkle(mask, layer.color(), layer.opacity(), layerVariant);
                    for (int i = 0; i < cells.length; i += STRIDE) {
                        sprinkles.addRaw(cells[i], cells[i + 1], cells[i + 2], cells[i + 3],
                                cells[i + 4], cells[i + 5], cells[i + 6], cells[i + 7], cells[i + 8] + extraLift);
                    }
                    sprinkleIndex++;
                }
            }

            Cells out = new Cells();
            if (hasCoat) {
                for (int y = 0; y < mask.height(); y++) {
                    for (int x = 0; x < mask.width(); x++) {
                        int cellIdx = y * mask.width() + x;
                        float a = coatA[cellIdx];
                        if (a > 1.0E-4F) {
                            out.addRaw(x / (float) mask.width(), y / (float) mask.height(),
                                    (x + 1) / (float) mask.width(), (y + 1) / (float) mask.height(),
                                    coatR[cellIdx], coatG[cellIdx], coatB[cellIdx], a, 0.0F);
                        }
                    }
                }
            }
            return sprinkles.size == 0 ? out.toArray() : out.concat(sprinkles);
        });
    }

    private static Mask mask(TextureAtlasSprite sprite) {
        int width = sprite.contents().width();
        int height = sprite.contents().height();
        int[] depth = new int[width * height];
        int big = width + height;
        double sumX = 0;
        double sumY = 0;
        int count = 0;
        boolean cutoutOnly = true;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int alpha = sprite.getPixelRGBA(0, x, y) >>> 24;
                if (alpha > 10 && alpha < 245) {
                    cutoutOnly = false;
                }
                boolean opaque = alpha >= 128;
                depth[y * width + x] = opaque ? big : 0;
                if (opaque) {
                    sumX += x + 0.5;
                    sumY += y + 0.5;
                    count++;
                }
            }
        }
        // 两遍倒角距离：出界当透明
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int i = y * width + x;
                if (depth[i] > 0) {
                    int up = y > 0 ? depth[i - width] : 0;
                    int left = x > 0 ? depth[i - 1] : 0;
                    depth[i] = Math.min(depth[i], Math.min(up, left) + 1);
                }
            }
        }
        int maxDepth = 0;
        for (int y = height - 1; y >= 0; y--) {
            for (int x = width - 1; x >= 0; x--) {
                int i = y * width + x;
                if (depth[i] > 0) {
                    int down = y < height - 1 ? depth[i + width] : 0;
                    int right = x < width - 1 ? depth[i + 1] : 0;
                    depth[i] = Math.min(depth[i], Math.min(down, right) + 1);
                    maxDepth = Math.max(maxDepth, depth[i]);
                }
            }
        }
        float centerX = count == 0 ? width / 2.0F : (float) (sumX / count);
        float centerY = count == 0 ? height / 2.0F : (float) (sumY / count);
        double xx = 0;
        double yy = 0;
        double xy = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (depth[y * width + x] > 0) {
                    double dx = x + 0.5 - centerX;
                    double dy = y + 0.5 - centerY;
                    xx += dx * dx;
                    yy += dy * dy;
                    xy += dx * dy;
                }
            }
        }
        float angle = (float) (0.5 * Math.atan2(2.0 * xy, xx - yy));
        return new Mask(width, height, depth, maxDepth, centerX, centerY, angle, cutoutOnly);
    }

    /** 贴图越大，图案的「一个像素」占的贴图格越多，保持和 16×16 贴图一样的颗粒感 */
    private static int scale(Mask mask) {
        return Math.max(1, mask.width() / 16);
    }

    /** 打蛋：蛋液摊满食材（留出最外一圈露出底下的边），黄色为主，夹着蛋白和少量煎黄的地方 */
    private static float[] egg(Mask mask, float opacity, int variant) {
        int s = scale(mask);
        int inner = mask.maxDepth() >= 2 ? 2 : 1;
        Cells cells = new Cells();
        for (int y = 0; y < mask.height(); y++) {
            for (int x = 0; x < mask.width(); x++) {
                int depth = mask.depth()[y * mask.width() + x];
                if (depth < inner || depth == inner && inner > 1 && hash(x, y, variant + 11) < 0.35F) {
                    continue;
                }
                float n = 0.65F * hash(x / (2 * s), y / (2 * s), variant) + 0.35F * hash(x, y, variant + 1);
                int rgb = n < 0.32F ? EGG_WHITE
                        : n > 0.86F ? EGG_BROWNED
                        : mix(EGG_YOLK, EGG_YOLK_LIGHT, hash(x, y, variant + 2));
                float shade = 0.96F + 0.08F * hash(x, y, variant + 3);
                cells.add(x, y, x + 1, y + 1, mask, rgb, shade, opacity, 0.0F);
            }
        }
        return cells.toArray();
    }

    /**
     * 刷酱：沿食材长边一道道刷过去，浓的那道是 opacity、淡的一半，带几处反光；
     * 稠的（opacity ≥ 0.6）酱里还有碎粒。
     */
    private static float[] brush(Mask mask, int color, float opacity, int variant) {
        int s = scale(mask);
        float strokeWidth = 2.0F * s;
        float px = (float) -Math.sin(mask.axisAngle());
        float py = (float) Math.cos(mask.axisAngle());
        float offset = hash(variant, 7, 31);
        Cells cells = new Cells();
        for (int y = 0; y < mask.height(); y++) {
            for (int x = 0; x < mask.width(); x++) {
                int depth = mask.depth()[y * mask.width() + x];
                if (depth == 0) {
                    continue;
                }
                float t = ((x + 0.5F - mask.centerX()) * px + (y + 0.5F - mask.centerY()) * py) / strokeWidth + offset;
                int stroke = (int) Math.floor(t);
                boolean heavy = hash(stroke, 3, variant + 4) < 0.72F;
                float alpha = heavy ? opacity : opacity * 0.5F;
                if (hash(x, y, variant + 5) < 0.10F) {
                    alpha *= 0.45F;
                }
                if (depth == 1) {
                    alpha *= 0.75F;
                }
                int rgb = color;
                float shade = 0.92F + 0.12F * hash(x, y, variant + 6);
                float roll = hash(x, y, variant + 7);
                if (heavy && roll < 0.10F && depth > 1) {
                    rgb = mix(color, 0xFFFFFF, 0.45F);
                    alpha = opacity * 0.9F;
                } else if (roll > 0.96F && opacity >= 0.6F) {
                    shade *= 0.62F;
                    alpha = Math.min(1.0F, opacity + 0.12F);
                }
                cells.add(x, y, x + 1, y + 1, mask, rgb, shade, alpha, 0.0F);
            }
        }
        return cells.toArray();
    }

    /** 撒料：零星的小颗粒，浅色的（盐、糖）右下各压一道淡影子，落在浅色食材上也看得出 */
    private static float[] sprinkle(Mask mask, int color, float opacity, int variant) {
        int s = scale(mask);
        int inner = mask.maxDepth() >= 2 ? 2 : 1;
        boolean light = luminance(color) > 0.7F;
        Cells shadows = new Cells();
        Cells grains = new Cells();
        for (int y = 0; y < mask.height(); y++) {
            for (int x = 0; x < mask.width(); x++) {
                if (mask.depth()[y * mask.width() + x] < inner || hash(x, y, variant + 8) >= 0.15F) {
                    continue;
                }
                float size = (hash(x, y, variant + 9) < 0.3F ? 0.75F : 0.5F) * s;
                float x0 = x + hash(x, y, variant + 10) * Math.max(0.0F, 1.0F - size);
                float y0 = y + hash(x, y, variant + 12) * Math.max(0.0F, 1.0F - size);
                float shade = 0.90F + 0.14F * hash(x, y, variant + 13);
                if (light) {
                    float drop = 0.2F * s;
                    shadows.add(x0 + drop, y0 + drop, x0 + size + drop, y0 + size + drop, mask, 0x000000, 1.0F,
                            0.28F * opacity, 0.0F);
                }
                grains.add(x0, y0, x0 + size, y0 + size, mask, color, shade, opacity, LAYER_LIFT * 0.4F);
            }
        }
        return shadows.concat(grains);
    }

    private static float luminance(int rgb) {
        return (0.299F * ((rgb >> 16) & 0xFF) + 0.587F * ((rgb >> 8) & 0xFF) + 0.114F * (rgb & 0xFF)) / 255.0F;
    }

    private static int mix(int a, int b, float t) {
        int r = Math.round(((a >> 16) & 0xFF) * (1 - t) + ((b >> 16) & 0xFF) * t);
        int g = Math.round(((a >> 8) & 0xFF) * (1 - t) + ((b >> 8) & 0xFF) * t);
        int bl = Math.round((a & 0xFF) * (1 - t) + (b & 0xFF) * t);
        return (r << 16) | (g << 8) | bl;
    }

    private static float hash(int x, int y, int seed) {
        int h = x * 0x27D4EB2D ^ y * 0x165667B1 ^ seed * 0x5BD1E995;
        h ^= h >>> 15;
        h *= 0x2C1B3C6D;
        h ^= h >>> 12;
        h *= 0x297A2D39;
        h ^= h >>> 15;
        return (h >>> 8) / 16777216.0F;
    }

    private static final class Cells {
        private float[] data = new float[STRIDE * 64];
        private int size;

        void add(float x0, float y0, float x1, float y1, Mask mask, int rgb, float shade, float alpha, float lift) {
            addRaw(x0 / mask.width(), y0 / mask.height(), x1 / mask.width(), y1 / mask.height(),
                    Math.min(1.0F, ((rgb >> 16) & 0xFF) / 255.0F * shade),
                    Math.min(1.0F, ((rgb >> 8) & 0xFF) / 255.0F * shade),
                    Math.min(1.0F, (rgb & 0xFF) / 255.0F * shade),
                    alpha, lift);
        }

        void addRaw(float u0, float v0, float u1, float v1, float r, float g, float b, float alpha, float lift) {
            if (this.size + STRIDE > this.data.length) {
                this.data = Arrays.copyOf(this.data, this.data.length * 2);
            }
            float[] d = this.data;
            int i = this.size;
            d[i] = u0;
            d[i + 1] = v0;
            d[i + 2] = u1;
            d[i + 3] = v1;
            d[i + 4] = r;
            d[i + 5] = g;
            d[i + 6] = b;
            d[i + 7] = alpha;
            d[i + 8] = lift;
            this.size += STRIDE;
        }

        float[] toArray() {
            return Arrays.copyOf(this.data, this.size);
        }

        float[] concat(Cells other) {
            float[] out = Arrays.copyOf(this.data, this.size + other.size);
            System.arraycopy(other.data, 0, out, this.size, other.size);
            return out;
        }
    }
}
