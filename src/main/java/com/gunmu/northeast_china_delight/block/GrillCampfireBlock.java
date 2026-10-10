package com.gunmu.northeast_china_delight.block;

import com.gunmu.northeast_china_delight.crafting.GrillRecipes;
import com.gunmu.northeast_china_delight.item.GrillSeasonings;
import com.gunmu.northeast_china_delight.item.ModItems;
import com.gunmu.northeast_china_delight.item.SeasoningBottleItem;
import com.gunmu.northeast_china_delight.util.DdPlayers;
import com.gunmu.northeast_china_delight.util.DdStacks;
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
import net.minecraft.world.InteractionResult;
//? if >=1.20.5 {
/*import net.minecraft.world.ItemInteractionResult;
*///?}
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
 * 架上烧烤架的营火。一个烤架能放 4 份食材 （什么都能放、可以混着放），四份各自独立计时。
 */
public class GrillCampfireBlock extends CampfireBlock {

    //? if >=1.20.2 {
    /*public static final MapCodec<GrillCampfireBlock> CODEC = simpleCodec(GrillCampfireBlock::new);
    *///?}

    public static final net.minecraft.world.level.block.state.properties.BooleanProperty SOUL =
            net.minecraft.world.level.block.state.properties.BooleanProperty.create("soul");
    public static final IntegerProperty PROGRESS = ModBlockStateProperties.GRILL_PROGRESS;
    public static final int MAX_PROGRESS = ModBlockStateProperties.GRILL_MAX_PROGRESS;

    private static final VoxelShape INTERACTION_SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 16.0);

    public GrillCampfireBlock(Properties properties) {
        super(true, 1, properties);
        registerDefaultState(defaultBlockState().setValue(PROGRESS, 0).setValue(SOUL, false));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    //? if >=1.20.2 {
    /*@Override
    public MapCodec<CampfireBlock> codec() {
        return (MapCodec) CODEC;
    }
    *///?}

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(PROGRESS);
        builder.add(SOUL);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GrillBlockEntity(pos, state);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return INTERACTION_SHAPE;
    }

    /** 碰撞还是原版营火的 7/16 高，走路照样能踩过去 */
    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

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
        if (state.getValue(SOUL)) {
            if (random.nextInt(10) == 0) {
                level.addParticle(ParticleTypes.SOUL_FIRE_FLAME,
                        pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        0.0, 0.0, 0.0);
            }
            if (random.nextInt(5) == 0) {
                for (int i = 0; i < random.nextInt(1) + 1; ++i) {
                    level.addParticle(ParticleTypes.SOUL,
                            pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                            (random.nextFloat() / 2.0F), 5.0E-5, (random.nextFloat() / 2.0F));
                }
            }
        }
    }

    @Override
    //? if <1.20.5 {
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        InteractionResult result = this.useOnGrill(player.getItemInHand(hand), state, level, pos, player, hand, hit);
        return result == InteractionResult.PASS ? super.use(state, level, pos, player, hand, hit) : result;
    }
    //?} else {
    /*protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        InteractionResult result = this.useOnGrill(stack, state, level, pos, player, hand, hit);
        return result == InteractionResult.PASS
                ? super.useItemOn(stack, state, level, pos, player, hand, hit)
                : ItemInteractionResult.sidedSuccess(level.isClientSide);
    }
    *///?}

    private InteractionResult useOnGrill(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                         Player player, InteractionHand hand, BlockHitResult hit) {
        GrillBlockEntity grill = level.getBlockEntity(pos) instanceof GrillBlockEntity existing ? existing : null;
        if (level.isClientSide) {
            return InteractionResult.sidedSuccess(true);
        }
        if (grill == null) {
            grill = new GrillBlockEntity(pos, state);
            level.setBlockEntity(grill);
        }

        if (stack.isEmpty()) {
            if (hand != InteractionHand.MAIN_HAND) {
                return InteractionResult.PASS;
            }
            int slot = clickedSlot(state, hit);
            if (!grill.getItems().get(slot).isEmpty()) {
                takeBack(player, level, pos, state, grill, slot);
                return InteractionResult.sidedSuccess(false);
            }
            if (!grill.isEmpty()) {
                hint(player, "message.northeast_china_delight.grill.no_food_here");
                return InteractionResult.sidedSuccess(false);
            }
            if (!player.isSecondaryUseActive()) {
                hint(player, "message.northeast_china_delight.grill.sneak_to_remove");
                return InteractionResult.sidedSuccess(false);
            }
            level.setBlock(pos, withoutGrill(state), 3);
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.8F, 1.2F);
            return InteractionResult.sidedSuccess(false);
        }

        if (GrillRecipes.byInput(stack) != null) {
            int slot = clickedSlot(state, hit);
            if (!grill.getItems().get(slot).isEmpty()) {
                slot = firstEmptySlot(grill);
            }
            if (slot < 0) {
                hint(player, "message.northeast_china_delight.grill.full");
                return InteractionResult.sidedSuccess(false);
            }
            grill.getItems().set(slot, DdStacks.consumeAndReturn(stack, 1, player));
            grill.setProgress(slot, 0);
            grill.setChanged();
            level.sendBlockUpdated(pos, state, state, 3);
            level.playSound(null, pos, SoundEvents.COMPOSTER_FILL_SUCCESS, SoundSource.BLOCKS, 1.0F, 1.0F);
            return InteractionResult.sidedSuccess(false);
        }

        boolean brushing = hand == InteractionHand.MAIN_HAND && stack.is(Items.BRUSH);
        ItemStack seasoning = brushing ? player.getOffhandItem() : stack;
        if (seasoning.isEmpty()) {
            return InteractionResult.sidedSuccess(false);
        }
        int slot = seasonTargetSlot(state, hit, grill);
        if (slot < 0) {
            hint(player, "message.northeast_china_delight.grill.no_food");
            return InteractionResult.sidedSuccess(false);
        }
        GrillRecipes.Recipe recipe = GrillRecipes.byInput(grill.getItems().get(slot));
        if (recipe == null || !recipe.needsSeasoning(grill.seasoningsOf(slot), seasoning)) {
            hint(player, "message.northeast_china_delight.grill.not_needed");
            return InteractionResult.sidedSuccess(false);
        }
        if (GrillSeasonings.needsBrush(seasoning) && !brushing) {
            hint(player, "message.northeast_china_delight.grill.need_brush");
            return InteractionResult.sidedSuccess(false);
        }
        if (!grill.addSeasoning(slot, seasoning)) {
            hint(player, "message.northeast_china_delight.grill.full");
            return InteractionResult.sidedSuccess(false);
        }

        if (GrillSeasonings.isSauce(seasoning)) {
            if (!DdPlayers.hasInfiniteMaterials(player)) {
                DdStacks.consume(seasoning, 1, player);
            }
            hint(player, "message.northeast_china_delight.grill.brushed_sauce", seasoning.getHoverName());
        } else if (SeasoningBottleItem.isBottle(seasoning)) {
            if (!DdPlayers.hasInfiniteMaterials(player)) {
                player.setItemInHand(InteractionHand.OFF_HAND,
                        SeasoningBottleItem.use(seasoning, SeasoningBottleItem.GRILL_DOSE_MB));
            }
            ItemStack left = player.getOffhandItem();
            if (SeasoningBottleItem.isBottle(left)) {
                hint(player, "message.northeast_china_delight.grill.brushed",
                        seasoning.getHoverName(), SeasoningBottleItem.remainingMb(left));
            } else {
                hint(player, "message.northeast_china_delight.grill.brushed_empty", seasoning.getHoverName());
            }
        } else {
            DdStacks.consume(seasoning, 1, player);
            hint(player, "message.northeast_china_delight.grill.sprinkled", seasoning.getHoverName());
        }
        level.sendBlockUpdated(pos, state, state, 3);
        level.playSound(null, pos, SoundEvents.COMPOSTER_FILL_SUCCESS, SoundSource.BLOCKS, 1.0F, 0.8F);
        return InteractionResult.sidedSuccess(false);
    }

    private static Vec3 localHit(BlockHitResult hit) {
        BlockPos pos = hit.getBlockPos();
        return new Vec3(hit.getLocation().x - pos.getX(), 0.0, hit.getLocation().z - pos.getZ());
    }

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

            ItemStack result = recipe.result().copy();
            grill.clearSlot(slot);
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, result);
            level.playSound(null, pos, SoundEvents.CAMPFIRE_CRACKLE, SoundSource.BLOCKS, 1.0F, 1.2F);
            changed = true;
        }

        if (state.getValue(PROGRESS) != highest) {
            level.setBlock(pos, state.setValue(PROGRESS, highest), 3);
        }
        if (changed) {
            level.sendBlockUpdated(pos, state, state, 3);
        }
    }

    public static void install(Level level, BlockPos pos, BlockState campfire, CampfireBlockEntity oldFire) {
        NonNullList<ItemStack> carried = NonNullList.withSize(oldFire.getItems().size(), ItemStack.EMPTY);
        for (int i = 0; i < carried.size(); i++) {
            carried.set(i, oldFire.getItems().get(i).copy());
        }
        oldFire.getItems().clear();

        BlockState grilled = ModBlocks.GRILL_CAMPFIRE.get().defaultBlockState()
                .setValue(LIT, campfire.getValue(LIT))
                .setValue(SIGNAL_FIRE, campfire.getValue(SIGNAL_FIRE))
                .setValue(WATERLOGGED, campfire.getValue(WATERLOGGED))
                .setValue(FACING, campfire.getValue(FACING))
                .setValue(SOUL, campfire.is(Blocks.SOUL_CAMPFIRE))
                .setValue(PROGRESS, 0);
        level.setBlock(pos, grilled, 3);

        if (level.getBlockEntity(pos) instanceof GrillBlockEntity grill) {
            grill.setItemsFrom(carried);
            level.sendBlockUpdated(pos, grilled, grilled, 3);
            return;
        }
        for (ItemStack stack : carried) {
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, stack);
            }
        }
    }

    private static BlockState withoutGrill(BlockState state) {
        Block base = state.getValue(SOUL) ? Blocks.SOUL_CAMPFIRE : Blocks.CAMPFIRE;
        return base.defaultBlockState()
                .setValue(LIT, state.getValue(LIT))
                .setValue(SIGNAL_FIRE, state.getValue(SIGNAL_FIRE))
                .setValue(WATERLOGGED, state.getValue(WATERLOGGED))
                .setValue(FACING, state.getValue(FACING));
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
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
