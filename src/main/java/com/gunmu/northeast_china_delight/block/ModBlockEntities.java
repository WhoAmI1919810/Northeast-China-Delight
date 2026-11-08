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

    public static final Supplier<BlockEntityType<VatBlockEntity>> VAT = BLOCK_ENTITIES.register(
            "vat",
            () -> BlockEntityType.Builder.of(VatBlockEntity::new, ModBlocks.VAT.get(), ModBlocks.CREATIVE_VAT.get()).build(null));

    /** 烤架营火：必须有自己的类型，不能借用原版营火的那个（类型会校验合法方块） */
    public static final Supplier<BlockEntityType<GrillBlockEntity>> GRILL_CAMPFIRE = BLOCK_ENTITIES.register(
            "grill_campfire",
            () -> BlockEntityType.Builder.of(GrillBlockEntity::new, ModBlocks.GRILL_CAMPFIRE.get()).build(null));

    public static final Supplier<BlockEntityType<WoodenMortarBlockEntity>> WOODEN_MORTAR = BLOCK_ENTITIES.register(
            "wooden_mortar",
            () -> BlockEntityType.Builder.of(WoodenMortarBlockEntity::new, ModBlocks.WOODEN_MORTAR.get()).build(null));

    /** 大铁锅里装的东西：只挂在大灶 nw 那一格上（见 BigStoveBlock#newBlockEntity） */
    public static final Supplier<BlockEntityType<BigPotBlockEntity>> BIG_POT = BLOCK_ENTITIES.register(
            "big_pot",
            () -> BlockEntityType.Builder.of(BigPotBlockEntity::new, ModBlocks.BIG_STOVE.get()).build(null));

    /** 大灶另外三格的方块实体：只在 1.20.1 上放，用来挂物品能力（见 BigStovePartBlockEntity） */
    public static final Supplier<BlockEntityType<BigStovePartBlockEntity>> BIG_STOVE_PART = BLOCK_ENTITIES.register(
            "big_stove_part",
            () -> BlockEntityType.Builder.of(BigStovePartBlockEntity::new, ModBlocks.BIG_STOVE.get()).build(null));

    /** 放下的盆装谜之炖菜：存着这一盆给什么效果 */
    public static final Supplier<BlockEntityType<MysteryStewPotBlockEntity>> MYSTERY_STEW_POT = BLOCK_ENTITIES.register(
            "mystery_stew_pot",
            () -> BlockEntityType.Builder.of(MysteryStewPotBlockEntity::new, ModBlocks.MYSTERY_STEW_POT.get()).build(null));

    /** 脸盆架：保留方块实体类型以兼容旧存档，治疗冷却已改存玩家数据 */
    public static final Supplier<BlockEntityType<BasinStandBlockEntity>> BASIN_STAND = BLOCK_ENTITIES.register(
            "basin_stand",
            () -> BlockEntityType.Builder.of(BasinStandBlockEntity::new, ModBlocks.BASIN_STAND.get()).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
