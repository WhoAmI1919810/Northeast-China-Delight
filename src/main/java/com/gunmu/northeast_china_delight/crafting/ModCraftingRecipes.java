package com.gunmu.northeast_china_delight.crafting;

import com.gunmu.northeast_china_delight.NortheastChinaDelight;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
//? if <1.20.2 {
/*import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
*///?} else {
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
//?}

import java.util.function.Supplier;

/** 工作台配方的变体（配方类型仍是原版 crafting，只换序列化器）。 */
public final class ModCraftingRecipes {

    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, NortheastChinaDelight.MODID);

    public static final Supplier<RecipeSerializer<WholeBottleShapelessRecipe>> WHOLE_BOTTLE_SHAPELESS =
            SERIALIZERS.register("whole_bottle_shapeless", WholeBottleShapelessRecipe.Serializer::new);

    private ModCraftingRecipes() {
    }

    public static void register(IEventBus eventBus) {
        SERIALIZERS.register(eventBus);
    }
}
