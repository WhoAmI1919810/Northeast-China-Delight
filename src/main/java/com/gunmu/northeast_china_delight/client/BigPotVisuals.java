package com.gunmu.northeast_china_delight.client;

import com.gunmu.northeast_china_delight.ModTags;
import com.gunmu.northeast_china_delight.NortheastChinaDelight;
import com.gunmu.northeast_china_delight.block.BigPotBlockEntity;
import com.gunmu.northeast_china_delight.item.ModItems;
import com.gunmu.northeast_china_delight.util.DdIds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * 大铁锅里的东西怎么画：锅里六个格子怎么摆、液面画多大、调料把液面染成什么颜色、哪样东西用哪个模型。
 * 锅里的画法（{@link BigPotRenderer}）和 JEI 的大锅烹饪页共用。
 *
 * <p>数值和出图脚本 work/tools/big_pot_contents/layout.py 一一对应（改一边记得改另一边）。坐标是整口灶 32×32 的像素坐标，
 * 原点在灶的西北角（朝北摆的时候），锅心 (16, 16)。</p>
 *
 * <ul>
 *   <li>食材模型：{@code northeast_china_delight:block/big_pot/<名字>}，底面中心 (8, 0, 8)，生成时已经等比缩放到
 *       正好装进半径 3、高 2.8 像素的圆柱，原样画；没有模型的东西（别的模组的食材）画成平躺的物品。
 *       成品：{@code block/big_pot/dish/<成品物品名>}，坐标已经是锅里的，最高不过锅盖底面。</li>
 *   <li>七个格子：锅底正中一份，外面一圈六份。圈上相邻两份、圈和正中都隔开两个圆柱半径，锅壁也刚好够宽，谁都碰不到谁
 *       （2026-11-03 用户说食材穿模后这样排；查法见 work/tools/big_pot_contents/fitcheck.py）。
 *       同一样放了好几个，每份占一格（{@link #pieces}）。外圈的顶比 1000 mB 的水面高 1.7 像素、正中那份刚露出水面
 *       （第一版泡得太深，用户说放进去看不见）。</li>
 * </ul>
 */
public final class BigPotVisuals {

    public static final float CENTER = 16.0F;
    public static final int MAX_COPIES = 3;
    /** 液面不透明度：清水 0.6，每倒一份调料 +0.02，最多 0.75（再浑就看不见锅里的料了） */
    public static final float LIQUID_ALPHA = 0.6F;
    private static final float LIQUID_ALPHA_MAX = 0.75F;

    /** 七个格子：离锅心、方位角（度，从 +x 往 +z 转）、底面高度 */
    private static final float[][] SLOT_PLACES = {
            {0.0F, 0, 10.8F},
            {6.0F, 30, 12.4F}, {6.0F, 90, 12.4F}, {6.0F, 150, 12.4F},
            {6.0F, 210, 12.4F}, {6.0F, 270, 12.4F}, {6.0F, 330, 12.4F}};
    public static final int SLOTS = SLOT_PLACES.length;

    /**
     * 摊开在锅底的（生大米）：不占格子，在锅底均匀铺一层，每份厚 {@link #SPREAD_STEP} 像素，最多算 {@link #SPREAD_MAX} 份
     * （2026-11-03 用户：生米放进锅里应该是摊开的，先水后米，米均匀铺在锅底）。和 layout.py 的 SPREAD 一样
     */
    private static final float FLOOR = 10.0F;
    private static final float SPREAD_STEP = 0.4F;
    private static final int SPREAD_MAX = 4;
    public static final ResourceLocation RAW_RICE = DdIds.of(NortheastChinaDelight.MODID, "block/big_pot/raw_rice");

    /** 锅身每一圈 (y0, y1, 内半径)，和大灶生成器 big_stove/build.py 的 POT 一样 */
    private static final float[][] POT = {
            {16, 16.75F, 11.5F}, {15, 16, 11.5F}, {14, 15, 11}, {13, 14, 10}, {12, 13, 9}, {11, 12, 7.5F}, {10, 11, 5.5F}};

    /** 调料：哪个标签、把液面往什么颜色染、每一份往那边靠多少 */
    private record Seasoning(TagKey<Item> tag, int color, float weight) {
    }

    private static final List<Seasoning> SEASONINGS = List.of(
            new Seasoning(ModTags.FOODS_SALT, 0xC8D4DC, 0.12F),
            new Seasoning(ModTags.FOODS_SOY_SAUCE, 0x9A5822, 0.55F),
            new Seasoning(ModTags.FOODS_COOKING_OIL, 0xE8B848, 0.35F),
            new Seasoning(ModTags.FOODS_ANIMAL_OIL, 0xECDCB0, 0.25F),
            new Seasoning(ModTags.FOODS_CHILI_OIL, 0xC0341C, 0.42F),
            new Seasoning(ModTags.FOODS_CHILI_SAUCE, 0xB0301A, 0.42F),
            new Seasoning(ModTags.FOODS_VINEGAR, 0x3A2414, 0.35F),
            new Seasoning(ModTags.FOODS_WHITE_VINEGAR, 0xE8E8E0, 0.08F),
            new Seasoning(ModTags.FOODS_FISH_SAUCE, 0x8A5A20, 0.35F),
            new Seasoning(ModTags.FOODS_SHRIMP_PASTE, 0x8A6A7A, 0.35F),
            new Seasoning(ModTags.FOODS_PEANUT_BUTTER, 0xB07838, 0.40F),
            new Seasoning(ModTags.FOODS_SOY_PASTE, 0x7A5210, 0.40F));

    /** 食材 → 模型名（先比物品、再比标签，先对上先用） */
    private record IngredientModel(Predicate<ItemStack> test, String name) {
    }

    private static List<IngredientModel> ingredientModels;
    private static final Map<Float, int[][]> SURFACE_ROWS = new HashMap<>();

    private BigPotVisuals() {
    }

    // ---------------------------------------------------------------- 几何

    /** 液面高度（像素），见 {@link BigPotBlockEntity#liquidSurfaceY} */
    public static float liquidY(int mb) {
        return BigPotBlockEntity.liquidSurfaceY(mb);
    }

    /** 这个高度上锅的内半径 */
    public static float innerRadius(float y) {
        for (float[] ring : POT) {
            if (ring[0] <= y && y < ring[1]) {
                return ring[2];
            }
        }
        return POT[0][2];
    }

    /** 液面：每一行（z）里格子中心离锅心比内半径近的那一段 {z, x0, x1}，正好填满锅里这一圈 */
    public static int[][] surfaceRows(float y) {
        return SURFACE_ROWS.computeIfAbsent(y, key -> {
            float r = innerRadius(key);
            List<int[]> rows = new ArrayList<>();
            for (int z = 0; z < 32; z++) {
                int x0 = -1;
                int x1 = -1;
                for (int x = 0; x < 32; x++) {
                    if (Math.hypot(x + 0.5 - CENTER, z + 0.5 - CENTER) < r) {
                        if (x0 < 0) {
                            x0 = x;
                        }
                        x1 = x + 1;
                    }
                }
                if (x0 >= 0) {
                    rows.add(new int[]{z, x0, x1});
                }
            }
            return rows.toArray(new int[0][]);
        });
    }

    /** 第 slot 格：模型底面中心摆到哪 {x, y, z}、绕竖轴转多少度 */
    public static float[] slot(int slot) {
        float[] place = SLOT_PLACES[slot];
        double a = Math.toRadians(place[1]);
        return new float[]{CENTER + place[0] * (float) Math.cos(a), place[2],
                CENTER + place[0] * (float) Math.sin(a), place[1] + 90};
    }

    /**
     * 锅里的食材（按放进去的顺序、同样的摞成一堆）摊成一份一份占格子：每样先占一格，格子还有富余再给放了好几个的多占
     * （每样最多 {@link #MAX_COPIES} 份），一共最多 {@link #SLOTS} 份。和 layout.py 的 pieces 一样
     */
    public static List<ItemStack> pieces(List<ItemStack> stacks) {
        List<ItemStack> out = new ArrayList<>();
        for (ItemStack stack : stacks) {
            if (out.size() < SLOTS) {
                out.add(stack);
            }
        }
        for (int extra = 1; extra < MAX_COPIES && out.size() < SLOTS; extra++) {
            for (ItemStack stack : stacks) {
                if (stack.getCount() > extra && out.size() < SLOTS) {
                    out.add(stack);
                }
            }
        }
        return out;
    }

    // ---------------------------------------------------------------- 调料、颜色

    /** 生大米：摊开在锅底，不占格子 */
    public static boolean isSpread(ItemStack stack) {
        return stack.is(ModTags.CROPS_RICE);
    }

    /** 锅底摊开那一层的顶面高度（像素） */
    public static float spreadY(int count) {
        return FLOOR + SPREAD_STEP * Math.min(count, SPREAD_MAX);
    }

    public static boolean isSeasoning(ItemStack stack) {
        return seasoningOf(stack) != null;
    }

    @Nullable
    private static Seasoning seasoningOf(ItemStack stack) {
        for (Seasoning seasoning : SEASONINGS) {
            if (stack.is(seasoning.tag())) {
                return seasoning;
            }
        }
        return null;
    }

    /**
     * 液面颜色（ARGB）：流体本来的颜色，每倒进去一份调料就往这种调料的颜色靠一点。
     * stacks 里不是调料的东西不算；数量就是倒了几份。
     */
    public static int liquidColor(Fluid fluid, List<ItemStack> stacks) {
        int base = VatRenderer.fluidTint(fluid);
        float r = (base >> 16) & 0xFF;
        float g = (base >> 8) & 0xFF;
        float b = base & 0xFF;
        float alpha = LIQUID_ALPHA;
        for (ItemStack stack : stacks) {
            Seasoning seasoning = seasoningOf(stack);
            if (seasoning == null) {
                continue;
            }
            for (int i = 0; i < stack.getCount(); i++) {
                float w = seasoning.weight();
                r += (((seasoning.color() >> 16) & 0xFF) - r) * w;
                g += (((seasoning.color() >> 8) & 0xFF) - g) * w;
                b += ((seasoning.color() & 0xFF) - b) * w;
                alpha = Math.min(LIQUID_ALPHA_MAX, alpha + 0.02F);
            }
        }
        return (Math.round(alpha * 255) << 24) | (Math.round(r) << 16) | (Math.round(g) << 8) | Math.round(b);
    }

    // ---------------------------------------------------------------- 模型

    /** 这样食材在锅里的模型；没有专门的模型返回 null（画成平躺的物品） */
    @Nullable
    public static BakedModel ingredientModel(ItemStack stack) {
        for (IngredientModel entry : ingredientModels()) {
            if (entry.test().test(stack)) {
                return baked(DdIds.of(NortheastChinaDelight.MODID, "block/big_pot/" + entry.name()));
            }
        }
        return null;
    }

    /** 煮好的菜在锅里的模型（block/big_pot/dish/<成品物品名>）；没有返回 null */
    @Nullable
    public static BakedModel dishModel(ItemStack result) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(result.getItem());
        return baked(DdIds.of(id.getNamespace(), "block/big_pot/dish/" + id.getPath()));
    }

    @Nullable
    private static BakedModel baked(ResourceLocation id) {
        BakedModel model = WoodenMortarRenderer.bakedModel(id);
        if (model == null || model == Minecraft.getInstance().getModelManager().getMissingModel()) {
            // 本模组自己的模型找不到多半是没登记上烘焙，记一行（每个只记一次），别的模组的物品本来就没有
            if (NortheastChinaDelight.MODID.equals(id.getNamespace()) && MISSING_LOGGED.add(id)) {
                LOGGER.warn("[大锅] 找不到锅里的模型 {}，画成平放的物品", id);
            }
            return null;
        }
        return model;
    }

    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();
    private static final java.util.Set<ResourceLocation> MISSING_LOGGED = new java.util.HashSet<>();

    private static List<IngredientModel> ingredientModels() {
        if (ingredientModels == null) {
            List<IngredientModel> list = new ArrayList<>();
            item(list, ModItems.PORK_RIBS.get(), "pork_ribs");
            item(list, ModItems.PORK_INTESTINE.get(), "pork_intestine");
            item(list, ModItems.BLOOD_SAUSAGE.get(), "blood_sausage");
            item(list, ModItems.FROZEN_TOFU.get(), "frozen_tofu");
            item(list, ModItems.HAZEL_MUSHROOM.get(), "hazel_mushroom");
            item(list, Items.CHICKEN, "chicken");
            tag(list, ModTags.FOODS_RAW_PORK, "raw_pork");
            tag(list, ModTags.FOODS_RAW_BEEF, "raw_beef");
            tag(list, ModTags.FOODS_RAW_MUTTON, "raw_mutton");
            tag(list, ModTags.FOODS_OYSTER, "oyster");
            tag(list, ModTags.FOODS_SHRIMP, "shrimp");
            tag(list, ModTags.FOODS_SAUERKRAUT, "sauerkraut");
            tag(list, ModTags.FOODS_VERMICELLI, "vermicelli");
            tag(list, ModTags.FOODS_TOFU, "tofu");
            tag(list, ModTags.FOODS_DOUGH_WHEAT, "dough");
            tag(list, ModTags.CROPS_GREEN_BEAN, "green_bean");
            tag(list, ModTags.CROPS_CORN, "corn");
            tag(list, ModTags.CROPS_POTATO, "potato");
            tag(list, ModTags.CROPS_EGGPLANT, "eggplant");
            tag(list, ModTags.CROPS_GREEN_PEPPER, "green_pepper");
            tag(list, ModTags.CROPS_NAPA_CABBAGE, "napa_cabbage");
            tag(list, ModTags.CROPS_GREEN_ONION, "green_onion");
            ingredientModels = List.copyOf(list);
        }
        return ingredientModels;
    }

    private static void item(List<IngredientModel> list, Item item, String name) {
        list.add(new IngredientModel(stack -> stack.is(item), name));
    }

    private static void tag(List<IngredientModel> list, TagKey<Item> tag, String name) {
        list.add(new IngredientModel(stack -> stack.is(tag), name));
    }
}
