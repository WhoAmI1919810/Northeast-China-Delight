package com.gunmu.northeast_china_delight.block;

import com.gunmu.northeast_china_delight.crafting.VatBrewing;
import com.gunmu.northeast_china_delight.crafting.VatRecipe;
import com.gunmu.northeast_china_delight.crafting.VatRecipes;
import com.gunmu.northeast_china_delight.item.ModItems;
import com.gunmu.northeast_china_delight.item.SeasoningBottleItem;
import com.gunmu.northeast_china_delight.NortheastChinaDelight;
import com.gunmu.northeast_china_delight.util.DdIds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
//? if >=1.20.5 {
/*import net.minecraft.world.ItemInteractionResult;
*///?}
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 大缸。
 *
 * 内容物（蔬菜、肉、盐、酱块）、压缸石、蒙缸地毯都记在 {@link VatBlockEntity} 里，
 * 会按实际物品渲染在缸内，不需要打开界面就能看见里面有什么。
 * 这里只负责交互流程；具体数值与规则见 {@link VatRecipes}。
 */
public class Vat extends Block implements EntityBlock {

    public static final IntegerProperty WATER_LEVEL = ModBlockStateProperties.WATER_LEVEL;
    public static final BooleanProperty FERMENTED = ModBlockStateProperties.VAT_FERMENTED;
    public static final BooleanProperty SALTED = ModBlockStateProperties.VAT_SALTED;
    public static final IntegerProperty PROGRESS = ModBlockStateProperties.VAT_PROGRESS;
    public static final int MAX_PROGRESS = ModBlockStateProperties.VAT_MAX_PROGRESS;

    /** 可以拿来压缸的方块（石头类） */
    public static final TagKey<Block> PRESS_STONES = TagKey.create(Registries.BLOCK,
            DdIds.of(NortheastChinaDelight.MODID, "vat_press_stones"));

    public Vat(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState()
                .setValue(WATER_LEVEL, 0)
                .setValue(FERMENTED, false)
                .setValue(SALTED, false)
                .setValue(PROGRESS, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(WATER_LEVEL, FERMENTED, SALTED, PROGRESS);
    }

    @Override
    public BlockState getStateForPlacement(@NotNull BlockPlaceContext context) {
        return this.defaultBlockState();
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new VatBlockEntity(pos, state);
    }

    // ===== 工具方法 =====

    @Nullable
    private static VatBlockEntity vatAt(Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof VatBlockEntity vat ? vat : null;
    }

    /** 手里拿的是不是可以压缸的石头类方块（必须是完整方块：台阶/墙/栅栏这种不算） */
    private static boolean isPressStone(ItemStack stack) {
        return stack.getItem() instanceof BlockItem blockItem
                && blockItem.getBlock().defaultBlockState().is(PRESS_STONES)
                && blockItem.getBlock().defaultBlockState().isSolid();
    }
    /** 手里拿的正好是这条配方点名要压的那件东西（比如冻梨配方的雪块） */
    private static boolean matchesRequiredSealItem(VatRecipe recipe, ItemStack stack) {
        var matcher = recipe.requiredSealItem();
        return matcher != null && matcher.test(stack);
    }

    /** 手里拿的是不是农夫乐事的粗布毯（酿醋盖缸用） */
    public static boolean isClothRug(ItemStack stack) {
        return stack.is(BuiltInRegistries.ITEM.get(
                DdIds.of("farmersdelight", "canvas_rug")));
    }

    /**
     * 能不能把瓶装酸引水倒进这个缸。
     * 注意：第一瓶倒进空缸后，缸的状态会变成「泡菜缸」但还没腌制，
     * 所以泡菜缸不能要求 fermented，否则只能倒一瓶。
     *
     * <p>缸里就装得下 {@link ModBlockStateProperties#VAT_MAX_WATER} 层（3000 mB）液体，
     * 所以先卡一道总容量：满了就不再收，免得液面显示停在 3 层、实际却越积越多。
     */
    private static boolean canPourSourWater(VatBlockEntity vat, BlockState state) {
        int capacity = ModBlockStateProperties.VAT_MAX_WATER * VatRecipes.WATER_MB_PER_LEVEL;
        if (vat.waterMb() + vat.sourWaterMb() + VatRecipes.SERVING_MB > capacity) {
            return false;
        }
        return switch (vat.kind()) {
            // 空缸：倒进去就成了一缸酸引水（状态记为泡菜缸）。
            // 缸里已经有清水就不许倒 —— 酸引水和清水不能混，混了水位会被算错、水还会凭空少掉。
            case NONE -> state.getValue(Vat.WATER_LEVEL) == 0
                    && vat.contents().isEmpty() && !vat.hasProductLiquid();
            // 泡菜缸：只有「本来就是酸引水缸」或「空缸」才能继续攒；
            // 拿清水正在腌的那一缸不许倒，否则整缸盐水就废了
            case PICKLE -> vat.sourWaterMb() > 0
                    || (state.getValue(Vat.WATER_LEVEL) == 0 && vat.contents().isEmpty());
            // 正在酿的白醋缸：可以把酸引水续上
            case WHITE_VINEGAR -> !state.getValue(FERMENTED);
            // 其它缸里已经有别的成品，不允许混液体
            default -> false;
        };
    }

    /**
     * 缸里是不是"酿完大酱 / 酱油、液体取空、只剩酱渣"的状态。
     * 这种缸允许再倒水接着酿醋 —— 否则 {@code FERMENTED = true} 会把加水这条直接挡死。
     */
    private static boolean canRefillForVinegar(VatBlockEntity vat) {
        return (vat.kind() == VatRecipes.Kind.PASTE || vat.kind() == VatRecipes.Kind.SOY_SAUCE)
                && vat.residueCount() >= VatRecipes.RESIDUE_COUNT
                && !vat.hasProductLiquid();
    }

    /**
     * 缸里的盐够不够这一缸的配方。
     *
     * <p>要几份盐完全由配方里的数量规则决定（泡菜一层水一份、大酱三份、腊肉和肉一样多…），
     * 这里只是"照表读一遍"，客户端渲染「发白的盐水」时也用这个判断。
     */
    public static boolean hasEnoughSalt(VatBlockEntity vat) {
        // 投料阶段 kind 还是 NONE，得看这缸**现在最像**哪条配方
        VatRecipe recipe = VatBrewing.current(vat);
        if (recipe == null) {
            return false;
        }
        VatRecipe.Counts counts = VatBrewing.counts(vat, null);
        ItemStack salt = new ItemStack(ModItems.SALT.get());
        for (int i = 0; i < recipe.slots().size(); i++) {
            if (!recipe.accepts(i, salt)) {
                continue;
            }
            int need = recipe.boundsOf(i, counts).min();
            return need > 0 && counts.count(recipe.slots().get(i).matcher()) >= need;
        }
        return false;
    }

    /** 重新判定盐水是否够咸，并把结果写进方块状态（状态一变，客户端水面颜色就会立刻刷新） */
    private static void refreshSalted(Level level, BlockPos pos, VatBlockEntity vat) {
        BlockState state = level.getBlockState(pos);
        boolean salted = hasEnoughSalt(vat);
        if (state.getValue(SALTED) != salted) {
            level.setBlock(pos, state.setValue(SALTED, salted), 3);
        }
    }

    /**
     * 条件齐了就开始计时。
     *
     * <p>"条件齐了"这句话现在完全由 {@link VatBrewing#ready} 按配方表判断：
     * 材料配齐、液体对得上、封口物也对。这里只负责把缸标成这条配方、然后排一次 tick。
     * 创造大缸子类会覆盖这个方法，把「排 tick」换成「当场完成」。</p>
     */
    protected void tryStart(Level level, BlockPos pos, VatBlockEntity vat) {
        BlockState state = level.getBlockState(pos);
        if (state.getValue(FERMENTED) || level.getBlockState(pos).getValue(PROGRESS) > 0) {
            return;
        }
        VatRecipe recipe = VatBrewing.ready(vat);
        if (recipe != null) {
            // 记下这条配方：Jade 的文案、取货规则、流体种类都看它
            if (vat.kind() != recipe.kind()) {
                vat.setKind(recipe.kind());
            }
            level.scheduleTick(pos, level.getBlockState(pos).getBlock(), VatBrewing.stepTicks(recipe, vat));
        }
    }

    /** 现在能不能再往缸里倒一桶水 */
    private static boolean canAddWater(VatBlockEntity vat) {
        if (vat.waterLayers() >= ModBlockStateProperties.VAT_MAX_WATER) {
            return false;
        }
        // 酸引水和清水不能混：混了水位会算错，水还会凭空少掉
        if (vat.sourWaterMb() > 0) {
            return false;
        }
        // 酿完大酱 / 酱油、只剩酱渣的缸可以续水接着酿醋，别的缸做完就不再收水
        return !vat.isFermented() || canRefillForVinegar(vat);
    }

    private static void give(Player player, ItemStack stack) {
        if (!stack.isEmpty() && !player.addItem(stack)) {
            player.drop(stack, false);
        }
    }

    /** 处理完成、内容清空后把大缸恢复成空缸 */
    /** 清空内容并把大缸恢复成空缸（流体管道抽空成品时也会用到） */
    public static void reset(Level level, BlockPos pos, BlockState state, VatBlockEntity vat) {
        vat.clearContents();
        level.setBlock(pos, state
                .setValue(WATER_LEVEL, 0)
                .setValue(FERMENTED, false)
                .setValue(SALTED, false)
                .setValue(PROGRESS, 0), 3);
    }

    // ===== 交互 =====

    /**
     * 拿着东西右键大缸。
     *
     * <p>顺序：加水 → 倒酸引水 → 用药瓶续液体 → 用碗 / 瓶装走成品 → 投料 → 封口。
     * 其中"投料"和"封口"完全交给 {@link VatBrewing} 按配方表判断，
     * 这里不再有任何"什么物种应该配什么牌子"的分支。
     */
    @Override
    //? if <1.20.5 {
    public @NotNull InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        ItemStack stack = player.getItemInHand(hand);
        InteractionResult itemResult = vatUseItem(stack, state, level, pos, player, hand, hit);
        if (itemResult != InteractionResult.PASS || hand != InteractionHand.MAIN_HAND) {
            return itemResult;
        }
        // 1.21.1 里这是两个方法：useItemOn 答「交给默认交互」时才轮到「空手右键」这一套
        return vatUseWithdrawn(state, level, pos, player, hit);
    }
    //?} else {
    /*protected @NotNull ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                                       Player player, InteractionHand hand, BlockHitResult hit) {
        InteractionResult result = vatUseItem(stack, state, level, pos, player, hand, hit);
        return result == InteractionResult.PASS
                ? ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
                : ItemInteractionResult.sidedSuccess(level.isClientSide());
    }
    *///?}

    /** 拿着东西右键大缸（两个版本共用的实现，由上面的壳子翻译返回值） */
    private @NotNull InteractionResult vatUseItem(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                                  Player player, InteractionHand hand, BlockHitResult hit) {
        VatBlockEntity vat = vatAt(level, pos);
        if (vat == null) {
            return InteractionResult.PASS;
        }

        // 潜行右键：优先把封口物取回来（石头 / 羊毛毯 / 粗布毯 / 雪块），一次右键取一样
        if (player.isSecondaryUseActive() && (vat.isPressed() || vat.isCovered())) {
            return takeSeal(level, pos, state, vat, player);
        }

        // 压着石头时是「锁住」状态：不能往里放任何东西，必须先取出石头。
        // 例外：手里拿玻璃瓶 / 调料瓶时，右键是**往外取液体**，不算"往缸里加东西"，
        // 不该被压缸石挡住 —— 泡菜腌好后石头还压在缸上，酸引水就是要能这样装走的。
        if (vat.isPressed() && !isBottleTake(stack)) {
            if (!level.isClientSide && !stack.isEmpty()) {
                hint(player, "message.northeast_china_delight.vat.locked_by_press");
            }
            return InteractionResult.PASS;
        }

        // 还没酿好：普通右键只报进度（拿着东西也一样，免得把东西投进去才发现点错了）
        if (!state.getValue(FERMENTED) && !stack.is(Items.WATER_BUCKET)) {
            if (!level.isClientSide && stack.isEmpty()) {
                showProgress(player, level, pos, state, vat);
            }
        }

        // 加水：桶装水，一桶正好一层
        if (stack.is(Items.WATER_BUCKET) && canAddWater(vat)) {
            if (!level.isClientSide) {
                vat.addWater(VatRecipes.WATER_MB_PER_LEVEL);
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                    player.addItem(new ItemStack(Items.BUCKET));
                }
                level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
                // 加了水之后盐可能又不够了，需要重新判定
                refreshSalted(level, pos, vat);
                // 水也是配方的一部分：先放料、后倒水（或者先盖好盖布再倒水）时，
                // 这一下同样要让条件齐了的配方开工，否则缸会一直停在"材料没配齐"
                tryStart(level, pos, vat);
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }

        // 客户端不做预测改动：一切判定与改动都在服务端做（和以前的写法一致）
        if (level.isClientSide) {
            return InteractionResult.PASS;
        }

        // 手里没东西也没别的事可做：显示当前发酵进度（actionbar）
        if (stack.isEmpty()) {
            showProgress(player, level, pos, state, vat);
            return InteractionResult.sidedSuccess(false);
        }

        // 把瓶装酸引水倒回缸里：空缸会变成「泡菜缸」，腌好的泡菜缸则继续攒酸引水
        if (stack.is(ModItems.SOUR_WATER.get()) && canPourSourWater(vat, state)) {
            if (vat.kind() == VatRecipes.Kind.NONE) {
                vat.setKind(VatRecipes.Kind.PICKLE);
            }
            vat.addSourWater(VatRecipes.SERVING_MB);
            consume(player, stack);
            give(player, new ItemStack(Items.GLASS_BOTTLE));
            playFill(level, pos);
            // 同理：白醋差的就是这一瓶酸引水时，倒进去就该开工
            tryStart(level, pos, vat);
            return InteractionResult.sidedSuccess(false);
        }

        // 用过的调料瓶：右键大缸把液体续进瓶里（缺多少补多少，缸里不够就补多少）
        if (SeasoningBottleItem.isBottle(stack) && vat.isFermented()) {
            if (SeasoningBottleItem.isFull(stack)) {
                hint(player, "message.northeast_china_delight.vat.bottle_full");
                return InteractionResult.sidedSuccess(false);
            }
            if (VatRecipes.bottleFor(vat.kind()) != stack.getItem()) {
                hint(player, "message.northeast_china_delight.vat.bottle_mismatch");
                return InteractionResult.sidedSuccess(false);
            }
            int moved = vat.drainProduct(vat.kind(), SeasoningBottleItem.usedMb(stack));
            if (moved <= 0) {
                hint(player, "message.northeast_china_delight.vat.bottle_empty");
                return InteractionResult.sidedSuccess(false);
            }
            SeasoningBottleItem.refill(stack, moved);
            playFill(level, pos);
            return InteractionResult.sidedSuccess(false);
        }

        // 用碗 / 玻璃瓶把成品液体装走（一缸 10 份，每装一份液面降一档）
        ItemStack filled = VatBrewing.fillContainer(vat, stack);
        if (filled != null) {
            boolean bowl = stack.is(Items.BOWL);
            consume(player, stack);
            give(player, filled);
            // 大酱是黏稠的，用蜂蜜的黏腻音效而不是水声
            level.playSound(null, pos, bowl
                    ? SoundEvents.HONEY_BLOCK_SLIDE : SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
            return InteractionResult.sidedSuccess(false);
        }

        // 投料：收不收、放哪一格、放进去变成哪条配方，全由配方表说了算
        VatRecipe forInsert = VatBrewing.forInsert(vat, stack);
        if (forInsert != null && insert(level, pos, vat, player, stack, forInsert, hand)) {
            return InteractionResult.sidedSuccess(false);
        }

        // 封口：压缸石 / 羊毛地毯 / 粗布毯，该用哪一种看这一缸的配方
        if (seal(level, pos, vat, player, stack)) {
            return InteractionResult.sidedSuccess(false);
        }
        return InteractionResult.PASS;
    }

    /** 把一份材料放进缸里（数量够不够、收不收，进来之前已经由配方表判断过） */
    private boolean insert(Level level, BlockPos pos, VatBlockEntity vat, Player player,
                                  ItemStack stack, VatRecipe recipe, InteractionHand hand) {
        int slot = recipe.findSlotFor(stack, VatBrewing.counts(vat, null));
        if (slot < 0) {
        }
        // 从"已经做完"的上一条配方转过来（泡菜→辣白菜、大酱→酱油、泡菜→白醋、酱渣→醋）：
        // 先把完成标记清掉，重新计时
        if (vat.isFermented()) {
            level.setBlock(pos, level.getBlockState(pos).setValue(FERMENTED, false).setValue(PROGRESS, 0), 3);
        }
        // 投料阶段不认领 kind：此时材料未必配齐（先放盐的缸会暂时像鱼露）。
        // kind 只由 tryStart→ready 在材料+封口真正凑齐时设置，避免中途误认后翻不回来。
        VatRecipe.Slot recipeSlot = recipe.slots().get(slot);
        // 把这份东西记进缸里：渲染器和 Jade 都从 contents 读，光扣玩家物品不记账缸里就永远是空的
        vat.addContent(stack);
        if (recipeSlot.perDose() && SeasoningBottleItem.isBottle(stack)) {
            // 带余量的瓶装调料（鱼露、虾酱）：缸里只记一份剂量，
            // 瓶子扣一份之后直接放回玩家手里（不走 give，避免跳到别的格子）；
            // 正好用空就变成空玻璃瓶
            if (!player.getAbilities().instabuild) {
                player.setItemInHand(hand, SeasoningBottleItem.use(stack, SeasoningBottleItem.DOSE_MB));
            }
            consume(player, stack);
        } else {
            // 连瓶 / 连碗扔进来的调料（辣椒酱）：空容器当场还给玩家
            Item refund = recipeSlot.refund();
            if (refund != null && !player.getAbilities().instabuild) {
                give(player, new ItemStack(refund));
            }
            consume(player, stack);
        }
        playFill(level, pos);
        refreshSalted(level, pos, vat);
        tryStart(level, pos, vat);
        return true;
    }

    /**
     * 封口：按这一缸的配方决定压石头还是蒙毯子。
     *
     * <p>压缸石给泡菜 / 腊肉 / 咸鱼 / 鱼露 / 虾酱，羊毛地毯给大酱 / 酱油，粗布毯给醋 / 豆芽 / 酸玉米粒 / 格瓦斯。
     */
    private boolean seal(Level level, BlockPos pos, VatBlockEntity vat, Player player, ItemStack stack) {
        if (vat.isEmpty()) {
            return false;
        }
        VatRecipe recipe = VatBrewing.current(vat);
        if (recipe == null) {
            return false;
        }
        boolean press = recipe.seal() == VatRecipe.Seal.PRESS && !vat.isPressed()
                && (isPressStone(stack) || matchesRequiredSealItem(recipe, stack));
        boolean carpet = recipe.seal() == VatRecipe.Seal.CARPET && stack.is(ItemTags.WOOL_CARPETS)
                && !vat.isCovered();
        boolean cloth = recipe.seal() == VatRecipe.Seal.CLOTH && isClothRug(stack) && !vat.isCovered();
        if (!press && !carpet && !cloth) {
            return false;
        }
        if (press) {
            vat.setPress(stack);
        } else {
            vat.setCover(stack);
        }
        consume(player, stack);
        playFill(level, pos);
        tryStart(level, pos, vat);
        // 封上了但材料还没凑齐：给一句提示，免得看起来像"点坏了"
        if (VatBrewing.ready(vat) == null) {
            hint(player, "message.northeast_china_delight.vat.materials_not_ready");
        }
        return true;
    }

    /** 潜行右键：先还石头、再还盖布，一次右键取一样 */
    private static @NotNull InteractionResult takeSeal(Level level, BlockPos pos, BlockState state,
                                                      VatBlockEntity vat, Player player) {
        if (vat.isPressed()) {
            if (!level.isClientSide) {
                give(player, vat.takePress());
                if (!state.getValue(FERMENTED)) {
                    level.setBlock(pos, state.setValue(PROGRESS, 0), 3);
                }
                level.playSound(null, pos, SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        if (vat.isCovered()) {
            if (!level.isClientSide) {
                give(player, vat.takeCover());
                if (!state.getValue(FERMENTED)) {
                    level.setBlock(pos, state.setValue(PROGRESS, 0), 3);
                }
                level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        return InteractionResult.PASS;
    }

    /** 空手右键：把当前发酵进度显示在物品栏上方 */
    private static void showProgress(Player player, Level level, BlockPos pos, BlockState state, VatBlockEntity vat) {
        // 已经完成：提示成品怎么处理
        if (state.getValue(FERMENTED)) {
            hint(player, "message.northeast_china_delight.vat.progress.done");
            return;
        }
        // 还没开工：看这一缸现在像哪条配方、缺什么（认不出来就不打扰玩家）
        VatRecipe recipe = VatBrewing.current(vat);
        if (recipe == null || vat.kind() == VatRecipes.Kind.NONE) {
            return;
        }
        int progress = state.getValue(PROGRESS);
        int total = Vat.MAX_PROGRESS;
        int percent = Math.min(100, progress * 100 / total);
        int stepTicks = VatBrewing.stepTicks(recipe, vat);
        int remainTicks = (total - progress) * stepTicks;
        long seconds = remainTicks / 20L;
        if (seconds >= 60) {
            player.displayClientMessage(Component.translatable(
                    "message.northeast_china_delight.vat.progress.hint",
                    percent, seconds / 60, seconds % 60), true);
        } else {
            player.displayClientMessage(Component.translatable(
                    "message.northeast_china_delight.vat.progress.hint_seconds",
                    percent, seconds), true);
        }
    }

    /** 空手右键：取回石头/地毯、取出成品、逐个取泡菜 */
    //? if >=1.20.5 {
    /*@Override
    protected @NotNull InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                        Player player, BlockHitResult hit) {
        return vatUseWithdrawn(state, level, pos, player, hit);
    }
    *///?}

    /** 空手右键：取回石头/地毯、取出成品、逐个取泡菜（两个版本共用） */
    private @NotNull InteractionResult vatUseWithdrawn(BlockState state, Level level, BlockPos pos,
                                                       Player player, BlockHitResult hit) {
        VatBlockEntity vat = vatAt(level, pos);
        if (vat == null || vat.isEmpty()) {
            return InteractionResult.PASS;
        }

        // 潜行空手右键：把封口物取回来（石头 / 羊毛毯 / 粗布毯 / 雪块），一次右键取一样
        if (player.isSecondaryUseActive() && (vat.isPressed() || vat.isCovered())) {
            if (!level.isClientSide) {
                if (vat.isPressed()) {
                    give(player, vat.takePress());
                    level.playSound(null, pos, SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
                } else {
                    give(player, vat.takeCover());
                    level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
                }
                if (!state.getValue(FERMENTED)) {
                    level.setBlock(pos, state.setValue(PROGRESS, 0), 3);
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }

        // 还没酿好：普通右键只报进度，不取石头也不取货
        if (!state.getValue(FERMENTED)) {
            if (!level.isClientSide) {
                showProgress(player, level, pos, state, vat);
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }

        // 酿好了但还封着口：普通右键也能把封口物取下来（取下来才拿得到里面的东西）
        if (vat.isPressed() || vat.isCovered()) {
            if (!level.isClientSide) {
                if (vat.isPressed()) {
                    give(player, vat.takePress());
                    level.playSound(null, pos, SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
                } else {
                    give(player, vat.takeCover());
                    level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }

        // 缸里还有成品液体（大酱 / 酱油 / 醋 / 白醋 / 鱼露 / 虾酱 / 格瓦斯）时，只能用对应的容器装。
        // 泡菜缸例外（它的成品是酸引水，菜可以照样空手拿走）——
        // 判断依据就是配方里有没有"取出来才有成品"的格子。
        VatRecipe recipe = VatRecipes.recipeOf(vat.kind());
        if (vat.hasProductLiquid() && (recipe == null || recipe.keepSlots().isEmpty())) {
            if (!level.isClientSide) {
                hint(player, liquidHintKey(vat.kind()));
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }

        // 液体取空之后（泡菜缸是随时），空手取内容物：泡菜、腊肉、咸鱼、酱渣、豆芽、酸玉米粒…
        // 取哪一样、取出来是什么，全部由配方里的格子（Slot#take）决定。
        if (!level.isClientSide) {
            List<ItemStack> refunds = new ArrayList<>();
            ItemStack taken = VatBrewing.takeOne(vat, refunds);
            for (ItemStack refund : refunds) {
                give(player, refund);
            }
            if (!taken.isEmpty()) {
                give(player, taken);
                level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.6F, 1.4F);
            }
            // 内容物和液体都空了，才把缸恢复成空缸
            if (vat.contents().isEmpty() && !vat.hasProductLiquid()) {
                reset(level, pos, state, vat);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    /** 计时结束 */
    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        VatBlockEntity vat = vatAt(level, pos);
        if (vat == null || vat.kind() == VatRecipes.Kind.NONE || state.getValue(FERMENTED)) {
            return;
        }

        // 每一步都重新按配方表确认一遍：石头被拿走了、或者料被取走一份，就该停下来
        VatRecipe recipe = VatBrewing.ready(vat);
        if (recipe == null) {
            return;
        }

        int progress = state.getValue(PROGRESS) + 1;
        if (progress >= MAX_PROGRESS) {
            // 完成：液体怎么变、产出什么、每格的东西变成什么，全按配方表执行
            VatBrewing.complete(level, pos, vat, recipe);
        } else {
            level.setBlock(pos, state.setValue(PROGRESS, progress), Block.UPDATE_CLIENTS);
            level.scheduleTick(pos, this, VatBrewing.stepTicks(recipe, vat));
        }
    }

    /** 大缸被破坏时把里面的东西吐出来 —— 除了已经"被吸收"的调料（盐、辣椒酱、鱼露、虾酱） */
    @Override
    //? if <1.20.2 {
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
    //?} else {
    /*public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
    *///?}
        VatBlockEntity destroyed = vatAt(level, pos);
        if (!level.isClientSide && destroyed != null) {
            VatBlockEntity vat = destroyed;
            for (ItemStack content : vat.contents()) {
                if (isAbsorbedSeasoning(content)) {
                    continue;
                }
                popResource(level, pos, content);
            }
            give(player, vat.takePress());
            give(player, vat.takeCover());
        }
        //? if <1.20.2 {
        super.playerWillDestroy(level, pos, state, player);
        //?} else {
        /*return super.playerWillDestroy(level, pos, state, player);
        *///?}
    }

    /**
     * 这份内容物是不是"投进去就已经溶进缸里"的调料：
     * 盐在任何配方里都是调料；ABSORB / CONSUME 的格子（辣椒酱、鱼露、虾酱、小麦、谷物…）
     * 也都已经被大缸吸收，不该在破坏时掉出来。
     */
    private static boolean isAbsorbedSeasoning(ItemStack stack) {
        if (VatRecipes.isSalt(stack)) {
            return true;
        }
        for (VatRecipe recipe : VatRecipes.all()) {
            for (VatRecipe.Slot slot : recipe.slots()) {
                if (slot.fate() != VatRecipe.Fate.KEEP && slot.fate() != VatRecipe.Fate.CONVERT
                        && slot.matcher().test(stack)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void consume(Player player, ItemStack stack) {
        consume(player, stack, true);
    }

    private static void consume(Player player, ItemStack stack, boolean shrink) {
        if (shrink && !player.getAbilities().instabuild) {
            stack.shrink(1);
        }
    }

    private static void playFill(Level level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.COMPOSTER_FILL_SUCCESS, SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    /** 放不进去时给玩家一句提示，免得看起来像"点坏了" */
    private static void hint(Player player, String translationKey) {
        player.displayClientMessage(Component.translatable(translationKey), true);
    }

    /** 缸里有成品液体时，空手右键提示要用什么容器装 */
    private static String liquidHintKey(VatRecipes.Kind kind) {
        return switch (kind) {
            case PASTE -> "message.northeast_china_delight.vat.need_container";
            case VINEGAR -> "message.northeast_china_delight.vat.need_bottle_vinegar";
            case WHITE_VINEGAR -> "message.northeast_china_delight.vat.need_bottle_white_vinegar";
            case FISH_SAUCE -> "message.northeast_china_delight.vat.need_bottle_fish_sauce";
            case SHRIMP_PASTE -> "message.northeast_china_delight.vat.need_bottle_shrimp_paste";
            case PICKLE, SPICY_PICKLE -> "message.northeast_china_delight.vat.need_bottle_sour_water";
            case KVASS -> "message.northeast_china_delight.vat.need_bottle_kvass";
            default -> "message.northeast_china_delight.vat.need_bottle";
        };
    }

    /**
     * 手里拿着**空玻璃瓶**右键大缸 = 往外装液体，不算往缸里加东西 ——
     * 即使缸上压着石头（泡菜腌好之后石头还留在缸上）也照样生效，酸引水就是这么装走的。
     *
     * <p>注意这里**故意**不收"用过的调料瓶 / 酸引水瓶"：那种瓶子点上去走的是"往缸里倒"，
     * 压着石头时应该继续被挡住。
     */
    private static boolean isBottleTake(ItemStack stack) {
        return stack.is(Items.GLASS_BOTTLE);
    }
}
