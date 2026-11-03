package com.gunmu.northeast_china_delight.block;

import com.gunmu.northeast_china_delight.item.ModItems;
import com.gunmu.northeast_china_delight.util.DdIds;
import com.gunmu.northeast_china_delight.util.DdStacks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
//? if >=1.20.5 {
/*import net.minecraft.world.ItemInteractionResult;
*///?}
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.FireChargeItem;
import net.minecraft.world.item.FlintAndSteelItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
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
 *   <li>打火石 / 火焰弹点火，锹灭火（{@link #LIT}，四格一起改；点着亮度 13）。算农夫乐事的热源（标签里），
 *       点着时上面的厨锅照样能加热。</li>
 *   <li>拆掉任意一格：这一格掉一个农夫乐事炉灶，剩下三格变回炉灶（朝向、点没点着照旧），锅和锅盖掉出来。</li>
 * </ul>
 *
 * <p>锅里装的东西存在 nw 那一格的 {@link BigPotBlockEntity} 上；在锅里做菜的入口都在 {@link BigPotCooking}，
 * 锅里的东西由 {@code client/BigPotRenderer} 画。</p>
 *
 * <p>不重写 codec()，理由同 {@link AnimalOilBlock}。</p>
 */
public class BigStoveBlock extends Block implements EntityBlock {

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
                    Block.popResource(level, pos, new ItemStack(ModItems.BIG_POT_LID.get()));
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
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (held.is(ModItems.BIG_POT_LID.get()) && pot == Pot.POT) {
            if (!level.isClientSide) {
                updateAll(level, pos, state, s -> s.setValue(POT, Pot.LID));
                DdStacks.consume(held, 1, player);
                level.playSound(null, pos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
                level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
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
                if (!level.isClientSide) {
                    player.displayClientMessage(
                            Component.translatable("message.northeast_china_delight.big_pot.not_empty"), true);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        if (!level.isClientSide) {
            if (pot == Pot.LID && contents != null) {
                // 煮着或煮好了揭盖：冒一股白雾
                BigPotCooking.lidLifted(level, pos, state, contents);
            }
            Pot next = pot == Pot.LID ? Pot.POT : Pot.NONE;
            updateAll(level, pos, state, s -> s.setValue(POT, next));
            ItemStack item = new ItemStack(pot == Pot.LID ? ModItems.BIG_POT_LID.get() : ModItems.BIG_IRON_POT.get());
            if (!player.getInventory().add(item)) {
                player.drop(item, false);
            }
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2F, 1.0F);
            level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
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
        return state.getValue(PART) == Part.NW ? new BigPotBlockEntity(pos, state) : null;
    }

    /**
     * 只有点着、而且灶上有锅时才 tick：服务端煮菜（BigPotCooking.serverTick），客户端冒泡、冒白雾（BigPotCooking.clientTick；
     * 不放在 animateTick 里，那个是随机抽周围的方块调，一格两三秒才轮到一次，冒泡会一阵一阵的）
     */
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (state.getValue(PART) != Part.NW || !state.getValue(LIT)
                || state.getValue(POT) == Pot.NONE || type != ModBlockEntities.BIG_POT.get()) {
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
}
