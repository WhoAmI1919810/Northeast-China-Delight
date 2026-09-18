package com.gunmu.dongbei_delight;

import com.gunmu.dongbei_delight.block.ModBlocks;
import com.gunmu.dongbei_delight.item.ModItems;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Item;
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
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.List;
import java.util.function.Supplier;

// modid 必须与 neoforge.mods.toml 中的条目保持一致
@Mod(DongbeiDelight.MODID)
public class DongbeiDelight
{
    public static final String MODID = "dongbei_delight";
    private static final Logger LOGGER = LogUtils.getLogger();

    // 注册到本模组创造模式物品栏中的物品
    private static final List<Supplier<Item>> MATERIALS = List.of(
            ModItems.SOUR_CABBAGE,
            ModItems.SOUL_CABBAGE,
            ModItems.UNFIRED_VAT_BLANK,
            ModItems.VAT
    );

    public DongbeiDelight(IEventBus modEventBus, ModContainer modContainer)
    {
        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus);
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
            MATERIALS.forEach(item -> event.accept(item.get()));
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
    }
}
