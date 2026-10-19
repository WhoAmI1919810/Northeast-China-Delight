package com.gunmu.northeast_china_delight.compat.jei;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * JEI 里展示的一条大缸配方。纯图形，没有任何文字。一条配方可以有多个「档位」：大缸的不少配方是按水位配比的（水位几层就配几份料），
 * 多档配方在 JEI 里每隔 1.5 秒自动轮换一档，把每个水位下的投料和产物都演一遍。无液体配方只有一个档位。
 */
public record VatJeiRecipe(net.minecraft.resources.ResourceLocation id, List<State> states) {

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
            List<ItemStack> results,
            @Nullable Fluid resultFluid,
            long resultFluidMb,
            long resultFluidCapacityMb,
            int seconds,
            net.minecraft.network.chat.Component verb,
            String timeKey,
            List<ItemStack> collectionContainers,
            @Nullable net.minecraft.network.chat.Component note) {

        public boolean hasLiquid() {
            return this.liquid != null && this.liquidMb > 0;
        }

        public boolean hasResultItem() {
            return !this.results.isEmpty();
        }

        public boolean hasResultFluid() {
            return this.resultFluid != null && this.resultFluidMb > 0;
        }
    }
}
