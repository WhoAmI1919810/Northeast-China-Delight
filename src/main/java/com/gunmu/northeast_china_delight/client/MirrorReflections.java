package com.gunmu.northeast_china_delight.client;

import com.gunmu.northeast_china_delight.NortheastChinaDelight;
import com.gunmu.northeast_china_delight.block.BasinStandBlock;
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
import net.minecraft.client.renderer.LevelRenderer;
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
 *   <li>镜子前面的活物照镜面对称画一遍，只画在圈出来的那块里，一个活物一个模板编号（1 | 序号 &lt;&lt; 1）；
 *       对称一下三角形绕向反了，画的时候把正面改成顺时针；</li>
 *   <li>每个活物上盖一层镜子贴图（同方块一样用光照贴图、乘朝向的面亮度，和模型里那片镜子一个颜色）：
 *       离镜子 {@link #CLEAR_DISTANCE} 格以内盖 {@link #GLOSS}，像隔着一层光滑的玻璃；再远越盖越实，
 *       到 {@link #FADE_DISTANCE} 格完全盖住、和不照一样，走近时倒影慢慢浮出来，不会突然冒出来
 *       （2026-11-06 用户：玩家越远照到的越不清晰，和完全不显示衔接一下，3 格以内最清晰）。
 *       人离镜子 {@link #CAMERA_CLEAR_DISTANCE}..{@link #MAX_DISTANCE} 格之间也照这样整体淡掉；</li>
 *   <li>这块的深度改回镜面本身的深度，后面画的粒子等镜子照样挡得住（画完活物要重新打开深度测试，原版画实体会把它关掉）；
 *       「极佳」里放着水盆的，没照到活物的地方深度写在镜面稍后，让半透明层里的镜子合成时盖上来。</li>
 * </ol>
 *
 * <p>只在图像品质「高品质」「极佳」里照，「流畅」里不照，镜子就是原来那张贴图（用户 2026-11-06）。
 * 主画面原来没有模板缓冲，客户端初始化时开（{@link #enableStencil}）。开着光影包（Iris / Oculus）时不画：
 * 光影包用自己的画面，模板那一套对不上，会画乱。</p>
 *
 * <p>装了 Iris / Oculus 就整个不开（2026-11-08 有人反馈装了东北乐事、开光影后什么都看不见）：以前不管装没装光影模组，
 * 初始化时都把主画面的深度缓冲换成「深度 + 模板」格式，这是东北乐事对光影画面唯一的改动。新版 Iris、Oculus 认这种格式，
 * 可旧版、别的光影实现未必，于是只要装了光影模组就不开模板缓冲、也不照（{@link #render} 见到没模板缓冲就不画），
 * 镜子就是原来那张贴图。</p>
 */
public final class MirrorReflections {

    /** 扫多大范围（以相机为中心，水平半径、上下各几格）、多久扫一次（游戏刻） */
    private static final int SCAN_RADIUS = 12;
    private static final int SCAN_HEIGHT = 5;
    private static final int RESCAN_TICKS = 10;
    /** 一次最多照几面镜子、每面最多照几个活物（模板编号用到 1 | 12 << 1 = 25，八位够） */
    private static final int MAX_MIRRORS = 2;
    private static final int MAX_ENTITIES = 12;
    /** 活物离镜子多远：这以内最清楚、到这儿完全看不见（中间平滑过渡） */
    private static final double CLEAR_DISTANCE = 3.0;
    private static final double FADE_DISTANCE = 8.0;
    /** 人（相机）离镜子多远：这以内不淡、到这儿完全不照 */
    private static final double CAMERA_CLEAR_DISTANCE = 9.0;
    private static final double MAX_DISTANCE = 12.0;

    /** 镜面在模型里的位置（basin_stand.json，朝北那份）：x 2.75..13.25、y 22.75..26.75，正面在 z 13.9 */
    private static final double HALF_WIDTH = (13.25 - 2.75) / 2.0 / 16.0;
    private static final double Y0 = 22.75 / 16.0;
    private static final double Y1 = 26.75 / 16.0;
    private static final double BACK = 13.9 / 16.0 - 0.5;
    /** 镜子贴图在镜面上用的那块（模型里的 uv）：u 2.75..13.25、v 0..4 */
    private static final float TEX_U0 = 2.75F / 16.0F;
    private static final float TEX_U1 = 13.25F / 16.0F;
    private static final float TEX_V1 = 4.0F / 16.0F;
    /** 离得近时盖在照出来的活物上那层镜子贴图有多实 */
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

    /** 主画面加模板缓冲（原版默认不带）；客户端初始化时在渲染线程调一次。装了光影模组就不开 */
    public static void enableStencil() {
        if (shaderModLoaded()) {
            return;
        }
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
            Mirror mirror = Mirror.of(pos, state.getValue(FacingDecorBlock.FACING),
                    state.getValue(BasinStandBlock.BASIN) == BasinStandBlock.Basin.WATER);
            if (mirror.side(cam) > 0.02 && mirror.center.distanceTo(cam) <= MAX_DISTANCE && frustum.isVisible(mirror.box())) {
                mirrors.add(mirror);
            }
        }
        mirrors.sort(Comparator.comparingDouble(m -> m.center.distanceToSqr(cam)));
        for (int i = 0; i < Math.min(MAX_MIRRORS, mirrors.size()); i++) {
            Mirror mirror = mirrors.get(i);
            float cameraFade = fade(mirror.center.distanceTo(cam), CAMERA_CLEAR_DISTANCE, MAX_DISTANCE);
            List<Seen> seen = mirror.entities(level, cameraFade);
            if (!seen.isEmpty()) {
                draw(mirror, seen, poseStack, cam, level, partialTick);
            }
        }
    }

    /** 清晰度：near 以内 1，far 以外 0，中间平滑（smoothstep）过渡 */
    private static float fade(double distance, double near, double far) {
        double t = Mth.clamp((distance - near) / (far - near), 0.0, 1.0);
        return (float) (1.0 - t * t * (3.0 - 2.0 * t));
    }

    /** 第 i 个活物的模板编号：第 0 位是「镜面里」，往上几位是序号 */
    private static int stencilRef(int i) {
        return 1 | ((i + 1) << 1);
    }

    private static void draw(Mirror mirror, List<Seen> seen, PoseStack poseStack, Vec3 cam,
                             ClientLevel level, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        mc.getMainRenderTarget().bindWrite(false);
        Matrix4f pose = poseStack.last().pose();
        // 镜面四个角（相对相机），往镜子正面挪一点点，免得和模型里那片镜子深度打架
        Vec3 shift = mirror.normal.scale(0.003).subtract(cam);
        Vec3[] corners = {mirror.corner(1, 1).add(shift), mirror.corner(-1, 1).add(shift),
                mirror.corner(-1, -1).add(shift), mirror.corner(1, -1).add(shift)};
        // 镜面稍后面一点（第 5 步「极佳」里放着水盆时用）
        Vec3 back = mirror.normal.scale(-0.01).subtract(cam);
        Vec3[] behind = {mirror.corner(1, 1).add(back), mirror.corner(-1, 1).add(back),
                mirror.corner(-1, -1).add(back), mirror.corner(1, -1).add(back)};

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

        // 3. 照出来的活物：只画在模板第 0 位是 1 的地方，画到的地方模板写成这个活物的编号（drawEntities 里一个一个设）
        RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE);
        RenderSystem.enableCull();
        GL11.glFrontFace(GL11.GL_CW);
        drawEntities(mirror, seen, poseStack, cam, partialTick);
        GL11.glFrontFace(GL11.GL_CCW);
        RenderSystem.disableCull();
        // 原版画完一批实体会把深度测试关掉（RenderStateShard.DepthTestStateShard 的 clearRenderState）。
        // 深度测试关着的时候 OpenGL 根本不写深度，第 5 步写回镜面深度就白写了：镜子那块深度停在「最远」，
        // 后画的粒子、玉米串辣椒串从镜子后面透出来（2026-11-06 用户报），这里重新打开
        RenderSystem.enableDepthTest();

        // 4. 每个活物上盖一层镜子贴图：近的薄（光滑的镜面），远的厚，到头完全盖住
        RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
        RenderSystem.depthMask(false);
        RenderSystem.depthFunc(GL11.GL_ALWAYS);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int light = LevelRenderer.getLightColor(level, mirror.pos.above());
        float shade = level.getShade(mirror.facing, true);
        for (int i = 0; i < seen.size(); i++) {
            RenderSystem.stencilFunc(GL11.GL_EQUAL, stencilRef(i), 0xFF);
            glossQuad(pose, corners, light, shade, GLOSS + (1.0F - GLOSS) * (1.0F - seen.get(i).fade()));
        }
        RenderSystem.disableBlend();

        // 5. 镜面那块的深度改回镜面本身
        RenderSystem.colorMask(false, false, false, false);
        RenderSystem.depthMask(true);
        RenderSystem.stencilFunc(GL11.GL_EQUAL, 1, 0x01);
        quad(pose, corners);
        if (mirror.translucent && Minecraft.useShaderTransparency()) {
            // 「极佳」里放着水盆的脸盆架：整个模型在半透明那一层的画面上，镜子最后合成时才盖到主画面上（按深度比远近）。
            // 没照到活物的地方（模板正好是 1）深度改写在镜面稍后面，让那一层的镜子盖上来（不然露出主画面里镜子后面的东西）；
            // 照到活物的地方留在镜面前面，倒影留在镜子上面
            RenderSystem.stencilFunc(GL11.GL_EQUAL, 1, 0xFF);
            quad(pose, behind);
        }

        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.depthMask(true);
        RenderSystem.stencilFunc(GL11.GL_ALWAYS, 0, 0xFF);
        RenderSystem.clear(GL11.GL_STENCIL_BUFFER_BIT, Minecraft.ON_OSX);
        GL11.glDisable(GL11.GL_STENCIL_TEST);
        RenderSystem.enableCull();
    }

    /** 活物照镜面对称画一遍：以镜面为界翻过去（镜面都是正南正北或正东正西，翻一个轴就行） */
    private static void drawEntities(Mirror mirror, List<Seen> seen, PoseStack poseStack, Vec3 cam,
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
            for (int i = 0; i < seen.size(); i++) {
                LivingEntity entity = seen.get(i).entity();
                double x = Mth.lerp(partialTick, entity.xOld, entity.getX()) - cam.x;
                double y = Mth.lerp(partialTick, entity.yOld, entity.getY()) - cam.y;
                double z = Mth.lerp(partialTick, entity.zOld, entity.getZ()) - cam.z;
                float yaw = Mth.lerp(partialTick, entity.yRotO, entity.getYRot());
                // 模板状态在真正画（endBatch）的时候才起作用，所以一个活物画完就 endBatch
                RenderSystem.stencilFunc(GL11.GL_EQUAL, stencilRef(i), 0x01);
                dispatcher.render(entity, x, y, z, yaw, partialTick, poseStack, buffers,
                        dispatcher.getPackedLightCoords(entity, partialTick));
                buffers.endBatch();
            }
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
     * 站在镜子前面看是左手边），模型里镜面正面 u 2.75 在这一边。
     * 颜色同方块：光照贴图（light）× 朝向的面亮度（shade），完全盖住时和模型里那片镜子一个样
     */
    private static void glossQuad(Matrix4f pose, Vec3[] corners, int light, float shade, float alpha) {
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(MIRROR_TEXTURE);
        float u0 = Mth.lerp(TEX_U0, sprite.getU0(), sprite.getU1());
        float u1 = Mth.lerp(TEX_U1, sprite.getU0(), sprite.getU1());
        float v0 = sprite.getV0();
        float v1 = Mth.lerp(TEX_V1, sprite.getV0(), sprite.getV1());
        float[][] uv = {{u0, v0}, {u1, v0}, {u1, v1}, {u0, v1}};
        RenderSystem.setShader(GameRenderer::getPositionColorTexLightmapShader);
        RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);
        Minecraft.getInstance().gameRenderer.lightTexture().turnOnLightLayer();
        //? if >=1.20.5 {
        /*BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP);
        for (int i = 0; i < 4; i++) {
            builder.addVertex(pose, (float) corners[i].x, (float) corners[i].y, (float) corners[i].z)
                    .setColor(shade, shade, shade, alpha).setUv(uv[i][0], uv[i][1]).setLight(light);
        }
        BufferUploader.drawWithShader(builder.buildOrThrow());
        *///?} else {
        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP);
        for (int i = 0; i < 4; i++) {
            builder.vertex(pose, (float) corners[i].x, (float) corners[i].y, (float) corners[i].z)
                    .color(shade, shade, shade, alpha).uv(uv[i][0], uv[i][1]).uv2(light).endVertex();
        }
        BufferUploader.drawWithShader(builder.end());
        //?}
        Minecraft.getInstance().gameRenderer.lightTexture().turnOffLightLayer();
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
    /** 装没装光影模组（Iris、Oculus），装了就不碰主画面的格式 */
    private static boolean shaderModLoaded() {
        //? if <1.20.2 {
        net.minecraftforge.fml.ModList mods = net.minecraftforge.fml.ModList.get();
        //?} else {
        /*net.neoforged.fml.ModList mods = net.neoforged.fml.ModList.get();
        *///?}
        return mods.isLoaded("iris") || mods.isLoaded("oculus");
    }

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
    private record Mirror(BlockPos pos, Direction facing, Vec3 center, Vec3 normal, Vec3 right, boolean translucent) {

        /** translucent：放着水盆，整个脸盆架模型在半透明那一层画 */
        static Mirror of(BlockPos pos, Direction facing, boolean translucent) {
            Vec3 normal = Vec3.atLowerCornerOf(facing.getNormal());
            Vec3 right = Vec3.atLowerCornerOf(facing.getClockWise().getNormal());
            Vec3 center = new Vec3(pos.getX() + 0.5, pos.getY() + (Y0 + Y1) / 2.0, pos.getZ() + 0.5)
                    .subtract(normal.scale(BACK));
            return new Mirror(pos, facing, center, normal, right, translucent);
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

        /** 镜子正面看得见的活物和各自的清晰度（离镜面中心越远越淡，再乘上人离镜子远近的 cameraFade），近的在前 */
        List<Seen> entities(ClientLevel level, float cameraFade) {
            AABB area = new AABB(center, center).inflate(FADE_DISTANCE, 4.0, FADE_DISTANCE);
            List<Seen> found = new ArrayList<>();
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, area,
                    x -> x.isAlive() && side(x.getBoundingBox().getCenter()) > 0.05)) {
                float f = fade(e.getBoundingBox().getCenter().distanceTo(center), CLEAR_DISTANCE, FADE_DISTANCE) * cameraFade;
                if (f > 0.004F) {
                    found.add(new Seen(e, f));
                }
            }
            found.sort(Comparator.comparingDouble(s -> s.entity().distanceToSqr(center)));
            return found.size() > MAX_ENTITIES ? new ArrayList<>(found.subList(0, MAX_ENTITIES)) : found;
        }
    }

    /** 照得进去的一个活物和它的清晰度（1 最清楚，0 看不见） */
    private record Seen(LivingEntity entity, float fade) {
    }
}
