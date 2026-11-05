package com.gunmu.northeast_china_delight.block;

import com.gunmu.northeast_china_delight.crafting.BigPotRecipe;
import com.gunmu.northeast_china_delight.crafting.BigPotRecipes;
import com.gunmu.northeast_china_delight.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;

/**
 * 大铁锅做菜（大锅烹饪，配方见 {@link BigPotRecipe}）。不做界面，全靠右键：
 *
 * <ul>
 *   <li>水桶 / 水盆右键：往锅里倒 1000 mB 水；空桶 / 空盆右键：舀走 1000 mB 水。舀干了倒进去的调料也跟着没了，
 *       调料放错了就把水舀走再倒回来（舀出来的是清水，调料不还）；</li>
 *   <li>拿着某条配方用得上的食材、调料右键：放一个进锅（瓶装、碗装的调料退回瓶子 / 碗）。锅里得先有水；
 *       食材最多放用得最多的那条配方那么多个、调料同理（{@link BigPotRecipes#maxIngredients} / {@link BigPotRecipes#maxSeasonings}）；</li>
 *   <li>食材和调料互不相干（2026-11-03 用户）：潜行空手右键随时拿出一个食材补救（最后放进去的先出来，隔着调料也照拿）；
 *       调料倒进去就拿不出来，放错了只能把水舀干重来；锅里只剩调料时右键没反应、不出提示；</li>
 *   <li>灶点着、锅里正好凑成一条配方、盖上锅盖才开始煮（每刻 {@link #serverTick}；2026-11-05 用户：不放锅盖就不开始烹饪）。
 *       开煮以后揭开盖照常煮，盖着时按配方的 {@code lid_speedup} 走得快，中途盖上只让剩下的变快（同日用户：中途可以揭盖）。
 *       开始煮以后锅里的东西就不能动了：不能放、不能拿、不能舀水（右键没反应、不出提示），只能等煮好（2026-11-03 用户）；</li>
 *   <li>煮好了：料和液体都没了，锅里是一锅菜（配方 result 的 count 就是份数），
 *       拿配方写的容器右键盛一份（没写容器就空手盛），每份给配方的经验。拆灶时没盛完的菜不掉，只掉锅。</li>
 * </ul>
 *
 * <p>往锅里放东西、盛菜都得先揭锅盖（盖着盖时右键只会揭盖，见 {@link BigStoveBlock}）。</p>
 *
 * <p>粒子：放料、倒水溅水花；煮着时冒泡和白雾（客户端每刻，{@link #clientTick}）；煮好时冒一阵绿色粒子
 * （像骨粉催熟）；煮着或煮好时揭锅盖冒一股白雾（{@link #lidLifted}）。</p>
 */
public final class BigPotCooking {

    private static final int BUCKET_MB = 1000;

    private BigPotCooking() {
    }

    /** 右键大灶、灶上放着锅（没盖盖），手里拿的东西不是锅 / 锅盖 / 打火石 / 火焰弹 / 锹时 */
    public static InteractionResult onUse(BigPotBlockEntity pot, ItemStack held, BlockState state, Level level,
                                          BlockPos pos, Player player, InteractionHand hand) {
        // 煮好了：只认盛菜的容器
        if (pot.hasResult()) {
            ItemStack container = pot.container();
            if (container.isEmpty() || !held.is(container.getItem())) {
                tell(player, container.isEmpty()
                        ? Component.translatable("message.northeast_china_delight.big_pot.serve_by_hand")
                        : Component.translatable("message.northeast_china_delight.big_pot.serve_with",
                        container.getHoverName()));
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
            if (!level.isClientSide) {
                if (!player.getAbilities().instabuild) {
                    held.shrink(1);
                }
                serve(pot, level, pos, state, player);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        // 倒水 / 舀水：水桶、水盆一次 1000 mB
        boolean waterBucket = held.is(Items.WATER_BUCKET);
        if (waterBucket || held.is(ModItems.WATER_BASIN.get())) {
            if (pot.liquidMb() > 0 && !pot.liquid().isSame(Fluids.WATER)
                    || pot.liquidMb() + BUCKET_MB > BigPotBlockEntity.LIQUID_CAPACITY) {
                tell(player, Component.translatable("message.northeast_china_delight.big_pot.liquid_full"));
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
            if (!level.isClientSide) {
                pot.addLiquid(Fluids.WATER, BUCKET_MB);
                player.setItemInHand(hand, ItemUtils.createFilledResult(held, player,
                        new ItemStack(waterBucket ? Items.BUCKET : ModItems.LARGE_BASIN.get())));
                level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
                splash(level, pos, state, pot, 20);
                level.gameEvent(player, GameEvent.FLUID_PLACE, pos);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        boolean bucket = held.is(Items.BUCKET);
        if ((bucket || held.is(ModItems.LARGE_BASIN.get()))
                && pot.liquid().isSame(Fluids.WATER) && pot.liquidMb() >= BUCKET_MB) {
            if (pot.cooking() != null) {
                // 煮着不让舀，也不出提示（2026-11-03 用户：开始烹饪后不给任何文案）
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
            if (!level.isClientSide) {
                // 舀干了调料跟着没（BigPotBlockEntity.removeLiquid）
                pot.removeLiquid(BUCKET_MB);
                player.setItemInHand(hand, ItemUtils.createFilledResult(held, player,
                        new ItemStack(bucket ? Items.WATER_BUCKET : ModItems.WATER_BASIN.get())));
                level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
                level.gameEvent(player, GameEvent.FLUID_PICKUP, pos);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        // 食材、调料：哪条配方用得上才让放；煮着不让放，锅里没水不让放，放满了不让放
        RecipeManager recipes = level.getRecipeManager();
        if (!BigPotRecipes.usable(recipes, held)) {
            return InteractionResult.PASS;
        }
        if (pot.cooking() != null) {
            // 煮着不让放，不出提示；仍算用掉这次右键，免得手里的食物被吃掉
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (pot.liquidMb() <= 0) {
            tell(player, Component.translatable("message.northeast_china_delight.big_pot.need_water"));
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        boolean seasoning = BigPotRecipes.isSeasoning(recipes, held);
        if (seasoning ? pot.seasoningCount() >= BigPotRecipes.maxSeasonings(recipes)
                : pot.ingredientCount() >= BigPotRecipes.maxIngredients(recipes)) {
            tell(player, Component.translatable(seasoning
                    ? "message.northeast_china_delight.big_pot.seasoning_full"
                    : "message.northeast_china_delight.big_pot.full"));
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!level.isClientSide) {
            ItemStack one = held.copyWithCount(1);
            pot.push(one, seasoning);
            ItemStack remainder = held.hasCraftingRemainingItem() ? held.getCraftingRemainingItem() : ItemStack.EMPTY;
            if (!player.getAbilities().instabuild) {
                held.shrink(1);
                if (!remainder.isEmpty()) {
                    if (held.isEmpty()) {
                        player.setItemInHand(hand, remainder);
                    } else if (!player.getInventory().add(remainder)) {
                        player.drop(remainder, false);
                    }
                }
            }
            level.playSound(null, pos, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 0.5F, 1.0F);
            splash(level, pos, state, pot, 12);
            tell(player, Component.translatable("message.northeast_china_delight.big_pot.added", one.getHoverName()));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /**
     * 空手（主手）右键、灶上放着锅没盖盖时，先问这里：煮好了而且不用容器就空手盛；潜行空手从食材栈顶拿出一个
     * （开始煮了就不让拿）。返回 PASS 就照常走揭盖 / 拿锅。
     */
    public static InteractionResult onEmptyHand(BigPotBlockEntity pot, BlockState state, Level level, BlockPos pos,
                                                Player player) {
        if (pot.hasResult() && pot.container().isEmpty()) {
            if (!level.isClientSide) {
                serve(pot, level, pos, state, player);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (player.isSecondaryUseActive() && !pot.hasResult() && !pot.contents().isEmpty()) {
            if (!level.isClientSide) {
                if (pot.cooking() != null) {
                    return InteractionResult.SUCCESS;       // 煮着不让拿，不出提示
                }
                // 拿最后放进去的那个食材；锅里只剩调料就什么都不拿，不出提示
                ItemStack taken = pot.popIngredient();
                if (!taken.isEmpty()) {
                    give(player, taken);
                    level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2F, 1.0F);
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    /**
     * 灶点着、灶上有锅时每个服务端 tick 调一次（只在 nw 那一格的方块实体上跑，见 {@link BigStoveBlock#getTicker}）。
     * 锅里凑成哪条配方、盖上锅盖（state 里 pot=lid）才开煮；开煮以后揭着盖照常煮，盖着按 lid_speedup 缩短时间；换了配方从头算。
     * {@code cooking} 不为空 = 已经开煮（锅里锁着），所以还没盖过盖时不设：料凑齐了没盖盖，照样能放能拿。
     */
    public static void serverTick(Level level, BlockPos pos, BlockState state, BigPotBlockEntity pot) {
        if (pot.hasResult()) {
            return;
        }
        BigPotRecipe recipe = BigPotRecipes.match(level.getRecipeManager(), pot.liquid(), pot.liquidMb(), pot.contents());
        boolean lid = state.getValue(BigStoveBlock.POT) == BigStoveBlock.Pot.LID;
        // 不盖锅盖不开煮（2026-11-05 用户）；已经开煮的揭开盖照常煮（同日用户：中途可以揭盖，进度正常走）
        boolean started = recipe != null && recipe.id().equals(pot.cooking());
        if (recipe == null || !started && !lid) {
            pot.setCooking(null);
            return;
        }
        pot.setCooking(recipe.id());
        // 进度按不盖锅盖的时长算；盖着锅盖每刻多走「不盖盖时长 / 盖盖时长」，中途盖上只让剩下的变快
        // （以前盖上就换成缩短后的总时长、煮过的刻数不变，进度一下放大 1.67 倍，2026-11-03 用户报）
        int total = recipe.cookingTime();
        pot.setCookTotal(total);
        pot.tickCooking(lid ? total / (float) recipe.lidCookingTime() : 1.0F);
        // 煮着时的冒泡、白雾在客户端画（clientTick，看锅里同步过去的 cooking）
        if (pot.cookProgress() < total - 0.001F) {
            return;
        }
        float surface = BigPotBlockEntity.liquidSurfaceY(Math.max(pot.liquidMb(), BUCKET_MB));
        pot.finish(recipe.result(), recipe.container(), recipe.experience());
        level.playSound(null, pos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 1.0F, 0.8F);
        // 煮好了：像骨粉催熟那样冒一阵绿色粒子
        if (level instanceof ServerLevel server) {
            Vec3 center = center(pos, state);
            server.sendParticles(ParticleTypes.HAPPY_VILLAGER, center.x, originY(pos, state) + surface / 16.0 + 0.2,
                    center.z, 24, 0.45, 0.2, 0.45, 0.0);
        }
    }

    /**
     * 客户端每刻（灶点着、灶上有锅时）：锅里凑成配方开始煮了（cooking 会同步过来），揭着盖就在液面上冒泡、往上飘白雾，
     * 盖着盖只从锅盖边上偶尔漏一点白雾。煮好以后不冒。
     */
    public static void clientTick(Level level, BlockPos pos, BlockState state, BigPotBlockEntity pot) {
        if (pot.cooking() == null || pot.hasResult()) {
            return;
        }
        RandomSource random = level.getRandom();
        Vec3 center = center(pos, state);
        double base = originY(pos, state);
        if (state.getValue(BigStoveBlock.POT) == BigStoveBlock.Pot.LID) {
            if (random.nextFloat() < 0.08F) {
                double a = random.nextDouble() * Math.PI * 2;
                level.addParticle(ParticleTypes.CLOUD, center.x + Math.cos(a) * 0.82, base + 1.1,
                        center.z + Math.sin(a) * 0.82, 0.0, 0.03, 0.0);
            }
            return;
        }
        double surface = base + (pot.liquidMb() > 0 ? BigPotBlockEntity.liquidSurfaceY(pot.liquidMb()) : 11.0F) / 16.0;
        if (pot.liquidMb() > 0 && random.nextFloat() < 0.6F) {
            // 液面上咕嘟咕嘟冒泡（锅里水面那一圈半径 10 像素，往里收一点）
            double a = random.nextDouble() * Math.PI * 2;
            double r = Math.sqrt(random.nextDouble()) * 0.55;
            level.addParticle(ParticleTypes.BUBBLE_POP, center.x + Math.cos(a) * r, surface + 0.02,
                    center.z + Math.sin(a) * r, 0.0, 0.02, 0.0);
        }
        if (random.nextFloat() < 0.15F) {
            double a = random.nextDouble() * Math.PI * 2;
            double r = random.nextDouble() * 0.5;
            level.addParticle(ParticleTypes.CLOUD, center.x + Math.cos(a) * r, surface + 0.1,
                    center.z + Math.sin(a) * r, 0.0, 0.03 + random.nextDouble() * 0.02, 0.0);
        }
    }

    /**
     * 揭锅盖时（BigStoveBlock 里，盖子拿下来之前调）：正在煮或者已经煮好，就从锅里冒起一股白雾。
     */
    public static void lidLifted(Level level, BlockPos pos, BlockState state, BigPotBlockEntity pot) {
        if (!(level instanceof ServerLevel server) || pot.cooking() == null && !pot.hasResult()) {
            return;
        }
        Vec3 center = center(pos, state);
        double y = originY(pos, state) + 1.1;
        RandomSource random = level.getRandom();
        for (int i = 0; i < 28; i++) {
            double a = random.nextDouble() * Math.PI * 2;
            double r = random.nextDouble() * 0.55;
            // count 0：后面三个数就是这一粒的速度，一股往上冲
            server.sendParticles(ParticleTypes.CLOUD, center.x + Math.cos(a) * r, y + random.nextDouble() * 0.2,
                    center.z + Math.sin(a) * r, 0, (random.nextDouble() - 0.5) * 0.04,
                    0.12 + random.nextDouble() * 0.1, (random.nextDouble() - 0.5) * 0.04, 1.0);
        }
        level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.4F, 1.6F);
    }

    /** 往锅里放东西 / 倒水：液面上溅一圈水花（锅里没液体就不溅） */
    private static void splash(Level level, BlockPos pos, BlockState state, BigPotBlockEntity pot, int count) {
        if (!(level instanceof ServerLevel server) || pot.liquidMb() <= 0) {
            return;
        }
        Vec3 center = center(pos, state);
        server.sendParticles(ParticleTypes.SPLASH, center.x, originY(pos, state)
                        + BigPotBlockEntity.liquidSurfaceY(pot.liquidMb()) / 16.0 + 0.02, center.z,
                count, 0.3, 0.02, 0.3, 0.1);
    }

    private static double originY(BlockPos pos, BlockState state) {
        return BigStoveBlock.origin(pos, state).getY();
    }

    /** 空手把锅从灶上拿下来之前问一声：锅里还有东西（液体、料、没盛完的菜）就不让拿 */
    public static boolean canTakePot(BigPotBlockEntity pot) {
        return pot.isEmpty();
    }

    /** 大灶被拆时：只掉还没煮的食材（调料、煮好的菜都不掉） */
    public static void onBroken(Level level, BlockPos pos, BigPotBlockEntity pot) {
        pot.dropContents(level, pos);
    }

    private static void serve(BigPotBlockEntity pot, Level level, BlockPos pos, BlockState state, Player player) {
        float experience = pot.experience();
        ItemStack dish = pot.serve();
        if (dish.isEmpty()) {
            return;
        }
        give(player, dish);
        level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 0.8F, 1.2F);
        if (experience > 0.0F && level instanceof ServerLevel server) {
            int xp = Mth.floor(experience);
            if (level.getRandom().nextFloat() < experience - xp) {
                xp++;
            }
            if (xp > 0) {
                ExperienceOrb.award(server, center(pos, state), xp);
            }
        }
    }

    /** 锅的正中（四格的交点），在灶面上一点 */
    private static Vec3 center(BlockPos pos, BlockState state) {
        BlockPos origin = BigStoveBlock.origin(pos, state);
        return new Vec3(origin.getX() + 1.0, origin.getY() + 1.05, origin.getZ() + 1.0);
    }

    private static void give(Player player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    private static void tell(Player player, Component message) {
        if (!player.level().isClientSide) {
            player.displayClientMessage(message, true);
        }
    }
}
