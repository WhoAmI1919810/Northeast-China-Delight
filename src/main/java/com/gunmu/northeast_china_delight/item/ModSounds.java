package com.gunmu.northeast_china_delight.item;

import com.gunmu.northeast_china_delight.NortheastChinaDelight;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
//? if <1.20.2 {
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
//?} else {
/*import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
*///?}
//? if <1.20.2 {
import net.minecraftforge.eventbus.api.IEventBus;
//?} else {
/*import net.neoforged.bus.api.IEventBus;
*///?}
import net.minecraft.resources.ResourceLocation;

public class ModSounds {
    //? if <1.20.2 {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(net.minecraftforge.registries.ForgeRegistries.SOUND_EVENTS, NortheastChinaDelight.MODID);
    public static final RegistryObject<SoundEvent> MUSIC_DISC_NORTHEAST = SOUNDS.register("music_disc.northeast",
            () -> SoundEvent.createVariableRangeEvent(com.gunmu.northeast_china_delight.util.DdIds.of(
                    NortheastChinaDelight.MODID, "music_disc.northeast")));
    //?} else {
    /*public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Registries.SOUND_EVENT, NortheastChinaDelight.MODID);
    public static final DeferredHolder<SoundEvent, SoundEvent> MUSIC_DISC_NORTHEAST = SOUNDS.register("music_disc.northeast",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(NortheastChinaDelight.MODID, "music_disc.northeast")));
    *///?}

    public static void register(IEventBus eventBus) {
        SOUNDS.register(eventBus);
    }
}