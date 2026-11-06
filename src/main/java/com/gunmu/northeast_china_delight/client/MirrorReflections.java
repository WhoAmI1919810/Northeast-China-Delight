package com.gunmu.northeast_china_delight.client;

import com.gunmu.northeast_china_delight.NortheastChinaDelight;
import com.gunmu.northeast_china_delight.block.FacingDecorBlock;
import com.gunmu.northeast_china_delight.block.ModBlocks;
import com.gunmu.northeast_china_delight.util.DdIds;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
//? if >=1.20.5 {
/*import com.mojang.blaze3d.vertex.ByteBufferBuilder;
*///?}
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 脸盆架上的小镜子照出镜子前面的活物（2026-11-06 用户：尝试让脸盆架做只反射活物的效果；镜子的贴图也要保留，显示一种光滑的材质）。
 *
 * <p>只照活物（玩家、村民、动物、怪），不照方块：镜面底下还是原来那张镜子贴图，活物左右颠倒地映在上面，
 * 再盖一层半透明的镜子贴图，像隔着一层光滑的玻璃。不用方块实体：客户端每半秒扫一遍身边的脸盆架记下位置，
 * 老存档里早就放着的、结构里生成的脸盆架也都照得出来。</p>
 *
 * <p>在「半透明方块画完」那一步（RenderLevelStageEvent AFTER_TRANSLUCENT_BLOCKS）画。放着水盆的脸盆架整个模型在半透明那一层，
 * 镜子到这时才画上；以前在「方块实体画完」时画，倒影画完后写回的深度比镜面靠前一点，后来画的镜子过不了深度测试，
 * 放了水盆就看不见镜子、透出后面的墙和窗（2026-11-06 用户报）。对离得近、看得见、人站在镜子正面的几面镜子：</p>
 * <ol>
 *   <li>模板缓冲把镜面没被挡住的那块圈出来（写 1）；</li>
 *   <li>这块的深度清到最远（glDepthRange(1, 1)）；</li>
 *   <li>镜子前面的活物照镜面对称画一遍，只画在圈出来的那块里，画到的地方模板改成 3；
 *       对称一下三角形绕向反了，画的时候把正面改成顺时针；</li>
 *   <li>模板为 3 的地方盖一层半透明的镜子贴图，亮度跟着那里的光照；</li>
 *   <li>这块的深度改回镜面本身的深度，后面画半透明方块、粒子时镜子照样挡得住。</li>
 * </ol>
 *
 * <p>只在图像品质「高品质」「极佳」里照，「流畅」里不照，镜子就是原来那张贴图（用户 2026-11-06）。
 * 主画面原来没有模板缓冲，客户端初始化时开（{@link #enableStencil}）。开着光影包（Iris / Oculus）时不画：
 * 光影包用自己的画面，模板那一套对不上，会画乱。</p>
 */
public final class MirrorReflections {

    /** 扫多大范围（以相机为中心，水平半径、上下各几格）、多久扫一次（游戏刻） */
    private static final int SCAN_RADIUS = 12;
    private static final int SCAN_HEIGHT = 5;
    private static final int RESCAN_TICKS = 10;
    /** 离镜子多远以内才照、一次最多照几面镜子、每面最多照几个活物、镜子前面多远的活物照得进去 */
    private static final double MAX_DISTANCE = 12.0;
    private static final int MAX_MIRRORS = 2;
    private static final int MAX_ENTITIES = 12;
    private static final double REACH = 8.0;

    /** 镜面在模型里的位置（basin_stand.json，朝北那份）：x 2.75..13.25、y 22.75..26.75，正面在 z 13.9 */
    private static final double HALF_WIDTH = (13.25 - 2.75) / 2.0 / 16.0;
    private static final double Y0 = 22.75 / 16.0;
    private static final double Y1 = 26.75 / 16.0;
    private static final double BACK = 13.9 / 16.0 - 0.5;
    /** 镜子贴图在镜面上用的那块（模型里的 uv）：u 2.75..13.25、v 0..4 */
    private static final float TEX_U0 = 2.75F / 16.0F;
    private static final float TEX_U1 = 13.25F / 16.0F;
    private static final float TEX_V1 = 4.0F / 16.0F;
    /** 盖在照出来的活物上那层镜子贴图有多实 */
    private static final float GLOSS = 0.35F;
    private static final ResourceLocation MIRROR_TEXTURE = DdIds.of(NortheastChinaDelight.MODID, "block/mirror");

    private static final List<BlockPos> STANDS = new ArrayList<>();
    private static ClientLevel scannedLevel;
    private static long lastScan;

    private static boolean irisChecked;
    private static Object irisApi;
    private static Method irisShaderPackInUse;

    //? if >=1.20.5 {
    /*private static ByteBufferBuilder entityBuffer;
    *///?} else {
    private static BufferBuilder entityBuffer;
    //?}

    private MirrorReflections() {
    }

    /** 主画面加模板缓冲（原版默认不带）；客户端初始化时在渲染线程调一次 */
    public static void enableStencil() {
        RenderTarget target = Minecraft.getInstance().getMainRenderTarget();
        if (!target.isStencilEnabled()) {
            target.enableStencil();
        }
    }

    /** RenderLevelStageEvent AFTER_TRANSLUCENT_BLOCKS 时调；poseStack 的坐标以相机为原点（1.21.1 那一步不给，调用处新建空的） */
    public static void render(PoseStack poseStack, Camera camera, Frustum frustum, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        // 图像品质「流畅」时不照（2026-11-06 用户：只在高品质和极佳里反光，流畅里就和没有一样）
        if (level == null || mc.options.graphicsMode().get() == GraphicsStatus.FAST
                || !mc.getMainRenderTarget().isStencilEnabled() || shaderPackInUse()) {
            return;
        }
        rescan(level, camera);
        if (STANDS.isEmpty()) {
            return;
        }
        Vec3 cam = camera.getPosition();
        List<Mirror> mirrors = new ArrayList<>();
        for (BlockPos pos : STANDS) {
            BlockState state = level.getBlockState(pos);
            if (!state.is(ModBlocks.BASIN_STAND.get())) {
                continue;
            }
            Mirror mirror = Mirror.of(pos, state.getValue(FacingDecorBlock.FACING));
            if (mirror.side(cam) > 0.02 && mirror.center.distanceTo(cam) <= MAX_DISTANCE && frustum.isVisible(mirror.box())) {
                mirrors.add(mirror);
            }
        }
        mirrors.sort(Comparator.comparingDouble(m -> m.center.distanceToSqr(cam)));
        for (int i = 0; i < Math.min(MAX_MIRRORS, mirrors.size()); i++) {
            Mirror mirror = mirrors.get(i);
            List<LivingEntity> entities = mirror.entities(level);
            if (!entities.isEmpty()) {
                draw(mirror, entities, poseStack, cam, level, partialTick);
            }
        }
    }

    private static void draw(Mirror mirror, List<LivingEntity> entities, PoseStack poseStack, Vec3 cam,
                             ClientLevel level, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        mc.getMainRenderTarget().bindWrite(false);
        Matrix4f pose = poseStack.last().pose();
        // 镜面四个角（相对相机），往镜子正面挪一点点，免得和模型里那片镜子深度打架
        Vec3 shift = mirror.normal.scale(0.003).subtract(cam);
        Vec3[] corners = {mirror.corner(1, 1).add(shift), mirror.corner(-1, 1).add(shift),
                mirror.corner(-1, -1).add(shift), mirror.corner(1, -1).add(shift)};

        RenderSystem.stencilMask(0xFF);
        RenderSystem.clearStencil(0);
        RenderSystem.clear(GL11.GL_STENCIL_BUFFER_BIT, Minecraft.ON_OSX);
        GL11.glEnable(GL11.GL_STENCIL_TEST);
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();

        // 1. 镜面没被挡住的那块：模板写 1；只做深度测试，不写颜色、不写深度
        RenderSystem.colorMask(false, false, false, false);
        RenderSystem.depthMask(false);
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.stencilFunc(GL11.GL_ALWAYS, 1, 0xFF);
        RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE);
        quad(pose, corners);

        // 2. 这块的深度清到最远，照出来的活物只和彼此比远近
        RenderSystem.stencilFunc(GL11.GL_EQUAL, 1, 0xFF);
        RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
        RenderSystem.depthMask(true);
        RenderSystem.depthFunc(GL11.GL_ALWAYS);
        GL11.glDepthRange(1.0, 1.0);
        quad(pose, corners);
        GL11.glDepthRange(0.0, 1.0);
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.colorMask(true, true, true, true);

        // 3. 照出来的活物：只画在模板第 0 位是 1 的地方，画到的地方模板改成 3
        RenderSystem.stencilFunc(GL11.GL_EQUAL, 3, 0x01);
        RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE);
        RenderSystem.enableCull();
        GL11.glFrontFace(GL11.GL_CW);
        drawEntities(mirror, entities, poseStack, cam, partialTick);
        GL11.glFrontFace(GL11.GL_CCW);
        RenderSystem.disableCull();

        // 4. 活物上面盖一层半透明的镜子贴图：光滑的镜面
        RenderSystem.stencilFunc(GL11.GL_EQUAL, 3, 0xFF);
        RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
        RenderSystem.depthMask(false);
        RenderSystem.depthFunc(GL11.GL_ALWAYS);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        float light = brightness(level, mirror.pos.above());
        RenderSystem.setShaderColor(light, light, light, GLOSS);
        glossQuad(pose, corners);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();

        // 5. 镜面那块的深度改回镜面本身
        RenderSystem.stencilFunc(GL11.GL_EQUAL, 1, 0x01);
        RenderSystem.colorMask(false, false, false, false);
        RenderSystem.depthMask(true);
        quad(pose, corners);

        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.depthMask(true);
        RenderSystem.stencilFunc(GL11.GL_ALWAYS, 0, 0xFF);
        RenderSystem.clear(GL11.GL_STENCIL_BUFFER_BIT, Minecraft.ON_OSX);
        GL11.glDisable(GL11.GL_STENCIL_TEST);
        RenderSystem.enableCull();
    }

    /** 活物照镜面对称画一遍：以镜面为界翻过去（镜面都是正南正北或正东正西，翻一个轴就行） */
    private static void drawEntities(Mirror mirror, List<LivingEntity> entities, PoseStack poseStack, Vec3 cam,
                                     float partialTick) {
        EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        MultiBufferSource.BufferSource buffers = bufferSource();
        Vec3 c = mirror.center.subtract(cam);
        poseStack.pushPose();
        poseStack.translate(c.x, c.y, c.z);
        poseStack.scale(mirror.normal.x != 0 ? -1.0F : 1.0F, 1.0F, mirror.normal.z != 0 ? -1.0F : 1.0F);
        poseStack.translate(-c.x, -c.y, -c.z);
        dispatcher.setRenderShadow(false);
        try {
            for (LivingEntity entity : entities) {
                double x = Mth.lerp(partialTick, entity.xOld, entity.getX()) - cam.x;
                double y = Mth.lerp(partialTick, entity.yOld, entity.getY()) - cam.y;
                double z = Mth.lerp(partialTick, entity.zOld, entity.getZ()) - cam.z;
                float yaw = Mth.lerp(partialTick, entity.yRotO, entity.getYRot());
                dispatcher.render(entity, x, y, z, yaw, partialTick, poseStack, buffers,
                        dispatcher.getPackedLightCoords(entity, partialTick));
            }
            buffers.endBatch();
        } finally {
            dispatcher.setRenderShadow(true);
            poseStack.popPose();
        }
    }

    private static MultiBufferSource.BufferSource bufferSource() {
        //? if >=1.20.5 {
        /*if (entityBuffer == null) {
            entityBuffer = new ByteBufferBuilder(1536);
        }
        *///?} else {
        if (entityBuffer == null) {
            entityBuffer = new BufferBuilder(1536);
        }
        //?}
        return MultiBufferSource.immediate(entityBuffer);
    }

    /** 只要位置的四边形（模板、深度用，不写颜色） */
    private static void quad(Matrix4f pose, Vec3[] corners) {
        RenderSystem.setShader(GameRenderer::getPositionShader);
        //? if >=1.20.5 {
        /*BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        for (Vec3 v : corners) {
            builder.addVertex(pose, (float) v.x, (float) v.y, (float) v.z);
        }
        BufferUploader.drawWithShader(builder.buildOrThrow());
        *///?} else {
        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        for (Vec3 v : corners) {
            builder.vertex(pose, (float) v.x, (float) v.y, (float) v.z).endVertex();
        }
        BufferUploader.drawWithShader(builder.end());
        //?}
    }

    /**
     * 镜子贴图那块（同模型里的 uv）贴在镜面上。四个角的顺序同 {@link #draw}：右上、左上、左下、右下（「右」是朝向顺时针那边，
     * 站在镜子前面看是左手边），模型里镜面正面 u 2.75 在这一边
     */
    private static void glossQuad(Matrix4f pose, Vec3[] corners) {
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(MIRROR_TEXTURE);
        float u0 = Mth.lerp(TEX_U0, sprite.getU0(), sprite.getU1());
        float u1 = Mth.lerp(TEX_U1, sprite.getU0(), sprite.getU1());
        float v0 = sprite.getV0();
        float v1 = Mth.lerp(TEX_V1, sprite.getV0(), sprite.getV1());
        float[][] uv = {{u0, v0}, {u1, v0}, {u1, v1}, {u0, v1}};
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);
        //? if >=1.20.5 {
        /*BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        for (int i = 0; i < 4; i++) {
            builder.addVertex(pose, (float) corners[i].x, (float) corners[i].y, (float) corners[i].z).setUv(uv[i][0], uv[i][1]);
        }
        BufferUploader.drawWithShader(builder.buildOrThrow());
        *///?} else {
        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        for (int i = 0; i < 4; i++) {
            builder.vertex(pose, (float) corners[i].x, (float) corners[i].y, (float) corners[i].z).uv(uv[i][0], uv[i][1]).endVertex();
        }
        BufferUploader.drawWithShader(builder.end());
        //?}
    }

    /** 盖上去那层贴图的亮度：跟着镜子那儿的光照（夜里不能比镜子本身还亮） */
    private static float brightness(ClientLevel level, BlockPos pos) {
        int sky = level.getBrightness(LightLayer.SKY, pos) - level.getSkyDarken();
        int block = level.getBrightness(LightLayer.BLOCK, pos);
        return Mth.clamp(Math.max(sky, block) / 15.0F, 0.15F, 1.0F);
    }

    /** 每半秒扫一遍相机周围的脸盆架 */
    private static void rescan(ClientLevel level, Camera camera) {
        long now = level.getGameTime();
        if (level == scannedLevel && now >= lastScan && now - lastScan < RESCAN_TICKS) {
            return;
        }
        scannedLevel = level;
        lastScan = now;
        STANDS.clear();
        BlockPos center = camera.getBlockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-SCAN_RADIUS, -SCAN_HEIGHT, -SCAN_RADIUS),
                center.offset(SCAN_RADIUS, SCAN_HEIGHT, SCAN_RADIUS))) {
            if (level.getBlockState(pos).is(ModBlocks.BASIN_STAND.get())) {
                STANDS.add(pos.immutable());
            }
        }
    }

    /** Iris / Oculus 开着光影包时不画；没装就当没开（反射拿，免得编译时依赖它们） */
    private static boolean shaderPackInUse() {
        if (!irisChecked) {
            irisChecked = true;
            try {
                Class<?> api = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
                irisApi = api.getMethod("getInstance").invoke(null);
                irisShaderPackInUse = api.getMethod("isShaderPackInUse");
            } catch (Throwable ignored) {
                irisApi = null;
            }
        }
        if (irisApi == null) {
            return false;
        }
        try {
            return (Boolean) irisShaderPackInUse.invoke(irisApi);
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * 一面镜子：镜面中心、朝外的法线（同脸盆架的朝向，镜子朝着放的人）、横向（朝向顺时针那边）。
     * 镜面中心 = 方块中心往朝向反方向挪 {@link #BACK}，高度在方块上面 {@link #Y0}..{@link #Y1}
     */
    private record Mirror(BlockPos pos, Vec3 center, Vec3 normal, Vec3 right) {

        static Mirror of(BlockPos pos, Direction facing) {
            Vec3 normal = Vec3.atLowerCornerOf(facing.getNormal());
            Vec3 right = Vec3.atLowerCornerOf(facing.getClockWise().getNormal());
            Vec3 center = new Vec3(pos.getX() + 0.5, pos.getY() + (Y0 + Y1) / 2.0, pos.getZ() + 0.5)
                    .subtract(normal.scale(BACK));
            return new Mirror(pos, center, normal, right);
        }

        /** 点在镜面哪一边、离镜面多远：正的是镜子正面 */
        double side(Vec3 point) {
            return point.subtract(center).dot(normal);
        }

        /** (r, u) 各取 ±1：镜面四个角 */
        Vec3 corner(double r, double u) {
            return center.add(right.scale(r * HALF_WIDTH)).add(0.0, u * (Y1 - Y0) / 2.0, 0.0);
        }

        AABB box() {
            return new AABB(corner(-1, -1), corner(1, 1)).inflate(0.05);
        }

        /** 镜子正面一定范围里的活物，近的在前 */
        List<LivingEntity> entities(ClientLevel level) {
            AABB area = new AABB(center, center).inflate(REACH, 4.0, REACH);
            List<LivingEntity> found = level.getEntitiesOfClass(LivingEntity.class, area,
                    e -> e.isAlive() && side(e.getBoundingBox().getCenter()) > 0.05);
            found.sort(Comparator.comparingDouble(e -> e.distanceToSqr(center)));
            return found.size() > MAX_ENTITIES ? new ArrayList<>(found.subList(0, MAX_ENTITIES)) : found;
        }
    }
}
