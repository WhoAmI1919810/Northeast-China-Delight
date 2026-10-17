package com.gunmu.northeast_china_delight.advancement;

import com.gunmu.northeast_china_delight.NortheastChinaDelight;
import com.gunmu.northeast_china_delight.util.DdIds;
import net.minecraft.advancements.Advancement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * 玩法代码里直接发放的进度，以及把这些进度判给玩家的入口。
 *
 * <p>这些进度的判据都写成 {@code minecraft:impossible} —— 触发时机（敲中一下、末影人记仇、
 * 走进第 n 种院子）在原版都没有对应的触发器，所以在玩法代码里直接调 {@link #award} 发放。</p>
 *
 * <p>「开舂」的判据只认「用的是什么工具」，不认配方，以后加新的舂捣配方不用回来改这里；
 * 「挨户串门」的判据按院子种类拆成多条（{@link #VISIT_ALL_YARDS}，名字见 {@link #yardCriterion}），
 * 数据包往结构里加院子模板时，在 {@code visit_all_yards.json} 里补一条同名的判据即可。</p>
 *
 * <p>「快乐老家」「有臼了！」不走这里 —— 它们的判据是原版触发器（{@code location} / {@code placed_block}），
 * 整条写在数据包里。</p>
 *
 * <p>{@code ServerAdvancementManager} 取进度的入口在 1.20.2 改过名（{@code getAdvancement} → {@code get}，
 * 返回类型也从 {@code Advancement} 变成 {@code AdvancementHolder}），版本差异都收在本类里。</p>
 */
public final class ModAdvancements
{
    /** 开舂——用木槌舂捣一次 */
    public static final ResourceLocation POUND = id("pound");
    /** 你瞅啥？——戴大脸盆、手持木槌惹毛末影人 */
    public static final ResourceLocation STARE = id("stare");
    /** 大锤小用——用重锤舂捣一次（1.21+ 的重锤） */
    public static final ResourceLocation MACE_POUND = id("mace_pound");
    /** 挨户串门——所有种类的东北小院都进过一遍（判据一条对一个院子模板） */
    public static final ResourceLocation VISIT_ALL_YARDS = id("visit_all_yards");

    private ModAdvancements()
    {
    }

    private static ResourceLocation id(String path)
    {
        return DdIds.of(NortheastChinaDelight.MODID, path);
    }

    /**
     * 把 {@code advancementId} 上的 {@code criterion} 判据判给玩家。
     *
     * <p>进度没加载出来、或者进度里没有这条判据时安静地跳过：数据包可以把院子模板和
     * 判据一起改，玩法代码不用跟着改。</p>
     */
    public static void award(ServerPlayer player, ResourceLocation advancementId, String criterion)
    {
        if (player.getServer() == null) {
            return;
        }
        Advancement advancement = player.getServer().getAdvancements().getAdvancement(advancementId);
        if (advancement != null && advancement.getCriteria().containsKey(criterion)) {
            player.getAdvancements().award(advancement, criterion);
        }
    }

    /**
     * 「挨户串门」里，一个院子模板对应的判据名。
     *
     * <p>{@code northeast_china_delight:northeast_yard_garden} → {@code garden}：
     * 去掉命名空间和 {@code northeast_yard_} 前缀。{@link VISIT_ALL_YARDS} 的 JSON 里
     * 每条判据都是这么写的。</p>
     */
    public static String yardCriterion(String templateId)
    {
        int colon = templateId.indexOf(':');
        String path = colon < 0 ? templateId : templateId.substring(colon + 1);
        String prefix = "northeast_yard_";
        return path.startsWith(prefix) ? path.substring(prefix.length()) : path;
    }
}
