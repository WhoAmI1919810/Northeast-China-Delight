package com.gunmu.northeast_china_delight.gametest;

import com.gunmu.northeast_china_delight.NortheastChinaDelight;
import com.gunmu.northeast_china_delight.item.ModItems;
import com.gunmu.northeast_china_delight.item.NortheastOutfit;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
//? if <1.20.2 {
/*import net.minecraftforge.gametest.GameTestHolder;
*///?} else {
import net.neoforged.neoforge.gametest.GameTestHolder;
//?}
//? if <1.20.2 {
/*import net.minecraftforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
//?}

@GameTestHolder(NortheastChinaDelight.MODID)
@PrefixGameTestTemplate(false)
public final class NortheastOutfitGameTests {

    private NortheastOutfitGameTests() {
    }

    private static Player mockPlayer(GameTestHelper helper) {
        //? if <1.20.5 {
        /*return helper.makeMockPlayer();
        *///?} else {
        return helper.makeMockPlayer(GameType.CREATIVE);
        //?}
    }

    private static void equipOutfit(Player player) {
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.LARGE_BASIN.get()));
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.FLORAL_JACKET.get()));
        player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.WOODEN_MALLET.get()));
        player.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(ModItems.BIG_POT_LID.get()));
    }

    @GameTest(template = "empty")
    public static void completeOutfitRefreshesStrengthOne(GameTestHelper helper) {
        Player player = mockPlayer(helper);
        equipOutfit(player);

        helper.assertTrue(NortheastOutfit.isComplete(player), "四件齐全时应识别为东北行头");
        NortheastOutfit.refreshStrength(player);
        MobEffectInstance strength = player.getEffect(MobEffects.DAMAGE_BOOST);
        helper.assertTrue(strength != null, "四件齐全时应获得力量");
        helper.assertTrue(strength.getAmplifier() == 0, "四件套给的是力量 I");
        helper.assertTrue(strength.getDuration() == NortheastOutfit.STRENGTH_DURATION_TICKS,
                "四件套的力量应是 5 秒的短效果");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void incompleteOutfitDoesNotGrantStrength(GameTestHelper helper) {
        Player player = mockPlayer(helper);
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.LARGE_BASIN.get()));
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.FLORAL_JACKET.get()));
        player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.WOODEN_MALLET.get()));

        helper.assertTrue(!NortheastOutfit.isComplete(player), "少锅盖时不应识别为四件套");
        NortheastOutfit.refreshStrength(player);
        helper.assertTrue(!player.hasEffect(MobEffects.DAMAGE_BOOST), "少一件时不应获得力量");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void outfitStrengthDoesNotDowngradeStrongerEffect(GameTestHelper helper) {
        Player player = mockPlayer(helper);
        equipOutfit(player);
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 1));

        NortheastOutfit.refreshStrength(player);
        MobEffectInstance strength = player.getEffect(MobEffects.DAMAGE_BOOST);
        helper.assertTrue(strength != null && strength.getAmplifier() == 1 && strength.getDuration() == 200,
                "四件套不应把力量 II 降成力量 I 或缩短它");
        helper.succeed();
    }
}
