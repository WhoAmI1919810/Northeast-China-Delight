package com.gunmu.northeast_china_delight.loot;

import com.gunmu.chinese_regional_culinary_ingredients.region.CrciAvailability;
import com.gunmu.chinese_regional_culinary_ingredients.region.CrciUseSite;
import com.gunmu.northeast_china_delight.NortheastChinaConfig;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
//? if <1.20.2 {
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
//?} else {
/*import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;
*///?}

public final class ChestLootConfigModifier extends LootModifier {
    //? if <1.20.5 {
    public static final Codec<ChestLootConfigModifier> CODEC = RecordCodecBuilder.create(
            instance -> codecStart(instance).apply(instance, ChestLootConfigModifier::new));

    //?} else {
    /*public static final MapCodec<ChestLootConfigModifier> CODEC = RecordCodecBuilder.mapCodec(
            instance -> codecStart(instance).apply(instance, ChestLootConfigModifier::new));
    *///?}

    public ChestLootConfigModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    //? if <1.20.5 {
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }

    //?} else {
    /*public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
    *///?}

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        //? if <1.20.5 {
        LootTable table = context.getLevel().getServer().getLootData()
                .getLootTable(context.getQueriedLootTableId());

        //?} else {
        /*LootTable table = context.getLevel().getServer().reloadableRegistries()
                .getLootTable(ResourceKey.create(Registries.LOOT_TABLE, context.getQueriedLootTableId()));
        *///?}
        if (table.getParamSet() == LootContextParamSets.CHEST) {
            generatedLoot.removeIf(stack -> !allowed(stack));
        }
        return generatedLoot;
    }

    public static void addIfAllowed(ObjectArrayList<ItemStack> generatedLoot, ItemStack stack) {
        if (!stack.isEmpty() && allowed(stack)) {
            generatedLoot.add(stack);
        }
    }

    private static boolean allowed(ItemStack stack) {
        var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id != null && NortheastChinaConfig.isChestLootItemAllowed(id)
                && CrciAvailability.isAvailable(stack, CrciUseSite.LOOT);
    }
}
