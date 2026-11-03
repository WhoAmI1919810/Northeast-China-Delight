package com.gunmu.northeast_china_delight.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 大铁锅做菜的埋点（2026-11-03 只留了入口，还没有功能）。
 *
 * <p>用户的设想：不做界面；水、调料、食材直接右键放进锅里，盖上锅盖、点着火煮，煮熟以后拿大脸盆盛出来。
 * 锅里的东西存在 {@link BigPotBlockEntity}（大灶 nw 那一格上）。大灶 {@link BigStoveBlock} 在下面这些时候调这里，
 * 以后加功能只改这个类和方块实体，大灶本身不用动。</p>
 */
public final class BigPotCooking {

    private BigPotCooking() {
    }

    /**
     * 右键大灶、灶上放着锅（没盖盖），手里拿的东西不是锅 / 锅盖 / 打火石 / 火焰弹 / 锹时。
     * 以后在这里：水桶 / 水盆倒水，调料瓶倒调料，食材放进锅里，拿空的大脸盆盛菜（{@code pot.result()}）。
     * 返回 PASS 就照常交给物品自己的用法。
     */
    public static InteractionResult onUse(BigPotBlockEntity pot, ItemStack held, BlockState state, Level level,
                                          BlockPos pos, Player player, InteractionHand hand) {
        return InteractionResult.PASS;
    }

    /**
     * 灶点着、灶上有锅时每个服务端 tick 调一次（只在 nw 那一格的方块实体上跑，见 {@link BigStoveBlock#getTicker}）。
     * 以后在这里按配方推进 {@code cookTicks}、煮好了写 {@code result} / {@code servings}；state 里 pot=lid 表示盖着盖。
     */
    public static void serverTick(Level level, BlockPos pos, BlockState state, BigPotBlockEntity pot) {
    }

    /** 空手把锅从灶上拿下来之前问一声：锅里还有东西就不让拿（以后可以改成连东西一起装进物品里） */
    public static boolean canTakePot(BigPotBlockEntity pot) {
        return pot.isEmpty();
    }

    /** 大灶被拆时：锅里的料、没盛完的菜掉出来 */
    public static void onBroken(Level level, BlockPos pos, BigPotBlockEntity pot) {
        pot.dropContents(level, pos);
    }
}
