package com.gunmu.northeast_china_delight.compat.jei;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * JEI 里展示的一条大缸配方。纯图形，没有任何文字。一条配方可以有多个「档位」：大缸的不少配方是按水位配比的（水位几层就配几份料），例如辣白菜在 1 / 2 / 3 层水时白菜
 * 分别是 2 / 4 / 6 份、调味品是 1 / 1 / 2 份。多档配方在 JEI 里每隔 1.5 秒自动轮换一档，把每个水位下的投料和产物都演一遍。
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

    public record State(
            @Nullable Fluid liquid,
            long liquidMb,
            long liquidCapacityMb,
            List<List<ItemStack>> seasoning,
            List<List<ItemStack>> primary,
            List<ItemStack> seal,
            ItemStack result,
            @Nullable Fluid resultFluid,
            long resultFluidMb,
            long resultFluidCapacityMb,
            int seconds,
            @Nullable net.minecraft.network.chat.Component note) {

        public boolean hasLiquid() {
            return this.liquid != null && this.liquidMb > 0;
        }

        public boolean hasResultFluid() {
            return this.resultFluid != null && this.resultFluidMb > 0;
        }
    }
}
