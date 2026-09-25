package com.gunmu.dongbei_delight.compat.jei;

import com.gunmu.dongbei_delight.DongbeiDelight;
import com.gunmu.dongbei_delight.crafting.VatRecipes;
import com.gunmu.dongbei_delight.item.ModItems;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.runtime.IJeiRuntime;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

/**
 * JEI 联动：把大缸的配方做成一个独立的「大缸」分类。
 *
 * JEI 是可选的，这个类只有在装了 JEI 的客户端才会被加载（服务端不会碰它），
 * 所以放在公共源码集里也不会影响服务端启动。
 */
@JeiPlugin
public class DongbeiJeiPlugin implements IModPlugin {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static final RecipeType<VatJeiRecipe> VAT_TYPE =
            RecipeType.create(DongbeiDelight.MODID, "vat", VatJeiRecipe.class);

    public static final RecipeType<GrillJeiRecipe> GRILL_TYPE =
            RecipeType.create(DongbeiDelight.MODID, "grill", GrillJeiRecipe.class);

    private static final ResourceLocation PLUGIN_UID =
            ResourceLocation.fromNamespaceAndPath(DongbeiDelight.MODID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper guiHelper = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(new VatRecipeCategory(guiHelper));
        registration.addRecipeCategories(new GrillRecipeCategory(guiHelper));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(VAT_TYPE, VatJeiRecipes.all());
        registration.addRecipes(GRILL_TYPE, GrillJeiRecipes.all());
    }

    /**
     * 配方页全部由 {@code VatRecipes.REGISTRY} 现算，算一次就在这里报个数：
     * 哪天一页都出不来，日志里能立刻看出来是哪一步挂了。
     */
    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        LOGGER.info("大缸 JEI 配方：{} 页，源配方 {} 条",
                VatJeiRecipes.all().size(), VatRecipes.all().size());
    }

    /** 拿着大缸 / 烧烤架按 R / 双击就能看到对应的配方 */
    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(ModItems.VAT.get(), VAT_TYPE);
        registration.addRecipeCatalyst(ModItems.GRILL_RACK.get(), GRILL_TYPE);
    }
}
