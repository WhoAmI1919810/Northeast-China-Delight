package com.gunmu.northeast_china_delight.client;

import com.gunmu.northeast_china_delight.block.Vat;
import com.gunmu.northeast_china_delight.block.VatBlockEntity;
import com.gunmu.northeast_china_delight.crafting.VatRecipe;
import com.gunmu.northeast_china_delight.crafting.VatRecipes;
import com.gunmu.northeast_china_delight.fluid.ModFluids;
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
//? if <1.20.2 {
import net.minecraftforge.client.model.data.ModelData;
//?} else {
/*import net.neoforged.neoforge.client.model.data.ModelData;
*///?}

import java.util.List;

/**
 * 把大缸里的东西按实际物品画出来 —— 不打开界面也能看见缸里有什么。内容物是立体的小模型（{@link VatItemShapes}）：一层放两个 —— 缸里一层水配两份食材，
 * 每个小模型宽度是缸内宽度的一半，两个正好铺满一层；高度是缸内净高的三分之一略少，三层叠起来（上层直接压在下层上）就正好把缸填满。
 */
public class VatRenderer implements BlockEntityRenderer<VatBlockEntity> {

    private static final float FLOOR = 0.125F;
    private static final int PER_LAYER = 2;
    private static final float ITEM_W = 0.35F;
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

        if (fermented) {
            net.minecraft.world.level.material.Fluid product = VatRecipes.productOf(vat);
            int amount = product == null ? 0 : vat.productMb(product);
            // 酸引水的水面由方块状态里的 water_level 模型画，这里再画会叠出两层
            if (product != null && amount > 0 && !product.isSame(ModFluids.sourWaterSource())) {
                float ratio = amount / (float) capacityOf(product);
                renderFluidSurface(pose, buffer, product, 0.10F + 0.72F * ratio, light);
            }
        }

        boolean done = vat.getBlockState().getValue(Vat.FERMENTED);
        List<ItemStack> contents = vat.contents();
        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
        float saltCoat = 0.0F;
        if (!vat.isSaltDissolved()) {
            int salt = vat.count(stack -> stack.is(com.gunmu.northeast_china_delight.ModTags.FOODS_SALT));
            int meaty = vat.meatCount() + vat.rawFishCount();
            if (salt > 0 && meaty > 0) {
                saltCoat = Math.min(1.0F, salt / (float) meaty) * 0.55F;
            }
        }
        int seasoningTint = 0xFFFFFF;
        float seasoningAmount = 0.0F;
        if (vat.chiliSauceCount() > 0) {
            seasoningTint = 0xE0553C;
            seasoningAmount = 0.45F;
        } else if (vat.countOf(ModItems.FISH_SAUCE.get()) > 0) {
            seasoningTint = 0xB0703A;
            seasoningAmount = 0.34F;
        } else if (vat.countOf(ModItems.SHRIMP_PASTE.get()) > 0) {
            seasoningTint = 0xC0603A;
            seasoningAmount = 0.34F;
        }
        int shown = 0;
        float contentTop = FLOOR + LAYER_H;
        for (ItemStack content : contents) {
            if (content.isEmpty() || shown >= 12) {
                continue;
            }
            if (isSeasoning(content)) {
                continue;
            }
            if (done && (vat.kind() == VatRecipes.Kind.PASTE || vat.kind() == VatRecipes.Kind.SOY_SAUCE)) {
                if (content.is(ModItems.SOY_PASTE_CHUNK.get())) {
                    continue;
                }
                if (vat.kind() == VatRecipes.Kind.SOY_SAUCE && content.is(com.gunmu.northeast_china_delight.ModTags.CROPS_WHEAT)) {
                    continue;
                }
            }
            ItemStack display = content;
            if (done) {
                Item result = VatRecipes.resultFor(vat, content.getItem());
                if (result != null) {
                    display = new ItemStack(result);
                }
            }

            int layer = shown / PER_LAYER;
            int slot = shown % PER_LAYER;
            float x = 0.3125F + slot * 0.375F;
            float z = 0.5F;
            float y = FLOOR + layer * LAYER_H;
            float shapeH = LAYER_H * VatItemShapes.heightScale(display);
            float shapeTop = y + shapeH;
            if (shapeTop > contentTop) {
                contentTop = shapeTop;
            }

            pose.pushPose();
            pose.translate(x, y, z);
            pose.mulPose(Axis.YP.rotationDegrees((shown % 2) * 90 + ((shown * 7) % 8) - 4));
            pose.translate(-ITEM_W * 0.5F, 0.0F, -ITEM_W * 0.5F);
            pose.scale(ITEM_W, shapeH, ITEM_W);
            renderShape(pose, buffer, display, itemRenderer, level, light, packedOverlay,
                    saltCoat, seasoningTint, seasoningAmount);
            pose.popPose();
            shown++;
        }

        if (vat.isPressed()) {
            ItemStack press = vat.press();
            float stoneH = 0.30F;
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

        if (vat.isCovered() && vat.cover().getItem() instanceof BlockItem blockItem) {
            pose.pushPose();
            pose.translate(0.14F, 0.94F, 0.14F);
            pose.scale(0.72F, 1.0F, 0.72F);
            blockRenderer.renderSingleBlock(blockItem.getBlock().defaultBlockState(), pose, buffer,
                    light, packedOverlay, ModelData.EMPTY, null);
            pose.popPose();
        }
    }

    /** 液面高度按这种液体的缸容量折算。格瓦斯一缸的量跟别的不一样，单独算 */
    private static int capacityOf(net.minecraft.world.level.material.Fluid fluid) {
        if (fluid.isSame(ModFluids.kvassSource())) {
            return VatRecipes.KVASS_SERVINGS * VatRecipes.SERVING_MB;
        }
        if (fluid.isSame(ModFluids.sourWaterSource())) {
            return VatRecipes.SOUR_WATER_MB;
        }
        return VatRecipes.PRODUCT_CAPACITY_MB;
    }

    /**
     * 在缸口高度画一层液面，贴图和颜色都取这种流体自己注册的静图和染色。
     * 所以数据包让配方产出什么流体，缸里就显示那种流体的液面，不用再为每种液体配一个冒充颜色的方块。
     */
    private static void renderFluidSurface(PoseStack pose, MultiBufferSource buffer,
                                           net.minecraft.world.level.material.Fluid fluid,
                                           float y, int light) {
        net.minecraft.client.renderer.texture.TextureAtlasSprite sprite = fluidSprite(fluid);
        if (sprite == null) {
            return;
        }
        int tint = fluidTint(fluid);
        float red = ((tint >> 16) & 0xFF) / 255.0F;
        float green = ((tint >> 8) & 0xFF) / 255.0F;
        float blue = (tint & 0xFF) / 255.0F;
        float alpha = ((tint >>> 24) & 0xFF) / 255.0F;
        float min = 0.125F;
        float max = 0.875F;
        var consumer = buffer.getBuffer(RenderType.translucent());
        // 液面略微抬高，避免和缸壁内侧重叠时闪烁
        quad(consumer, pose.last(), light, 0xF000F0, red, green, blue, alpha,
                min, y, min, sprite.getU0(), sprite.getV0(),
                min, y, max, sprite.getU0(), sprite.getV1(),
                max, y, max, sprite.getU1(), sprite.getV1(),
                max, y, min, sprite.getU1(), sprite.getV0(),
                0, 1, 0);
    }

    @org.jetbrains.annotations.Nullable
    private static net.minecraft.client.renderer.texture.TextureAtlasSprite fluidSprite(
            net.minecraft.world.level.material.Fluid fluid) {
        net.minecraft.resources.ResourceLocation texture = fluidExtensions(fluid).getStillTexture();
        if (texture == null) {
            return null;
        }
        return Minecraft.getInstance()
                .getTextureAtlas(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS).apply(texture);
    }

    //? if <1.20.2 {
    private static net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions fluidExtensions(
            net.minecraft.world.level.material.Fluid fluid) {
        return net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions.of(fluid);
    }
    //?} else {
    /*private static net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions fluidExtensions(
            net.minecraft.world.level.material.Fluid fluid) {
        return net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions.of(fluid);
    }
    *///?}

    private static int fluidTint(net.minecraft.world.level.material.Fluid fluid) {
        if (fluid.isSame(net.minecraft.world.level.material.Fluids.WATER)) {
            return 0xFF3F76E4;
        }
        return fluidExtensions(fluid).getTintColor();
    }

    private static boolean isSeasoning(ItemStack stack) {
        return stack.is(com.gunmu.northeast_china_delight.ModTags.FOODS_SALT)
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

    private static int blend(int base, int tint, float amount) {
        float a = Math.max(0.0F, Math.min(1.0F, amount));
        int br = (base >> 16) & 0xFF, bg = (base >> 8) & 0xFF, bb = base & 0xFF;
        int tr = (tint >> 16) & 0xFF, tg = (tint >> 8) & 0xFF, tb = tint & 0xFF;
        int r = (int) (br + (tr - br) * a);
        int g = (int) (bg + (tg - bg) * a);
        int b = (int) (bb + (tb - bb) * a);
        return (r << 16) | (g << 8) | b;
    }

    private static void box(com.mojang.blaze3d.vertex.VertexConsumer vc, PoseStack.Pose pose,
                            TextureAtlasSprite sprite, VatItemShapes.Box b, int light, int overlay) {
        float r = ((b.tint() >> 16) & 0xFF) / 255.0F;
        float g = ((b.tint() >> 8) & 0xFF) / 255.0F;
        float bl = (b.tint() & 0xFF) / 255.0F;

        float x0 = b.x0(), y0 = b.y0(), z0 = b.z0(), x1 = b.x1(), y1 = b.y1(), z1 = b.z1();
        float vTop = sprite.getV(1.0F - y1);
        float vBot = sprite.getV(1.0F - y0);
        float uX0 = sprite.getU(x0), uX1 = sprite.getU(x1);
        float uZ0 = sprite.getU(z0), uZ1 = sprite.getU(z1);
        float vZ0 = sprite.getV(1.0F - z1), vZ1 = sprite.getV(1.0F - z0);

        quad(vc, pose, light, overlay, r, g, bl, 1.0F,
                x0, y1, z0, uX0, vZ0, x0, y1, z1, uX0, vZ1, x1, y1, z1, uX1, vZ1, x1, y1, z0, uX1, vZ0, 0, 1, 0);
        quad(vc, pose, light, overlay, r * 0.7F, g * 0.7F, bl * 0.7F, 1.0F,
                x0, y0, z1, uX0, vZ0, x0, y0, z0, uX0, vZ1, x1, y0, z0, uX1, vZ1, x1, y0, z1, uX1, vZ0, 0, -1, 0);
        quad(vc, pose, light, overlay, r * 0.85F, g * 0.85F, bl * 0.85F, 1.0F,
                x1, y1, z0, uX1, vTop, x1, y0, z0, uX1, vBot, x0, y0, z0, uX0, vBot, x0, y1, z0, uX0, vTop, 0, 0, -1);
        quad(vc, pose, light, overlay, r * 0.85F, g * 0.85F, bl * 0.85F, 1.0F,
                x0, y1, z1, uX0, vTop, x0, y0, z1, uX0, vBot, x1, y0, z1, uX1, vBot, x1, y1, z1, uX1, vTop, 0, 0, 1);
        quad(vc, pose, light, overlay, r * 0.75F, g * 0.75F, bl * 0.75F, 1.0F,
                x0, y1, z0, uZ0, vTop, x0, y0, z0, uZ0, vBot, x0, y0, z1, uZ1, vBot, x0, y1, z1, uZ1, vTop, -1, 0, 0);
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
        //? if <1.20.5 {
        vc.vertex(pose.pose(), ax, ay, az).color(r, g, b, alpha).uv(au, av)
                .overlayCoords(overlay).uv2(light).normal(pose.normal(), nx, ny, nz).endVertex();
        vc.vertex(pose.pose(), bx, by, bz).color(r, g, b, alpha).uv(bu, bv)
                .overlayCoords(overlay).uv2(light).normal(pose.normal(), nx, ny, nz).endVertex();
        vc.vertex(pose.pose(), cx, cy, cz).color(r, g, b, alpha).uv(cu, cv)
                .overlayCoords(overlay).uv2(light).normal(pose.normal(), nx, ny, nz).endVertex();
        vc.vertex(pose.pose(), dx, dy, dz).color(r, g, b, alpha).uv(du, dv)
                .overlayCoords(overlay).uv2(light).normal(pose.normal(), nx, ny, nz).endVertex();
        //?} else {
        /*vc.addVertex(pose, ax, ay, az).setColor(r, g, b, alpha).setUv(au, av)
                .setOverlay(overlay).setLight(light).setNormal(pose, nx, ny, nz);
        vc.addVertex(pose, bx, by, bz).setColor(r, g, b, alpha).setUv(bu, bv)
                .setOverlay(overlay).setLight(light).setNormal(pose, nx, ny, nz);
        vc.addVertex(pose, cx, cy, cz).setColor(r, g, b, alpha).setUv(cu, cv)
                .setOverlay(overlay).setLight(light).setNormal(pose, nx, ny, nz);
        vc.addVertex(pose, dx, dy, dz).setColor(r, g, b, alpha).setUv(du, dv)
                .setOverlay(overlay).setLight(light).setNormal(pose, nx, ny, nz);
        *///?}
    }
}
