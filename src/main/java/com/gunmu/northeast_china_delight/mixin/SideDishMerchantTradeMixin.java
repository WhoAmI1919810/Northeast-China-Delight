package com.gunmu.northeast_china_delight.mixin;

import com.gunmu.northeast_china_delight.item.ModItems;
import com.gunmu.northeast_china_delight.villager.ModVillagerProfessions;
import com.gunmu.northeast_china_delight.villager.SideDishMerchantTrades;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 原版每级只会从交易池里随机抽两条；副食商升到 5 级后额外补齐黄桃罐头，
 * 让大师级交易必定包含这一条。若随机抽取已经选中，则不重复添加。
 */
@Mixin(Villager.class)
public abstract class SideDishMerchantTradeMixin
{
    @Inject(method = "updateTrades", at = @At("TAIL"))
    private void northeast_china_delight$guaranteeCannedYellowPeach(CallbackInfo callback)
    {
        Villager villager = (Villager) (Object) this;
        if (villager.getVillagerData().getProfession() != ModVillagerProfessions.SIDE_DISH_MERCHANT.get()
                || villager.getVillagerData().getLevel() < 5)
        {
            return;
        }

        MerchantOffers offers = villager.getOffers();
        for (MerchantOffer offer : offers)
        {
            if (offer.getResult().is(ModItems.CANNED_YELLOW_PEACH.get()))
            {
                return;
            }
        }

        VillagerTrades.ItemListing listing = SideDishMerchantTrades.cannedYellowPeachTrade();
        MerchantOffer offer = listing.getOffer(villager, villager.getRandom());
        if (offer != null)
        {
            offers.add(offer);
        }
    }
}