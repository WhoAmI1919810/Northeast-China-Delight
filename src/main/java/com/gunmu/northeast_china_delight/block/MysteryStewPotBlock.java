package com.gunmu.northeast_china_delight.block;

import com.gunmu.northeast_china_delight.item.ModItems;
import com.gunmu.northeast_china_delight.item.MysteryStewEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 盆装谜之炖菜放下来的那一盆（2026-11-05 用户）：和别的盆装菜一样拿碗盛、能盛 4 碗、盛完剩个空大脸盆（{@link DdFeastBlock}），
 * 只是盛出来的是一碗原版的谜之炖菜，效果照这一盆存的给（{@link MysteryStewPotBlockEntity}）。
 *
 * <p>彩蛋（2026-11-05 用户）：放在灵魂营火的烧烤架上（底下那格是 {@link GrillCampfireBlock}，{@code soul} 为真）时，
 * 盆里变成一只蓝色章鱼（{@link #OCTOPUS}，模型 block/mystery_stew_pot_octopus_servings*，用户画的）。
 * 底下换了就变回来；只是样子，盛出来的碗装谜之炖菜、收回的盆装谜之炖菜都不受影响。</p>
 *
 * <p>盆里往上飘效果粒子（2026-11-05 用户：形状和玩家身上有状态效果时的粒子一样，颜色取自两种谜之炖菜的颜色），
 * 见 {@link #effectParticle}；锅里煮好的一锅也飘（BigPotCooking#clientTick）。</p>
 *
 * <p>不重写 codec()，理由同 {@link AnimalOilBlock}：父类 {@link DdFeastBlock} 的 codec() 写死了自己的类型。</p>
 */
public class MysteryStewPotBlock extends DdFeastBlock implements EntityBlock {

    /** 放在灵魂营火的烧烤架上：盆里是一只蓝色章鱼 */
    public static final BooleanProperty OCTOPUS = BooleanProperty.create("octopus");

    /**
     * 效果粒子的颜色，都取自用户画的贴图：正常的一盆是蓝汤和紫、绿、深蓝的料块（mystery_stew_pot.png），
     * 章鱼那盆是章鱼身上的几种蓝和蓝汤（mystery_stew_pot_octopus.png）
     */
    private static final int[] COLORS = {0x4f90b8, 0x963696, 0x72ae33, 0x2b316d};
    private static final int[] OCTOPUS_COLORS = {0x37479c, 0x263898, 0x2b316d, 0x4f90b8};
    /** 盆里每刻往上飘一个效果粒子的几率 */
    private static final float EFFECT_CHANCE = 0.2F;
    /** 汤面多高（像素），按还剩几份（用户模型里汤那一层的顶面：剩 1 份 2.25 …… 满盆 4；0 份时方块已经变回空盆） */
    private static final float[] SURFACE_Y = {2.25F, 2.25F, 3.25F, 3.5F, 4.0F};

    public MysteryStewPotBlock(BlockBehaviour.Properties properties) {
        super(properties, () -> Items.SUSPICIOUS_STEW);
        this.registerDefaultState(this.defaultBlockState().setValue(OCTOPUS, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(OCTOPUS);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        return state == null ? null : state.setValue(OCTOPUS,
                onSoulGrill(context.getLevel().getBlockState(context.getClickedPos().below())));
    }

    /** 底下那格变了（烧烤架架上、拿掉，灵魂营火换成普通营火……）就重新看一遍（1.21.1 原版这个是 protected，写成 public 两个版本都能用） */
    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        BlockState updated = super.updateShape(state, direction, neighborState, level, pos, neighborPos);
        if (direction == Direction.DOWN && updated.is(this)) {
            return updated.setValue(OCTOPUS, onSoulGrill(neighborState));
        }
        return updated;
    }

    private static boolean onSoulGrill(BlockState below) {
        return below.is(ModBlocks.GRILL_CAMPFIRE.get()) && below.getValue(GrillCampfireBlock.SOUL);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MysteryStewPotBlockEntity(pos, state);
    }

    /** 只在客户端 tick：汤面上往上飘效果粒子 */
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (!level.isClientSide || type != ModBlockEntities.MYSTERY_STEW_POT.get()) {
            return null;
        }
        return (tickLevel, tickPos, tickState, be) -> {
            RandomSource random = tickLevel.getRandom();
            if (random.nextFloat() >= EFFECT_CHANCE || !tickState.is(this)) {
                return;
            }
            // 汤那一圈半径 5 像素上下，往里收一点
            double a = random.nextDouble() * Math.PI * 2;
            double r = Math.sqrt(random.nextDouble()) * 0.25;
            float surface = SURFACE_Y[Math.min(tickState.getValue(SERVINGS), SURFACE_Y.length - 1)];
            effectParticle(tickLevel, random, tickPos.getX() + 0.5 + Math.cos(a) * r,
                    tickPos.getY() + surface / 16.0 + 0.05, tickPos.getZ() + 0.5 + Math.sin(a) * r,
                    tickState.getValue(OCTOPUS));
        };
    }

    /**
     * 往上飘一个效果粒子：原版玩家身上有状态效果时冒的那种（entity_effect），颜色随机取一种谜之炖菜的颜色
     * （{@link #COLORS}，章鱼那盆用 {@link #OCTOPUS_COLORS}）。只在客户端调
     */
    public static void effectParticle(Level level, RandomSource random, double x, double y, double z, boolean octopus) {
        int[] colors = octopus ? OCTOPUS_COLORS : COLORS;
        int color = colors[random.nextInt(colors.length)];
        float red = (color >> 16 & 255) / 255.0F;
        float green = (color >> 8 & 255) / 255.0F;
        float blue = (color & 255) / 255.0F;
        //? if <1.20.5 {
        // 1.20.1 原版（LivingEntity#tickEffects）把颜色当速度传，粒子那边再拿去上色
        level.addParticle(ParticleTypes.ENTITY_EFFECT, x, y, z, red, green, blue);
        //?} else {
        /*// 1.20.5 起颜色放在粒子参数里，速度原版传 1、1、1（LivingEntity#tickEffects）；用 float 那个 create，int 那个要带透明度
        level.addParticle(net.minecraft.core.particles.ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, red, green, blue),
                x, y, z, 1.0, 1.0, 1.0);
        *///?}
    }

    /** 盛一碗：原版谜之炖菜，带上这一盆存的效果 */
    @Override
    protected ItemStack servingStack(Level level, BlockPos pos) {
        return MysteryStewEffects.bowl(level.getBlockEntity(pos) instanceof MysteryStewPotBlockEntity pot
                ? pot.effects() : List.of());
    }

    /** 放下时把物品上的效果抄进方块实体 */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.getBlockEntity(pos) instanceof MysteryStewPotBlockEntity pot) {
            pot.setEffects(MysteryStewEffects.read(stack));
        }
    }

    /**
     * 掉落照战利品表（满盆掉一盆谜之炖菜，吃过的掉空大脸盆，同别的盆装菜）；掉出来的那盆谜之炖菜补上这一盆存的效果。
     * 1.21.1 原版这个方法是 protected，这里写成 public 两个版本都能用
     */
    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> drops = super.getDrops(state, params);
        if (params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof MysteryStewPotBlockEntity pot) {
            for (ItemStack stack : drops) {
                if (stack.is(ModItems.MYSTERY_STEW.get())) {
                    MysteryStewEffects.write(stack, pot.effects());
                }
            }
        }
        return drops;
    }
}
