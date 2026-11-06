package com.gunmu.northeast_china_delight.block;

import com.gunmu.northeast_china_delight.advancement.ModAdvancements;
import com.gunmu.northeast_china_delight.crafting.PoundingRecipes;
import com.gunmu.northeast_china_delight.item.ModItems;
import com.gunmu.northeast_china_delight.item.SeasoningBottleItem;
import com.gunmu.northeast_china_delight.util.DdStacks;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
//? if >=1.20.5 {
import net.minecraft.world.ItemInteractionResult;
//?}
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 木臼：可以放下的「舂捣」容器方块。
 *
 * <p>{@link #STATE} 表达外观阶段：0=空臼，1..N=装料/加工中/成品。非空阶段由
 * {@code WoodenMortarRenderer} 按 {@link PoundingRecipes.Recipe#stageModels()} 逐阶段画模型，
 * 因此数据包可以使用任意已注册的方块模型，而不受 blockstate 里那五个内置模型限制。
 *
 * <p>交互：
 * <ul>
 *   <li>空臼（STATE 0）手持木槌右键 → 把木槌搁在臼上（{@link #MALLET}），再空手右键取回</li>
 *   <li>空臼（STATE 0）手持配方投入物右键 → 逐次投入；臼里的料正好凑齐某条配方就转到它的装料状态，
 *       还没开舂前能接着放、改凑别的配方（2026-11-06，见 {@link #deposit}）</li>
 *   <li>装料 / 加工中（filled..done-1）左键敲击 → 加工一次，切模型；每敲一下扣木槌 1 点耐久。
 *       第一下敲击时才按臼里的料定下是哪条配方</li>
 *   <li>成品状态（doneState）空手右键 → 取出成品，转回 STATE 0</li>
 * </ul>
 *
 * <p>敲击为什么走左键：右键要留给装料 / 放木槌 / 取货，而左键本来就带「挥」的动作。
 * 代价是左键也会推进挖掘进度，所以只有「手里拿着当前配方的工具、臼里正在加工」时 {@link #getDestroyProgress}
 * 返回 0（这一下是敲击）；其余任何阶段、空手或拿别的工具都能照常挖掉。挖掉只掉木臼本身，臼里的料不掉；
 * 搁着木槌时连木槌一起掉（原样带着存进去的耐久和附魔），见 {@link #onRemove}；
 * 又因为 {@code Block#attack} 只在客户端跑（1.20.1 / 1.21.1 的 {@code MultiPlayerGameMode#startDestroyBlock}），
 * 真正改方块的敲击挂在服务端左键事件上，见 {@code ModGameplayEvents#onLeftClickBlock}。</p>
 *
 * <p>状态机：空臼 → 带槌空臼 → 空臼。带槌状态是只进不出的收纳状态，
 * 不接受任何手持物装料，也不能加工；只能空手右键把木槌取回。</p>
 */
public class WoodenMortarBlock extends BaseEntityBlock {

    //? if >=1.20.2 {
    public static final MapCodec<WoodenMortarBlock> CODEC = simpleCodec(WoodenMortarBlock::new);
    //?}

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    /** 0=空臼；1..N=装料/加工中/成品。最多 4 个阶段模型 + 1 个成品阶段。 */
    public static final int MAX_STATE = 5;
    public static final IntegerProperty STATE = IntegerProperty.create("state", 0, MAX_STATE);

    /**
     * 搁着的木槌把手朝哪：放木槌那一刻按玩家来定，把手朝着玩家（模型里把手朝北，blockstate 按它转）。
     * 和木臼本体的 {@link #FACING} 分开存，放木槌不会把整只臼转过去。
     */
    public static final DirectionProperty MALLET_FACING = DirectionProperty.create("mallet_facing",
            Direction.Plane.HORIZONTAL);

    /** 臼上是否搁着一把木槌：只影响空臼的外观（带锤空木臼），加工中不画木槌 */
    public static final BooleanProperty MALLET = BooleanProperty.create("mallet");

    /**
     * 默认「蓄力」门槛；每条配方可用 {@code charge_threshold} 覆盖，旧配方仍是 90%。
     *
     * <p>攻速 1.0 的玩家约 0.9 秒攒满（20 tick 的 90% = 18 tick），所以连着猛点没用，得按节奏敲。
     * 判定在 {@link #pound(Player, Level, BlockPos)} 里解析配方后执行。</p>
     */
    public static final float POUND_CHARGE_THRESHOLD = 0.9F;

    private static final VoxelShape SHAPE = Block.box(1.0, 0.0, 1.0, 15.0, 8.0, 15.0);

    public WoodenMortarBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(STATE, 0)
                .setValue(MALLET, false)
                .setValue(MALLET_FACING, Direction.NORTH));
    }

    //? if >=1.20.2 {
    @Override
    public MapCodec<WoodenMortarBlock> codec() {
        return CODEC;
    }
    //?}

    @Override
    public RenderShape getRenderShape(BlockState state) {
        // 非空状态的模型由 BER 根据当前配方动态选择；blockstate 里的旧阶段模型保持原样，
        // 但不再让方块烘焙器重复画一层。
        return state.getValue(STATE) > 0 ? RenderShape.INVISIBLE : RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WoodenMortarBlockEntity(pos, state);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(STATE, 0);
    }

    //? if >=1.20.5 {
    @Override
    protected ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level level, BlockPos pos,
                                             Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        InteractionResult result = onUse(held, state, level, pos, player);
        if (result == InteractionResult.PASS) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        return result == InteractionResult.FAIL
                ? ItemInteractionResult.FAIL
                : ItemInteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        InteractionResult r = onTakeResult(state, level, pos, player);
        return r == null ? InteractionResult.PASS : r;
    }
    //?} else {
    /*@Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        ItemStack held = player.getItemInHand(hand);
        InteractionResult result = onUse(held, state, level, pos, player);
        if (result != InteractionResult.PASS) {
            return result;
        }
        InteractionResult take = onTakeResult(state, level, pos, player);
        return take == null ? InteractionResult.PASS : take;
    }
    *///?}

    /**
     * 手持物品交互：装料 或用工具加工。返回 InteractionResult.PASS 表示没接住，交给空手分支。
     *
     * <p>返回类型用 InteractionResult，不用 1.20.5+ 才有的 ItemInteractionResult ——
     * 版本差异留给上面 useItemOn / use 两个外壳去转，和 Vat 同一套写法。</p>
     */
    private InteractionResult onUse(ItemStack held, BlockState state, Level level, BlockPos pos, Player player) {
        if (!(level.getBlockEntity(pos) instanceof WoodenMortarBlockEntity mortar)) {
            return InteractionResult.PASS;
        }
        int stage = state.getValue(STATE);

        // 带槌空臼只允许空手取回木槌。这里必须用 FAIL 而不是 PASS：
        // 1.21 的 useItemOn 收到 PASS 后会继续走 useWithoutItem，等于手持米饭也能把槌取走。
        if (!held.isEmpty() && (state.getValue(MALLET) || mortar.hasMallet())) {
            return InteractionResult.FAIL;
        }

        if (stage == 0) {
            // 手持木槌：把木槌搁在臼上（耐久、附魔一并记进方块实体）
            if (held.is(ModItems.WOODEN_MALLET.get())) {
                if (mortar.hasDepositedInputs()) {
                    return InteractionResult.FAIL;
                }
                if (level.isClientSide) {
                    return InteractionResult.SUCCESS;
                }
                mortar.setMallet(DdStacks.consumeAndReturn(held, 1, player));
                level.setBlock(pos, state.setValue(MALLET, true)
                        .setValue(MALLET_FACING, player.getDirection().getOpposite()), 3);
                level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 0.8F, 1.0F);
                return InteractionResult.SUCCESS;
            }
        }

        return deposit(held, state, level, pos, player, mortar, stage);
    }

    /**
     * 放一样料（2026-11-06 改：开舂之前随时能接着放，每放一样都按臼里全部的料重挑配方）。
     *
     * <p>以前一放第一样东西就认死一条配方：熟花生一放进去就凑齐了花生酱，想再加蜂蜜做别的来不及。
     * 用户：「臼应该在玩家捶打时，才检查匹配哪一项配方」。现在：</p>
     * <ul>
     *   <li>手里这样东西只要还有哪条配方装得下（连同臼里已有的料）就让放，放几个见 {@link PoundingRecipes#room}；</li>
     *   <li>放完按 {@link PoundingRecipes#choose} 挑：正好凑齐某条就显示它装好料的样子（可以开舂了），
     *       没凑齐就显示放了一半的料；凑齐了也还能接着放，凑成别的配方就改显示那条；</li>
     *   <li>开舂那一下再挑一遍才定（{@link #pound}），敲过以后不能再放。</li>
     * </ul>
     */
    private InteractionResult deposit(ItemStack held, BlockState state, Level level, BlockPos pos, Player player,
                                      WoodenMortarBlockEntity mortar, int stage) {
        if (held.isEmpty() || mortar.started()) {
            return InteractionResult.PASS;
        }
        PoundingRecipes.Recipe bound = mortar.resolve(level);
        if (bound == null && mortar.recipeId() != null) {
            // 已经有部分投料、但原配方被数据包删掉/禁用：先把旧料交给空手右键退还，
            // 不能借机挑一条新配方，否则旧料会被静默清掉。
            return InteractionResult.FAIL;
        }
        // 只有空臼、放了一半（还是空臼的样子）、或者刚凑齐还没敲的时候能放
        if (stage != 0 && (bound == null || stage != bound.filledState())) {
            return InteractionResult.PASS;
        }
        var manager = level.getRecipeManager();
        List<ItemStack> contents = mortar.depositedStacks();
        int movedCount = PoundingRecipes.room(manager, contents, held);
        if (movedCount <= 0) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        ItemStack moved = held.copyWithCount(movedCount);
        contents.add(moved.copy());
        PoundingRecipes.Fit fit = PoundingRecipes.choose(manager, contents);
        if (fit == null) {
            return InteractionResult.PASS;
        }
        // 用完剩下的容器（配方写的 return_container，或者水桶 / 水盆 / 水瓶 / 蜂蜜瓶自己剩的空桶 / 空盆 / 空瓶），
        // 按挑中的这条配方里认它的那一格算
        ItemStack left = leftover(fit.recipe(), moved);
        if (!player.getAbilities().instabuild) {
            held.shrink(movedCount);
        }
        mortar.setDeposits(fit.recipe().id(), fit.slots());

        if (!player.getAbilities().instabuild && !left.isEmpty()) {
            // 手里那个用光了，第一个空容器放回手上（同原版倒空水桶），其余的塞背包
            boolean handFree = held.isEmpty();
            for (int i = 0; i < movedCount; i++) {
                if (handFree) {
                    player.setItemInHand(InteractionHand.MAIN_HAND, left.copy());
                    handFree = false;
                } else {
                    giveOrDrop(player, left.copy());
                }
            }
        }

        setStage(level, pos, state, fit.complete() ? fit.recipe().filledState() : 0);
        level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 0.8F, 1.0F);
        return InteractionResult.SUCCESS;
    }

    /** 放进去的这样东西用完剩什么：看配方里认它的那一格（同一样东西各条配方一般剩的一样） */
    private static ItemStack leftover(PoundingRecipes.Recipe recipe, ItemStack moved) {
        for (PoundingRecipes.Input input : recipe.inputs()) {
            if (input.ingredient().test(moved)) {
                return input.leftover(moved);
            }
        }
        return ItemStack.EMPTY;
    }

    /** 空手右键：成品状态时取货 */
    @Nullable
    private InteractionResult onTakeResult(BlockState state, Level level, BlockPos pos, Player player) {
        if (!(level.getBlockEntity(pos) instanceof WoodenMortarBlockEntity mortar)) {
            return null;
        }

        // 带槌状态优先把木槌取回来。以方块实体里的木槌为准，
        // 顺带修复旧版本可能留下的「装料 + 带槌」脏状态。
        if (mortar.hasMallet() || state.getValue(MALLET)) {
            if (level.isClientSide) {
                return InteractionResult.SUCCESS;
            }
            ItemStack mallet = mortar.takeMallet();
            if (state.getValue(MALLET)) {
                level.setBlock(pos, state.setValue(MALLET, false), 3);
            }
            if (!mallet.isEmpty()) {
                giveOrDrop(player, mallet);
            }
            level.playSound(null, pos, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 0.8F, 1.0F);
            return InteractionResult.SUCCESS;
        }

        PoundingRecipes.Recipe recipe = mortar.resolve(level);
        if (recipe == null) {
            // 配方被删掉/禁用后不再把已经投入的料锁死；有产物快照就先发产物，否则退还输入。
            if (mortar.hasPendingResults()) {
                if (level.isClientSide) {
                    return InteractionResult.SUCCESS;
                }
                giveResults(player, mortar.pendingResults());
                mortar.clear();
                setStage(level, pos, state, 0);
                level.playSound(null, pos, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 0.8F, 1.0F);
                return InteractionResult.SUCCESS;
            }
            if (mortar.hasDepositedInputs()) {
                if (level.isClientSide) {
                    return InteractionResult.SUCCESS;
                }
                for (ItemStack stack : mortar.takeDepositedInputs()) {
                    giveOrDrop(player, stack);
                }
                mortar.clear();
                setStage(level, pos, state, 0);
                level.playSound(null, pos, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 0.8F, 1.0F);
                return InteractionResult.SUCCESS;
            }
            if (state.getValue(STATE) > 0) {
                if (level.isClientSide) {
                    return InteractionResult.SUCCESS;
                }
                mortar.clear();
                setStage(level, pos, state, 0);
                level.playSound(null, pos, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 0.8F, 1.0F);
                return InteractionResult.SUCCESS;
            }
            return null;
        }
        if (state.getValue(STATE) < recipe.doneState()) {
            return null;
        }
        // 配方写了 result_container（比如舂出来的油）就得拿着那个容器才能取，每取一次扣一个；
        // 再写了 result_mb 就按毫升装：拿空容器装出一瓶 result_mb 的，拿没装满的同种瓶子就往里续，不扣瓶子
        ItemStack held = player.getMainHandItem();
        ItemStack container = recipe.resultContainer();
        int mb = recipe.resultMb();
        boolean topUp = mb > 0 && held.is(recipe.liquidProduct().getItem());
        if (container != null && !held.is(container.getItem()) && !topUp) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable(
                        "message.northeast_china_delight.mortar.need_container", container.getHoverName()), true);
            }
            return InteractionResult.FAIL;
        }
        if (topUp && SeasoningBottleItem.usedMb(held) < mb) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable(
                        "message.northeast_china_delight.mortar.bottle_full"), true);
            }
            return InteractionResult.FAIL;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        if (topUp) {
            SeasoningBottleItem.refill(held, mb);
        } else {
            if (container != null && !player.getAbilities().instabuild) {
                held.shrink(1);
            }
            if (mb > 0) {
                ItemStack filled = recipe.liquidProduct().copyWithCount(1);
                filled.setDamageValue(SeasoningBottleItem.CAPACITY_MB - mb);
                giveOrDrop(player, filled);
            } else {
                giveResults(player, mortar.hasPendingResults()
                        ? mortar.pendingResults()
                        : recipe.rollResults(level.random));
            }
        }
        mortar.clear();
        setStage(level, pos, state, 0);
        level.playSound(null, pos, container != null && container.is(Items.GLASS_BOTTLE)
                ? SoundEvents.BOTTLE_FILL : SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 0.8F, 1.0F);
        return InteractionResult.SUCCESS;
    }

    private static void giveResults(Player player, List<ItemStack> results) {
        for (ItemStack stack : results) {
            if (!stack.isEmpty()) {
                giveOrDrop(player, stack.copy());
            }
        }
    }

    /**
     * 左键敲击一次 = 舂捣一次。返回 true 表示这一下算「敲击」、应当拦下挖掘。
     *
     * <p>用的是玩家手里那把木槌（配方的 {@code tool}）；每敲一下扣 1 点耐久。
     * 挥击蓄力门槛由当前配方的 {@code charge_threshold} 决定。
     * 臼上搁着的木槌只用于收纳，不参与加工 —— 带槌状态必须空手取回后才能装料。
     * 真正调用它的是服务端的左键事件，见 {@code ModGameplayEvents}。</p>
     */
    public static boolean pound(Player player, Level level, BlockPos pos) {
        if (level.isClientSide || !(level.getBlockEntity(pos) instanceof WoodenMortarBlockEntity mortar)) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof WoodenMortarBlock)) {
            return false;
        }
        // 带槌状态只用于收纳，不能加工；旧存档里的「装料 + 带槌」也得先空手取回木槌。
        if (state.getValue(MALLET) || mortar.hasMallet()) {
            return false;
        }
        int stage = state.getValue(STATE);
        PoundingRecipes.Recipe recipe = mortar.resolve(level);
        // recipe == null：空臼，或者原配方被数据包删掉 / 禁用了（旧料等空手右键退还，不趁机换配方）
        if (recipe != null && !mortar.started()) {
            // 开舂这一下才定配方（2026-11-06 用户）：按臼里现有的料再挑一遍，放料时挑的只是显示用的
            PoundingRecipes.Fit fit = PoundingRecipes.choose(level.getRecipeManager(), mortar.depositedStacks());
            if (fit == null || !fit.complete()) {
                return false;
            }
            if (!recipe.id().equals(fit.recipe().id())) {
                recipe = fit.recipe();
                mortar.setDeposits(recipe.id(), fit.slots());
                stage = recipe.filledState();
                setStage(level, pos, state, stage);
                state = level.getBlockState(pos);
            }
        }
        if (recipe == null || stage < recipe.filledState() || stage >= recipe.doneState()) {
            return false;
        }

        ItemStack held = player.getMainHandItem();
        if (!recipe.tool().test(held)) {
            return false;
        }
        if (player.getAttackStrengthScale(0.5F) < recipe.chargeThreshold()) {
            return false;
        }

        mortar.poundOnce();
        boolean finished = mortar.progress() >= recipe.poundTimes();
        if (finished) {
            mortar.setPendingResults(recipe.rollResults(level.getRandom()));
        }
        int next = finished ? recipe.doneState() : nextPoundState(recipe, stage, level.getRandom());
        setStage(level, pos, state, next);
        level.playSound(null, pos, SoundEvents.WOOD_HIT, SoundSource.BLOCKS, 1.0F,
                0.9F + level.getRandom().nextFloat() * 0.2F);

        if (!player.getAbilities().instabuild) {
            DdStacks.hurtAndBreak(held, 1, player, InteractionHand.MAIN_HAND);
        }
        awardPound(player, held);
        return true;
    }

    /**
     * 舂捣成功一次就发进度：木槌发「开舂」，1.21 的重锤发「大锤小用」。
     *
     * <p>刻意只认「用的是什么工具」而不认配方 —— 以后加新的舂捣配方不用回来改这里。
     * 重锤那段用 stonecutter 圈起来，1.20.x 编出来的 jar 里根本没有这段代码。</p>
     */
    private static void awardPound(Player player, ItemStack tool) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (tool.is(ModItems.WOODEN_MALLET.get())) {
            ModAdvancements.award(serverPlayer, ModAdvancements.POUND, "pound");
        }
        //? if >=1.20.5 {
        else if (tool.is(Items.MACE)) {
            ModAdvancements.award(serverPlayer, ModAdvancements.MACE_POUND, "pound");
        }
        //?}
    }

    /**
     * 拿着当前配方的工具敲正在加工的木臼时挖不动：这几下左键是「敲击」，不能顺手把臼拆了。
     * 别的情况（空手、拿别的工具、装料没满、已经出成品、搁着木槌）都能照常挖。
     */
    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        if (isPoundingHit(state, player, level, pos)) {
            return 0.0F;
        }
        return super.getDestroyProgress(state, player, level, pos);
    }

    /** 这一下左键会被 {@link #pound} 当成敲击（工具对、正在加工、没搁木槌）；不看蓄力，免得没蓄满时变成在挖 */
    private static boolean isPoundingHit(BlockState state, Player player, BlockGetter getter, BlockPos pos) {
        if (!(getter instanceof Level level) || !(level.getBlockEntity(pos) instanceof WoodenMortarBlockEntity mortar)
                || state.getValue(MALLET) || mortar.hasMallet()) {
            return false;
        }
        PoundingRecipes.Recipe recipe = mortar.resolve(level);
        int stage = state.getValue(STATE);
        return recipe != null && stage >= recipe.filledState() && stage < recipe.doneState()
                && recipe.tool().test(player.getMainHandItem());
    }

    /**
     * 木臼没了（被挖、被炸、被命令换掉）时，搁在上面的木槌原样掉出来；臼里的料不掉，掉落表只给木臼本身。
     */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide
                && level.getBlockEntity(pos) instanceof WoodenMortarBlockEntity mortar && mortar.hasMallet()) {
            Block.popResource(level, pos, mortar.takeMallet());
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /** 加工中下一次该切到哪个模型状态 */
    private static int nextPoundState(PoundingRecipes.Recipe recipe, int current, RandomSource random) {
        int filled = recipe.filledState();
        int lastProcessing = recipe.doneState() - 1;
        if (lastProcessing <= filled) {
            return filled;
        }
        return switch (recipe.transition()) {
            case SEQUENTIAL -> Math.min(current + 1, lastProcessing);
            case CONDITIONAL -> current; // 预留：按条件定
            case RANDOM -> filled + random.nextInt(lastProcessing - filled + 1);
        };
    }

    private static void setStage(Level level, BlockPos pos, BlockState state, int stage) {
        BlockState next = state.setValue(STATE, Math.min(stage, MAX_STATE));
        // 防御旧存档/命令写入：只要进入装料或加工状态，就不能再带 mallet 标记。
        if (stage > 0) {
            next = next.setValue(MALLET, false);
        }
        level.setBlock(pos, next, 3);
    }

    private static void giveOrDrop(Player player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, STATE, MALLET, MALLET_FACING);
    }
}
