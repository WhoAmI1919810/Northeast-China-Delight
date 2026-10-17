package com.gunmu.northeast_china_delight.compat.jei;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** JEI 里的一条「舂捣」配方，保留全部输入、产物和阶段模型信息。 */
public record PoundJeiRecipe(ResourceLocation id, List<Input> inputs, List<ItemStack> tool,
                             int poundTimes, float chargeThreshold, List<Output> outputs,
                             List<ResourceLocation> stageModels) {

    /** {@code items} 中每个候选的 count 已经设置成配方需求数量。 */
    public record Input(List<ItemStack> items, int count) {
    }

    /** 随机数量范围用 minCount..maxCount 表示；固定数量时两者相同。 */
    public record Output(ItemStack stack, int minCount, int maxCount, float chance, boolean byproduct,
                         @Nullable String pool, int weight, int poolTotalWeight, float poolChance) {
        public boolean pooled() {
            return this.pool != null;
        }
    }
}
