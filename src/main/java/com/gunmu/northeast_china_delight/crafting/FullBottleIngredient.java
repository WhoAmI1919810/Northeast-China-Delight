// 1.20.1（Forge 47.1）专用：匹配未消耗的满瓶调料（damage == 0）。
// Forge 自带的 forge:partial_nbt 要求 ItemStack 必须带有非空 tag 且含 Damage:0 键，
// 但 1.20.1 全新制作的物品 tag 为 null，会导致动物油块合成与 Create 分液配方全部拒收全新满瓶。
//? if <1.20.2 {
package com.gunmu.northeast_china_delight.crafting;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.gunmu.northeast_china_delight.NortheastChinaDelight;
import com.gunmu.northeast_china_delight.util.DdIds;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.crafting.AbstractIngredient;
import net.minecraftforge.common.crafting.IIngredientSerializer;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

// 匹配指定物品且当前损耗值 getDamageValue() == 0 的堆叠（兼容无 tag 全新物品与合瓶补满后的 Damage:0 物品）。
public final class FullBottleIngredient extends AbstractIngredient {

    public static final ResourceLocation ID = DdIds.of(NortheastChinaDelight.MODID, "full_bottle");
    public static final IIngredientSerializer<FullBottleIngredient> SERIALIZER = new Serializer();

    private final List<Item> items;

    public FullBottleIngredient(List<Item> items) {
        super(items.stream().map(item -> new Ingredient.ItemValue(new ItemStack(item))));
        this.items = List.copyOf(items);
    }

    @Override
    public boolean test(@Nullable ItemStack stack) {
        if (stack == null || stack.isEmpty() || stack.getDamageValue() != 0) {
            return false;
        }
        for (Item item : this.items) {
            if (stack.is(item)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isSimple() {
        return false;
    }

    @Override
    public IIngredientSerializer<? extends Ingredient> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public JsonElement toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("type", ID.toString());
        if (this.items.size() == 1) {
            json.addProperty("item", BuiltInRegistries.ITEM.getKey(this.items.get(0)).toString());
        } else {
            JsonArray array = new JsonArray();
            for (Item item : this.items) {
                array.add(BuiltInRegistries.ITEM.getKey(item).toString());
            }
            json.add("items", array);
        }
        return json;
    }

    private static final class Serializer implements IIngredientSerializer<FullBottleIngredient> {
        @Override
        public FullBottleIngredient parse(FriendlyByteBuf buffer) {
            int size = buffer.readVarInt();
            List<Item> items = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                Item item = BuiltInRegistries.ITEM.get(buffer.readResourceLocation());
                if (item != Items.AIR) {
                    items.add(item);
                }
            }
            return new FullBottleIngredient(items);
        }

        @Override
        public FullBottleIngredient parse(JsonObject json) {
            List<Item> items = new ArrayList<>();
            if (json.has("items")) {
                JsonArray array = GsonHelper.getAsJsonArray(json, "items");
                for (JsonElement element : array) {
                    items.add(resolveItem(GsonHelper.convertToString(element, "item")));
                }
            } else {
                items.add(resolveItem(GsonHelper.getAsString(json, "item")));
            }
            if (items.isEmpty()) {
                throw new JsonSyntaxException("full_bottle ingredient requires at least one item");
            }
            return new FullBottleIngredient(items);
        }

        @Override
        public void write(FriendlyByteBuf buffer, FullBottleIngredient ingredient) {
            buffer.writeVarInt(ingredient.items.size());
            for (Item item : ingredient.items) {
                buffer.writeResourceLocation(BuiltInRegistries.ITEM.getKey(item));
            }
        }

        private static Item resolveItem(String id) {
            ResourceLocation loc = DdIds.parse(id);
            Item item = BuiltInRegistries.ITEM.get(loc);
            if (item == Items.AIR) {
                throw new JsonSyntaxException("Unknown item '" + id + "' in full_bottle ingredient");
            }
            return item;
        }
    }
}
//?}
