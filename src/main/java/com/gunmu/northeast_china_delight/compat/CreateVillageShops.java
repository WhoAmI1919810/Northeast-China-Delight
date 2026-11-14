package com.gunmu.northeast_china_delight.compat;

import com.gunmu.northeast_china_delight.NortheastChinaDelight;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
//? if <1.20.2 {
/*import net.minecraft.server.packs.PathPackResources;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.fml.ModList;

import java.nio.file.Path;
*///?} else {
import com.gunmu.northeast_china_delight.util.DdIds;
import net.neoforged.neoforge.event.AddPackFindersEvent;
//?}

/**
 * 机械动力兼容的村庄副食店数据包。安装 Create 时覆盖同路径结构，使用带 Create 方块的副食店；
 * 未安装时不注册监听。两套结构只在屋檐与山墙、窗和桌布上不同，村庄模板池不变。
 * 东北小院始终使用 data 中不含 Create 方块的版本。
 */
public final class CreateVillageShops
{
    /** 模组 jar 里自带数据包的文件夹 */
    private static final String PATH = "resourcepacks/create_village_shops";

    private CreateVillageShops()
    {
    }

    public static void onAddPackFinders(AddPackFindersEvent event)
    {
        if (event.getPackType() != PackType.SERVER_DATA)
        {
            return;
        }
        Component title = Component.translatable("pack.northeast_china_delight.create_village_shops");
        //? if <1.20.2 {
        /*Path path = ModList.get().getModFileById(NortheastChinaDelight.MODID).getFile().findResource(PATH);
        Pack pack = Pack.readMetaAndCreate("builtin/" + NortheastChinaDelight.MODID + "_create_village_shops", title, true,
                name -> new PathPackResources(name, path, true), PackType.SERVER_DATA, Pack.Position.TOP, PackSource.BUILT_IN);
        if (pack != null)
        {
            event.addRepositorySource(consumer -> consumer.accept(pack));
        }
        *///?} else {
        event.addPackFinders(DdIds.of(NortheastChinaDelight.MODID, PATH), PackType.SERVER_DATA, title,
                PackSource.BUILT_IN, true, Pack.Position.TOP);
        //?}
    }
}
