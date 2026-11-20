package com.gunmu.northeast_china_delight.effect;


import com.gunmu.chinese_regional_culinary_ingredients.effect.CrciMobEffects;
import com.gunmu.northeast_china_delight.block.PlacedDishEating;
import com.gunmu.northeast_china_delight.item.ModItems;
import com.gunmu.chinese_regional_culinary_ingredients.item.MysteryStewEffects;
import com.gunmu.chinese_regional_culinary_ingredients.item.SeasoningBottleItem;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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

    /** 额外送「养生」10 秒的菜：参鸡汤、葱烧海参、海参豆腐汤。吃和提示框共用这一份判断，别再各写一半。 */
    private static boolean givesNourishing(ItemStack stack) {
        return stack.is(ModItems.SHEN_JI_TANG.get())
                || stack.is(ModItems.CONG_SHAO_HAI_SHEN.get())
                || stack.is(ModItems.HAI_SHEN_DOU_FU_TANG.get());
    }

    /** 会赋予「养生」效果的全部菜肴（与 {@link #givesNourishing} 共用同一份清单，供 JEI 信息页登记）。 */
    public static List<Item> nourishingItems() {
        return List.of(
                ModItems.SHEN_JI_TANG.get(),
                ModItems.CONG_SHAO_HAI_SHEN.get(),
                ModItems.HAI_SHEN_DOU_FU_TANG.get());
    }

    @SubscribeEvent
    public static void onFinishUsingItem(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()) {
            return;
        }
        ItemStack stack = event.getItem();
        if (stack.is(ModItems.CANNED_YELLOW_PEACH.get())) {
            for (DishEffects.Applied applied : DishEffects.cannedYellowPeachExtras()) {
                player.addEffect(applied.instance());
            }
        }
        DishFlavors.Info info = DishFlavors.of(stack.getItem());
        if (info == null) {
            return;
        }

        switch (info.flavor()) {
            case GREASY -> DdEffects.removeEffect(player, CrciMobEffects.REFRESHING);
            case REFRESHING -> DdEffects.removeEffect(player, CrciMobEffects.GREASY);
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
        if (givesNourishing(stack)) {
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

    /**
     * 摆放菜肴分阶段食用时的状态效果结算（不额外提示数值差异）。
     * <ul>
     *   <li>1 口小餐盘菜：直接按满态给 100% 效果；</li>
     *   <li>2 / 3 口分阶段菜：常规效果按口分摊（2 口 60%/口、3 口 40%/口）并有上限累加至 115%；</li>
     *   <li>养生与生命恢复同样按口拆开，且合起来的总时长略高于手持整吃。</li>
     * </ul>
     */
    public static void applyPlacedDishEffects(Player player, Item dishItem, int totalBites) {
        ItemStack stack = new ItemStack(dishItem);
        if (stack.is(ModItems.CANNED_YELLOW_PEACH.get())) {
            // 满态生命恢复 I 200t（10s，回 4 HP）：3 口每口 100t（5s，保底回 2 HP），累加封顶 250t（12.5s，3 口连吃共回 5 HP）
            int delta = totalBites <= 1 ? 200 : 100;
            int cap = totalBites <= 1 ? 200 : 250;
            for (DishEffects.Applied applied : DishEffects.cannedYellowPeachExtras()) {
                if (totalBites <= 1) {
                    player.addEffect(applied.instance());
                } else {
                    addStackedEffect(player, applied.effect(), applied.amplifier(), delta, cap);
                }
            }
        }
        DishFlavors.Info info = DishFlavors.of(dishItem);
        if (info == null) {
            return;
        }

        switch (info.flavor()) {
            case GREASY -> DdEffects.removeEffect(player, CrciMobEffects.REFRESHING);
            case REFRESHING -> DdEffects.removeEffect(player, CrciMobEffects.GREASY);
            default -> {
            }
        }

        if (totalBites <= 1) {
            for (DishEffects.Applied applied : DishEffects.of(info)) {
                player.addEffect(applied.instance());
            }
            int warmth = DishFlavors.warmthTicks(dishItem, info);
            if (warmth > 0) {
                for (DishEffects.Applied applied : DishEffects.warmth(warmth)) {
                    player.addEffect(applied.instance());
                }
            }
            if (givesNourishing(stack)) {
                for (DishEffects.Applied applied : DishEffects.nourishingExtras()) {
                    player.addEffect(applied.instance());
                }
            }
            if (stack.is(ModItems.ORANGE_GUO_BAO_ROU.get())) {
                for (DishEffects.Applied applied : DishEffects.orangeJuiceExtras()) {
                    player.addEffect(applied.instance());
                }
            }
            return;
        }

        float perBiteRatio = totalBites == 2 ? 0.60F : 0.40F;
        float maxCapRatio = 1.15F;

        for (DishEffects.Applied applied : DishEffects.of(info)) {
            int delta = Math.max(1, Math.round(applied.durationTicks() * perBiteRatio));
            int cap = Math.max(delta, Math.round(applied.durationTicks() * maxCapRatio));
            addStackedEffect(player, applied.effect(), applied.amplifier(), delta, cap);
        }
        int warmth = DishFlavors.warmthTicks(dishItem, info);
        if (warmth > 0) {
            for (DishEffects.Applied applied : DishEffects.warmth(warmth)) {
                int delta = Math.max(1, Math.round(applied.durationTicks() * perBiteRatio));
                int cap = Math.max(delta, Math.round(applied.durationTicks() * maxCapRatio));
                addStackedEffect(player, applied.effect(), applied.amplifier(), delta, cap);
            }
        }
        if (givesNourishing(stack)) {
            // 满态 200t（10s）：3 口 80t/口、2 口 120t/口，上限 240t（12s）
            int delta = totalBites == 2 ? 120 : 80;
            int cap = 240;
            for (DishEffects.Applied applied : DishEffects.nourishingExtras()) {
                addStackedEffect(player, applied.effect(), applied.amplifier(), delta, cap);
            }
        }
        if (stack.is(ModItems.ORANGE_GUO_BAO_ROU.get())) {
            // 满态生命恢复 I 100t（每 50t 回 1 HP，共 2 HP）：每口 50t 保底跳 1 点血，3 口上限 150t（共 3 HP）
            int delta = 50;
            int cap = 150;
            for (DishEffects.Applied applied : DishEffects.orangeJuiceExtras()) {
                addStackedEffect(player, applied.effect(), applied.amplifier(), delta, cap);
            }
        }
    }

    private static void addStackedEffect(Player player, Holder<MobEffect> effect, int amplifier,
                                         int deltaTicks, int maxDurationTicks) {
        if (deltaTicks <= 0) {
            return;
        }
        MobEffectInstance existing = DdEffects.getEffect(player, effect);
        int newDuration;
        if (existing != null) {
            if (existing.getAmplifier() > amplifier) {
                return;
            }
            if (existing.getAmplifier() == amplifier) {
                int cur = existing.getDuration();
                newDuration = Math.max(cur, Math.min(cur + deltaTicks, maxDurationTicks));
            } else {
                newDuration = Math.max(existing.getDuration(), deltaTicks);
            }
        } else {
            newDuration = deltaTicks;
        }
        player.addEffect(DdEffects.instance(effect, newDuration, amplifier));
    }

    /** 若玩家身上正好带着与这道菜同口味的减速效果（油腻吃油腻 / 爽口吃爽口），返回该效果实例，否则返回 null。 */
    public static MobEffectInstance matchingSlowdownEffect(Player player, Item item) {
        DishFlavors.Flavor flavor = DishFlavors.flavorOf(item);
        if (flavor == null) {
            return null;
        }
        Holder<MobEffect> effect = switch (flavor) {
            case GREASY -> DdEffects.hold(CrciMobEffects.GREASY);
            case REFRESHING -> DdEffects.hold(CrciMobEffects.REFRESHING);
            default -> null;
        };
        return effect == null ? null : DdEffects.getEffect(player, effect);
    }

    /** 油腻/爽口同口味手持进食减速耗时。 */
    public static int applyFlavorSlowdown(Player player, Item item, int baseTicks) {
        if (baseTicks <= 0) {
            return 0;
        }
        MobEffectInstance instance = matchingSlowdownEffect(player, item);
        if (instance == null) {
            return baseTicks;
        }
        int percent = 50 + (instance.getAmplifier() + 1) * 10;
        float remaining = Math.max(0.25F, 1.0F - percent / 100.0F);
        return Math.min(baseTicks * 4, Math.round(baseTicks / remaining));
    }

    /**
     * 摆放菜肴两口之间的同口味冷却放大：吃第 1 口就会挂上油腻/爽口，
     * 因此采用更温和的 +25%/级（上限 2.0×：爽口 I 为 1.25×、油腻 II 为 1.50×），避免吃单盘菜耗时过长。
     */
    public static int applyPlacedFlavorCooldown(Player player, Item item, int baseTicks) {
        if (baseTicks <= 0) {
            return 0;
        }
        MobEffectInstance instance = matchingSlowdownEffect(player, item);
        if (instance == null) {
            return baseTicks;
        }
        int level = instance.getAmplifier() + 1;
        float multiplier = Math.min(2.0F, 1.0F + 0.25F * level);
        return Math.round(baseTicks * multiplier);
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (appendMysteryStew(event)) {
            return;
        }
        if (stack.is(ModItems.GRILL_RACK.get())) {
            event.getToolTip().add(Component.translatable("tooltip.northeast_china_delight.grill_rack")
                    .withStyle(ChatFormatting.GRAY));
        }
        // 海水桶：只为旧存档留着，还能拿去厨锅煮盐（2026-11-04 用户）
        if (stack.is(ModItems.SEA_WATER_BUCKET.get())) {
            event.getToolTip().add(Component.translatable("tooltip.northeast_china_delight.sea_water_bucket")
                    .withStyle(ChatFormatting.GRAY));
        }
        if (SeasoningBottleItem.isBottle(stack)) {
            event.getToolTip().add(Component.translatable("tooltip.northeast_china_delight.bottle_amount",
                            SeasoningBottleItem.remainingMb(stack),
                            SeasoningBottleItem.CAPACITY_MB)
                    .withStyle(ChatFormatting.GRAY));
        }
        if (stack.is(ModItems.DEEP_FRY_OILS)) {
            event.getToolTip().add(Component.translatable("tooltip.northeast_china_delight.deep_fry_oil")
                    .withStyle(ChatFormatting.GRAY));
        }
        DishFlavors.Info info = DishFlavors.of(stack.getItem());
        boolean cannedYellowPeach = stack.is(ModItems.CANNED_YELLOW_PEACH.get());
        if (info == null && !cannedYellowPeach) {
            return;
        }
        List<DishEffects.Applied> effects = new ArrayList<>();
        if (info != null) {
            effects.addAll(DishEffects.of(info));
            int warmth = DishFlavors.warmthTicks(stack.getItem(), info);
            if (warmth > 0) {
                effects.addAll(DishEffects.warmth(warmth));
            }
            if (givesNourishing(stack)) {
                effects.addAll(DishEffects.nourishingExtras());
            }
        }
        if (stack.is(ModItems.ORANGE_GUO_BAO_ROU.get())) {
            effects.addAll(DishEffects.orangeJuiceExtras());
        }
        if (cannedYellowPeach) {
            effects.addAll(DishEffects.cannedYellowPeachExtras());
        }
        if (effects.isEmpty()) {
            return;
        }

        List<Component> tooltip = event.getToolTip();
        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("tooltip.northeast_china_delight.dish_effects")
                .withStyle(ChatFormatting.GRAY));
        for (DishEffects.Applied applied : effects) {
            tooltip.add(effectLine(applied.instance()));
        }
    }

    /**
     * 谜之炖菜（盆装谜之炖菜、拿碗盛出来的原版谜之炖菜）：只给创造模式的玩家看效果，同样写在「食用后获得：」下面。
     * 原版只在创造模式物品栏的物品列表里列效果（TooltipFlag#isCreative），拿到自己背包里就看不见了
     * （2026-11-05 用户：创造模式里看不到盛出来的炖菜的效果），所以这里按玩家是不是创造模式。
     * 返回 true 表示是谜之炖菜（别的提示不用再看）
     */
    private static boolean appendMysteryStew(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        boolean basin = stack.is(ModItems.MYSTERY_STEW.get());
        if (!basin && !stack.is(Items.SUSPICIOUS_STEW)) {
            return false;
        }
        Player player = event.getEntity();
        // 原版谜之炖菜在创造模式物品列表里原版自己会列，不重复
        if (player == null || !player.isCreative() || !basin && event.getFlags().isCreative()) {
            return true;
        }
        List<MysteryStewEffects.Entry> effects = basin ? MysteryStewEffects.read(stack) : MysteryStewEffects.readBowl(stack);
        if (effects.isEmpty()) {
            return true;
        }
        List<Component> tooltip = event.getToolTip();
        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("tooltip.northeast_china_delight.dish_effects")
                .withStyle(ChatFormatting.GRAY));
        for (MysteryStewEffects.Entry entry : effects) {
            tooltip.add(effectLine(DdEffects.instance(entry.effect(), entry.duration(), 0)));
        }
        return true;
    }

    /** 「食用后获得：」下面的一行：效果名（等级）（时长）；不到 1 秒的不写时长（同原版药水提示，谜之炖菜的饱和只有 7 刻） */
    private static Component effectLine(MobEffectInstance instance) {
        Component name = Component.translatable(instance.getDescriptionId());
        if (instance.getAmplifier() > 0) {
            name = Component.translatable("potion.withAmplifier", name,
                    Component.translatable("potion.potency." + instance.getAmplifier()));
        }
        name = name.copy().withStyle(DdEffects.category(instance).getTooltipFormatting());
        net.minecraft.network.chat.MutableComponent line = Component.literal(" ").append(name);
        if (instance.getDuration() > 20) {
            line = line.append(" (")
                    //? if <1.20.2 {
                    .append(MobEffectUtil.formatDuration(instance, 1.0F))
                    //?} else {
                    /*.append(MobEffectUtil.formatDuration(instance, 1.0F, 20.0F))
                    *///?}
                    .append(")");
        }
        return line.withStyle(ChatFormatting.GRAY);
    }

    @SubscribeEvent
    public static void onStartUsingItem(LivingEntityUseItemEvent.Start event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        int duration = applyFlavorSlowdown(player, event.getItem().getItem(), event.getDuration());
        if (duration != event.getDuration()) {
            event.setDuration(duration);
        }
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

        MobEffectInstance greasy = DdEffects.getEffect(player, CrciMobEffects.GREASY);
        float exhaustion = food.getExhaustionLevel();
        Float last = LAST_EXHAUSTION.put(id, exhaustion);
        if (greasy != null && last != null && exhaustion > last) {
            int percent = 50 + (greasy.getAmplifier() + 1) * 10;
            float keep = Math.max(0.1F, 1.0F - percent / 100.0F);
            float adjusted = last + (exhaustion - last) * keep;
            food.setExhaustion(adjusted);
            LAST_EXHAUSTION.put(id, adjusted);
        }

        MobEffectInstance refreshing = DdEffects.getEffect(player, CrciMobEffects.REFRESHING);
        applyWarmthSpeed(player);
        if (refreshing == null || !player.isAlive()
                || !player.level().getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_NATURAL_REGENERATION)
                || player.getHealth() >= player.getMaxHealth() || food.getFoodLevel() < 18) {
            REGEN_CREDIT.remove(id);
            return;
        }
        int level = refreshing.getAmplifier() + 1;
        float saturation = food.getSaturationLevel();
        float percent = (20.0F + saturation * level) / 100.0F;
        float baseRate = food.getFoodLevel() >= 20 && saturation > 0 ? 0.1F : 1.0F / 80.0F;
        float credit = REGEN_CREDIT.getOrDefault(id, 0.0F) + baseRate * percent;
        boolean healed = false;
        while (credit >= 1.0F && player.getHealth() < player.getMaxHealth()) {
            player.heal(1.0F);
            food.addExhaustion(6.0F);
            credit -= 1.0F;
            healed = true;
        }
        if (healed) {
            LAST_EXHAUSTION.put(id, food.getExhaustionLevel());
        }
        REGEN_CREDIT.put(id, credit);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID id = event.getEntity().getUUID();
        LAST_EXHAUSTION.remove(id);
        REGEN_CREDIT.remove(id);
        PlacedDishEating.clearCooldown(id);
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
        MobEffectInstance warmth = DdEffects.getEffect(player, CrciMobEffects.WARMTH);
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
