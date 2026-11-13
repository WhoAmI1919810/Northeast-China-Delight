package com.gunmu.northeast_china_delight.gametest;

import com.mojang.authlib.GameProfile;
import com.gunmu.northeast_china_delight.NortheastChinaDelight;
import com.gunmu.northeast_china_delight.advancement.ModAdvancements;
import com.gunmu.chinese_regional_culinary_ingredients.block.BigPotBlockEntity;
import com.gunmu.chinese_regional_culinary_ingredients.block.BigPotCooking;
import com.gunmu.chinese_regional_culinary_ingredients.block.BigStoveBlock;
import com.gunmu.chinese_regional_culinary_ingredients.block.MysteryStewPotBlockEntity;
import com.gunmu.northeast_china_delight.block.ModBlocks;
import com.gunmu.chinese_regional_culinary_ingredients.crafting.MysteryStew;
import com.gunmu.chinese_regional_culinary_ingredients.effect.CrciMobEffects;
import com.gunmu.chinese_regional_culinary_ingredients.item.CrciItems;
import com.gunmu.chinese_regional_culinary_ingredients.item.MysteryStewEffects;
import com.gunmu.northeast_china_delight.item.ModItems;
import com.gunmu.northeast_china_delight.util.DdEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
//? if >=1.20.2 {
import net.minecraft.server.level.ClientInformation;
//?}
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
//? if <1.20.5 {
/*import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
*///?} else {
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
//?}

import java.util.List;
import java.util.UUID;

@GameTestHolder(NortheastChinaDelight.MODID)
@PrefixGameTestTemplate(false)
public final class MysteryStewGameTests {

    private MysteryStewGameTests() {
    }

    private static Player mockCreativePlayer(GameTestHelper helper) {
        //? if <1.20.5 {
        /*return helper.makeMockPlayer();
        *///?} else {
        return helper.makeMockPlayer(GameType.CREATIVE);
        //?}
    }

    private static ServerPlayer serverPlayer(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        //? if >=1.20.2 {
        return new ServerPlayer(level.getServer(), level,
                new GameProfile(UUID.randomUUID(), "test-mock-player"), ClientInformation.createDefault());
        //?} else {
        /*return new ServerPlayer(level.getServer(), level,
                new GameProfile(UUID.randomUUID(), "test-mock-player"));
        *///?}
    }

    @GameTest(template = "empty")
    public static void challengeMysteryStewAwardsFeastWhenServed(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = serverPlayer(helper);
        BlockPos ground = helper.absolutePos(new BlockPos(1, 1, 1));
        BlockPos target = ground.above();
        level.setBlockAndUpdate(ground, Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(target, ModBlocks.BIG_STOVE.get().defaultBlockState()
                .setValue(BigStoveBlock.FACING, Direction.NORTH)
                .setValue(BigStoveBlock.PART, BigStoveBlock.Part.NW)
                .setValue(BigStoveBlock.LIT, true)
                .setValue(BigStoveBlock.POT, BigStoveBlock.Pot.POT));
        helper.assertTrue(level.getBlockEntity(target) instanceof BigPotBlockEntity,
                "大灶西北角应有大锅方块实体");
        BigPotBlockEntity pot = (BigPotBlockEntity) level.getBlockEntity(target);

        pot.addLiquid(Fluids.WATER, 1000);
        pot.push(new ItemStack(Items.BEEF), false);
        pot.push(new ItemStack(Items.CARROT), false);
        pot.push(new ItemStack(Items.APPLE), false);
        pot.push(new ItemStack(CrciItems.SALT.get()), true);
        pot.push(new ItemStack(CrciItems.SOY_SAUCE.get()), true);
        pot.push(new ItemStack(CrciItems.VINEGAR.get()), true);
        List<MysteryStewEffects.Entry> effects =
                MysteryStew.effects(pot.ingredientStacks(), pot.seasoningKinds());
        helper.assertTrue(effects.size() == MysteryStew.KINDS_GIVEN,
                "挑战锅应算出三种效果，实际为 " + describe(effects));
        helper.assertTrue(pot.seasoningKinds() == 3, "挑战锅应有三类调料，实际为 " + pot.seasoningKinds());

        pot.beginCooking(MysteryStew.ID, MysteryStew.COOKING_TIME, 0, MysteryStew.cook(pot));
        pot.tickCooking(MysteryStew.COOKING_TIME - 1);
        BigPotCooking.serverTick(level, target, level.getBlockState(target), pot);
        helper.assertTrue(pot.hasResult() && pot.result().is(CrciItems.MYSTERY_STEW.get()),
                "这锅应煮成盆装谜之炖菜");
        //? if <1.20.5 {
        /*helper.assertTrue(pot.saveWithFullMetadata().getBoolean("ChallengeMysteryStew"),
                "这锅应设置挑战标记");
        *///?} else {
        helper.assertTrue(pot.saveWithFullMetadata(level.registryAccess()).getBoolean("ChallengeMysteryStew"),
                "这锅应设置挑战标记");
        //?}

        ItemStack basin = new ItemStack(CrciItems.LARGE_BASIN.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, basin);
        BigPotCooking.onUse(pot, basin, level.getBlockState(target), level, target, player,
                InteractionHand.MAIN_HAND);
        helper.assertTrue(player.getInventory().items.stream()
                        .anyMatch(stack -> stack.is(CrciItems.MYSTERY_STEW.get())),
                "玩家应拿到盆装谜之炖菜");
        //? if <1.20.5 {
        /*helper.assertTrue(!pot.saveWithFullMetadata().getBoolean("ChallengeMysteryStew"),
                "盛出时应取走挑战标记");
        *///?} else {
        helper.assertTrue(!pot.saveWithFullMetadata(level.registryAccess()).getBoolean("ChallengeMysteryStew"),
                "盛出时应取走挑战标记");
        //?}
        helper.assertTrue(player.getServer() != null, "测试玩家应绑定服务器");
        //? if <1.20.2 {
        /*var advancement = level.getServer().getAdvancements()
                .getAdvancement(ModAdvancements.MYSTERY_STEW_FEAST);
        *///?} else {
        var advancement = level.getServer().getAdvancements()
                .get(ModAdvancements.MYSTERY_STEW_FEAST);
        //?}
        helper.assertTrue(advancement != null, "应能加载谜之炖菜挑战进度");
        helper.assertTrue(player.getAdvancements().getOrStartProgress(advancement).isDone(),
                "挑战锅盛出后应完成谜之炖菜挑战进度");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void bowlWritesEveryEffectToItsTag(GameTestHelper helper) {
        List<MysteryStewEffects.Entry> effects = mixedEffects();
        ItemStack stew = MysteryStewEffects.bowl(effects);
        //? if <1.20.5 {
        /*CompoundTag tag = stew.getTag();
        ListTag raw = tag == null ? new ListTag() : tag.getList("Effects", Tag.TAG_COMPOUND);
        helper.assertTrue(raw.size() == effects.size(),
                "直接盛出的碗 NBT 应写入 " + effects.size() + " 种效果，实际为 "
                        + raw.size() + " 种：" + raw);
        *///?}
        assertEffects(helper, "直接盛出的碗", effects, MysteryStewEffects.readBowl(stew));
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void placedBasinKeepsAllEffectsWhenServed(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Player player = mockCreativePlayer(helper);
        List<MysteryStewEffects.Entry> effects = mixedEffects();
        BlockPos target = placeBasin(helper, level, player, effects);

        ItemStack served = serve(helper, level, player, target);
        assertEffects(helper, "碗装谜之炖菜", effects, MysteryStewEffects.readBowl(served));
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void placedBasinKeepsAllEffectsAfterSaveLoadAndEating(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Player player = mockCreativePlayer(helper);
        List<MysteryStewEffects.Entry> effects = mixedEffects();
        BlockPos target = placeBasin(helper, level, player, effects);

        MysteryStewPotBlockEntity pot = (MysteryStewPotBlockEntity) level.getBlockEntity(target);
        //? if <1.20.5 {
        /*CompoundTag saved = pot.saveWithFullMetadata();
        pot.load(saved);
        *///?} else {
        CompoundTag saved = pot.saveWithFullMetadata(level.registryAccess());
        pot.loadWithComponents(saved, level.registryAccess());
        //?}
        assertEffects(helper, "方块实体存读档后", effects, pot.effects());

        ItemStack served = serve(helper, level, player, target);
        assertEffects(helper, "存读档后盛出的碗", effects, MysteryStewEffects.readBowl(served));

        ItemStack held = served.copy();
        player.setItemInHand(InteractionHand.MAIN_HAND, held);
        held.getItem().finishUsingItem(held, level, player);
        for (MysteryStewEffects.Entry entry : effects) {
            helper.assertTrue(DdEffects.hasEffect(player, entry.effect()),
                    "食用碗装谜之炖菜后缺少效果 " + effectId(entry));
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void stewDurationUsesItemCountVarietyAndSeasoning(GameTestHelper helper) {
        List<ItemStack> ingredients = List.of(
                new ItemStack(Items.BEEF, 3),
                new ItemStack(Items.PORKCHOP, 2),
                new ItemStack(Items.MUTTON, 2));

        assertFormulaDuration(helper, "无调料", ingredients, 0, "[minecraft:strength:170]");
        assertFormulaDuration(helper, "1 种调料", ingredients, 1, "[minecraft:strength:238]");
        assertFormulaDuration(helper, "2 种调料", ingredients, 2, "[minecraft:strength:297]");
        assertFormulaDuration(helper, "3 种调料", ingredients, 3, "[minecraft:strength:391]");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void stewDurationIsCalculatedPerEffectKind(GameTestHelper helper) {
        List<ItemStack> ingredients = List.of(
                new ItemStack(Items.BEEF, 3),
                new ItemStack(Items.PORKCHOP, 2),
                new ItemStack(Items.MUTTON),
                new ItemStack(Items.CARROT));

        assertFormulaDuration(helper, "肉类与根茎类", ingredients, 2,
                "[minecraft:strength:287, minecraft:night_vision:178]");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void stewKeepsSaturationDurationAndThreeEffectSelection(GameTestHelper helper) {
        List<ItemStack> ingredients = List.of(
                new ItemStack(Items.BEEF, 2),
                new ItemStack(Items.APPLE, 2),
                new ItemStack(Items.POTATO, 2),
                new ItemStack(Items.CARROT));

        assertFormulaDuration(helper, "三效果与饱和", ingredients, 3,
                "[minecraft:strength:294, minecraft:jump_boost:179, minecraft:saturation:7]");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void stewEffectInitialDurationsMatchDesign(GameTestHelper helper) {
        assertInitialTicks(helper, MysteryStew.Kind.POISON, 60);
        assertInitialTicks(helper, MysteryStew.Kind.ROTTEN, 60);
        assertInitialTicks(helper, MysteryStew.Kind.SEA_CUCUMBER, 60);
        assertInitialTicks(helper, MysteryStew.Kind.OFFAL, 60);
        assertInitialTicks(helper, MysteryStew.Kind.BONE, 60);
        assertInitialTicks(helper, MysteryStew.Kind.FRUIT, 60);
        assertInitialTicks(helper, MysteryStew.Kind.GINSENG, 80);
        assertInitialTicks(helper, MysteryStew.Kind.GOLDEN, 80);
        assertInitialTicks(helper, MysteryStew.Kind.CHORUS, 80);
        assertInitialTicks(helper, MysteryStew.Kind.SPICY, 80);
        assertInitialTicks(helper, MysteryStew.Kind.NUT, 80);
        assertInitialTicks(helper, MysteryStew.Kind.SEAFOOD, 80);
        assertInitialTicks(helper, MysteryStew.Kind.FAT, 90);
        assertInitialTicks(helper, MysteryStew.Kind.ROOT, 90);
        assertInitialTicks(helper, MysteryStew.Kind.ALLIUM, 90);
        assertInitialTicks(helper, MysteryStew.Kind.BEAN, 90);
        assertInitialTicks(helper, MysteryStew.Kind.GREENS, 90);
        assertInitialTicks(helper, MysteryStew.Kind.PICKLE, 110);
        assertInitialTicks(helper, MysteryStew.Kind.FUNGUS, 110);
        assertInitialTicks(helper, MysteryStew.Kind.EGG, 110);
        assertInitialTicks(helper, MysteryStew.Kind.MEAT, 110);
        assertInitialTicks(helper, MysteryStew.Kind.STAPLE, 0);
        helper.succeed();
    }

    private static void assertFormulaDuration(GameTestHelper helper, String stage,
                                              List<ItemStack> ingredients, int seasoningKinds,
                                              String expected) {
        List<MysteryStewEffects.Entry> actual = MysteryStew.effects(ingredients, seasoningKinds);
        helper.assertTrue(expected.equals(describe(actual)),
                stage + "时长不符，期望 " + expected + "，实际 " + describe(actual));
    }

    private static void assertInitialTicks(GameTestHelper helper, MysteryStew.Kind kind, int expected) {
        helper.assertTrue(kind.initialTicks() == expected,
                kind + " 初始时长应为 " + expected + " 刻，实际为 " + kind.initialTicks());
    }

    private static List<MysteryStewEffects.Entry> mixedEffects() {
        return List.of(
                new MysteryStewEffects.Entry(DdEffects.hold(MobEffects.POISON), 80),
                new MysteryStewEffects.Entry(DdEffects.hold(CrciMobEffects.GREASY), 80),
                new MysteryStewEffects.Entry(DdEffects.hold(CrciMobEffects.WARMTH), 80));
    }

    private static BlockPos placeBasin(GameTestHelper helper, ServerLevel level, Player player,
                                       List<MysteryStewEffects.Entry> effects) {
        BlockPos ground = helper.absolutePos(new BlockPos(1, 1, 1));
        BlockPos target = ground.above();
        level.setBlockAndUpdate(ground, Blocks.STONE.defaultBlockState());

        ItemStack basin = new ItemStack(ModItems.MYSTERY_STEW.get());
        MysteryStewEffects.write(basin, effects);
        player.setItemInHand(InteractionHand.MAIN_HAND, basin);

        BlockHitResult placeHit = new BlockHitResult(Vec3.atCenterOf(target), Direction.UP, target.below(), false);
        basin.getItem().useOn(new UseOnContext(level, player, InteractionHand.MAIN_HAND, basin, placeHit));
        helper.assertTrue(level.getBlockState(target).is(ModBlocks.MYSTERY_STEW_POT.get()),
                "盆装谜之炖菜应能放下");
        helper.assertTrue(level.getBlockEntity(target) instanceof MysteryStewPotBlockEntity,
                "放下后应有方块实体");
        MysteryStewPotBlockEntity pot = (MysteryStewPotBlockEntity) level.getBlockEntity(target);
        assertEffects(helper, "放下后的方块实体", effects, pot.effects());
        return target;
    }

    private static ItemStack serve(GameTestHelper helper, ServerLevel level, Player player, BlockPos target) {
        ItemStack bowl = new ItemStack(Items.BOWL);
        player.setItemInHand(InteractionHand.MAIN_HAND, bowl);
        BlockHitResult serveHit = new BlockHitResult(Vec3.atCenterOf(target), Direction.UP, target, false);
        //? if <1.20.5 {
        /*level.getBlockState(target).use(level, player, InteractionHand.MAIN_HAND, serveHit);
        *///?} else {
        level.getBlockState(target).useItemOn(bowl, level, player, InteractionHand.MAIN_HAND, serveHit);
        //?}

        ItemStack served = level.getEntitiesOfClass(ItemEntity.class,
                        new AABB(target).inflate(2.0D)).stream()
                .map(ItemEntity::getItem)
                .filter(stack -> stack.is(Items.SUSPICIOUS_STEW))
                .findFirst()
                .orElse(ItemStack.EMPTY);
        helper.assertTrue(!served.isEmpty(), "用碗盛时应掉出一碗谜之炖菜");
        return served;
    }

    private static void assertEffects(GameTestHelper helper, String stage,
                                      List<MysteryStewEffects.Entry> expected,
                                      List<MysteryStewEffects.Entry> actual) {
        helper.assertTrue(actual.size() == expected.size(),
                stage + "应保留 " + expected.size() + " 种效果，实际为 " + actual.size() + " 种："
                        + describe(actual));
        for (int index = 0; index < expected.size(); index++) {
            MysteryStewEffects.Entry expectedEntry = expected.get(index);
            MysteryStewEffects.Entry actualEntry = actual.get(index);
            helper.assertTrue(effectId(expectedEntry).equals(effectId(actualEntry))
                            && expectedEntry.duration() == actualEntry.duration(),
                    stage + "第 " + (index + 1) + " 种效果不一致，期望 "
                            + describe(List.of(expectedEntry)) + "，实际 "
                            + describe(List.of(actualEntry)));
        }
    }

    private static String describe(List<MysteryStewEffects.Entry> effects) {
        return effects.stream()
                .map(entry -> effectId(entry) + ":" + entry.duration())
                .toList()
                .toString();
    }

    private static String effectId(MysteryStewEffects.Entry entry) {
        return entry.effect().unwrapKey().orElseThrow().location().toString();
    }
}
