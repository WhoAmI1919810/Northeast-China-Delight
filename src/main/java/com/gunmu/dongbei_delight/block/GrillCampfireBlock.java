package com.gunmu.dongbei_delight.block;

import com.gunmu.dongbei_delight.crafting.GrillRecipes;
import com.gunmu.dongbei_delight.item.ModItems;
import com.gunmu.dongbei_delight.item.SeasoningBottleItem;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * 架上烧烤架的营火。
 *
 * <p>一个烤架能放 <b>4 份食材</b>（什么都能放、可以混着放），四份各自独立计时。
 * 调料要「刷到具体某一份上」：
 * <ul>
 *     <li>瓶装调料 = 左手拿瓶子、右手拿刷子右键那一份食材，每份只花 10 mB，并且会在食材上叠一层那种调料颜色的半透明层；</li>
 *     <li>盐、糖这类不是瓶装的，直接右键那一份，消耗一份，食材上会撒几个白点。</li>
 * </ul>
 *
 * <p>哪一份是「点到的」由 {@link GrillBlockEntity#slotOffset} 算出来的四个角位置决定，
 * 和渲染用的是同一个公式。
 */
public class GrillCampfireBlock extends CampfireBlock {

    public static final MapCodec<GrillCampfireBlock> CODEC = simpleCodec(GrillCampfireBlock::new);

    public static final IntegerProperty PROGRESS = ModBlockStateProperties.GRILL_PROGRESS;
    public static final int MAX_PROGRESS = ModBlockStateProperties.GRILL_MAX_PROGRESS;

    /** 交互范围铺满整格：这样抬到烤架上的那 4 份食材也能右键点到 */
    private static final VoxelShape INTERACTION_SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 16.0);

    public GrillCampfireBlock(Properties properties) {
        super(true, 1, properties);
        registerDefaultState(defaultBlockState().setValue(PROGRESS, 0));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public MapCodec<CampfireBlock> codec() {
        return (MapCodec) CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(PROGRESS);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GrillBlockEntity(pos, state);
    }

    /** 选中 / 右键用的轮廓：整格高，不然只能点到下面那截营火 */
    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return INTERACTION_SHAPE;
    }

    /** 碰撞还是原版营火的 7/16 高，走路照样能踩过去 */
    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /**
     * 火焰被烤架压住以后，爆裂声和火星都比原版营火少很多
     * （原版是 1/10 出声、1/5 冒火星，这里降到 1/24 和 1/20）。
     */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) {
            return;
        }
        if (random.nextInt(24) == 0) {
            level.playLocalSound(
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    SoundEvents.CAMPFIRE_CRACKLE, SoundSource.BLOCKS,
                    0.35F + random.nextFloat() * 0.4F, random.nextFloat() * 0.7F + 0.6F, false);
        }
        if (random.nextInt(20) == 0) {
            level.addParticle(ParticleTypes.LAVA,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    random.nextFloat() / 2.0F, 5.0E-5, random.nextFloat() / 2.0F);
        }
    }

    // ===== 交互 =====

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        GrillBlockEntity grill = level.getBlockEntity(pos) instanceof GrillBlockEntity existing ? existing : null;
        if (level.isClientSide) {
            return ItemInteractionResult.sidedSuccess(true);
        }
        // 自愈：万一这格是早期版本放坏的（方块在、方块实体没建起来），这里补一个
        if (grill == null) {
            grill = new GrillBlockEntity(pos, state);
            level.setBlockEntity(grill);
        }

        // 空手：取回点到的这一份（连它身上的调料一起）
        if (stack.isEmpty()) {
            // 只认主手：不然一次右键里主手、副手各来一遍，会把手刚空出来的那次也算成一次操作
            if (hand != InteractionHand.MAIN_HAND) {
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }
            int slot = clickedSlot(state, hit);
            if (!grill.getItems().get(slot).isEmpty()) {
                takeBack(player, level, pos, state, grill, slot);
                return ItemInteractionResult.sidedSuccess(false);
            }
            if (!grill.isEmpty()) {
                hint(player, "message.dongbei_delight.grill.no_food_here");
                return ItemInteractionResult.sidedSuccess(false);
            }
            // 架子空了：要潜行右键才收回烤架，免得顺手一点就把烤架撸下来
            if (!player.isSecondaryUseActive()) {
                hint(player, "message.dongbei_delight.grill.sneak_to_remove");
                return ItemInteractionResult.sidedSuccess(false);
            }
            level.setBlock(pos, withoutGrill(state), 3);
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.8F, 1.2F);
            return ItemInteractionResult.sidedSuccess(false);
        }

        // 放食材：优先放在点到的那个角，那个角有东西就找第一个空位
        if (GrillRecipes.byInput(stack) != null) {
            int slot = clickedSlot(state, hit);
            if (!grill.getItems().get(slot).isEmpty()) {
                slot = firstEmptySlot(grill);
            }
            if (slot < 0) {
                hint(player, "message.dongbei_delight.grill.full");
                return ItemInteractionResult.sidedSuccess(false);
            }
            grill.getItems().set(slot, stack.consumeAndReturn(1, player));
            grill.setProgress(slot, 0);
            grill.setChanged();
            level.sendBlockUpdated(pos, state, state, 3);
            level.playSound(null, pos, SoundEvents.COMPOSTER_FILL_SUCCESS, SoundSource.BLOCKS, 1.0F, 1.0F);
            return ItemInteractionResult.sidedSuccess(false);
        }

        // 刷料 / 撒料：瓶装调料要「左手拿瓶子、右手拿刷子」
        boolean brushing = hand == InteractionHand.MAIN_HAND && stack.is(Items.BRUSH);
        ItemStack seasoning = brushing ? player.getOffhandItem() : stack;
        if (seasoning.isEmpty()) {
            return ItemInteractionResult.sidedSuccess(false);
        }
        int slot = seasonTargetSlot(state, hit, grill);
        if (slot < 0) {
            hint(player, "message.dongbei_delight.grill.no_food");
            return ItemInteractionResult.sidedSuccess(false);
        }
        GrillRecipes.Recipe recipe = GrillRecipes.byInput(grill.getItems().get(slot));
        if (recipe == null || !recipe.needsSeasoning(grill.seasoningsOf(slot), seasoning)) {
            hint(player, "message.dongbei_delight.grill.not_needed");
            return ItemInteractionResult.sidedSuccess(false);
        }
        if (SeasoningBottleItem.isBottle(seasoning) && !brushing) {
            hint(player, "message.dongbei_delight.grill.need_brush");
            return ItemInteractionResult.sidedSuccess(false);
        }
        if (!grill.addSeasoning(slot, seasoning)) {
            hint(player, "message.dongbei_delight.grill.full");
            return ItemInteractionResult.sidedSuccess(false);
        }

        if (SeasoningBottleItem.isBottle(seasoning)) {
            // 给一份食材刷一次只花 10 mB；创造模式不扣（和盐、糖这些直接消耗的调料保持一致）
            if (!player.hasInfiniteMaterials()) {
                player.setItemInHand(InteractionHand.OFF_HAND,
                        SeasoningBottleItem.use(seasoning, SeasoningBottleItem.GRILL_DOSE_MB));
            }
            ItemStack left = player.getOffhandItem();
            if (SeasoningBottleItem.isBottle(left)) {
                hint(player, "message.dongbei_delight.grill.brushed",
                        seasoning.getHoverName(), SeasoningBottleItem.remainingMb(left));
            } else {
                hint(player, "message.dongbei_delight.grill.brushed_empty", seasoning.getHoverName());
            }
        } else {
            seasoning.consume(1, player);
            hint(player, "message.dongbei_delight.grill.sprinkled", seasoning.getHoverName());
        }
        level.sendBlockUpdated(pos, state, state, 3);
        level.playSound(null, pos, SoundEvents.COMPOSTER_FILL_SUCCESS, SoundSource.BLOCKS, 1.0F, 0.8F);
        return ItemInteractionResult.sidedSuccess(false);
    }

    /** 鼠标点到的位置在方块里的水平坐标（0~1） */
    private static Vec3 localHit(BlockHitResult hit) {
        BlockPos pos = hit.getBlockPos();
        return new Vec3(hit.getLocation().x - pos.getX(), 0.0, hit.getLocation().z - pos.getZ());
    }

    /** 点到的是哪一份食材：取四个角里离命中点最近的那个 */
    private static int clickedSlot(BlockState state, BlockHitResult hit) {
        Vec3 local = localHit(hit);
        Direction facing = state.getValue(FACING);
        int best = 0;
        double bestDistance = Double.MAX_VALUE;
        for (int slot = 0; slot < GrillBlockEntity.SLOTS; slot++) {
            float[] offset = GrillBlockEntity.slotOffset(slot, facing);
            double dx = local.x - (0.5 + offset[0]);
            double dz = local.z - (0.5 + offset[1]);
            double distance = dx * dx + dz * dz;
            if (distance < bestDistance) {
                bestDistance = distance;
                best = slot;
            }
        }
        return best;
    }

    /** 刷料时刷哪一份：点到的那份有东西就刷它，否则刷离得最近的那份有食材的 */
    private static int seasonTargetSlot(BlockState state, BlockHitResult hit, GrillBlockEntity grill) {
        int clicked = clickedSlot(state, hit);
        if (!grill.getItems().get(clicked).isEmpty()) {
            return clicked;
        }
        Vec3 local = localHit(hit);
        Direction facing = state.getValue(FACING);
        int best = -1;
        double bestDistance = Double.MAX_VALUE;
        for (int slot = 0; slot < GrillBlockEntity.SLOTS; slot++) {
            if (grill.getItems().get(slot).isEmpty()) {
                continue;
            }
            float[] offset = GrillBlockEntity.slotOffset(slot, facing);
            double dx = local.x - (0.5 + offset[0]);
            double dz = local.z - (0.5 + offset[1]);
            double distance = dx * dx + dz * dz;
            if (distance < bestDistance) {
                bestDistance = distance;
                best = slot;
            }
        }
        return best;
    }

    private static int firstEmptySlot(GrillBlockEntity grill) {
        for (int slot = 0; slot < GrillBlockEntity.SLOTS; slot++) {
            if (grill.getItems().get(slot).isEmpty()) {
                return slot;
            }
        }
        return -1;
    }

    private static void takeBack(Player player, Level level, BlockPos pos, BlockState state,
                                 GrillBlockEntity grill, int slot) {
        ItemStack taken = grill.getItems().get(slot).copy();
        grill.clearSlot(slot);
        give(player, taken);
        level.sendBlockUpdated(pos, state, state, 3);
        level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.6F, 1.4F);
    }

    // ===== 每刻：客户端冒烟，服务端烤制 =====

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return createTickerHelper(type, ModBlockEntities.GRILL_CAMPFIRE.get(),
                    (tickLevel, pos, tickState, grill) -> clientTick(tickLevel, pos, tickState, grill));
        }
        return createTickerHelper(type, ModBlockEntities.GRILL_CAMPFIRE.get(),
                (tickLevel, pos, tickState, grill) -> tickGrill(tickLevel, pos, tickState, grill));
    }

    /**
     * 冒烟：火本身偶尔冒一股，架子上有东西时再从东西那边冒一股。
     * 架上烤架以后火被压住，所以烟量只有原版营火的三分之一左右
     * （原版是 0.11 概率出 2~3 股、0.2 概率出 4 颗，这里降到 0.035 出 1~2 股、0.08 出 2 颗）。
     */
    private static void clientTick(Level level, BlockPos pos, BlockState state, GrillBlockEntity grill) {
        RandomSource random = level.random;
        if (random.nextFloat() < 0.035F) {
            for (int i = 0; i < random.nextInt(2) + 1; i++) {
                makeParticles(level, pos, state.getValue(SIGNAL_FIRE), false);
            }
        }

        Direction facing = state.getValue(FACING);
        NonNullList<ItemStack> items = grill.getItems();
        for (int slot = 0; slot < items.size(); slot++) {
            if (items.get(slot).isEmpty() || random.nextFloat() >= 0.08F) {
                continue;
            }
            Direction direction = Direction.from2DDataValue(Math.floorMod(slot + facing.get2DDataValue(), 4));
            double x = pos.getX() + 0.5 - direction.getStepX() * 0.3125 + direction.getClockWise().getStepX() * 0.3125;
            double y = pos.getY() + 0.5;
            double z = pos.getZ() + 0.5 - direction.getStepZ() * 0.3125 + direction.getClockWise().getStepZ() * 0.3125;
            for (int k = 0; k < 2; k++) {
                level.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0, 5.0E-4, 0.0);
            }
        }
    }

    /** 四份食材各自独立计时：谁的调料配齐了谁就开始烤 */
    private static void tickGrill(Level level, BlockPos pos, BlockState state, GrillBlockEntity grill) {
        boolean lit = state.getValue(LIT);
        int highest = 0;
        boolean changed = false;

        for (int slot = 0; slot < GrillBlockEntity.SLOTS; slot++) {
            ItemStack food = grill.getItems().get(slot);
            if (food.isEmpty()) {
                grill.setProgress(slot, 0);
                continue;
            }
            GrillRecipes.Recipe recipe = GrillRecipes.byInput(food);
            // 调料没配齐之前一直停在火上，进度不涨
            if (recipe == null || !recipe.seasoningsReady(grill.seasoningsOf(slot)) || !lit) {
                highest = Math.max(highest, grill.progress(slot));
                continue;
            }

            int step = Math.max(1, recipe.ticks() / MAX_PROGRESS);
            if (level.getGameTime() % step != 0L) {
                highest = Math.max(highest, grill.progress(slot));
                continue;
            }
            int progress = grill.progress(slot) + 1;
            if (progress < MAX_PROGRESS) {
                grill.setProgress(slot, progress);
                highest = Math.max(highest, progress);
                continue;
            }

            // 这份烤好了：成品掉在火上，把这一格腾出来
            ItemStack result = recipe.result().copy();
            grill.clearSlot(slot);
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, result);
            level.playSound(null, pos, SoundEvents.CAMPFIRE_CRACKLE, SoundSource.BLOCKS, 1.0F, 1.2F);
            changed = true;
        }

        // 方块状态里的进度只作汇总（给外部看），真正的进度在方块实体里按份记
        if (state.getValue(PROGRESS) != highest) {
            level.setBlock(pos, state.setValue(PROGRESS, highest), 3);
        }
        if (changed) {
            level.sendBlockUpdated(pos, state, state, 3);
        }
    }

    // ===== 与普通营火互相转换 =====

    /** 把普通营火换成架了烤架的营火，火上正在烤的东西一起搬过去 */
    public static void install(Level level, BlockPos pos, BlockState campfire, CampfireBlockEntity oldFire) {
        NonNullList<ItemStack> carried = NonNullList.withSize(oldFire.getItems().size(), ItemStack.EMPTY);
        for (int i = 0; i < carried.size(); i++) {
            carried.set(i, oldFire.getItems().get(i).copy());
        }
        // 先清空，免得原版营火的 onRemove 把火上的东西掉一地（等下我们自己搬过去）
        oldFire.getItems().clear();

        BlockState grilled = ModBlocks.GRILL_CAMPFIRE.get().defaultBlockState()
                .setValue(LIT, campfire.getValue(LIT))
                .setValue(SIGNAL_FIRE, campfire.getValue(SIGNAL_FIRE))
                .setValue(WATERLOGGED, campfire.getValue(WATERLOGGED))
                .setValue(FACING, campfire.getValue(FACING))
                .setValue(PROGRESS, 0);
        level.setBlock(pos, grilled, 3);

        if (level.getBlockEntity(pos) instanceof GrillBlockEntity grill) {
            grill.setItemsFrom(carried);
            level.sendBlockUpdated(pos, grilled, grilled, 3);
            return;
        }
        // 理论上不会走到这里；真出了意外就把东西还给地面，别弄丢
        for (ItemStack stack : carried) {
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, stack);
            }
        }
    }

    /** 收走烤架，变回普通营火 */
    private static BlockState withoutGrill(BlockState state) {
        return Blocks.CAMPFIRE.defaultBlockState()
                .setValue(LIT, state.getValue(LIT))
                .setValue(SIGNAL_FIRE, state.getValue(SIGNAL_FIRE))
                .setValue(WATERLOGGED, state.getValue(WATERLOGGED))
                .setValue(FACING, state.getValue(FACING));
    }

    /** 拆掉的时候把烤架和火上没取走的东西都掉出来 */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide
                && level.getBlockEntity(pos) instanceof GrillBlockEntity grill) {
            for (ItemStack stack : grill.getItems()) {
                if (!stack.isEmpty()) {
                    popResource(level, pos, stack.copy());
                }
            }
            popResource(level, pos, new ItemStack(ModItems.GRILL_RACK.get()));
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    private static void give(Player player, ItemStack stack) {
        if (!stack.isEmpty() && !player.addItem(stack)) {
            player.drop(stack, false);
        }
    }

    private static void hint(Player player, String translationKey, Object... args) {
        player.displayClientMessage(Component.translatable(translationKey, args), true);
    }
}
