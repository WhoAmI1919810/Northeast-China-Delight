package com.gunmu.northeast_china_delight.item;

import com.gunmu.northeast_china_delight.util.DdEffects;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
//? if <1.20.5 {
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.SuspiciousStewItem;
//?} else {
/*import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.SuspiciousStewEffects;
*///?}

import java.util.ArrayList;
import java.util.List;

/**
 * 谜之炖菜效果的读取、写入，以及盆装方块与碗装物品之间的转换。
 *
 * <ul>
 *   <li>盆装谜之炖菜（物品）：1.21.1 直接挂原版谜之炖菜同一个组件 {@code minecraft:suspicious_stew_effects}；
 *       1.20.1 没有组件，写在物品 NBT 的 {@value #ITEM_TAG} 里（效果注册名 + 刻数，不靠会变的数字 id）；</li>
 *   <li>放下的盆（{@code block/MysteryStewPotBlockEntity}）：两个版本都用 {@link #save} / {@link #load} 这个列表；</li>
 *   <li>碗装：一碗原版 {@code minecraft:suspicious_stew}（贴图、名字都是原版的），效果照原版的存法写进去（{@link #bowl}），
 *       吃的时候原版自己给效果、等级都是 I。</li>
 * </ul>
 */
public final class MysteryStewEffects {

    /** 一种效果：哪个效果、给多少刻（等级都是 I，同原版谜之炖菜） */
    public record Entry(Holder<MobEffect> effect, int duration) {
    }

    /** 1.20.1 盆装谜之炖菜物品 NBT 里存效果的键 */
    public static final String ITEM_TAG = "MysteryStewEffects";

    private MysteryStewEffects() {
    }

    /** 盆装谜之炖菜（物品）上存的效果；没有就是空的 */
    public static List<Entry> read(ItemStack stack) {
        //? if <1.20.5 {
        CompoundTag tag = stack.getTag();
        return tag == null ? List.of() : load(tag.getList(ITEM_TAG, Tag.TAG_COMPOUND));
        //?} else {
        /*SuspiciousStewEffects effects = stack.get(DataComponents.SUSPICIOUS_STEW_EFFECTS);
        if (effects == null) {
            return List.of();
        }
        List<Entry> out = new ArrayList<>();
        for (SuspiciousStewEffects.Entry entry : effects.effects()) {
            out.add(new Entry(entry.effect(), entry.duration()));
        }
        return out;
        *///?}
    }

    /**
     * 一碗原版谜之炖菜上的效果（创造模式提示框用，effect/DishEffectEvents）。1.21.1 和盆装是同一个组件；
     * 1.20.1 照原版的存法读：Effects 列表，每项 EffectId（数字 id，Forge 另存 forge:effect_id）+ EffectDuration（没写是 160 刻）
     */
    public static List<Entry> readBowl(ItemStack stack) {
        //? if <1.20.5 {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains("Effects", Tag.TAG_LIST)) {
            return List.of();
        }
        ListTag list = tag.getList("Effects", Tag.TAG_COMPOUND);
        List<Entry> out = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            int duration = entry.contains("EffectDuration", Tag.TAG_ANY_NUMERIC) ? entry.getInt("EffectDuration") : 160;
            ResourceLocation id = entry.contains("forge:effect_id", Tag.TAG_STRING)
                    ? ResourceLocation.tryParse(entry.getString("forge:effect_id")) : null;
            MobEffect effect = id != null ? BuiltInRegistries.MOB_EFFECT.get(id) : null;
            if (effect == null) {
                effect = MobEffect.byId(entry.getInt("EffectId"));
            }
            if (effect != null) {
                out.add(new Entry(DdEffects.hold(effect), duration));
            }
        }
        return out;
        //?} else {
        /*return read(stack);
        *///?}
    }

    /** 把效果写到盆装谜之炖菜（物品）上，原来有的换掉 */
    public static void write(ItemStack stack, List<Entry> effects) {
        //? if <1.20.5 {
        stack.getOrCreateTag().put(ITEM_TAG, save(effects));
        //?} else {
        /*stack.set(DataComponents.SUSPICIOUS_STEW_EFFECTS, component(effects));
        *///?}
    }

    /**
     * 盛一碗：原版谜之炖菜，带上这些效果。1.20.1 不能循环调用 {@code SuspiciousStewItem.saveMobEffect}：
     * 它用 {@code getList("Effects", 9)} 读已有的 compound 列表会得到空表，第二次起会把前面的效果覆盖掉。
     * 这里一次构造完整的 Effects 列表再写回，字段与原版一致。
     */
    public static ItemStack bowl(List<Entry> effects) {
        ItemStack stew = new ItemStack(Items.SUSPICIOUS_STEW);
        //? if <1.20.5 {
        ListTag list = new ListTag();
        for (Entry entry : effects) {
            CompoundTag effect = new CompoundTag();
            effect.putInt(SuspiciousStewItem.EFFECT_ID_TAG, MobEffect.getId(entry.effect().value()));
            net.minecraftforge.common.ForgeHooks.saveMobEffect(effect, "forge:effect_id", entry.effect().value());
            effect.putInt(SuspiciousStewItem.EFFECT_DURATION_TAG, entry.duration());
            list.add(effect);
        }
        if (!list.isEmpty()) {
            stew.getOrCreateTag().put(SuspiciousStewItem.EFFECTS_TAG, list);
        }
        //?} else {
        /*stew.set(DataComponents.SUSPICIOUS_STEW_EFFECTS, component(effects));
        *///?}
        return stew;
    }

    //? if >=1.20.5 {
    /*private static SuspiciousStewEffects component(List<Entry> effects) {
        List<SuspiciousStewEffects.Entry> list = new ArrayList<>();
        for (Entry entry : effects) {
            list.add(new SuspiciousStewEffects.Entry(entry.effect(), entry.duration()));
        }
        return new SuspiciousStewEffects(list);
    }
    *///?}

    /** 存档用的列表：每项 Id（效果注册名）+ Duration（刻） */
    public static ListTag save(List<Entry> effects) {
        ListTag list = new ListTag();
        for (Entry entry : effects) {
            entry.effect().unwrapKey().ifPresent(key -> {
                CompoundTag tag = new CompoundTag();
                tag.putString("Id", key.location().toString());
                tag.putInt("Duration", entry.duration());
                list.add(tag);
            });
        }
        return list;
    }

    /** 读 {@link #save} 存的列表；认不出的效果（比如卸掉了提供它的模组）跳过 */
    public static List<Entry> load(ListTag list) {
        List<Entry> out = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            ResourceLocation id = ResourceLocation.tryParse(tag.getString("Id"));
            int duration = tag.getInt("Duration");
            if (id != null && duration > 0) {
                DdEffects.holder(id).ifPresent(effect -> out.add(new Entry(effect, duration)));
            }
        }
        return out;
    }
}
