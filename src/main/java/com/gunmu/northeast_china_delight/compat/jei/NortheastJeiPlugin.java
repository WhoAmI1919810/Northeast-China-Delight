package com.gunmu.northeast_china_delight.compat.jei;

import com.gunmu.northeast_china_delight.crafting.VatRecipes;
import com.gunmu.northeast_china_delight.item.ModItems;
import com.gunmu.northeast_china_delight.NortheastChinaDelight;
import com.gunmu.northeast_china_delight.util.DdIds;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.runtime.IJeiRuntime;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

/**
 * JEI 联动：把大缸的配方做成一个独立的「大缸」分类。
 *
 * JEI 是可选的，这个类只有在装了 JEI 的客户端才会被加载（服务端不会碰它），
 * 所以放在公共源码集里也不会影响服务端启动。
 */
@JeiPlugin
public class NortheastJeiPlugin implements IModPlugin {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static final RecipeType<VatJeiRecipe> VAT_TYPE =
            RecipeType.create(NortheastChinaDelight.MODID, "vat", VatJeiRecipe.class);

    public static final RecipeType<GrillJeiRecipe> GRILL_TYPE =
            RecipeType.create(NortheastChinaDelight.MODID, "grill", GrillJeiRecipe.class);

    //? if >=1.20.5 {
    /** 批量风干是机械动力的鼓风机行为，1.20.4 节点没有机械动力，这个分类整块不注册 */
    public static final RecipeType<DryingJeiRecipe> DRYING_TYPE =
            RecipeType.create(NortheastChinaDelight.MODID, "drying", DryingJeiRecipe.class);
    //?}

    private static final ResourceLocation PLUGIN_UID =
            DdIds.of(NortheastChinaDelight.MODID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper guiHelper = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(new VatRecipeCategory(guiHelper));
        registration.addRecipeCategories(new GrillRecipeCategory(guiHelper));
        //? if >=1.20.5 {
        registration.addRecipeCategories(new DryingRecipeCategory(guiHelper));
        //?}
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(VAT_TYPE, VatJeiRecipes.all());
        registration.addRecipes(GRILL_TYPE, GrillJeiRecipes.all());
        //? if >=1.20.5 {
        registration.addRecipes(DRYING_TYPE, DryingJeiRecipes.all());
        //?}
        registration.addItemStackInfo(new ItemStack(ModItems.GINSENG.get()),
                Component.translatable("jei.northeast_china_delight.info.ginseng"));
        registration.addItemStackInfo(new ItemStack(ModItems.WOOD_EAR.get()),
                Component.translatable("jei.northeast_china_delight.info.wood_ear"));
        registration.addItemStackInfo(new ItemStack(ModItems.HAZEL_MUSHROOM.get()),
                Component.translatable("jei.northeast_china_delight.info.hazel_mushroom"));
        registration.addItemStackInfo(new ItemStack(ModItems.HAIRTAIL.get()),
                Component.translatable("jei.northeast_china_delight.info.hairtail"));
        registration.addItemStackInfo(new ItemStack(ModItems.OYSTER.get()),
                Component.translatable("jei.northeast_china_delight.info.oyster"));
        registration.addItemStackInfo(new ItemStack(ModItems.SEA_CUCUMBER.get()),
                Component.translatable("jei.northeast_china_delight.info.sea_cucumber"));
        registration.addItemStackInfo(new ItemStack(ModItems.SALT.get()),
                Component.translatable("jei.northeast_china_delight.info.salt"));
        registration.addItemStackInfo(new ItemStack(ModItems.SEA_WATER_BUCKET.get()),
                Component.translatable("jei.northeast_china_delight.info.sea_water_bucket"));
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
        // 1.20.4 的 JEI（17.x）只收 ItemStack，1.21 收 ItemLike —— 用 ItemStack 两边都能过
        registration.addRecipeCatalyst(new ItemStack(ModItems.VAT.get()), VAT_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModItems.GRILL_RACK.get()), GRILL_TYPE);
        //? if >=1.20.5 {
        registration.addRecipeCatalyst(new ItemStack(
                net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(
                        DdIds.of("create", "encased_fan"))), DRYING_TYPE);
        //?}
    }
}
