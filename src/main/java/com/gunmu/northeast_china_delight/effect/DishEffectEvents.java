package com.gunmu.northeast_china_delight.effect;

import com.gunmu.northeast_china_delight.item.ModItems;
import com.gunmu.northeast_china_delight.item.SeasoningBottleItem;
import com.gunmu.northeast_china_delight.util.DdIds;
import com.gunmu.northeast_china_delight.util.DdAttributes;
import com.gunmu.northeast_china_delight.util.DdEffects;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
//? if <1.20.2 {
import net.minecraftforge.eventbus.api.SubscribeEvent;
//?} else {
/*import net.neoforged.bus.api.SubscribeEvent;
*///?}
//? if <1.20.2 {
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
//?} else {
/*import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
*///?}
//? if <1.20.2 {
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
//?} else {
/*import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
*///?}
//? if <1.20.2 {
import net.minecraftforge.event.entity.player.PlayerEvent;
//?} else {
/*import net.neoforged.neoforge.event.entity.player.PlayerEvent;
*///?}
//? if <1.20.2 {
import net.minecraftforge.event.TickEvent;
//?} else if <1.20.5 {
/*import net.neoforged.neoforge.event.TickEvent;*/
//?} else {
/*import net.neoforged.neoforge.event.tick.PlayerTickEvent;
*///?}

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DishEffectEvents {

    private static final Map<UUID, Float> LAST_EXHAUSTION = new HashMap<>();
    private static final Map<UUID, Float> REGEN_CREDIT = new HashMap<>();

    @SubscribeEvent
    public static void onFinishUsingItem(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()) {
            return;
        }
        ItemStack stack = event.getItem();
        DishFlavors.Info info = DishFlavors.of(stack.getItem());
        if (info == null) {
            return;
        }

        switch (info.flavor()) {
            case GREASY -> DdEffects.removeEffect(player, ModEffects.REFRESHING);
            case REFRESHING -> DdEffects.removeEffect(player, ModEffects.GREASY);
            default -> {
            }
        }
        for (DishEffects.Applied applied : DishEffects.of(info)) {
            player.addEffect(applied.instance());
        }
        int warmth = DishFlavors.warmthTicks(stack.getItem(), info);
        if (warmth > 0) {
            for (DishEffects.Applied applied : DishEffects.warmth(warmth)) {
                player.addEffect(applied.instance());
            }
        }
        if (stack.is(ModItems.SHEN_JI_TANG.get()) || stack.is(ModItems.CONG_SHAO_HAI_SHEN.get())) {
            for (DishEffects.Applied applied : DishEffects.nourishingExtras()) {
                player.addEffect(applied.instance());
            }
        }
        if (stack.is(ModItems.ORANGE_GUO_BAO_ROU.get())) {
            for (DishEffects.Applied applied : DishEffects.orangeJuiceExtras()) {
                player.addEffect(applied.instance());
            }
        }
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.is(ModItems.GRILL_RACK.get())) {
            event.getToolTip().add(Component.translatable("tooltip.northeast_china_delight.grill_rack")
                    .withStyle(ChatFormatting.GRAY));
        }
        if (SeasoningBottleItem.isBottle(stack)) {
            event.getToolTip().add(Component.translatable("tooltip.northeast_china_delight.bottle_amount",
                            SeasoningBottleItem.remainingMb(stack),
                            SeasoningBottleItem.CAPACITY_MB,
                            SeasoningBottleItem.remainingUses(stack))
                    .withStyle(ChatFormatting.GRAY));
        }
        if (stack.is(ModItems.DEEP_FRY_OILS)) {
            event.getToolTip().add(Component.translatable("tooltip.northeast_china_delight.deep_fry_oil")
                    .withStyle(ChatFormatting.GRAY));
        }
        DishFlavors.Info info = DishFlavors.of(stack.getItem());
        if (info == null) {
            return;
        }
        List<DishEffects.Applied> effects = new ArrayList<>(DishEffects.of(info));
        int warmth = DishFlavors.warmthTicks(stack.getItem(), info);
        if (warmth > 0) {
            effects.addAll(DishEffects.warmth(warmth));
        }
        if (stack.is(ModItems.SHEN_JI_TANG.get()) || stack.is(ModItems.CONG_SHAO_HAI_SHEN.get())) {
            effects.addAll(DishEffects.nourishingExtras());
        }
        if (stack.is(ModItems.ORANGE_GUO_BAO_ROU.get())) {
            effects.addAll(DishEffects.orangeJuiceExtras());
        }
        if (effects.isEmpty()) {
            return;
        }

        List<Component> tooltip = event.getToolTip();
        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("tooltip.northeast_china_delight.dish_effects")
                .withStyle(ChatFormatting.GRAY));
        for (DishEffects.Applied applied : effects) {
            MobEffectInstance instance = applied.instance();
            Component name = Component.translatable(instance.getDescriptionId());
            if (instance.getAmplifier() > 0) {
                name = Component.translatable("potion.withAmplifier", name,
                        Component.translatable("potion.potency." + instance.getAmplifier()));
            }
            name = name.copy().withStyle(DdEffects.category(instance).getTooltipFormatting());
            tooltip.add(Component.literal(" ")
                    .append(name)
                    .append(" (")
                    //? if <1.20.2 {
                    .append(MobEffectUtil.formatDuration(instance, 1.0F))
                    //?} else {
                    /*.append(MobEffectUtil.formatDuration(instance, 1.0F, 20.0F))
                    *///?}
                    .append(")")
                    .withStyle(ChatFormatting.GRAY));
        }
    }

    @SubscribeEvent
    public static void onStartUsingItem(LivingEntityUseItemEvent.Start event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()) {
            return;
        }
        DishFlavors.Flavor flavor = DishFlavors.flavorOf(event.getItem().getItem());
        Holder<MobEffect> effect = switch (flavor == null ? DishFlavors.Flavor.BALANCED : flavor) {
            case GREASY -> DdEffects.hold(ModEffects.GREASY);
            case REFRESHING -> DdEffects.hold(ModEffects.REFRESHING);
            default -> null;
        };
        if (effect == null) {
            return;
        }
        MobEffectInstance instance = DdEffects.getEffect(player, effect);
        if (instance == null) {
            return;
        }

        int percent = 50 + (instance.getAmplifier() + 1) * 10;
        float remaining = Math.max(0.25F, 1.0F - percent / 100.0F);
        int duration = Math.min(event.getDuration() * 4, Math.round(event.getDuration() / remaining));
        event.setDuration(duration);
    }

    @SubscribeEvent
    //? if <1.20.5 {
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        tickPlayer(event.player);
    }
    //?} else {
    /*public static void onPlayerTick(PlayerTickEvent.Post event) {
        tickPlayer(event.getEntity());
    }
    *///?}

    private static void tickPlayer(Player player) {
        if (player.level().isClientSide()) {
            return;
        }
        UUID id = player.getUUID();
        FoodData food = player.getFoodData();

        MobEffectInstance greasy = DdEffects.getEffect(player, ModEffects.GREASY);
        float exhaustion = food.getExhaustionLevel();
        Float last = LAST_EXHAUSTION.put(id, exhaustion);
        if (greasy != null && last != null && exhaustion > last) {
            int percent = 50 + (greasy.getAmplifier() + 1) * 10;
            float keep = Math.max(0.1F, 1.0F - percent / 100.0F);
            float adjusted = last + (exhaustion - last) * keep;
            food.setExhaustion(adjusted);
            LAST_EXHAUSTION.put(id, adjusted);
        }

        MobEffectInstance refreshing = DdEffects.getEffect(player, ModEffects.REFRESHING);
        applyWarmthSpeed(player);
        if (refreshing == null || player.getHealth() >= player.getMaxHealth() || food.getFoodLevel() < 18) {
            REGEN_CREDIT.remove(id);
            return;
        }
        int level = refreshing.getAmplifier() + 1;
        float saturation = food.getSaturationLevel();
        float percent = (20.0F + saturation * level) / 100.0F;
        float baseRate = food.getFoodLevel() >= 20 && saturation > 0 ? 0.1F : 1.0F / 80.0F;
        float credit = REGEN_CREDIT.getOrDefault(id, 0.0F) + baseRate * percent;
        while (credit >= 1.0F && player.getHealth() < player.getMaxHealth()) {
            player.heal(1.0F);
            food.addExhaustion(6.0F);
            credit -= 1.0F;
        }
        REGEN_CREDIT.put(id, credit);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID id = event.getEntity().getUUID();
        LAST_EXHAUSTION.remove(id);
        REGEN_CREDIT.remove(id);
    }

    private static final net.minecraft.resources.ResourceLocation WARMTH_SPEED_ID =
            DdIds.of(com.gunmu.northeast_china_delight.NortheastChinaDelight.MODID, "warmth_speed");
    public static final double WARMTH_SPEED_PER_LEVEL = 0.05D;
    public static final float COLD_TEMPERATURE = 0.15F;

    private static void applyWarmthSpeed(Player player) {
        net.minecraft.world.entity.ai.attributes.AttributeInstance attribute =
                player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
        if (attribute == null) {
            return;
        }
        MobEffectInstance warmth = DdEffects.getEffect(player, ModEffects.WARMTH);
        int level = warmth == null ? 0 : warmth.getAmplifier() + 1;
        boolean cold = level > 0 && isColdBiome(player);
        net.minecraft.world.entity.ai.attributes.AttributeModifier existing =
                DdAttributes.get(attribute, WARMTH_SPEED_ID);
        if (!cold) {
            if (existing != null) {
                DdAttributes.remove(attribute, WARMTH_SPEED_ID);
            }
            return;
        }
        double amount = WARMTH_SPEED_PER_LEVEL * level;
        if (existing != null && Math.abs(DdAttributes.amount(existing) - amount) < 1.0E-6D) {
            return;
        }
        if (existing != null) {
            DdAttributes.remove(attribute, WARMTH_SPEED_ID);
        }
        attribute.addTransientModifier(DdAttributes.create(
                WARMTH_SPEED_ID, amount,
                DdAttributes.OP_MULTIPLY_BASE));
    }

    public static boolean isColdBiome(Player player) {
        Holder<net.minecraft.world.level.biome.Biome> biome =
                player.level().getBiome(player.blockPosition());
        return biome.value().getBaseTemperature() <= COLD_TEMPERATURE;
    }
}
