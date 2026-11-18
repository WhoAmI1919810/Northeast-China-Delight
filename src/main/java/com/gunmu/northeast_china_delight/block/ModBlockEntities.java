package com.gunmu.northeast_china_delight.block;

import com.gunmu.northeast_china_delight.NortheastChinaDelight;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
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

public class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, NortheastChinaDelight.MODID);

    public static final Supplier<BlockEntityType<FoldedQuiltBlockEntity>> FOLDED_QUILT = BLOCK_ENTITIES.register(
            "folded_quilt",
            () -> BlockEntityType.Builder.of(FoldedQuiltBlockEntity::new, ModBlocks.FOLDED_QUILT.get()).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
