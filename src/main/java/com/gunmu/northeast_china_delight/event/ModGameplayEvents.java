package com.gunmu.northeast_china_delight.event;

import com.gunmu.northeast_china_delight.NortheastChinaDelight;
import com.gunmu.northeast_china_delight.advancement.ModAdvancements;
import com.gunmu.northeast_china_delight.block.GinsengCropBlock;
import com.gunmu.northeast_china_delight.block.ModBlocks;
import com.gunmu.northeast_china_delight.block.PlatedDishBlock;
import com.gunmu.northeast_china_delight.compat.displaydelight.DisplayDelightCompat;
import com.gunmu.northeast_china_delight.NortheastChinaConfig;
import com.gunmu.northeast_china_delight.block.WoodenMortarBlock;
import com.gunmu.northeast_china_delight.item.ModItems;
import com.gunmu.northeast_china_delight.item.SeaWaterBucketItem;
import com.gunmu.northeast_china_delight.item.SeaWaterBuckets;
import com.gunmu.northeast_china_delight.util.DdIds;
import com.gunmu.northeast_china_delight.worldgen.CourtyardPiece;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import com.gunmu.northeast_china_delight.util.DdStacks;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.stats.Stats;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.sounds.SoundSource;
//? if <1.20.2 {
/*import net.minecraftforge.event.entity.player.ItemTooltipEvent;
*///?} else {
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
//?}
//? if <1.20.2 {
/*import net.minecraftforge.eventbus.api.SubscribeEvent;
*///?} else {
import net.neoforged.bus.api.SubscribeEvent;
//?}
//? if <1.20.2 {
/*import net.minecraftforge.event.entity.player.PlayerInteractEvent;
*///?} else {
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
//?}
//? if <1.20.2 {
/*import net.minecraftforge.event.furnace.FurnaceFuelBurnTimeEvent;
*///?}
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
//? if <1.20.2 {
/*import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
*///?} else {
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
//?}
//? if <1.20.2 {
/*import net.minecraftforge.event.TickEvent;
*///?} else {
import net.neoforged.neoforge.event.tick.ServerTickEvent;
//?}

/**
 * 四个和小玩法有关的钩子：
 *
 * <ul>
 *   <li><b>木耳</b>：给原木去皮的那一刻按树种／生物群系掷骰子（黑森林的深色橡木、白桦林的白桦 7%，
 *       橡木 5%，针叶树不产）。去皮本身就是「第一次」，已经去过的原木不会再触发；</li>
 *   <li><b>人参</b>：空手刨开成熟人参脚下的土，才会掉出人参和参籽；</li>
 *   <li><b>海水桶</b>：空桶在干净的海洋水柱里取水时，把原版给的水桶换成海水桶；</li>
 *   <li><b>挨户串门</b>：每秒看一次玩家脚下的小院是哪一种院子（见 {@link #awardYardVisits}）。</li>
 * </ul>
 */
public final class ModGameplayEvents
{
    /** 「挨户串门」每隔多少 tick 查一次玩家脚下的小院（20 tick = 1 秒） */
    public static final int YARD_CHECK_INTERVAL = 20;

    /** 小院群落的注册名，见 data/northeast_china_delight/worldgen/structure/northeast_yard.json */
    private static final ResourceLocation YARD_STRUCTURE =
            DdIds.of(NortheastChinaDelight.MODID, "northeast_yard");

    private ModGameplayEvents()
    {
    }

    /**
     * 左键敲木臼 = 舂捣一次。木臼里还有料时顺手把这一下拦下来，别把臼当方块拆了
     * （{@code Block#attack} 只在客户端跑，改方块必须挂在服务端的左键事件上）。
     *
     * <p>只认 {@code Action.START}：一次左键会按 START（按下）、ABORT（松手）、STOP（挖穿）分别触发事件，不过滤的话一次点击会敲两下（掉 2 点耐久、进度走两步）。</p>
     *
     * <p>还得攒够当前配方要求的蓄力才能响这一下（连点没用）；旧配方默认仍是 90%。
     * 敲成之后手动把挥击计数器归零 —— 原版只在客户端重置它，服务端那个只增不减，
     * 不归零的话蓄力门槛从第二下起就永远满足，等于没做。</p>
     */
    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getAction() != PlayerInteractEvent.LeftClickBlock.Action.START) {
            return;
        }
        if (event.getLevel().isClientSide()) {
            return;
        }
        Player player = event.getEntity();
        if (WoodenMortarBlock.pound(player, event.getLevel(), event.getPos())) {
            player.resetAttackStrengthTicker();
            event.setCanceled(true);
        }
    }

    /**
     * 「你瞅啥？」：头戴大脸盆、手持木槌的玩家被末影人锁定（盯着它看把它惹毛）时发进度。
     *
     * <p>末影人被激怒走的是 {@code EnderMan#setTarget}，而 {@code Mob#setTarget} 上挂着平台事件，
     * 不用 mixin 就能拿到「目标要变成谁」。NeoForge 1.21.1 把这两个访问器改了名
     * （{@code getNewTarget} → {@code getNewAboutToBeSetTarget}），差异收在下面的条件编译里。</p>
     */
    @SubscribeEvent
    public static void onLivingChangeTarget(LivingChangeTargetEvent event) {
        if (event.getEntity().level().isClientSide() || !(event.getEntity() instanceof EnderMan)) {
            return;
        }
        //? if <1.20.2 {
        /*LivingEntity target = event.getNewTarget();
        *///?} else {
        LivingEntity target = event.getNewAboutToBeSetTarget();
        //?}
        if (!(target instanceof ServerPlayer player)) {
            return;
        }
        if (!player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.LARGE_BASIN.get())) {
            return;
        }
        if (!player.getMainHandItem().is(ModItems.WOODEN_MALLET.get())) {
            return;
        }
        ModAdvancements.award(player, ModAdvancements.STARE, "stare");
    }

    /**
     * 「挨户串门」的驱动：进过所有种类的东北小院之后发进度。
     *
     * <p>这件事光靠数据包做不到：{@code minecraft:location} 触发器只知道「玩家站在哪个结构里」，
     * 而菜园、四合院、牲口院三种院子共用同一个结构 id，分不出站的是哪一种。所以这里每秒查一次
     * 玩家脚下的小院 —— 结构里的每个 {@link CourtyardPiece} 记着自己用的是哪个 NBT 模板，
     * 找到罩住玩家的那一块，把模板名换算成判据名（{@link ModAdvancements#yardCriterion}）
     * 判给玩家；判据一览见 {@code data/northeast_china_delight/advancement/visit_all_yards.json}，
     * 那边漏写了哪个模板，这里就安静地跳过，不会报错。</p>
     *
     * <p>进度自己会把同一条判据去重，所以每秒重复判也没有副作用。</p>
     */
    //? if <1.20.2 {
    /*@SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event)
    {
        // 1.20.1 的 Forge 只发一对 TickEvent，START / END 各来一次，只认 END（服务器跑完这一 tick）
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        awardYardVisits(event.getServer());
    }
    *///?} else {
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event)
    {
        awardYardVisits(event.getServer());
    }
    //?}

    private static void awardYardVisits(MinecraftServer server)
    {
        if (server.getTickCount() % YARD_CHECK_INTERVAL != 0) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            awardYardVisit(player);
        }
    }

    private static void awardYardVisit(ServerPlayer player)
    {
        ServerLevel level = player.serverLevel();
        Structure structure = level.registryAccess()
                .registryOrThrow(Registries.STRUCTURE)
                .get(ResourceKey.create(Registries.STRUCTURE, YARD_STRUCTURE));
        if (structure == null) {
            return;
        }
        BlockPos pos = player.blockPosition();
        StructureStart start = level.structureManager().getStructureAt(pos, structure);
        if (!start.isValid()) {
            return;
        }
        for (StructurePiece piece : start.getPieces()) {
            if (piece instanceof CourtyardPiece courtyard && piece.getBoundingBox().isInside(pos)) {
                ModAdvancements.award(player, ModAdvancements.VISIT_ALL_YARDS,
                        ModAdvancements.yardCriterion(courtyard.templateId()));
            }
        }
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
        int mode = NortheastChinaConfig.SEA_WATER_BUCKET_MODE.get();
        if (mode == SeaWaterBuckets.VANILLA_WATER_BUCKET)
        {
            return;
        }
        // 取水用的必须是空桶（旧代码判断的是「水桶」，那个条件永远不成立）
        ItemStack stack = event.getEmptyBucket();
        if (!stack.is(Items.BUCKET) || !(event.getTarget() instanceof BlockHitResult hit))
        {
            return;
        }
        Level level = event.getLevel();
        Player player = event.getEntity();
        BlockPos pos = hit.getBlockPos();
        if (!canPickSeaWater(player, level, pos, hit, stack))
        {
            return;
        }
        ItemStack picked = pickUpOceanWater(player, level, pos);
        if (picked.isEmpty())
        {
            return;
        }
        // 「扣掉空桶、把新桶塞回玩家手里」交给原版事件流程
        event.setFilledBucket(SeaWaterBuckets.fromOceanPickup(picked, mode));
        event.setResult(Event.Result.ALLOW);
    }
    *///?} else {
    @SubscribeEvent
    public static void onWaterBucketPickup(PlayerInteractEvent.RightClickItem event)
    {
        int mode = NortheastChinaConfig.SEA_WATER_BUCKET_MODE.get();
        if (mode == SeaWaterBuckets.VANILLA_WATER_BUCKET)
        {
            return;
        }
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
            return;
        }
        ItemStack picked = pickUpOceanWater(player, level, pos);
        if (picked.isEmpty())
        {
            return;
        }
        ItemStack seaBucket = ItemUtils.createFilledResult(
                stack, player, SeaWaterBuckets.fromOceanPickup(picked, mode));
        player.setItemInHand(event.getHand(), seaBucket);
        if (!player.isUsingItem() && player instanceof ServerPlayer serverPlayer)
        {
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

    private static ItemStack pickUpOceanWater(Player player, Level level, BlockPos pos)
    {
        if (level.isClientSide())
        {
            return new ItemStack(Items.WATER_BUCKET);
        }
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof BucketPickup pickup))
        {
            return ItemStack.EMPTY;
        }
        ItemStack picked = pickupFrom(pickup, player, level, pos, state);
        if (picked.isEmpty())
        {
            return ItemStack.EMPTY;
        }
        player.awardStat(Stats.ITEM_USED.get(Items.BUCKET));
        pickup.getPickupSound(state).ifPresent(sound -> player.playSound(sound, 1.0F, 1.0F));
        level.gameEvent(player, GameEvent.FLUID_PICKUP, pos);
        if (player instanceof ServerPlayer serverPlayer)
        {
            CriteriaTriggers.FILLED_BUCKET.trigger(serverPlayer, picked);
        }
        return picked;
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

    @SubscribeEvent
    public static void onMarkedWaterBucketTooltip(ItemTooltipEvent event)
    {
        if (SeaWaterBuckets.isMarkedWaterBucket(event.getItemStack()))
        {
            event.getToolTip().add(Component.translatable("tooltip.northeast_china_delight.sea_water_marker")
                    .withStyle(ChatFormatting.GRAY));
        }
    }

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
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        String id = itemId.getPath();
        var dishBlock = itemId.getNamespace().equals(NortheastChinaDelight.MODID)
                ? ModBlocks.DISH_BLOCKS.get(id) : null;
        if (dishBlock == null)
        {
            return;
        }

        Player player = event.getEntity();
        if (NortheastChinaConfig.DISH_PLACEMENT_ENABLED.get()
                && dishBlock.get() instanceof PlatedDishBlock plated)
        {
            InteractionResult onPlate = placeOnPlate(plated, event, stack, player);
            if (onPlate != InteractionResult.PASS)
            {
                event.setCanceled(true);
                event.setCancellationResult(onPlate);
                return;
            }
        }
        if (player.isSecondaryUseActive() && NortheastChinaConfig.DISH_PLACEMENT_ENABLED.get()
                && ModBlocks.canPlaceDish(id))
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
     * 无器具菜肴：右键展示乐事的餐盘 / 小餐盘，把盘子换成摆好菜的方块（潜行与否都行）。
     * 点的是另一种盘子就提示该摆哪种；点的不是展示乐事的盘子返回 PASS，照常走后面的吃。
     */
    private static InteractionResult placeOnPlate(PlatedDishBlock dish, PlayerInteractEvent.RightClickBlock event,
                                                  ItemStack stack, Player player)
    {
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        ResourceLocation clicked = BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock());
        if (!clicked.equals(DisplayDelightCompat.PLATE) && !clicked.equals(DisplayDelightCompat.SMALL_PLATE))
        {
            return InteractionResult.PASS;
        }
        if (!clicked.equals(dish.plate()))
        {
            if (!level.isClientSide)
            {
                Block wanted = DisplayDelightCompat.plateBlock(dish.plate());
                player.displayClientMessage(Component.translatable("message.northeast_china_delight.dish.needs_plate",
                        wanted == null ? Component.literal(dish.plate().toString()) : wanted.getName()), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!level.isClientSide)
        {
            BlockState state = dish.defaultBlockState();
            level.setBlock(pos, state, 3);
            SoundType sound = state.getSoundType(level, pos, player);
            level.playSound(null, pos, sound.getPlaceSound(), SoundSource.BLOCKS,
                    (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
            level.gameEvent(GameEvent.BLOCK_PLACE, pos, GameEvent.Context.of(player, state));
            DdStacks.consume(stack, 1, player);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
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
