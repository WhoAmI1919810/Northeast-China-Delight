package com.gunmu.northeast_china_delight.compat.displaydelight;

import com.gunmu.northeast_china_delight.util.DdIds;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.Nullable;

/**
 * 展示乐事（Display Delight）可选联动。
 *
 * <p>无器具的菜肴（饺子、烤鸡架、粘豆包等）不能潜行放下，要右键展示乐事的餐盘 / 小餐盘摆上去：
 * 盘子方块换成本模组的菜肴方块（模型里自带那块盘子），空手右键再拿回来、盘子留在原地。
 * 两个版本的展示乐事里盘子的 ID 一样，按 ID 认。</p>
 */
public final class DisplayDelightCompat
{
    public static final String MODID = "displaydelight";

    /** 餐盘（量多的无器具菜） */
    public static final ResourceLocation PLATE = DdIds.of(MODID, "food_plate");
    /** 小餐盘（单个 / 少量的无器具菜） */
    public static final ResourceLocation SMALL_PLATE = DdIds.of(MODID, "small_food_plate");

    private DisplayDelightCompat()
    {
    }

    public static boolean isLoaded()
    {
        //? if <1.20.2 {
        /*return net.minecraftforge.fml.ModList.get().isLoaded(MODID);
        *///?} else {
        return net.neoforged.fml.ModList.get().isLoaded(MODID);
        //?}
    }

    /** 盘子方块；没装展示乐事时返回 null */
    @Nullable
    public static Block plateBlock(ResourceLocation plate)
    {
        Block block = BuiltInRegistries.BLOCK.get(plate);
        return block == Blocks.AIR ? null : block;
    }

    /** 拆掉菜肴方块时还回去的那块盘子；没装展示乐事时为空 */
    public static ItemStack plateItem(ResourceLocation plate)
    {
        Block block = plateBlock(plate);
        return block == null ? ItemStack.EMPTY : new ItemStack(block.asItem());
    }
}
