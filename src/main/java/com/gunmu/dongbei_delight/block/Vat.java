package com.gunmu.dongbei_delight.block;

import com.gunmu.dongbei_delight.DongbeiDelight;
import com.gunmu.dongbei_delight.crafting.VatRecipes;
import com.gunmu.dongbei_delight.item.ModItems;
import com.gunmu.dongbei_delight.item.SeasoningBottleItem;
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

    /** 手里拿的是不是农夫乐事的粗布毯（酿醋盖缸用） */
    private static boolean isVinegarCloth(ItemStack stack) {
        return stack.is(BuiltInRegistries.ITEM.get(
                ResourceLocation.fromNamespaceAndPath("farmersdelight", "canvas_rug")));
    }

    /**
     * 能不能把瓶装酸引水倒进这个缸。
     * 注意：第一瓶倒进空缸后，缸的状态会变成「泡菜缸」但还没腌制，
     * 所以泡菜缸不能要求 fermented，否则只能倒一瓶。
     */
    private static boolean canPourSourWater(VatBlockEntity vat, BlockState state) {
        return switch (vat.kind()) {
            // 空缸：倒进去就成了一缸酸引水（状态记为泡菜缸）
            case NONE -> vat.contents().isEmpty() && !vat.hasProductLiquid();
            // 泡菜缸：不管腌没腌好都能继续攒酸引水
            case PICKLE -> true;
            // 正在酿的白醋缸：可以把酸引水续上
            case WHITE_VINEGAR -> !state.getValue(FERMENTED);
            // 其它缸里已经有别的成品，不允许混液体
            default -> false;
        };
    }

    /** 这次处理要几个腌制单位：泡水类按水位算（满水 3 层 = 3 个单位），干腌固定 1 个单位 */
    private static int totalUnits(BlockState state, VatBlockEntity vat) {
        return switch (vat.kind()) {
            case PICKLE, DOUGH -> Math.max(1, state.getValue(WATER_LEVEL));
            default -> 1;
        };
    }

    /**
     * 处理过程中会不会抽走缸里的水。
     * 泡菜现在保留水位（腌完后水会变成酸引水），所以只剩水面团会抽水。
     */
    private static boolean drainsWater(VatRecipes.Kind kind) {
        return kind == VatRecipes.Kind.DOUGH;
    }

    /** 每走一格进度条要多少刻：总时长写在各配方自己的表里（VatRecipes#processTicks） */
    private static int stepTicks(BlockState state, VatBlockEntity vat) {
        int totalTicks = VatRecipes.processTicks(vat.kind(), state.getValue(WATER_LEVEL));
        return Math.max(1, totalTicks / MAX_PROGRESS);
    }

    /**
     * 当前水量的盐够不够。
     * 泡菜：一层水配一份盐，盐数必须 ≥ 水位；大酱：一份盐就够。
     * 客户端渲染「发白的盐水」时也用这个判断。
     */
    public static boolean hasEnoughSalt(VatBlockEntity vat, BlockState state) {
        int salt = vat.countOf(ModItems.SALT.get());
        return switch (vat.kind()) {
            case PICKLE, SPICY_PICKLE -> salt >= state.getValue(WATER_LEVEL) * VatRecipes.SALT_PER_WATER;
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
            // 腊肉：盐的数量要跟肉一样多（谁先放都行）
            case MEAT -> vat.isPressed()
                    && vat.meatCount() > 0
                    && vat.countOf(ModItems.SALT.get()) == vat.meatCount();
            // 咸鱼：和腊肉一个路子，盐的数量要跟生鱼一样多（谁先放都行）
            case SALTED_FISH -> vat.isPressed()
                    && vat.rawFishCount() > 0
                    && vat.countOf(ModItems.SALT.get()) == vat.rawFishCount();
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
            // 醋：只剩酱渣 + 3 份玉米粒 / 荞麦，蒙粗布毯
            case VINEGAR -> vat.isCovered()
                    && vat.residueCount() >= VatRecipes.RESIDUE_COUNT
                    && vat.vinegarGrainCount() >= VatRecipes.VINEGAR_GRAIN_COUNT;
            // 白醋：泡菜剩下的酸引水 + 3 份玉米粒 / 荞麦，蒙粗布毯
            case WHITE_VINEGAR -> vat.isCovered()
                    && vat.sourWaterMb() >= VatRecipes.SOUR_WATER_MB
                    && vat.vinegarGrainCount() >= VatRecipes.VINEGAR_GRAIN_COUNT;
            // 鱼露：6 条生鱼 + 3 份盐，压石头发酵（不加水）
            case FISH_SAUCE -> vat.isPressed()
                    && vat.rawFishCount() >= VatRecipes.FISH_SAUCE_FISH
                    && vat.countOf(ModItems.SALT.get()) >= VatRecipes.FISH_SAUCE_SALT;
            // 虾酱：6 只大虾 + 3 份盐，压石头发酵（不加水）
            case SHRIMP_PASTE -> vat.isPressed()
                    && vat.shrimpCount() >= VatRecipes.SHRIMP_PASTE_SHRIMP
                    && vat.countOf(ModItems.SALT.get()) >= VatRecipes.SHRIMP_PASTE_SALT;
            // 辣白菜：泡菜的基础上必须有红辣椒，调味品数量按份数算
            case SPICY_PICKLE -> vat.isPressed()
                    && state.getValue(WATER_LEVEL) > 0
                    && hasEnoughSalt(vat, state)
                    && vat.vegetableCount() > 0
                    && vat.chiliSauceCount() >= 1
                    && vat.seasoningCount()
                            >= VatRecipes.spicySeasoningNeed(state.getValue(WATER_LEVEL));
            case DOUGH -> true;
            // 豆芽：1 层水 + 黄豆（1~2 份），蒙粗布毯
            case BEAN_SPROUTS -> vat.isCovered()
                    && state.getValue(WATER_LEVEL) == 1
                    && vat.soybeanCount() >= 1;
            // 酸玉米粒：每层水配 2 份玉米粒，蒙粗布毯
            case SOUR_CORN -> vat.isCovered()
                    && state.getValue(WATER_LEVEL) > 0
                    && vat.cornKernelCount() == state.getValue(WATER_LEVEL) * VatRecipes.SOUR_CORN_PER_WATER;
            // 格瓦斯：满水 + 6 个面包，蒙粗布毯
            case KVASS -> vat.isCovered()
                    && state.getValue(WATER_LEVEL) >= VatRecipes.KVASS_WATER_LEVEL
                    && vat.countOf(VatRecipes.kvassBread()) >= VatRecipes.KVASS_BREAD;
            case NONE -> false;
        };
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
            if (!level.isClientSide && !stack.isEmpty()) {
                hint(player, "message.dongbei_delight.vat.locked_by_press");
            }
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
            // 把瓶装酸引水倒回缸里：空缸会变成「泡菜缸」，腌好的泡菜缸则继续攒酸引水
            if (stack.is(ModItems.SOUR_WATER.get()) && canPourSourWater(vat, state)) {
                if (vat.kind() == VatRecipes.Kind.NONE) {
                    vat.setKind(VatRecipes.Kind.PICKLE);
                }
                vat.addSourWater(VatRecipes.SERVING_MB);
                consume(player, stack);
                give(player, new ItemStack(Items.GLASS_BOTTLE));
                playFill(level, pos);
                return ItemInteractionResult.sidedSuccess(false);
            }

            // 用过的调料瓶：右键大缸把液体续进瓶里（缺多少补多少，缸里不够就补多少）
            if (SeasoningBottleItem.isBottle(stack) && state.getValue(FERMENTED)) {
                if (SeasoningBottleItem.isFull(stack)) {
                    hint(player, "message.dongbei_delight.vat.bottle_full");
                    return ItemInteractionResult.sidedSuccess(false);
                }
                if (VatRecipes.bottleFor(vat.kind()) != stack.getItem()) {
                    hint(player, "message.dongbei_delight.vat.bottle_mismatch");
                    return ItemInteractionResult.sidedSuccess(false);
                }
                int moved = vat.drainProduct(vat.kind(), SeasoningBottleItem.usedMb(stack));
                if (moved <= 0) {
                    hint(player, "message.dongbei_delight.vat.bottle_empty");
                    return ItemInteractionResult.sidedSuccess(false);
                }
                SeasoningBottleItem.refill(stack, moved);
                playFill(level, pos);
                return ItemInteractionResult.sidedSuccess(false);
            }

            // 放盐：泡菜要按「一份水配一份盐」；腊肉最多 5 份，和肉搭配着放（不分先后）
            if (stack.is(ModItems.SALT.get())) {
                boolean ok = switch (vat.kind()) {
                    case NONE -> water > 0;                                   // 水里放盐 → 泡菜
                    case PICKLE -> vat.countOf(ModItems.SALT.get()) < water * VatRecipes.SALT_PER_WATER;
                    // 腊肉 / 咸鱼：盐最多 5 份，和肉（鱼）搭配着放，不分先后
                    case MEAT, SALTED_FISH -> vat.countOf(ModItems.SALT.get()) < VatRecipes.MAX_MEATS;
                    case PASTE, SOY_SAUCE -> vat.countOf(ModItems.SALT.get()) < VatRecipes.PASTE_SALT;
                    // 辣白菜：盐只算盐水那一份，调味品是鱼露 / 虾酱
                    case SPICY_PICKLE -> vat.countOf(ModItems.SALT.get())
                            < water * VatRecipes.SALT_PER_WATER;
                    case FISH_SAUCE -> vat.countOf(ModItems.SALT.get()) < VatRecipes.FISH_SAUCE_SALT;
                    case SHRIMP_PASTE -> vat.countOf(ModItems.SALT.get()) < VatRecipes.SHRIMP_PASTE_SALT;
                    case DOUGH, VINEGAR, WHITE_VINEGAR, BEAN_SPROUTS, SOUR_CORN, KVASS -> false;
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

            // 放猪肉：腊肉类，不加水，最多 5 块（和盐不分先后）
            // 已经腌好的缸不再收料：否则新放的肉会立刻被算成成品
            if (stack.is(VatRecipes.meatInput()) && !state.getValue(FERMENTED)) {
                boolean ok = switch (vat.kind()) {
                    case NONE -> water <= 0;
                    case MEAT -> vat.meatCount() < VatRecipes.MAX_MEATS;
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

            // 生鱼：空缸先按「咸鱼」起步（和腊肉一样，鱼和盐 1:1、最多 5 条），压石头发酵；
            // 鱼露要 6 条，所以在 5 条的基础上再放第 6 条时自动转成鱼露流程。
            if (VatRecipes.isRawFish(stack) && water <= 0) {
                if (state.getValue(FERMENTED)) {
                    return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
                }
                boolean salting = vat.kind() == VatRecipes.Kind.NONE || vat.kind() == VatRecipes.Kind.SALTED_FISH;
                boolean sauceBrewing = vat.kind() == VatRecipes.Kind.FISH_SAUCE;
                boolean roomForSaltedFish = salting && vat.rawFishCount() < VatRecipes.MAX_MEATS;
                // 已经压满 5 条、盐又没超过鱼露的份数：这一条是冲着鱼露来的
                boolean becomesFishSauce = salting
                        && vat.rawFishCount() == VatRecipes.MAX_MEATS
                        && vat.countOf(ModItems.SALT.get()) <= VatRecipes.FISH_SAUCE_SALT;
                boolean roomForFishSauce = sauceBrewing && vat.rawFishCount() < VatRecipes.FISH_SAUCE_FISH;
                if (roomForSaltedFish || becomesFishSauce || roomForFishSauce) {
                    if (vat.addContent(stack)) {
                        if (vat.kind() == VatRecipes.Kind.NONE) {
                            vat.setKind(VatRecipes.Kind.SALTED_FISH);
                        }
                        if (becomesFishSauce) {
                            vat.setKind(VatRecipes.Kind.FISH_SAUCE);
                        }
                        consume(player, stack);
                        playFill(level, pos);
                        tryStart(level, pos, state, vat);
                        return ItemInteractionResult.sidedSuccess(false);
                    }
                }
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }

            // 虾酱：6 只大虾 + 3 份盐，不加水，压石头发酵
            if (stack.is(ModItems.SHRIMP.get()) && water <= 0
                    && (vat.kind() == VatRecipes.Kind.NONE || vat.kind() == VatRecipes.Kind.SHRIMP_PASTE)
                    && vat.shrimpCount() < VatRecipes.SHRIMP_PASTE_SHRIMP) {
                if (vat.addContent(stack)) {
                    if (vat.kind() == VatRecipes.Kind.NONE) {
                        vat.setKind(VatRecipes.Kind.SHRIMP_PASTE);
                    }
                    consume(player, stack);
                    playFill(level, pos);
                    tryStart(level, pos, state, vat);
                    return ItemInteractionResult.sidedSuccess(false);
                }
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }

            // 生豆芽：1 层水 + 1~2 份黄豆，蒙粗布毯发芽
            if (stack.is(ModItems.SOYBEAN.get()) && water == 1
                    && (vat.kind() == VatRecipes.Kind.NONE || vat.kind() == VatRecipes.Kind.BEAN_SPROUTS)
                    && vat.soybeanCount() < VatRecipes.SPROUT_SOYBEAN_MAX) {
                if (vat.addContent(stack)) {
                    if (vat.kind() == VatRecipes.Kind.NONE) {
                        vat.setKind(VatRecipes.Kind.BEAN_SPROUTS);
                    }
                    consume(player, stack);
                    playFill(level, pos);
                    tryStart(level, pos, state, vat);
                    return ItemInteractionResult.sidedSuccess(false);
                }
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }

            // 辣白菜：泡菜缸里放辣椒酱，就转成辣白菜流程（辣椒酱是必选调料）
            if (stack.is(ModItems.CHILI_SAUCE.get()) && water > 0
                    && (vat.kind() == VatRecipes.Kind.PICKLE || vat.kind() == VatRecipes.Kind.SPICY_PICKLE)
                    && vat.chiliSauceCount() < VatRecipes.SPICY_SAUCE_MAX) {
                if (vat.addContent(stack)) {
                    if (vat.kind() == VatRecipes.Kind.PICKLE) {
                        vat.setKind(VatRecipes.Kind.SPICY_PICKLE);
                    }
                    consume(player, stack);
                    playFill(level, pos);
                    tryStart(level, pos, state, vat);
                    return ItemInteractionResult.sidedSuccess(false);
                }
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }

            // 辣白菜的可选调味品：鱼露 / 虾酱（瓶装，倒进缸里返还空瓶）
            if ((stack.is(ModItems.FISH_SAUCE.get()) || stack.is(ModItems.SHRIMP_PASTE.get()))
                    && vat.kind() == VatRecipes.Kind.SPICY_PICKLE
                    && vat.seasoningCount()
                            < VatRecipes.SPICY_SEASONING_MAX) {
                if (vat.addContent(stack)) {
                    consume(player, stack);
                    give(player, new ItemStack(Items.GLASS_BOTTLE));
                    playFill(level, pos);
                    tryStart(level, pos, state, vat);
                    return ItemInteractionResult.sidedSuccess(false);
                }
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }

            // 大酱块：最多放 3 块（第一块决定进入酿酱状态）
            // 先放干缸再倒水、或者先倒水再放酱块都可以；已经放了小麦（酱油流程）也还能补酱块
            if (stack.is(ModItems.SOY_PASTE_CHUNK.get()) && !state.getValue(FERMENTED)
                    && (vat.kind() == VatRecipes.Kind.NONE
                            || vat.kind() == VatRecipes.Kind.PASTE
                            || vat.kind() == VatRecipes.Kind.SOY_SAUCE)
                    && vat.countOf(ModItems.SOY_PASTE_CHUNK.get()) < VatRecipes.PASTE_CHUNKS) {
                if (vat.addContent(stack)) {
                    if (vat.kind() == VatRecipes.Kind.NONE) {
                        vat.setKind(VatRecipes.Kind.PASTE);
                    }
                    consume(player, stack);
                    playFill(level, pos);
                    refreshSalted(level, pos, state, vat);
                    return ItemInteractionResult.sidedSuccess(false);
                }
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }

            // 酿酱油：大酱的料里再加一份小麦就转成酱油（酱块没放够也可以先加，凑齐 3 块才会开始发酵）
            if (stack.is(VatRecipes.wheatInput())) {
                if (state.getValue(FERMENTED)) {
                    // 已经酿好的缸不能再加料，否则大酱会被"变成"一缸空酱油
                    hint(player, "message.dongbei_delight.vat.already_fermented");
                    return ItemInteractionResult.sidedSuccess(false);
                }
                if (vat.kind() == VatRecipes.Kind.NONE) {
                    hint(player, "message.dongbei_delight.vat.need_chunks_first");
                    return ItemInteractionResult.sidedSuccess(false);
                }
                if (vat.kind() == VatRecipes.Kind.PASTE
                        && vat.countOf(VatRecipes.wheatInput()) == 0
                        && vat.addContent(stack)) {
                    vat.setKind(VatRecipes.Kind.SOY_SAUCE);
                    consume(player, stack);
                    playFill(level, pos);
                    tryStart(level, pos, state, vat);
                    return ItemInteractionResult.sidedSuccess(false);
                }
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }

            // 酸玉米粒：每 1 层水配 2 份玉米粒，蒙粗布毯发酵（旧的水面团配方已移除）
            if (stack.is(ModItems.CORN_SEEDS.get()) && water > 0
                    && (vat.kind() == VatRecipes.Kind.NONE || vat.kind() == VatRecipes.Kind.SOUR_CORN)
                    && vat.cornKernelCount() < water * VatRecipes.SOUR_CORN_PER_WATER) {
                if (vat.addContent(stack)) {
                    if (vat.kind() == VatRecipes.Kind.NONE) {
                        vat.setKind(VatRecipes.Kind.SOUR_CORN);
                    }
                    consume(player, stack);
                    playFill(level, pos);
                    tryStart(level, pos, state, vat);
                    return ItemInteractionResult.sidedSuccess(false);
                }
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }

            // 格瓦斯：满水时放 6 个面包，蒙粗布毯发酵
            if (stack.is(VatRecipes.kvassBread()) && water >= VatRecipes.KVASS_WATER_LEVEL
                    && (vat.kind() == VatRecipes.Kind.NONE || vat.kind() == VatRecipes.Kind.KVASS)
                    && vat.countOf(VatRecipes.kvassBread()) < VatRecipes.KVASS_BREAD) {
                if (vat.addContent(stack)) {
                    if (vat.kind() == VatRecipes.Kind.NONE) {
                        vat.setKind(VatRecipes.Kind.KVASS);
                    }
                    consume(player, stack);
                    playFill(level, pos);
                    tryStart(level, pos, state, vat);
                    return ItemInteractionResult.sidedSuccess(false);
                }
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }

            // 压石头：启动腌制
            // 压石头用于泡菜 / 腊肉 / 辣白菜 / 鱼露 / 虾酱
            if (isPressStone(stack) && !vat.isEmpty() && !vat.isPressed()
                    && (vat.kind() == VatRecipes.Kind.PICKLE
                            || vat.kind() == VatRecipes.Kind.MEAT
                            || vat.kind() == VatRecipes.Kind.SALTED_FISH
                            || vat.kind() == VatRecipes.Kind.SPICY_PICKLE
                            || vat.kind() == VatRecipes.Kind.FISH_SAUCE
                            || vat.kind() == VatRecipes.Kind.SHRIMP_PASTE)) {
                vat.setPress(stack);
                consume(player, stack);
                playFill(level, pos);
                if (readyToProcess(vat, state)) {
                    tryStart(level, pos, state, vat);
                } else {
                    // 石头压上了但材料还没凑齐：给一句提示，避免「压了没反应」
                    hint(player, "message.dongbei_delight.vat.materials_not_ready");
                }
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

            // 酿醋第二步：缸里只剩 3 块酱渣时，加 3 份玉米粒或荞麦
            boolean residueOnly = (vat.kind() == VatRecipes.Kind.PASTE || vat.kind() == VatRecipes.Kind.SOY_SAUCE)
                    && vat.residueCount() >= VatRecipes.RESIDUE_COUNT
                    && !vat.hasProductLiquid();
            // 酿白醋：泡菜腌好、缸里还有足够酸引水时，同样加 3 份玉米粒或荞麦
            boolean sourWaterReady = vat.kind() == VatRecipes.Kind.PICKLE
                    && vat.sourWaterMb() >= VatRecipes.SOUR_WATER_MB;
            if (VatRecipes.isVinegarGrain(stack)
                    && (residueOnly || sourWaterReady
                            || vat.kind() == VatRecipes.Kind.VINEGAR
                            || vat.kind() == VatRecipes.Kind.WHITE_VINEGAR)
                    && vat.vinegarGrainCount() < VatRecipes.VINEGAR_GRAIN_COUNT) {
                if (vat.addContent(stack)) {
                    if (vat.kind() == VatRecipes.Kind.PASTE || vat.kind() == VatRecipes.Kind.SOY_SAUCE) {
                        // 从「酿好的大酱 / 酱油」切换到酿醋：清掉完成标记，等蒙布后重新计时
                        level.setBlock(pos, state.setValue(FERMENTED, false).setValue(PROGRESS, 0), 3);
                        vat.setKind(VatRecipes.Kind.VINEGAR);
                    } else if (vat.kind() == VatRecipes.Kind.PICKLE) {
                        // 从「腌好的泡菜」切换到酿白醋：水位保留（酸引水还在），重新计时
                        level.setBlock(pos, state.setValue(FERMENTED, false).setValue(PROGRESS, 0), 3);
                        vat.setKind(VatRecipes.Kind.WHITE_VINEGAR);
                    }
                    consume(player, stack);
                    playFill(level, pos);
                    return ItemInteractionResult.sidedSuccess(false);
                }
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }

            // 酿醋第三步：蒙上农夫乐事的粗布毯，开始二次发酵
            if (isVinegarCloth(stack)
                    && (vat.kind() == VatRecipes.Kind.VINEGAR
                            || vat.kind() == VatRecipes.Kind.WHITE_VINEGAR
                            || vat.kind() == VatRecipes.Kind.BEAN_SPROUTS
                            || vat.kind() == VatRecipes.Kind.SOUR_CORN
                            || vat.kind() == VatRecipes.Kind.KVASS)
                    && !vat.isCovered()) {
                vat.setCover(stack);
                consume(player, stack);
                playFill(level, pos);
                tryStart(level, pos, state, vat);
                return ItemInteractionResult.sidedSuccess(false);
            }

            // 用碗装大酱：一缸 10 碗，每装一碗液面下降一档
            if (stack.is(Items.BOWL) && vat.kind() == VatRecipes.Kind.PASTE
                    && state.getValue(FERMENTED) && vat.pasteMb() > 0 && !vat.isCovered()) {
                vat.takeOnePaste();
                consume(player, stack);
                give(player, new ItemStack(ModItems.SOY_PASTE.get()));
                // 大酱是黏稠的，用蜂蜜的黏腻音效而不是水声
                level.playSound(null, pos, SoundEvents.HONEY_BLOCK_SLIDE, SoundSource.BLOCKS, 1.0F, 1.0F);
                // 液体取空后酱渣留在缸里：空手右键取出，或者加玉米粒 / 荞麦继续酿醋
                return ItemInteractionResult.sidedSuccess(false);
            }

            // 用玻璃瓶装酱油：一缸 10 瓶，每装一瓶液面下降一档
            if (stack.is(Items.GLASS_BOTTLE) && vat.kind() == VatRecipes.Kind.SOY_SAUCE
                    && state.getValue(FERMENTED) && vat.soySauceMb() > 0 && !vat.isCovered()) {
                vat.takeOneSoySauce();
                consume(player, stack);
                give(player, new ItemStack(ModItems.SOY_SAUCE.get()));
                level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
                return ItemInteractionResult.sidedSuccess(false);
            }

            // 用玻璃瓶装醋：一缸 10 瓶
            if (stack.is(Items.GLASS_BOTTLE) && vat.kind() == VatRecipes.Kind.VINEGAR
                    && state.getValue(FERMENTED) && vat.vinegarMb() > 0 && !vat.isCovered()) {
                vat.takeOneVinegar();
                consume(player, stack);
                give(player, new ItemStack(ModItems.VINEGAR.get()));
                level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
                return ItemInteractionResult.sidedSuccess(false);
            }

            // 用玻璃瓶装酸引水（泡菜腌好后缸里的那缸水，一层水 = 4 瓶）
            if (stack.is(Items.GLASS_BOTTLE) && vat.kind() == VatRecipes.Kind.PICKLE
                    && state.getValue(FERMENTED) && vat.sourWaterMb() > 0) {
                vat.takeOneSourWater();
                consume(player, stack);
                give(player, new ItemStack(ModItems.SOUR_WATER.get()));
                level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
                return ItemInteractionResult.sidedSuccess(false);
            }

            // 用玻璃瓶装白醋：一缸 10 瓶
            if (stack.is(Items.GLASS_BOTTLE) && vat.kind() == VatRecipes.Kind.WHITE_VINEGAR
                    && state.getValue(FERMENTED) && vat.whiteVinegarMb() > 0 && !vat.isCovered()) {
                vat.takeOneWhiteVinegar();
                consume(player, stack);
                give(player, new ItemStack(ModItems.WHITE_VINEGAR.get()));
                level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
                return ItemInteractionResult.sidedSuccess(false);
            }

            // 用玻璃瓶装鱼露：一缸就 1 瓶
            if (stack.is(Items.GLASS_BOTTLE) && vat.kind() == VatRecipes.Kind.FISH_SAUCE
                    && state.getValue(FERMENTED) && vat.fishSauceMb() > 0) {
                vat.takeOneFishSauce();
                consume(player, stack);
                give(player, new ItemStack(ModItems.FISH_SAUCE.get()));
                level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
                return ItemInteractionResult.sidedSuccess(false);
            }

            // 用玻璃瓶装虾酱
            if (stack.is(Items.GLASS_BOTTLE) && vat.kind() == VatRecipes.Kind.SHRIMP_PASTE
                    && state.getValue(FERMENTED) && vat.shrimpPasteMb() > 0) {
                vat.takeOneShrimpPaste();
                consume(player, stack);
                give(player, new ItemStack(ModItems.SHRIMP_PASTE.get()));
                level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
                return ItemInteractionResult.sidedSuccess(false);
            }

            // 用玻璃瓶装格瓦斯：一缸 6 瓶
            if (stack.is(Items.GLASS_BOTTLE) && vat.kind() == VatRecipes.Kind.KVASS
                    && state.getValue(FERMENTED) && vat.kvassMb() > 0 && !vat.isCovered()) {
                vat.takeOneKvass();
                consume(player, stack);
                give(player, new ItemStack(ModItems.KVASS.get()));
                level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
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

        // 缸里还有成品液体（大酱 / 酱油 / 醋 / 白醋 / 鱼露 / 虾酱）时，只能用对应的容器装；
        // 泡菜缸例外：空手照样取菜，酸引水要用玻璃瓶装
        if (vat.hasProductLiquid()
                && (vat.kind() == VatRecipes.Kind.PASTE
                        || vat.kind() == VatRecipes.Kind.SOY_SAUCE
                        || vat.kind() == VatRecipes.Kind.VINEGAR
                        || vat.kind() == VatRecipes.Kind.WHITE_VINEGAR
                        || vat.kind() == VatRecipes.Kind.FISH_SAUCE
                        || vat.kind() == VatRecipes.Kind.SHRIMP_PASTE)) {
            if (!level.isClientSide) {
                hint(player, liquidHintKey(vat.kind()));
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }

        // 液体取空之后，空手取内容物：泡菜、腊肉、水面团、酱渣
        if (!level.isClientSide) {
            // 腌制完成、开始取货时，盐已经被吸收掉了，直接从缸里清掉
            vat.consumeSalt();
            // 辣白菜用掉的调料同样被吸收：辣椒酱还碗，鱼露 / 虾酱还玻璃瓶
            for (ItemStack refund : vat.consumeSeasonings()) {
                give(player, refund);
            }
            boolean taken;
            if (vat.kind() == VatRecipes.Kind.PASTE
                    || vat.kind() == VatRecipes.Kind.SOY_SAUCE
                    || vat.kind() == VatRecipes.Kind.VINEGAR) {
                taken = vat.removeOneOf(ModItems.SOY_RESIDUE.get());
                if (taken) {
                    give(player, new ItemStack(ModItems.SOY_RESIDUE.get()));
                }
            } else {
                taken = switch (vat.kind()) {
                    // 每右键一次取出一份泡菜（按放入顺序），取完为止
                    // 白醋流程里没取走的泡菜也在这里取
                    case PICKLE, WHITE_VINEGAR -> takePickle(player, vat);
                    // 辣白菜：白菜给辣白菜，其它蔬菜还是按普通泡菜拿
                    case SPICY_PICKLE -> takeSpicyPickle(player, vat);
                    case MEAT -> {
                        if (vat.removeOneMatching(s -> s.is(VatRecipes.meatInput()))) {
                            give(player, new ItemStack(VatRecipes.meatResult()));
                            yield true;
                        }
                        yield false;
                    }
                    // 咸鱼：取一条生鱼的位置给一条咸鱼（鱼的种类不限）
                    case SALTED_FISH -> {
                        if (vat.removeOneMatching(VatRecipes::isRawFish)) {
                            give(player, new ItemStack(VatRecipes.saltedFishResult()));
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
                    case BEAN_SPROUTS -> {
                        if (vat.removeOneOf(ModItems.BEAN_SPROUTS.get())) {
                            give(player, new ItemStack(ModItems.BEAN_SPROUTS.get()));
                            yield true;
                        }
                        yield false;
                    }
                    case SOUR_CORN -> {
                        if (vat.removeOneOf(ModItems.SOUR_CORN_KERNELS.get())) {
                            give(player, new ItemStack(ModItems.SOUR_CORN_KERNELS.get()));
                            yield true;
                        }
                        yield false;
                    }
                    default -> false;
                };
            }

            if (taken) {
                level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.6F, 1.4F);
            }
            // 内容物和液体都空了，才把缸恢复成空缸
            if (vat.contents().isEmpty() && !vat.hasProductLiquid()) {
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

    /** 取出一份辣白菜：白菜给辣白菜，混进去的其它蔬菜还是按普通泡菜拿 */
    private static boolean takeSpicyPickle(Player player, VatBlockEntity vat) {
        for (ItemStack content : vat.contents()) {
            if (content.is(ModItems.NAPA_CABBAGE.get())) {
                if (vat.removeOneMatching(s -> s.is(ModItems.NAPA_CABBAGE.get()))) {
                    give(player, new ItemStack(VatRecipes.spicyCabbage()));
                    return true;
                }
            }
        }
        return takePickle(player, vat);
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
                    || vat.kind() == VatRecipes.Kind.SOY_SAUCE
                    || vat.kind() == VatRecipes.Kind.VINEGAR
                    || vat.kind() == VatRecipes.Kind.WHITE_VINEGAR
                    || vat.kind() == VatRecipes.Kind.BEAN_SPROUTS
                    || vat.kind() == VatRecipes.Kind.SOUR_CORN
                    || vat.kind() == VatRecipes.Kind.KVASS;
            level.setBlock(pos, state.setValue(PROGRESS, MAX_PROGRESS).setValue(FERMENTED, true)
                            .setValue(WATER_LEVEL, clearWater ? 0 : state.getValue(WATER_LEVEL)),
                    Block.UPDATE_CLIENTS);
            // 大酱酿好了：一缸能装 10 碗
            if (vat.kind() == VatRecipes.Kind.PASTE) {
                vat.setPaste(VatRecipes.PASTE_SERVINGS);
                // 三块酱块变成酱渣留在缸里
                vat.finishBrewing();
            }
            // 泡菜 / 辣白菜腌好了：水位保持不变，缸里的水等量变成酸引水
            if (vat.kind() == VatRecipes.Kind.PICKLE || vat.kind() == VatRecipes.Kind.SPICY_PICKLE) {
                vat.addSourWater(state.getValue(WATER_LEVEL) * VatRecipes.WATER_MB_PER_LEVEL);
            }
            // 酱油酿好了：一缸能装 10 瓶
            if (vat.kind() == VatRecipes.Kind.SOY_SAUCE) {
                vat.setSoySauce(VatRecipes.SOY_SAUCE_SERVINGS);
                vat.finishBrewing();
            }
            // 醋酿好了：一缸能装 10 瓶，谷物被消耗、酱渣留下
            if (vat.kind() == VatRecipes.Kind.VINEGAR) {
                vat.setVinegar(VatRecipes.VINEGAR_SERVINGS);
                vat.finishVinegar();
            }
            // 白醋酿好了：酸引水被用掉，一缸能装 10 瓶
            if (vat.kind() == VatRecipes.Kind.WHITE_VINEGAR) {
                vat.setSourWaterMb(0);
                vat.setWhiteVinegar(VatRecipes.WHITE_VINEGAR_SERVINGS);
                vat.finishVinegar();
            }
            // 鱼露：鱼肉连同盐一起被分解掉，只剩一点点液体
            if (vat.kind() == VatRecipes.Kind.FISH_SAUCE) {
                vat.consumeAllContents();
                vat.setFishSauce(VatRecipes.FISH_SAUCE_SERVINGS);
            }
            // 虾酱：整缸虾都化成了酱
            if (vat.kind() == VatRecipes.Kind.SHRIMP_PASTE) {
                vat.consumeAllContents();
                vat.setShrimpPaste(VatRecipes.SHRIMP_PASTE_SERVINGS);
            }
            // 豆芽发好了：每份黄豆变成一份豆芽，水被吸收
            if (vat.kind() == VatRecipes.Kind.BEAN_SPROUTS) {
                vat.finishSprouting();
            }
            // 酸玉米粒发好了：每份玉米粒变成一份酸玉米粒，水被吸收
            if (vat.kind() == VatRecipes.Kind.SOUR_CORN) {
                vat.finishSourCorn();
            }
            // 格瓦斯：面包泡化了，一缸出 6 瓶
            if (vat.kind() == VatRecipes.Kind.KVASS) {
                vat.consumeAllContents();
                vat.setKvass(VatRecipes.KVASS_SERVINGS);
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

    /** 放不进去时给玩家一句提示，免得看起来像"点坏了" */
    private static void hint(Player player, String translationKey) {
        player.displayClientMessage(Component.translatable(translationKey), true);
    }

    /** 缸里有成品液体时，空手右键提示要用什么容器装 */
    private static String liquidHintKey(VatRecipes.Kind kind) {
        return switch (kind) {
            case PASTE -> "message.dongbei_delight.vat.need_container";
            case VINEGAR -> "message.dongbei_delight.vat.need_bottle_vinegar";
            case WHITE_VINEGAR -> "message.dongbei_delight.vat.need_bottle_white_vinegar";
            case FISH_SAUCE -> "message.dongbei_delight.vat.need_bottle_fish_sauce";
            case SHRIMP_PASTE -> "message.dongbei_delight.vat.need_bottle_shrimp_paste";
            case PICKLE -> "message.dongbei_delight.vat.need_bottle_sour_water";
            default -> "message.dongbei_delight.vat.need_bottle";
        };
    }
}
