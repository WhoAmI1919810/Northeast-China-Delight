package com.gunmu.northeast_china_delight.compat.ali;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * 只读地取用本模组全局掉落修改器（GLM）里的掉落数据，供 ALI 兼容插件展示。
 *
 * <p>掉落表在本模组里是各 GLM 的 {@code private static final Entry[]} 字段，物品清单写在
 * Java 里（不在数据包里），所以 ALI 静态看不到。这里用反射按「形状」而不是按字段名读取，
 * GLM 里新增一张表或改名都不需要同步改这里；读不到时只少了内容，不会影响原有逻辑。</p>
 *
 * <p>只读：不修改、不调用任何 {@code doApply}，不产生副作用。</p>
 */
final class AliGlmReflection {
    private static final Logger LOGGER = LoggerFactory.getLogger("northeast_china_delight/ali");
    private static final LootItemCondition[] NO_CONDITIONS = new LootItemCondition[0];

    /** 掉落表里的一条：物品 + 权重 */
    record Entry(Item item, int weight) {}

    /** 一张掉落表（对应 GLM 里一个 Entry[] 字段） */
    record Table(String name, List<Entry> entries) {
        int total() {
            int sum = 0;

            for (Entry entry : entries) {
                sum += entry.weight;
            }

            return sum;
        }
    }

    /**
     * 读取 GLM 的 conditions（NeoForge/Forge 的 {@code LootModifier} 里是 protected 字段，
     * ALI 自己也靠 mixin accessor 读它）。读不到返回空数组，只是提示条少一段说明。
     */
    static List<LootItemCondition> conditionsOf(Object modifier) {
        for (Class<?> type = modifier.getClass(); type != null; type = type.getSuperclass()) {
            try {
                Field field = type.getDeclaredField("conditions");
                field.setAccessible(true);
                Object value = field.get(modifier);

                if (value instanceof LootItemCondition[] conditions) {
                    return List.of(conditions);
                }

                return List.of();
            } catch (NoSuchFieldException ignored) {
                // 往上找父类
            } catch (Throwable throwable) {
                LOGGER.warn("Failed to read GLM conditions from {}", modifier.getClass().getName(), throwable);
                return List.of();
            }
        }

        LOGGER.warn("Failed to read GLM conditions from {}", modifier.getClass().getName());
        return List.of();
    }

    /**
     * 扫出 GLM 里所有「静态的加权表」：字段类型是数组，元素是 (物品提供器, 权重) 两元 record。
     */
    static List<Table> tablesOf(Class<?> owner) {
        List<Table> tables = new ArrayList<>();

        for (Field field : owner.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers()) || !field.getType().isArray()) {
                continue;
            }

            List<Entry> entries = readEntries(field);

            if (!entries.isEmpty()) {
                tables.add(new Table(field.getName(), entries));
            }
        }

        return tables;
    }

    private static List<Entry> readEntries(Field field) {
        Class<?> component = field.getType().getComponentType();
        List<Entry> entries = new ArrayList<>();

        if (component == null || !component.isRecord()) {
            return entries;
        }

        RecordComponent[] components = component.getRecordComponents();
        boolean shaped = components.length == 2;

        for (RecordComponent recordComponent : components) {
            Class<?> type = recordComponent.getType();

            if (!Supplier.class.isAssignableFrom(type) && type != int.class) {
                shaped = false;
            }
        }

        if (!shaped) {
            return entries;
        }

        try {
            field.setAccessible(true);
            Object value = field.get(null);

            if (!(value instanceof Object[] values)) {
                return entries;
            }

            for (Object element : values) {
                Entry entry = readEntry(element, components);

                if (entry != null && entry.weight() > 0) {
                    entries.add(entry);
                }
            }
        } catch (Throwable throwable) {
            LOGGER.warn("Failed to read loot table field {} of {}", field.getName(), field.getDeclaringClass().getName(), throwable);
            entries.clear();
        }

        return entries;
    }

    private static Entry readEntry(Object element, RecordComponent[] components) {
        Item item = null;
        int weight = 0;

        for (RecordComponent recordComponent : components) {
            try {
                Method accessor = recordComponent.getAccessor();
                accessor.setAccessible(true);
                Object value = accessor.invoke(element);

                if (value instanceof Supplier<?> supplier && supplier.get() instanceof Item found) {
                    item = found;
                } else if (value instanceof Integer number) {
                    weight = number;
                }
            } catch (Throwable throwable) {
                LOGGER.warn("Failed to read loot table entry of {}", element.getClass().getName(), throwable);
                return null;
            }
        }

        return item == null ? null : new Entry(item, weight);
    }

    static LootItemCondition[] noConditions() {
        return NO_CONDITIONS;
    }

    private AliGlmReflection() {}
}