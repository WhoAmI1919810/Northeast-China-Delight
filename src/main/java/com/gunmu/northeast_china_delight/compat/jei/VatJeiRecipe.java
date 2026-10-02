package com.gunmu.northeast_china_delight.compat.jei;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * JEI 里展示的一条大缸配方。纯图形，没有任何文字。
 *
 * 一条配方可以有多个「档位」：大缸的不少配方是**按水位配比**的（水位几层就配几份料），
 * 例如辣白菜在 1 / 2 / 3 层水时白菜分别是 2 / 4 / 6 份、调味品是 1 / 1 / 2 份。
 * 多档配方在 JEI 里每隔 1.5 秒自动轮换一档，把每个水位下的投料和产物都演一遍。
 *
 * 每一档的画法：左侧竖排是要放进缸里的东西（顶部封缸物 → 配料 → 食材 → 缸底液体），
 * 右侧是产物。数量写在物品格上、水位画成液面高度。
 */
public record VatJeiRecipe(List<State> states) {

    public VatJeiRecipe(State state) {
        this(List.of(state));
    }

    /** 第一档决定界面上的槽位布局，所以多档配方各档的槽位结构必须一致 */
    public State first() {
        return this.states.get(0);
    }

    public boolean animated() {
        return this.states.size() > 1;
    }

    /**
     * 一个水位档位。
     *
     * {@code seasoning} / {@code primary} 是「槽位列表」：外层一个元素 = 一个槽，
     * 内层放多个物品 = JEI 在同一个槽里轮播（表示任选其一）。
     */
    public record State(
            /** 缸底的液体（水 / 酸引水）；null 表示这档不加水 */
            @Nullable Fluid liquid,
            long liquidMb,
            /** 液体格容量：把「水位能到 3 层」这类信息画成液面高度 */
            long liquidCapacityMb,
            /** 配料（盐、小麦、辣椒酱、谷物……） */
            List<List<ItemStack>> seasoning,
            /** 食材 */
            List<List<ItemStack>> primary,
            /** 顶部封缸物：压缸石 / 羊毛地毯 / 粗布毯 */
            List<ItemStack> seal,
            ItemStack result,
            /** 酿出来的液体；null 表示没有 */
            @Nullable Fluid resultFluid,
            long resultFluidMb,
            long resultFluidCapacityMb,
            /** 这一档的发酵时长（秒），显示在箭头下方 */
            int seconds,
            /** 额外条件注记（冻梨的"要在会下雪的群系"），写在时长下方；null 表示没有 */
            @Nullable net.minecraft.network.chat.Component note) {

        public boolean hasLiquid() {
            return this.liquid != null && this.liquidMb > 0;
        }

        public boolean hasResultFluid() {
            return this.resultFluid != null && this.resultFluidMb > 0;
        }
    }
}
