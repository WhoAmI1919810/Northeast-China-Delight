package com.gunmu.northeast_china_delight.item;

import com.gunmu.northeast_china_delight.util.DdStacks;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
//? if <1.20.2 {
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.event.entity.living.ShieldBlockEvent;
//?} else {
/*import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
*///?}

/**
 * 木锅盖：可盖在大灶的铁锅上（见 BigStoveBlock），也可拿在手里当盾牌。
 *
 * <p>右键举起来，和原版盾牌一样：举满 5 刻才算挡着、只挡正面来的攻击、斧子能把它打下来；
 * 但挡住的只有三成伤害（{@link #BLOCKED_FRACTION}），其余七成照样挨（{@link #onShieldBlock}）；
 * 穿齐「东北行头」四件套就和盾牌一样全挡（{@link NortheastOutfit#LID_BLOCKED_FRACTION}）。
 * 潜行点物品栏里的锅盖会进副手（{@link Equipable}，同原版盾牌）。</p>
 *
 * <p>耐久为 {@link #DURABILITY}；每挡一下掉 1 点，被斧子劈中（原版判定能打掉盾牌的那一下）
 * 掉最大耐久的六成（{@link #AXE_WEAR}），并且像盾牌一样 5 秒内举不起来。耐久、经验修补能附（附魔书上铁砧；
 * 同原版盾牌，附魔台上附不了），见 data/minecraft/tags/item/enchantable/durability.json。
 * 盖在锅上时大灶把这一个锅盖原样存着（耐久、附魔、改的名字都在），揭下来、拆灶还回来的就是它（BigPotBlockEntity#putLid）。</p>
 *
 * <p>格挡、被斧击落和破损音效都从服务端播放。原版盾牌音效由客户端实体事件播放，
 * 由 mixin/LivingEntityLidSoundMixin 拦掉（看 {@link #blocksWithLid}）；碎掉时原版的「物品损坏」声音连同碎片
 * 走的是另一个实体事件，扣耐久改用 {@link DdStacks#hurtAndBreakSilently} 不发它，碎片自己冒。</p>
 *
 * <p>对着大灶右键时先走大灶的交互（盖到锅上），大灶不接（PASS）才举起来，和拿盾牌点方块一样。
 * 「举着」的手里模型见 models/item/big_pot_lid_blocking.json，由客户端的 blocking 属性切换
 * （NortheastChinaDelight.ClientModEvents#onClientSetup）。</p>
 */
public class PotLidItem extends Item implements Equipable {

    /** 挡下的伤害比例：挡三成（穿齐「东北行头」四件套时全挡，见 {@link NortheastOutfit}） */
    public static final float BLOCKED_FRACTION = 0.3F;
    /** 锅盖耐久。 */
    public static final int DURABILITY = 64;
    /** 被斧子劈中掉的耐久：最大耐久的六成（四舍五入，64 → 38） */
    private static final float AXE_WEAR = 0.6F;
    /** 被斧子劈中后举不起来的时间（同原版盾牌被打掉，5 秒） */
    private static final int AXE_COOLDOWN = 100;
    /** 举着不放就一直举着（同原版盾牌） */
    private static final int USE_DURATION = 72000;
    /**
     * 挡住时的声音音调：同原版挖方块时一下下的 hit 声（木头 SoundType 音调 1 × 0.5），上下浮一点。
     * 音量用 1：原版挖方块那声只有 (1 + 1) / 8，打架时听不清
     */
    private static final float BLOCK_SOUND_PITCH = 0.5F;

    public PotLidItem(Properties properties) {
        super(properties);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BLOCK;
    }

    //? if <1.20.5 {
    @Override
    public int getUseDuration(ItemStack stack) {
        return USE_DURATION;
    }
    //?} else {
    /*@Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return USE_DURATION;
    }
    *///?}

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public EquipmentSlot getEquipmentSlot() {
        return EquipmentSlot.OFFHAND;
    }

    /** 让（Neo）Forge 把它当盾牌：LivingEntity#isBlocking 认的是这个，不是物品类 */
    //? if <1.20.2 {
    @Override
    public boolean canPerformAction(ItemStack stack, ToolAction toolAction) {
        return ToolActions.DEFAULT_SHIELD_ACTIONS.contains(toolAction);
    }
    //?} else {
    /*@Override
    public boolean canPerformAction(ItemStack stack, ItemAbility itemAbility) {
        return ItemAbilities.DEFAULT_SHIELD_ACTIONS.contains(itemAbility);
    }
    *///?}

    /**
     * 举着锅盖挡住了一下：挡下的伤害改成原本的三成（穿齐四件套时全挡、和盾牌一样），放木头挖掘中的声音，耐久自己扣（{@link #wear}）。
     * 原版扣盾牌耐久是按伤害算的（不到 3 点不扣、多了扣 1 + 伤害），这里不要，所以把原版那份关掉。
     * 没挡满，原版就当这一下没挡住（照样受伤、红一下、出受伤的声音），只是伤害少了一些；
     * 全挡（四件套）时原版照盾牌处理：不受伤、箭弹开、近身打的人被推开。
     */
    //? if <1.20.2 {
    public static void onShieldBlock(ShieldBlockEvent event) {
        LivingEntity blocker = event.getEntity();
        ItemStack lid = blocker.getUseItem();
        if (!lid.is(ModItems.BIG_POT_LID.get())) {
            return;
        }
        event.setBlockedDamage(event.getOriginalBlockedDamage() * blockedFraction(blocker));
        event.setShieldTakesDamage(false);
        playBlockSound(blocker);
        wear(blocker, lid, event.getDamageSource());
    }
    //?} else {
    /*public static void onShieldBlock(LivingShieldBlockEvent event) {
        LivingEntity blocker = event.getEntity();
        ItemStack lid = blocker.getUseItem();
        if (!event.getBlocked() || !lid.is(ModItems.BIG_POT_LID.get())) {
            return;
        }
        event.setBlockedDamage(event.getOriginalBlockedDamage() * blockedFraction(blocker));
        event.setShieldDamage(0.0F);
        playBlockSound(blocker);
        wear(blocker, lid, event.getDamageSource());
    }
    *///?}

    /**
     * 穿齐四件套时全挡，否则只挡三成。
     */
    private static float blockedFraction(LivingEntity blocker) {
        return NortheastOutfit.isComplete(blocker) ? NortheastOutfit.LID_BLOCKED_FRACTION : BLOCKED_FRACTION;
    }

    /**
     * 扣耐久：挡一下 1 点；被斧子劈中掉最大耐久的六成、5 秒举不起来。耐久附魔照样有几率免掉（斧子那六成也一样）、创造模式不扣。
     * 碎了就放下（同原版盾牌）。「斧子劈中」用原版判定能不能打掉盾牌的同一个条件：近身打（不是弹射物）、打的人 canDisableShield。
     *
     * <p>被斧子打掉放「门：晃动」，碎掉放「门：毁坏」（同一下又打掉又碎掉只放「门：毁坏」）。扣耐久用
     * {@link DdStacks#hurtAndBreakSilently}：原版碎掉时发的实体事件会让客户端放「物品损坏」的声音，这里不要，所以不发，
     * 物品碎片从服务端冒。</p>
     */
    private static void wear(LivingEntity blocker, ItemStack lid, DamageSource source) {
        if (blocker.level().isClientSide() || !(blocker instanceof Player player)) {
            return;
        }
        boolean axe = !source.is(DamageTypeTags.IS_PROJECTILE)
                && source.getDirectEntity() instanceof LivingEntity attacker && attacker.canDisableShield();
        ItemStack before = lid.copy();
        DdStacks.hurtAndBreakSilently(lid, axe ? Math.round(lid.getMaxDamage() * AXE_WEAR) : 1, player);
        if (lid.isEmpty()) {
            player.stopUsingItem();
            playDoorSound(player, SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR);
            if (player.level() instanceof ServerLevel server) {
                // 原版碎掉时客户端在手边冒的物品碎片（LivingEntity#breakItem），这里从服务端冒
                server.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, before), player.getX(),
                        player.getY(0.6), player.getZ(), 8, 0.2, 0.15, 0.2, 0.05);
            }
        } else if (axe) {
            player.getCooldowns().addCooldown(lid.getItem(), AXE_COOLDOWN);
            playDoorSound(player, SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR);
        }
    }

    /**
     * 格挡时播放一次木头命中音效，由服务端广播给附近玩家。
     */
    private static void playBlockSound(LivingEntity blocker) {
        if (blocker.level().isClientSide()) {
            return;
        }
        blocker.level().playSound(null, blocker.getX(), blocker.getY(), blocker.getZ(), SoundEvents.WOOD_HIT,
                blocker.getSoundSource(), 1.0F, BLOCK_SOUND_PITCH * (0.9F + blocker.getRandom().nextFloat() * 0.2F));
    }

    /**
     * 僵尸撞木门的声音（被斧子打掉：entity.zombie.attack_wooden_door「门：晃动」；碎掉：entity.zombie.break_wooden_door
     * 「门：毁坏」），音量、音调照原版僵尸破门（LevelRenderer#levelEvent 1019 / 1021：音量 2、音调 1 上下浮两成）。
     * Player#playSound 在服务端不发给自己（原版留给客户端自己放），这里是服务端算的，得发给所有人
     */
    private static void playDoorSound(LivingEntity entity, SoundEvent sound) {
        RandomSource random = entity.getRandom();
        entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), sound, entity.getSoundSource(),
                2.0F, (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F);
    }

    /**
     * 这个实体是不是举着木锅盖挡（客户端收到原版「挡住」「被斧子打掉」的实体事件时问，见 mixin/LivingEntityLidSoundMixin）。
     * 正在用的东西就是锅盖才算；没在用东西时（收到事件前已经同步成放下了）看手里有没有锅盖
     */
    public static boolean blocksWithLid(LivingEntity entity) {
        ItemStack using = entity.getUseItem();
        if (!using.isEmpty()) {
            return using.is(ModItems.BIG_POT_LID.get());
        }
        return entity.getMainHandItem().is(ModItems.BIG_POT_LID.get())
                || entity.getOffhandItem().is(ModItems.BIG_POT_LID.get());
    }
}
