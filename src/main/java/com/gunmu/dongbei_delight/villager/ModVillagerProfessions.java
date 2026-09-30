package com.gunmu.dongbei_delight.villager;

import com.google.common.collect.ImmutableSet;
import com.gunmu.dongbei_delight.DongbeiDelight;
import com.gunmu.dongbei_delight.block.ModBlocks;
import com.gunmu.dongbei_delight.item.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Set;

/**
 * 副食商：卖调料和食材的村民职业。
 *
 * 工作方块用大缸（{@link ModBlocks#VAT}），所以老存档里已有的缸会自动成为职业方块，
 * 不需要额外加一个工作方块贴图。以后要有专门的工作方块，只要把下面的
 * {@link #VAT_POI} 改成新方块的方块状态即可，职业和交易表都不用动。
 */
public class ModVillagerProfessions
{
    public static final DeferredRegister<PoiType> POI_TYPES =
            DeferredRegister.create(Registries.POINT_OF_INTEREST_TYPE, DongbeiDelight.MODID);
    public static final DeferredRegister<VillagerProfession> PROFESSIONS =
            DeferredRegister.create(Registries.VILLAGER_PROFESSION, DongbeiDelight.MODID);

    /** 副食商的工作站点：大缸的全部方块状态 */
    public static final DeferredHolder<PoiType, PoiType> VAT_POI =
            POI_TYPES.register("side_dish_merchant", () -> new PoiType(vatStates(), 1, 1));

    public static final DeferredHolder<VillagerProfession, VillagerProfession> SIDE_DISH_MERCHANT =
            PROFESSIONS.register("side_dish_merchant", () -> new VillagerProfession(
                    "side_dish_merchant",
                    // 已经就职的副食商认自己的大缸
                    holder -> holder.is(VAT_POI.getKey()),
                    // 待业村民也认这个站点
                    holder -> holder.is(VAT_POI.getKey()),
                    // 会顺手捡起来的东西
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
