package com.gunmu.dongbei_delight;

import com.gunmu.dongbei_delight.item.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModCreativeTabs {
    public static final ResourceKey<CreativeModeTab> DONGBEI_DELIGHT_TAB_KEY =
            ResourceKey.create(
                    Registries.CREATIVE_MODE_TAB,
                    ResourceLocation.fromNamespaceAndPath(DongbeiDelight.MODID, "dongbei_delight_tab")
            );

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, DongbeiDelight.MODID);

    public static final Supplier<CreativeModeTab> DONGBEI_DELIGHT_TAB =
            CREATIVE_MODE_TABS.register(DONGBEI_DELIGHT_TAB_KEY.location().getPath(),
                    () -> CreativeModeTab.builder()
                            .icon(() -> new ItemStack(ModItems.SOUR_CABBAGE.get()))
                            .title(Component.translatable("itemGroup.dongbei_delight"))
                            .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
