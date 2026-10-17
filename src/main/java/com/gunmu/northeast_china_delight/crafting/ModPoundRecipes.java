package com.gunmu.northeast_china_delight.crafting;

import com.gunmu.northeast_china_delight.NortheastChinaDelight;
import com.gunmu.northeast_china_delight.util.DdIds;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
//? if <1.20.2 {
/*import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
*///?} else {
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
//?}

import java.util.function.Supplier;

public final class ModPoundRecipes {

    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, NortheastChinaDelight.MODID);
    private static final DeferredRegister<RecipeType<?>> TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, NortheastChinaDelight.MODID);

    public static final Supplier<RecipeSerializer<PoundDataRecipe>> SERIALIZER =
            SERIALIZERS.register("pounding", PoundDataRecipe.Serializer::new);
    public static final Supplier<RecipeType<PoundDataRecipe>> TYPE =
            TYPES.register("pounding", () -> RecipeType.simple(DdIds.of(NortheastChinaDelight.MODID, "pounding")));

    private ModPoundRecipes() {
    }

    public static void register(IEventBus eventBus) {
        SERIALIZERS.register(eventBus);
        TYPES.register(eventBus);
    }
}
