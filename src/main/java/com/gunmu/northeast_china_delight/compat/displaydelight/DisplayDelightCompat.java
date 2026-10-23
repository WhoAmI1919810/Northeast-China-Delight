package com.gunmu.northeast_china_delight.compat.displaydelight;

/**
 * 展示乐事（Display Delight）可选联动。
 *
 * <p>无器具的菜肴（饺子、烤鸡架、粘豆包等）摆成方块时放在展示乐事风格的餐盘 / 小餐盘上，
 * 所以只有装了展示乐事才允许潜行右键放置；没装时右键照常是吃。</p>
 */
public final class DisplayDelightCompat
{
    public static final String MODID = "displaydelight";

    private DisplayDelightCompat()
    {
    }

    public static boolean isLoaded()
    {
        //? if <1.20.2 {
        return net.minecraftforge.fml.ModList.get().isLoaded(MODID);
        //?} else {
        /*return net.neoforged.fml.ModList.get().isLoaded(MODID);
        *///?}
    }
}
