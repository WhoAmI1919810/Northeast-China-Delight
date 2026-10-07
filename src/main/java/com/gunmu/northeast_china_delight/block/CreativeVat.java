package com.gunmu.northeast_china_delight.block;

import com.gunmu.northeast_china_delight.crafting.VatBrewing;
import com.gunmu.northeast_china_delight.crafting.VatRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 创造大缸：和 {@link Vat} 外观、右键交互一模一样，
 * 区别只在「条件凑齐的瞬间就完成」 —— 投料 + 封口都到位时不排队，直接酿好。
 *
 * <p>没有对应配方：创造大缸只能靠创造模式 / 指令拿到。</p>
 */
public class CreativeVat extends Vat {

    public CreativeVat(Properties properties) {
        super(properties);
    }

    /** 这一缸条件齐了就当场完成，不走计时 */
    @Override
    protected void tryStart(Level level, BlockPos pos, VatBlockEntity vat) {
        if (level instanceof ServerLevel serverLevel) {
            VatRecipe recipe = VatBrewing.ready(vat);
            if (recipe != null) {
                if (vat.kind() != recipe.kind()) {
                    vat.setKind(recipe.kind());
                }
                VatBrewing.complete(serverLevel, pos, vat, recipe);
            }
        }
    }
}
