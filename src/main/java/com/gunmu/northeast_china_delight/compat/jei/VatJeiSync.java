package com.gunmu.northeast_china_delight.compat.jei;

import com.gunmu.chinese_regional_culinary_ingredients.crafting.VatRecipeValidator;
import com.gunmu.chinese_regional_culinary_ingredients.crafting.VatRecipes;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
//? if <1.20.2 {
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.client.event.RecipesUpdatedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
//?} else {
/*import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.RecipesUpdatedEvent;
import net.neoforged.bus.api.SubscribeEvent;
*///?}

public final class VatJeiSync {

    private static final Logger LOGGER = LogUtils.getLogger();

    private VatJeiSync() {
    }

    public static void register() {
        //? if <1.20.2 {
        MinecraftForge.EVENT_BUS.register(VatJeiSync.class);
        //?} else {
        /*NeoForge.EVENT_BUS.register(VatJeiSync.class);
        *///?}
    }

    @SubscribeEvent
    public static void onRecipesUpdated(RecipesUpdatedEvent event) {
        NortheastJeiPlugin.refreshRecipes(event.getRecipeManager());
        for (String problem : VatRecipeValidator.check(VatRecipes.all(event.getRecipeManager()), true)) {
            LOGGER.warn("[大缸配方] {}", problem);
        }
    }
}
