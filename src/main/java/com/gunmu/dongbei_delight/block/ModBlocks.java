package com.gunmu.dongbei_delight.block;

import com.gunmu.dongbei_delight.DongbeiDelight;
import com.gunmu.dongbei_delight.item.ModItems;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(DongbeiDelight.MODID);

    // ===== 作物 =====
    // 种子用延迟取值的方式传入，避免 ModItems / ModBlocks 之间的类初始化死循环

    public static final DeferredBlock<Block> SOYBEAN_CROP =
            crop("soybean", () -> ModItems.SOYBEAN.get());
    public static final DeferredBlock<Block> EGGPLANT_CROP =
            fruitCrop("eggplant", () -> ModItems.EGGPLANT_SEEDS.get(), () -> ModItems.EGGPLANT.get());
    public static final DeferredBlock<Block> GREEN_PEPPER_CROP =
            fruitCrop("green_pepper", () -> ModItems.GREEN_PEPPER_SEEDS.get(), () -> ModItems.GREEN_PEPPER.get());
    public static final DeferredBlock<Block> CORN_CROP = BLOCKS.registerBlock(
            "corn_crop",
            properties -> new CornCropBlock(properties, () -> ModItems.CORN_SEEDS.get()),
            cropProperties()
    );
    /** 玉米上方的茎秆，没有物品，由作物自己长出来 */
    public static final DeferredBlock<Block> CORN_STALK = BLOCKS.registerBlock(
            "corn_stalk",
            CornStalkBlock::new,
            plantProperties()
    );
    public static final DeferredBlock<Block> GREEN_BEANS_CROP =
            fruitCrop("green_beans", () -> ModItems.GREEN_BEANS_SEEDS.get(), () -> ModItems.GREEN_BEANS.get());
    public static final DeferredBlock<Block> BUCKWHEAT_CROP =
            crop("buckwheat", () -> ModItems.BUCKWHEAT.get());
    public static final DeferredBlock<Block> NAPA_CABBAGE_CROP =
            crop("napa_cabbage", () -> ModItems.NAPA_CABBAGE_SEEDS.get());
    public static final DeferredBlock<Block> CUCUMBER_CROP =
            fruitCrop("cucumber", () -> ModItems.CUCUMBER_SEEDS.get(), () -> ModItems.CUCUMBER.get());

    // ===== 功能方块 =====

    // registerBlock 会自动把注册名绑定为方块的 id（1.21.4 起方块必须带 id）
    public static final DeferredBlock<Block> VAT = BLOCKS.registerBlock(
            "vat",
            Vat::new,
            BlockBehaviour.Properties.of().strength(3.5f)
    );

    private static DeferredBlock<Block> crop(String name, Supplier<? extends ItemLike> seed) {
        return BLOCKS.registerBlock(name + "_crop", properties -> new DdCropBlock(properties, seed), cropProperties());
    }

    /** 果菜：成熟后可以用剪刀只摘果实，植株继续生长 */
    private static DeferredBlock<Block> fruitCrop(String name, Supplier<? extends ItemLike> seed,
                                                  Supplier<? extends ItemLike> fruit) {
        return BLOCKS.registerBlock(name + "_crop",
                properties -> new DdCropBlock(properties, seed, fruit), cropProperties());
    }

    /** 与原版作物一致的方块属性 */
    private static BlockBehaviour.Properties cropProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.PLANT)
                .noCollission()
                .randomTicks()
                .instabreak()
                .sound(SoundType.CROP)
                .pushReaction(PushReaction.DESTROY);
    }

    /** 不会随机刻的植物方块 */
    private static BlockBehaviour.Properties plantProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.PLANT)
                .noCollission()
                .instabreak()
                .sound(SoundType.CROP)
                .pushReaction(PushReaction.DESTROY);
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
