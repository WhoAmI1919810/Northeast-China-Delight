package com.gunmu.northeast_china_delight.mixin;

import com.gunmu.northeast_china_delight.villager.YardMapIconMarker;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
//? if <1.20.5 {
/*import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
*///?}
import org.spongepowered.asm.mixin.Mixin;

/**
 * 1.20.1 地图数据钩子：
 * 1. 为东北小院地图的目标标记挂上内部识别组件，经原版地图同步包发往客户端；
 * 2. 自动升级旧存档中以 {@code "+"} 登记的已有小院地图；
 * 3. 在客户端原版地图图标绘制期间临时屏蔽该标记，交由 {@link MapRendererMixin} 绘制小屋图标。
 */
@Mixin(MapItemSavedData.class)
public abstract class MapItemSavedDataMixin
{
    //? if <1.20.5 {
    /*@Shadow
    @Final
    Map<String, MapDecoration> decorations;

    @Shadow
    protected abstract void removeDecoration(String identifier);

    @Shadow
    protected abstract void setDecorationsDirty();

    @Inject(method = "tickCarriedBy", at = @At("HEAD"))
    private void ncd$upgradeLegacyYardMap(Player player, ItemStack stack, CallbackInfo ci)
    {
        if (!stack.hasTag() || !stack.getTag().contains("Decorations", Tag.TAG_LIST))
        {
            return;
        }
        if (!stack.hasCustomHoverName()
                || !(stack.getHoverName().getContents() instanceof TranslatableContents contents)
                || !"filled_map.northeast_yard".equals(contents.getKey()))
        {
            return;
        }
        ListTag list = stack.getTag().getList("Decorations", Tag.TAG_COMPOUND);
        boolean upgraded = false;
        for (int i = 0; i < list.size(); i++)
        {
            CompoundTag entry = list.getCompound(i);
            if ("+".equals(entry.getString("id"))
                    && entry.getByte("type") == MapDecoration.Type.TARGET_X.getIcon())
            {
                entry.putString("id", YardMapIconMarker.DECORATION_ID);
                upgraded = true;
            }
        }
        if (upgraded && this.decorations.containsKey("+"))
        {
            this.removeDecoration("+");
        }
    }

    @Inject(method = "addDecoration", at = @At("TAIL"))
    private void ncd$tagYardDecoration(MapDecoration.Type type, @Nullable LevelAccessor level,
                                       String decorationName, double x, double z, double rotation,
                                       @Nullable Component name, CallbackInfo ci)
    {
        if (name == null && type == MapDecoration.Type.TARGET_X
                && YardMapIconMarker.DECORATION_ID.equals(decorationName))
        {
            MapDecoration existing = this.decorations.get(decorationName);
            if (existing != null && !YardMapIconMarker.isYardDecoration(existing))
            {
                this.decorations.put(decorationName, new MapDecoration(
                        existing.getType(), existing.getX(), existing.getY(),
                        existing.getRot(), YardMapIconMarker.MARKER_COMPONENT));
                this.setDecorationsDirty();
            }
        }
    }

    @Inject(method = "getDecorations", at = @At("RETURN"), cancellable = true)
    private void ncd$filterYardDecorationsForVanillaDraw(CallbackInfoReturnable<Iterable<MapDecoration>> cir)
    {
        if (!YardMapIconMarker.isSuppressingVanillaDraw())
        {
            return;
        }
        Iterable<MapDecoration> original = cir.getReturnValue();
        if (original == null)
        {
            return;
        }
        List<MapDecoration> filtered = null;
        int idx = 0;
        for (MapDecoration dec : original)
        {
            if (YardMapIconMarker.isYardDecoration(dec))
            {
                if (filtered == null)
                {
                    filtered = new ArrayList<>();
                    int i = 0;
                    for (MapDecoration prev : original)
                    {
                        if (i++ >= idx)
                        {
                            break;
                        }
                        filtered.add(prev);
                    }
                }
            }
            else if (filtered != null)
            {
                filtered.add(dec);
            }
            idx++;
        }
        if (filtered != null)
        {
            cir.setReturnValue(filtered);
        }
    }
    *///?}
}
