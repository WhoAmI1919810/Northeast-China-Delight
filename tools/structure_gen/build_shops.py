"""副食店（村庄建筑）：根据玩家给的两个 NBT 生成各生物群系版本，并接进村庄。

输入（玩家做的）：
    临时素材/relish_plains.nbt    平原副食店（12×7×7）
    临时素材/relish_savanna.nbt   热带草原副食店

输出：
    data/dongbei_delight/structure/village/relish_<biome>.nbt
        plains / savanna —— 直接复用玩家那两张（补上村庄要用的 jigsaw 接口）
        desert / snowy / taiga —— 按平原版换材质生成

注意 jigsaw 约定（照抄原版村庄房子）：
    * 门口一个 minecraft:building_entrance，pool 指向该群系的 streets
    * 屋里一个 minecraft:bottom，pool 指向该群系的 villagers（刷村民）
"""

import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import nbt_io

ROOT = r"H:\IdeaProjects\dongbei_delight1.21.1"
SRC = os.path.join(ROOT, "临时素材")
OUT = os.path.join(ROOT, "src", "main", "resources", "data", "dongbei_delight",
                   "structure", "village")

# 平原版用到的材质 → 各生物群系怎么换
REMAP = {
    "desert": {
        "minecraft:stone": "minecraft:sandstone",
        "minecraft:brick_stairs": "minecraft:sandstone_stairs",
        "minecraft:smooth_sandstone_stairs": "minecraft:sandstone_stairs",
        "minecraft:mushroom_stem": "minecraft:smooth_sandstone",
        "minecraft:grass_block": "minecraft:sand",
        "minecraft:oak_door": "minecraft:acacia_door",
        "minecraft:spruce_trapdoor": "minecraft:acacia_trapdoor",
        "minecraft:spruce_log": "minecraft:acacia_log",
        "minecraft:polished_diorite_slab": "minecraft:smooth_sandstone_slab",
        "minecraft:potted_spruce_sapling": "minecraft:potted_cactus",
    },
    "snowy": {
        "minecraft:smooth_sandstone": "minecraft:spruce_planks",
        "minecraft:stone": "minecraft:cobblestone",
        "minecraft:brick_stairs": "minecraft:spruce_stairs",
        "minecraft:smooth_sandstone_stairs": "minecraft:spruce_stairs",
        "minecraft:mushroom_stem": "minecraft:spruce_log",
        "minecraft:oak_door": "minecraft:spruce_door",
        "minecraft:polished_diorite_slab": "minecraft:spruce_slab",
    },
    "taiga": {
        "minecraft:smooth_sandstone": "minecraft:spruce_planks",
        "minecraft:stone": "minecraft:mossy_cobblestone",
        "minecraft:brick_stairs": "minecraft:spruce_stairs",
        "minecraft:smooth_sandstone_stairs": "minecraft:spruce_stairs",
        "minecraft:mushroom_stem": "minecraft:spruce_log",
        "minecraft:oak_door": "minecraft:spruce_door",
        "minecraft:polished_diorite_slab": "minecraft:spruce_slab",
    },
}

# 门外的台阶（jigsaw 的 final_state），按群系给个像样的
DOOR_STEP = {
    "plains": "minecraft:stone_brick_stairs[facing=north,half=bottom,shape=straight,waterlogged=false]",
    "savanna": "minecraft:acacia_stairs[facing=north,half=bottom,shape=straight,waterlogged=false]",
    "desert": "minecraft:sandstone_stairs[facing=north,half=bottom,shape=straight,waterlogged=false]",
    "snowy": "minecraft:stone_brick_stairs[facing=north,half=bottom,shape=straight,waterlogged=false]",
    "taiga": "minecraft:stone_brick_stairs[facing=north,half=bottom,shape=straight,waterlogged=false]",
}

# 门口第二格台阶（和 jigsaw 那格拼成 2 格宽，正好和双开门同宽）
STEP_BLOCK = {
    "plains": ("minecraft:stone_brick_stairs",
               {"facing": "north", "half": "bottom", "shape": "straight", "waterlogged": "false"}),
    "savanna": ("minecraft:acacia_stairs",
                {"facing": "north", "half": "bottom", "shape": "straight", "waterlogged": "false"}),
    "desert": ("minecraft:sandstone_stairs",
               {"facing": "north", "half": "bottom", "shape": "straight", "waterlogged": "false"}),
    "snowy": ("minecraft:stone_brick_stairs",
              {"facing": "north", "half": "bottom", "shape": "straight", "waterlogged": "false"}),
    "taiga": ("minecraft:stone_brick_stairs",
              {"facing": "north", "half": "bottom", "shape": "straight", "waterlogged": "false"}),
}

# 每个群系一个专属色：店里那两块桌布换成对应颜色，进村一眼认得出
CLOTH = {
    "plains": "create:red_table_cloth",
    "savanna": "create:orange_table_cloth",
    "desert": "create:yellow_table_cloth",
    "snowy": "create:light_blue_table_cloth",
    "taiga": "create:green_table_cloth",
}
FLOOR = {
    "plains": "minecraft:smooth_sandstone",
    "savanna": "minecraft:acacia_planks",
    "desert": "minecraft:smooth_sandstone",
    "snowy": "minecraft:spruce_planks",
    "taiga": "minecraft:spruce_planks",
}

# 换材质后「新方块没有属性」的那些：只有这些才需要把 Properties 丢掉，
# 其余（门/活板门/楼梯/台阶/原木）属性名完全一样，必须原样保留 ——
# 尤其是门的 half=upper/lower，丢了就会变成半截门。
NO_PROPS = {
    "minecraft:sand", "minecraft:sandstone", "minecraft:smooth_sandstone",
    "minecraft:cut_sandstone", "minecraft:spruce_planks", "minecraft:cobblestone",
    "minecraft:mossy_cobblestone", "minecraft:potted_cactus",
}

# 店里的货：箱装一种作物 + 袋装一种作物，按当地物产换。
# （平原/热带草原是你自己摆的，保持原样；下面三家是我生成的，按气候换货。）
GOODS = {
    # 沙漠：耐旱的辣椒 + 花生，箱装 + 袋装各一
    "desert": {
        "dongbei_delight:napa_cabbage_crate": "dongbei_delight:red_chili_crate",
        "dongbei_delight:sweet_potato_crate": "dongbei_delight:peanut_sack",
    },
    # 雪原：冬天窖里的青萝卜 + 荞麦
    "snowy": {
        "dongbei_delight:napa_cabbage_crate": "dongbei_delight:green_radish_crate",
        "dongbei_delight:sweet_potato_crate": "dongbei_delight:buckwheat_sack",
    },
    # 针叶林：大白菜 + 林子里采的榛蘑
    "taiga": {
        "dongbei_delight:sweet_potato_crate": "dongbei_delight:hazel_mushroom_sack",
    },
}

# 门口（前墙 z=5）与室内刷村民的位置
DOOR_X, DOOR_Z = 5, 5
OUTSIDE_Z = 6
VILLAGER_POS = (3, 0, 2)


def build(src_file, biome):
    src = os.path.join(SRC, src_file)
    tag = nbt_io.read_nbt(src)
    palette = tag["palette"]
    remap = REMAP.get(biome, {})
    if remap:
        for entry in palette:
            new_name = remap.get(entry["Name"], entry["Name"])
            entry["Name"] = new_name
            # 只有新方块压根没有这些属性时才清（比如草方块→沙子）
            if new_name in NO_PROPS:
                entry.pop("Properties", None)
    goods = GOODS.get(biome, {})
    if goods:
        for entry in palette:
            entry["Name"] = goods.get(entry["Name"], entry["Name"])
    cloth = CLOTH.get(biome)
    if cloth:
        for entry in palette:
            if entry["Name"].endswith("_table_cloth"):
                entry["Name"] = cloth
    # 空气要保留：结构放下去时空气会把原本的树/地形顶掉
    blocks = list(tag["blocks"])

    def add(name, props, pos, nbt):
        palette.append({"Name": name, "Properties": props})
        entry = {"pos": list(pos), "state": len(palette) - 1}
        if nbt:
            entry["nbt"] = nbt
        blocks.append(entry)

    # 门口：让村庄的街道能接上来
    add("minecraft:jigsaw", {"orientation": "south_up"}, (DOOR_X, 0, OUTSIDE_Z), {
        "id": "minecraft:jigsaw",
        "name": "minecraft:building_entrance",
        "target": "minecraft:building_entrance",
        "pool": "minecraft:village/%s/streets" % biome,
        "joint": "aligned",
        "final_state": DOOR_STEP[biome],
    })
    # 补上双开门另一半的台阶
    step_name, step_props = STEP_BLOCK[biome]
    add(step_name, step_props, (DOOR_X + 1, 0, OUTSIDE_Z), None)
    # 屋里：刷一个村民（他会去认领店里的大缸，变成副食商）
    add("minecraft:jigsaw", {"orientation": "up_north"}, VILLAGER_POS, {
        "id": "minecraft:jigsaw",
        "name": "minecraft:bottom",
        "target": "minecraft:bottom",
        "pool": "minecraft:village/%s/villagers" % biome,
        "joint": "rollable",
        "final_state": FLOOR[biome],
    })
    out = {
        "DataVersion": tag.get("DataVersion", 3955),
        "size": tag["size"],
        "palette": palette,
        "blocks": blocks,
        "entities": nbt_io.ListOf(nbt_io.TAG_COMPOUND, []),
    }
    path = os.path.join(OUT, "relish_%s.nbt" % biome)
    os.makedirs(OUT, exist_ok=True)
    nbt_io.write_nbt(path, out)
    return path, tag["size"], len(blocks)


def main():
    jobs = [
        ("relish_plains.nbt", "plains"),
        ("relish_savanna.nbt", "savanna"),
        ("relish_plains.nbt", "desert"),
        ("relish_plains.nbt", "snowy"),
        ("relish_plains.nbt", "taiga"),
    ]
    for src, biome in jobs:
        path, size, count = build(src, biome)
        print("%-8s <- %-20s %2dx%2dx%-2d %4d blocks -> %s"
              % (biome, src, size[0], size[1], size[2], count, path))


if __name__ == "__main__":
    main()
