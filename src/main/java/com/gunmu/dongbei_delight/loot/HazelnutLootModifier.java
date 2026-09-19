package com.gunmu.dongbei_delight.loot;

import com.gunmu.dongbei_delight.item.ModItems;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

/**
 * 榛子掉落：破坏大型蕨时有 12.5% 概率额外掉一个榛子。
 *
 * 概率和原版大型蕨掉小麦种子的概率一致，所以「薅蕨草」时会时不时摸到榛子。
 * 用全局掉落修改器而不是覆盖原版战利品表，原版大型蕨的掉落行为不受影响。
 */
public class HazelnutLootModifier extends LootModifier {

    /** 掉落概率：12.5% */
    public static final float CHANCE = 0.125F;

    public static final MapCodec<HazelnutLootModifier> CODEC = RecordCodecBuilder.mapCodec(
            instance -> codecStart(instance).apply(instance, HazelnutLootModifier::new));

    public HazelnutLootModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        if (context.getRandom().nextFloat() < CHANCE) {
            generatedLoot.add(new ItemStack(ModItems.HAZELNUT.get()));
        }
        return generatedLoot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
