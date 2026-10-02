"""把院子里的每种「建筑单体」单独生成 NBT，并渲染一张效果图。

产物：
  NBT  -> src/main/resources/data/northeast_china_delight/structure/parts/<id>.nbt
          （可以直接在游戏里用 结构方块 / /place template 摆出来）
  效果图 -> docs/结构渲染图/建筑单体/<中文名>.png

用法：H:\\miniconda3\\python.exe tools\\structure_gen\\build_parts.py
      … build_parts.py --no-render   # 只更新 NBT，不动 docs 里的效果图
"""

import argparse
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import nbt_io
import parts as P
from kit import Build
from mcblocks import B
from render_structure import render

ROOT = r"H:\IdeaProjects\northeast_china_delight1.21.1"
NBT_OUT = os.path.join(ROOT, "src", "main", "resources", "data", "northeast_china_delight", "structure", "parts")
IMG_OUT = os.path.join(ROOT, "docs", "结构渲染图", "建筑单体")

X, Z, Y = 2, 2, 1


def _base(b, w, d, block="minecraft:coarse_dirt"):
    b.fill(0, 0, 0, w - 1, 0, d - 1, block)
    b.fill(1, 1, 1, w - 2, 1, d - 2, "minecraft:coarse_dirt")


# ------------------------------------------------------------------ 正房们

def house_garden_brick():
    b = Build("house_garden_brick")
    _base(b, 32, 20)
    P.house(b, X, Z, X + 21, Z + 10, Y, 3, "minecraft:bricks", "tile_gray",
            door_x=X + 10, windows=((X + 2, X + 5), (X + 15, X + 18)),
            gable_chimney_x=X + 23, porch=True)
    return b


def house_mud_thatch():
    b = Build("house_mud_thatch")
    _base(b, 30, 20)
    P.house(b, X, Z, X + 19, Z + 9, Y, 3, "minecraft:packed_mud", "thatch",
            door_x=X + 9, windows=((X + 3, X + 6), (X + 12, X + 15)),
            gable_chimney_x=X + 21, porch=True)
    return b


def house_corn_metal():
    b = Build("house_corn_metal")
    _base(b, 32, 20)
    P.house(b, X, Z, X + 21, Z + 10, Y, 3, "minecraft:bricks", "metal",
            door_x=X + 10, windows=((X + 2, X + 5), (X + 15, X + 18)),
            gable_chimney_x=X + 23, porch=True)
    return b


def house_compound_five():
    b = Build("house_compound_five")
    _base(b, 36, 22)
    P.house(b, X + 3, Z, X + 28, Z + 12, Y, 4, "minecraft:bricks", "tile_gray",
            door_x=X + 15, windows=((X + 5, X + 8), (X + 10, X + 13),
                                    (X + 17, X + 20), (X + 23, X + 26)),
            gable_chimney_x=X + 30, porch=True)
    P.chimney(b, X + 1, Z - 1, 5, 9)
    return b


def house_metal_roof():
    b = Build("house_metal_roof")
    _base(b, 32, 20)
    P.house(b, X, Z, X + 21, Z + 10, Y, 3, "minecraft:bricks", "metal",
            door_x=X + 10, windows=((X + 2, X + 5), (X + 15, X + 18)),
            gable_chimney_x=X + 23, porch=True)
    return b


def house_log_cabin():
    b = Build("house_log_cabin")
    _base(b, 26, 20, "minecraft:podzol")
    x1, x2, z1, z2 = X, X + 15, Z, Z + 9
    b.fill(x1 - 1, 0, z1 - 1, x2 + 1, 0, z2 + 1, "minecraft:cobblestone")
    b.fill(x1, 1, z1, x2, 1, z2, "minecraft:spruce_planks")
    for y in range(2, 6):
        for x in range(x1, x2 + 1):
            b.set(x, y, z1, B("minecraft:spruce_log", axis="x"))
            b.set(x, y, z2, B("minecraft:spruce_log", axis="x"))
        for z in range(z1, z2 + 1):
            b.set(x1, y, z, B("minecraft:spruce_log", axis="z"))
            b.set(x2, y, z, B("minecraft:spruce_log", axis="z"))
    b.fill(x1 + 1, 2, z1 + 1, x2 - 1, 5, z2 - 1, "minecraft:air")
    b.door(x1 + 7, 2, z2)
    P.shutter_window(b, x1 + 2, x1 + 4, 3, z2, frame="minecraft:spruce_planks", height=2)
    P.shutter_window(b, x1 + 10, x1 + 12, 3, z2, frame="minecraft:spruce_planks", height=2)
    b.gable_roof(x1, x2, z1, z2, 6, "wood", overhang=2)
    P.chimney(b, x2 + 1, z1 + 1, 6, 10, block="minecraft:stone_bricks")
    for x in range(x1 - 2, x2 + 3):
        for z in (z1 - 3, z2 + 3):
            b.set(x, 6, z, B("minecraft:snow", layers=4))
    for z in ((z1 + z2) // 2, (z1 + z2) // 2 + 1):
        for x in range(x1 - 2, x2 + 3):
            b.set(x, 10, z, B("minecraft:snow", layers=3))
    return b


def wing_house():
    """厢房（单坡顶，朝院内）"""
    b = Build("wing_house")
    _base(b, 22, 22)
    b.fill(X, Y, Z, X + 8, Y, Z + 14, "minecraft:spruce_planks")
    b.shell(X, 2, Z, X + 8, 6, Z + 14, "minecraft:bricks")
    b.fill(X + 1, 2, Z + 1, X + 7, 6, Z + 13, "minecraft:air")
    P.shutter_window(b, X + 3, X + 5, 3, Z + 14, height=2)
    b.door(X + 2, 2, Z + 14)
    b.shed_roof(X - 1, X + 9, Z - 1, Z + 15, 7, 10, "tile_gray", axis="x")
    P.grain_bin(b, X + 2, Z + 3, 2)
    b.set(X + 3, 2, Z + 11, "minecraft:barrel")
    return b


# ------------------------------------------------------------------ 院里的建筑

def shed_kitchen():
    b = Build("shed_kitchen")
    _base(b, 14, 14)
    P.outdoor_kitchen(b, X + 3, Z + 3, Y, "thatch", loot_table="courtyard_kitchen")
    return b


def shed_barn():
    """牛棚：立柱 + 单坡草顶 + 草料 + 食槽"""
    b = Build("shed_barn")
    _base(b, 16, 14)
    for (dx, dz) in ((X, Z), (X, Z + 6), (X + 7, Z), (X + 7, Z + 6)):
        for yy in range(Y, Y + 3):
            b.set(dx, yy, dz, "minecraft:spruce_log")
    b.shed_roof(X - 1, X + 8, Z - 1, Z + 7, 4, 6, "thatch", axis="x")
    b.fill(X + 1, Y, Z + 2, X + 6, Y, Z + 5, "minecraft:hay_block")
    b.set(X + 1, Y + 1, Z + 1, "minecraft:cauldron")
    b.set(X + 6, Y + 1, Z + 1, "minecraft:water_cauldron")
    P.loot_barrel(b, X + 6, Y, Z + 6, "courtyard_granary")
    return b


def shed_storage():
    """仓房 / 车库：砖墙单坡顶"""
    b = Build("shed_storage")
    _base(b, 16, 14)
    b.fill(X, Y, Z, X + 6, Y, Z + 6, "minecraft:stone_bricks")
    b.shell(X, 2, Z, X + 6, 5, Z + 6, "minecraft:bricks")
    b.fill(X + 1, 2, Z + 1, X + 5, 5, Z + 5, "minecraft:air")
    b.door(X + 3, 2, Z + 6)
    b.shed_roof(X - 1, X + 7, Z - 1, Z + 7, 6, 8, "metal", axis="x")
    P.grain_bin(b, X + 1, Z + 2, 2)
    b.set(X + 5, 2, Z + 4, "minecraft:barrel")
    b.set(X + 1, 2, Z + 4, "minecraft:chest")
    return b


def corn_crib_part():
    b = Build("corn_crib")
    _base(b, 16, 16)
    P.corn_crib(b, X + 1, Z + 1, Y, w=7, d=7, style="wood")
    return b


def grain_bin_part():
    b = Build("grain_bin")
    _base(b, 10, 10)
    P.grain_bin(b, X + 1, Z + 1, Y)
    return b


def pig_pen_part():
    b = Build("pig_pen")
    _base(b, 15, 13)
    P.pig_pen(b, X, Z, X + 9, Z + 7, Y)
    return b


def chicken_coop_part():
    b = Build("chicken_coop")
    _base(b, 11, 11)
    P.chicken_coop(b, X + 1, Z + 1)
    return b


def dog_house_part():
    b = Build("dog_house")
    _base(b, 8, 8)
    P.dog_house(b, X + 1, Z + 1)
    return b


def toilet_part():
    b = Build("toilet")
    _base(b, 8, 8)
    P.toilet(b, X + 1, Z + 1)
    return b


def cellar_part():
    b = Build("cellar")
    _base(b, 8, 8)
    P.cellar(b, X + 1, Z + 1)
    return b


# ------------------------------------------------------------------ 院子家具

def well_part():
    b = Build("well")
    _base(b, 11, 11)
    P.well(b, X + 2, Z + 2)
    return b


def mill_part():
    b = Build("mill")
    _base(b, 11, 9)
    P.mill(b, X + 1, Z + 2)
    P.millstone(b, X + 5, Y, Z + 2)
    return b


def vat_row_part():
    b = Build("vat_row")
    _base(b, 10, 8)
    P.vat(b, X + 1, Y, Z + 2, layers=3)
    P.vat(b, X + 2, Y, Z + 2, layers=2)
    P.vat(b, X + 3, Y, Z + 2, layers=0)
    P.vat(b, X + 4, Y, Z + 2, layers=1)
    return b


def wood_stack_part():
    b = Build("wood_stack")
    _base(b, 13, 9)
    P.wood_stack(b, X + 1, Z + 1, Y, length=7, rows=4)
    P.wood_stack(b, X + 1, Z + 3, Y, length=5, rows=2, use="minecraft:spruce_log")
    return b


def hay_rick_part():
    b = Build("hay_rick")
    _base(b, 10, 10)
    P.hay_rick(b, X + 1, Z + 1, Y, size=4)
    return b


def screen_wall_part():
    b = Build("screen_wall")
    _base(b, 12, 8)
    P.screen_wall(b, X + 1, X + 4, Z + 3, Y)
    return b


def front_gate_part():
    b = Build("front_gate")
    _base(b, 14, 10)
    P.front_gate(b, X + 1, X + 4, Z + 4, Y, wall_block="minecraft:bricks")
    return b


def clothesline_part():
    b = Build("clothesline")
    _base(b, 14, 8)
    P.clothesline(b, X + 1, Z + 2, X + 9, Z + 2, y=2)
    return b


PARTS = [
    ("house_brick_tile", "正房·红砖灰瓦", house_garden_brick),
    ("house_mud_thatch", "正房·土坯茅草", house_mud_thatch),
    ("house_brick_metal", "正房·红砖彩钢瓦", house_metal_roof),
    ("house_five_bay", "正房·五间大户", house_compound_five),
    ("house_log_cabin", "正房·林区木刻楞", house_log_cabin),
    ("house_wing", "厢房·单坡顶", wing_house),
    ("shed_kitchen", "灶棚·带烤架", shed_kitchen),
    ("shed_barn", "牛棚", shed_barn),
    ("shed_storage", "仓房", shed_storage),
    ("corn_crib", "苞米楼子", corn_crib_part),
    ("grain_bin", "粮囤", grain_bin_part),
    ("pig_pen", "猪圈", pig_pen_part),
    ("chicken_coop", "鸡架", chicken_coop_part),
    ("dog_house", "狗窝", dog_house_part),
    ("toilet", "旱厕", toilet_part),
    ("cellar", "菜窖", cellar_part),
    ("well", "水井", well_part),
    ("mill", "石磨石碾", mill_part),
    ("vat_row", "大缸（酱缸）", vat_row_part),
    ("wood_stack", "柴火垛", wood_stack_part),
    ("hay_rick", "草垛", hay_rick_part),
    ("screen_wall", "影壁", screen_wall_part),
    ("front_gate", "门楼", front_gate_part),
    ("clothesline", "晾衣绳", clothesline_part),
]


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--no-render", action="store_true", help="只写 NBT，不重新渲染效果图")
    args = ap.parse_args()

    os.makedirs(NBT_OUT, exist_ok=True)
    if not args.no_render:
        os.makedirs(IMG_OUT, exist_ok=True)
    for pid, cn, fn in PARTS:
        build = fn()
        # 有地窖的单体（比如菜窖）会挖到 y<0，给它足够的垫土层，别让方块漏出模板
        lowest = min(k[1] for k in build.cells) if build.cells else 0
        tag = build.to_nbt(base_y=max(0, -lowest))
        nbt_path = os.path.join(NBT_OUT, pid + ".nbt")
        nbt_io.write_nbt(nbt_path, tag)
        if args.no_render:
            print("%-22s %-14s %2dx%2dx%-2d %5d blocks -> %s" % (
                pid, cn, tag["size"][0], tag["size"][1], tag["size"][2], len(tag["blocks"]), nbt_path))
        else:
            png_path = os.path.join(IMG_OUT, cn + ".png")
            render(nbt_path, png_path, scale=14)
            print("%-22s %-14s %2dx%2dx%-2d %5d blocks -> %s" % (
                pid, cn, tag["size"][0], tag["size"][1], tag["size"][2], len(tag["blocks"]), png_path))


if __name__ == "__main__":
    main()
