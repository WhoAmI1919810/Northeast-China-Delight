package com.gunmu.northeast_china_delight.block;

import com.gunmu.northeast_china_delight.advancement.ModAdvancements;
import com.gunmu.northeast_china_delight.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
//? if >=1.20.5 {
import net.minecraft.world.ItemInteractionResult;
//?}
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * 脸盆架：四条腿、托盆的方框，后两根立柱上搭毛巾、顶上一面小镜子（镜子朝着放的人）。
 *
 * <p>拿着大脸盆 / 水盆右键空架子，把盆放上去；水盆架用空手右键洗脸，潜行空手右键把盆拿下来（背包满了就掉在脚下）。
 * 架子上的盆和地上的一样能倒水、舀水（照 {@link BasinBlock}）：空盆拿水桶 / 水盆右键倒满，水盆拿空桶 / 空盆右键舀走。
 * 拆架子时盆一起掉出来（掉落表按 basin 属性给）。</p>
 */
public class BasinStandBlock extends FacingDecorBlock implements EntityBlock {

    public enum Basin implements StringRepresentable {
        NONE("none"), BASIN("basin"), WATER("water");

        private final String name;

        Basin(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }

        ItemStack toItem() {
            return new ItemStack(this == WATER ? ModItems.WATER_BASIN.get() : ModItems.LARGE_BASIN.get());
        }
    }

    public static final EnumProperty<Basin> BASIN = EnumProperty.create("basin", Basin.class);

    /** 架子四角在 1..15（立柱、镜子高出一格的部分不算进选框）；架子上的盆缩到 0.8 倍、整个落在架子里面，放不放盆一个形状 */
    private static final VoxelShape STAND_SHAPE = Block.box(1, 0, 1, 15, 16, 15);

    /** 从脸盆架获得的生命恢复时长：3 秒。 */
    private static final int HEAL_DURATION_TICKS = 3 * 20;

    /** 任意脸盆架共享的玩家冷却：5 秒。 */
    private static final int HEAL_COOLDOWN_TICKS = 5 * 20;

    /** 写在玩家持久数据里，登出、重登和跨维度都不会重置。 */
    private static final String TAG_HEAL_READY_AT = "northeast_china_delight:basin_stand_heal_ready_at";

    public BasinStandBlock(BlockBehaviour.Properties properties) {
        super(properties, STAND_SHAPE, true);
        registerDefaultState(defaultBlockState().setValue(BASIN, Basin.NONE));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BasinStandBlockEntity(pos, state);
    }

    //? if <1.20.5 {
    /*@Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hitResult) {
        ItemStack held = player.getItemInHand(hand);
        if (held.isEmpty()) {
            // 和 1.21.1 一样只认主手空着：主手拿着东西没用上时还会再拿副手问一遍，那时不该把盆拿下来
            return hand == InteractionHand.MAIN_HAND ? handleEmptyHand(state, level, pos, player) : InteractionResult.PASS;
        }
        return useBasin(held, state, level, pos, player, hand);
    }
    *///?} else {
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        InteractionResult result = useBasin(stack, state, level, pos, player, hand);
        return result == InteractionResult.PASS
                ? ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
                : ItemInteractionResult.sidedSuccess(level.isClientSide());
    }

    /** 空手右键：水盆洗脸，其它状态把盆拿下来（只认主手空着，手里拿着东西时让给物品自己的用法） */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hitResult) {
        return player.getMainHandItem().isEmpty() ? handleEmptyHand(state, level, pos, player) : InteractionResult.PASS;
    }
    //?}

    /** 空手右键：正常右键水盆洗脸，潜行右键或其它状态则取下盆。 */
    private static InteractionResult handleEmptyHand(BlockState state, Level level, BlockPos pos, Player player) {
        if (state.getValue(BASIN) == Basin.WATER && !player.isSecondaryUseActive()) {
            return wash(state, level, pos, player);
        }
        return takeBasin(state, level, pos, player);
    }

    /**
     * 洗脸：每次空手右键都出水花；玩家冷却好了才给 3 秒生命恢复 I，所有脸盆架共用该冷却。
     * 一盆水洗三次（2026-11-07 用户）：方块实体记已经洗过几次，用过 {@link BasinStandBlockEntity#USES_BEFORE_EMPTY} 次以后
     * 再洗一次照常出水花、给效果，然后水盆变空盆（次数跟着清零，见 {@link BasinStandBlockEntity#setBlockState}）。
     */
    private static InteractionResult wash(BlockState state, Level level, BlockPos pos, Player player) {
        if (level instanceof ServerLevel server) {
            if (player instanceof ServerPlayer serverPlayer) {
                ModAdvancements.award(serverPlayer, ModAdvancements.WASH_FACE, "wash");
            }
            double x = pos.getX() + 0.5;
            double y = pos.getY() + 0.75;
            double z = pos.getZ() + 0.5;
            server.sendParticles(ParticleTypes.SPLASH, x, y, z, 16, 0.22, 0.08, 0.22, 0.06);
            server.sendParticles(ParticleTypes.FALLING_WATER, x, y + 0.03, z, 6, 0.16, 0.01, 0.16, 0.0);
            if (tryStartHealing(player, server.getServer().overworld().getGameTime())) {
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, HEAL_DURATION_TICKS, 0));
            }
            if (level.getBlockEntity(pos) instanceof BasinStandBlockEntity stand) {
                if (stand.uses() >= BasinStandBlockEntity.USES_BEFORE_EMPTY) {
                    level.setBlock(pos, state.setValue(BASIN, Basin.BASIN), Block.UPDATE_ALL);
                    level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
                } else {
                    stand.setUses(stand.uses() + 1);
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** 冷却跟随玩家而不是脸盆架；玩家在冷却内换架右键也不会再次获得效果。 */
    private static boolean tryStartHealing(Player player, long gameTime) {
        CompoundTag data = player.getPersistentData();
        long readyAt = data.getLong(TAG_HEAL_READY_AT);
        if (gameTime < readyAt) {
            return false;
        }
        data.putLong(TAG_HEAL_READY_AT, gameTime + HEAL_COOLDOWN_TICKS);
        return true;
    }

    private static InteractionResult takeBasin(BlockState state, Level level, BlockPos pos, Player player) {
        Basin basin = state.getValue(BASIN);
        if (basin == Basin.NONE) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            level.setBlock(pos, state.setValue(BASIN, Basin.NONE), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2F, 1.0F);
            level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
            ItemStack item = basin.toItem();
            if (!player.getInventory().add(item)) {
                player.drop(item, false);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static InteractionResult useBasin(ItemStack held, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand) {
        Basin basin = state.getValue(BASIN);
        if (basin == Basin.NONE) {
            Basin put = held.is(ModItems.LARGE_BASIN.get()) ? Basin.BASIN
                    : held.is(ModItems.WATER_BASIN.get()) ? Basin.WATER : null;
            if (put == null) {
                return InteractionResult.PASS;
            }
            if (!level.isClientSide) {
                level.setBlock(pos, state.setValue(BASIN, put), Block.UPDATE_ALL);
                if (!player.getAbilities().instabuild) {
                    held.shrink(1);
                }
                level.playSound(null, pos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
                level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        // 桶和盆都行：倒进去剩空桶 / 空盆，舀出来得水桶 / 水盆（同 BasinBlock）
        boolean pour = basin == Basin.BASIN && (held.is(Items.WATER_BUCKET) || held.is(ModItems.WATER_BASIN.get()));
        boolean scoop = basin == Basin.WATER && (held.is(Items.BUCKET) || held.is(ModItems.LARGE_BASIN.get()));
        if (!pour && !scoop) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            boolean withBasin = held.is(ModItems.WATER_BASIN.get()) || held.is(ModItems.LARGE_BASIN.get());
            ItemStack result = new ItemStack(withBasin
                    ? (pour ? ModItems.LARGE_BASIN.get() : ModItems.WATER_BASIN.get())
                    : (pour ? Items.BUCKET : Items.WATER_BUCKET));
            player.awardStat(Stats.ITEM_USED.get(held.getItem()));
            player.setItemInHand(hand, ItemUtils.createFilledResult(held, player, result));
            level.setBlock(pos, state.setValue(BASIN, pour ? Basin.WATER : Basin.BASIN), Block.UPDATE_ALL);
            level.playSound(null, pos, pour ? SoundEvents.BUCKET_EMPTY : SoundEvents.BUCKET_FILL,
                    SoundSource.BLOCKS, 1.0F, 1.0F);
            level.gameEvent(player, pour ? GameEvent.FLUID_PLACE : GameEvent.FLUID_PICKUP, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(BASIN);
    }
}
