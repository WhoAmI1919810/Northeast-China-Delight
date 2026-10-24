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
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
//? if >=1.20.5 {
import net.minecraft.world.ItemInteractionResult;
//?}
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
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
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 大缸。内容物（蔬菜、肉、盐、酱块）、压缸石、蒙缸地毯都记在 {@link VatBlockEntity} 里，会按实际物品渲染在缸内，不需要打开界面就能看见里面有什么。这里只负
 * 责交互流程；具体数值与规则见 {@link VatRecipes}。
 */
public class Vat extends Block implements EntityBlock {

    public static final IntegerProperty WATER_LEVEL = ModBlockStateProperties.WATER_LEVEL;
    public static final BooleanProperty FERMENTED = ModBlockStateProperties.VAT_FERMENTED;
    public static final BooleanProperty SALTED = ModBlockStateProperties.VAT_SALTED;
    public static final IntegerProperty PROGRESS = ModBlockStateProperties.VAT_PROGRESS;
    public static final int MAX_PROGRESS = ModBlockStateProperties.VAT_MAX_PROGRESS;

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

    /** 比较器输出发酵进度：没开始 0，做完 15，中间按进度涨 */
    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof VatBlockEntity vat ? vat.comparatorOutput() : 0;
    }

    /** 和堆肥桶空缸一致：底厚 2 像素，四壁厚 2 像素，缸口挖到顶。 */
    private static final VoxelShape OPEN_SHAPE = Shapes.join(
            Shapes.block(),
            Block.box(2.0, 2.0, 2.0, 14.0, 16.0, 14.0),
            BooleanOp.ONLY_FIRST);

    private static boolean productionStarted(BlockState state) {
        return state.getValue(FERMENTED) || state.getValue(PROGRESS) > 0;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return productionStarted(state) ? Shapes.block() : OPEN_SHAPE;
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

    @Nullable
    private static VatBlockEntity vatAt(Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof VatBlockEntity vat ? vat : null;
    }

    /** 手里拿的是不是可以压缸的石头类方块（必须是完整方块：台阶/墙/栅栏这种不算）。封口判定在 VatBrewing，这里是 public */
    public static boolean isPressStone(ItemStack stack) {
        return stack.getItem() instanceof BlockItem blockItem
                && blockItem.getBlock().defaultBlockState().is(PRESS_STONES)
                && blockItem.getBlock().defaultBlockState().isSolid();
    }

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
        if (vat.hasMissingUnfinishedRecipe()) {
            return false;
        }
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
            case WHITE_VINEGAR -> !state.getValue(FERMENTED);
            default -> false;
        };
    }

    public static boolean hasEnoughSalt(VatBlockEntity vat) {
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
     *
     * @return 这一缸现在是不是在做（刚开工、本来就在做，或者创造大缸当场做完）
     */
    protected boolean tryStart(Level level, BlockPos pos, VatBlockEntity vat) {
        BlockState state = level.getBlockState(pos);
        boolean fermented = state.getValue(FERMENTED);
        if (!fermented && state.getValue(PROGRESS) > 0) {
            if (VatBrewing.ready(vat) != null) {
                return true;
            }
            // 配方被数据包删掉时保留绑定和进度，交给空手右键的「返还原料」流程处理。
            if (vat.hasMissingUnfinishedRecipe()) {
                return false;
            }
            // 进度已经跑起来、配方却因数据包改动或误操作不再匹配：把进度退回 0，
            // 否则 tryStart 永远因为 progress > 0 提前返回，这一缸就再也没有开工机会。
            level.setBlock(pos, state.setValue(PROGRESS, 0), 3);
            vat.clearRecipeBinding();
            state = level.getBlockState(pos);
        }
        VatRecipe recipe = VatBrewing.ready(vat);
        if (recipe == null || (fermented && VatBrewing.isCurrent(vat, recipe))) {
            return false;
        }
        if (fermented) {
            level.setBlock(pos, state.setValue(FERMENTED, false).setValue(PROGRESS, 0), 3);
        }
        vat.setRecipe(recipe);
        level.scheduleTick(pos, level.getBlockState(pos).getBlock(), VatBrewing.stepTicks(recipe, vat));
        return true;
    }

    /** 现在能不能再往缸里倒一桶水 */
    private static boolean canAddWater(VatBlockEntity vat) {
        if (vat.hasMissingUnfinishedRecipe()) {
            return false;
        }
        if (vat.waterLayers() >= ModBlockStateProperties.VAT_MAX_WATER) {
            return false;
        }
        // 酸引水和清水不能混：混了水位会算错，水还会凭空少掉
        if (vat.sourWaterMb() > 0) {
            return false;
        }
        if (!vat.isFermented() && (vat.recipeId() != null
                || vat.getBlockState().getValue(PROGRESS) > 0)) {
            // 已经开始计时的配方可能只要 0~1 层水（冻梨、豆芽等）。这时再倒水会超过
            // water_max，ready() 返回 null，而 tick 又不会继续排程 —— 正是最典型的卡死路径。
            VatRecipe recipe = VatRecipes.recipeOf(vat);
            return recipe != null && vat.waterLayers() < recipe.liquid().waterMax();
        }
        return !vat.isFermented() || VatBrewing.canContinueWithWater(vat);
    }

    private static void give(Player player, ItemStack stack) {
        if (!stack.isEmpty() && !player.addItem(stack)) {
            player.drop(stack, false);
        }
    }

    public static void reset(Level level, BlockPos pos, BlockState state, VatBlockEntity vat) {
        vat.clearContents();
        level.setBlock(pos, state
                .setValue(WATER_LEVEL, 0)
                .setValue(FERMENTED, false)
                .setValue(SALTED, false)
                .setValue(PROGRESS, 0), 3);
    }

    @Override
    //? if <1.20.5 {
    /*public @NotNull InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        ItemStack stack = player.getItemInHand(hand);
        InteractionResult itemResult = vatUseItem(stack, state, level, pos, player, hand, hit);
        if (itemResult != InteractionResult.PASS || hand != InteractionHand.MAIN_HAND) {
            return itemResult;
        }
        // 1.21.1 里这是两个方法：useItemOn 答「交给默认交互」时才轮到「空手右键」这一套
        return vatUseWithdrawn(state, level, pos, player, hit);
    }*/
    //?} else {
    protected @NotNull ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                                       Player player, InteractionHand hand, BlockHitResult hit) {
        InteractionResult result = vatUseItem(stack, state, level, pos, player, hand, hit);
        return result == InteractionResult.PASS
                ? ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
                : ItemInteractionResult.sidedSuccess(level.isClientSide());
    }
    //?}

    /** 拿着东西右键大缸（两个版本共用的实现，由上面的壳子翻译返回值） */
    private @NotNull InteractionResult vatUseItem(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                                  Player player, InteractionHand hand, BlockHitResult hit) {
        VatBlockEntity vat = vatAt(level, pos);
        if (vat == null) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            resetStalledProgress(level, pos, state, vat);
            state = level.getBlockState(pos);
        }

        if (player.isSecondaryUseActive() && (vat.isPressed() || vat.isCovered())) {
            return takeSeal(level, pos, state, vat, player);
        }

        // 压着石头时是「锁住」状态：不能往里放任何东西，必须先取出石头。
        // 例外：手里拿玻璃瓶 / 调料瓶时，右键是往外取液体，不算"往缸里加东西"，
        // 不该被压缸石挡住 —— 泡菜腌好后石头还压在缸上，酸引水就是要能这样装走的。
        if (vat.isPressed() && !isBottleTake(stack)) {
            if (!level.isClientSide && !stack.isEmpty()) {
                hint(player, "message.northeast_china_delight.vat.locked_by_press");
            }
            return InteractionResult.PASS;
        }

        if (stack.isEmpty() && vat.hasMissingUnfinishedRecipe()) {
            return returnRemovedRecipe(level, pos, state, vat, player);
        }

        if (stack.is(Items.WATER_BUCKET) && canAddWater(vat)) {
            if (!level.isClientSide) {
                vat.addWater(VatRecipes.WATER_MB_PER_LEVEL);
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                    player.addItem(new ItemStack(Items.BUCKET));
                }
                level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
                refreshSalted(level, pos, vat);
                // 水也是配方的一部分：先放料、后倒水（或者先盖好盖布再倒水）时，
                // 这一下同样要让条件齐了的配方开工，否则缸会一直停在"材料没配齐"
                tryStart(level, pos, vat);
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }

        // 客户端不做预测改动：一切判定与改动都在服务端做（和以前的写法一致）
        if (level.isClientSide) {
            if (keepLeftovers(vat, stack)) {
                return InteractionResult.sidedSuccess(true);
            }
            return InteractionResult.PASS;
        }

        if (stack.isEmpty()) {
            return InteractionResult.PASS;
        }

        if (stack.is(ModItems.SOUR_WATER.get()) && canPourSourWater(vat, state)) {
            if (vat.kind() == VatRecipes.Kind.NONE) {
                vat.setKind(VatRecipes.Kind.PICKLE);
            }
            vat.addSourWater(VatRecipes.SERVING_MB);
            consume(player, stack);
            give(player, new ItemStack(Items.GLASS_BOTTLE));
            playFill(level, pos);
            tryStart(level, pos, vat);
            return InteractionResult.sidedSuccess(false);
        }

        if (SeasoningBottleItem.isBottle(stack) && vat.isFermented()) {
            if (SeasoningBottleItem.isFull(stack)) {
                hint(player, "message.northeast_china_delight.vat.bottle_full");
                return InteractionResult.sidedSuccess(false);
            }
            if (VatRecipes.bottleFor(vat) != stack.getItem()) {
                hint(player, "message.northeast_china_delight.vat.bottle_mismatch");
                return InteractionResult.sidedSuccess(false);
            }
            int moved = vat.drainProduct(SeasoningBottleItem.usedMb(stack));
            if (moved <= 0) {
                hint(player, "message.northeast_china_delight.vat.bottle_empty");
                return InteractionResult.sidedSuccess(false);
            }
            SeasoningBottleItem.refill(stack, moved);
            playFill(level, pos);
            vat.resetIfDrained();
            return InteractionResult.sidedSuccess(false);
        }

        ItemStack filled = VatBrewing.fillContainer(vat, stack);
        if (filled != null) {
            boolean bowl = stack.is(Items.BOWL);
            consume(player, stack);
            give(player, filled);
            level.playSound(null, pos, bowl
                    ? SoundEvents.HONEY_BLOCK_SLIDE : SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
            // 取完最后一份就复位，下一次投料不用先空点一下
            vat.resetIfDrained();
            return InteractionResult.sidedSuccess(false);
        }

        VatRecipe forInsert = VatBrewing.forInsert(vat, stack);
        if (forInsert != null && insert(level, pos, vat, player, stack, forInsert, hand)) {
            return InteractionResult.sidedSuccess(false);
        }

        if (seal(level, pos, vat, player, stack)) {
            return InteractionResult.sidedSuccess(false);
        }
        if (keepLeftovers(vat, stack)) {
            return InteractionResult.sidedSuccess(false);
        }
        return InteractionResult.PASS;
    }

    /** 已完成的缸里还有下一道工序可能要用的东西时，拿错物品不能顺势把它们取出。 */
    private static boolean keepLeftovers(VatBlockEntity vat, ItemStack stack) {
        return !stack.isEmpty() && vat.isFermented()
                && (!vat.contents().isEmpty() || vat.hasProductLiquid());
    }

    private boolean insert(Level level, BlockPos pos, VatBlockEntity vat, Player player,
                                  ItemStack stack, VatRecipe recipe, InteractionHand hand) {
        int slot = recipe.findSlotFor(stack, VatBrewing.counts(vat, null));
        if (slot < 0) {
            return false;
        }
        // 投料阶段不认领 kind：此时材料未必配齐（先放盐的缸会暂时像鱼露）。
        // kind 只由 tryStart→ready 在材料+封口真正凑齐时设置，避免中途误认后翻不回来。
        VatRecipe.Slot recipeSlot = recipe.slots().get(slot);
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

    private boolean seal(Level level, BlockPos pos, VatBlockEntity vat, Player player, ItemStack stack) {
        if (vat.isEmpty()) {
            return false;
        }
        VatRecipe recipe = VatBrewing.sealRecipe(vat, stack);
        if (recipe == null) {
            return false;
        }
        boolean press = recipe.seal() == VatRecipe.Seal.PRESS;
        if (press ? vat.isPressed() : vat.isCovered()) {
            return false;
        }
        if (press) {
            vat.setPress(stack);
        } else {
            vat.setCover(stack);
        }
        consume(player, stack);
        playFill(level, pos);
        // 创造大缸当场做完后 ready() 已经是 null，所以看 tryStart 的结果，不再看 ready()
        if (!tryStart(level, pos, vat)) {
            hint(player, VatBrewing.onlyNeedsSnowyBiome(vat)
                    ? "message.northeast_china_delight.vat.needs_snowy_biome"
                    : "message.northeast_china_delight.vat.materials_not_ready");
        }
        return true;
    }

    private static @NotNull InteractionResult takeSeal(Level level, BlockPos pos, BlockState state,
                                                      VatBlockEntity vat, Player player) {
        if (vat.isPressed()) {
            if (!level.isClientSide) {
                give(player, vat.takePress());
                interrupt(level, pos, state, vat);
                level.playSound(null, pos, SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        if (vat.isCovered()) {
            if (!level.isClientSide) {
                give(player, vat.takeCover());
                interrupt(level, pos, state, vat);
                level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        return InteractionResult.PASS;
    }

    /**
     * 没做完就揭开封口：进度清零，并放开配方绑定。揭盖多半是要补料或改做别的，
     * 还锁在原配方上的话，大酱补不进小麦、咸鱼加不成鱼露。配方被数据包删掉时保留绑定，空手右键才能退回原料。
     */
    private static void interrupt(Level level, BlockPos pos, BlockState state, VatBlockEntity vat) {
        if (state.getValue(FERMENTED)) {
            return;
        }
        level.setBlock(pos, state.setValue(PROGRESS, 0), 3);
        if (!vat.hasMissingUnfinishedRecipe()) {
            vat.releaseUnfinishedRecipe();
        }
    }

    private static void showProgress(Player player, Level level, BlockPos pos, BlockState state, VatBlockEntity vat) {
        if (state.getValue(FERMENTED)) {
            return;
        }
        VatRecipe recipe = VatRecipes.recipeOf(vat);
        if (recipe == null && vat.recipeId() == null) {
            recipe = VatBrewing.current(vat);
        }
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
                    recipe.hintKey(),
                    recipe.verb(), percent, seconds / 60, seconds % 60), true);
        } else {
            player.displayClientMessage(Component.translatable(
                    recipe.hintSecondsKey(),
                    recipe.verb(), percent, seconds), true);
        }
    }

    /** 空手右键：取回石头/地毯、取出成品、逐个取泡菜 */
    //? if >=1.20.5 {
    @Override
    protected @NotNull InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                        Player player, BlockHitResult hit) {
        return vatUseWithdrawn(state, level, pos, player, hit);
    }
    //?}

    /** 空手右键：取回石头/地毯、取出成品、逐个取泡菜（两个版本共用） */
    private @NotNull InteractionResult vatUseWithdrawn(BlockState state, Level level, BlockPos pos,
                                                       Player player, BlockHitResult hit) {
        VatBlockEntity vat = vatAt(level, pos);
        // 取空后复位的缸可能还压着石头 / 盖着布，空缸也得让玩家把封口物拿回去
        if (vat == null || (vat.isEmpty() && !vat.isPressed() && !vat.isCovered())) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            resetStalledProgress(level, pos, state, vat);
            state = level.getBlockState(pos);
        }

        if (player.isSecondaryUseActive() && (vat.isPressed() || vat.isCovered())) {
            if (!level.isClientSide) {
                if (vat.isPressed()) {
                    give(player, vat.takePress());
                    level.playSound(null, pos, SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
                } else {
                    give(player, vat.takeCover());
                    level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
                }
                interrupt(level, pos, state, vat);
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }

        if (vat.hasMissingUnfinishedRecipe()) {
            return returnRemovedRecipe(level, pos, state, vat, player);
        }

        // 还没开工就盖上的布/石头，普通空手右键即可取回；
        // 已经开工的缸仍然只显示进度，避免误触把计时清零（要中断请 Shift 右键）。
        if (!state.getValue(FERMENTED) && state.getValue(PROGRESS) == 0 && vat.recipeId() == null
                && (vat.isPressed() || vat.isCovered())) {
            return takeSeal(level, pos, state, vat, player);
        }

        if (!state.getValue(FERMENTED)) {
            if (!level.isClientSide) {
                showProgress(player, level, pos, state, vat);
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }

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

        if (!level.isClientSide) {
            List<ItemStack> refunds = new ArrayList<>();
            ItemStack taken = VatBrewing.takeOne(vat, refunds);
            for (ItemStack refund : refunds) {
                give(player, refund);
            }
            if (!taken.isEmpty()) {
                give(player, taken);
                level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.6F, 1.4F);
            } else if (vat.hasProductLiquid()) {
                showContainerHint(player, vat);
            }
            if (vat.contents().isEmpty() && !vat.hasProductLiquid()) {
                reset(level, pos, state, vat);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    private static InteractionResult returnRemovedRecipe(Level level, BlockPos pos, BlockState state,
                                                         VatBlockEntity vat, Player player) {
        if (!level.isClientSide) {
            give(player, vat.takePress());
            give(player, vat.takeCover());
            for (ItemStack content : vat.contentSnapshot()) {
                give(player, content);
            }
            reset(level, pos, state, vat);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        VatBlockEntity vat = vatAt(level, pos);
        if (vat == null || vat.kind() == VatRecipes.Kind.NONE || state.getValue(FERMENTED)) {
            return;
        }

        VatRecipe recipe = VatBrewing.ready(vat);
        if (recipe == null) {
            // 没有合法配方时不能停在 PROGRESS > 0 或旧绑定上：那会让 tryStart 永远提前返回。
            // 但"配方被删掉"要保留原状态（resetStalledProgress 不动它），玩家空手右键才能把所有原料拿回来。
            if (vat.hasMissingUnfinishedRecipe()) {
                return;
            }
            resetStalledProgress(level, pos, state, vat);
            // 解绑后要是另一条配方已经齐了，接着开工，不用等玩家再点一下
            tryStart(level, pos, vat);
            return;
        }

        int progress = state.getValue(PROGRESS) + 1;
        if (progress >= MAX_PROGRESS) {
            VatBrewing.complete(level, pos, vat, recipe);
        } else {
            // UPDATE_CLIENTS 只同步客户端，比较器要的邻居更新得单独加
            level.setBlock(pos, state.setValue(PROGRESS, progress),
                    Block.UPDATE_CLIENTS | Block.UPDATE_NEIGHBORS);
            level.scheduleTick(pos, this, VatBrewing.stepTicks(recipe, vat));
        }
    }

    @Override
    //? if <1.20.2 {
    /*public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
    *///?} else {
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
    //?}
        VatBlockEntity vat = vatAt(level, pos);
        if (!level.isClientSide && vat != null) {
            // 封口物直接还给玩家；缸里的东西在 onRemove 里掉落，爆炸、指令拆掉时也一样
            give(player, vat.takePress());
            give(player, vat.takeCover());
        }
        //? if <1.20.2 {
        /*super.playerWillDestroy(level, pos, state, player);
        *///?} else {
        return super.playerWillDestroy(level, pos, state, player);
        //?}
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide) {
            VatBlockEntity vat = vatAt(level, pos);
            if (vat != null) {
                for (ItemStack content : vat.contents()) {
                    if (!isAbsorbedSeasoning(vat, content)) {
                        popResource(level, pos, content.copy());
                    }
                }
                // 玩家拆缸时封口物已经在 playerWillDestroy 里还回去了，这里只剩别的拆法
                if (vat.isPressed()) {
                    popResource(level, pos, vat.press().copy());
                }
                if (vat.isCovered()) {
                    popResource(level, pos, vat.cover().copy());
                }
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /**
     * 这份内容物是不是"投进去就已经溶进缸里"的调料：盐在任何配方里都是调料；
     * 这缸所走配方里 ABSORB / CONSUME 的调料格（辣椒酱、鱼露、虾酱、小麦、谷物…）也已经被大缸吸收，不该在破坏时掉出来。
     * 生鱼、虾、面包、酱渣这些主料没做完前还是原样，拆缸照常掉出。还没绑定配方的缸按它眼下最像的那条配方（{@link VatBrewing#current}）算。
     */
    private static boolean isAbsorbedSeasoning(VatBlockEntity vat, ItemStack stack) {
        if (VatRecipes.isSalt(stack)) {
            return true;
        }
        VatRecipe recipe = vat.recipeId() != null ? VatRecipes.recipeOf(vat) : VatBrewing.current(vat);
        if (recipe == null) {
            return false;
        }
        for (VatRecipe.Slot slot : recipe.slots()) {
            if (slot.layer() == VatRecipe.Layer.SEASONING
                    && slot.fate() != VatRecipe.Fate.KEEP && slot.fate() != VatRecipe.Fate.CONVERT
                    && slot.matcher().test(stack)) {
                return true;
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

    private static void hint(Player player, String translationKey) {
        player.displayClientMessage(Component.translatable(translationKey), true);
    }

    private static void showContainerHint(Player player, VatBlockEntity vat) {
        VatRecipes.Serving serving = VatRecipes.servingOf(vat);
        if (serving != null) {
            player.displayClientMessage(Component.translatable(
                    "message.northeast_china_delight.vat.take_with_container",
                    new ItemStack(serving.container()).getHoverName()), true);
        }
    }

    /**
     * 没做完的缸已经计了进度、或者绑着配方，却没有任何配方能匹配时，退回未开工状态并解除旧绑定，
     * 保证玩家还能补料、重新封口开工。只绑定没计进度的情况（封口后第一次 tick 前就被打断、旧版本管道抽空留下的缸）也算。
     */
    private static void resetStalledProgress(Level level, BlockPos pos, BlockState state, VatBlockEntity vat) {
        if (state.getValue(FERMENTED) || (state.getValue(PROGRESS) == 0 && vat.recipeId() == null)
                || vat.hasMissingUnfinishedRecipe() || VatBrewing.ready(vat) != null) {
            return;
        }
        if (state.getValue(PROGRESS) > 0) {
            level.setBlock(pos, state.setValue(PROGRESS, 0),
                    Block.UPDATE_CLIENTS | Block.UPDATE_NEIGHBORS);
        }
        vat.clearRecipeBinding();
    }

    /**
     * 手里拿着空玻璃瓶右键大缸 = 往外装液体，不算往缸里加东西 ——
     * 即使缸上压着石头（泡菜腌好之后石头还留在缸上）也照样生效，酸引水就是这么装走的。
     *
     * <p>注意这里故意不收"用过的调料瓶 / 酸引水瓶"：那种瓶子点上去走的是"往缸里倒"，
     * 压着石头时应该继续被挡住。
     */
    private static boolean isBottleTake(ItemStack stack) {
        return stack.is(Items.GLASS_BOTTLE);
    }
}
