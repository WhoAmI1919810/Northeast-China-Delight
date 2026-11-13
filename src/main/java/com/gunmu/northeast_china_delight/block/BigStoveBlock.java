package com.gunmu.northeast_china_delight.block;

import com.gunmu.northeast_china_delight.advancement.ModAdvancements;
import com.gunmu.northeast_china_delight.item.ModItems;
import com.gunmu.northeast_china_delight.util.DdIds;
import com.gunmu.northeast_china_delight.util.DdStacks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.WorldlyContainerHolder;
//? if >=1.20.5 {
/*import net.minecraft.world.ItemInteractionResult;
*///?}
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.FireChargeItem;
import net.minecraft.world.item.FlintAndSteelItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import org.jetbrains.annotations.Nullable;

import java.util.function.UnaryOperator;

/**
 * 大灶：四个农夫乐事的炉灶在同一层摆成 2×2，就自动拼成一口大灶（见 {@link #tryForm}，放方块事件里调）。
 *
 * <p>四格各是一个方块，{@link #PART} 记着自己是四格里的哪一格（按 facing=north 时的方位：nw / ne 在前排、
 * sw / se 在后排），模型是把整个灶切成的四块，跟着 facing 一起转。灶正中是放锅的灶膛，正面一个灶门。</p>
 *
 * <ul>
 *   <li>拿大铁锅右键放锅，再拿木锅盖右键盖上；空手右键先揭盖、再把锅拿下来（{@link #POT}，四格一起改）。</li>
 *   <li>打火石 / 火焰弹点火，锹灭火（{@link #LIT}，四格一起改；点着亮度 15，可融化灶周积雪）。算农夫乐事的热源（标签里），
 *       点着时上面的厨锅照样能加热。</li>
 *   <li>拆掉任意一格：这一格掉一个农夫乐事炉灶，剩下三格变回炉灶（朝向、点没点着照旧），锅和锅盖掉出来。</li>
 *   <li>东西直接掉进锅里（2026-11-05 用户）：落在锅口的掉落物（{@link #stepOn}）、从上面往下送的漏斗和投掷器
 *       （{@link #getContainer}），收什么见 {@link BigPotCooking#dropIn}。</li>
 * </ul>
 *
 * <p>锅里装的东西存在 nw 那一格的 {@link BigPotBlockEntity} 上；在锅里做菜的入口都在 {@link BigPotCooking}，
 * 锅里的东西由 {@code client/BigPotRenderer} 画。</p>
 *
 * <p>不重写 codec()，理由同 {@link AnimalOilBlock}。</p>
 */
public class BigStoveBlock extends Block implements EntityBlock, WorldlyContainerHolder {

    /** 四格里的哪一格（facing=north 时的方位） */
    public enum Part implements StringRepresentable {
        NW("nw", 0, 0), NE("ne", 1, 0), SW("sw", 0, 1), SE("se", 1, 1);

        private final String name;
        private final int east;
        private final int south;

        Part(String name, int east, int south) {
            this.name = name;
            this.east = east;
            this.south = south;
        }

        @Override
        public String getSerializedName() {
            return name;
        }

        /** 这一格在 2×2 里的位置（相对西北角，x 往东、z 往南）：模型跟着 facing 转，格子的位置也跟着转 */
        int[] offset(Direction facing) {
            return switch (facing) {
                case EAST -> new int[]{1 - south, east};
                case SOUTH -> new int[]{1 - east, 1 - south};
                case WEST -> new int[]{south, 1 - east};
                default -> new int[]{east, south};
            };
        }
    }

    /** 灶上放着什么：没有锅 / 锅 / 锅加锅盖 */
    public enum Pot implements StringRepresentable {
        NONE("none"), POT("pot"), LID("lid");

        private final String name;

        Pot(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);
    public static final EnumProperty<Pot> POT = EnumProperty.create("pot", Pot.class);

    /** 拆的时候会把另外三格改回炉灶，改的过程中别再连锁拆、别重复掉锅（只在服务端线程上用） */
    private static boolean reverting;

    public BigStoveBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PART, Part.NW)
                .setValue(LIT, false).setValue(POT, Pot.NONE));
    }

    // ---------------------------------------------------------------- 拼起来 / 拆开

    private static Block stoveBlock() {
        return BuiltInRegistries.BLOCK.get(DdIds.of("farmersdelight", "stove"));
    }

    /** 农夫乐事的厨锅方块；大灶成型时要把压在上面的厨锅震下来 */
    public static boolean isCookingPot(BlockState state) {
        return state.is(BuiltInRegistries.BLOCK.get(DdIds.of("farmersdelight", "cooking_pot")));
    }

    /**
     * 刚在 pos 放下一个炉灶：看它所在的四个 2×2 里有没有一个四格全是炉灶，有就拼成大灶。
     * 大灶正面朝刚放的这个炉灶的正面（也就是朝着放它的人）；四个里有一个点着，大灶就是点着的。
     */
    public static void tryForm(Level level, BlockPos pos, BlockState placed) {
        if (level.isClientSide || !placed.is(ModBlockTags.BIG_STOVE_PARTS)) {
            return;
        }
        Direction facing = placed.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                ? placed.getValue(BlockStateProperties.HORIZONTAL_FACING) : Direction.NORTH;
        for (int ox = 0; ox < 2; ox++) {
            for (int oz = 0; oz < 2; oz++) {
                BlockPos origin = pos.offset(-ox, 0, -oz);
                boolean all = true;
                boolean lit = false;
                for (int dx = 0; dx < 2 && all; dx++) {
                    for (int dz = 0; dz < 2 && all; dz++) {
                        BlockState s = level.getBlockState(origin.offset(dx, 0, dz));
                        all = s.is(ModBlockTags.BIG_STOVE_PARTS);
                        lit |= all && s.hasProperty(BlockStateProperties.LIT) && s.getValue(BlockStateProperties.LIT);
                    }
                }
                if (!all) {
                    continue;
                }
                // 大灶成型后上面不能再放厨锅：先把已经压在上面的厨锅连内容物一起震下来
                for (Part part : Part.values()) {
                    int[] o = part.offset(facing);
                    BlockPos above = origin.offset(o[0], 0, o[1]).above();
                    if (isCookingPot(level.getBlockState(above))) {
                        level.destroyBlock(above, true);
                    }
                }
                BlockState base = ModBlocks.BIG_STOVE.get().defaultBlockState().setValue(FACING, facing).setValue(LIT, lit);
                for (Part part : Part.values()) {
                    int[] o = part.offset(facing);
                    level.setBlock(origin.offset(o[0], 0, o[1]), base.setValue(PART, part), Block.UPDATE_ALL);
                }
                level.playSound(null, origin.offset(1, 0, 1), SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 1.0F, 0.8F);
                return;
            }
        }
    }

    /** 这口灶 2×2 的西北角 */
    public static BlockPos origin(BlockPos pos, BlockState state) {
        int[] o = state.getValue(PART).offset(state.getValue(FACING));
        return pos.offset(-o[0], 0, -o[1]);
    }

    /** 这口灶的锅（nw 那一格上的方块实体）；那一格不在了返回 null。准星指着哪一格都能找到（Jade 用） */
    @Nullable
    public static BigPotBlockEntity potAt(net.minecraft.world.level.BlockGetter level, BlockPos pos, BlockState state) {
        int[] o = Part.NW.offset(state.getValue(FACING));
        BlockPos at = origin(pos, state).offset(o[0], 0, o[1]);
        return level.getBlockEntity(at) instanceof BigPotBlockEntity pot ? pot : null;
    }

    /** 烹饪信号的 2×2 多格同步：比较器贴着任意一格都要收到刷新通知。 */
    public static void updateAnalogOutput(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide || !(state.getBlock() instanceof BigStoveBlock)) {
            return;
        }
        Direction facing = state.getValue(FACING);
        BlockPos origin = origin(pos, state);
        Block block = state.getBlock();
        for (Part part : Part.values()) {
            int[] o = part.offset(facing);
            BlockPos at = origin.offset(o[0], 0, o[1]);
            BlockState sibling = level.getBlockState(at);
            if (sibling.is(block) && sibling.getValue(FACING) == facing && sibling.getValue(PART) == part) {
                level.updateNeighbourForOutputSignal(at, block);
            }
        }
    }

    /** 四格一起改（只改还在、而且确实是同一口灶的那几格） */
    private void updateAll(Level level, BlockPos pos, BlockState state, UnaryOperator<BlockState> change) {
        Direction facing = state.getValue(FACING);
        BlockPos origin = origin(pos, state);
        for (Part part : Part.values()) {
            int[] o = part.offset(facing);
            BlockPos at = origin.offset(o[0], 0, o[1]);
            BlockState s = level.getBlockState(at);
            if (s.is(this) && s.getValue(FACING) == facing && s.getValue(PART) == part) {
                level.setBlock(at, change.apply(s), Block.UPDATE_ALL);
            }
        }
    }

    /**
     * 大灶头顶不许有农夫乐事厨锅（2026-11-07 用户：「当炉灶变成大灶时，上面就不能放置厨锅」）。右键大灶顶面放厨锅在
     * ModGameplayEvents#onCookingPotOnBigStove 里直接拦了，可从旁边方块的侧面点、用命令放，厨锅照样会落到大灶上，
     * 而大灶在农夫乐事的热源标签里，放上去就能加热做菜。所以头顶一出现厨锅（放方块会通知下面这格），下一刻把它连内容物震下来。
     * 排到下一刻再拆，不在厨锅自己放下的过程中途拆，免得农夫乐事放完方块再去碰已经没了的方块实体。
     */
    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos,
                                boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        if (!level.isClientSide && neighborPos.equals(pos.above()) && isCookingPot(level.getBlockState(neighborPos))) {
            level.scheduleTick(pos, this, 1);
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        BlockPos above = pos.above();
        if (isCookingPot(level.getBlockState(above))) {
            level.destroyBlock(above, true);
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!newState.is(this) && !reverting) {
            reverting = true;
            try {
                BigPotBlockEntity contents = potAt(level, pos, state);
                if (contents != null) {
                    BigPotCooking.onBroken(level, pos, contents);
                }
                Pot pot = state.getValue(POT);
                if (pot != Pot.NONE) {
                    Block.popResource(level, pos, new ItemStack(ModItems.BIG_IRON_POT.get()));
                }
                if (pot == Pot.LID) {
                    Block.popResource(level, pos, takeLid(contents));
                }
                Block stove = stoveBlock();
                Direction facing = state.getValue(FACING);
                BlockPos origin = origin(pos, state);
                for (Part part : Part.values()) {
                    int[] o = part.offset(facing);
                    BlockPos at = origin.offset(o[0], 0, o[1]);
                    BlockState s = level.getBlockState(at);
                    if (at.equals(pos) || !s.is(this) || s.getValue(FACING) != facing || s.getValue(PART) != part) {
                        continue;
                    }
                    BlockState back = stove.defaultBlockState();
                    if (back.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
                        back = back.setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
                    }
                    if (back.hasProperty(BlockStateProperties.LIT)) {
                        back = back.setValue(BlockStateProperties.LIT, s.getValue(LIT));
                    }
                    level.setBlock(at, back, Block.UPDATE_ALL);
                }
            } finally {
                reverting = false;
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    // ---------------------------------------------------------------- 右键

    //? if <1.20.5 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hitResult) {
        ItemStack held = player.getItemInHand(hand);
        if (held.isEmpty()) {
            // 和 1.21.1 一样只认主手空着：主手拿着东西没用上时还会再拿副手问一遍，那时不该把锅拿下来
            return hand == InteractionHand.MAIN_HAND ? takeOff(state, level, pos, player) : InteractionResult.PASS;
        }
        return useItem(held, state, level, pos, player, hand);
    }
    //?} else {
    /*@Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        InteractionResult result = useItem(stack, state, level, pos, player, hand);
        return result == InteractionResult.PASS
                ? ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
                : ItemInteractionResult.sidedSuccess(level.isClientSide());
    }

    /^* 空手右键：先揭锅盖、再拿锅（只认主手空着） ^/
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hitResult) {
        return player.getMainHandItem().isEmpty() ? takeOff(state, level, pos, player) : InteractionResult.PASS;
    }
    *///?}

    private InteractionResult useItem(ItemStack held, BlockState state, Level level, BlockPos pos,
                                      Player player, InteractionHand hand) {
        Pot pot = state.getValue(POT);
        boolean lit = state.getValue(LIT);
        if (held.is(ModItems.BIG_IRON_POT.get()) && pot == Pot.NONE) {
            if (!level.isClientSide) {
                updateAll(level, pos, state, s -> s.setValue(POT, Pot.POT));
                DdStacks.consume(held, 1, player);
                level.playSound(null, pos, SoundEvents.NETHERITE_BLOCK_PLACE, SoundSource.BLOCKS, 1.0F, 0.8F);
                level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
                if (player instanceof ServerPlayer serverPlayer) {
                    ModAdvancements.award(serverPlayer, ModAdvancements.STOKE_STOVE, "stoke");
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (held.is(ModItems.BIG_POT_LID.get()) && pot == Pot.POT) {
            if (!level.isClientSide) {
                // 锅盖原样存进灶里（耐久、附魔），揭下来还的是同一个
                ItemStack lid = DdStacks.consumeAndReturn(held, 1, player);
                placeLid(level, pos, state, lid, player);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        boolean flint = held.getItem() instanceof FlintAndSteelItem;
        if (!lit && (flint || held.getItem() instanceof FireChargeItem)) {
            if (!level.isClientSide) {
                updateAll(level, pos, state, s -> s.setValue(LIT, true));
                if (flint) {
                    DdStacks.hurtAndBreak(held, 1, player, hand);
                } else {
                    DdStacks.consume(held, 1, player);
                }
                level.playSound(null, pos, flint ? SoundEvents.FLINTANDSTEEL_USE : SoundEvents.FIRECHARGE_USE,
                        SoundSource.BLOCKS, 1.0F, level.getRandom().nextFloat() * 0.4F + 0.8F);
                level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (lit && held.is(ItemTags.SHOVELS)) {
            if (!level.isClientSide) {
                updateAll(level, pos, state, s -> s.setValue(LIT, false));
                DdStacks.hurtAndBreak(held, 1, player, hand);
                level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 1.0F);
                level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        // 往锅里放东西、拿大脸盆盛菜：以后做（埋点）
        if (pot == Pot.POT) {
            BigPotBlockEntity contents = potAt(level, pos, state);
            if (contents != null) {
                return BigPotCooking.onUse(contents, held, state, level, pos, player, hand);
            }
        }
        return InteractionResult.PASS;
    }

    /**
     * 把一枚锅盖装到已经放锅的灶上。玩家右键和机械动力动力臂共用这一条路径，
     * 避免两边各写一套状态、音效和开始烹饪的逻辑。
     */
    private static boolean placeLid(Level level, BlockPos pos, BlockState state, ItemStack lid,
                                    @Nullable Player player) {
        if (level.isClientSide || lid.isEmpty() || !(state.getBlock() instanceof BigStoveBlock)
                || state.getValue(POT) != Pot.POT) {
            return false;
        }
        BigPotBlockEntity contents = potAt(level, pos, state);
        if (contents == null) {
            return false;
        }
        ((BigStoveBlock) state.getBlock()).updateAll(level, pos, state, s -> s.setValue(POT, Pot.LID));
        contents.putLid(lid.copyWithCount(1));
        BigPotCooking.lidPlaced(contents, level, pos, state, player);
        level.playSound(null, pos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
        level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
        return true;
    }

    /**
     * 机械动力动力臂的插入入口。锅盖不是食材，不能交给普通物品能力；在交互点里截住，
     * 走与玩家右键相同的 {@link #placeLid} 路径。simulate 只检查能否放，不改状态、不扣物。
     */
    public static ItemStack insertLid(Level level, BlockPos pos, BlockState state, ItemStack stack,
                                      @Nullable Player player, boolean simulate) {
        if (stack.isEmpty() || !stack.is(ModItems.BIG_POT_LID.get())
                || !(state.getBlock() instanceof BigStoveBlock) || state.getValue(POT) != Pot.POT
                || potAt(level, pos, state) == null) {
            return stack;
        }
        ItemStack remainder = stack.copy();
        remainder.shrink(1);
        if (simulate || placeLid(level, pos, state, stack, player)) {
            return remainder;
        }
        return stack;
    }

    /** 灶里存着的那个锅盖；以前的存档没存（或者灶上的方块实体没了）就给个新的 */
    private static ItemStack takeLid(@Nullable BigPotBlockEntity contents) {
        ItemStack lid = contents != null ? contents.takeLid() : ItemStack.EMPTY;
        return lid.isEmpty() ? new ItemStack(ModItems.BIG_POT_LID.get()) : lid;
    }

    private InteractionResult takeOff(BlockState state, Level level, BlockPos pos, Player player) {
        Pot pot = state.getValue(POT);
        if (pot == Pot.NONE) {
            return InteractionResult.PASS;
        }
        BigPotBlockEntity contents = potAt(level, pos, state);
        if (pot == Pot.POT && contents != null) {
            // 先问做菜那边：空手盛菜、潜行把料拿出来
            InteractionResult cooking = BigPotCooking.onEmptyHand(contents, state, level, pos, player);
            if (cooking != InteractionResult.PASS) {
                return cooking;
            }
            if (!BigPotCooking.canTakePot(contents)) {
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        if (isFakePlayer(player)) {
            // 机械动力的机械手（Deployer）等假玩家空手右键：盛菜（上面 onEmptyHand）照常，揭盖、拿锅不行。
            // 2026-11-08 用户报机械手把锅取出来了：机械手投完手里的料、手空了照样右键，以前就走到这里把锅盖、锅拿走
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            if (pot == Pot.LID && contents != null) {
                // 反悔等待中揭盖取消；煮着或煮好了揭盖只按常速继续 / 冒一股白雾
                BigPotCooking.lidRemoved(contents, level, pos, state);
            }
            Pot next = pot == Pot.LID ? Pot.POT : Pot.NONE;
            updateAll(level, pos, state, s -> s.setValue(POT, next));
            ItemStack item = pot == Pot.LID ? takeLid(contents) : new ItemStack(ModItems.BIG_IRON_POT.get());
            if (!player.getInventory().add(item)) {
                player.drop(item, false);
            }
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2F, 1.0F);
            level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** 机械动力的机械手（Deployer）用的是加载器的假玩家 */
    static boolean isFakePlayer(Player player) {
        //? if <1.20.2 {
        return player instanceof net.minecraftforge.common.util.FakePlayer;
        //?} else {
        /*return player instanceof net.neoforged.neoforge.common.util.FakePlayer;
        *///?}
    }

    // ---------------------------------------------------------------- 掉进锅里

    /** 锅口那一圈的半径（格，从四格的交点量）：锅沿外圈 13.75 像素，往里收到内壁 */
    private static final double MOUTH_RADIUS = 0.8;

    /**
     * 掉落物落在锅口里就进锅（2026-11-05 用户，收什么见 {@link BigPotCooking#dropIn}）：一堆能收几个收几个，
     * 收不进的留在灶上；停在灶上的掉落物原版每 4 刻还会挪一下、再调到这里，锅里有了水、腾出地方就接着收。
     * 落在锅外的四个角上不收。返还的空桶、空盆、调料瓶、碗先送进大灶相邻库存，送不进才从锅口外沿弹出。
     */
    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        super.stepOn(level, pos, state, entity);
        if (level.isClientSide || !(entity instanceof ItemEntity item) || !item.isAlive()
                || state.getValue(POT) != Pot.POT) {
            return;
        }
        BigPotBlockEntity pot = potAt(level, pos, state);
        if (pot == null) {
            return;
        }
        Vec3 center = BigPotCooking.center(pos, state);
        double dx = item.getX() - center.x;
        double dz = item.getZ() - center.z;
        if (dx * dx + dz * dz > MOUTH_RADIUS * MOUTH_RADIUS) {
            return;
        }
        ItemStack stack = item.getItem().copy();
        int before = stack.getCount();
        while (!stack.isEmpty()) {
            ItemStack back = BigPotCooking.dropIn(pot, state, level, pos, stack, item, false);
            if (back == null) {
                break;
            }
            stack.shrink(1);
            BigPotCooking.output(level, pos, state, back);
        }
        if (stack.getCount() != before) {
            if (stack.isEmpty()) {
                item.discard();
            } else {
                item.setItem(stack);
            }
        }
    }

    /**
     * 漏斗、投掷器从上面往锅里送（2026-11-05 用户：漏斗、投掷器等送来的也收）：灶上有锅时，灶就是一个只能从顶上
     * 放进去的「箱子」，同原版堆肥桶。原版漏斗、投掷器找送去哪时认这个（Forge、NeoForge 都是没有物品能力才问这里，
     * 大灶没有物品能力；NeoForge 只给原版堆肥桶套了物品能力，机械动力的漏斗这类按物品能力找的不认）。
     * 收什么同掉落物（{@link BigPotCooking#dropIn}）；收不进的留在漏斗、投掷器里，等锅里腾出地方（同往满了的箱子里送）。
     * 灶上没锅时不算箱子，投掷器照常把东西扔出去。
     */
    @Override
    public WorldlyContainer getContainer(BlockState state, LevelAccessor level, BlockPos pos) {
        if (state.getValue(POT) == Pot.NONE || !(level instanceof Level world) || world.isClientSide) {
            return null;
        }
        BigPotBlockEntity pot = potAt(world, pos, state);
        return pot == null ? null : new PotInput(pot, state, world, pos);
    }

    /**
     * 从上面送进锅的一格：看着永远是空的、一次只收一个，放进来马上倒进锅（setChanged 时），同原版堆肥桶的 InputContainer。
     * 万一放进来的收不进（问的时候能收、放的时候锅变了），就送到相邻库存或从锅口外沿弹出，不吞东西
     */
    private static final class PotInput extends SimpleContainer implements WorldlyContainer {
        private static final int[] TOP = {0};
        private static final int[] NONE = {};

        private final BigPotBlockEntity pot;
        private final BlockState state;
        private final Level level;
        private final BlockPos pos;

        PotInput(BigPotBlockEntity pot, BlockState state, Level level, BlockPos pos) {
            super(1);
            this.pot = pot;
            this.state = state;
            this.level = level;
            this.pos = pos;
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public int[] getSlotsForFace(Direction side) {
            return side == Direction.UP ? TOP : NONE;
        }

        @Override
        public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
            return side == Direction.UP && BigPotCooking.dropIn(pot, state, level, pos, stack, null, true) != null;
        }

        @Override
        public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
            return false;
        }

        @Override
        public void setChanged() {
            ItemStack stack = removeItemNoUpdate(0);
            while (!stack.isEmpty()) {
                ItemStack back = BigPotCooking.dropIn(pot, state, level, pos, stack, null, false);
                if (back == null) {
                    BigPotCooking.output(level, pos, state, stack);
                    return;
                }
                stack.shrink(1);
                BigPotCooking.output(level, pos, state, back);
            }
        }
    }

    // ---------------------------------------------------------------- 火苗

    /** 点着时灶门口冒火苗和烟、偶尔噼啪响；每口灶只让 nw 那一格出（灶门在 nw / ne 交界上） */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT) || state.getValue(PART) != Part.NW) {
            return;
        }
        BlockPos origin = origin(pos, state);
        // 灶门口：facing=north 时在 2×2 的 (x 16, y 4..8, z 0) 像素处，跟着 facing 转
        double u = 16 + (random.nextDouble() - 0.5) * 6;
        double v = -0.5;
        double[] xz = switch (state.getValue(FACING)) {
            case EAST -> new double[]{32 - v, u};
            case SOUTH -> new double[]{32 - u, 32 - v};
            case WEST -> new double[]{v, 32 - u};
            default -> new double[]{u, v};
        };
        double x = origin.getX() + xz[0] / 16.0;
        double y = origin.getY() + (4 + random.nextDouble() * 4) / 16.0;
        double z = origin.getZ() + xz[1] / 16.0;
        if (random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.FLAME, x, y, z, 0.0, 0.01, 0.0);
        }
        if (random.nextInt(4) == 0) {
            level.addParticle(ParticleTypes.SMOKE, x, y + 0.2, z, 0.0, 0.02, 0.0);
        }
        if (random.nextInt(12) == 0) {
            level.playLocalSound(x, y, z, SoundEvents.CAMPFIRE_CRACKLE, SoundSource.BLOCKS,
                    0.6F + random.nextFloat() * 0.4F, 0.8F + random.nextFloat() * 0.4F, false);
        }
    }

    // ---------------------------------------------------------------- 方块实体（只在 nw 那一格）

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        if (state.getValue(PART) == Part.NW) {
            return new BigPotBlockEntity(pos, state);
        }
        // 另外三格：1.20.1 挂一个空的方块实体放物品能力（Forge 的能力只能挂在方块实体上），1.20.2 起按方块注册能力，不用
        //? if <1.20.2 {
        return new BigStovePartBlockEntity(pos, state);
        //?} else {
        /*return null;
        *///?}
    }

    /**
     * 灶上有锅时才 tick：服务端煮菜（BigPotCooking.serverTick，还得点着），客户端冒泡、冒白雾、谜之炖菜飘效果粒子
     * （BigPotCooking.clientTick；煮好以后的冒泡、效果粒子灭了火也有，所以客户端不看点没点着。
     * 不放在 animateTick 里，那个是随机抽周围的方块调，一格两三秒才轮到一次，冒泡会一阵一阵的）
     */
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (state.getValue(PART) != Part.NW || state.getValue(POT) == Pot.NONE
                || type != ModBlockEntities.BIG_POT.get() || !level.isClientSide && !state.getValue(LIT)) {
            return null;
        }
        if (level.isClientSide) {
            return (tickLevel, tickPos, tickState, be) -> {
                if (be instanceof BigPotBlockEntity pot) {
                    BigPotCooking.clientTick(tickLevel, tickPos, tickState, pot);
                }
            };
        }
        return (tickLevel, tickPos, tickState, be) -> {
            if (be instanceof BigPotBlockEntity pot) {
                BigPotCooking.serverTick(tickLevel, tickPos, tickState, pot);
            }
        };
    }

    // ---------------------------------------------------------------- 结构方块旋转 / 镜像

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    /**
     * 镜像：不管镜面怎么摆，四格都是左右（东西）对调，朝向照原版镜像（镜面和正面平行时正面翻到另一边）。
     * 灶本身左右对称，模型不用镜像。
     */
    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        if (mirror == Mirror.NONE) {
            return state;
        }
        Part part = switch (state.getValue(PART)) {
            case NW -> Part.NE;
            case NE -> Part.NW;
            case SW -> Part.SE;
            case SE -> Part.SW;
        };
        return state.setValue(PART, part).setValue(FACING, mirror.mirror(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART, LIT, POT);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return state.getValue(POT) != Pot.NONE;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        BigPotBlockEntity pot = potAt(level, pos, state);
        return pot == null ? 0 : pot.comparatorOutput();
    }
}
