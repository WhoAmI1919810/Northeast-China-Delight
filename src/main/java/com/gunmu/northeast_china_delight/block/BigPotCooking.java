package com.gunmu.northeast_china_delight.block;

import com.gunmu.chinese_regional_culinary_ingredients.item.CrciItems;

import com.gunmu.northeast_china_delight.crafting.BigPotRecipe;
import com.gunmu.northeast_china_delight.crafting.BigPotRecipes;
import com.gunmu.northeast_china_delight.crafting.MysteryStew;
import com.gunmu.northeast_china_delight.item.ModItems;
import com.gunmu.northeast_china_delight.item.SeasoningBottleItem;
import com.gunmu.northeast_china_delight.particle.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
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
import org.jetbrains.annotations.Nullable;

//? if <1.20.2 {
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
//?} else {
/*import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
*///?}

import java.util.HashSet;
import java.util.Set;

/**
 * 大铁锅做菜（大锅烹饪，配方见 {@link BigPotRecipe}）。不做界面，全靠右键：
 *
 * <ul>
 *   <li>水桶 / 水盆右键：往锅里倒 1000 mB 水；空桶 / 空盆右键：舀走 1000 mB 水。舀干了倒进去的调料也跟着没了，
 *       调料放错了就把水舀走再倒回来（舀出来的是清水，调料不还）；</li>
 *   <li>拿着某条配方用得上的食材、调料右键：放一个进锅（瓶装、碗装的调料退回瓶子 / 碗）。配方用不上、但谜之炖菜认的食材调料
 *       也能放（{@link MysteryStew#accepts}：原版胡萝卜、农夫乐事番茄洋葱、醋……）。锅里得先有水；
 *       食材最多放用得最多的那条配方那么多个、调料同理（{@link BigPotRecipes#maxIngredients} / {@link BigPotRecipes#maxSeasonings}）；</li>
 *   <li>食材和调料互不相干（2026-11-03 用户）：潜行空手右键随时拿出一个食材补救（最后放进去的先出来，隔着调料也照拿）；
 *       调料倒进去就拿不出来，放错了只能把水舀干重来；锅里只剩调料时右键没反应、不出提示；</li>
 *   <li>灶点着、锅里正好凑成一条配方、盖上锅盖后先等 3 秒再开始煮（每刻 {@link #serverTick}）；这 3 秒内揭盖会取消本次烹饪。
 *       开煮以后揭开盖照常煮，盖着时按配方的 {@code lid_speedup} 走得快，中途盖上只让剩下的变快。
 *       开始煮以后锅里的东西就不能动了：不能放、不能拿、不能舀水（右键没反应、不出提示），只能等煮好（2026-11-03 用户）；
 *       也不能倒水（2026-11-06 配方的水量有了上限，多倒一桶会凑不上）；</li>
 *   <li>凑不上任何配方、锅里有水有食材，盖上锅盖照样煮，煮出一锅谜之炖菜（同日用户，{@link MysteryStew}）：
 *       用大脸盆盛出一盆，效果按锅里数量最多的两类食材定；</li>
 *   <li>煮好了：料和液体都没了，锅里是一锅菜（配方 result 的 count 就是份数），
 *       拿配方写的容器右键盛一份（没写容器就空手盛），每份给配方的经验。拆灶时没盛完的菜不掉，只掉锅。</li>
 * </ul>
 *
 * <p>往锅里放东西、盛菜都得先揭锅盖（盖着盖时右键只会揭盖，见 {@link BigStoveBlock}）。</p>
 *
 * <p>东西也能直接掉进锅里（2026-11-05 用户，{@link #dropIn}）：落在锅口的掉落物、从上面往下送的漏斗和投掷器。
 * 水桶、水盆倒水后还出空桶、空盆；食材和所有配方认的调料都收，瓶装调料一次只扣完整的一份 50 mB。
 * 收不进的留在灶上，都不出提示；返还的瓶、碗先送进贴着大灶的库存，送不进才从锅口外沿蹦出来。</p>
 *
 * <p>粒子：放料、倒水溅水花；煮着时冒白雾，煮到六成以上汤面上冒泡（{@link ModParticles#POT_BUBBLE}，
 * 客户端每刻，{@link #clientTick}）；煮好时冒一阵绿色粒子（像骨粉催熟），之后接着冒泡、越来越少，
 * 盛过一份就不冒；一锅谜之炖菜往上飘效果粒子（{@link MysteryStewPotBlock#effectParticle}）；
 * 煮着或煮好时揭锅盖冒一股白雾（{@link #lidLifted}）。</p>
 */
public final class BigPotCooking {

    private static final int BUCKET_MB = 1000;
    /** 放上锅盖后先等 3 秒再开煮；这 3 秒内揭盖可以反悔 */
    private static final int START_DELAY_TICKS = 60;

    private BigPotCooking() {
    }

    /** 右键大灶、灶上放着锅（没盖盖），手里拿的东西不是锅 / 锅盖 / 打火石 / 火焰弹 / 锹时 */
    public static InteractionResult onUse(BigPotBlockEntity pot, ItemStack held, BlockState state, Level level,
                                          BlockPos pos, Player player, InteractionHand hand) {
        // 煮好了：只认盛菜的容器
        if (pot.hasResult()) {
            ItemStack container = pot.container();
            if (container.isEmpty()) {
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
            if (!held.is(container.getItem())) {
                tell(player, Component.translatable("message.northeast_china_delight.big_pot.serve_with",
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
        if (isWater(held)) {
            if (pot.cooking() != null) {
                // 煮着不让倒水，不出提示：配方的水量有范围（2026-11-06），多倒一桶就凑不上、改煮谜之炖菜了
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
            if (!canPour(pot)) {
                tell(player, Component.translatable("message.northeast_china_delight.big_pot.full"));
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
            if (!level.isClientSide) {
                ItemStack emptyContainer = emptied(held);
                if (BigStoveBlock.isFakePlayer(player)) {
                    // 机械手（Deployer）没有正常背包：倒完的空桶 / 空盆直接送进贴锅库存
                    if (!player.getAbilities().instabuild) {
                        held.shrink(1);
                        output(level, pos, state, emptyContainer);
                    }
                } else {
                    player.setItemInHand(hand, ItemUtils.createFilledResult(held, player, emptyContainer));
                }
                pour(pot, level, pos, state, player);
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

        // 食材、调料：哪条配方用得上、或者谜之炖菜认的才让放；煮着不让放，锅里没水不让放，放满了不让放
        RecipeManager recipes = level.getRecipeManager();
        if (!usable(recipes, held)) {
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
        boolean seasoning = isSeasoning(recipes, held);
        if (seasoning && !seasoningReady(held)) {
            // 瓶里不足一份：不把余量当一份吞掉，直接当这次右键没发生
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (full(pot, recipes, seasoning)) {
            tell(player, Component.translatable("message.northeast_china_delight.big_pot.full"));
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!level.isClientSide) {
            ItemStack one = held.copyWithCount(1);
            addOne(pot, one, seasoning, level, pos, state);
            ItemStack remainder = remainderOf(one);
            if (!player.getAbilities().instabuild) {
                held.shrink(1);
                if (!remainder.isEmpty()) {
                    if (BigStoveBlock.isFakePlayer(player)) {
                        // 机械手（Deployer）投完调料后，剩余瓶 / 碗不留在假玩家背包里
                        output(level, pos, state, remainder);
                    } else if (held.isEmpty()) {
                        player.setItemInHand(hand, remainder);
                    } else if (!player.getInventory().add(remainder)) {
                        player.drop(remainder, false);
                    }
                }
            }
            tell(player, Component.translatable("message.northeast_china_delight.big_pot.added", one.getHoverName()));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    // ---------------------------------------------------------------- 倒水、放料（右键和掉进锅里共用）

    /** 水桶、水盆：一次倒 1000 mB 水 */
    private static boolean isWater(ItemStack stack) {
        return stack.is(Items.WATER_BUCKET) || stack.is(ModItems.WATER_BASIN.get());
    }

    /** 倒完水还回来的：水桶还空桶，水盆还空盆 */
    private static ItemStack emptied(ItemStack water) {
        return new ItemStack(water.is(Items.WATER_BUCKET) ? Items.BUCKET : ModItems.LARGE_BASIN.get());
    }

    /** 还倒得下一桶水：锅里没液体或者是水，而且没满 */
    private static boolean canPour(BigPotBlockEntity pot) {
        return !(pot.liquidMb() > 0 && !pot.liquid().isSame(Fluids.WATER))
                && pot.liquidMb() + BUCKET_MB <= BigPotBlockEntity.LIQUID_CAPACITY;
    }

    /** 倒一桶水：倒水声、溅水花（容器由调用的地方还） */
    private static void pour(BigPotBlockEntity pot, Level level, BlockPos pos, BlockState state, @Nullable Entity by) {
        pot.addLiquid(Fluids.WATER, BUCKET_MB);
        level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
        splash(level, pos, state, pot, 20);
        level.gameEvent(by, GameEvent.FLUID_PLACE, pos);
    }

    /**
     * 放进锅里以后还回来的容器：登记了合成剩余物的照还（瓶装调料还玻璃瓶、辣椒酱还碗）。
     * 大酱是一碗酱，但没登记合成剩余物——蘸酱菜是拿这碗酱直接合成的，酱碗就是菜的碗，登记了合成时会白多一个碗；
     * 下锅时在这里单独还碗（2026-11-06 用户：得莫利炖鱼的盐换成大酱，投入酱要返回碗）
     */
    private static ItemStack remainderOf(ItemStack held) {
        if (SeasoningBottleItem.isBottle(held)) {
            return SeasoningBottleItem.consumePortion(held);
        }
        if (held.hasCraftingRemainingItem()) {
            return held.getCraftingRemainingItem();
        }
        return held.is(CrciItems.SOY_PASTE.get()) ? new ItemStack(Items.BOWL) : ItemStack.EMPTY;
    }

    /** 瓶装调料至少要剩完整的一份；盐、碗装调料等一个物品本身就是一份 */
    private static boolean seasoningReady(ItemStack stack) {
        return !SeasoningBottleItem.isBottle(stack) || SeasoningBottleItem.hasPortion(stack);
    }

    /** 能当料下锅：哪条配方用得上，或者谜之炖菜认 */
    private static boolean usable(RecipeManager recipes, ItemStack stack) {
        return BigPotRecipes.usable(recipes, stack) || MysteryStew.accepts(stack);
    }

    private static boolean isSeasoning(RecipeManager recipes, ItemStack stack) {
        return BigPotRecipes.isSeasoning(recipes, stack) || MysteryStew.isSeasoning(stack);
    }

    /** 这一类（食材 / 调料）放满了：最多放用得最多的那条配方那么多个 */
    private static boolean full(BigPotBlockEntity pot, RecipeManager recipes, boolean seasoning) {
        return seasoning ? pot.seasoningCount() >= BigPotRecipes.maxSeasonings(recipes)
                : pot.ingredientCount() >= BigPotRecipes.maxIngredients(recipes);
    }

    /** 放一个料：压在栈顶、水声、溅水花（瓶子碗这些由调用的地方还） */
    private static void addOne(BigPotBlockEntity pot, ItemStack one, boolean seasoning, Level level, BlockPos pos,
                               BlockState state) {
        pot.push(one, seasoning);
        level.playSound(null, pos, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 0.5F, 1.0F);
        splash(level, pos, state, pot, 12);
    }

    // ---------------------------------------------------------------- 掉进锅里

    /**
     * 掉进锅里一个（2026-11-05 用户）：落在锅口的掉落物（{@link BigStoveBlock#stepOn}）、从上面往下送的漏斗和投掷器
     * （{@link BigStoveBlock#getContainer}）都走这里，只在服务端调。
     *
     * <ul>
     *   <li>收水桶、水盆（倒 1000 mB 水，还一个空桶 / 空盆）、食材和配方认的调料。瓶装调料一次扣完整的一份
     *       50 mB，不足一份不收；碗装调料一个算一份，返还一个碗；非调料且带合成剩余物的物品不收；</li>
     *   <li>别的规矩同右键：灶上得是揭开盖的锅、锅里不能是煮好的菜；放料得锅里先有水、没在煮、没放满；
     *       倒水煮着也能倒（同右键，多出来的水不影响凑配方）；</li>
     *   <li>都不出提示（用户：收不进就掉在灶上，放进去也不弹提示）。</li>
     * </ul>
     *
     * @param simulate 只问收不收，不动锅
     * @return 收不进返回 null；收了返回要还出来的东西（空桶、空盆、剩余调料瓶、空玻璃瓶或碗，没有则空），
     *         调用的地方把送来的那一堆减一个
     */
    @Nullable
    public static ItemStack dropIn(BigPotBlockEntity pot, BlockState state, Level level, BlockPos pos, ItemStack stack,
                                   @Nullable Entity by, boolean simulate) {
        if (stack.isEmpty() || state.getValue(BigStoveBlock.POT) != BigStoveBlock.Pot.POT || pot.hasResult()) {
            return null;
        }
        if (isWater(stack)) {
            if (pot.cooking() != null || !canPour(pot)) {
                return null;
            }
            if (!simulate) {
                pour(pot, level, pos, state, by);
            }
            return emptied(stack);
        }
        RecipeManager recipes = level.getRecipeManager();
        boolean seasoning = isSeasoning(recipes, stack);
        if (!usable(recipes, stack) || !seasoningReady(stack)
                || (!seasoning && stack.hasCraftingRemainingItem())) {
            return null;
        }
        if (pot.cooking() != null || pot.liquidMb() <= 0 || full(pot, recipes, seasoning)) {
            return null;
        }
        if (!simulate) {
            addOne(pot, stack.copyWithCount(1), seasoning, level, pos, state);
        }
        return remainderOf(stack);
    }

    /** 在指定位置生成一个掉落物（收不进锅的东西、没有相邻库存可收的返还物） */
    public static void popOut(Level level, double x, double y, double z, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        ItemEntity item = new ItemEntity(level, x, y, z, stack);
        item.setDefaultPickUpDelay();
        level.addFreshEntity(item);
    }

    /**
     * 投料后返还的容器：先尝试送进贴着大灶四格的库存，全都送不进才在锅口外沿蹦出来。
     * 不往上方找，避免剩余调料瓶又回到本次输入用的溜槽 / 漏斗，形成闭环。
     */
    public static void output(Level level, BlockPos pos, BlockState state, ItemStack stack) {
        if (stack.isEmpty() || level.isClientSide) {
            return;
        }
        ItemStack left = insertIntoNearby(level, pos, state, stack);
        if (!left.isEmpty()) {
            popOutSafe(level, pos, state, left);
        }
    }

    private static final Direction[] OUTPUT_SIDES = {
            Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.DOWN
    };

    private static ItemStack insertIntoNearby(Level level, BlockPos pos, BlockState state, ItemStack stack) {
        BlockPos origin = BigStoveBlock.origin(pos, state);
        Direction facing = state.getValue(BigStoveBlock.FACING);
        Set<BlockPos> stove = new HashSet<>();
        for (BigStoveBlock.Part part : BigStoveBlock.Part.values()) {
            int[] offset = part.offset(facing);
            stove.add(origin.offset(offset[0], 0, offset[1]));
        }
        Set<BlockPos> checked = new HashSet<>();
        for (BlockPos cell : stove) {
            for (Direction side : OUTPUT_SIDES) {
                BlockPos target = cell.relative(side);
                if (stove.contains(target) || !checked.add(target)) {
                    continue;
                }
                if (level.getBlockState(target).getBlock() instanceof BigStoveBlock) {
                    // 别的灶也不是容器，别把返还物投进下一口锅里
                    continue;
                }
                IItemHandler handler = itemHandlerAt(level, target, side.getOpposite());
                if (handler == null) {
                    continue;
                }
                for (int slot = 0; slot < handler.getSlots() && !stack.isEmpty(); slot++) {
                    stack = handler.insertItem(slot, stack, false);
                }
                if (stack.isEmpty()) {
                    return ItemStack.EMPTY;
                }
            }
        }
        return stack;
    }

    @Nullable
    private static IItemHandler itemHandlerAt(Level level, BlockPos pos, Direction side) {
        //? if <1.20.2 {
        net.minecraft.world.level.block.entity.BlockEntity blockEntity = level.getBlockEntity(pos);
        return blockEntity == null ? null
                : blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER, side).orElse(null);
        //?} else {
        /*return level.getCapability(Capabilities.ItemHandler.BLOCK, pos, side);
        *///?}
    }

    private static void popOutSafe(Level level, BlockPos pos, BlockState state, ItemStack stack) {
        Vec3 center = center(pos, state);
        Direction facing = state.getValue(BigStoveBlock.FACING);
        popOut(level, center.x + facing.getStepX() * 1.35, center.y + 0.2,
                center.z + facing.getStepZ() * 1.35, stack);
    }

    /**
     * 空手（主手）右键、灶上放着锅没盖盖时，先问这里：煮好了而且不用容器就空手盛；潜行空手从食材栈顶拿出一个
     * （开始煮了就不让拿）。返回 PASS 就照常走揭盖 / 拿锅。
     */
    public static InteractionResult onEmptyHand(BigPotBlockEntity pot, BlockState state, Level level, BlockPos pos,
                                                Player player) {
        if (pot.hasResult()) {
            if (pot.container().isEmpty()) {
                if (!level.isClientSide) {
                    serve(pot, level, pos, state, player);
                }
            } else if (!level.isClientSide) {
                tell(player, Component.translatable("message.northeast_china_delight.big_pot.serve_with",
                        pot.container().getHoverName()));
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
     * 锅里凑成哪条配方、盖上锅盖（state 里 pot=lid）才进入 3 秒反悔期，等满后开煮；开煮以后揭着盖照常煮，
     * 盖着按 lid_speedup 缩短时间；换了配方从头算。
     * 凑不上配方但有水有食材，盖上锅盖就煮谜之炖菜（cooking 记 {@link MysteryStew#ID}）。
     * {@code cooking} 不为空 = 已经开煮（锅里锁着），所以还没盖过盖时不设：料凑齐了没盖盖，照样能放能拿。
     */
    public static void serverTick(Level level, BlockPos pos, BlockState state, BigPotBlockEntity pot) {
        if (pot.hasResult()) {
            return;
        }
        boolean lid = state.getValue(BigStoveBlock.POT) == BigStoveBlock.Pot.LID;
        CookingPlan plan = plan(pot, level);
        if (plan == null) {
            pot.setCooking(null);
            return;
        }
        // 锅盖刚放下：先进入 3 秒反悔期，不推进度
        if (pot.cooking() == null) {
            if (!lid) {
                return;
            }
            pot.beginCooking(plan.id(), plan.total(), START_DELAY_TICKS, plan.result());
            return;
        }
        // 反悔期：锅盖揭开就取消；等满 3 秒才开始真正烹饪
        if (pot.startDelay() > 0) {
            if (!lid) {
                pot.setCooking(null);
                return;
            }
            pot.tickStartDelay();
            return;
        }
        // 旧存档没有预览菜名：补一次；数据包在中途换了配方（正常玩法不会发生）：从头按当前配方算
        pot.rememberDish(plan.result());
        if (!plan.id().equals(pot.cooking())) {
            pot.beginCooking(plan.id(), plan.total(), 0, plan.result());
            return;
        }
        // 进度按不盖锅盖的时长算；盖着锅盖每刻多走「不盖盖时长 / 盖盖时长」，中途盖上只让剩下的变快
        // （以前盖上就换成缩短后的总时长、煮过的刻数不变，进度一下放大 1.67 倍，2026-11-03 用户报）
        pot.setCookTotal(plan.total());
        pot.tickCooking(lid ? plan.total() / (float) plan.lidTotal() : 1.0F);
        // 煮着时的冒泡、白雾在客户端画（clientTick，看锅里同步过去的 cooking 和每秒一次的进度）
        if (pot.cookProgress() < plan.total() - 0.001F) {
            return;
        }
        float surface = BigPotBlockEntity.liquidSurfaceY(Math.max(pot.liquidMb(), BUCKET_MB));
        pot.finish(plan.result(), plan.container(), plan.experience());
        level.playSound(null, pos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 1.0F, 0.8F);
        // 煮好了：像骨粉催熟那样冒一阵绿色粒子
        if (level instanceof ServerLevel server) {
            Vec3 center = center(pos, state);
            server.sendParticles(ParticleTypes.HAPPY_VILLAGER, center.x, originY(pos, state) + surface / 16.0 + 0.2,
                    center.z, 24, 0.45, 0.2, 0.45, 0.0);
        }
    }

    /** 当前锅里能做什么：配方优先，凑不上就是谜之炖菜；都不能煮返回 null */
    @Nullable
    private static CookingPlan plan(BigPotBlockEntity pot, Level level) {
        BigPotRecipe recipe = BigPotRecipes.match(level.getRecipeManager(), pot.liquid(), pot.liquidMb(), pot.contents());
        if (recipe != null) {
            return new CookingPlan(recipe.id(), recipe.cookingTime(), recipe.lidCookingTime(),
                    recipe.result(), recipe.container(), recipe.experience());
        }
        if (MysteryStew.canCook(pot)) {
            return new CookingPlan(MysteryStew.ID, MysteryStew.COOKING_TIME, MysteryStew.LID_COOKING_TIME,
                    MysteryStew.cook(pot), new ItemStack(ModItems.LARGE_BASIN.get()), 0.0F);
        }
        return null;
    }

    /** 放上锅盖时由 BigStoveBlock 调：立刻显示「本次烹饪：XX」，并开始 3 秒反悔期 */
    public static void lidPlaced(BigPotBlockEntity pot, Level level, BlockPos pos, BlockState state, Player player) {
        if (level.isClientSide || pot.hasResult() || pot.cooking() != null) {
            return;
        }
        CookingPlan plan = plan(pot, level);
        if (plan == null) {
            return;
        }
        pot.beginCooking(plan.id(), plan.total(), START_DELAY_TICKS, plan.result());
        tell(player, Component.translatable("message.northeast_china_delight.big_pot.cooking",
                plan.result().getHoverName()));
    }

    /** 揭盖时先处理反悔：还在 3 秒等待里就取消这次烹饪；已经开始煮的只按常速继续 */
    public static void lidRemoved(BigPotBlockEntity pot, Level level, BlockPos pos, BlockState state) {
        if (pot.cooking() != null && pot.startDelay() > 0) {
            pot.setCooking(null);
        }
        lidLifted(level, pos, state, pot);
    }

    private record CookingPlan(ResourceLocation id, int total, int lidTotal, ItemStack result, ItemStack container,
                               float experience) {
    }

    /** 煮到几成开始在汤面上冒泡（2026-11-05 用户：烹饪进度超过 60%） */
    private static final float BUBBLE_FROM = 0.6F;
    /** 冒泡时每刻冒一个的几率 */
    private static final float BUBBLE_CHANCE = 0.5F;
    /** 煮好以后接着冒泡、越冒越少，过这么久就不冒了（刻，1 分钟；盛过一份就不冒，见 BigPotBlockEntity#finishedAt） */
    private static final int BUBBLE_FADE_TICKS = 1200;
    /** 一锅谜之炖菜每刻往上飘一个效果粒子的几率 */
    private static final float STEW_EFFECT_CHANCE = 0.35F;

    /**
     * 客户端每刻（灶上有锅时，点没点着都跑，见 {@link BigStoveBlock#getTicker}）。盖着盖看不见锅里，只在煮着时从锅盖边上偶尔漏一点白雾。
     * <ul>
     *   <li>煮着（点着火、cooking 同步过来了）：往上飘白雾；煮到六成以上汤面上冒泡（{@link #BUBBLE_FROM}，2026-11-05 用户）。
     *       进度每秒同步一次，所以最多晚一秒才开始冒；</li>
     *   <li>煮好了：接着冒泡，一分钟里越冒越少直到不冒（{@link #BUBBLE_FADE_TICKS}），盛过一份就不冒了（同日用户）；
     *       是一锅谜之炖菜就往上飘效果粒子（同日用户，{@link MysteryStewPotBlock#effectParticle}）。</li>
     * </ul>
     */
    public static void clientTick(Level level, BlockPos pos, BlockState state, BigPotBlockEntity pot) {
        RandomSource random = level.getRandom();
        Vec3 center = center(pos, state);
        double base = originY(pos, state);
        boolean lid = state.getValue(BigStoveBlock.POT) == BigStoveBlock.Pot.LID;
        if (pot.hasResult()) {
            if (lid) {
                return;
            }
            // 锅里的成品模型汤面同 1000 mB 的水面（block/big_pot/dish/<菜>，BigPotRenderer）
            double surface = base + BigPotBlockEntity.liquidSurfaceY(BUCKET_MB) / 16.0;
            long since = pot.finishedAt() < 0 ? -1 : level.getGameTime() - pot.finishedAt();
            if (since >= 0 && since < BUBBLE_FADE_TICKS
                    && random.nextFloat() < BUBBLE_CHANCE * (1.0F - since / (float) BUBBLE_FADE_TICKS)) {
                bubble(level, random, center, surface);
            }
            if (pot.result().is(ModItems.MYSTERY_STEW.get()) && random.nextFloat() < STEW_EFFECT_CHANCE) {
                double a = random.nextDouble() * Math.PI * 2;
                double r = Math.sqrt(random.nextDouble()) * 0.55;
                MysteryStewPotBlock.effectParticle(level, random, center.x + Math.cos(a) * r, surface + 0.05,
                        center.z + Math.sin(a) * r, false);
            }
            return;
        }
        if (pot.cooking() == null || !state.getValue(BigStoveBlock.LIT)) {
            return;
        }
        if (lid) {
            if (random.nextFloat() < 0.08F) {
                double a = random.nextDouble() * Math.PI * 2;
                level.addParticle(ParticleTypes.CLOUD, center.x + Math.cos(a) * 0.82, base + 1.1,
                        center.z + Math.sin(a) * 0.82, 0.0, 0.03, 0.0);
            }
            return;
        }
        double surface = base + (pot.liquidMb() > 0 ? BigPotBlockEntity.liquidSurfaceY(pot.liquidMb()) : 11.0F) / 16.0;
        if (pot.liquidMb() > 0 && pot.cookTotal() > 0 && pot.cookProgress() >= pot.cookTotal() * BUBBLE_FROM
                && random.nextFloat() < BUBBLE_CHANCE) {
            bubble(level, random, center, surface);
        }
        if (random.nextFloat() < 0.15F) {
            double a = random.nextDouble() * Math.PI * 2;
            double r = random.nextDouble() * 0.5;
            level.addParticle(ParticleTypes.CLOUD, center.x + Math.cos(a) * r, surface + 0.1,
                    center.z + Math.sin(a) * r, 0.0, 0.03 + random.nextDouble() * 0.02, 0.0);
        }
    }

    /** 汤面上冒一个泡（锅里汤面那一圈半径 10 像素，往里收一点） */
    private static void bubble(Level level, RandomSource random, Vec3 center, double surface) {
        double a = random.nextDouble() * Math.PI * 2;
        double r = Math.sqrt(random.nextDouble()) * 0.55;
        level.addParticle(ModParticles.POT_BUBBLE.get(), center.x + Math.cos(a) * r, surface + 0.01,
                center.z + Math.sin(a) * r, 0.0, 0.0, 0.0);
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

    /** 锅的正中（四格的交点），在灶面上一点（漏斗、投掷器送进来的还出空桶空盆也从这儿蹦出来，见 BigStoveBlock） */
    static Vec3 center(BlockPos pos, BlockState state) {
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
