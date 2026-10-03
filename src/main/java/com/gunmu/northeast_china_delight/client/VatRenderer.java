package com.gunmu.northeast_china_delight.client;

import com.gunmu.northeast_china_delight.block.Vat;
import com.gunmu.northeast_china_delight.block.VatBlockEntity;
import com.gunmu.northeast_china_delight.crafting.VatRecipes;
import com.gunmu.northeast_china_delight.item.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.client.model.data.ModelData;

import java.util.List;

/**
 * 把大缸里的东西按实际物品画出来 —— 不打开界面也能看见缸里有什么。
 *
 * <p>内容物是**立体的**小模型（{@link VatItemShapes}）：一层放**两个** —— 缸里一层水配两份食材，
 * 每个小模型宽度是缸内宽度的一半，两个正好铺满一层；高度是缸内净高的三分之一略少，
 * 三层叠起来（上层直接压在下层上）就正好把缸填满。
 *
 * <p>**调料不建模**：盐抹在肉上让肉发白、辣椒酱 / 鱼露 / 虾酱泡进水里让水变色，
 * 它们自己不会作为一件物品出现在缸里。
 *
 * <p>压缸石压在内容物顶上（y 随内容物层数变化，会有"压下去"的视觉效果），
 * 蒙缸地毯盖在缸口，成品液体按剩余量决定液面高度。
 */
public class VatRenderer implements BlockEntityRenderer<VatBlockEntity> {

    /** 缸内地面高度（= 缸底那层方块的上表面，2/16） */
    private static final float FLOOR = 0.125F;
    /** 一层放几个：缸里一层水配两份食材，所以一层放两个 —— 两个正好铺满一层 */
    private static final int PER_LAYER = 2;
    /** 每个小模型的宽度：缸内宽度 0.75 的一半（0.375），留一点缝 */
    private static final float ITEM_W = 0.35F;
    /**
     * 每个小模型的高度：缸内净高（地面 0.125 到缸口 1.0 = 0.875）的三分之一略少一点。
     * 三层叠起来正好把缸填满，上层直接压在下层上（层高 = 模型高度）。
     */
    private static final float ITEM_H = 0.28F;
    private static final float LAYER_H = ITEM_H;

    public VatRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(VatBlockEntity vat, float partialTick, PoseStack pose, MultiBufferSource buffer,
                       int packedLight, int packedOverlay) {
        Level level = vat.getLevel();
        if (level == null) {
            return;
        }
        int light = LevelRenderer.getLightColor(level, vat.getBlockPos().above());
        BlockRenderDispatcher blockRenderer = Minecraft.getInstance().getBlockRenderer();

        boolean fermented = vat.getBlockState().getValue(Vat.FERMENTED);

        // ===== 成品液体：按剩余量决定液面高度 =====
        if (fermented) {
            var liquid = liquidOf(vat);
            if (liquid != null) {
                float ratio = liquid.ratio();
                float height = 0.15F + 0.7F * ratio;
                pose.pushPose();
                pose.translate(0.14F, 0.10F, 0.14F);
                pose.scale(0.72F, height, 0.72F);
                blockRenderer.renderSingleBlock(liquid.state(), pose, buffer, light, packedOverlay,
                        ModelData.EMPTY, null);
                pose.popPose();
            }
        }

        // ===== 内容物：一层三个，立体小模型 =====
        boolean done = vat.getBlockState().getValue(Vat.FERMENTED);
        List<ItemStack> contents = vat.contents();
        org.slf4j.LoggerFactory.getLogger(VatRenderer.class).info("[VatRenderer] pos={} contents={} count={}",
                vat.getBlockPos(), contents.stream().map(s -> s.isEmpty() ? "empty" : s.getItem().toString()).toList(), contents.size());
        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
        // 干腌时抹上去的盐：让肉 / 鱼 的模型发白，而不是把盐本身画成一件东西
        float saltCoat = 0.0F;
        if (!vat.isSaltDissolved()) {
            int salt = vat.countOf(ModItems.SALT.get());
            int meaty = vat.meatCount() + vat.rawFishCount();
            if (salt > 0 && meaty > 0) {
                saltCoat = Math.min(1.0F, salt / (float) meaty) * 0.55F;
            }
        }
        // 抹在食材上的调料：辣椒酱 / 鱼露 / 虾酱会把缸里的菜、肉、鱼染上对应颜色
        int seasoningTint = 0xFFFFFF;
        float seasoningAmount = 0.0F;
        if (vat.chiliSauceCount() > 0) {
            seasoningTint = 0xE0553C;      // 辣椒酱：红
            seasoningAmount = 0.45F;
        } else if (vat.countOf(ModItems.FISH_SAUCE.get()) > 0) {
            seasoningTint = 0xB0703A;      // 鱼露：琥珀
            seasoningAmount = 0.34F;
        } else if (vat.countOf(ModItems.SHRIMP_PASTE.get()) > 0) {
            seasoningTint = 0xC0603A;      // 虾酱：虾红
            seasoningAmount = 0.34F;
        }
        int shown = 0;
        // 内容物顶面：按**实际画出来的最高食材**算（白菜比一层还高一点），
        // 压缸石就坐在这个高度上，绝不会和菜叶子/肉块重叠。
        float contentTop = FLOOR + LAYER_H;
        for (ItemStack content : contents) {
            if (content.isEmpty() || shown >= 12) {
                continue;
            }
            // 调料不建模：盐抹在肉上 → 肉变白；辣椒酱 / 鱼露 / 虾酱泡进水里 → 水变色。
            if (isSeasoning(content)) {
                continue;
            }
            // 发酵完成后酱块 / 小麦已经化进液体里
            if (done && (vat.kind() == VatRecipes.Kind.PASTE || vat.kind() == VatRecipes.Kind.SOY_SAUCE)) {
                if (content.is(ModItems.SOY_PASTE_CHUNK.get())) {
                    continue;
                }
                if (vat.kind() == VatRecipes.Kind.SOY_SAUCE && content.is(VatRecipes.wheatInput())) {
                    continue;
                }
            }
            // 腌好后缸里显示的是成品的样子
            ItemStack display = content;
            if (done) {
                // 问这一缸对应的配方：这一份取出来会变成什么（新增配方不用再改渲染）
                Item result = VatRecipes.resultFor(vat.kind(), content.getItem());
                if (result != null) {
                    display = new ItemStack(result);
                }
            }

            int layer = shown / PER_LAYER;
            int slot = shown % PER_LAYER;
            float x = 0.3125F + slot * 0.375F;
            float z = 0.5F;
            float y = FLOOR + layer * LAYER_H;
            // 白菜这类食材本身要长一截（heightScale > 1），整个模型按倍数往高里拉
            float shapeH = LAYER_H * VatItemShapes.heightScale(display);
            float shapeTop = y + shapeH;
            if (shapeTop > contentTop) {
                contentTop = shapeTop;
            }

            pose.pushPose();
            pose.translate(x, y, z);
            // 两份食材朝向错开一点，看起来不是复制粘贴（只转 90 度倍数，免得戳出缸壁）
            pose.mulPose(Axis.YP.rotationDegrees((shown % 2) * 90 + ((shown * 7) % 8) - 4));
            pose.translate(-ITEM_W * 0.5F, 0.0F, -ITEM_W * 0.5F);
            pose.scale(ITEM_W, shapeH, ITEM_W);
            renderShape(pose, buffer, display, itemRenderer, level, light, packedOverlay,
                    saltCoat, seasoningTint, seasoningAmount);
            pose.popPose();
            shown++;
        }

        // ===== 压缸石：压在内容物上（往下沉一点，做出压着的效果） =====
        if (vat.isPressed()) {
            ItemStack press = vat.press();
            float stoneH = 0.30F;
            // 石头**坐在**食材上面：底面刚好贴住内容物顶面，绝不和食材重叠。
            // 食材装满时石头会高出缸口一点 —— 真实的压缸石本来就是这样。
            float stoneTop = contentTop + stoneH + 0.01F;
            if (press.getItem() instanceof BlockItem blockItem) {
                float s = 0.55F;
                pose.pushPose();
                pose.translate(0.5F - s * 0.5F, stoneTop - stoneH, 0.5F - s * 0.5F);
                pose.scale(s, stoneH, s);
                blockRenderer.renderSingleBlock(blockItem.getBlock().defaultBlockState(), pose, buffer,
                        light, packedOverlay, ModelData.EMPTY, null);
                pose.popPose();
            } else {
                pose.pushPose();
                pose.translate(0.5F, stoneTop - 0.02F, 0.5F);
                pose.scale(0.45F, 0.45F, 0.45F);
                itemRenderer.renderStatic(press, ItemDisplayContext.GROUND, light, packedOverlay, pose, buffer, level, 0);
                pose.popPose();
            }
        }

        // ===== 蒙缸地毯：正好盖住缸口 =====
        if (vat.isCovered() && vat.cover().getItem() instanceof BlockItem blockItem) {
            pose.pushPose();
            pose.translate(0.14F, 0.94F, 0.14F);
            pose.scale(0.72F, 1.0F, 0.72F);
            blockRenderer.renderSingleBlock(blockItem.getBlock().defaultBlockState(), pose, buffer,
                    light, packedOverlay, ModelData.EMPTY, null);
            pose.popPose();
        }
    }

    /** 成品液体对应的方块与剩余比例 */
    private record Liquid(net.minecraft.world.level.block.state.BlockState state, float ratio) {
    }

    private static Liquid liquidOf(VatBlockEntity vat) {
        float cap = VatRecipes.PRODUCT_CAPACITY_MB;
        return switch (vat.kind()) {
            case PASTE -> vat.pasteMb() > 0
                    ? new Liquid(Blocks.BROWN_CONCRETE.defaultBlockState(), vat.pasteMb() / cap) : null;
            case SOY_SAUCE -> vat.soySauceMb() > 0
                    ? new Liquid(Blocks.BROWN_TERRACOTTA.defaultBlockState(), vat.soySauceMb() / cap) : null;
            case VINEGAR -> vat.vinegarMb() > 0
                    ? new Liquid(Blocks.RED_TERRACOTTA.defaultBlockState(), vat.vinegarMb() / cap) : null;
            case WHITE_VINEGAR -> vat.whiteVinegarMb() > 0
                    ? new Liquid(Blocks.WHITE_TERRACOTTA.defaultBlockState(), vat.whiteVinegarMb() / cap) : null;
            case FISH_SAUCE -> vat.fishSauceMb() > 0
                    ? new Liquid(Blocks.ORANGE_TERRACOTTA.defaultBlockState(), vat.fishSauceMb() / cap) : null;
            case SHRIMP_PASTE -> vat.shrimpPasteMb() > 0
                    ? new Liquid(Blocks.PINK_TERRACOTTA.defaultBlockState(), vat.shrimpPasteMb() / cap) : null;
            case KVASS -> vat.kvassMb() > 0
                    ? new Liquid(Blocks.TERRACOTTA.defaultBlockState(),
                            vat.kvassMb() / (float) (VatRecipes.KVASS_SERVINGS * VatRecipes.SERVING_MB)) : null;
            // 泡菜缸的水面由方块状态里的 water_level 模型画（液面高度、酸引水颜色都在那边），
            // 这里绝对不能再画一层，否则缸里会出现两层水面
            default -> null;
        };
    }

    // ===== 立体小模型 =====

    /** 调料：不建模，只影响食材 / 液体的样子 */
    private static boolean isSeasoning(ItemStack stack) {
        return stack.is(ModItems.SALT.get())
                || stack.is(ModItems.CHILI_SAUCE.get())
                || stack.is(ModItems.FISH_SAUCE.get())
                || stack.is(ModItems.SHRIMP_PASTE.get());
    }

    private static void renderShape(PoseStack pose, MultiBufferSource buffer, ItemStack stack,
                                    ItemRenderer itemRenderer, Level level, int light, int overlay,
                                    float saltCoat, int seasoningTint, float seasoningAmount) {
        BakedModel model = itemRenderer.getModel(stack, level, null, 0);
        TextureAtlasSprite sprite = model.getParticleIcon();
        if (sprite == null) {
            return;
        }
        // "往上加颜色"（盐抹白 / 调料染色）乘法 tint 做不到，只能用一层半透明色雾盖上去
        int washColor = seasoningAmount > 0.0F ? seasoningTint : 0xFFFFFF;
        float washAlpha = Math.max(saltCoat, seasoningAmount * 0.6F);

        // 注意：同一次渲染里不能一边画一边切换渲染类型 —— 切换会让上一个顶点消费者失效
        // （BufferBuilder 抛 IllegalStateException: Not building!）。所以分成两趟：
        // 第一趟把所有不透明面画完，第二趟再画色雾。
        var shapes = VatItemShapes.of(stack);
        var vc = buffer.getBuffer(RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS));
        for (VatItemShapes.Box box : shapes) {
            int tint = box.tint();
            if (seasoningAmount > 0.0F) {
                tint = blend(tint, seasoningTint, seasoningAmount);
            }
            VatItemShapes.Box tinted = tint != box.tint()
                    ? new VatItemShapes.Box(box.x0(), box.y0(), box.z0(), box.x1(), box.y1(), box.z1(),
                            tint)
                    : box;
            box(vc, pose.last(), sprite, tinted, light, overlay);
        }
        if (washAlpha > 0.02F) {
            var wash = buffer.getBuffer(RenderType.entityTranslucent(InventoryMenu.BLOCK_ATLAS));
            for (VatItemShapes.Box box : shapes) {
                boxWash(wash, pose.last(), sprite, box, light, overlay, washColor, washAlpha);
            }
        }
    }

    /** 用半透明色雾把同一个盒子再盖一遍（盐霜 / 调料裹一层） */
    private static void boxWash(com.mojang.blaze3d.vertex.VertexConsumer vc, PoseStack.Pose pose,
                                TextureAtlasSprite sprite, VatItemShapes.Box b, int light, int overlay,
                                int color, float alpha) {
        float r = ((color >> 16) & 0xFF) / 255.0F;
        float g = ((color >> 8) & 0xFF) / 255.0F;
        float bl = (color & 0xFF) / 255.0F;
        float x0 = b.x0(), y0 = b.y0(), z0 = b.z0(), x1 = b.x1(), y1 = b.y1(), z1 = b.z1();
        float vTop = sprite.getV(1.0F - y1);
        float vBot = sprite.getV(1.0F - y0);
        float uX0 = sprite.getU(x0), uX1 = sprite.getU(x1);
        float uZ0 = sprite.getU(z0), uZ1 = sprite.getU(z1);
        float vZ0 = sprite.getV(1.0F - z1), vZ1 = sprite.getV(1.0F - z0);
        quad(vc, pose, light, overlay, r, g, bl, alpha,
                x0, y1, z0, uX0, vZ0, x0, y1, z1, uX0, vZ1, x1, y1, z1, uX1, vZ1, x1, y1, z0, uX1, vZ0, 0, 1, 0);
        quad(vc, pose, light, overlay, r, g, bl, alpha,
                x0, y0, z1, uX0, vZ0, x0, y0, z0, uX0, vZ1, x1, y0, z0, uX1, vZ1, x1, y0, z1, uX1, vZ0, 0, -1, 0);
        quad(vc, pose, light, overlay, r, g, bl, alpha,
                x1, y1, z0, uX1, vTop, x1, y0, z0, uX1, vBot, x0, y0, z0, uX0, vBot, x0, y1, z0, uX0, vTop, 0, 0, -1);
        quad(vc, pose, light, overlay, r, g, bl, alpha,
                x0, y1, z1, uX0, vTop, x0, y0, z1, uX0, vBot, x1, y0, z1, uX1, vBot, x1, y1, z1, uX1, vTop, 0, 0, 1);
        quad(vc, pose, light, overlay, r, g, bl, alpha,
                x0, y1, z0, uZ0, vTop, x0, y0, z0, uZ0, vBot, x0, y0, z1, uZ1, vBot, x0, y1, z1, uZ1, vTop, -1, 0, 0);
        quad(vc, pose, light, overlay, r, g, bl, alpha,
                x1, y1, z1, uZ1, vTop, x1, y0, z1, uZ1, vBot, x1, y0, z0, uZ0, vBot, x1, y1, z0, uZ0, vTop, 1, 0, 0);
    }

    /** 按比例混色（和客户端液体着色用的是同一套算法） */
    private static int blend(int base, int tint, float amount) {
        float a = Math.max(0.0F, Math.min(1.0F, amount));
        int br = (base >> 16) & 0xFF, bg = (base >> 8) & 0xFF, bb = base & 0xFF;
        int tr = (tint >> 16) & 0xFF, tg = (tint >> 8) & 0xFF, tb = tint & 0xFF;
        int r = (int) (br + (tr - br) * a);
        int g = (int) (bg + (tg - bg) * a);
        int b = (int) (bb + (tb - bb) * a);
        return (r << 16) | (g << 8) | b;
    }

    /**
     * 画一个盒子：贴图取**物品图标里和这个盒子位置对应的那一小块**。
     *
     * <p>也就是说模型下部的盒子用图标下部的像素、上部的盒子用图标上部的像素，
     * 六个面各自按自己那一片取 UV，所以整体就是「把物品图标立起来」的效果，
     * 而不是每个面都糊一张完整图标。
     */
    private static void box(com.mojang.blaze3d.vertex.VertexConsumer vc, PoseStack.Pose pose,
                            TextureAtlasSprite sprite, VatItemShapes.Box b, int light, int overlay) {
        float r = ((b.tint() >> 16) & 0xFF) / 255.0F;
        float g = ((b.tint() >> 8) & 0xFF) / 255.0F;
        float bl = (b.tint() & 0xFF) / 255.0F;

        float x0 = b.x0(), y0 = b.y0(), z0 = b.z0(), x1 = b.x1(), y1 = b.y1(), z1 = b.z1();
        // 图标 v=0 在上；模型 y 越大越靠上
        float vTop = sprite.getV(1.0F - y1);
        float vBot = sprite.getV(1.0F - y0);
        float uX0 = sprite.getU(x0), uX1 = sprite.getU(x1);
        float uZ0 = sprite.getU(z0), uZ1 = sprite.getU(z1);
        float vZ0 = sprite.getV(1.0F - z1), vZ1 = sprite.getV(1.0F - z0);

        // 上
        quad(vc, pose, light, overlay, r, g, bl, 1.0F,
                x0, y1, z0, uX0, vZ0, x0, y1, z1, uX0, vZ1, x1, y1, z1, uX1, vZ1, x1, y1, z0, uX1, vZ0, 0, 1, 0);
        // 下
        quad(vc, pose, light, overlay, r * 0.7F, g * 0.7F, bl * 0.7F, 1.0F,
                x0, y0, z1, uX0, vZ0, x0, y0, z0, uX0, vZ1, x1, y0, z0, uX1, vZ1, x1, y0, z1, uX1, vZ0, 0, -1, 0);
        // 北
        quad(vc, pose, light, overlay, r * 0.85F, g * 0.85F, bl * 0.85F, 1.0F,
                x1, y1, z0, uX1, vTop, x1, y0, z0, uX1, vBot, x0, y0, z0, uX0, vBot, x0, y1, z0, uX0, vTop, 0, 0, -1);
        // 南
        quad(vc, pose, light, overlay, r * 0.85F, g * 0.85F, bl * 0.85F, 1.0F,
                x0, y1, z1, uX0, vTop, x0, y0, z1, uX0, vBot, x1, y0, z1, uX1, vBot, x1, y1, z1, uX1, vTop, 0, 0, 1);
        // 西
        quad(vc, pose, light, overlay, r * 0.75F, g * 0.75F, bl * 0.75F, 1.0F,
                x0, y1, z0, uZ0, vTop, x0, y0, z0, uZ0, vBot, x0, y0, z1, uZ1, vBot, x0, y1, z1, uZ1, vTop, -1, 0, 0);
        // 东
        quad(vc, pose, light, overlay, r * 0.75F, g * 0.75F, bl * 0.75F, 1.0F,
                x1, y1, z1, uZ1, vTop, x1, y0, z1, uZ1, vBot, x1, y0, z0, uZ0, vBot, x1, y1, z0, uZ0, vTop, 1, 0, 0);
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    private static void quad(com.mojang.blaze3d.vertex.VertexConsumer vc, PoseStack.Pose pose, int light, int overlay,
                             float r, float g, float b, float alpha,
                             float ax, float ay, float az, float au, float av,
                             float bx, float by, float bz, float bu, float bv,
                             float cx, float cy, float cz, float cu, float cv,
                             float dx, float dy, float dz, float du, float dv,
                             float nx, float ny, float nz) {
        vc.addVertex(pose, ax, ay, az).setColor(r, g, b, alpha).setUv(au, av)
                .setOverlay(overlay).setLight(light).setNormal(pose, nx, ny, nz);
        vc.addVertex(pose, bx, by, bz).setColor(r, g, b, alpha).setUv(bu, bv)
                .setOverlay(overlay).setLight(light).setNormal(pose, nx, ny, nz);
        vc.addVertex(pose, cx, cy, cz).setColor(r, g, b, alpha).setUv(cu, cv)
                .setOverlay(overlay).setLight(light).setNormal(pose, nx, ny, nz);
        vc.addVertex(pose, dx, dy, dz).setColor(r, g, b, alpha).setUv(du, dv)
                .setOverlay(overlay).setLight(light).setNormal(pose, nx, ny, nz);
    }
}
