package com.gunmu.northeast_china_delight.event;

import com.gunmu.northeast_china_delight.block.GinsengCropBlock;
import com.gunmu.northeast_china_delight.block.ModBlocks;
import com.gunmu.northeast_china_delight.NortheastChinaConfig;
import com.gunmu.northeast_china_delight.item.ModItems;
import com.gunmu.northeast_china_delight.item.SeaWaterBucketItem;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.stats.Stats;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.sounds.SoundSource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * 两个和小玩法有关的钩子：
 *
 * <ul>
 *   <li><b>木耳</b>：给原木去皮的那一刻按树种／生物群系掷骰子（黑森林的深色橡木、白桦林的白桦 7%，
 *       橡木 5%，针叶树不产）。去皮本身就是「第一次」，已经去过的原木不会再触发；</li>
 *   <li><b>人参</b>：空手刨开成熟人参脚下的土，才会掉出人参和参籽。</li>
 * </ul>
 */
public final class ModGameplayEvents
{
    /** 黑森林／白桦林的去皮出木耳概率 */
    public static final float WOOD_EAR_FOREST_CHANCE = 0.07F;
    /** 橡木的去皮出木耳概率 */
    public static final float WOOD_EAR_OAK_CHANCE = 0.05F;

    private ModGameplayEvents()
    {
    }

    @SubscribeEvent
    public static void onWaterBucketPickup(PlayerInteractEvent.RightClickBlock event)
    {
        ItemStack stack = event.getItemStack();
        if (!stack.is(Items.WATER_BUCKET))
        {
            return;
        }
        BlockPos pos = event.getPos();
        BlockState state = event.getLevel().getBlockState(pos);
        if (!state.getFluidState().is(FluidTags.WATER) || !state.getFluidState().isSource()
                || !(state.getBlock() instanceof BucketPickup pickup)
                || !SeaWaterBucketItem.isValidOceanColumn(event.getLevel(), pos))
        {
            return;
        }

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (event.getLevel().isClientSide())
        {
            return;
        }
        ItemStack picked = pickup.pickupBlock(event.getEntity(), event.getLevel(), pos, state);
        if (picked.isEmpty())
        {
            return;
        }
        ItemStack seaBucket = ItemUtils.createFilledResult(
                stack, event.getEntity(), new ItemStack(ModItems.SEA_WATER_BUCKET.get()));
        event.getEntity().setItemInHand(event.getHand(), seaBucket);
        event.getEntity().awardStat(Stats.ITEM_USED.get(Items.WATER_BUCKET));
        pickup.getPickupSound(state).ifPresent(sound -> event.getEntity().playSound(sound, 1.0F, 1.0F));
        event.getLevel().gameEvent(event.getEntity(), GameEvent.FLUID_PICKUP, pos);
        if (event.getEntity() instanceof ServerPlayer serverPlayer)
        {
            CriteriaTriggers.FILLED_BUCKET.trigger(serverPlayer, picked);
        }
    }

    /**
     * 单份料理的唯一交互入口：普通右键直接吃，潜行右键尝试摆放。
     *
     * <p>料理本身仍然是普通 {@link net.minecraft.world.item.Item}，所以不会改变碗、
     * 黄铜碗等 craft remainder。摆放时临时借用 {@link BlockItem} 的原版放置流程，
     * 放置失败则回到吃东西的流程；开关关闭时也只走吃东西，不发送提示文字。</p>
     */
    @SubscribeEvent
    public static void onDishRightClick(PlayerInteractEvent.RightClickBlock event)
    {
        if (event.isCanceled())
        {
            return;
        }
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty() || event.getHand() == InteractionHand.OFF_HAND)
        {
            return;
        }
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        var dishBlock = ModBlocks.DISH_BLOCKS.get(id);
        if (dishBlock == null)
        {
            return;
        }

        Player player = event.getEntity();
        if (player.isSecondaryUseActive() && NortheastChinaConfig.DISH_PLACEMENT_ENABLED.get())
        {
            BlockPlaceContext placement = new BlockPlaceContext(
                    event.getLevel(), player, event.getHand(), stack, event.getHitVec());
            InteractionResult placed = placeDishBlock(dishBlock.get(), placement);
            if (placed.consumesAction())
            {
                event.setCanceled(true);
                event.setCancellationResult(placed);
                return;
            }
        }

        // 非潜行右键，以及配置关闭时的潜行右键：直接走物品的食用逻辑。
        // 这里显式取消方块交互，保证对着箱子/工作台右键也不会出现“放不下”的提示。
        if (stack.has(net.minecraft.core.component.DataComponents.FOOD))
        {
            InteractionResult eaten = stack.getItem()
                    .use(event.getLevel(), player, event.getHand())
                    .getResult();
            if (eaten.consumesAction())
            {
                event.setCanceled(true);
                event.setCancellationResult(eaten);
            }
        }
    }

    /**
     * 原版 BlockItem.place 的最小等价实现。不能在这里 new BlockItem：Item 构造器会尝试
     * 向已冻结的物品注册表创建 intrusive holder，而料理物品本身已经完成注册。
     */
    private static InteractionResult placeDishBlock(Block block, BlockPlaceContext context)
    {
        if (!block.isEnabled(context.getLevel().enabledFeatures()) || !context.canPlace())
        {
            return InteractionResult.FAIL;
        }
        BlockState state = block.getStateForPlacement(context);
        if (state == null || !state.canSurvive(context.getLevel(), context.getClickedPos())
                || !context.getLevel().isUnobstructed(
                        state, context.getClickedPos(), CollisionContext.of(context.getPlayer())))
        {
            return InteractionResult.FAIL;
        }
        if (!context.getLevel().setBlock(context.getClickedPos(), state, 11))
        {
            return InteractionResult.FAIL;
        }

        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        block.setPlacedBy(context.getLevel(), context.getClickedPos(), state, player, stack);
        SoundType sound = state.getSoundType(context.getLevel(), context.getClickedPos(), player);
        context.getLevel().playSound(player, context.getClickedPos(), sound.getPlaceSound(),
                SoundSource.BLOCKS, (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
        context.getLevel().gameEvent(GameEvent.BLOCK_PLACE, context.getClickedPos(),
                GameEvent.Context.of(player, state));
        if (!player.getAbilities().instabuild)
        {
            stack.consume(1, player);
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }

    @SubscribeEvent
    public static void onToolModification(BlockEvent.BlockToolModificationEvent event)
    {
        if (event.isSimulated() || event.getItemAbility() != ItemAbilities.AXE_STRIP)
        {
            return;
        }
        LevelAccessor accessor = event.getLevel();
        if (!(accessor instanceof ServerLevel level))
        {
            return;
        }
        BlockPos pos = event.getPos();
        float chance = woodEarChance(level, pos, event.getState());
        if (chance <= 0.0F || level.random.nextFloat() >= chance)
        {
            return;
        }
        Block.popResource(level, pos, new ItemStack(ModItems.WOOD_EAR.get()));
    }

    /** 这一根原木在这个生物群系里去皮出木耳的概率 */
    private static float woodEarChance(ServerLevel level, BlockPos pos, BlockState logState)
    {
        Holder<Biome> biome = level.getBiome(pos);
        if (logState.is(Blocks.DARK_OAK_LOG) || logState.is(Blocks.DARK_OAK_WOOD))
        {
            return biome.is(Biomes.DARK_FOREST) ? WOOD_EAR_FOREST_CHANCE : 0.0F;
        }
        if (logState.is(Blocks.BIRCH_LOG) || logState.is(Blocks.BIRCH_WOOD))
        {
            return biome.is(Biomes.BIRCH_FOREST) || biome.is(Biomes.OLD_GROWTH_BIRCH_FOREST)
                    ? WOOD_EAR_FOREST_CHANCE : 0.0F;
        }
        if (logState.is(Blocks.OAK_LOG) || logState.is(Blocks.OAK_WOOD))
        {
            return WOOD_EAR_OAK_CHANCE;
        }
        return 0.0F;
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event)
    {
        if (!(event.getLevel() instanceof ServerLevel level))
        {
            return;
        }
        Player player = event.getPlayer();
        // 必须空手：拿着工具刨土只会把参苗一起铲掉
        if (!player.getMainHandItem().isEmpty())
        {
            return;
        }
        BlockPos plantPos = event.getPos().above();
        BlockState plant = level.getBlockState(plantPos);
        if (!plant.is(ModBlocks.GINSENG_CROP.get())
                || plant.getValue(GinsengCropBlock.AGE) < GinsengCropBlock.MAX_AGE)
        {
            return;
        }
        level.removeBlock(plantPos, false);
        Block.popResource(level, plantPos, new ItemStack(ModItems.GINSENG.get()));
        Block.popResource(level, plantPos, new ItemStack(ModItems.GINSENG_SEEDS.get(), 1 + level.random.nextInt(2)));
    }
}
