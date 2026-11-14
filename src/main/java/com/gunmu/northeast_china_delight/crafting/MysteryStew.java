package com.gunmu.northeast_china_delight.crafting;

import com.gunmu.northeast_china_delight.ModTags;
import com.gunmu.northeast_china_delight.NortheastChinaDelight;
import com.gunmu.northeast_china_delight.block.BigPotBlockEntity;
import com.gunmu.northeast_china_delight.item.ModItems;
import com.gunmu.northeast_china_delight.item.MysteryStewEffects;
import com.gunmu.northeast_china_delight.util.DdEffects;
import com.gunmu.northeast_china_delight.util.DdIds;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 谜之炖菜：大锅里凑不上任何一条大锅烹饪配方时，盖上锅盖照样煮，煮出来一锅谜之炖菜，用大脸盆盛。
 *
 * <ul>
 *   <li>能下锅的：配方用得上的东西，加上这里认的所有食材（{@link Kind} 那些标签里的，原版胡萝卜、农夫乐事番茄洋葱都算）
 *       和调料（{@link #SEASONINGS}）；</li>
 *   <li>每样食材按它的「性子」归一类（{@link Kind}，一类对一种效果，标签 {@code mystery_stew/kinds/<类>}）；
 *       锅里数量最多的三类给效果；个数一样多时，珍稀度加起来高的先
 *       （{@link #rarity}：费工 2、难得 3、稀有 5，其余家常 1），再一样就先放进锅的先；</li>
 *   <li>等级无论放多少都是 I（同原版谜之炖菜）；每种效果先按 {@link Kind#initialTicks()} 取初始时长，
 *       同类每件食材再加 {@link #BASE_TICKS_PER_ITEM} 刻、每多一种不同物品再加 {@link #BASE_TICKS_PER_VARIETY} 刻，
 *       最后按投入的调料种数乘倍率。饱和例外，固定 {@link #SATURATION_TICKS} 刻（同原版蒲公英炖菜）：
 *       饱和每刻都补饥饿值，给 4 秒就是一碗吃满；</li>
 *   <li>一锅出一盆（盆里能盛 4 碗），不给经验；效果存在盆装谜之炖菜上（{@link MysteryStewEffects}），
 *       放下的盆、拿碗盛出来的原版谜之炖菜都带着。</li>
 * </ul>
 */
public final class MysteryStew {

    /** 锅里正在煮谜之炖菜时 BigPotBlockEntity#cooking 记的 id（不是哪条配方） */
    public static final ResourceLocation ID = DdIds.of(NortheastChinaDelight.MODID, "mystery_stew");
    /** 煮多久：同本模组的大锅烹饪配方（揭着盖 30 秒，盖着锅盖缩短 40%） */
    public static final int COOKING_TIME = 600;
    public static final int LID_COOKING_TIME = COOKING_TIME * (100 - 40) / 100;
    /** 同类每件食材给几刻（0.3 秒） */
    public static final int BASE_TICKS_PER_ITEM = 6;
    /** 同类每多一种不同物品再给几刻（0.3 秒） */
    public static final int BASE_TICKS_PER_VARIETY = 6;
    /** 饱和固定几刻（同原版蒲公英炖菜） */
    public static final int SATURATION_TICKS = 7;
    /** 最多给几种效果：数量最多的三类 */
    public static final int KINDS_GIVEN = 3;

    /**
     * 能当调料下锅的：和锅里液面染色认的那几种一样（client/BigPotVisuals#SEASONINGS）。调料倒进去拿不出来、水舀干就跟着没
     */
    public static final List<TagKey<Item>> SEASONINGS = List.of(
            ModTags.FOODS_SALT, ModTags.FOODS_SOY_SAUCE, ModTags.FOODS_COOKING_OIL, ModTags.FOODS_ANIMAL_OIL,
            ModTags.FOODS_CHILI_OIL, ModTags.FOODS_CHILI_SAUCE, ModTags.FOODS_VINEGAR, ModTags.FOODS_WHITE_VINEGAR,
            ModTags.FOODS_FISH_SAUCE, ModTags.FOODS_SHRIMP_PASTE, ModTags.FOODS_PEANUT_BUTTER, ModTags.FOODS_SOY_PASTE);

    /**
     * 食材的「性子」和对应的效果。一样东西同时在好几类的标签里时按这里的先后认第一类：
     * 先认特别的（毒物、人参、海参、金苹果……），再认笼统的（肉、鱼虾、青菜、主食）——比如河豚在通用的生鱼标签里，但算毒物
     */
    public enum Kind {
        POISON("poison", "minecraft:poison", 60),
        ROTTEN("rotten", "minecraft:hunger", 60),
        GINSENG("ginseng", "northeast_china_delight:nourishing", 80),
        SEA_CUCUMBER("sea_cucumber", "minecraft:dolphins_grace", 60),
        GOLDEN("golden", "minecraft:absorption", 80),
        CHORUS("chorus", "minecraft:slow_falling", 80),
        OFFAL("offal", "minecraft:health_boost", 60),
        BONE("bone", "minecraft:resistance", 60),
        FAT("fat", "northeast_china_delight:greasy", 90),
        SPICY("spicy", "minecraft:fire_resistance", 80),
        PICKLE("pickle", "minecraft:haste", 110),
        ROOT("root", "minecraft:night_vision", 90),
        ALLIUM("allium", "northeast_china_delight:warmth", 90),
        BEAN("bean", "farmersdelight:nourishment", 90),
        NUT("nut", "minecraft:luck", 80),
        FUNGUS("fungus", "minecraft:regeneration", 110),
        EGG("egg", "minecraft:speed", 110),
        SEAFOOD("seafood", "minecraft:water_breathing", 80),
        MEAT("meat", "minecraft:strength", 110),
        FRUIT("fruit", "minecraft:jump_boost", 60),
        GREENS("greens", "northeast_china_delight:refreshing", 90),
        STAPLE("staple", "minecraft:saturation", 0);

        private final TagKey<Item> tag;
        private final ResourceLocation effect;
        private final int initialTicks;

        Kind(String name, String effect, int initialTicks) {
            this.tag = modTag("mystery_stew/kinds/" + name);
            this.effect = ResourceLocation.tryParse(effect);
            this.initialTicks = initialTicks;
        }

        public TagKey<Item> tag() {
            return tag;
        }

        public ResourceLocation effect() {
            return effect;
        }

        /** 这类效果的基础初始时长；饱和不用这个值，固定走 {@link #SATURATION_TICKS} */
        public int initialTicks() {
            return initialTicks;
        }
    }

    /** 珍稀度：只列费工、难得、稀有三档，别的都算家常 */
    private static final TagKey<Item> LABORIOUS = modTag("mystery_stew/rarity/laborious");
    private static final TagKey<Item> SCARCE = modTag("mystery_stew/rarity/scarce");
    private static final TagKey<Item> RARE = modTag("mystery_stew/rarity/rare");

    private MysteryStew() {
    }

    private static TagKey<Item> modTag(String path) {
        return TagKey.create(Registries.ITEM, DdIds.of(NortheastChinaDelight.MODID, path));
    }

    /** 这样东西能不能为谜之炖菜下锅（配方用得上的另外由 BigPotRecipes#usable 认） */
    public static boolean accepts(ItemStack stack) {
        return !stack.isEmpty() && (isSeasoning(stack) || kindOf(stack) != null);
    }

    public static boolean isSeasoning(ItemStack stack) {
        for (TagKey<Item> tag : SEASONINGS) {
            if (stack.is(tag)) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    public static Kind kindOf(ItemStack stack) {
        for (Kind kind : Kind.values()) {
            if (stack.is(kind.tag())) {
                return kind;
            }
        }
        return null;
    }

    /** 一个的珍稀度：稀有 5、难得 3、费工 2、家常 1 */
    public static int rarity(ItemStack stack) {
        if (stack.is(RARE)) {
            return 5;
        }
        if (stack.is(SCARCE)) {
            return 3;
        }
        return stack.is(LABORIOUS) ? 2 : 1;
    }

    /** 凑不上配方时能不能煮成谜之炖菜：锅里有水、至少有一个食材（光有调料不煮） */
    public static boolean canCook(BigPotBlockEntity pot) {
        return pot.liquidMb() > 0 && pot.ingredientCount() > 0;
    }

    /** 煮好的一盆：盆装谜之炖菜，带上按锅里的东西算出来的效果 */
    public static ItemStack cook(BigPotBlockEntity pot) {
        ItemStack basin = new ItemStack(ModItems.MYSTERY_STEW.get());
        MysteryStewEffects.write(basin, effects(pot.ingredientStacks(), pot.seasoningKinds()));
        return basin;
    }

    /**
     * 按锅里的食材（先放的在前、同样的并成一堆）和调料种数算效果：数量最多的三类，各给一种 I 级效果（不够三类就少给）
     */
    public static List<MysteryStewEffects.Entry> effects(List<ItemStack> ingredients, int seasoningKinds) {
        // 每一类：[个数, 珍稀度合计, 最早放进去的是第几样]
        Map<Kind, int[]> tally = new EnumMap<>(Kind.class);
        Map<Kind, Set<Item>> varieties = new EnumMap<>(Kind.class);
        for (int i = 0; i < ingredients.size(); i++) {
            ItemStack stack = ingredients.get(i);
            Kind kind = kindOf(stack);
            if (kind == null) {
                continue;
            }
            int[] t = tally.computeIfAbsent(kind, k -> new int[]{0, 0, Integer.MAX_VALUE});
            t[0] += stack.getCount();
            t[1] += rarity(stack) * stack.getCount();
            t[2] = Math.min(t[2], i);
            varieties.computeIfAbsent(kind, ignored -> new HashSet<>()).add(stack.getItem());
        }
        List<Map.Entry<Kind, int[]>> ranked = new ArrayList<>(tally.entrySet());
        ranked.sort(Comparator.<Map.Entry<Kind, int[]>>comparingInt(e -> -e.getValue()[0])
                .thenComparingInt(e -> -e.getValue()[1])
                .thenComparingInt(e -> e.getValue()[2]));
        List<MysteryStewEffects.Entry> out = new ArrayList<>();
        for (Map.Entry<Kind, int[]> entry : ranked) {
            if (out.size() >= KINDS_GIVEN) {
                break;
            }
            Kind kind = entry.getKey();
            int baseTicks = BASE_TICKS_PER_ITEM * entry.getValue()[0]
                    + BASE_TICKS_PER_VARIETY * varieties.get(kind).size();
            int ticks = kind == Kind.STAPLE ? SATURATION_TICKS
                    : scaledTicks(kind.initialTicks() + baseTicks, seasoningKinds);
            // 效果找不到（提供它的模组没装）就跳过这一类，轮到下一类
            DdEffects.holder(kind.effect()).ifPresent(effect -> out.add(new MysteryStewEffects.Entry(effect, ticks)));
        }
        return out;
    }

    /** 按调料种数乘倍率；结果不是整刻时向下取整 */
    private static int scaledTicks(int ticks, int seasoningKinds) {
        int percent;
        if (seasoningKinds <= 0) {
            percent = 100;
        } else if (seasoningKinds == 1) {
            percent = 140;
        } else if (seasoningKinds == 2) {
            percent = 175;
        } else {
            percent = 230;
        }
        return ticks * percent / 100;
    }
}
