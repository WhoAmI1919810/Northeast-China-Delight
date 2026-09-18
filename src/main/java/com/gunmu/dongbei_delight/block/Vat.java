package com.gunmu.dongbei_delight.block;

import com.gunmu.dongbei_delight.DongbeiDelight;
import com.gunmu.dongbei_delight.crafting.VatRecipes;
import com.gunmu.dongbei_delight.item.ModItems;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
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
            ResourceLocation.fromNamespaceAndPath(DongbeiDelight.MODID, "vat_press_stones"));

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

    /** 手里拿的是不是可以压缸的石头类方块 */
    private static boolean isPressStone(ItemStack stack) {
        return stack.getItem() instanceof BlockItem blockItem
                && blockItem.getBlock().defaultBlockState().is(PRESS_STONES);
    }

    /** 这次处理要几个腌制单位：泡水类按水位算（满水 3 层 = 3 个单位 = 6 秒），干腌固定 1 个单位 */
    private static int totalUnits(BlockState state, VatBlockEntity vat) {
        return switch (vat.kind()) {
            case PICKLE, DOUGH -> Math.max(1, state.getValue(WATER_LEVEL));
            default -> 1;
        };
    }

    /** 处理过程中会不会抽走缸里的水（只有泡菜和水面团会；酿酱、酱油、干腌都不动水） */
    private static boolean drainsWater(VatRecipes.Kind kind) {
        return kind == VatRecipes.Kind.PICKLE || kind == VatRecipes.Kind.DOUGH;
    }

    private static int stepTicks(BlockState state, VatBlockEntity vat) {
        return Math.max(1, totalUnits(state, vat) * VatRecipes.UNIT_TICKS / MAX_PROGRESS);
    }

    /**
     * 当前水量的盐够不够。
     * 泡菜：一层水配一份盐，盐数必须 ≥ 水位；大酱：一份盐就够。
     * 客户端渲染「发白的盐水」时也用这个判断。
     */
    public static boolean hasEnoughSalt(VatBlockEntity vat, BlockState state) {
        int salt = vat.countOf(ModItems.SALT.get());
        return switch (vat.kind()) {
            case PICKLE -> salt >= state.getValue(WATER_LEVEL) * VatRecipes.SALT_PER_WATER;
            case PASTE, SOY_SAUCE -> salt >= VatRecipes.PASTE_SALT;
            default -> false;
        };
    }

    /** 重新判定盐水是否够咸，并把结果写进方块状态（状态一变，客户端水面颜色就会立刻刷新） */
    private static void refreshSalted(Level level, BlockPos pos, BlockState state, VatBlockEntity vat) {
        boolean salted = hasEnoughSalt(vat, state);
        if (state.getValue(SALTED) != salted) {
            level.setBlock(pos, state.setValue(SALTED, salted), 3);
        }
    }

    /** 条件齐了就开始计时 */
    private static void tryStart(Level level, BlockPos pos, BlockState state, VatBlockEntity vat) {
        if (state.getValue(FERMENTED) || level.getBlockState(pos).getValue(PROGRESS) > 0) {
            return;
        }
        if (readyToProcess(vat, state)) {
            level.scheduleTick(pos, state.getBlock(), stepTicks(state, vat));
        }
    }

    /** 是否满足「可以开始处理」的条件 */
    private static boolean readyToProcess(VatBlockEntity vat, BlockState state) {
        return switch (vat.kind()) {
            case PICKLE -> vat.isPressed()
                    && state.getValue(WATER_LEVEL) > 0
                    && hasEnoughSalt(vat, state)          // 盐不够就不开腌
                    && vat.vegetableCount() > 0;
            // 腊肉必须「一层肉一层盐」配对完成才能开腌：盐的数量要跟肉一样多
            case MEAT -> vat.isPressed()
                    && vat.meatCount() > 0
                    && vat.countOf(ModItems.SALT.get()) == vat.meatCount();
            // 大酱：满水 + 3 酱块 + 3 盐，蒙地毯
            case PASTE -> vat.isCovered()
                    && state.getValue(WATER_LEVEL) == 3
                    && vat.countOf(ModItems.SOY_PASTE_CHUNK.get()) >= VatRecipes.PASTE_CHUNKS
                    && vat.countOf(ModItems.SALT.get()) >= VatRecipes.PASTE_SALT;
            // 酱油：在大酱基础上再加一份小麦
            case SOY_SAUCE -> vat.isCovered()
                    && state.getValue(WATER_LEVEL) == 3
                    && vat.countOf(ModItems.SOY_PASTE_CHUNK.get()) >= VatRecipes.PASTE_CHUNKS
                    && vat.countOf(ModItems.SALT.get()) >= VatRecipes.PASTE_SALT
                    && vat.countOf(VatRecipes.wheatInput()) > 0;
            case DOUGH -> true;
            case NONE -> false;
        };
    }

    private static void give(Player player, ItemStack stack) {
        if (!stack.isEmpty() && !player.addItem(stack)) {
            player.drop(stack, false);
        }
    }

    /** 处理完成、内容清空后把大缸恢复成空缸 */
    private static void reset(Level level, BlockPos pos, BlockState state, VatBlockEntity vat) {
        vat.clearContents();
        level.setBlock(pos, state
                .setValue(WATER_LEVEL, 0)
                .setValue(FERMENTED, false)
                .setValue(SALTED, false)
                .setValue(PROGRESS, 0), 3);
    }

    // ===== 交互 =====

    @Override
    protected @NotNull ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                                       Player player, InteractionHand hand, BlockHitResult hit) {
        VatBlockEntity vat = vatAt(level, pos);
        if (vat == null) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        int water = state.getValue(WATER_LEVEL);

        // 压着石头时是「锁住」状态：不能往里放任何东西，必须先取出石头
        if (vat.isPressed()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        // 加水
        // 只要不在处理中就能加水：缸里还剩着没取完的菜时也允许续水
        if (stack.is(Items.WATER_BUCKET) && !state.getValue(FERMENTED) && water < 3) {
            if (!level.isClientSide) {
                level.setBlock(pos, state.setValue(WATER_LEVEL, water + 1), 3);
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                    player.addItem(new ItemStack(Items.BUCKET));
                }
                level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
                // 加了水之后盐可能又不够了，需要重新判定
                refreshSalted(level, pos, state.setValue(WATER_LEVEL, water + 1), vat);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }

        if (!level.isClientSide) {
            // 放盐：泡菜要先在水里放一份盐；腊肉要一层肉一层盐交替
            // 放盐：泡菜要按「一份水配一份盐」；腊肉要一层肉一层盐交替
            if (stack.is(ModItems.SALT.get())) {
                boolean ok = switch (vat.kind()) {
                    case NONE -> water > 0;                                   // 水里放盐 → 泡菜
                    case PICKLE -> vat.countOf(ModItems.SALT.get()) < water * VatRecipes.SALT_PER_WATER;
                    case MEAT -> vat.meatCount() < VatRecipes.MAX_MEATS
                            && vat.lastContent().is(VatRecipes.meatInput());  // 一层肉一层盐
                    case PASTE, SOY_SAUCE -> vat.countOf(ModItems.SALT.get()) < VatRecipes.PASTE_SALT;
                    case DOUGH -> false;
                };
                if (ok) {
                    if (vat.kind() == VatRecipes.Kind.NONE) {
                        vat.setKind(VatRecipes.Kind.PICKLE);
                    }
                    if (vat.addContent(stack)) {
                        consume(player, stack);
                        playFill(level, pos);
                        refreshSalted(level, pos, state, vat);
                        tryStart(level, pos, state, vat);
                        return ItemInteractionResult.sidedSuccess(false);
                    }
                }
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }

            // 放蔬菜：泡菜类，按「一份水配两份菜」限制（满水 = 6 份），可以混着放
            if (VatRecipes.isPickleIngredient(stack)) {
                if (vat.kind() == VatRecipes.Kind.PICKLE
                        && vat.countOf(ModItems.SALT.get()) > 0
                        && vat.vegetableCount() < water * VatRecipes.VEGETABLES_PER_WATER
                        && vat.addContent(stack)) {
                    consume(player, stack);
                    playFill(level, pos);
                    return ItemInteractionResult.sidedSuccess(false);
                }
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }

            // 放猪肉：腊肉类，不加水，最多 5 块，必须与盐交替
            if (stack.is(VatRecipes.meatInput())) {
                boolean ok = switch (vat.kind()) {
                    case NONE -> water <= 0;
                    case MEAT -> vat.meatCount() < VatRecipes.MAX_MEATS
                            && vat.lastContent().is(ModItems.SALT.get());
                    default -> false;
                };
                if (ok) {
                    if (vat.kind() == VatRecipes.Kind.NONE) {
                        vat.setKind(VatRecipes.Kind.MEAT);
                    }
                    refreshSalted(level, pos, state, vat);
                    if (vat.addContent(stack)) {
                        consume(player, stack);
                        playFill(level, pos);
                        tryStart(level, pos, state, vat);
                        return ItemInteractionResult.sidedSuccess(false);
                    }
                }
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }

            // 碎玉米粒泡水 → 水面团
            // 大酱块：先放进干缸，再放盐、倒水、蒙地毯
            // 酱块：最多放 3 块（第一块决定进入酿酱状态）
            if (stack.is(ModItems.SOY_PASTE_CHUNK.get()) && water <= 0
                    && (vat.kind() == VatRecipes.Kind.NONE || vat.kind() == VatRecipes.Kind.PASTE)
                    && vat.countOf(ModItems.SOY_PASTE_CHUNK.get()) < VatRecipes.PASTE_CHUNKS) {
                if (vat.addContent(stack)) {
                    vat.setKind(VatRecipes.Kind.PASTE);
                    consume(player, stack);
                    playFill(level, pos);
                    refreshSalted(level, pos, state, vat);
                    return ItemInteractionResult.sidedSuccess(false);
                }
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }

            // 酿酱油：放满 3 块酱之后，再放一份小麦就转成酱油
            if (stack.is(VatRecipes.wheatInput()) && vat.kind() == VatRecipes.Kind.PASTE
                    && vat.countOf(ModItems.SOY_PASTE_CHUNK.get()) >= VatRecipes.PASTE_CHUNKS
                    && vat.countOf(VatRecipes.wheatInput()) == 0) {
                if (vat.addContent(stack)) {
                    vat.setKind(VatRecipes.Kind.SOY_SAUCE);
                    consume(player, stack);
                    playFill(level, pos);
                    tryStart(level, pos, state, vat);
                    return ItemInteractionResult.sidedSuccess(false);
                }
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }

            // 水面团：一份水 + 两份玉米粒
            if (stack.is(VatRecipes.doughInput()) && vat.isEmpty() && water > 0 && stack.getCount() >= 2) {
                if (vat.addContent(stack) && vat.addContent(stack)) {
                    vat.setKind(VatRecipes.Kind.DOUGH);
                    if (!player.getAbilities().instabuild) {
                        stack.shrink(2);
                    }
                    playFill(level, pos);
                    tryStart(level, pos, state, vat);
                    return ItemInteractionResult.sidedSuccess(false);
                }
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }

            // 压石头：启动腌制
            // 压石头只用于泡菜和腊肉
            if (isPressStone(stack) && !vat.isEmpty() && !vat.isPressed()
                    && (vat.kind() == VatRecipes.Kind.PICKLE || vat.kind() == VatRecipes.Kind.MEAT)) {
                vat.setPress(stack);
                consume(player, stack);
                playFill(level, pos);
                tryStart(level, pos, state, vat);
                return ItemInteractionResult.sidedSuccess(false);
            }

            // 蒙羊毛地毯：启动大酱发酵
            // 蒙地毯用于大酱和酱油
            if (stack.is(ItemTags.WOOL_CARPETS)
                    && (vat.kind() == VatRecipes.Kind.PASTE || vat.kind() == VatRecipes.Kind.SOY_SAUCE)
                    && !vat.isCovered()) {
                vat.setCover(stack);
                consume(player, stack);
                playFill(level, pos);
                tryStart(level, pos, state, vat);
                return ItemInteractionResult.sidedSuccess(false);
            }

            // 用碗装大酱：一缸 10 碗，每装一碗液面下降一档
            if (stack.is(Items.BOWL) && vat.kind() == VatRecipes.Kind.PASTE
                    && state.getValue(FERMENTED) && vat.paste() > 0 && !vat.isCovered()) {
                vat.takeOnePaste();
                consume(player, stack);
                give(player, new ItemStack(ModItems.SOY_PASTE.get()));
                // 大酱是黏稠的，用蜂蜜的黏腻音效而不是水声
                level.playSound(null, pos, SoundEvents.HONEY_BLOCK_SLIDE, SoundSource.BLOCKS, 1.0F, 1.0F);
                if (vat.paste() <= 0) {
                    reset(level, pos, state, vat);
                }
                return ItemInteractionResult.sidedSuccess(false);
            }

            // 用玻璃瓶装酱油：一缸 10 瓶，每装一瓶液面下降一档
            if (stack.is(Items.GLASS_BOTTLE) && vat.kind() == VatRecipes.Kind.SOY_SAUCE
                    && state.getValue(FERMENTED) && vat.soySauce() > 0 && !vat.isCovered()) {
                vat.takeOneSoySauce();
                consume(player, stack);
                give(player, new ItemStack(ModItems.SOY_SAUCE.get()));
                level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
                if (vat.soySauce() <= 0) {
                    reset(level, pos, state, vat);
                }
                return ItemInteractionResult.sidedSuccess(false);
            }
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    /** 空手右键：取回石头/地毯、取出成品、逐个取泡菜 */
    @Override
    protected @NotNull InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                        Player player, BlockHitResult hit) {
        VatBlockEntity vat = vatAt(level, pos);
        if (vat == null || vat.isEmpty()) {
            return InteractionResult.PASS;
        }

        // 压着石头时右键：把石头取回来。石头是「锁」——取下来之后才能继续加东西。
        // 如果当时还没腌好，取石头等于中断，进度清零重新开始。
        if (vat.isPressed()) {
            if (!level.isClientSide) {
                give(player, vat.takePress());
                if (!state.getValue(FERMENTED)) {
                    level.setBlock(pos, state.setValue(PROGRESS, 0), 3);
                }
                level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }

        if (vat.isCovered() && state.getValue(FERMENTED)) {
            if (!level.isClientSide) {
                give(player, vat.takeCover());
                level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }

        if (!state.getValue(FERMENTED)) {
            return InteractionResult.PASS;
        }

        if (vat.kind() == VatRecipes.Kind.PASTE) {
            if (!level.isClientSide) {
                player.displayClientMessage(
                        Component.translatable("message.dongbei_delight.vat.need_container"), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }

        // 酱油必须用玻璃瓶装，空手只给提示
        if (vat.kind() == VatRecipes.Kind.SOY_SAUCE) {
            if (!level.isClientSide) {
                player.displayClientMessage(
                        Component.translatable("message.dongbei_delight.vat.need_bottle"), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }

        if (!level.isClientSide) {
            // 腌制完成、开始取货时，盐已经被吸收掉了，直接从缸里清掉
            vat.consumeSalt();
            boolean taken = switch (vat.kind()) {
                // 每右键一次取出一份泡菜（按放入顺序），取完为止
                case PICKLE -> takePickle(player, vat);
                case MEAT -> {
                    if (vat.removeOneMatching(s -> s.is(VatRecipes.meatInput()))) {
                        give(player, new ItemStack(VatRecipes.meatResult()));
                        yield true;
                    }
                    yield false;
                }
                case DOUGH -> {
                    if (vat.removeOneMatching(s -> s.is(VatRecipes.doughInput()))) {
                        give(player, new ItemStack(VatRecipes.doughResult()));
                        yield true;
                    }
                    yield false;
                }
                default -> false;
            };

            if (taken) {
                level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.6F, 1.4F);
                if (vat.vegetableCount() == 0 && vat.meatCount() == 0
                        && vat.count(VatRecipes::isPickleIngredient) == 0
                        && vat.count(s -> s.is(VatRecipes.doughInput())) == 0) {
                    reset(level, pos, state, vat);
                }
            } else {
                reset(level, pos, state, vat);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    /** 取出一份泡菜：从缸里挑一种蔬菜，给出对应的腌制成品 */
    private static boolean takePickle(Player player, VatBlockEntity vat) {
        for (ItemStack content : vat.contents()) {
            if (VatRecipes.isPickleIngredient(content)) {
                if (vat.removeOneMatching(s -> s.is(content.getItem()))) {
                    give(player, new ItemStack(VatRecipes.pickles().get(content.getItem())));
                    return true;
                }
            }
        }
        return false;
    }

    /** 计时结束 */
    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        VatBlockEntity vat = vatAt(level, pos);
        if (vat == null || vat.kind() == VatRecipes.Kind.NONE || state.getValue(FERMENTED)) {
            return;
        }

        if (!readyToProcess(vat, state)) {
            return;
        }

        int progress = state.getValue(PROGRESS) + 1;
        int units = totalUnits(state, vat);
        // 每过一个单位，水位下降一层（干腌不涉及水）
        int water = drainsWater(vat.kind())
                ? Math.max(0, units - progress * units / MAX_PROGRESS)
                : state.getValue(WATER_LEVEL);
        if (progress >= MAX_PROGRESS) {
            // 大酱和酱油酿好后水已经被吸收/转化，缸里不再留水
            boolean clearWater = drainsWater(vat.kind())
                    || vat.kind() == VatRecipes.Kind.PASTE
                    || vat.kind() == VatRecipes.Kind.SOY_SAUCE;
            level.setBlock(pos, state.setValue(PROGRESS, MAX_PROGRESS).setValue(FERMENTED, true)
                            .setValue(WATER_LEVEL, clearWater ? 0 : state.getValue(WATER_LEVEL)),
                    Block.UPDATE_CLIENTS);
            // 大酱酿好了：一缸能装 10 碗
            if (vat.kind() == VatRecipes.Kind.PASTE) {
                vat.setPaste(VatRecipes.PASTE_SERVINGS);
            }
            // 酱油酿好了：一缸能装 10 瓶
            if (vat.kind() == VatRecipes.Kind.SOY_SAUCE) {
                vat.setSoySauce(VatRecipes.SOY_SAUCE_SERVINGS);
            }
            level.playSound(null, pos, SoundEvents.COMPOSTER_READY, SoundSource.BLOCKS, 1.0F, 1.0F);
        } else {
            level.setBlock(pos, state.setValue(PROGRESS, progress).setValue(WATER_LEVEL, water), Block.UPDATE_CLIENTS);
            level.scheduleTick(pos, this, stepTicks(state, vat));
        }
    }

    /** 大缸被破坏时把里面的东西都吐出来 */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && vatAt(level, pos) instanceof VatBlockEntity vat) {
            for (ItemStack content : vat.contents()) {
                popResource(level, pos, content);
            }
            give(player, vat.takePress());
            give(player, vat.takeCover());
        }
        return super.playerWillDestroy(level, pos, state, player);
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
}
