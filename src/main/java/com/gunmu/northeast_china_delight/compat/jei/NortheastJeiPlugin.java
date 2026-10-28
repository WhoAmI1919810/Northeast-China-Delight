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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

/**
 * JEI 联动：把大缸的配方做成一个独立的「大缸」分类。 JEI 是可选的，这个类只有在装了 JEI 的客户端才会被加载（服务端不会碰它），所以放在公共源码集里也不会影响服务端启
 * 动。
 */
@JeiPlugin
public class NortheastJeiPlugin implements IModPlugin {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static IJeiRuntime runtime;
    private static java.util.List<VatJeiRecipe> registeredVatRecipes = java.util.List.of();
    private static java.util.List<GrillJeiRecipe> registeredGrillRecipes = java.util.List.of();
    private static java.util.List<PoundJeiRecipe> registeredPoundRecipes = java.util.List.of();
    //? if >=1.20.5 {
    /*private static java.util.List<DryingJeiRecipe> registeredDryingRecipes = java.util.List.of();
    *///?}

    public static final RecipeType<VatJeiRecipe> VAT_TYPE =
            RecipeType.create(NortheastChinaDelight.MODID, "vat", VatJeiRecipe.class);

    public static final RecipeType<GrillJeiRecipe> GRILL_TYPE =
            RecipeType.create(NortheastChinaDelight.MODID, "grill", GrillJeiRecipe.class);

    public static final RecipeType<PoundJeiRecipe> POUND_TYPE =
            RecipeType.create(NortheastChinaDelight.MODID, "pounding", PoundJeiRecipe.class);
    //? if >=1.20.5 {
    /*/^* 批量风干是机械动力的鼓风机行为，1.20.4 节点没有机械动力，这个分类整块不注册 ^/
    public static final RecipeType<DryingJeiRecipe> DRYING_TYPE =
            RecipeType.create(NortheastChinaDelight.MODID, "drying", DryingJeiRecipe.class);
    *///?}

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
        registration.addRecipeCategories(new PoundRecipeCategory(guiHelper));
        //? if >=1.20.5 {
        /*registration.addRecipeCategories(new DryingRecipeCategory(guiHelper));
        *///?}
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        var connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            registeredVatRecipes = VatJeiRecipes.all(connection.getRecipeManager());
            registration.addRecipes(VAT_TYPE, registeredVatRecipes);
            registeredGrillRecipes = GrillJeiRecipes.all(connection.getRecipeManager());
            registeredPoundRecipes = PoundJeiRecipes.all(connection.getRecipeManager());
            registration.addRecipes(POUND_TYPE, registeredPoundRecipes);
            registration.addRecipes(GRILL_TYPE, registeredGrillRecipes);
            //? if >=1.20.5 {
            /*registeredDryingRecipes = DryingJeiRecipes.all(connection.getRecipeManager());
            registration.addRecipes(DRYING_TYPE, registeredDryingRecipes);
            *///?}
        }
        Component ginsengInfo = Component.translatable("jei.northeast_china_delight.info.ginseng");
        registration.addItemStackInfo(new ItemStack(ModItems.GINSENG.get()), ginsengInfo);
        registration.addItemStackInfo(new ItemStack(ModItems.GINSENG_SEEDS.get()), ginsengInfo);
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
        registration.addItemStackInfo(new ItemStack(ModItems.FROZEN_PEAR.get()), frozenPearInfo());
        Component bellflowerInfo = Component.translatable("jei.northeast_china_delight.info.bellflower");
        registration.addItemStackInfo(new ItemStack(ModItems.BELLFLOWER_ROOT.get()), bellflowerInfo);
        registration.addItemStackInfo(new ItemStack(ModItems.BELLFLOWER_SEEDS.get()), bellflowerInfo);
    }

    /**
     * 冻梨在 JEI 里的说明。它不像别的食材那样有一条稳定的自产途径：
     *
     * <ul>
     *   <li>主流来源是农民村民的职业交易（学徒级，4 个绿宝石换 1 个）；</li>
     *   <li>装了「果园乐事」（Fruits Delight）时，它的梨子会进通用标签
     *       {@code c:foods/fruits/pear}，而大缸的冻梨配方正好认这个标签，于是还能自己冻。</li>
     * </ul>
     *
     * <p>判断「装了没」直接查物品注册表，不走可选的编译期依赖、也不用 {@code ModList}：
     * 两版 Minecraft 上注册表的写法一样，1.20.1 那边不用加版本分支。</p>
     */
    private static Component[] frozenPearInfo()
    {
        Component sold = Component.translatable("jei.northeast_china_delight.info.frozen_pear");
        if (!BuiltInRegistries.ITEM.containsKey(DdIds.of("fruitsdelight", "pear"))) {
            return new Component[] { sold };
        }
        return new Component[] {
                sold,
                Component.translatable("jei.northeast_china_delight.info.frozen_pear.pear")
        };
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
        LOGGER.info("大缸 JEI 配方：{} 页，源配方 {} 条",
                registeredVatRecipes.size(), registeredVatRecipes.size());
        if (registeredVatRecipes.isEmpty() || registeredGrillRecipes.isEmpty()) {
            refreshRecipes(Minecraft.getInstance().getConnection() == null ? null
                    : Minecraft.getInstance().getConnection().getRecipeManager());
        }
    }

    @Override
    public void onRuntimeUnavailable() {
        runtime = null;
        registeredVatRecipes = java.util.List.of();
        registeredGrillRecipes = java.util.List.of();
        registeredPoundRecipes = java.util.List.of();
        //? if >=1.20.5 {
        /*registeredDryingRecipes = java.util.List.of();
        *///?}
    }

    public static void refreshRecipes(net.minecraft.world.item.crafting.RecipeManager manager) {
        if (runtime == null || manager == null) {
            return;
        }
        java.util.List<VatJeiRecipe> updated = VatJeiRecipes.all(manager);
        runtime.getRecipeManager().hideRecipes(VAT_TYPE, registeredVatRecipes);
        runtime.getRecipeManager().addRecipes(VAT_TYPE, updated);
        registeredVatRecipes = updated;
        java.util.List<GrillJeiRecipe> updatedGrill = GrillJeiRecipes.all(manager);
        runtime.getRecipeManager().hideRecipes(GRILL_TYPE, registeredGrillRecipes);
        runtime.getRecipeManager().addRecipes(GRILL_TYPE, updatedGrill);
        registeredGrillRecipes = updatedGrill;
        java.util.List<PoundJeiRecipe> updatedPound = PoundJeiRecipes.all(manager);
        runtime.getRecipeManager().hideRecipes(POUND_TYPE, registeredPoundRecipes);
        runtime.getRecipeManager().addRecipes(POUND_TYPE, updatedPound);
        registeredPoundRecipes = updatedPound;
        //? if >=1.20.5 {
        /*java.util.List<DryingJeiRecipe> updatedDrying = DryingJeiRecipes.all(manager);
        runtime.getRecipeManager().hideRecipes(DRYING_TYPE, registeredDryingRecipes);
        runtime.getRecipeManager().addRecipes(DRYING_TYPE, updatedDrying);
        registeredDryingRecipes = updatedDrying;
        *///?}
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(ModItems.VAT.get()), VAT_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModItems.GRILL_RACK.get()), GRILL_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModItems.WOODEN_MORTAR.get()), POUND_TYPE);
        //? if >=1.20.5 {
        /*registration.addRecipeCatalyst(new ItemStack(
                net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(
                        DdIds.of("create", "encased_fan"))), DRYING_TYPE);
        *///?}
    }
}
