package com.gunmu.northeast_china_delight.compat.ali;

import com.yanny.aci.api.NumberExpr;
import com.yanny.aci.tooltip.TooltipBuilder;
import com.yanny.ali.api.IDataNode;
import com.yanny.ali.api.IOperation;
import com.yanny.ali.api.IServerUtils;
import com.yanny.ali.plugin.common.NodeUtils;
import com.yanny.ali.plugin.common.nodes.ItemNode;
import com.yanny.ali.plugin.server.LootCount;
import com.yanny.ali.plugin.server.TooltipUtils;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * 把本模组 GLM 的掉落拼成 ALI 的操作（{@link IOperation}）。
 *
 * <p>节点一律用 ALI 自己的 {@code ItemNode}（id 为 {@code ali:item}），这样客户端用 ALI 现成的
 * 物品控件渲染、并且能正常联网传输，我们不需要额外注册控件。</p>
 */
final class AliGlmNodes {
    /** 数量：区间或定值 */
    static NumberExpr count(int min, int max) {
        return min == max ? NumberExpr.constant(min) : NumberExpr.range(min, max);
    }

    /** 「会掉出某物品」，物品列在对应页面的条目里 */
    static IOperation add(IServerUtils utils, List<LootItemCondition> conditions, Item item, float chance, NumberExpr count) {
        return new IOperation.AddOperation(stack -> true, node(utils, conditions, new ItemStack(item), chance, count));
    }

    /** 一张加权表：每个物品的概率是 rollChance × 它在表里的权重占比 */
    static List<IOperation> table(IServerUtils utils, List<LootItemCondition> conditions, AliGlmReflection.Table table,
                                 float rollChance, NumberExpr count) {
        List<IOperation> operations = new ArrayList<>();
        int total = table.total();

        if (total <= 0) {
            return operations;
        }

        for (AliGlmReflection.Entry entry : table.entries()) {
            operations.add(add(utils, conditions, entry.item(), rollChance * entry.weight() / total, count));
        }

        return operations;
    }

    /** 多张表里任选其一（钓鱼按水域、村庄种子按群系）：同一物品取最大占比，避免重复列 */
    static List<IOperation> union(IServerUtils utils, List<LootItemCondition> conditions,
                                 List<AliGlmReflection.Table> tables, NumberExpr count) {
        Map<Item, Float> best = new LinkedHashMap<>();

        for (AliGlmReflection.Table table : tables) {
            int total = table.total();

            if (total <= 0) {
                continue;
            }

            for (AliGlmReflection.Entry entry : table.entries()) {
                best.merge(entry.item(), (float) entry.weight() / total, Math::max);
            }
        }

        List<IOperation> operations = new ArrayList<>();
        best.forEach((item, chance) -> operations.add(add(utils, conditions, item, chance, count)));
        return operations;
    }

    /** 有条件的替换（着火时猪肘/猪肝换成熏制版），ALI 会画成「被修改」 */
    static IOperation replace(IServerUtils utils, List<LootItemCondition> conditions, Item from, Item to,
                             float chance, NumberExpr count) {
        return new IOperation.ReplaceOperation(stack -> stack.is(from),
                source -> List.of(node(utils, conditions, new ItemStack(to), chance, count)));
    }

    /** 移除（本模组只用于箱子表上按配置过滤物品） */
    static IOperation remove(Predicate<ItemStack> predicate) {
        return new IOperation.RemoveOperation(predicate, source -> null);
    }

    static IDataNode node(IServerUtils utils, List<LootItemCondition> conditions, ItemStack stack, float chance, NumberExpr count) {
        LootCount chanceCount = NodeUtils.getChance(utils, conditions, chance);
        TooltipBuilder tooltip = TooltipUtils.getTooltip(utils, LootPoolSingletonContainer.DEFAULT_QUALITY,
                chanceCount, LootCount.of(count), NodeUtils.getCountLimit(stack), List.of(), conditions);

        return new ItemNode(chance, count, stack, tooltip.build(), List.of(), conditions);
    }

    private AliGlmNodes() {}
}