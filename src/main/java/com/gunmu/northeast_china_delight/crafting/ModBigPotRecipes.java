package com.gunmu.northeast_china_delight.crafting;

import com.gunmu.northeast_china_delight.NortheastChinaDelight;
import com.gunmu.northeast_china_delight.util.DdIds;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
//? if <1.20.2 {
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
//?} else {
/*import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
*///?}

import java.util.function.Supplier;

/** 「大锅烹饪」配方类型（大灶上的大铁锅，见 BigPotCooking） */
public final class ModBigPotRecipes {

    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, NortheastChinaDelight.MODID);
    private static final DeferredRegister<RecipeType<?>> TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, NortheastChinaDelight.MODID);

    public static final Supplier<RecipeSerializer<BigPotDataRecipe>> SERIALIZER =
            SERIALIZERS.register("big_pot_cooking", BigPotDataRecipe.Serializer::new);
    public static final Supplier<RecipeType<BigPotDataRecipe>> TYPE =
            TYPES.register("big_pot_cooking", () -> RecipeType.simple(DdIds.of(NortheastChinaDelight.MODID, "big_pot_cooking")));

    private ModBigPotRecipes() {
    }

    public static void register(IEventBus eventBus) {
        SERIALIZERS.register(eventBus);
        TYPES.register(eventBus);
    }
}
