package com.gunmu.northeast_china_delight.compat.jei;

import com.gunmu.northeast_china_delight.crafting.VatRecipes;
import com.gunmu.northeast_china_delight.block.ModBlocks;
import com.gunmu.northeast_china_delight.item.ModItems;
import com.gunmu.northeast_china_delight.NortheastChinaDelight;
import com.gunmu.northeast_china_delight.compat.OptionalMods;
import com.gunmu.northeast_china_delight.util.DdIds;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
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
    private static java.util.List<BigPotJeiRecipe> registeredBigPotRecipes = java.util.List.of();
    //? if >=1.20.5 {
    /*private static java.util.List<DryingJeiRecipe> registeredDryingRecipes = java.util.List.of();
    *///?}

    public static final RecipeType<VatJeiRecipe> VAT_TYPE =
            RecipeType.create(NortheastChinaDelight.MODID, "vat", VatJeiRecipe.class);

    public static final RecipeType<GrillJeiRecipe> GRILL_TYPE =
            RecipeType.create(NortheastChinaDelight.MODID, "grill", GrillJeiRecipe.class);

    public static final RecipeType<PoundJeiRecipe> POUND_TYPE =
            RecipeType.create(NortheastChinaDelight.MODID, "pounding", PoundJeiRecipe.class);

    /** 大锅烹饪：大灶上的大铁锅 */
    public static final RecipeType<BigPotJeiRecipe> BIG_POT_TYPE =
            RecipeType.create(NortheastChinaDelight.MODID, "big_pot_cooking", BigPotJeiRecipe.class);
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
        registration.addRecipeCategories(new BigPotRecipeCategory(guiHelper));
        //? if >=1.20.5 {
        /*// 批量风干页画的是 Create 的鼓风机：机械动力是可选前置，没装就不注册这一页
        if (OptionalMods.create()) {
            registration.addRecipeCategories(new DryingRecipeCategory(guiHelper));
        }
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
            registeredBigPotRecipes = BigPotJeiRecipes.all(connection.getRecipeManager());
            registration.addRecipes(BIG_POT_TYPE, registeredBigPotRecipes);
            //? if >=1.20.5 {
            /*if (OptionalMods.create()) {
                registeredDryingRecipes = DryingJeiRecipes.all(connection.getRecipeManager());
                registration.addRecipes(DRYING_TYPE, registeredDryingRecipes);
            }
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
        // 棉门帘、大花布门帘是普通门帘在织布机里染出来的，不是合成配方
        registration.addItemStackInfo(java.util.List.of(new ItemStack(ModBlocks.PLAIN_DOOR_CURTAIN.get()),
                        new ItemStack(ModBlocks.COTTON_DOOR_CURTAIN.get()), new ItemStack(ModBlocks.FLORAL_DOOR_CURTAIN.get()),
                        new ItemStack(ModBlocks.ARMY_GREEN_DOOR_CURTAIN.get())),
                Component.translatable("jei.northeast_china_delight.info.door_curtains"));
        // 窗花不是合成出来的：一手红纸一手剪刀右键剪，贴在糊纸窗上
        registration.addItemStackInfo(new ItemStack(ModItems.PAPER_CUT.get()),
                Component.translatable("jei.northeast_china_delight.info.paper_cut"));
        // 大灶是摆出来的，不是合成的：大铁锅、农夫乐事炉灶和锅盖共用一页（2026-11-06 用户把锅盖也放进来）；
        // 锅盖另有自己一页（2026-11-05 用户拆开）
        registration.addItemStackInfo(java.util.List.of(new ItemStack(ModItems.BIG_IRON_POT.get()),
                        new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                                com.gunmu.northeast_china_delight.util.DdIds.of("farmersdelight", "stove"))),
                        new ItemStack(ModItems.BIG_POT_LID.get())),
                Component.translatable("jei.northeast_china_delight.info.big_stove"));
        registration.addItemStackInfo(new ItemStack(ModItems.BIG_POT_LID.get()),
                Component.translatable("jei.northeast_china_delight.info.big_pot_lid"));
        // 东北四件套（NortheastOutfit）单开一页，文案是用户原话（2026-11-06）
        registration.addItemStackInfo(java.util.List.of(new ItemStack(ModItems.LARGE_BASIN.get()),
                        new ItemStack(ModItems.FLORAL_JACKET.get()), new ItemStack(ModItems.WOODEN_MALLET.get()),
                        new ItemStack(ModItems.BIG_POT_LID.get())),
                Component.translatable("jei.northeast_china_delight.info.northeast_outfit"));
        // 压水井能给容器装水（2026-11-05 用户：脸盆架那页意义不明，换成压水井）
        registration.addItemStackInfo(new ItemStack(ModBlocks.HAND_PUMP.get()),
                Component.translatable("jei.northeast_china_delight.info.hand_pump"));
        // 谜之炖菜不是配方煮的：大锅里凑不上配方时盖上锅盖煮出来
        registration.addItemStackInfo(new ItemStack(ModItems.MYSTERY_STEW.get()),
                Component.translatable("jei.northeast_china_delight.info.mystery_stew"));
        registration.addItemStackInfo(new ItemStack(ModItems.SALT.get()),
                Component.translatable("jei.northeast_china_delight.info.salt"));
        // 手持小刀宰猪掉的副产物（PigOffalLootModifier），连着火时的熏制版一起，共用一页
        registration.addItemStackInfo(java.util.List.of(
                        new ItemStack(ModItems.PORK_RIBS.get()), new ItemStack(ModItems.PORK_INTESTINE.get()),
                        new ItemStack(ModItems.PORK_HOCK.get()), new ItemStack(ModItems.PIG_BLOOD.get()),
                        new ItemStack(ModItems.PIG_LIVER.get()), new ItemStack(ModItems.PORK_FAT.get()),
                        new ItemStack(ModItems.OIL_EDGE.get()),
                        new ItemStack(ModItems.SMOKED_PORK_HOCK.get()), new ItemStack(ModItems.SMOKED_PIG_LIVER.get())),
                Component.translatable("jei.northeast_china_delight.info.pig_offal"));
        // 冻梨、黄桃罐头共用一页（2026-11-05 用户）：平时只能买（冻梨农民卖，黄桃罐头副食商卖）；
        // 第二句告诉没装果园乐事的人，装了就能用它的梨、桃子自己做（大缸冻梨、厨锅黄桃罐头），所以不管装没装都显示
        registration.addItemStackInfo(java.util.List.of(new ItemStack(ModItems.FROZEN_PEAR.get()),
                        new ItemStack(ModItems.CANNED_YELLOW_PEACH.get())),
                Component.translatable("jei.northeast_china_delight.info.frozen_pear_canned_peach"),
                Component.translatable("jei.northeast_china_delight.info.frozen_pear_canned_peach.fruitsdelight"));
        Component bellflowerInfo = Component.translatable("jei.northeast_china_delight.info.bellflower");
        registration.addItemStackInfo(new ItemStack(ModItems.BELLFLOWER_ROOT.get()), bellflowerInfo);
        registration.addItemStackInfo(new ItemStack(ModItems.BELLFLOWER_SEEDS.get()), bellflowerInfo);
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
        // 海水桶只为老存档留着，没有获取方式，不在 JEI 里出现
        jeiRuntime.getIngredientManager().removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK,
                java.util.List.of(new ItemStack(ModItems.SEA_WATER_BUCKET.get())));
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
        registeredBigPotRecipes = java.util.List.of();
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
        java.util.List<BigPotJeiRecipe> updatedBigPot = BigPotJeiRecipes.all(manager);
        runtime.getRecipeManager().hideRecipes(BIG_POT_TYPE, registeredBigPotRecipes);
        runtime.getRecipeManager().addRecipes(BIG_POT_TYPE, updatedBigPot);
        registeredBigPotRecipes = updatedBigPot;
        //? if >=1.20.5 {
        /*if (OptionalMods.create()) {
            java.util.List<DryingJeiRecipe> updatedDrying = DryingJeiRecipes.all(manager);
            runtime.getRecipeManager().hideRecipes(DRYING_TYPE, registeredDryingRecipes);
            runtime.getRecipeManager().addRecipes(DRYING_TYPE, updatedDrying);
            registeredDryingRecipes = updatedDrying;
        }
        *///?}
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(ModItems.VAT.get()), VAT_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModItems.GRILL_RACK.get()), GRILL_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModItems.WOODEN_MORTAR.get()), POUND_TYPE);
        // 大锅烹饪：大铁锅，加上拼大灶用的农夫乐事炉灶
        registration.addRecipeCatalyst(new ItemStack(ModItems.BIG_IRON_POT.get()), BIG_POT_TYPE);
        registration.addRecipeCatalyst(new ItemStack(BuiltInRegistries.ITEM.get(DdIds.of("farmersdelight", "stove"))),
                BIG_POT_TYPE);
        //? if >=1.20.5 {
        /*if (OptionalMods.create()) {
            registration.addRecipeCatalyst(new ItemStack(
                    net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(
                            DdIds.of("create", "encased_fan"))), DRYING_TYPE);
        }
        *///?}
    }
}
