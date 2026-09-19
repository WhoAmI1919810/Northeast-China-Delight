package com.gunmu.dongbei_delight;

import com.gunmu.dongbei_delight.block.ModBlocks;
import com.gunmu.dongbei_delight.block.ModBlockEntities;
import com.gunmu.dongbei_delight.block.Vat;
import com.gunmu.dongbei_delight.block.VatBlockEntity;
import com.gunmu.dongbei_delight.client.VatRenderer;
import com.gunmu.dongbei_delight.effect.DishEffectEvents;
import com.gunmu.dongbei_delight.effect.ModEffects;
import com.gunmu.dongbei_delight.fluid.ModFluids;
import com.gunmu.dongbei_delight.item.ModItems;
import com.gunmu.dongbei_delight.loot.ModLootModifiers;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;


// modid 必须与 neoforge.mods.toml 中的条目保持一致
@Mod(DongbeiDelight.MODID)
public class DongbeiDelight
{
    public static final String MODID = "dongbei_delight";
    private static final Logger LOGGER = LogUtils.getLogger();

    public DongbeiDelight(IEventBus modEventBus, ModContainer modContainer)
    {
        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModFluids.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        ModLootModifiers.register(modEventBus);
        ModEffects.register(modEventBus);

        // 将物品添加到本模组的创造模式物品栏
        modEventBus.addListener(this::addItemsToCreativeTab);
        // 大缸的流体能力：让流体管道能把酱油 / 大酱抽进储罐
        modEventBus.addListener(this::registerCapabilities);

        // 注册服务器及其他游戏事件
        NeoForge.EVENT_BUS.register(this);
        NeoForge.EVENT_BUS.register(DishEffectEvents.class);
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
    }

    private void registerCapabilities(@NotNull RegisterCapabilitiesEvent event)
    {
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ModBlockEntities.VAT.get(),
                (vat, side) -> vat);
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
        LOGGER.info("da dong bei shi wo di jia xiang ~");
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

        /**
         * 大缸里的水面用原版 water_still 贴图，而那张贴图本身是灰白的，
         * 蓝色来自群系着色 —— 所以这里给水面（tintindex 0）注册水的颜色。
         */
        @SubscribeEvent
        public static void onRegisterBlockColors(RegisterColorHandlersEvent.Block event)
        {
            event.register(
                    (state, level, pos, tintIndex) -> {
                        // 酸引水：泡菜腌完后缸里的水，用发浑的淡黄色和其它液体区分开
                        if (level != null && pos != null
                                && level.getBlockEntity(pos) instanceof VatBlockEntity vat
                                && vat.sourWaterMb() > 0) {
                            return 0xD9D2AE;
                        }
                        int color = level != null && pos != null
                                ? BiomeColors.getAverageWaterColor(level, pos)
                                : 0x3F76E4;
                        // 盐够了的水稍微发白，用来提示「可以压石头开腌了」
                        if (state.getValue(Vat.SALTED)) {
                            int r = (color >> 16) & 0xFF;
                            int g = (color >> 8) & 0xFF;
                            int b = color & 0xFF;
                            r += (int) ((255 - r) * 0.45F);
                            g += (int) ((255 - g) * 0.45F);
                            b += (int) ((255 - b) * 0.45F);
                            color = (r << 16) | (g << 8) | b;
                        }
                        return color;
                    },
                    ModBlocks.VAT.get());
        }

        /** 大缸里的内容物需要渲染在方块上，所以注册方块实体渲染器 */
        @SubscribeEvent
        public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event)
        {
            event.registerBlockEntityRenderer(ModBlockEntities.VAT.get(), VatRenderer::new);
        }

        /**
         * 酱油、大酱在流体储罐 / 管道里的贴图。
         * 没有这一段的话，机械动力的储罐会显示成紫黑格。
         */
        @SubscribeEvent
        public static void onRegisterClientExtensions(RegisterClientExtensionsEvent event)
        {
            event.registerFluidType(new IClientFluidTypeExtensions()
            {
                @Override
                public ResourceLocation getStillTexture()
                {
                    return ResourceLocation.fromNamespaceAndPath(MODID, "block/soy_sauce_still");
                }

                @Override
                public ResourceLocation getFlowingTexture()
                {
                    return ResourceLocation.fromNamespaceAndPath(MODID, "block/soy_sauce_flow");
                }
            }, ModFluids.SOY_SAUCE_TYPE);

            event.registerFluidType(new IClientFluidTypeExtensions()
            {
                @Override
                public ResourceLocation getStillTexture()
                {
                    return ResourceLocation.fromNamespaceAndPath(MODID, "block/soy_paste_still");
                }

                @Override
                public ResourceLocation getFlowingTexture()
                {
                    return ResourceLocation.fromNamespaceAndPath(MODID, "block/soy_paste_flow");
                }
            }, ModFluids.SOY_PASTE_TYPE);

            event.registerFluidType(new IClientFluidTypeExtensions()
            {
                @Override
                public ResourceLocation getStillTexture()
                {
                    return ResourceLocation.fromNamespaceAndPath(MODID, "block/soy_milk_still");
                }

                @Override
                public ResourceLocation getFlowingTexture()
                {
                    return ResourceLocation.fromNamespaceAndPath(MODID, "block/soy_milk_flow");
                }
            }, ModFluids.SOY_MILK_TYPE);

            event.registerFluidType(new IClientFluidTypeExtensions()
            {
                @Override
                public ResourceLocation getStillTexture()
                {
                    return ResourceLocation.fromNamespaceAndPath(MODID, "block/vinegar_still");
                }

                @Override
                public ResourceLocation getFlowingTexture()
                {
                    return ResourceLocation.fromNamespaceAndPath(MODID, "block/vinegar_flow");
                }
            }, ModFluids.VINEGAR_TYPE);

            event.registerFluidType(new IClientFluidTypeExtensions()
            {
                @Override
                public ResourceLocation getStillTexture()
                {
                    return ResourceLocation.fromNamespaceAndPath(MODID, "block/sour_water_still");
                }

                @Override
                public ResourceLocation getFlowingTexture()
                {
                    return ResourceLocation.fromNamespaceAndPath(MODID, "block/sour_water_flow");
                }
            }, ModFluids.SOUR_WATER_TYPE);

            event.registerFluidType(new IClientFluidTypeExtensions()
            {
                @Override
                public ResourceLocation getStillTexture()
                {
                    return ResourceLocation.fromNamespaceAndPath(MODID, "block/white_vinegar_still");
                }

                @Override
                public ResourceLocation getFlowingTexture()
                {
                    return ResourceLocation.fromNamespaceAndPath(MODID, "block/white_vinegar_flow");
                }
            }, ModFluids.WHITE_VINEGAR_TYPE);

            event.registerFluidType(new IClientFluidTypeExtensions()
            {
                @Override
                public ResourceLocation getStillTexture()
                {
                    return ResourceLocation.fromNamespaceAndPath(MODID, "block/fish_sauce_still");
                }

                @Override
                public ResourceLocation getFlowingTexture()
                {
                    return ResourceLocation.fromNamespaceAndPath(MODID, "block/fish_sauce_flow");
                }
            }, ModFluids.FISH_SAUCE_TYPE);

            event.registerFluidType(new IClientFluidTypeExtensions()
            {
                @Override
                public ResourceLocation getStillTexture()
                {
                    return ResourceLocation.fromNamespaceAndPath(MODID, "block/shrimp_paste_still");
                }

                @Override
                public ResourceLocation getFlowingTexture()
                {
                    return ResourceLocation.fromNamespaceAndPath(MODID, "block/shrimp_paste_flow");
                }
            }, ModFluids.SHRIMP_PASTE_TYPE);

            event.registerFluidType(new IClientFluidTypeExtensions()
            {
                @Override
                public ResourceLocation getStillTexture()
                {
                    return ResourceLocation.fromNamespaceAndPath(MODID, "block/vegetable_oil_still");
                }

                @Override
                public ResourceLocation getFlowingTexture()
                {
                    return ResourceLocation.fromNamespaceAndPath(MODID, "block/vegetable_oil_flow");
                }
            }, ModFluids.VEGETABLE_OIL_TYPE);
        }
    }
}
