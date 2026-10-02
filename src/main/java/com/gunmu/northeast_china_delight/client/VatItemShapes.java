package com.gunmu.northeast_china_delight.client;

import com.gunmu.northeast_china_delight.crafting.VatRecipes;
import com.gunmu.northeast_china_delight.item.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * 大缸里每种食材的「小模型」。
 *
 * <p>做法：按现实里这种食材的形状摆几个盒子（白菜是下窄上宽的菜帮子加散开的叶子、
 * 黄瓜胡萝卜是躺着的长条加尖端、鱼有身子和尾鳍、猪排有肉有骨头、面包有鼓起和刀口…），
 * 盒子贴图直接取**物品自己的贴图**，而且是按盒子在模型里的位置取对应那一小块
 * （模型下部的盒子取图标下部、上部的盒子取图标上部），所以整体看起来就是
 * 「把物品图标立起来做成 mc 风格的小模型」。
 *
 * <p>所有坐标归一化到 0~1 立方体（y 向上，原点在底部一角），由 {@link VatRenderer}
 * 缩放到缸里：一层三个、每个比一层水略矮。
 */
public final class VatItemShapes {

    private VatItemShapes() {
    }

    /**
     * 一个盒子。
     *
     * @param tint 颜色倍数（0xFFFFFF 原色），用来区分「新鲜的 / 腌好的」这类差别
     */
    record Box(float x0, float y0, float z0, float x1, float y1, float z1, int tint) {
    }

    private static Box box(float x0, float y0, float z0, float x1, float y1, float z1, int tint) {
        return new Box(x0, y0, z0, x1, y1, z1, tint);
    }

    /** 大缸里这种物品该长什么样 */
    static List<Box> of(ItemStack stack) {
        Item item = stack.getItem();
        int fresh = 0xFFFFFF;

        // ===== 白菜 / 酸菜 / 辣白菜：一颗立着的菜（下窄上宽，顶上收拢） =====
        if (item == ModItems.NAPA_CABBAGE.get() || item == ModItems.SOUR_CABBAGE.get()
                || item == ModItems.SPICY_CABBAGE.get()) {
            int tint = item == ModItems.SOUR_CABBAGE.get() ? 0xE8F0C8
                    : item == ModItems.SPICY_CABBAGE.get() ? 0xFFD8C0 : fresh;
            return List.of(
                    box(0.30F, 0.00F, 0.30F, 0.70F, 0.34F, 0.70F, tint),   // 菜帮：细长的一段白帮
                    box(0.14F, 0.30F, 0.14F, 0.86F, 0.70F, 0.86F, tint),   // 中段（胖起来）
                    box(0.08F, 0.66F, 0.08F, 0.92F, 0.90F, 0.92F, tint),   // 最胖的一段
                    box(0.20F, 0.88F, 0.20F, 0.80F, 1.00F, 0.80F, tint));  // 顶叶：收拢
        }

        // ===== 黄瓜 / 胡萝卜 / 青萝卜 / 腌萝卜条：躺着的长条，一头收细 =====
        if (item == ModItems.CUCUMBER.get() || item == Items.CARROT
                || item == ModItems.GREEN_RADISH.get() || item == ModItems.PICKLED_CUCUMBER.get()
                || item == ModItems.PICKLED_CARROT.get() || item == ModItems.PICKLED_GREEN_RADISH.get()) {
            boolean pickled = item == ModItems.PICKLED_CUCUMBER.get() || item == ModItems.PICKLED_CARROT.get()
                    || item == ModItems.PICKLED_GREEN_RADISH.get();
            int tint = pickled ? 0xF0E0B0 : fresh;
            boolean leafy = item == Items.CARROT || item == ModItems.GREEN_RADISH.get();  // 这两种带缨子
            List<Box> boxes = new ArrayList<>(List.of(
                    box(0.32F, 0.06F, 0.06F, 0.68F, 0.58F, 0.62F, tint),   // 粗的那头
                    box(0.36F, 0.10F, 0.60F, 0.64F, 0.52F, 0.86F, tint),   // 中段
                    box(0.42F, 0.16F, 0.84F, 0.58F, 0.44F, 1.00F, tint))); // 收细的尖
            if (leafy) {
                boxes.add(box(0.38F, 0.54F, 0.02F, 0.62F, 0.82F, 0.16F, 0x9CC860));  // 缨子
            }
            return boxes;
        }

        // ===== 猪肉 / 咸腊肉：一块肉 + 一条肥边 + 一根骨头 =====
        if (item == Items.PORKCHOP || item == ModItems.SALTED_PORK.get()) {
            int meat = item == ModItems.SALTED_PORK.get() ? 0xE0B8A8 : fresh;
            return List.of(
                    box(0.06F, 0.06F, 0.12F, 0.86F, 0.46F, 0.88F, meat),   // 瘦肉
                    box(0.12F, 0.44F, 0.20F, 0.78F, 0.60F, 0.80F, 0xFFE8DC),// 肥边
                    box(0.78F, 0.08F, 0.34F, 1.00F, 0.34F, 0.56F, 0xFFF4E8));// 骨头
        }

        // ===== 鱼（生鱼 / 咸鱼 / 带鱼）：身子 + 头 + 背鳍 + 尾鳍 =====
        if (VatRecipes.isRawFish(stack) || item == ModItems.SALTED_FISH.get()) {
            int tint = item == ModItems.SALTED_FISH.get() ? 0xE8D4B4 : fresh;
            return List.of(
                    box(0.00F, 0.16F, 0.30F, 0.12F, 0.48F, 0.72F, tint),   // 头
                    box(0.10F, 0.08F, 0.18F, 0.70F, 0.54F, 0.84F, tint),   // 身子
                    box(0.30F, 0.52F, 0.42F, 0.58F, 0.68F, 0.58F, tint),   // 背鳍
                    box(0.68F, 0.06F, 0.32F, 0.98F, 0.52F, 0.68F, tint));  // 尾鳍
        }

        // ===== 大虾：弯着的三节身子 + 尾扇 + 头 =====
        if (item == ModItems.SHRIMP.get()) {
            return List.of(
                    box(0.54F, 0.04F, 0.46F, 0.88F, 0.40F, 0.76F, fresh),   // 尾扇
                    box(0.30F, 0.08F, 0.30F, 0.60F, 0.48F, 0.66F, 0xFFE0D0),// 后段
                    box(0.10F, 0.12F, 0.22F, 0.38F, 0.52F, 0.58F, fresh),   // 前段
                    box(0.02F, 0.20F, 0.34F, 0.14F, 0.46F, 0.56F, fresh));  // 头
        }

        // ===== 谷物（玉米粒 / 荞麦 / 黄豆 / 酸玉米粒）：一把散粒 =====
        if (item == ModItems.CORN_SEEDS.get() || item == ModItems.BUCKWHEAT.get()
                || item == ModItems.SOYBEAN.get() || item == ModItems.SOUR_CORN_KERNELS.get()) {
            int tint = item == ModItems.SOUR_CORN_KERNELS.get() ? 0xF0E0A0 : fresh;
            return List.of(
                    box(0.06F, 0.00F, 0.16F, 0.26F, 0.20F, 0.36F, tint),
                    box(0.30F, 0.00F, 0.06F, 0.52F, 0.22F, 0.28F, tint),
                    box(0.56F, 0.00F, 0.14F, 0.78F, 0.19F, 0.34F, tint),
                    box(0.80F, 0.00F, 0.24F, 0.98F, 0.17F, 0.42F, tint),
                    box(0.14F, 0.00F, 0.46F, 0.38F, 0.24F, 0.68F, tint),
                    box(0.44F, 0.00F, 0.52F, 0.68F, 0.21F, 0.74F, tint),
                    box(0.70F, 0.00F, 0.56F, 0.94F, 0.18F, 0.78F, tint));
        }

        // ===== 小麦：一束麦子（几根麦秆 + 麦穗） =====
        if (item == Items.WHEAT) {
            return List.of(
                    box(0.20F, 0.00F, 0.30F, 0.32F, 0.66F, 0.44F, 0xE8D8A0),
                    box(0.44F, 0.00F, 0.22F, 0.56F, 0.72F, 0.36F, 0xE8D8A0),
                    box(0.66F, 0.00F, 0.32F, 0.78F, 0.64F, 0.46F, 0xE8D8A0),
                    box(0.16F, 0.62F, 0.26F, 0.36F, 0.94F, 0.48F, 0xFFE8A8),  // 麦穗
                    box(0.40F, 0.68F, 0.18F, 0.60F, 1.00F, 0.40F, 0xFFE8A8),
                    box(0.62F, 0.60F, 0.28F, 0.82F, 0.92F, 0.50F, 0xFFE8A8));
        }

        // ===== 豆芽：几根立着的芽（细茎 + 顶上一颗豆） =====
        if (item == ModItems.BEAN_SPROUTS.get()) {
            return List.of(
                    box(0.16F, 0.00F, 0.24F, 0.26F, 0.74F, 0.34F, 0xF8FFE8),  // 茎
                    box(0.12F, 0.72F, 0.20F, 0.30F, 0.92F, 0.38F, 0xE8F0A0),  // 豆瓣
                    box(0.44F, 0.00F, 0.14F, 0.54F, 0.80F, 0.24F, 0xF8FFE8),
                    box(0.40F, 0.78F, 0.10F, 0.58F, 0.98F, 0.28F, 0xE8F0A0),
                    box(0.68F, 0.00F, 0.30F, 0.78F, 0.70F, 0.40F, 0xF8FFE8),
                    box(0.64F, 0.68F, 0.26F, 0.82F, 0.88F, 0.44F, 0xE8F0A0));
        }

        // ===== 面包：面包身 + 鼓起的顶 + 两道刀口 =====
        if (item == Items.BREAD) {
            return List.of(
                    box(0.04F, 0.00F, 0.16F, 0.96F, 0.40F, 0.84F, fresh),      // 面包身
                    box(0.12F, 0.38F, 0.22F, 0.88F, 0.62F, 0.78F, 0xE8C08C),   // 顶
                    box(0.22F, 0.60F, 0.32F, 0.44F, 0.68F, 0.68F, 0xB88448),  // 刀口
                    box(0.56F, 0.60F, 0.32F, 0.78F, 0.68F, 0.68F, 0xB88448));
        }

        // ===== 酱块 / 酱渣：三块粗糙的疙瘩 =====
        if (item == ModItems.SOY_PASTE_CHUNK.get() || item == ModItems.SOY_RESIDUE.get()) {
            int tint = item == ModItems.SOY_RESIDUE.get() ? 0xD0B090 : fresh;
            return List.of(
                    box(0.06F, 0.00F, 0.18F, 0.46F, 0.42F, 0.66F, tint),
                    box(0.36F, 0.00F, 0.10F, 0.78F, 0.52F, 0.58F, tint),
                    box(0.24F, 0.34F, 0.44F, 0.66F, 0.78F, 0.90F, tint));
        }

        // ===== 盐：一小堆白盐 =====
        if (item == ModItems.SALT.get()) {
            return List.of(
                    box(0.10F, 0.00F, 0.10F, 0.90F, 0.16F, 0.90F, fresh),
                    box(0.24F, 0.14F, 0.24F, 0.76F, 0.30F, 0.76F, 0xF4F8FF),
                    box(0.36F, 0.28F, 0.36F, 0.64F, 0.42F, 0.64F, fresh));
        }

        // ===== 瓶装调料（辣椒酱 / 鱼露 / 虾酱）：瓶身 + 肩 + 颈 + 盖 =====
        if (item == ModItems.CHILI_SAUCE.get() || item == ModItems.FISH_SAUCE.get()
                || item == ModItems.SHRIMP_PASTE.get()) {
            int bodyTint = item == ModItems.CHILI_SAUCE.get() ? 0xFFD8D0
                    : item == ModItems.FISH_SAUCE.get() ? 0xFFE0B0 : 0xFFD0C0;
            return List.of(
                    box(0.28F, 0.00F, 0.28F, 0.72F, 0.54F, 0.72F, bodyTint),   // 瓶身
                    box(0.34F, 0.52F, 0.34F, 0.66F, 0.70F, 0.66F, fresh),      // 肩
                    box(0.42F, 0.68F, 0.42F, 0.58F, 0.86F, 0.58F, fresh),      // 颈
                    box(0.38F, 0.84F, 0.38F, 0.62F, 0.98F, 0.62F, 0xE0E0E8));  // 盖
        }

        // ===== 水面团：一坨压扁的面团 =====
        if (item == ModItems.WATER_DOUGH.get()) {
            return List.of(
                    box(0.10F, 0.00F, 0.10F, 0.90F, 0.34F, 0.90F, fresh),
                    box(0.20F, 0.32F, 0.20F, 0.80F, 0.56F, 0.80F, 0xF4F0E8));
        }

        // ===== 其它能进宫的东西：圆乎乎的一小块 =====
        return List.of(
                box(0.12F, 0.00F, 0.12F, 0.88F, 0.44F, 0.88F, fresh),
                box(0.24F, 0.42F, 0.24F, 0.76F, 0.66F, 0.76F, 0xF0E8E0));
    }

    /**
     * 这种食材要按几个「层高」来渲染（1.0 = 正好一层）。
     *
     * <p>白菜比别的食材长一截（1.16），整颗按这个倍数往高里拉 —— 注意是**整体拉伸**，
     * 盒子的坐标仍然在 0~1 之间，UV 才不会越界采到隔壁贴图。
     *
     * <p>压缸石也用这个高度算落位，所以石块永远坐在菜顶上，不会啃进叶子里。
     */
    static float heightScale(ItemStack stack) {
        Item item = stack.getItem();
        if (item == ModItems.NAPA_CABBAGE.get() || item == ModItems.SOUR_CABBAGE.get()
                || item == ModItems.SPICY_CABBAGE.get()) {
            return 1.16F;
        }
        return 1.0F;
    }

    /**
     * 这种食材泡进水里会把水染成什么颜色（0xFFFFFF 表示不染色）。
     * 辣椒酱、酱块这类颜色重的东西会给缸里的水明显换色。
     */
    public static int liquidTint(ItemStack stack) {
        Item item = stack.getItem();
        if (item == ModItems.CHILI_SAUCE.get()) {
            return 0xE0553C;               // 辣椒酱：红
        }
        if (item == ModItems.SOY_PASTE_CHUNK.get() || item == ModItems.SOY_RESIDUE.get()) {
            return 0x6E4A26;               // 酱块 / 酱渣：深酱色
        }
        if (item == ModItems.FISH_SAUCE.get()) {
            return 0xB0703A;
        }
        if (item == ModItems.SHRIMP_PASTE.get()) {
            return 0xC0603A;
        }
        if (VatRecipes.isRawFish(stack)) {
            return 0xE0B0A0;               // 生鱼：粉白，水会发浑
        }
        if (item == Items.PORKCHOP) {
            return 0xE0A0A0;
        }
        if (item == ModItems.SHRIMP.get()) {
            return 0xE8A88C;
        }
        if (item == ModItems.NAPA_CABBAGE.get() || item == ModItems.CUCUMBER.get()
                || item == ModItems.GREEN_RADISH.get() || item == Items.CARROT
                || item == ModItems.SOUR_CABBAGE.get() || item == ModItems.SPICY_CABBAGE.get()) {
            return 0xA8C070;               // 蔬菜：淡绿
        }
        if (item == Items.BREAD || item == ModItems.CORN_SEEDS.get() || item == ModItems.BUCKWHEAT.get()
                || item == Items.WHEAT || item == ModItems.SOYBEAN.get()
                || item == ModItems.BEAN_SPROUTS.get() || item == ModItems.SOUR_CORN_KERNELS.get()) {
            return 0xE0C078;               // 谷物 / 面包：米黄
        }
        if (item == ModItems.SALT.get()) {
            return 0xF2F6FF;               // 盐：发白
        }
        return 0xFFFFFF;
    }

    /** 这种食材染水的力度（0~1），颜色越重的东西染得越狠 */
    public static float liquidStrength(ItemStack stack) {
        Item item = stack.getItem();
        if (item == ModItems.CHILI_SAUCE.get()) {
            return 0.72F;                  // 辣椒酱：一缸盐水会明显发红
        }
        if (item == ModItems.SOY_PASTE_CHUNK.get() || item == ModItems.SOY_RESIDUE.get()) {
            return 0.62F;
        }
        if (item == ModItems.FISH_SAUCE.get() || item == ModItems.SHRIMP_PASTE.get()) {
            return 0.55F;
        }
        if (VatRecipes.isRawFish(stack) || item == Items.PORKCHOP || item == ModItems.SHRIMP.get()) {
            return 0.28F;
        }
        if (item == ModItems.SALT.get()) {
            return 0.5F;
        }
        return 0.26F;
    }
}
