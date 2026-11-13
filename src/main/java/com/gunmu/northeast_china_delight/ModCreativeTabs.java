package com.gunmu.northeast_china_delight;

import com.gunmu.northeast_china_delight.item.ModItems;
import com.gunmu.northeast_china_delight.util.DdIds;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
//? if <1.20.2 {
/*import net.minecraftforge.eventbus.api.IEventBus;
*///?} else {
import net.neoforged.bus.api.IEventBus;
//?}
//? if <1.20.2 {
/*import net.minecraftforge.registries.DeferredRegister;
*///?} else {
import net.neoforged.neoforge.registries.DeferredRegister;
//?}

import java.util.function.Supplier;

/** 本模组的创造模式物品栏：整个地区模组共用一个。 */
public class ModCreativeTabs {

    public static final ResourceKey<CreativeModeTab> INGREDIENTS_TAB_KEY =
            tabKey("ingredients");

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, NortheastChinaDelight.MODID);

    public static final Supplier<CreativeModeTab> INGREDIENTS_TAB =
            CREATIVE_MODE_TABS.register(INGREDIENTS_TAB_KEY.location().getPath(),
                    () -> CreativeModeTab.builder()
                            .icon(() -> new ItemStack(ModItems.SOYBEAN.get()))
                            .title(Component.translatable("itemGroup.northeast_china_delight.ingredients"))
                            .build());

    private static ResourceKey<CreativeModeTab> tabKey(String path) {
        return ResourceKey.create(Registries.CREATIVE_MODE_TAB,
                DdIds.of(NortheastChinaDelight.MODID, path));
    }

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
