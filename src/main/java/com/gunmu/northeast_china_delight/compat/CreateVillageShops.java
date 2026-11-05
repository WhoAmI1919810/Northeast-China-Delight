package com.gunmu.northeast_china_delight.compat;

import com.gunmu.northeast_china_delight.NortheastChinaDelight;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
//? if <1.20.2 {
import net.minecraft.server.packs.PathPackResources;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.fml.ModList;

import java.nio.file.Path;
//?} else {
/*import com.gunmu.northeast_china_delight.util.DdIds;
import net.neoforged.neoforge.event.AddPackFindersEvent;
*///?}

/**
 * 村庄副食店五座都有两套：没装机械动力时用模组 data 里的那一套（全是原版 / 本模组方块），装了机械动力时换成带机械动力方块的那一套
 * （2026-11-04 用户：加载了机械动力就生成带机械动力方块的副食店）。两套只差三样：
 * <ul>
 *   <li>屋檐、山墙：机械动力那套照用户做的平原、热带草原原版（2026-11-05 用户：屋檐和山墙就按照这一版来）——屋顶铺到房身两头，
 *       四周一圈仿制台阶（平原 40 格：前后檐口两排、两头山墙斜边、屋脊两头；热带草原是檐下两排仿制面板）；
 *       data 那套这圈不要（用户嫌顶替它的半砖不协调），屋顶改成比房子左右各宽一格。</li>
 *   <li>窗：机械动力那套下排原版玻璃板、上排带框玻璃板；data 那套是本模组木格窗（雪原两套都是糊纸窗贴窗花）。</li>
 *   <li>桌布：机械动力桌布 / 本模组格子桌布，同色。</li>
 * </ul>
 * 沙漠、雪原、针叶林三座 2026-11-05 重做过（原来是照平原换材料的，用户嫌重复）；它们的机械动力版仿制台阶位置、朝向逐格照用户平原那座，
 * 材料分别是泥砖、雪块、白桦木板（work/tools/shop_redesign/create_variant.py）。
 *
 * <p>机械动力那一套放在模组 jar 里的一个自带数据包（{@link #PATH}）：同样的路径 northeast_china_delight:village/relish_*，
 * 只在装了机械动力时由这里挂上，摆在最上面、始终启用，于是同名的结构文件盖住 data 里的那一套；村庄的模板池、结构都不用改。
 * 包里 structure/（1.21.1）和 structures/（1.20.1）各放一份：打 1.20.1 包时那套 data 转换只管主 data 目录、不碰这个包。</p>
 *
 * <p>东北小院只有一套：data 里用户做的无机械动力版（work/tools/yard_no_create 补完最后几块），装没装机械动力都生成它
 * （2026-11-05 用户：只生成新版——带机械动力方块的是改之前的旧版，缺了后来加的大灶、压水井、炕围画等，没放进这个包）。</p>
 *
 * <p>没装机械动力时根本不注册这个监听（见 NortheastChinaDelight），这个类也不引用机械动力的任何类。</p>
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
        Path path = ModList.get().getModFileById(NortheastChinaDelight.MODID).getFile().findResource(PATH);
        Pack pack = Pack.readMetaAndCreate("builtin/" + NortheastChinaDelight.MODID + "_create_village_shops", title, true,
                name -> new PathPackResources(name, path, true), PackType.SERVER_DATA, Pack.Position.TOP, PackSource.BUILT_IN);
        if (pack != null)
        {
            event.addRepositorySource(consumer -> consumer.accept(pack));
        }
        //?} else {
        /*event.addPackFinders(DdIds.of(NortheastChinaDelight.MODID, PATH), PackType.SERVER_DATA, title,
                PackSource.BUILT_IN, true, Pack.Position.TOP);
        *///?}
    }
}
