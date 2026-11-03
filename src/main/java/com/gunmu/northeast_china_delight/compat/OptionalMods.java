package com.gunmu.northeast_china_delight.compat;

/**
 * 可选前置装没装。2026-11-04 起机械动力（Create）是可选前置：用到 Create 类的代码（鼓风机批量风干、JEI 风干页）
 * 只在装了它时才去碰，没装就一行都不执行，免得类加载不到崩游戏。
 * Create 那套加工配方在数据里带 neoforge:mod_loaded 条件，不装就不加载。
 */
public final class OptionalMods
{
    private OptionalMods()
    {
    }

    public static boolean create()
    {
        return isLoaded("create");
    }

    public static boolean isLoaded(String modId)
    {
        //? if <1.20.2 {
        /*return net.minecraftforge.fml.ModList.get().isLoaded(modId);
        *///?} else {
        return net.neoforged.fml.ModList.get().isLoaded(modId);
        //?}
    }
}
