package com.gunmu.northeast_china_delight.item;

import com.gunmu.northeast_china_delight.NortheastChinaDelight;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
//? if <1.20.2 {
/*import net.minecraft.sounds.SoundEvent;
*///?} else {
import com.gunmu.northeast_china_delight.util.DdIds;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.List;
//?}

/**
 * 本模组的盔甲材料。现在只有大花袄一件（胸甲槽，2026-11-04 用户：花袄可以作为盔甲栏的装备穿在玩家身上）：
 * 护甲值、耐久、附魔能力都照皮革胸甲（3 点护甲、耐久 80、附魔能力 15），用羊毛在铁砧上修。
 * 穿在身上的样子是 textures/models/armor/floral_jacket_layer_1.png（用户画的）。
 *
 * <p>1.21.1 的盔甲材料是注册表里的东西（{@code Registries.ARMOR_MATERIAL}），ArmorItem 拿的是它的 Holder，
 * 贴图由 {@link ArmorMaterial.Layer} 的名字拼出来；1.20.1 还是个接口，名字写成「模组 id:材料名」，
 * Forge 就去本模组的 textures/models/armor 下找同一张贴图。</p>
 */
public final class ModArmorMaterials {

    /** 材料名，也是穿在身上那张贴图的名字（floral_jacket_layer_1.png） */
    private static final String FLORAL_JACKET_NAME = "floral_jacket";
    /** 胸甲的护甲值（皮革胸甲也是 3） */
    private static final int CHEST_DEFENSE = 3;
    /** 胸甲的耐久：皮革的倍数 5 × 胸甲的 16 */
    public static final int CHEST_DURABILITY = 16 * 5;
    private static final int ENCHANTMENT_VALUE = 15;

    private ModArmorMaterials() {
    }

    //? if <1.20.2 {
    /*public static final ArmorMaterial FLORAL_JACKET = new ArmorMaterial() {
        @Override
        public int getDurabilityForType(ArmorItem.Type type) {
            return type == ArmorItem.Type.CHESTPLATE ? CHEST_DURABILITY : 0;
        }

        @Override
        public int getDefenseForType(ArmorItem.Type type) {
            return defense(type);
        }

        @Override
        public int getEnchantmentValue() {
            return ENCHANTMENT_VALUE;
        }

        @Override
        public SoundEvent getEquipSound() {
            return SoundEvents.ARMOR_EQUIP_LEATHER;
        }

        @Override
        public Ingredient getRepairIngredient() {
            return Ingredient.of(ItemTags.WOOL);
        }

        @Override
        public String getName() {
            return NortheastChinaDelight.MODID + ":" + FLORAL_JACKET_NAME;
        }

        @Override
        public float getToughness() {
            return 0.0F;
        }

        @Override
        public float getKnockbackResistance() {
            return 0.0F;
        }
    };
    *///?} else {
    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS =
            DeferredRegister.create(Registries.ARMOR_MATERIAL, NortheastChinaDelight.MODID);

    public static final Holder<ArmorMaterial> FLORAL_JACKET = ARMOR_MATERIALS.register(FLORAL_JACKET_NAME, () -> {
        EnumMap<ArmorItem.Type, Integer> defense = new EnumMap<>(ArmorItem.Type.class);
        for (ArmorItem.Type type : ArmorItem.Type.values()) {
            defense.put(type, defense(type));
        }
        return new ArmorMaterial(defense, ENCHANTMENT_VALUE, SoundEvents.ARMOR_EQUIP_LEATHER,
                () -> Ingredient.of(ItemTags.WOOL),
                List.of(new ArmorMaterial.Layer(DdIds.of(NortheastChinaDelight.MODID, FLORAL_JACKET_NAME))),
                0.0F, 0.0F);
    });

    public static void register(IEventBus modEventBus) {
        ARMOR_MATERIALS.register(modEventBus);
    }
    //?}

    /** 只做了胸甲，别的槽位给 0 */
    private static int defense(ArmorItem.Type type) {
        return type == ArmorItem.Type.CHESTPLATE ? CHEST_DEFENSE : 0;
    }
}
