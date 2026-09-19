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

/**
 * 本模组的创造模式物品栏。
 *
 * 拆成两个：一个放食材与调料（农作物、种子、加工食材、腌制品、调味料、方块），
 * 另一个只放做好的菜肴。
 */
public class ModCreativeTabs {

    public static final ResourceKey<CreativeModeTab> INGREDIENTS_TAB_KEY =
            tabKey("ingredients");
    public static final ResourceKey<CreativeModeTab> DISHES_TAB_KEY =
            tabKey("dishes");

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, DongbeiDelight.MODID);

    /** 食材与调料 */
    public static final Supplier<CreativeModeTab> INGREDIENTS_TAB =
            CREATIVE_MODE_TABS.register(INGREDIENTS_TAB_KEY.location().getPath(),
                    () -> CreativeModeTab.builder()
                            .icon(() -> new ItemStack(ModItems.SOYBEAN.get()))
                            .title(Component.translatable("itemGroup.dongbei_delight.ingredients"))
                            .build());

    /** 菜肴 */
    public static final Supplier<CreativeModeTab> DISHES_TAB =
            CREATIVE_MODE_TABS.register(DISHES_TAB_KEY.location().getPath(),
                    () -> CreativeModeTab.builder()
                            .icon(() -> new ItemStack(ModItems.NEW_STYLE_GUO_BAO_ROU.get()))
                            .title(Component.translatable("itemGroup.dongbei_delight.dishes"))
                            .build());

    private static ResourceKey<CreativeModeTab> tabKey(String path) {
        return ResourceKey.create(Registries.CREATIVE_MODE_TAB,
                ResourceLocation.fromNamespaceAndPath(DongbeiDelight.MODID, path));
    }

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
