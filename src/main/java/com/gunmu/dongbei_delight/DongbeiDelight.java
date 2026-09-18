package com.gunmu.dongbei_delight;

import com.gunmu.dongbei_delight.block.ModBlocks;
import com.gunmu.dongbei_delight.block.ModBlockEntities;
import com.gunmu.dongbei_delight.block.Vat;
import com.gunmu.dongbei_delight.block.VatBlockEntity;
import com.gunmu.dongbei_delight.client.VatRenderer;
import com.gunmu.dongbei_delight.item.ModItems;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BiomeColors;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
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
        ModBlockEntities.register(modEventBus);
        ModCreativeTabs.register(modEventBus);

        // 将物品添加到本模组的创造模式物品栏
        modEventBus.addListener(this::addItemsToCreativeTab);

        // 注册服务器及其他游戏事件
        NeoForge.EVENT_BUS.register(this);
    }

    private void addItemsToCreativeTab(@NotNull BuildCreativeModeTabContentsEvent event)
    {
        if (event.getTabKey().equals(ModCreativeTabs.DONGBEI_DELIGHT_TAB_KEY))
        {
            ModItems.CREATIVE_TAB_ITEMS.forEach(item -> event.accept(item.get()));
        }
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
    }
}
