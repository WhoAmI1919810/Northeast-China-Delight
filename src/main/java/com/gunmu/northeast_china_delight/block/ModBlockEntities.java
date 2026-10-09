package com.gunmu.northeast_china_delight.block;

import com.gunmu.northeast_china_delight.NortheastChinaDelight;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
//? if <1.20.2 {
import net.minecraftforge.eventbus.api.IEventBus;
//?} else {
/*import net.neoforged.bus.api.IEventBus;
*///?}
//? if <1.20.2 {
import net.minecraftforge.registries.DeferredRegister;
//?} else {
/*import net.neoforged.neoforge.registries.DeferredRegister;
*///?}

import java.util.function.Supplier;

public class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, NortheastChinaDelight.MODID);

    public static final Supplier<BlockEntityType<VatBlockEntity>> VAT = BLOCK_ENTITIES.register(
            "vat",
            () -> BlockEntityType.Builder.of(VatBlockEntity::new, ModBlocks.VAT.get(), ModBlocks.CREATIVE_VAT.get()).build(null));

    /** 烤架营火：必须有自己的类型，不能借用原版营火的那个（类型会校验合法方块） */
    public static final Supplier<BlockEntityType<GrillBlockEntity>> GRILL_CAMPFIRE = BLOCK_ENTITIES.register(
            "grill_campfire",
            () -> BlockEntityType.Builder.of(GrillBlockEntity::new, ModBlocks.GRILL_CAMPFIRE.get()).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
