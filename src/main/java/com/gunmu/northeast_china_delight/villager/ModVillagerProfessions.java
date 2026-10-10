package com.gunmu.northeast_china_delight.villager;

import com.google.common.collect.ImmutableSet;
import com.gunmu.northeast_china_delight.NortheastChinaDelight;
import com.gunmu.northeast_china_delight.block.ModBlocks;
import com.gunmu.northeast_china_delight.item.ModItems;
import com.gunmu.northeast_china_delight.util.DdIds;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.level.block.state.BlockState;
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

import java.util.Set;
import java.util.function.Supplier;

public class ModVillagerProfessions
{
    public static final DeferredRegister<PoiType> POI_TYPES =
            DeferredRegister.create(Registries.POINT_OF_INTEREST_TYPE, NortheastChinaDelight.MODID);
    public static final DeferredRegister<VillagerProfession> PROFESSIONS =
            DeferredRegister.create(Registries.VILLAGER_PROFESSION, NortheastChinaDelight.MODID);

    public static final Supplier<PoiType> VAT_POI =
            POI_TYPES.register("side_dish_merchant", () -> new PoiType(vatStates(), 1, 1));

    private static final ResourceLocation VAT_POI_ID =
            DdIds.of(NortheastChinaDelight.MODID, "side_dish_merchant");

    public static final Supplier<VillagerProfession> SIDE_DISH_MERCHANT =
            PROFESSIONS.register("side_dish_merchant", () -> new VillagerProfession(
                    "side_dish_merchant",
                    holder -> holder.unwrapKey().map(key -> key.location().equals(VAT_POI_ID)).orElse(false),
                    holder -> holder.unwrapKey().map(key -> key.location().equals(VAT_POI_ID)).orElse(false),
                    ImmutableSet.of(
                            ModItems.SALT.get(),
                            ModItems.SOYBEAN.get(),
                            ModItems.NAPA_CABBAGE.get()),
                    ImmutableSet.of(),
                    SoundEvents.VILLAGER_WORK_FARMER));

    private ModVillagerProfessions()
    {
    }

    private static Set<BlockState> vatStates()
    {
        return ImmutableSet.copyOf(ModBlocks.VAT.get().getStateDefinition().getPossibleStates());
    }

    public static void register(IEventBus eventBus)
    {
        POI_TYPES.register(eventBus);
        PROFESSIONS.register(eventBus);
    }
}
