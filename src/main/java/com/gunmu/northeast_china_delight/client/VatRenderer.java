package com.gunmu.northeast_china_delight.client;

import com.gunmu.northeast_china_delight.block.Vat;
import com.gunmu.northeast_china_delight.block.VatBlockEntity;
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
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
//? if <1.20.2 {
import net.minecraftforge.client.model.data.ModelData;
//?} else {
/*import net.neoforged.neoforge.client.model.data.ModelData;
*///?}

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * 把大缸里的东西按实际物品画出来 —— 不打开界面也能看见缸里有什么。
 *
 * <p>食材按总数（1~12 份）分批铺底与叠堆：底层铺满缸底（1 份居中、2 份斜对、3 份三角、4 份错落 2×2），
 * 超出 4 份则自然错位堆叠到上层并带轻微倾角与水平约束，不竖直插柱、不悬空也不穿出缸壁；
 * 压缸石铺满 12×12 缸口并按实际食材顶面微微下压，看起来扎实压在食材上。</p>
 */
public class VatRenderer implements BlockEntityRenderer<VatBlockEntity> {

    private static final float FLOOR = 0.125F;
    /** 缸内净空 2..14 像素（半径 0.375），留 0.01 安全边防止食材穿壁 */
    private static final float INNER_HALF_USABLE = 0.365F;
    /** 压缸石略小于 0.75（12 像素）以贴合内壁又避免与内壁闪烁 */
    private static final float PRESS_STONE_WIDTH = 0.746F;
    private static final float PRESS_STONE_HEIGHT = 0.32F;
    private static final float RIM_TOP = 0.995F;

    private static final float ITEM_W = 0.35F;
    private static final float LAYER_H = 0.28F;
    private static final int MAX_PER_LAYER = 4;

    private static final Map<BakedModel, ModelBounds> MODEL_BOUNDS_CACHE = new WeakHashMap<>();

    /** 1~4 份在单层里的水平相对位置 (dx, dz) 与基准偏航角（度），各层轮换角度避免上下直对齐。 */
    private static final float[][][] LAYER_LAYOUTS = {
            // 1 份
            {
                    {0.00F, 0.00F, 24.0F}
            },
            // 2 份
            {
                    {-0.15F, -0.07F, 18.0F},
                    {0.15F, 0.07F, 196.0F}
            },
            // 3 份（三角铺开）
            {
                    {-0.16F, -0.11F, 22.0F},
                    {0.16F, -0.09F, 146.0F},
                    {-0.01F, 0.16F, 266.0F}
            },
            // 4 份（错落 2×2）
            {
                    {-0.16F, -0.15F, 14.0F},
                    {0.16F, -0.14F, 108.0F},
                    {-0.15F, 0.16F, 286.0F},
                    {0.16F, 0.15F, 198.0F}
            }
    };

    private static final float[] LAYER_YAW_OFFSET = {0.0F, 48.0F, 92.0F};
    private static final float[] LAYER_SPREAD_SCALE = {1.0F, 0.88F, 0.76F};

    private record ModelBounds(float halfX, float height, float halfZ) {
    }

    private record DisplayEntry(ItemStack display, @org.jetbrains.annotations.Nullable BakedModel model,
                                ModelBounds bounds) {
    }

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

        List<DisplayEntry> entries = collectDisplayEntries(vat, contents, fermented);
        int total = entries.size();
        int posHash = (int) (vat.getBlockPos().asLong() * 31L);
        float vatYawJitter = (hash(posHash, 1) - 0.5F) * 16.0F;

        float contentTop = FLOOR;
        float topLayerHeight = 0.0F;
        float layerBaseY = FLOOR;
        int cursor = 0;
        int layerIndex = 0;

        while (cursor < total) {
            int countInLayer = Math.min(MAX_PER_LAYER, total - cursor);
            float[][] pattern = LAYER_LAYOUTS[countInLayer - 1];
            float spread = LAYER_SPREAD_SCALE[Math.min(layerIndex, LAYER_SPREAD_SCALE.length - 1)];
            float layerYawBase = LAYER_YAW_OFFSET[Math.min(layerIndex, LAYER_YAW_OFFSET.length - 1)] + vatYawJitter;

            float layerTop = layerBaseY;
            float layerHeightSum = 0.0F;
            float layerHeightMax = 0.0F;

            for (int slot = 0; slot < countInLayer; slot++) {
                DisplayEntry entry = entries.get(cursor + slot);
                ModelBounds b = entry.bounds();
                boolean tallUpright = b.height() > 0.22F;
                boolean narrowTall = tallUpright && Math.max(b.halfX(), b.halfZ()) < 0.13F;

                float yaw = pattern[slot][2] + layerYawBase + (hash(posHash + cursor, slot + 3) - 0.5F) * 14.0F;
                float pitch;
                float roll;
                if (narrowTall) {
                    // 梨等细高食材自然斜倚/半卧在缸里，避免像保龄球瓶一样笔直竖立
                    float baseTilt = layerIndex == 0 ? 28.0F : 38.0F;
                    pitch = ((slot & 1) == 0 ? 1.0F : -1.0F) * (baseTilt + (hash(posHash, cursor + 11) - 0.5F) * 10.0F);
                    roll = ((slot & 2) == 0 ? 1.0F : -1.0F) * (10.0F + hash(posHash, cursor + 19) * 10.0F);
                } else if (tallUpright) {
                    // 白菜等高大蔬菜在缸里微倾倚靠，第二层倾斜更明显以形成自然堆叠
                    float baseTilt = layerIndex == 0 ? 13.0F : 21.0F;
                    pitch = ((slot & 1) == 0 ? 1.0F : -1.0F) * (baseTilt + (hash(posHash, cursor + 11) - 0.5F) * 6.0F);
                    roll = ((slot & 2) == 0 ? 1.0F : -1.0F) * (6.0F + hash(posHash, cursor + 19) * 6.0F);
                } else if (layerIndex == 0) {
                    // 底层平放类食材稳贴缸底，只留极小自然偏斜
                    pitch = (hash(posHash, cursor + 23) - 0.5F) * 6.0F;
                    roll = (hash(posHash, cursor + 29) - 0.5F) * 6.0F;
                } else {
                    // 上层食材搭在下层起伏面上，带自然倾角
                    pitch = ((slot & 1) == 0 ? 1.0F : -1.0F) * (9.0F + hash(posHash, cursor + 23) * 6.0F);
                    roll = ((slot & 2) == 0 ? 1.0F : -1.0F) * (7.0F + hash(posHash, cursor + 29) * 6.0F);
                }

                float radP = Math.abs(pitch) * Mth.DEG_TO_RAD;
                float radR = Math.abs(roll) * Mth.DEG_TO_RAD;
                float radY = yaw * Mth.DEG_TO_RAD;
                float cosP = Mth.cos(radP);
                float sinP = Mth.sin(radP);
                float cosR = Mth.cos(radR);
                float sinR = Mth.sin(radR);
                float halfH = (b.height() * 0.5F) * cosP * cosR + b.halfZ() * sinP + b.halfX() * sinR;
                float localHX = b.halfX() * cosR + (b.height() * 0.5F) * sinR;
                float localHZ = b.halfZ() * cosP + (b.height() * 0.5F) * sinP;
                float cosY = Math.abs(Mth.cos(radY));
                float sinY = Math.abs(Mth.sin(radY));
                float worldHX = (float) Math.hypot(localHX * cosY, localHZ * sinY);
                float worldHZ = (float) Math.hypot(localHX * sinY, localHZ * cosY);

                float maxDx = Math.max(0.0F, INNER_HALF_USABLE - worldHX);
                float maxDz = Math.max(0.0F, INNER_HALF_USABLE - worldHZ);
                float rawDx = pattern[slot][0] * spread;
                float rawDz = pattern[slot][1] * spread;
                // 上层整体随层偏航角旋转布局坐标，使每层错开落入下层缝隙
                float rotCos = Mth.cos(layerYawBase * Mth.DEG_TO_RAD);
                float rotSin = Mth.sin(layerYawBase * Mth.DEG_TO_RAD);
                float dx = Mth.clamp(rawDx * rotCos - rawDz * rotSin, -maxDx, maxDx);
                float dz = Mth.clamp(rawDx * rotSin + rawDz * rotCos, -maxDz, maxDz);

                float itemFloor = layerBaseY + slot * 0.0025F;
                float itemTop = itemFloor + halfH * 2.0F;
                if (itemTop > RIM_TOP - 0.02F) {
                    float shift = itemTop - (RIM_TOP - 0.02F);
                    itemFloor = Math.max(FLOOR, itemFloor - shift);
                    itemTop = itemFloor + halfH * 2.0F;
                }

                float centerY = itemFloor + halfH;
                pose.pushPose();
                pose.translate(0.5F + dx, centerY, 0.5F + dz);
                pose.mulPose(Axis.YP.rotationDegrees(yaw));
                pose.mulPose(Axis.XP.rotationDegrees(pitch));
                pose.mulPose(Axis.ZP.rotationDegrees(roll));
                pose.translate(0.0F, -b.height() * 0.5F, 0.0F);
                if (entry.model() != null) {
                    // 配方给了展示模型：按方块模型的原始像素尺寸画，模型以 (8, 0, 8) 为底面中心
                    pose.translate(-0.5F, 0.0F, -0.5F);
                    renderDisplayModel(pose, buffer, vat, entry.model(), light, packedOverlay,
                            seasoningTint, seasoningAmount);
                } else {
                    float shapeH = LAYER_H * VatItemShapes.heightScale(entry.display());
                    pose.translate(-ITEM_W * 0.5F, 0.0F, -ITEM_W * 0.5F);
                    pose.scale(ITEM_W, shapeH, ITEM_W);
                    renderShape(pose, buffer, entry.display(), itemRenderer, level, light, packedOverlay,
                            saltCoat, seasoningTint, seasoningAmount);
                }
                pose.popPose();

                if (itemTop > layerTop) {
                    layerTop = itemTop;
                }
                layerHeightSum += halfH * 2.0F;
                layerHeightMax = Math.max(layerHeightMax, halfH * 2.0F);
            }

            if (layerTop > contentTop) {
                contentTop = layerTop;
            }
            topLayerHeight = layerHeightMax;
            float avgH = layerHeightSum / countInLayer;
            // 上层略微嵌入下层空隙（~72% 高度），避免层间出现肉眼可见的悬浮缝隙
            layerBaseY = Math.min(RIM_TOP - 0.14F, layerBaseY + Math.max(0.045F, avgH * 0.72F));
            cursor += countInLayer;
            layerIndex++;
        }

        if (vat.isPressed()) {
            ItemStack press = vat.press();
            float stoneH = PRESS_STONE_HEIGHT;
            // 压缸石直接贴实并略微压入顶层食材（压入量按顶层食材高度自适应），彻底消除悬浮感
            float pressSink = total > 0 ? Mth.clamp(topLayerHeight * 0.26F, 0.014F, 0.045F) : 0.0F;
            float stoneBottom = total > 0
                    ? Math.max(FLOOR + 0.015F, contentTop - pressSink)
                    : FLOOR + 0.002F;
            stoneBottom = Math.min(stoneBottom, RIM_TOP - stoneH);
            if (press.getItem() instanceof BlockItem blockItem) {
                float s = PRESS_STONE_WIDTH;
                pose.pushPose();
                pose.translate(0.5F - s * 0.5F, stoneBottom, 0.5F - s * 0.5F);
                pose.scale(s, stoneH, s);
                blockRenderer.renderSingleBlock(blockItem.getBlock().defaultBlockState(), pose, buffer,
                        light, packedOverlay, ModelData.EMPTY, null);
                pose.popPose();
            } else {
                pose.pushPose();
                pose.translate(0.5F, stoneBottom + stoneH - 0.02F, 0.5F);
                pose.scale(0.55F, 0.55F, 0.55F);
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

    private static List<DisplayEntry> collectDisplayEntries(VatBlockEntity vat, List<ItemStack> contents,
                                                            boolean done) {
        List<DisplayEntry> entries = new ArrayList<>();
        for (ItemStack content : contents) {
            if (content.isEmpty() || entries.size() >= VatBlockEntity.MAX_ENTRIES) {
                continue;
            }
            if (isSeasoning(content)) {
                continue;
            }
            if (done && (vat.kind() == VatRecipes.Kind.PASTE || vat.kind() == VatRecipes.Kind.SOY_SAUCE)) {
                if (content.is(ModItems.SOY_PASTE_CHUNK.get())) {
                    continue;
                }
                if (vat.kind() == VatRecipes.Kind.SOY_SAUCE
                        && content.is(com.gunmu.northeast_china_delight.ModTags.CROPS_WHEAT)) {
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
            BakedModel model = displayModel(vat, display);
            ModelBounds bounds = model != null ? measureModel(model) : measureFallbackShape(display);
            entries.add(new DisplayEntry(display, model, bounds));
        }
        return entries;
    }

    /** 读取烘焙模型所有顶点的实际包围盒（按中心 0.5, 0.0, 0.5 折算半宽与高度），数据包自定义模型也能精准测高。 */
    private static ModelBounds measureModel(BakedModel model) {
        return MODEL_BOUNDS_CACHE.computeIfAbsent(model, m -> {
            float minX = Float.POSITIVE_INFINITY;
            float maxX = Float.NEGATIVE_INFINITY;
            float minY = Float.POSITIVE_INFINITY;
            float maxY = Float.NEGATIVE_INFINITY;
            float minZ = Float.POSITIVE_INFINITY;
            float maxZ = Float.NEGATIVE_INFINITY;
            RandomSource random = RandomSource.create(42L);
            for (Direction dir : Direction.values()) {
                random.setSeed(42L);
                for (BakedQuad quad : m.getQuads(null, dir, random)) {
                    int[] v = quad.getVertices();
                    int stride = v.length / 4;
                    if (stride < 3) {
                        continue;
                    }
                    for (int k = 0; k < 4; k++) {
                        float vx = Float.intBitsToFloat(v[k * stride]);
                        float vy = Float.intBitsToFloat(v[k * stride + 1]);
                        float vz = Float.intBitsToFloat(v[k * stride + 2]);
                        if (vx < minX) minX = vx;
                        if (vx > maxX) maxX = vx;
                        if (vy < minY) minY = vy;
                        if (vy > maxY) maxY = vy;
                        if (vz < minZ) minZ = vz;
                        if (vz > maxZ) maxZ = vz;
                    }
                }
            }
            random.setSeed(42L);
            for (BakedQuad quad : m.getQuads(null, null, random)) {
                int[] v = quad.getVertices();
                int stride = v.length / 4;
                if (stride < 3) {
                    continue;
                }
                for (int k = 0; k < 4; k++) {
                    float vx = Float.intBitsToFloat(v[k * stride]);
                    float vy = Float.intBitsToFloat(v[k * stride + 1]);
                    float vz = Float.intBitsToFloat(v[k * stride + 2]);
                    if (vx < minX) minX = vx;
                    if (vx > maxX) maxX = vx;
                    if (vy < minY) minY = vy;
                    if (vy > maxY) maxY = vy;
                    if (vz < minZ) minZ = vz;
                    if (vz > maxZ) maxZ = vz;
                }
            }
            if (!Float.isFinite(maxY)) {
                return new ModelBounds(ITEM_W * 0.5F, LAYER_H * 0.6F, ITEM_W * 0.5F);
            }
            float halfX = Math.max(0.05F, Math.max(Math.abs(minX - 0.5F), Math.abs(maxX - 0.5F)));
            float halfZ = Math.max(0.05F, Math.max(Math.abs(minZ - 0.5F), Math.abs(maxZ - 0.5F)));
            float height = Math.max(0.05F, maxY - Math.min(0.0F, minY));
            return new ModelBounds(halfX, height, halfZ);
        });
    }

    private static ModelBounds measureFallbackShape(ItemStack display) {
        float shapeH = LAYER_H * VatItemShapes.heightScale(display);
        float maxY = 0.0F;
        float maxHalfX = 0.0F;
        float maxHalfZ = 0.0F;
        for (VatItemShapes.Box b : VatItemShapes.of(display)) {
            maxY = Math.max(maxY, b.y1());
            maxHalfX = Math.max(maxHalfX, Math.max(Math.abs(b.x0() - 0.5F), Math.abs(b.x1() - 0.5F)));
            maxHalfZ = Math.max(maxHalfZ, Math.max(Math.abs(b.z0() - 0.5F), Math.abs(b.z1() - 0.5F)));
        }
        return new ModelBounds(
                Math.max(0.05F, maxHalfX * ITEM_W),
                Math.max(0.05F, maxY * shapeH),
                Math.max(0.05F, maxHalfZ * ITEM_W));
    }

    private static float hash(int seed, int salt) {
        int h = seed * 0x27D4EB2D ^ salt * 0x165667B1;
        h ^= h >>> 15;
        h *= 0x2C1B3C6D;
        h ^= h >>> 12;
        h *= 0x297A2D39;
        h ^= h >>> 15;
        return (h >>> 8) / 16777216.0F;
    }

    /**
     * 配方 {@code display_models} 里给这件东西指定的方块模型。没写、或者模型没被烘焙（路径写错 / 资源包缺文件）
     * 都返回 null，调用方退回 {@link VatItemShapes} 的贴图盒子。
     */
    @org.jetbrains.annotations.Nullable
    private static BakedModel displayModel(VatBlockEntity vat, ItemStack stack) {
        net.minecraft.resources.ResourceLocation id = VatRecipes.displayModelFor(vat, stack);
        if (id == null) {
            return null;
        }
        BakedModel model = WoodenMortarRenderer.bakedModel(id);
        return model == Minecraft.getInstance().getModelManager().getMissingModel() ? null : model;
    }

    /**
     * 调料的颜色只染带 tintindex 的面（本模组的展示模型都带），数据包模型不写 tintindex 就保持原色。
     * 用实体式的方块图集缓冲画：按面的法线分明暗，小模型才有立体感（普通 cutout 方块缓冲在 BER 里各面一样亮）。
     */
    private static void renderDisplayModel(PoseStack pose, MultiBufferSource buffer, VatBlockEntity vat,
                                           BakedModel model, int light, int overlay,
                                           int seasoningTint, float seasoningAmount) {
        int tint = seasoningAmount > 0.0F ? blend(0xFFFFFF, seasoningTint, seasoningAmount) : 0xFFFFFF;
        Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(
                pose.last(), buffer.getBuffer(net.minecraft.client.renderer.Sheets.cutoutBlockSheet()),
                vat.getBlockState(), model,
                ((tint >> 16) & 0xFF) / 255.0F, ((tint >> 8) & 0xFF) / 255.0F, (tint & 0xFF) / 255.0F,
                light, overlay);
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
        // 缸内壁大约在 2/16 到 14/16，液面贴着内壁画
        float min = 0.125F;
        float max = 0.875F;
        // 实体半透明的固定缓冲：在缸里的展示模型（cutoutBlockSheet）之后才画，液面下的东西隔着液面看得见
        // （以前用 RenderType.translucent()，方块实体里会被提前画掉、写进深度，液面下的展示模型被挡没了，同大锅）
        var consumer = buffer.getBuffer(net.minecraft.client.renderer.Sheets.translucentCullBlockSheet());
        // 液面略微抬高，避免和缸壁内侧重叠时闪烁
        quad(consumer, pose.last(), light, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,
                red, green, blue, alpha,
                min, y, min, sprite.getU0(), sprite.getV0(),
                min, y, max, sprite.getU0(), sprite.getV1(),
                max, y, max, sprite.getU1(), sprite.getV1(),
                max, y, min, sprite.getU1(), sprite.getV0(),
                0, 1, 0);
    }

    /** 流体静止贴图（JEI 的大锅烹饪页也用） */
    @org.jetbrains.annotations.Nullable
    public static net.minecraft.client.renderer.texture.TextureAtlasSprite fluidSprite(
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

    /** 流体染色；水单独给群系默认水色，别画成白的（JEI 的大锅烹饪页也用） */
    public static int fluidTint(net.minecraft.world.level.material.Fluid fluid) {
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

    private static float spriteU(TextureAtlasSprite sprite, float normalized) {
        //? if <1.20.2 {
        return sprite.getU(normalized * 16.0F);
        //?} else {
        /*return sprite.getU(normalized);
        *///?}
    }

    private static float spriteV(TextureAtlasSprite sprite, float normalized) {
        //? if <1.20.2 {
        return sprite.getV(normalized * 16.0F);
        //?} else {
        /*return sprite.getV(normalized);
        *///?}
    }

    private static void boxWash(com.mojang.blaze3d.vertex.VertexConsumer vc, PoseStack.Pose pose,
                                TextureAtlasSprite sprite, VatItemShapes.Box b, int light, int overlay,
                                int color, float alpha) {
        float r = ((color >> 16) & 0xFF) / 255.0F;
        float g = ((color >> 8) & 0xFF) / 255.0F;
        float bl = (color & 0xFF) / 255.0F;
        float x0 = b.x0(), y0 = b.y0(), z0 = b.z0(), x1 = b.x1(), y1 = b.y1(), z1 = b.z1();
        float vTop = spriteV(sprite, 1.0F - y1);
        float vBot = spriteV(sprite, 1.0F - y0);
        float uX0 = spriteU(sprite, x0), uX1 = spriteU(sprite, x1);
        float uZ0 = spriteU(sprite, z0), uZ1 = spriteU(sprite, z1);
        float vZ0 = spriteV(sprite, 1.0F - z1), vZ1 = spriteV(sprite, 1.0F - z0);
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
        float vTop = spriteV(sprite, 1.0F - y1);
        float vBot = spriteV(sprite, 1.0F - y0);
        float uX0 = spriteU(sprite, x0), uX1 = spriteU(sprite, x1);
        float uZ0 = spriteU(sprite, z0), uZ1 = spriteU(sprite, z1);
        float vZ0 = spriteV(sprite, 1.0F - z1), vZ1 = spriteV(sprite, 1.0F - z0);

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
    public static void quad(com.mojang.blaze3d.vertex.VertexConsumer vc, PoseStack.Pose pose, int light, int overlay,
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
