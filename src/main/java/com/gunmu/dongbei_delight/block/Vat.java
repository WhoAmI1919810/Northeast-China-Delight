package com.gunmu.dongbei_delight.block;

import com.gunmu.dongbei_delight.block.ModBlockStateProperties.VatContent;
import com.gunmu.dongbei_delight.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 大缸：加水后投入大白菜、黄豆、黄瓜、胡萝卜或碎玉米粒发酵；
 * 先放盐再放猪肉则可以腌出咸腊肉（腌制不需要水）。
 *
 * 当前是演示用的简易机制（方块状态 + 计划刻，无需方块实体）。
 * 后续要接入 GUI 时，把内容物与进度迁移到 BlockEntity 即可，
 * 交互入口（useItemOn / useWithoutItem）保持不变。
 */
public class Vat extends Block {

    public static final IntegerProperty WATER_LEVEL = ModBlockStateProperties.WATER_LEVEL;
    public static final EnumProperty<VatContent> CONTENT = ModBlockStateProperties.VAT_CONTENT;
    public static final BooleanProperty FERMENTED = ModBlockStateProperties.VAT_FERMENTED;
    public static final IntegerProperty PROGRESS = ModBlockStateProperties.VAT_PROGRESS;

    /** 进度条步数上限 */
    public static final int MAX_PROGRESS = ModBlockStateProperties.VAT_MAX_PROGRESS;

    /** 发酵总时长：测试阶段为 100 tick = 5 秒 */
    public static final int FERMENT_TICKS = 100;

    /** 每走一步进度条的间隔 */
    private static final int STEP_TICKS = FERMENT_TICKS / MAX_PROGRESS;

    public Vat(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState()
                .setValue(WATER_LEVEL, 0)
                .setValue(CONTENT, VatContent.EMPTY)
                .setValue(FERMENTED, false)
                .setValue(PROGRESS, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(WATER_LEVEL, CONTENT, FERMENTED, PROGRESS);
    }

    @Override
    public BlockState getStateForPlacement(@NotNull BlockPlaceContext context) {
        return this.defaultBlockState();
    }

    // ===== 状态判定 =====

    /** 可以直接投入发酵的原料（这些需要缸里有水） */
    @Nullable
    private static VatContent fermentableContent(ItemStack stack) {
        if (stack.is(ModItems.SOYBEAN.get())) {
            return VatContent.SOYBEAN;
        }
        if (stack.is(ModItems.NAPA_CABBAGE.get())) {
            return VatContent.NAPA_CABBAGE;
        }
        if (stack.is(ModItems.CUCUMBER.get())) {
            return VatContent.CUCUMBER;
        }
        if (stack.is(Items.CARROT)) {
            return VatContent.CARROT;
        }
        if (stack.is(ModItems.CRUSHED_CORN.get())) {
            return VatContent.CRUSHED_CORN;
        }
        return null;
    }

    /** 这一流程是否需要消耗缸里的水（腌制不需要） */
    private static boolean needsWater(VatContent content) {
        return content != VatContent.CURING_MEAT;
    }

    /** 取出时得到的东西 */
    private static ItemStack outputFor(VatContent content) {
        return switch (content) {
            case SOYBEAN -> new ItemStack(ModItems.SOY_PASTE.get());
            case NAPA_CABBAGE -> new ItemStack(ModItems.SOUR_CABBAGE.get());
            case CUCUMBER -> new ItemStack(ModItems.PICKLED_CUCUMBER.get());
            case CARROT -> new ItemStack(ModItems.PICKLED_CARROT.get());
            case CRUSHED_CORN -> new ItemStack(ModItems.WATER_DOUGH.get());
            case CURING_MEAT -> new ItemStack(ModItems.SALTED_PORK.get());
            case SALT -> new ItemStack(ModItems.SALT.get());
            case EMPTY -> ItemStack.EMPTY;
        };
    }

    // ===== 交互 =====

    @Override
    protected @NotNull ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                                       Player player, InteractionHand hand, BlockHitResult hit) {
        VatContent content = state.getValue(CONTENT);

        // 空缸加水
        if (stack.is(Items.WATER_BUCKET) && content == VatContent.EMPTY && state.getValue(WATER_LEVEL) < 3) {
            if (!level.isClientSide) {
                level.setBlock(pos, state.setValue(WATER_LEVEL, state.getValue(WATER_LEVEL) + 1), 3);
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                    player.addItem(new ItemStack(Items.BUCKET));
                }
                level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }

        // 第一步：放盐
        if (stack.is(ModItems.SALT.get()) && content == VatContent.EMPTY) {
            if (!level.isClientSide) {
                level.setBlock(pos, state.setValue(CONTENT, VatContent.SALT), 3);
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                level.playSound(null, pos, SoundEvents.COMPOSTER_FILL_SUCCESS, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }

        // 第二步：放猪肉开始腌制
        if (stack.is(Items.PORKCHOP) && content == VatContent.SALT) {
            if (!level.isClientSide) {
                level.setBlock(pos, state
                        .setValue(CONTENT, VatContent.CURING_MEAT)
                        .setValue(FERMENTED, false)
                        .setValue(PROGRESS, 0), 3);
                level.scheduleTick(pos, this, STEP_TICKS);
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                level.playSound(null, pos, SoundEvents.COMPOSTER_FILL_SUCCESS, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }

        // 投入原料开始发酵（需要缸里有水）
        VatContent toFerment = fermentableContent(stack);
        if (toFerment != null && content == VatContent.EMPTY && state.getValue(WATER_LEVEL) > 0) {
            if (!level.isClientSide) {
                level.setBlock(pos, state
                        .setValue(CONTENT, toFerment)
                        .setValue(FERMENTED, false)
                        .setValue(PROGRESS, 0), 3);
                level.scheduleTick(pos, this, STEP_TICKS);
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                level.playSound(null, pos, SoundEvents.COMPOSTER_FILL_SUCCESS, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }

        // 大酱是酱料，必须用碗盛出来
        if (stack.is(Items.BOWL) && content == VatContent.SOYBEAN && state.getValue(FERMENTED)) {
            collect(level, pos, state, player, content, stack);
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    /** 发酵/腌制计时 */
    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(CONTENT) == VatContent.EMPTY || state.getValue(FERMENTED)) {
            return;
        }

        int progress = state.getValue(PROGRESS) + 1;
        if (progress >= MAX_PROGRESS) {
            level.setBlock(pos, state.setValue(PROGRESS, MAX_PROGRESS).setValue(FERMENTED, true),
                    Block.UPDATE_CLIENTS);
            level.playSound(null, pos, SoundEvents.COMPOSTER_READY, SoundSource.BLOCKS, 1.0F, 1.0F);
        } else {
            // 只同步给客户端，不触发邻居更新
            level.setBlock(pos, state.setValue(PROGRESS, progress), Block.UPDATE_CLIENTS);
            level.scheduleTick(pos, this, STEP_TICKS);
        }
    }

    /** 空手右键：取回盐 / 取出成品 */
    @Override
    protected @NotNull InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                        Player player, BlockHitResult hit) {
        VatContent content = state.getValue(CONTENT);

        // 只放了盐还没放肉：可以把盐取回来
        if (content == VatContent.SALT) {
            if (!level.isClientSide) {
                ItemStack salt = new ItemStack(ModItems.SALT.get());
                if (!player.addItem(salt)) {
                    player.drop(salt, false);
                }
                level.setBlock(pos, state.setValue(CONTENT, VatContent.EMPTY), 3);
                level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }

        if (content == VatContent.EMPTY || !state.getValue(FERMENTED)) {
            // 未完成时不做处理，后续接 GUI 后可在这里打开界面
            return InteractionResult.PASS;
        }

        // 大酱空手拿不到，需要碗
        if (content == VatContent.SOYBEAN) {
            if (!level.isClientSide) {
                player.displayClientMessage(
                        Component.translatable("message.dongbei_delight.vat.need_bowl"), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }

        collect(level, pos, state, player, content, ItemStack.EMPTY);
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    /**
     * 取出成品并重置大缸。
     *
     * @param heldStack 玩家手里的盛具（例如碗）；非空时会消耗一个
     */
    private void collect(Level level, BlockPos pos, BlockState state, Player player,
                         VatContent content, ItemStack heldStack) {
        if (level.isClientSide) {
            return;
        }

        if (!heldStack.isEmpty() && !player.getAbilities().instabuild) {
            heldStack.shrink(1);
        }

        ItemStack output = outputFor(content);
        if (!player.addItem(output)) {
            player.drop(output, false);
        }

        // 腌制不消耗水，其余流程消耗一档水（并保证不会变成负数）
        int water = needsWater(content) ? Math.max(0, state.getValue(WATER_LEVEL) - 1) : state.getValue(WATER_LEVEL);
        level.setBlock(pos, state
                .setValue(CONTENT, VatContent.EMPTY)
                .setValue(FERMENTED, false)
                .setValue(PROGRESS, 0)
                .setValue(WATER_LEVEL, water), 3);
        level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
    }
}
