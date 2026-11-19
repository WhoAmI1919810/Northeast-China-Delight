package com.gunmu.northeast_china_delight.block;

import com.gunmu.northeast_china_delight.NortheastChinaConfig;
import com.gunmu.northeast_china_delight.effect.DishEffectEvents;
import com.gunmu.northeast_china_delight.effect.DishFlavors;
import com.gunmu.northeast_china_delight.util.DdIds;
import com.gunmu.northeast_china_delight.util.DdPlayers;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
//? if >=1.20.5 {
/*import net.minecraft.core.component.DataComponents;
*///?}
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * 摆放菜肴的分阶段进食逻辑与个人独立进食冷却。
 *
 * <p>每位玩家独立记录上次夹菜的时间戳，多人同桌合餐互不干扰；每次点击按「当前口味效果 + 眼前菜肴口味」动态算冷却：
 * 已有油腻时吃油腻菜、已有爽口时吃爽口菜会延长冷却，换口味或吃均衡/暖身菜则按基础冷却结算。</p>
 */
public final class PlacedDishEating
{
    private static final String CRCI_NAMESPACE = "chinese_regional_culinary_ingredients";
    private static final float MULTI_BITE_SATURATION_MULTIPLIER = 1.10F;
    private static final Map<UUID, Long> LAST_BITE_TICK = new HashMap<>();

    private PlacedDishEating()
    {
    }

    public static void clearCooldown(UUID playerId)
    {
        LAST_BITE_TICK.remove(playerId);
    }

    /** 菜肴方块与对应食物物品同名；拆分版中豆浆等共用物品在 CRCI 命名空间下，查不到时顺手回退一次。 */
    public static Item dishItemFor(Block block)
    {
        ResourceLocation key = BuiltInRegistries.BLOCK.getKey(block);
        Item item = BuiltInRegistries.ITEM.get(key);
        if (item == Items.AIR)
        {
            item = BuiltInRegistries.ITEM.get(DdIds.of(CRCI_NAMESPACE, key.getPath()));
        }
        return item;
    }

    /** 饱食度总量守恒：均分向下取整，余数依次分摊到后面的口次（最后一口最扎实）。 */
    public static int biteNutrition(int fullNutrition, int currentBites, int totalBites)
    {
        if (totalBites <= 1)
        {
            return fullNutrition;
        }
        int base = fullNutrition / totalBites;
        int remainder = fullNutrition % totalBites;
        int extraStart = totalBites - remainder;
        return base + (currentBites >= extraStart ? 1 : 0);
    }

    public static void giveContainer(Player player, Item container)
    {
        if (container == null || container == Items.AIR || DdPlayers.hasInfiniteMaterials(player))
        {
            return;
        }
        ItemStack stack = new ItemStack(container);
        if (!player.getInventory().add(stack))
        {
            player.drop(stack, false);
        }
    }

    public static InteractionResult tryEat(BlockState state, Level level, BlockPos pos, Player player,
                                           int currentBites, int totalBites, Runnable advanceBlock)
    {
        if (player.isSecondaryUseActive() || !player.getMainHandItem().isEmpty())
        {
            return InteractionResult.PASS;
        }
        Item dishItem = dishItemFor(state.getBlock());
        if (dishItem == Items.AIR)
        {
            return InteractionResult.PASS;
        }
        ItemStack dishStack = new ItemStack(dishItem);
        //? if <1.20.5 {
        FoodProperties food = dishStack.getFoodProperties(player);
        //?} else {
        /*FoodProperties food = dishStack.get(DataComponents.FOOD);
        *///?}
        if (food == null || !player.canEat(food.canAlwaysEat()))
        {
            return InteractionResult.PASS;
        }
        if (level.isClientSide)
        {
            return InteractionResult.SUCCESS;
        }

        int baseCooldown = NortheastChinaConfig.dishBiteCooldownTicks();
        long now = level.getGameTime();
        if (baseCooldown > 0)
        {
            Long lastTick = LAST_BITE_TICK.get(player.getUUID());
            if (lastTick != null)
            {
                int required = DishEffectEvents.applyPlacedFlavorCooldown(player, dishItem, baseCooldown);
                long elapsed = now - lastTick;
                if (elapsed >= 0L && elapsed < required)
                {
                    long remainTicks = required - elapsed;
                    String seconds = String.format(Locale.ROOT, "%.1f", remainTicks / 20.0D);
                    player.displayClientMessage(Component.translatable(cooldownMessageKey(player, dishItem), seconds), true);
                    return InteractionResult.CONSUME;
                }
            }
        }

        LAST_BITE_TICK.put(player.getUUID(), now);

        //? if <1.20.5 {
        int fullNutrition = food.getNutrition();
        float fullSaturationPoints = fullNutrition * food.getSaturationModifier() * 2.0F;
        //?} else {
        /*int fullNutrition = food.nutrition();
        float fullSaturationPoints = food.saturation();
        *///?}
        int biteNutrition = biteNutrition(fullNutrition, currentBites, totalBites);
        float satMultiplier = totalBites > 1 ? MULTI_BITE_SATURATION_MULTIPLIER : 1.0F;
        float biteSaturationPoints = (fullSaturationPoints * satMultiplier) / Math.max(1, totalBites);
        if (biteNutrition > 0)
        {
            player.getFoodData().eat(biteNutrition, biteSaturationPoints / (biteNutrition * 2.0F));
        }

        DishEffectEvents.applyPlacedDishEffects(player, dishItem, totalBites);

        level.playSound(null, pos, SoundEvents.GENERIC_EAT, SoundSource.PLAYERS,
                0.8F, 0.9F + level.random.nextFloat() * 0.2F);
        if (level instanceof ServerLevel server)
        {
            server.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, dishStack),
                    pos.getX() + 0.5D, pos.getY() + 0.25D, pos.getZ() + 0.5D,
                    6, 0.18D, 0.08D, 0.18D, 0.04D);
        }
        level.gameEvent(player, GameEvent.EAT, pos);
        if (player instanceof ServerPlayer serverPlayer)
        {
            CriteriaTriggers.CONSUME_ITEM.trigger(serverPlayer, dishStack);
        }

        advanceBlock.run();
        return InteractionResult.CONSUME;
    }

    private static String cooldownMessageKey(Player player, Item dishItem)
    {
        MobEffectInstance slow = DishEffectEvents.matchingSlowdownEffect(player, dishItem);
        if (slow != null)
        {
            DishFlavors.Flavor flavor = DishFlavors.flavorOf(dishItem);
            if (flavor == DishFlavors.Flavor.GREASY)
            {
                return "message.northeast_china_delight.dish.cooldown_greasy";
            }
            if (flavor == DishFlavors.Flavor.REFRESHING)
            {
                return "message.northeast_china_delight.dish.cooldown_refreshing";
            }
        }
        return "message.northeast_china_delight.dish.cooldown";
    }
}
