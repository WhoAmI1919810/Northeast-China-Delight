package com.gunmu.dongbei_delight.compat.jei;

import com.gunmu.dongbei_delight.DongbeiDelight;
import com.gunmu.dongbei_delight.item.ModItems;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.ResourceLocation;

/**
 * JEI 联动：把大缸的配方做成一个独立的「大缸」分类。
 *
 * JEI 是可选的，这个类只有在装了 JEI 的客户端才会被加载（服务端不会碰它），
 * 所以放在公共源码集里也不会影响服务端启动。
 */
@JeiPlugin
public class DongbeiJeiPlugin implements IModPlugin {

    public static final RecipeType<VatJeiRecipe> VAT_TYPE =
            RecipeType.create(DongbeiDelight.MODID, "vat", VatJeiRecipe.class);

    private static final ResourceLocation PLUGIN_UID =
            ResourceLocation.fromNamespaceAndPath(DongbeiDelight.MODID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new VatRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(VAT_TYPE, VatJeiRecipes.all());
    }

    /** 拿着大缸按 R / 双击就能看到这些配方 */
    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(ModItems.VAT.get(), VAT_TYPE);
    }
}
