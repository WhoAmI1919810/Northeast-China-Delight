package com.gunmu.northeast_china_delight.villager;

import com.gunmu.northeast_china_delight.ModMapDecorations;
import com.gunmu.northeast_china_delight.NortheastChinaDelight;
import com.gunmu.northeast_china_delight.util.DdIds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
//? if >=1.20.5 {
import net.minecraft.core.component.DataComponents;
//?}
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
//? if >=1.20.5 {
import net.minecraft.world.item.trading.ItemCost;
//?}
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.levelgen.structure.Structure;

//? if >=1.20.5 {
import net.minecraft.world.level.saveddata.maps.MapDecorationType;
//?}
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

import javax.annotation.Nullable;
import java.util.Optional;

public class YardMapForEmeralds implements VillagerTrades.ItemListing
{
    private static final TagKey<Structure> DESTINATION = TagKey.create(Registries.STRUCTURE,
            DdIds.of(NortheastChinaDelight.MODID, "on_northeast_yard_maps"));

    private static final String DISPLAY_NAME = "filled_map.northeast_yard";

    private static final String DECORATION_LABEL = "+";

    private static final byte MAP_SCALE = 2;

    private final int emeraldCost;
    private final int maxUses;
    private final int villagerXp;

    public YardMapForEmeralds(int emeraldCost, int maxUses, int villagerXp)
    {
        this.emeraldCost = emeraldCost;
        this.maxUses = maxUses;
        this.villagerXp = villagerXp;
    }

    @Nullable
    @Override
    public MerchantOffer getOffer(Entity trader, RandomSource random)
    {
        if (!(trader.level() instanceof ServerLevel serverLevel))
        {
            return null;
        }
        BlockPos yardPos = serverLevel.findNearestMapStructure(DESTINATION, trader.blockPosition(), 100, true);
        if (yardPos == null)
        {
            return null;
        }
        ItemStack map = MapItem.create(serverLevel, yardPos.getX(), yardPos.getZ(), MAP_SCALE, true, true);
        MapItem.renderBiomePreviewMap(serverLevel, map);
        //? if <1.20.5 {
        /*MapItemSavedData.addTargetDecoration(map, yardPos, DECORATION_LABEL, ModMapDecorations.DONGBEI_YARD);
        *///?} else {
        MapItemSavedData.addTargetDecoration(map, yardPos, DECORATION_LABEL, ModMapDecorations.dongbeiYardHolder());
        //?}
        //? if <1.20.5 {
        /*map.setHoverName(Component.translatable(DISPLAY_NAME));
        return new MerchantOffer(new ItemStack(Items.EMERALD, this.emeraldCost), new ItemStack(Items.COMPASS),
                map, this.maxUses, this.villagerXp, 0.2F);*/
        //?} else {
        map.set(DataComponents.ITEM_NAME, Component.translatable(DISPLAY_NAME));
        return new MerchantOffer(new ItemCost(Items.EMERALD, this.emeraldCost), Optional.of(new ItemCost(Items.COMPASS)),
                map, this.maxUses, this.villagerXp, 0.2F);
        //?}
    }
}
