package com.gunmu.northeast_china_delight.item;

//? if <1.20.2 {
/*import net.minecraftforge.eventbus.api.IEventBus;
*///?} else {
import net.neoforged.bus.api.IEventBus;
//?}
//? if >=1.20.5 {
import com.gunmu.northeast_china_delight.NortheastChinaDelight;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.function.Supplier;
//?}

/**
 * 旧存档兼容：3.2.2 起制盐改用水盆，海水桶只作为过渡物品留着（{@link ModItems#SEA_WATER_BUCKET}，没有获取方式）。
 *
 * <p>以前的「海水标记」水桶：1.21.1 身上带 {@code northeast_china_delight:sea_water} 数据组件，组件类型不注册的话
 * 整组物品读档失败、直接消失，所以类型照旧注册（已经没有任何作用，就是一只普通水桶）。
 * 1.20.1 的标记是普通 NBT 标签，读档不受影响，这里什么也不用做。</p>
 */
public final class LegacySeaWater {

    //? if >=1.20.5 {
    private static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, NortheastChinaDelight.MODID);
    /** 旧版的海水标记，只为老存档里的水桶能读出来 */
    public static final Supplier<DataComponentType<Boolean>> SEA_WATER =
            COMPONENTS.register("sea_water", () -> DataComponentType.<Boolean>builder()
                    .persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL).build());
    //?}

    private LegacySeaWater() {
    }

    public static void register(IEventBus modEventBus) {
        //? if >=1.20.5 {
        COMPONENTS.register(modEventBus);
        //?}
    }
}
