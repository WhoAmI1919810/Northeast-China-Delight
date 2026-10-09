package com.gunmu.northeast_china_delight.event;

import com.gunmu.northeast_china_delight.block.GinsengCropBlock;
import com.gunmu.northeast_china_delight.block.ModBlocks;
import com.gunmu.northeast_china_delight.NortheastChinaConfig;
import com.gunmu.northeast_china_delight.item.ModItems;
import com.gunmu.northeast_china_delight.item.SeaWaterBucketItem;
import com.gunmu.northeast_china_delight.util.DdStacks;
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
import net.minecraft.world.level.Level;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.sounds.SoundSource;
//? if <1.20.2 {
/*import net.minecraftforge.eventbus.api.SubscribeEvent;
*///?} else {
import net.neoforged.bus.api.SubscribeEvent;
//?}
//? if <1.20.2 {
/*import net.minecraftforge.event.furnace.FurnaceFuelBurnTimeEvent;
*///?}
//? if >=1.20.5 {
import net.neoforged.neoforge.common.ItemAbilities;
//?}
//? if <1.20.2 {
/*import net.minecraftforge.event.level.BlockEvent;
*///?} else {
import net.neoforged.neoforge.event.level.BlockEvent;
//?}
//? if <1.20.2 {
/*import net.minecraftforge.event.entity.player.PlayerInteractEvent;
*///?} else {
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
//?}
//? if <1.20.2 {
/*import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.event.entity.player.FillBucketEvent;
*///?} else if <1.20.5 {
/*import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.event.entity.player.FillBucketEvent;
*///?}
//? if >=1.20.5 {
import net.minecraft.world.phys.HitResult;
//?}

/**
 * 三个和小玩法有关的钩子：
 *
 * <ul>
 *   <li><b>木耳</b>：给原木去皮的那一刻按树种／生物群系掷骰子（黑森林的深色橡木、白桦林的白桦 7%，
 *       橡木 5%，针叶树不产）。去皮本身就是「第一次」，已经去过的原木不会再触发；</li>
 *   <li><b>人参</b>：空手刨开成熟人参脚下的土，才会掉出人参和参籽；</li>
 *   <li><b>海水桶</b>：空桶在干净的海洋水柱里取水时，把原版给的水桶换成海水桶。</li>
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

    /**
     * 空桶在「干净的海洋水柱」里取水时，把原版给的水桶换成海水桶。
     *
     * <p>触发点按版本分成两套：</p>
     * <ul>
     *   <li>1.20.1：用 {@code FillBucketEvent}（Forge 47 里还在），
     *       直接换掉「桶里舀到的战利品」，扣空桶、进背包这些原版流程照旧；</li>
     *   <li>1.21.1：NeoForge 21.1 删掉了 {@code FillBucketEvent}，而水方块没有准星命中框，
     *       {@code RightClickBlock} / {@code UseItemOnBlockEvent} 对着水都不会触发 —— 这正是
     *       之前海水桶怎么舀都出不来的原因。原版空桶取水走的是 {@code useItem} 那条路
     *       （{@code BucketItem.use} 自己再做一次视线检测），所以这里改挂 {@code RightClickItem}，
     *       并复刻原版那次检测。</li>
     * </ul>
     */
    //? if <1.20.5 {
    /*@SubscribeEvent
    public static void onWaterBucketPickup(FillBucketEvent event)
    {
        // 取水用的必须是空桶（旧代码判断的是「水桶」，那个条件永远不成立）
        ItemStack stack = event.getEmptyBucket();
        if (!stack.is(Items.BUCKET) || !(event.getTarget() instanceof BlockHitResult hit))
        {
            return;
        }
        Level level = event.getLevel();
        Player player = event.getEntity();
        BlockPos pos = hit.getBlockPos();
        if (!canPickSeaWater(player, level, pos, hit, stack) || !pickUpOceanWater(player, level, pos))
        {
            return;
        }
        // 「扣掉空桶、把新桶塞回玩家手里」交给原版事件流程
        event.setFilledBucket(new ItemStack(ModItems.SEA_WATER_BUCKET.get()));
        event.setResult(Event.Result.ALLOW);
    }
    *///?} else {
    @SubscribeEvent
    public static void onWaterBucketPickup(PlayerInteractEvent.RightClickItem event)
    {
        ItemStack stack = event.getItemStack();
        if (!stack.is(Items.BUCKET))
        {
            return;
        }
        Player player = event.getEntity();
        Level level = player.level();
        BlockHitResult hit = SeaWaterBucketItem.raycastSourceFluid(level, player);
        if (hit.getType() != HitResult.Type.BLOCK)
        {
            return;
        }
        BlockPos pos = hit.getBlockPos();
        if (!canPickSeaWater(player, level, pos, hit, stack))
        {
            return;
        }

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (level.isClientSide())
        {
            // 客户端只负责取消（别预测成普通水桶），动手都在服务端
            return;
        }
        if (!pickUpOceanWater(player, level, pos))
        {
            return;
        }
        ItemStack seaBucket = ItemUtils.createFilledResult(
                stack, player, new ItemStack(ModItems.SEA_WATER_BUCKET.get()));
        player.setItemInHand(event.getHand(), seaBucket);
        if (!player.isUsingItem() && player instanceof ServerPlayer serverPlayer)
        {
            // 事件被取消后不会再走原版 useItem 的同步，这里补上
            serverPlayer.inventoryMenu.sendAllDataToRemote();
        }
    }
    //?}

    /** 这一格是不是「干净的海洋水柱」里的水源（空桶一舀就能舀起来的那种）。 */
    private static boolean canPickSeaWater(Player player, Level level, BlockPos pos,
            BlockHitResult hit, ItemStack stack)
    {
        BlockState state = level.getBlockState(pos);
        if (!state.getFluidState().is(FluidTags.WATER) || !state.getFluidState().isSource()
                || !(state.getBlock() instanceof BucketPickup)
                || !level.mayInteract(player, pos)
                || !player.mayUseItemAt(pos.relative(hit.getDirection()), hit.getDirection(), stack))
        {
            return false;
        }
        return SeaWaterBucketItem.isValidOceanColumn(level, pos);
    }

    /**
     * 服务端：把水源舀走，并补上原版取水会有的那一串反馈（统计 / 音效 / 装桶的进度条件）。
     * 客户端不做世界改动，直接算成功（这里只用于预测）。
     */
    private static boolean pickUpOceanWater(Player player, Level level, BlockPos pos)
    {
        if (level.isClientSide())
        {
            return true;
        }
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof BucketPickup pickup))
        {
            return false;
        }
        ItemStack picked = pickupFrom(pickup, player, level, pos, state);
        if (picked.isEmpty())
        {
            return false;
        }
        player.awardStat(Stats.ITEM_USED.get(Items.BUCKET));
        pickup.getPickupSound(state).ifPresent(sound -> player.playSound(sound, 1.0F, 1.0F));
        level.gameEvent(player, GameEvent.FLUID_PICKUP, pos);
        if (player instanceof ServerPlayer serverPlayer)
        {
            CriteriaTriggers.FILLED_BUCKET.trigger(serverPlayer, picked);
        }
        return true;
    }

    /** 1.20.1 的 {@code BucketPickup#pickupBlock} 还没有 player 参数，这里抹平差异。 */
    private static ItemStack pickupFrom(BucketPickup pickup, Player player, Level level, BlockPos pos, BlockState state)
    {
        //? if <1.20.2 {
        /*return pickup.pickupBlock(level, pos, state);
        *///?} else {
        return pickup.pickupBlock(player, level, pos, state);
        //?}
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
        if (DdStacks.isEdible(stack))
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
            DdStacks.consume(stack, 1, player);
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }

    /** 斧头去皮这个「工具行为」在两个版本里分属两个类：1.20.1 在 ToolActions，1.21 起在 ItemAbilities */
    private static boolean isAxeStrip(BlockEvent.BlockToolModificationEvent event)
    {
        //? if <1.20.2 {
        /*return event.getToolAction() == net.minecraftforge.common.ToolActions.AXE_STRIP;
        *///?} else if <1.20.5 {
        /*return event.getToolAction() == net.neoforged.neoforge.common.ToolActions.AXE_STRIP;*/
        //?} else {
        return event.getItemAbility() == ItemAbilities.AXE_STRIP;
        //?}
    }

    @SubscribeEvent
    public static void onToolModification(BlockEvent.BlockToolModificationEvent event)
    {
        if (event.isSimulated() || !isAxeStrip(event))
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

    /**
     * 1.20.1 没有数据表（data map）：熔炉 / 烟熏炉 / 高炉 / 机械动力烈焰燃烧器的燃料时长
     * 只能在事件里给。1.20.5+ 走 {@code data/neoforge/data_maps/item/furnace_fuels.json}，
     * 这一段在那些版本上不编译。
     */
    //? if <1.20.2 {
    /*@SubscribeEvent
    public static void onFurnaceFuel(FurnaceFuelBurnTimeEvent event)
    {
        ItemStack stack = event.getItemStack();
        if (stack.is(ModItems.CORN_STALK.get()))
        {
            event.setBurnTime(200);
        }
        else if (stack.is(ModItems.ANIMAL_OIL.get()))
        {
            event.setBurnTime(800);
        }
        else if (stack.is(ModItems.ANIMAL_OIL_BLOCK_ITEM.get()))
        {
            event.setBurnTime(3200);
        }
    }
    *///?}
}
