package com.gunmu.northeast_china_delight.block;

import com.gunmu.northeast_china_delight.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
//? if >=1.20.5 {
/*import net.minecraft.world.ItemInteractionResult;
*///?}
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 糊纸木格窗：在 {@link LatticeWindowBlock} 上多一个窗花（{@link #PAPER_CUT}）。
 *
 * <p>拿窗花（{@link com.gunmu.northeast_china_delight.item.PaperCutItem}）右键窗户贴上去：点窗户中间贴团花，
 * 点四个角贴那个角的角花；空手右键取下来（进背包），剪刀右键也能揭（掉在窗前）。拆窗户时贴着的窗花一起掉（掉落表里有）。</p>
 */
public class PaperLatticeWindowBlock extends LatticeWindowBlock {

    public enum PaperCut implements StringRepresentable {
        NONE("none"), ROUND("round"), CORNER_TL("corner_tl"), CORNER_TR("corner_tr"),
        CORNER_BL("corner_bl"), CORNER_BR("corner_br");

        private final String name;

        PaperCut(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    public static final EnumProperty<PaperCut> PAPER_CUT = EnumProperty.create("paper_cut", PaperCut.class);

    public PaperLatticeWindowBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(PAPER_CUT, PaperCut.NONE));
    }

    /**
     * 点在窗上的哪儿就贴哪种：先把点击位置换算到模型坐标（facing=north 时的 x，0 是西、1 是东；
     * 资源里 corner_*l 的角花在模型西边），左右各 0.3 以内、上下各 0.3 以内算角，其余贴团花。
     */
    public static PaperCut cutFor(BlockState state, BlockPos pos, Vec3 hit) {
        double x = hit.x - pos.getX();
        double y = hit.y - pos.getY();
        double z = hit.z - pos.getZ();
        double mx = switch (state.getValue(FACING)) {
            case SOUTH -> 1 - x;
            case EAST -> z;
            case WEST -> 1 - z;
            default -> x;
        };
        boolean left = mx < 0.3, right = mx > 0.7, top = y > 0.7, bottom = y < 0.3;
        if (top && left) {
            return PaperCut.CORNER_TL;
        }
        if (top && right) {
            return PaperCut.CORNER_TR;
        }
        if (bottom && left) {
            return PaperCut.CORNER_BL;
        }
        if (bottom && right) {
            return PaperCut.CORNER_BR;
        }
        return PaperCut.ROUND;
    }

    //? if <1.20.5 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hitResult) {
        ItemStack held = player.getItemInHand(hand);
        if (held.isEmpty()) {
            // 和 1.21.1 一样只认主手空着：主手拿着东西没用上时还会再拿副手问一遍，那时不该把窗花取下来
            return hand == InteractionHand.MAIN_HAND ? takeOff(state, level, pos, player) : InteractionResult.PASS;
        }
        return peelOff(held, state, level, pos);
    }
    //?} else {
    /*@Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        InteractionResult result = peelOff(stack, state, level, pos);
        return result == InteractionResult.PASS
                ? ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
                : ItemInteractionResult.sidedSuccess(level.isClientSide());
    }

    /^* 空手右键：把窗花取下来（只认主手空着，手里拿着东西时让给物品自己的用法） ^/
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hitResult) {
        return player.getMainHandItem().isEmpty() ? takeOff(state, level, pos, player) : InteractionResult.PASS;
    }
    *///?}

    /** 空手取窗花：直接放进背包（背包满了掉在脚下） */
    private static InteractionResult takeOff(BlockState state, Level level, BlockPos pos, Player player) {
        if (state.getValue(PAPER_CUT) == PaperCut.NONE) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            level.setBlock(pos, state.setValue(PAPER_CUT, PaperCut.NONE), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.PAINTING_BREAK, SoundSource.BLOCKS, 0.8F, 1.2F);
            ItemStack cut = new ItemStack(ModItems.PAPER_CUT.get());
            if (!player.getInventory().add(cut)) {
                player.drop(cut, false);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** 剪刀揭窗花：窗花还给玩家（掉在窗前） */
    private static InteractionResult peelOff(ItemStack held, BlockState state, Level level, BlockPos pos) {
        if (!held.is(Items.SHEARS) || state.getValue(PAPER_CUT) == PaperCut.NONE) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            level.setBlock(pos, state.setValue(PAPER_CUT, PaperCut.NONE), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.SHEEP_SHEAR, SoundSource.BLOCKS, 0.8F, 1.2F);
            popResource(level, pos, new ItemStack(ModItems.PAPER_CUT.get()));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(PAPER_CUT);
    }
}
