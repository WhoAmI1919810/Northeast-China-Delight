package com.gunmu.dongbei_delight.block;

import com.gunmu.dongbei_delight.DongbeiDelight;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(DongbeiDelight.MODID);

    // registerBlock 会自动把注册名绑定为方块的 id（1.21.4 起方块必须带 id）
    public static final DeferredBlock<Block> VAT = BLOCKS.registerBlock(
            "vat",
            Vat::new,
            BlockBehaviour.Properties.of().strength(3.5f)
    );

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
