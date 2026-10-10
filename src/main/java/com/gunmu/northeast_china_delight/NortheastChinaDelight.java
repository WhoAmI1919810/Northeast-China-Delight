package com.gunmu.northeast_china_delight;

import com.gunmu.northeast_china_delight.block.GrillCampfireBlock;
import com.gunmu.northeast_china_delight.block.ModBlockEntities;
import com.gunmu.northeast_china_delight.block.ModBlocks;
import com.gunmu.northeast_china_delight.block.Vat;
import com.gunmu.northeast_china_delight.block.VatBlockEntity;
import com.gunmu.northeast_china_delight.client.GrillCampfireRenderer;
import com.gunmu.northeast_china_delight.client.VatRenderer;
import com.gunmu.northeast_china_delight.compat.create.ModFanProcessingTypes;
import com.gunmu.northeast_china_delight.effect.DishEffectEvents;
import com.gunmu.northeast_china_delight.effect.ModEffects;
import com.gunmu.northeast_china_delight.event.ModGameplayEvents;
import com.gunmu.northeast_china_delight.fluid.ModFluids;
import com.gunmu.northeast_china_delight.item.ModItems;
import com.gunmu.northeast_china_delight.loot.ModLootModifiers;
import com.gunmu.northeast_china_delight.util.DdIds;
import com.gunmu.northeast_china_delight.villager.ModButcherTrades;
import com.gunmu.northeast_china_delight.villager.ModCartographerTrades;
import com.gunmu.northeast_china_delight.villager.ModFarmerTrades;
import com.gunmu.northeast_china_delight.villager.ModFishermanTrades;
import com.gunmu.northeast_china_delight.villager.ModVillagerProfessions;
import com.gunmu.northeast_china_delight.villager.ModWandererTrades;
import com.gunmu.northeast_china_delight.villager.SideDishMerchantTrades;
import com.gunmu.northeast_china_delight.worldgen.ModWorldGen;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
//? if <1.20.2 {
/*import net.minecraftforge.api.distmarker.Dist;
*///?} else {
import net.neoforged.api.distmarker.Dist;
//?}
//? if <1.20.2 {
/*import net.minecraftforge.eventbus.api.IEventBus;
*///?} else {
import net.neoforged.bus.api.IEventBus;
//?}
//? if <1.20.2 {
/*import net.minecraftforge.eventbus.api.SubscribeEvent;
*///?} else {
import net.neoforged.bus.api.SubscribeEvent;
//?}
//? if <1.20.2 {
/*import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
*///?} else {
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
//?}
//? if <1.20.2 {
/*import net.minecraftforge.fml.ModContainer;
*///?} else {
import net.neoforged.fml.ModContainer;
//?}
//? if <1.20.2 {
/*import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
*///?} else if <1.20.5 {
/*import net.neoforged.fml.common.Mod.EventBusSubscriber;*/
//?} else {
import net.neoforged.fml.common.EventBusSubscriber;
//?}
//? if <1.20.2 {
/*import net.minecraftforge.fml.common.Mod;
*///?} else {
import net.neoforged.fml.common.Mod;
//?}
//? if <1.20.2 {
/*import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
*///?} else {
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
//?}
//? if <1.20.2 {
/*import net.minecraftforge.common.MinecraftForge;
*///?} else {
import net.neoforged.neoforge.common.NeoForge;
//?}
//? if <1.20.2 {
/*import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
*///?} else {
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
//?}
//? if <1.20.2 {
/*import net.minecraftforge.event.entity.player.PlayerInteractEvent;
*///?} else {
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
//?}
//? if <1.20.2 {
/*import net.minecraftforge.event.server.ServerStartingEvent;
*///?} else {
import net.neoforged.neoforge.event.server.ServerStartingEvent;
//?}
//? if <1.20.2 {
/*import net.minecraftforge.client.event.RegisterColorHandlersEvent;
*///?} else {
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
//?}
//? if <1.20.2 {
/*import net.minecraftforge.client.event.EntityRenderersEvent;
*///?} else {
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
//?}
//? if <1.20.2 {
/*import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
*///?} else {
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
//?}
//? if >=1.20.5 {
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
//?}
//? if <1.20.2 {
/*import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
*///?} else {
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
//?}
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

// modid 必须与 neoforge.mods.toml 中的条目保持一致
@Mod(NortheastChinaDelight.MODID)
public class NortheastChinaDelight
{
    public static final String MODID = "northeast_china_delight";
    private static final Logger LOGGER = LogUtils.getLogger();

    //? if <1.20.2 {
    /*public NortheastChinaDelight()
    {
        IEventBus modEventBus = net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus();
    *///?} else {
    public NortheastChinaDelight(IEventBus modEventBus, ModContainer modContainer)
    {
    //?}
        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModFluids.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        ModLootModifiers.register(modEventBus);
        ModEffects.register(modEventBus);
        ModFanProcessingTypes.register(modEventBus);
        ModVillagerProfessions.register(modEventBus);
        ModMapDecorations.register(modEventBus);
        ModWorldGen.register(modEventBus);
        // 模组配置（菜肴方块形态的总开关）
        //? if <1.20.2 {
        /*net.minecraftforge.fml.ModLoadingContext.get().registerConfig(
                net.minecraftforge.fml.config.ModConfig.Type.COMMON, NortheastChinaConfig.SPEC);
        *///?} else if <1.20.5 {
        /*net.neoforged.fml.ModLoadingContext.get().registerConfig(
                net.neoforged.fml.config.ModConfig.Type.COMMON, NortheastChinaConfig.SPEC);*/
        //?} else {
        modContainer.registerConfig(net.neoforged.fml.config.ModConfig.Type.COMMON, NortheastChinaConfig.SPEC);
        //?}

        modEventBus.addListener(this::addItemsToCreativeTab);
        modEventBus.addListener(this::registerCapabilities);
        modEventBus.addListener(this::onCommonSetup);

        // 注册服务器及其他游戏事件
        //? if <1.20.2 {
        /*MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(DishEffectEvents.class);
        // 木耳（给原木去皮）与人参（空手刨土）的钩子
        MinecraftForge.EVENT_BUS.register(ModGameplayEvents.class);
        // 临时调试：进度授予 / 方块放置 / 大缸状态
        MinecraftForge.EVENT_BUS.register(com.gunmu.northeast_china_delight.event.ModDebugEvents.class);
        // 副食商的交易表（每次数据包重载时重新填一遍）
        MinecraftForge.EVENT_BUS.addListener(SideDishMerchantTrades::addTrades);
        // 原版各职业的交易表：农民 / 屠夫 / 渔夫 / 制图师 / 流浪商人
        MinecraftForge.EVENT_BUS.addListener(ModFarmerTrades::addTrades);
        MinecraftForge.EVENT_BUS.addListener(ModButcherTrades::addTrades);
        MinecraftForge.EVENT_BUS.addListener(ModFishermanTrades::addTrades);
        MinecraftForge.EVENT_BUS.addListener(ModCartographerTrades::addTrades);
        MinecraftForge.EVENT_BUS.addListener(ModWandererTrades::addTrades);
        *///?} else {
        NeoForge.EVENT_BUS.register(this);
        NeoForge.EVENT_BUS.register(DishEffectEvents.class);
        NeoForge.EVENT_BUS.register(ModGameplayEvents.class);
        NeoForge.EVENT_BUS.register(com.gunmu.northeast_china_delight.event.ModDebugEvents.class);
        NeoForge.EVENT_BUS.addListener(SideDishMerchantTrades::addTrades);
        NeoForge.EVENT_BUS.addListener(ModFarmerTrades::addTrades);
        NeoForge.EVENT_BUS.addListener(ModButcherTrades::addTrades);
        NeoForge.EVENT_BUS.addListener(ModFishermanTrades::addTrades);
        NeoForge.EVENT_BUS.addListener(ModCartographerTrades::addTrades);
        NeoForge.EVENT_BUS.addListener(ModWandererTrades::addTrades);
        //?}
    }

    private void onCommonSetup(@NotNull FMLCommonSetupEvent event)
    {
        event.enqueueWork(() -> {
            net.minecraft.world.level.block.ComposterBlock.COMPOSTABLES.put(
                    ModItems.CORN_STALK.get(), 0.65F);
        });
    }

    private void addItemsToCreativeTab(@NotNull BuildCreativeModeTabContentsEvent event)
    {
        if (event.getTabKey().equals(ModCreativeTabs.INGREDIENTS_TAB_KEY))
        {
            ModItems.INGREDIENT_TAB_ITEMS.forEach(item -> event.accept(item.get()));
        }
        else if (event.getTabKey().equals(ModCreativeTabs.DISHES_TAB_KEY))
        {
            ModItems.DISH_TAB_ITEMS.forEach(item -> event.accept(item.get()));
        }
        else if (event.getTabKey().equals(ModCreativeTabs.BLOCKS_TAB_KEY))
        {
            ModItems.BLOCK_TAB_ITEMS.forEach(item -> event.accept(item.get()));
        }
    }

    private void registerCapabilities(@NotNull RegisterCapabilitiesEvent event)
    {
        //? if <1.20.2 {
        /*// 1.20.1（Forge）的流体能力在 VatBlockEntity#getCapability 里暴露，这里不用注册
        *///?} else {
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ModBlockEntities.VAT.get(),
                (vat, side) -> vat);
        //?}
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
        LOGGER.info("da dong bei shi wo di jia xiang ~");
    }

    @SubscribeEvent
    public void onRightClickCampfire(PlayerInteractEvent.RightClickBlock event)
    {
        if (event.getHand() != InteractionHand.MAIN_HAND || event.isCanceled()
                || !event.getItemStack().is(ModItems.GRILL_RACK.get()))
        {
            return;
        }
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        if (!(state.is(Blocks.CAMPFIRE) || state.is(Blocks.SOUL_CAMPFIRE))
                || !(level.getBlockEntity(pos) instanceof CampfireBlockEntity fire))
        {
            return;
        }
        // 客户端先拦下来，避免本地预测跑一遍原版营火的交互
        event.setCanceled(true);
        // 必须给一个「已消费」的结果：默认的 PASS 会让客户端接着去处理副手，
        // 副手那次「空手右键」打在刚装好的烤架营火上，会把烤架又撸下来
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (level.isClientSide)
        {
            return;
        }
        GrillCampfireBlock.install(level, pos, state, fire);
        if (!event.getEntity().getAbilities().instabuild)
        {
            event.getItemStack().shrink(1);
        }
        level.playSound(null, pos, SoundEvents.LANTERN_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    @EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents
    {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)
        {
            LOGGER.info("da dong bei shi wo di jia xiang ~");
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
        }

        @SubscribeEvent
        public static void onRegisterBlockColors(RegisterColorHandlersEvent.Block event)
        {
            event.register(
                    (state, level, pos, tintIndex) -> {
                        if (level != null && pos != null
                                && level.getBlockEntity(pos) instanceof VatBlockEntity vat
                                && vat.sourWaterMb() > 0) {
                            return 0xD9D2AE;
                        }
                        int color = level != null && pos != null
                                ? BiomeColors.getAverageWaterColor(level, pos)
                                : 0x3F76E4;
                        if (state.getValue(Vat.SALTED)) {
                            int r = (color >> 16) & 0xFF;
                            int g = (color >> 8) & 0xFF;
                            int b = color & 0xFF;
                            r += (int) ((255 - r) * 0.45F);
                            g += (int) ((255 - g) * 0.45F);
                            b += (int) ((255 - b) * 0.45F);
                            color = (r << 16) | (g << 8) | b;
                        }
                        if (level != null && pos != null
                                && level.getBlockEntity(pos) instanceof VatBlockEntity vat) {
                            float weight = 1.0F;
                            for (ItemStack content : vat.contents()) {
                                int tint = com.gunmu.northeast_china_delight.client.VatItemShapes.liquidTint(content);
                                if (tint == 0xFFFFFF) {
                                    continue;
                                }
                                float strength = com.gunmu.northeast_china_delight.client.VatItemShapes
                                        .liquidStrength(content) * weight;
                                color = blend(color, tint, strength);
                                weight *= 0.55F;
                            }
                        }
                        return color;
                    },
                    ModBlocks.VAT.get());
        }

        private static int blend(int base, int tint, float amount)
        {
            float a = Math.max(0.0F, Math.min(1.0F, amount));
            int br = (base >> 16) & 0xFF;
            int bg = (base >> 8) & 0xFF;
            int bb = base & 0xFF;
            int tr = (tint >> 16) & 0xFF;
            int tg = (tint >> 8) & 0xFF;
            int tb = tint & 0xFF;
            int r = (int) (br + (tr - br) * a);
            int g = (int) (bg + (tg - bg) * a);
            int b = (int) (bb + (tb - bb) * a);
            return (r << 16) | (g << 8) | b;
        }

        @SubscribeEvent
        public static void onRegisterItemColors(RegisterColorHandlersEvent.Item event)
        {
            event.register(
                    (stack, tintIndex) -> tintIndex == 0
                            ? com.gunmu.northeast_china_delight.item.BottleColors.of(stack.getItem())
                            : -1,
                    com.gunmu.northeast_china_delight.item.BottleColors.bottles());
        }

        @SubscribeEvent
        public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event)
        {
            event.registerBlockEntityRenderer(ModBlockEntities.VAT.get(), VatRenderer::new);
            event.registerBlockEntityRenderer(ModBlockEntities.GRILL_CAMPFIRE.get(), GrillCampfireRenderer::new);
        }

        /**
         * 酱油、大酱在流体储罐 / 管道里的贴图。
         * 没有这一段的话，机械动力的储罐会显示成紫黑格。
         *
         * <p>1.20.1 没有这个事件 —— 那边的贴图挂在 {@code FluidType#initializeClient} 上，
         * 见 {@code ModFluids.DdFluidType}。</p>
         */
        //? if >=1.20.5 {
        @SubscribeEvent
        public static void onRegisterClientExtensions(RegisterClientExtensionsEvent event)
        {
            event.registerFluidType(new IClientFluidTypeExtensions()
            {
                @Override
                public ResourceLocation getStillTexture()
                {
                    return DdIds.of(MODID, "block/soy_sauce_still");
                }

                @Override
                public ResourceLocation getFlowingTexture()
                {
                    return DdIds.of(MODID, "block/soy_sauce_flow");
                }
            }, ModFluids.SOY_SAUCE_TYPE);

            event.registerFluidType(new IClientFluidTypeExtensions()
            {
                @Override
                public ResourceLocation getStillTexture()
                {
                    return DdIds.of(MODID, "block/soy_paste_still");
                }

                @Override
                public ResourceLocation getFlowingTexture()
                {
                    return DdIds.of(MODID, "block/soy_paste_still");
                }

                @Override
                public int getTintColor()
                {
                    return 0xFF9A672E;
                }
            }, ModFluids.SOY_PASTE_TYPE);

            event.registerFluidType(new IClientFluidTypeExtensions()
            {
                @Override
                public ResourceLocation getStillTexture()
                {
                    return DdIds.of(MODID, "block/soy_milk_still");
                }

                @Override
                public ResourceLocation getFlowingTexture()
                {
                    return DdIds.of(MODID, "block/soy_milk_flow");
                }
            }, ModFluids.SOY_MILK_TYPE);

            event.registerFluidType(new IClientFluidTypeExtensions()
            {
                @Override
                public ResourceLocation getStillTexture()
                {
                    return DdIds.of(MODID, "block/vinegar_still");
                }

                @Override
                public ResourceLocation getFlowingTexture()
                {
                    return DdIds.of(MODID, "block/vinegar_flow");
                }
            }, ModFluids.VINEGAR_TYPE);

            event.registerFluidType(new IClientFluidTypeExtensions()
            {
                @Override
                public ResourceLocation getStillTexture()
                {
                    return DdIds.of(MODID, "block/sour_water_still");
                }

                @Override
                public ResourceLocation getFlowingTexture()
                {
                    return DdIds.of(MODID, "block/sour_water_flow");
                }
            }, ModFluids.SOUR_WATER_TYPE);

            event.registerFluidType(new IClientFluidTypeExtensions()
            {
                @Override
                public ResourceLocation getStillTexture()
                {
                    return DdIds.of(MODID, "block/white_vinegar_still");
                }

                @Override
                public ResourceLocation getFlowingTexture()
                {
                    return DdIds.of(MODID, "block/white_vinegar_flow");
                }
            }, ModFluids.WHITE_VINEGAR_TYPE);

            event.registerFluidType(new IClientFluidTypeExtensions()
            {
                @Override
                public ResourceLocation getStillTexture()
                {
                    return DdIds.of(MODID, "block/fish_sauce_still");
                }

                @Override
                public ResourceLocation getFlowingTexture()
                {
                    return DdIds.of(MODID, "block/fish_sauce_flow");
                }
            }, ModFluids.FISH_SAUCE_TYPE);

            event.registerFluidType(new IClientFluidTypeExtensions()
            {
                @Override
                public ResourceLocation getStillTexture()
                {
                    return DdIds.of(MODID, "block/shrimp_paste_still");
                }

                @Override
                public ResourceLocation getFlowingTexture()
                {
                    return DdIds.of(MODID, "block/shrimp_paste_still");
                }

                @Override
                public int getTintColor()
                {
                    return 0xFF8A4A34;
                }
            }, ModFluids.SHRIMP_PASTE_TYPE);

            event.registerFluidType(new IClientFluidTypeExtensions()
            {
                @Override
                public ResourceLocation getStillTexture()
                {
                    return DdIds.of(MODID, "block/vegetable_oil_still");
                }

                @Override
                public ResourceLocation getFlowingTexture()
                {
                    return DdIds.of(MODID, "block/vegetable_oil_flow");
                }
            }, ModFluids.VEGETABLE_OIL_TYPE);

            event.registerFluidType(new IClientFluidTypeExtensions()
            {
                @Override
                public ResourceLocation getStillTexture()
                {
                    return DdIds.of(MODID, "block/animal_oil_still");
                }

                @Override
                public ResourceLocation getFlowingTexture()
                {
                    return DdIds.of(MODID, "block/animal_oil_flow");
                }
            }, ModFluids.ANIMAL_OIL_TYPE);

            event.registerFluidType(new IClientFluidTypeExtensions()
            {
                @Override
                public ResourceLocation getStillTexture()
                {
                    return DdIds.of(MODID, "block/peanut_butter_still");
                }

                @Override
                public ResourceLocation getFlowingTexture()
                {
                    return DdIds.of(MODID, "block/peanut_butter_still");
                }

                @Override
                public int getTintColor()
                {
                    return 0xFFC9A05A;
                }
            }, ModFluids.PEANUT_BUTTER_TYPE);

            event.registerFluidType(new IClientFluidTypeExtensions()
            {
                @Override
                public ResourceLocation getStillTexture()
                {
                    return DdIds.of(MODID, "block/chili_oil_still");
                }

                @Override
                public ResourceLocation getFlowingTexture()
                {
                    return DdIds.of(MODID, "block/chili_oil_flow");
                }
            }, ModFluids.CHILI_OIL_TYPE);

            event.registerFluidType(new IClientFluidTypeExtensions()
            {
                @Override
                public ResourceLocation getStillTexture()
                {
                    return DdIds.of(MODID, "block/chili_sauce_still");
                }

                @Override
                public ResourceLocation getFlowingTexture()
                {
                    return DdIds.of(MODID, "block/chili_sauce_still");
                }

                @Override
                public int getTintColor()
                {
                    return 0xFFE04A1F;
                }
            }, ModFluids.CHILI_SAUCE_TYPE);

            event.registerFluidType(new IClientFluidTypeExtensions()
            {
                @Override
                public ResourceLocation getStillTexture()
                {
                    return DdIds.of(MODID, "block/kvass_still");
                }

                @Override
                public ResourceLocation getFlowingTexture()
                {
                    return DdIds.of(MODID, "block/kvass_flow");
                }
            }, ModFluids.KVASS_TYPE);
        }
        //?}
    }
}
