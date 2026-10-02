package com.gunmu.northeast_china_delight.effect;

import com.gunmu.northeast_china_delight.item.ModItems;
import com.gunmu.northeast_china_delight.item.SeasoningBottleItem;
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
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 菜肴口味效果的全部机制：
 *
 * <ul>
 *     <li>吃完油腻菜 → 缓慢 + 生命提升 2 + 急迫 + 油腻 2，并解除爽口；</li>
 *     <li>吃完爽口菜 → 爽口 + 速度，并解除油腻；</li>
 *     <li>吃完荤素搭配的菜 → 农夫乐事的滋养；</li>
 *     <li>带着「油腻」再吃油腻菜、带着「爽口」再吃爽口菜 → 进食速度降低 (50 + 等级×10)%；</li>
 *     <li>油腻：饱食度与饱和度下降速度降低 (50 + 等级×10)%；</li>
 *     <li>爽口：回血速度提高 (20 + 当前饱和度 × 等级)%，回血消耗的饥饿值照常扣除。</li>
 * </ul>
 */
public class DishEffectEvents {

    /** 每 tick 记录上一 tick 的饥饿消耗，用来把「油腻」的减伤补回去 */
    private static final Map<UUID, Float> LAST_EXHAUSTION = new HashMap<>();
    /** 「爽口」额外回血的进度累计 */
    private static final Map<UUID, Float> REGEN_CREDIT = new HashMap<>();

    // ===== 吃菜：上状态 =====

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

        // 两种口味互相解除
        switch (info.flavor()) {
            case GREASY -> player.removeEffect(ModEffects.REFRESHING);
            case REFRESHING -> player.removeEffect(ModEffects.GREASY);
            default -> {
            }
        }
        for (DishEffects.Applied applied : DishEffects.of(info)) {
            player.addEffect(applied.instance());
        }
        // 汤类：额外给「暖身」（寒冷群系加速 + 不陷细雪）
        if (DishFlavors.isSoup(stack.getItem())) {
            for (DishEffects.Applied applied : DishEffects.soupWarmth(info.durationTicks())) {
                player.addEffect(applied.instance());
            }
        }
        // 新派橙汁锅包肉：额外带上橘子汁本身的 5 秒生命恢复与解毒
        if (stack.is(ModItems.ORANGE_GUO_BAO_ROU.get())) {
            for (DishEffects.Applied applied : DishEffects.orangeJuiceExtras()) {
                player.addEffect(applied.instance());
            }
        }
    }

    // ===== 食物提示框：像农夫乐事的盘装食物那样列出吃完的效果 =====

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        // 烧烤架：提示一下用法（它自己不消耗、要架在营火上）
        if (stack.is(ModItems.GRILL_RACK.get())) {
            event.getToolTip().add(Component.translatable("tooltip.northeast_china_delight.grill_rack")
                    .withStyle(ChatFormatting.GRAY));
        }
        // 瓶装调料：把还剩多少 mB、还能用几次写出来
        if (SeasoningBottleItem.isBottle(stack)) {
            event.getToolTip().add(Component.translatable("tooltip.northeast_china_delight.bottle_amount",
                            SeasoningBottleItem.remainingMb(stack),
                            SeasoningBottleItem.CAPACITY_MB,
                            SeasoningBottleItem.remainingUses(stack))
                    .withStyle(ChatFormatting.GRAY));
        }
        // 植物油 / 动物油：油炸菜一次烧完一整瓶，别的做法照常按 50 mB 扣
        if (stack.is(ModItems.DEEP_FRY_OILS)) {
            event.getToolTip().add(Component.translatable("tooltip.northeast_china_delight.deep_fry_oil")
                    .withStyle(ChatFormatting.GRAY));
        }
        DishFlavors.Info info = DishFlavors.of(stack.getItem());
        if (info == null) {
            return;
        }
        List<DishEffects.Applied> effects = new ArrayList<>(DishEffects.of(info));
        if (DishFlavors.isSoup(stack.getItem())) {
            effects.addAll(DishEffects.soupWarmth(info.durationTicks()));
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
            name = name.copy().withStyle(instance.getEffect().value().getCategory().getTooltipFormatting());
            tooltip.add(Component.literal(" ")
                    .append(name)
                    .append(" (")
                    .append(MobEffectUtil.formatDuration(instance, 1.0F, 20.0F))
                    .append(")")
                    .withStyle(ChatFormatting.GRAY));
        }
    }

    // ===== 进食速度：带着同口味状态时吃得更慢 =====

    @SubscribeEvent
    public static void onStartUsingItem(LivingEntityUseItemEvent.Start event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()) {
            return;
        }
        DishFlavors.Flavor flavor = DishFlavors.flavorOf(event.getItem().getItem());
        Holder<MobEffect> effect = switch (flavor == null ? DishFlavors.Flavor.BALANCED : flavor) {
            case GREASY -> ModEffects.GREASY;
            case REFRESHING -> ModEffects.REFRESHING;
            default -> null;
        };
        if (effect == null) {
            return;
        }
        MobEffectInstance instance = player.getEffect(effect);
        if (instance == null) {
            return;
        }

        // 速度降低 (50 + 等级×10)%：耗时 = 原耗时 / 剩余速度比例，最多拖到 4 倍
        int percent = 50 + (instance.getAmplifier() + 1) * 10;
        float remaining = Math.max(0.25F, 1.0F - percent / 100.0F);
        int duration = Math.min(event.getDuration() * 4, Math.round(event.getDuration() / remaining));
        event.setDuration(duration);
    }

    // ===== 每 tick 的持续效果 =====

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) {
            return;
        }
        UUID id = player.getUUID();
        FoodData food = player.getFoodData();

        // 油腻：把这一 tick 新增的饥饿消耗按比例减掉
        MobEffectInstance greasy = player.getEffect(ModEffects.GREASY);
        float exhaustion = food.getExhaustionLevel();
        Float last = LAST_EXHAUSTION.put(id, exhaustion);
        if (greasy != null && last != null && exhaustion > last) {
            int percent = 50 + (greasy.getAmplifier() + 1) * 10;
            float keep = Math.max(0.1F, 1.0F - percent / 100.0F);
            float adjusted = last + (exhaustion - last) * keep;
            food.setExhaustion(adjusted);
            LAST_EXHAUSTION.put(id, adjusted);
        }

        // 爽口：回血更快（照常消耗饥饿值）
        MobEffectInstance refreshing = player.getEffect(ModEffects.REFRESHING);
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

    // ===== 暖身：寒冷覆雪群系里跑得动 =====

    /** 移动速度修饰符的 id（每次 tick 按需刷新，离开寒冷群系立刻撤掉） */
    private static final net.minecraft.resources.ResourceLocation WARMTH_SPEED_ID =
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                    com.gunmu.northeast_china_delight.NortheastChinaDelight.MODID, "warmth_speed");
    /** 每级加多少移动速度：5% */
    public static final double WARMTH_SPEED_PER_LEVEL = 0.05D;
    /**
     * 「寒冷覆雪」的判定阈值：生物群系基础温度低于它就生效。
     * 想改生效范围（比如连温带针叶林也算），改这一个数就行。
     */
    public static final float COLD_TEMPERATURE = 0.15F;

    private static void applyWarmthSpeed(Player player) {
        net.minecraft.world.entity.ai.attributes.AttributeInstance attribute =
                player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
        if (attribute == null) {
            return;
        }
        MobEffectInstance warmth = player.getEffect(ModEffects.WARMTH);
        int level = warmth == null ? 0 : warmth.getAmplifier() + 1;
        boolean cold = level > 0 && isColdBiome(player);
        net.minecraft.world.entity.ai.attributes.AttributeModifier existing =
                attribute.getModifier(WARMTH_SPEED_ID);
        if (!cold) {
            if (existing != null) {
                attribute.removeModifier(WARMTH_SPEED_ID);
            }
            return;
        }
        double amount = WARMTH_SPEED_PER_LEVEL * level;
        if (existing != null && Math.abs(existing.amount() - amount) < 1.0E-6D) {
            return;
        }
        if (existing != null) {
            attribute.removeModifier(WARMTH_SPEED_ID);
        }
        attribute.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                WARMTH_SPEED_ID, amount,
                net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
    }

    /** 寒冷覆雪的生物群系：按生物群系基础温度判断 */
    public static boolean isColdBiome(Player player) {
        Holder<net.minecraft.world.level.biome.Biome> biome =
                player.level().getBiome(player.blockPosition());
        return biome.value().getBaseTemperature() <= COLD_TEMPERATURE;
    }
}
