package com.gunmu.northeast_china_delight.compat.jade;

import com.gunmu.northeast_china_delight.block.Vat;
import com.gunmu.northeast_china_delight.NortheastChinaDelight;
import com.gunmu.northeast_china_delight.util.DdIds;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

/**
 * 玉（Jade）适配入口。
 *
 * Jade 会扫描带 {@link WailaPlugin} 注解的类并自动加载，
 * 所以这个类只会在装了 Jade 的客户端被加载，不需要额外的入口声明。
 */
@WailaPlugin
public class NortheastJadePlugin implements IWailaPlugin {

    public static final ResourceLocation VAT_UID =
            DdIds.of(NortheastChinaDelight.MODID, "vat");

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(VatComponentProvider.INSTANCE, Vat.class);
    }
}
