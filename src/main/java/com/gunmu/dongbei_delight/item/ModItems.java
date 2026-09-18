package com.gunmu.dongbei_delight.item;

import com.gunmu.dongbei_delight.block.ModBlocks;
import com.gunmu.dongbei_delight.DongbeiDelight;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.minecraft.world.item.BlockItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;


public class ModItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(DongbeiDelight.MODID);

    public static final DeferredItem<Item> SOUR_CABBAGE = ITEMS.registerItem(
            "sour_cabbage",
            Item::new,
            new Item.Properties()
    );

    public static final DeferredItem<Item> SOUL_CABBAGE = ITEMS.registerItem(
            "soul_cabbage",
            Item::new,
            new Item.Properties()
    );

    public static final DeferredItem<Item> UNFIRED_VAT_BLANK = ITEMS.registerItem(
            "unfired_vat_blank",
            Item::new,
            new Item.Properties().stacksTo(1)
    );

    public static final DeferredItem<Item> VAT = ITEMS.registerItem(
            "vat",
            (properties) -> new BlockItem(ModBlocks.VAT.get(), properties),
            new Item.Properties().stacksTo(1)
    );

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}

