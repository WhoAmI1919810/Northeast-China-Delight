package com.gunmu.northeast_china_delight.event;


import com.gunmu.chinese_regional_culinary_ingredients.crafting.SeasoningBottleMergeRecipe;
import com.gunmu.chinese_regional_culinary_ingredients.block.CrciBlocks;

import com.gunmu.chinese_regional_culinary_ingredients.item.CrciItems;

import com.gunmu.northeast_china_delight.NortheastChinaDelight;
import com.gunmu.northeast_china_delight.advancement.ModAdvancements;
import com.gunmu.chinese_regional_culinary_ingredients.block.BigStoveBlock;
import com.gunmu.chinese_regional_culinary_ingredients.block.FacingDecorBlock;
import com.gunmu.chinese_regional_culinary_ingredients.block.GinsengCropBlock;
import com.gunmu.chinese_regional_culinary_ingredients.block.CrciBlockTags;
import com.gunmu.northeast_china_delight.block.ModBlocks;
import com.gunmu.chinese_regional_culinary_ingredients.block.PlacedDishBlock;
import com.gunmu.chinese_regional_culinary_ingredients.block.PlatedDishBlock;
import com.gunmu.chinese_regional_culinary_ingredients.compat.displaydelight.DisplayDelightCompat;
import com.gunmu.chinese_regional_culinary_ingredients.block.WoodenMortarBlock;
import com.gunmu.chinese_regional_culinary_ingredients.effect.CrciMobEffects;
import com.gunmu.northeast_china_delight.item.ModItems;
import com.gunmu.chinese_regional_culinary_ingredients.item.SeasoningBottleItem;
import com.gunmu.northeast_china_delight.util.DdEffects;
import com.gunmu.northeast_china_delight.util.DdIds;
import com.gunmu.northeast_china_delight.worldgen.CourtyardPiece;
import net.minecraft.network.chat.Component;
import com.gunmu.northeast_china_delight.util.DdStacks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.sounds.SoundSource;
//? if <1.20.2 {
import net.minecraftforge.eventbus.api.SubscribeEvent;
//?} else {
/*import net.neoforged.bus.api.SubscribeEvent;
*///?}
//? if <1.20.2 {
import net.minecraftforge.event.furnace.FurnaceFuelBurnTimeEvent;
//?}
//? if <1.20.2 {
import net.minecraftforge.event.level.BlockEvent;
//?} else {
/*import net.neoforged.neoforge.event.level.BlockEvent;
*///?}
//? if <1.20.2 {
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
//?} else {
/*import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
*///?}
//? if <1.20.2 {
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.event.GrindstoneEvent;
//?} else {
/*import net.neoforged.neoforge.event.AnvilUpdateEvent;
import net.neoforged.neoforge.event.GrindstoneEvent;
*///?}
//? if <1.20.2 {
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
//?} else {
/*import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
*///?}
//? if <1.20.2 {
import net.minecraftforge.event.TickEvent;
//?} else {
/*import net.neoforged.neoforge.event.tick.ServerTickEvent;
*///?}

public final class ModGameplayEvents
{
    /** 「挨户串门」每隔多少 tick 查一次玩家脚下的小院（20 tick = 1 秒） */
    public static final int YARD_CHECK_INTERVAL = 20;

    public static final int RADIATOR_CHECK_INTERVAL = 80;
    public static final int RADIATOR_DURATION_TICKS = 220;
    public static final double RADIATOR_RANGE = 5.0D;

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
     *
     * <p>拿着工具敲正在加工的木臼时，客户端和服务端都把这一下拦下（2026-11-07 用户报：创造模式舂捣时木臼总显示一个固定的
     * 打糕模型）。创造模式下客户端收到左键会先在本地把方块拆掉（{@code MultiPlayerGameMode#startDestroyBlock}，事件没被拦才拆），
     * 木臼的方块实体跟着没了；服务端回过来的只有方块状态、没有配方，渲染器只好按 blockstate 里自带的打糕模型画。
     * 服务端那边蓄力不够没敲成也照样拦，免得创造模式点一下就把木臼拆了（生存模式这时本来就挖不动，见 getDestroyProgress）。
     * 客户端只在创造模式拦：生存模式客户端拦了就不记「正在挖这一格」，按住左键时每刻都重发一次开始挖，
     * 变成按住不放每攒满蓄力就自动敲一下（还每秒发 20 个包）；生存模式本地本来就不会拆，不用拦。</p>
     */
    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getAction() != PlayerInteractEvent.LeftClickBlock.Action.START) {
            return;
        }
        Player player = event.getEntity();
        Level level = event.getLevel();
        if ((level.isClientSide() && !player.isCreative())
                || !WoodenMortarBlock.isPoundingHit(player, level, event.getPos())) {
            return;
        }
        event.setCanceled(true);
        if (!level.isClientSide() && WoodenMortarBlock.pound(player, level, event.getPos())) {
            player.resetAttackStrengthTicker();
        }
    }

    /**
     * 砂轮不处理瓶装调料。瓶装调料的耐久就是剩余毫升，原版砂轮会把两瓶的耐久加在一起再多送一截，
     * 等于凭空多出调料、还吞掉一个玻璃瓶。并瓶请用工作台，见 {@code SeasoningBottleMergeRecipe}。
     */
    @SubscribeEvent
    public static void onGrindstonePlace(GrindstoneEvent.OnPlaceItem event) {
        if (SeasoningBottleItem.isBottle(event.getTopItem()) || SeasoningBottleItem.isBottle(event.getBottomItem())) {
            event.setCanceled(true);
        }
    }

    /**
     * 铁砧同理：左边是瓶装调料、右边放了东西（拿另一瓶「修理」）就不给结果。只改名（右边空着）照常。
     *
     * <p>取消后原版直接 return、不会清结果格，所以手动清掉，免得还留着上一次（比如只改名）算出来的结果。</p>
     */
    @SubscribeEvent
    public static void onAnvilUpdate(AnvilUpdateEvent event) {
        if (!SeasoningBottleItem.isBottle(event.getLeft()) || event.getRight().isEmpty()) {
            return;
        }
        event.setCanceled(true);
        if (event.getPlayer() != null && event.getPlayer().containerMenu instanceof AnvilMenu menu) {
            menu.getSlot(AnvilMenu.RESULT_SLOT).set(ItemStack.EMPTY);
        }
    }

    /**
     * 「你瞅啥？」：穿着全套「东北行头」——头戴大脸盆、身穿大花袄、主手木槌、副手木锅盖——的玩家被末影人锁定
     * （盯着它看把它惹毛）时发进度（2026-11-04 用户；这一身的套装效果见 NortheastOutfit）。
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
        LivingEntity target = event.getNewTarget();
        if (!(target instanceof ServerPlayer player)) {
            return;
        }
        if (!com.gunmu.northeast_china_delight.item.NortheastOutfit.isComplete(player)) {
            return;
        }
        ModAdvancements.award(player, ModAdvancements.STARE, "stare");
    }

    /**
     * 「挨户串门」的驱动：进过所有种类的东北小院之后发进度。
     *
     * <p>这件事光靠数据包做不到：{@code minecraft:location} 触发器只知道「玩家站在哪个结构里」，
     * 而所有东北小院模板共用同一个结构 id，分不出站的是哪一种。所以这里每秒查一次
     * 玩家脚下的小院 —— 结构里的每个 {@link CourtyardPiece} 记着自己用的是哪个 NBT 模板，
     * 找到罩住玩家的那一块，把模板名换算成判据名（{@link ModAdvancements#yardCriterion}）
     * 判给玩家；判据一览见 {@code data/northeast_china_delight/advancement/visit_all_yards.json}，
     * 那边漏写了哪个模板，这里就安静地跳过，不会报错。</p>
     *
     * <p>进度自己会把同一条判据去重，所以每秒重复判也没有副作用。</p>
     */
    //? if <1.20.2 {
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event)
    {
        // 1.20.1 的 Forge 只发一对 TickEvent，START / END 各来一次，只认 END（服务器跑完这一 tick）
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        awardYardVisits(event.getServer());
        applyRadiatorWarmth(event.getServer());
    }
    //?} else {
    /*@SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event)
    {
        awardYardVisits(event.getServer());
        applyRadiatorWarmth(event.getServer());
    }
    *///?}

    private static void awardYardVisits(MinecraftServer server)
    {
        if (server.getTickCount() % YARD_CHECK_INTERVAL != 0) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            awardYardVisit(player);
        }
    }

    private static void applyRadiatorWarmth(MinecraftServer server)
    {
        if (server.getTickCount() % RADIATOR_CHECK_INTERVAL != 0) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (hasVisibleRadiator(player)) {
                // 环境效果：粒子半透明（同信标）
                MobEffectInstance warmth = DdEffects.ambientInstance(
                        DdEffects.hold(CrciMobEffects.WARMTH), RADIATOR_DURATION_TICKS, 0);
                player.addEffect(warmth);
            }
        }
    }

    private static boolean hasVisibleRadiator(ServerPlayer player)
    {
        ServerLevel level = player.serverLevel();
        BlockPos center = player.blockPosition();
        Vec3 eye = player.getEyePosition();
        Vec3 target = player.getBoundingBox().getCenter();
        double maxDistanceSqr = RADIATOR_RANGE * RADIATOR_RANGE;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-5, -5, -5), center.offset(5, 5, 5))) {
            BlockState state = level.getBlockState(pos);
            if (!state.is(ModBlocks.RADIATOR.get())) {
                continue;
            }
            Vec3 sourceCenter = Vec3.atCenterOf(pos);
            if (sourceCenter.distanceToSqr(target) > maxDistanceSqr) {
                continue;
            }
            Direction facing = state.getValue(FacingDecorBlock.FACING);
            Vec3 source = sourceCenter.add(
                    facing.getStepX() * 0.51D, 0.0D, facing.getStepZ() * 0.51D);
            ClipContext context = new ClipContext(source, eye,
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player);
            var hit = level.clip(context);
            if (hit.getType() == HitResult.Type.MISS) {
                return true;
            }
        }
        return false;
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
        String namespace = itemId.getNamespace();
        var dishBlock = (namespace.equals(NortheastChinaDelight.MODID)
                || namespace.equals("chinese_regional_culinary_ingredients"))
                ? ModBlocks.DISH_BLOCKS.get(id) : null;
        if (dishBlock == null)
        {
            return;
        }

        Player player = event.getEntity();
        // 菜肴一定能摆成方块（2026-11-04 用户：去掉「菜肴方块形态」那条配置）
        if (dishBlock.get() instanceof PlatedDishBlock plated)
        {
            InteractionResult onPlate = placeOnPlate(plated, event, stack, player);
            if (onPlate != InteractionResult.PASS)
            {
                event.setCanceled(true);
                event.setCancellationResult(onPlate);
                return;
            }
        }
        if (player.isSecondaryUseActive() && ModBlocks.canPlaceDish(id))
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
            BlockState state = dish.defaultBlockState().setValue(PlacedDishBlock.FACING, player.getDirection().getOpposite());
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

    /**
     * 放下农夫乐事的炉灶：凑齐 2×2 就拼成大灶（见 {@link BigStoveBlock#tryForm}）。
     * 放方块的这一套（快照、邻居更新）还没走完，所以挪到这一 tick 的任务队列里再拼。
     */
    @SubscribeEvent
    public static void onEntityPlaceBlock(BlockEvent.EntityPlaceEvent event)
    {
        if (!(event.getLevel() instanceof ServerLevel level) || !event.getPlacedBlock().is(CrciBlockTags.BIG_STOVE_PARTS))
        {
            return;
        }
        BlockPos pos = event.getPos().immutable();
        level.getServer().execute(() -> BigStoveBlock.tryForm(level, pos, level.getBlockState(pos)));
    }

    /** 大灶上不能再放农夫乐事厨锅；右键大灶时直接拦下，避免先放上去再被震掉 */
    @SubscribeEvent
    public static void onCookingPotOnBigStove(PlayerInteractEvent.RightClickBlock event)
    {
        if (event.isCanceled())
        {
            return;
        }
        if (event.getFace() != Direction.UP)
        {
            return;
        }
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()
                || !BuiltInRegistries.ITEM.getKey(stack.getItem())
                .equals(DdIds.of("farmersdelight", "cooking_pot")))
        {
            return;
        }
        if (!(event.getLevel().getBlockState(event.getPos()).getBlock() instanceof BigStoveBlock))
        {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.FAIL);
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
        if (!plant.is(CrciBlocks.GINSENG_CROP.get())
                || plant.getValue(GinsengCropBlock.AGE) < GinsengCropBlock.MAX_AGE)
        {
            return;
        }
        level.removeBlock(plantPos, false);
        Block.popResource(level, plantPos, new ItemStack(CrciItems.GINSENG.get()));
        Block.popResource(level, plantPos, new ItemStack(CrciItems.GINSENG_SEEDS.get(), 1 + level.random.nextInt(2)));
    }

    /**
     * 1.20.1 没有数据表（data map）：熔炉 / 烟熏炉 / 高炉 / 机械动力烈焰燃烧器的燃料时长
     * 只能在事件里给。1.20.5+ 走 {@code data/neoforge/data_maps/item/furnace_fuels.json}，
     * 这一段在那些版本上不编译。
     */
    //? if <1.20.2 {
    @SubscribeEvent
    public static void onFurnaceFuel(FurnaceFuelBurnTimeEvent event)
    {
        ItemStack stack = event.getItemStack();
        if (stack.is(CrciItems.CORN_STALK.get()))
        {
            event.setBurnTime(200);
        }
        else if (stack.is(CrciItems.ANIMAL_OIL.get()))
        {
            // 熔炉烧掉的是一剂（50 mB）：剩下的瓶子留在燃料槽接着烧，一满瓶共 5 × 160 = 800，4 瓶 = 一块动物油块
            event.setBurnTime(160);
        }
        else if (stack.is(ModItems.ANIMAL_OIL_BLOCK_ITEM.get()))
        {
            event.setBurnTime(3200);
        }
    }
    //?}
}
