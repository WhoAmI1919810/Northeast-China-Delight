"""方块名 + 渲染用配色。

渲染器的颜色是「手调的代表色」，不是贴图采样 —— 它的用途是比对建筑形体、
比例、体块关系，不追求像素级复刻贴图。
"""


def B(name, **props):
    if ":" not in name:
        name = "minecraft:" + name
    return (name, props)


# 渲染用的代表色：(顶面, 侧面) —— 侧面会自动再压暗一点
COLORS = {
    "minecraft:air": None,
    "minecraft:bricks": ((168, 96, 78), (150, 84, 68)),
    "minecraft:brick_stairs": ((168, 96, 78), (150, 84, 68)),
    "minecraft:brick_slab": ((168, 96, 78), (150, 84, 68)),
    "minecraft:stone_bricks": ((158, 158, 158), (140, 140, 140)),
    "minecraft:stone_brick_stairs": ((158, 158, 158), (140, 140, 140)),
    "minecraft:stone_brick_slab": ((158, 158, 158), (140, 140, 140)),
    "minecraft:cobblestone": ((128, 128, 128), (112, 112, 112)),
    "minecraft:cobblestone_stairs": ((128, 128, 128), (112, 112, 112)),
    "minecraft:cobblestone_slab": ((128, 128, 128), (112, 112, 112)),
    "minecraft:packed_mud": ((150, 122, 92), (134, 108, 80)),
    "minecraft:mud_bricks": ((140, 116, 96), (124, 102, 84)),
    "minecraft:mud_brick_stairs": ((140, 116, 96), (124, 102, 84)),
    "minecraft:mud_brick_slab": ((140, 116, 96), (124, 102, 84)),
    "minecraft:spruce_log": ((104, 78, 48), (74, 55, 34)),
    "minecraft:oak_log": ((122, 96, 56), (88, 68, 40)),
    "minecraft:oak_leaves": ((86, 138, 62), (74, 122, 54)),
    "minecraft:spruce_leaves": ((70, 118, 86), (60, 102, 74)),
    "minecraft:stripped_spruce_log": ((150, 118, 74), (132, 104, 66)),
    "minecraft:stripped_oak_log": ((172, 140, 90), (152, 124, 80)),
    "minecraft:spruce_planks": ((118, 88, 56), (104, 78, 50)),
    "minecraft:oak_planks": ((162, 130, 78), (144, 116, 70)),
    "minecraft:spruce_stairs": ((118, 88, 56), (104, 78, 50)),
    "minecraft:oak_stairs": ((162, 130, 78), (144, 116, 70)),
    "minecraft:spruce_slab": ((118, 88, 56), (104, 78, 50)),
    "minecraft:oak_slab": ((162, 130, 78), (144, 116, 70)),
    "minecraft:spruce_fence": ((118, 88, 56), (104, 78, 50)),
    "minecraft:oak_fence": ((162, 130, 78), (144, 116, 70)),
    "minecraft:spruce_trapdoor": ((118, 88, 56), (104, 78, 50)),
    "minecraft:glass": ((210, 230, 240), (196, 218, 230)),
    "minecraft:glass_pane": ((210, 230, 240), (196, 218, 230)),
    "minecraft:water": ((64, 118, 200), (56, 104, 180)),
    "minecraft:farmland": ((122, 88, 60), (104, 74, 50)),
    "minecraft:dirt": ((122, 88, 60), (104, 74, 50)),
    "minecraft:coarse_dirt": ((116, 84, 58), (100, 72, 50)),
    "minecraft:dirt_path": ((148, 120, 74), (132, 106, 66)),
    "minecraft:grass_block": ((124, 158, 84), (122, 88, 60)),
    "minecraft:podzol": ((104, 84, 48), (96, 78, 48)),
    "minecraft:snow_block": ((246, 250, 252), (236, 242, 248)),
    "minecraft:snow": ((246, 250, 252), (236, 242, 248)),
    "minecraft:hay_block": ((190, 160, 60), (170, 142, 52)),
    "minecraft:gravel": ((136, 130, 126), (120, 114, 110)),
    "minecraft:stone": ((130, 130, 130), (116, 116, 116)),
    "minecraft:oak_door": ((162, 130, 78), (144, 116, 70)),
    "minecraft:dark_oak_door": ((84, 62, 38), (72, 52, 32)),
    "minecraft:iron_door": ((190, 190, 190), (172, 172, 172)),
    "minecraft:lantern": ((250, 220, 130), (216, 190, 120)),
    "minecraft:torch": ((250, 220, 130), (216, 190, 120)),
    "minecraft:wall_torch": ((250, 220, 130), (216, 190, 120)),
    "minecraft:campfire": ((120, 90, 60), (110, 82, 54)),
    "minecraft:furnace": ((120, 120, 120), (104, 104, 104)),
    "minecraft:cauldron": ((96, 96, 96), (84, 84, 84)),
    "minecraft:water_cauldron": ((96, 96, 96), (84, 84, 84)),
    "minecraft:chest": ((150, 116, 62), (134, 104, 56)),
    "minecraft:barrel": ((140, 108, 62), (126, 96, 56)),
    "minecraft:crafting_table": ((150, 116, 62), (134, 104, 56)),
    "minecraft:grindstone": ((140, 140, 140), (126, 126, 126)),
    "minecraft:stonecutter": ((140, 136, 130), (126, 122, 116)),
    "minecraft:red_wool": ((180, 60, 60), (164, 54, 54)),
    "minecraft:white_wool": ((236, 236, 236), (222, 222, 222)),
    "minecraft:gray_concrete": ((128, 128, 128), (114, 114, 114)),
    "minecraft:polished_blackstone_stairs": ((70, 66, 70), (60, 56, 60)),
    "minecraft:polished_blackstone": ((70, 66, 70), (60, 56, 60)),
    "minecraft:deepslate_tile_stairs": ((72, 72, 76), (62, 62, 66)),
    "minecraft:deepslate_tiles": ((72, 72, 76), (62, 62, 66)),
    "minecraft:birch_planks": ((196, 176, 122), (176, 158, 110)),
    "minecraft:white_concrete": ((228, 230, 232), (214, 216, 218)),
    "minecraft:yellow_wool": ((233, 221, 106), (215, 202, 92)),
    "minecraft:brown_wool": ((114, 71, 40), (100, 62, 34)),
    "minecraft:yellow_terracotta": ((186, 133, 35), (168, 118, 30)),
    "minecraft:brown_terracotta": ((119, 86, 60), (105, 76, 52)),
    "minecraft:red_bed": ((161, 39, 34), (144, 34, 30)),
    "minecraft:flower_pot": ((150, 100, 66), (134, 88, 58)),
    "minecraft:smoker": ((104, 104, 104), (92, 92, 92)),
    "minecraft:ladder": ((152, 112, 64), (136, 100, 56)),
    "minecraft:white_carpet": ((236, 236, 236), (222, 222, 222)),
    "minecraft:mud": ((60, 55, 60), (52, 48, 52)),
    "minecraft:sand": ((219, 207, 163), (200, 189, 148)),
    "minecraft:sandstone": ((216, 203, 156), (198, 186, 142)),
    "minecraft:smooth_sandstone": ((222, 209, 163), (204, 192, 148)),
    "minecraft:cut_sandstone": ((218, 205, 158), (200, 188, 144)),
    "minecraft:sandstone_stairs": ((216, 203, 156), (198, 186, 142)),
    "minecraft:sandstone_slab": ((216, 203, 156), (198, 186, 142)),
    "minecraft:smooth_sandstone_stairs": ((222, 209, 163), (204, 192, 148)),
    "minecraft:smooth_sandstone_slab": ((222, 209, 163), (204, 192, 148)),
    "minecraft:mossy_cobblestone": ((110, 122, 96), (96, 108, 84)),
    "minecraft:acacia_log": ((104, 96, 60), (89, 82, 51)),
    "minecraft:acacia_planks": ((186, 100, 56), (168, 90, 50)),
    "minecraft:acacia_stairs": ((186, 100, 56), (168, 90, 50)),
    "minecraft:acacia_slab": ((186, 100, 56), (168, 90, 50)),
    "minecraft:acacia_door": ((186, 100, 56), (168, 90, 50)),
    "minecraft:acacia_trapdoor": ((186, 100, 56), (168, 90, 50)),
    "minecraft:spruce_door": ((118, 88, 56), (104, 78, 50)),
    "minecraft:potted_cactus": ((96, 140, 72), (150, 110, 66)),
    "minecraft:smoker_top": ((104, 104, 104), (92, 92, 92)),
}

# 模组方块（northeast_china_delight）—— 结构里用来做「粮食囤 / 酸菜缸 / 挂的菜」
MOD_COLORS = {
    "northeast_china_delight:vat": ((120, 112, 104), (104, 96, 90)),
    "northeast_china_delight:corn_seeds_sack": ((222, 206, 168), (206, 190, 154)),
    "northeast_china_delight:soybean_sack": ((214, 198, 162), (198, 182, 148)),
    "northeast_china_delight:red_bean_sack": ((206, 188, 156), (190, 172, 142)),
    "northeast_china_delight:buckwheat_sack": ((206, 192, 160), (190, 176, 146)),
    "northeast_china_delight:peanut_sack": ((214, 196, 156), (198, 180, 142)),
    "northeast_china_delight:napa_cabbage_crate": ((196, 188, 150), (170, 150, 108)),
    "northeast_china_delight:red_chili_crate": ((176, 78, 56), (156, 68, 50)),
    "northeast_china_delight:corn_crate": ((214, 184, 82), (186, 158, 70)),
    "northeast_china_delight:cucumber_crate": ((126, 158, 78), (140, 120, 74)),
}

for _name, _id in (
    ("napa_cabbage_crop", "northeast_china_delight:napa_cabbage_crop"),
    ("cucumber_crop", "northeast_china_delight:cucumber_crop"),
    ("green_radish_crop", "northeast_china_delight:green_radish_crop"),
    ("green_onion_crop", "northeast_china_delight:green_onion_crop"),
    ("eggplant_crop", "northeast_china_delight:eggplant_crop"),
    ("green_pepper_crop", "northeast_china_delight:green_pepper_crop"),
    ("red_chili_crop", "northeast_china_delight:red_chili_crop"),
    ("green_beans_crop", "northeast_china_delight:green_beans_crop"),
    ("corn_crop", "northeast_china_delight:corn_crop"),
    ("corn_stalk", "northeast_china_delight:corn_stalk"),
    ("soybean_crop", "northeast_china_delight:soybean_crop"),
    ("peanut_crop", "northeast_china_delight:peanut_crop"),
    ("red_bean_crop", "northeast_china_delight:red_bean_crop"),
    ("sweet_potato_crop", "northeast_china_delight:sweet_potato_crop"),
    ("buckwheat_crop", "northeast_china_delight:buckwheat_crop"),
):
    MOD_COLORS[_id] = ((112, 156, 72), (96, 136, 62))


def color_of(block):
    if block.startswith("northeast_china_delight:"):
        return MOD_COLORS.get(block, ((150, 150, 150), (135, 135, 135)))
    return COLORS.get(block, ((150, 150, 150), (135, 135, 135)))
