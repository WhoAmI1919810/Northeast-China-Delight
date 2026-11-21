package com.gunmu.northeast_china_delight.entity;

/**
 * 记录生物开始使用物品（含油腻 / 爽口减速等事件延长）时的实际总时长，
 * 供第一人称进食动画与咀嚼粒子/音效读取，避免仍然按物品默认 32 刻计算导致前几秒卡顿无动画。
 */
public interface UseDurationData {
    int ncd$getActiveUseDuration();
}
