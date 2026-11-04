package com.gunmu.northeast_china_delight.mixin;

import com.gunmu.northeast_china_delight.item.NortheastOutfit;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 「东北行头」四件套的「自家人」折扣：原版开始交易时在 updateSpecialPrices 里按声望、村庄英雄给折扣，
 * 这里在它最后再按四件套少要一点（{@link NortheastOutfit#applyVillagerDiscount}）。
 * 关掉交易界面时原版 resetSpecialPrices 会把这些折扣一起清掉，所以每次开交易都重新算、不会越叠越多。
 */
@Mixin(Villager.class)
public abstract class VillagerOutfitDiscountMixin
{
    @Inject(method = "updateSpecialPrices", at = @At("TAIL"))
    private void northeast_china_delight$outfitDiscount(Player player, CallbackInfo callback)
    {
        NortheastOutfit.applyVillagerDiscount((Villager) (Object) this, player);
    }
}
