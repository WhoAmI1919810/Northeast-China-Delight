package com.gunmu.northeast_china_delight.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.gunmu.northeast_china_delight.NortheastChinaDelight;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;

/**
 * 烤架铁条的几何：1px 粗的密集铁条 + 四边边框 + 四角立柱，贴的是原版铁栏杆的贴图。
 *
 * <p>两处共用同一份几何：
 * <ul>
 *     <li>世界里由 {@link GrillCampfireRenderer} 调用 —— 它会按四个方向有没有相邻的烤架
 *         决定边框和立柱怎么拼；</li>
 *     <li>JEI 的「烧烤」页面里当成单独一台调用（四面都不相连）。</li>
 * </ul>
 *
 * <p>坐标是方块局部坐标（0~1），所以调用方只要把 PoseStack 摆到方块上，
 * 世界里和 GUI 里画出来的就是同一个东西。
 */
public final class GrillRackGeometry {

    /**
     * 铁条用的贴图。
     *
     * <p>**不能借原版的 `minecraft:block/iron_bars`**：那张图是给「铁栏杆方块模型」用的，
     * 整张 16×16 里只有 x = 2~3 / 7~8 / 12~13 三列是真铁条，其余都是 alpha≈32 的深色填充像素。
     * 我们的烤架是一根根 1px 的铁条、会在 16 像素里采到任意一列，采到暗列时铁条就成了一条
     * 半透明的黑条（被营火一照发褐，看着像木条）。
     * 所以这里用本模组自己的贴图（由 `tools/generate_grill_rack_texture.ps1` 生成，整张都是金属）。
     */
    private static final ResourceLocation BARS_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(NortheastChinaDelight.MODID, "block/grill_rack_bars");

    /**
     * 烤架架面的高度：架在火焰上方。
     * 模型里架面的底面在 y = 14（也就是 14/16 格），四条支脚从 y = 0 撑下来；
     * 渲染时把整个模型抬到 RACK_TOP_Y。
     */
    public static final float RACK_TOP_Y = 0.9F;
    /** 一格像素 */
    private static final float PX = 1.0F / 16.0F;
    /**
     * 每个方向上铁条的根数。
     *
     * 8 根、位置固定在 1/3/5/…/15 像素处 —— 这样：
     * <ul>
     *     <li>每两根之间的缝都是 1 像素，整张网间距完全均匀（不会中间挤、边上松）；</li>
     *     <li>拼起来的两台之间也正好是 1 像素的缝，两个格子的铁条自然连成一张网，
     *         所以不再需要额外画「骑在接缝上」的共享铁条（多出那一根会让间距不均匀）。</li>
     * </ul>
     */
    private static final int BAR_COUNT = 8;

    private GrillRackGeometry() {
    }

    /**
     * @param east  东边那格是不是也架着烤架（是的话自己这边不画边框，改由正方向那台画共享铁条）
     * @param west  西边同理
     * @param north 北边同理
     * @param south 南边同理
     */
    public static void render(PoseStack pose, MultiBufferSource buffer, int packedLight, int packedOverlay,
                              boolean east, boolean west, boolean north, boolean south) {
        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
                .apply(BARS_TEXTURE);
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS));
        PoseStack.Pose matrix = pose.last();

        float y0 = RACK_TOP_Y;
        float y1 = RACK_TOP_Y + PX;

        // 纵向铁条（沿 Z 贯穿整格）：1/3/5/…/15 像素，缝宽统一 1 像素
        for (int i = 0; i < BAR_COUNT; i++) {
            float x = (1 + i * 2) * PX;
            box(consumer, matrix, sprite, x, y0, 0, x + PX, y1, 1, packedLight, packedOverlay);
        }

        // 横向铁条（沿 X 贯穿整格）：同样的间距，和纵向那组配成一张均匀的网。
        // 故意比纵向铁条低半像素：交叉的地方藏在纵向铁条下面，不会和它共面打架（z-fighting）。
        float crossY0 = y0 - PX * 0.5F;
        float crossY1 = y0 + PX * 0.5F;
        for (int i = 0; i < BAR_COUNT; i++) {
            float z = (1 + i * 2) * PX;
            box(consumer, matrix, sprite, 0, crossY0, z, 1, crossY1, z + PX, packedLight, packedOverlay);
        }

        // 四角立柱：只有外侧的角才立柱子
        float legSize = 2 * PX;
        if (!west && !north) {
            box(consumer, matrix, sprite, PX, 0, PX, PX + legSize, y0, PX + legSize, packedLight, packedOverlay);
        }
        if (!east && !north) {
            box(consumer, matrix, sprite, 1 - PX - legSize, 0, PX, 1 - PX, y0, PX + legSize, packedLight, packedOverlay);
        }
        if (!west && !south) {
            box(consumer, matrix, sprite, PX, 0, 1 - PX - legSize, PX + legSize, y0, 1 - PX, packedLight, packedOverlay);
        }
        if (!east && !south) {
            box(consumer, matrix, sprite, 1 - PX - legSize, 0, 1 - PX - legSize, 1 - PX, y0, 1 - PX, packedLight, packedOverlay);
        }
    }

    /** 一根方柱：六个面都贴上铁栏杆贴图，贴图按「一格 16 像素」的原尺度铺 */
    private static void box(VertexConsumer consumer, PoseStack.Pose matrix, TextureAtlasSprite sprite,
                            float x0, float y0, float z0, float x1, float y1, float z1,
                            int packedLight, int packedOverlay) {
        float su = sprite.getU0();
        float sv = sprite.getV0();
        float du = sprite.getU1() - su;
        float dv = sprite.getV1() - sv;
        // 贴图坐标不只要夹在 0~1，还得**再往里让半个像素**：
        // 拼起来的两台之间那根「骑在接缝上」的共享铁条有一半伸到格子外（x 最大到 1.031），
        // 直接夹到 1.0 会正好落在贴图边界上 —— 图集开了 mipmap，采样会把隔壁那张贴图混进来
        // （表现就是铁条上冒出一节节木头色的黑条）。夹到「最外圈像素的中心」就干净了。
        float halfTexel = 0.5F / Math.max(1, sprite.contents().width());
        float u0 = su + clampTexel(x0, halfTexel) * du;
        float u1 = su + clampTexel(x1, halfTexel) * du;
        float v0 = sv + clampTexel(y0, halfTexel) * dv;
        float v1 = sv + clampTexel(y1, halfTexel) * dv;
        // 沿 Z 的那一组：给「东西面」当 U 用（所以必须从 su 起算），
        // 给「顶 / 底面」当 V 用（从 sv 起算）。之前图省事只算了一组、还用了 sv，
        // 结果东西两个侧面采到了图集里完全不相干的位置 —— 表现就是「只有南北方向颜色对，东西侧面是黑条」。
        float uz0 = su + clampTexel(z0, halfTexel) * du;
        float uz1 = su + clampTexel(z1, halfTexel) * du;
        float vz0 = sv + clampTexel(z0, halfTexel) * dv;
        float vz1 = sv + clampTexel(z1, halfTexel) * dv;

        // 上（+Y）：u 沿 x，v 沿 z
        vertex(consumer, matrix, x0, y1, z0, u0, vz0, 0, 1, 0, packedLight, packedOverlay);
        vertex(consumer, matrix, x0, y1, z1, u0, vz1, 0, 1, 0, packedLight, packedOverlay);
        vertex(consumer, matrix, x1, y1, z1, u1, vz1, 0, 1, 0, packedLight, packedOverlay);
        vertex(consumer, matrix, x1, y1, z0, u1, vz0, 0, 1, 0, packedLight, packedOverlay);
        // 下（-Y）
        vertex(consumer, matrix, x0, y0, z0, u0, vz0, 0, -1, 0, packedLight, packedOverlay);
        vertex(consumer, matrix, x1, y0, z0, u1, vz0, 0, -1, 0, packedLight, packedOverlay);
        vertex(consumer, matrix, x1, y0, z1, u1, vz1, 0, -1, 0, packedLight, packedOverlay);
        vertex(consumer, matrix, x0, y0, z1, u0, vz1, 0, -1, 0, packedLight, packedOverlay);
        // 北（-Z）
        vertex(consumer, matrix, x0, y0, z0, u0, v1, 0, 0, -1, packedLight, packedOverlay);
        vertex(consumer, matrix, x0, y1, z0, u0, v0, 0, 0, -1, packedLight, packedOverlay);
        vertex(consumer, matrix, x1, y1, z0, u1, v0, 0, 0, -1, packedLight, packedOverlay);
        vertex(consumer, matrix, x1, y0, z0, u1, v1, 0, 0, -1, packedLight, packedOverlay);
        // 南（+Z）
        vertex(consumer, matrix, x0, y0, z1, u0, v1, 0, 0, 1, packedLight, packedOverlay);
        vertex(consumer, matrix, x1, y0, z1, u1, v1, 0, 0, 1, packedLight, packedOverlay);
        vertex(consumer, matrix, x1, y1, z1, u1, v0, 0, 0, 1, packedLight, packedOverlay);
        vertex(consumer, matrix, x0, y1, z1, u0, v0, 0, 0, 1, packedLight, packedOverlay);
        // 西（-X）
        vertex(consumer, matrix, x0, y0, z0, uz0, v1, -1, 0, 0, packedLight, packedOverlay);
        vertex(consumer, matrix, x0, y0, z1, uz1, v1, -1, 0, 0, packedLight, packedOverlay);
        vertex(consumer, matrix, x0, y1, z1, uz1, v0, -1, 0, 0, packedLight, packedOverlay);
        vertex(consumer, matrix, x0, y1, z0, uz0, v0, -1, 0, 0, packedLight, packedOverlay);
        // 东（+X）
        vertex(consumer, matrix, x1, y0, z0, uz0, v1, 1, 0, 0, packedLight, packedOverlay);
        vertex(consumer, matrix, x1, y1, z0, uz0, v0, 1, 0, 0, packedLight, packedOverlay);
        vertex(consumer, matrix, x1, y1, z1, uz1, v0, 1, 0, 0, packedLight, packedOverlay);
        vertex(consumer, matrix, x1, y0, z1, uz1, v1, 1, 0, 0, packedLight, packedOverlay);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose matrix, float x, float y, float z,
                               float u, float v, float nx, float ny, float nz, int packedLight, int packedOverlay) {
        consumer.addVertex(matrix, x, y, z)
                .setColor(1.0F, 1.0F, 1.0F, 1.0F)
                .setUv(u, v)
                .setOverlay(packedOverlay)
                .setLight(packedLight)
                .setNormal(nx, ny, nz);
    }

    /** 把贴图坐标因子夹到「最外圈像素的中心」之间，避免采样到图集里隔壁的贴图 */
    private static float clampTexel(float value, float halfTexel) {
        return Math.max(halfTexel, Math.min(value, 1.0F - halfTexel));
    }
}
