"""7 种东北小院的设计。

坐标约定（重要）：
    y=0 垫土；y=1 **院子地面＝草方块**；y=2 起才是「摆在地面上」的东西
    —— 房子楼板、院墙、栅栏、工作台、大缸、烤架、石磨、箱子全在 y=2 以上。

不用干草捆（hay_block）这类「一拆就是 9 个小麦」的方块，茅草/草垛一律用陶瓦。
正房默认三开间内构：中间灶房（灶台+锅+案板+水缸），东西两屋（火炕+柜子+灯）。
"""

from kit import Build
from mcblocks import B
import parts as P

GROUND = P.GROUND
ON = P.ON

CROPS_VEG = ["napa_cabbage_crop", "green_radish_crop", "cucumber_crop",
             "green_onion_crop", "red_chili_crop", "eggplant_crop"]
CROPS_MIX = ["green_onion_crop", "green_radish_crop", "green_pepper_crop", "napa_cabbage_crop"]


def _ground(b, w, d, block="minecraft:dirt"):
    """模板最底下那层就是院子地面（草地），所以不再垫土。

    （保留这个函数是为了不动每个设计里的调用；以前垫的那三层土会让院子
    用结构方块摆出来时顶起一截，所以去掉了。）
    """
    return


def _yard(b, w, d, block="minecraft:grass_block"):
    """院子地面：草方块。"""
    b.fill(1, GROUND, 1, w - 2, GROUND, d - 2, block)


def _brick_wall(b, w, d, h=3, block="minecraft:bricks", cap="minecraft:stone_brick_slab"):
    """砖院墙：从地面之上那一层起砌，顶上压瓦。"""
    b.shell(0, ON, 0, w - 1, ON + h - 1, d - 1, block)
    for x in range(w):
        b.set(x, ON + h, 0, B(cap, type="bottom"))
        b.set(x, ON + h, d - 1, B(cap, type="bottom"))
    for z in range(d):
        b.set(0, ON + h, z, B(cap, type="bottom"))
        b.set(w - 1, ON + h, z, B(cap, type="bottom"))


# --------------------------------------------------------------------------
# 1. 红砖正房 + 大菜园（38 × 37）
# --------------------------------------------------------------------------

def red_brick_garden():
    b = Build("红砖正房·菜园院")
    w, d = 38, 37
    _ground(b, w, d)
    _yard(b, w, d)
    _brick_wall(b, w, d, h=3)
    P.front_gate(b, 16, 21, d - 1, wall_block="minecraft:bricks")

    # 正房：24 × 12，三开间，门斗 + 山墙烟囱
    P.house(b, 6, 3, 29, 14, ON, 4, "minecraft:bricks", "tile_gray",
            door_x=17, windows=((9, 13), (22, 26)), gable_chimney_x=31,
            porch=True, loot_house="courtyard_house", loot_kitchen="courtyard_kitchen")

    P.stone_path(b, 17, 15, 20, 35)
    P.stone_path(b, 12, 22, 26, 23)

    # 菜园 16 × 14（草地翻成耕地）
    # 菜园往西缩一点，给正房门口留出通道（门口 3 格宽内不能有围栏/作物）
    P.garden(b, 3, 19, 14, 32, GROUND, CROPS_VEG, water_every=5)
    P.clothesline(b, 5, 17, 13, 17)
    P.vat(b, 20, ON, 16, layers=3)
    P.vat(b, 21, ON, 16, layers=0)
    P.vat(b, 22, ON, 16, layers=2)
    P.corn_crib(b, 26, 17, ON, w=7, d=7, style="wood")
    P.grain_bin(b, 33, 21, ON)
    P.wood_stack(b, 3, 34, ON, length=7, rows=4)
    P.wood_stack(b, 3, 35, ON, length=5, rows=2, use="minecraft:spruce_log")
    P.well(b, 22, 27)
    P.mill(b, 20, 34)
    P.millstone(b, 24, ON, 34)
    P.outdoor_kitchen(b, 26, 31, ON, "thatch", loot_table="courtyard_kitchen")
    P.cellar(b, 34, 32)
    P.dog_house(b, 34, 16)
    P.loot_barrel(b, 27, ON + 5, 18, "courtyard_granary")
    return b


# --------------------------------------------------------------------------
# 2. 土坯房 + 养殖院（36 × 35）
# --------------------------------------------------------------------------

def mud_livestock():
    b = Build("土坯房·养殖院")
    w, d = 36, 35
    _ground(b, w, d)
    _yard(b, w, d)
    P.zhangzi_fence(b, 0, 0, w - 1, d - 1, ON, post="minecraft:oak_log",
                    rail="minecraft:oak_fence", every=4)
    P.front_gate(b, 15, 20, d - 1)

    # 土坯房：20 × 10，茅草顶（陶瓦色）+ 门斗 + 三开间
    P.house(b, 5, 3, 24, 12, ON, 4, "minecraft:packed_mud", "thatch",
            door_x=14, windows=((8, 11), (17, 20)), gable_chimney_x=26,
            porch=True, loot_house="courtyard_house", loot_kitchen="courtyard_kitchen")

    P.stone_path(b, 16, 14, 18, 34)
    P.pig_pen(b, 25, 16, 34, 24, ON)
    P.chicken_coop(b, 5, 16)
    P.chicken_coop(b, 9, 16)
    # 牛棚：立柱 + 单坡草顶 + 食槽（垫料用陶瓦，不用干草捆）
    for (dx, dz) in ((4, 25), (4, 31), (11, 25), (11, 31)):
        for yy in range(ON, ON + 3):
            b.set(dx, yy, dz, "minecraft:spruce_log")
    b.shed_roof(3, 12, 24, 32, ON + 2, ON + 4, "thatch", axis="x")
    b.fill(5, GROUND, 27, 10, GROUND, 30, "minecraft:coarse_dirt")
    b.fill(5, ON, 27, 10, ON, 30, "minecraft:spruce_planks")
    b.set(5, ON, 26, "minecraft:cauldron")
    b.set(10, ON, 26, "minecraft:water_cauldron")
    P.wood_stack(b, 19, 25, ON, length=5, rows=3)
    # 菜园往东挪，别压在大门通道上
    P.garden(b, 23, 31, 30, 33, GROUND, CROPS_MIX, water_every=4)
    P.well(b, 30, 27)
    P.mill(b, 27, 31)
    P.millstone(b, 29, ON, 31)
    P.vat(b, 31, ON, 13, layers=3)
    P.vat(b, 32, ON, 13, layers=0)
    P.dog_house(b, 3, 21)
    P.wood_stack(b, 32, 16, ON, length=4, rows=3)
    P.outdoor_kitchen(b, 21, 19, ON, "thatch", loot_table="courtyard_kitchen")
    P.loot_barrel(b, 6, ON, 29, "courtyard_granary")
    return b


# --------------------------------------------------------------------------
# 3. 苞米院（38 × 39）
# --------------------------------------------------------------------------

def corn_crib_yard():
    b = Build("苞米院")
    w, d = 38, 39
    _ground(b, w, d)
    _yard(b, w, d)
    _brick_wall(b, w, d, h=3)
    P.front_gate(b, 16, 21, d - 1, wall_block="minecraft:bricks")

    P.house(b, 8, 3, 29, 14, ON, 4, "minecraft:bricks", "metal",
            door_x=18, windows=((11, 14), (23, 26)), gable_chimney_x=31,
            porch=True, loot_house="courtyard_house", loot_kitchen="courtyard_kitchen")
    P.stone_path(b, 18, 15, 21, 37)

    P.drying_yard(b, 8, 18, 20, 30, GROUND)
    # 玉米地：草地翻成耕地，两格高
    for z in range(18, 31):
        for x in range(23, 34):
            b.set(x, GROUND, z, B("minecraft:farmland", moisture=7))
            b.set(x, GROUND + 1, z, B("dongbei_delight:corn_crop", age=7))
            b.set(x, GROUND + 2, z, B("dongbei_delight:corn_stalk", stage=2))
    P.zhangzi_fence(b, 22, 17, 35, 31, ON, every=4)

    P.corn_crib(b, 3, 17, ON, w=7, d=7, style="wood")
    P.grain_bin(b, 3, 27, ON)
    P.grain_bin(b, 3, 32, ON)
    P.vat(b, 13, ON, 16, layers=3)
    P.vat(b, 14, ON, 16, layers=1)
    P.vat(b, 15, ON, 16, layers=0)
    P.well(b, 33, 34)
    P.mill(b, 29, 35)
    P.millstone(b, 31, ON, 35)
    P.outdoor_kitchen(b, 25, 33, ON, "thatch", loot_table="courtyard_kitchen")
    P.wood_stack(b, 8, 34, ON, length=6, rows=4)
    P.cellar(b, 22, 35)
    P.dog_house(b, 35, 16)
    P.loot_barrel(b, 5, ON + 5, 18, "courtyard_granary")
    P.loot_barrel(b, 5, ON + 4, 23, "courtyard_granary")
    return b


# --------------------------------------------------------------------------
# 4. 大户：四合院式（48 × 47）
# --------------------------------------------------------------------------

def big_compound():
    b = Build("大户·四合院式")
    w, d = 48, 47
    _ground(b, w, d)
    _yard(b, w, d)
    _brick_wall(b, w, d, h=4)
    P.front_gate(b, 21, 26, d - 1, wall_block="minecraft:bricks")

    # 正房五间：26 × 13
    P.house(b, 11, 3, 36, 15, ON, 5, "minecraft:bricks", "tile_gray",
            door_x=23, windows=((13, 16), (18, 21), (25, 28), (31, 34)),
            gable_chimney_x=38, porch=True,
            loot_house="courtyard_house", loot_kitchen="courtyard_kitchen")
    P.chimney(b, 9, 2, 6, 10)

    # 东西厢房（单坡顶朝院内）+ 室内
    b.fill(38, ON, 19, 46, ON, 33, "minecraft:spruce_planks")
    b.shell(38, ON + 1, 19, 46, ON + 5, 33, "minecraft:bricks")
    b.fill(39, ON + 1, 20, 45, ON + 5, 32, "minecraft:air")
    P.shutter_window(b, 41, 43, ON + 2, 33, height=2)
    b.door(40, ON + 1, 33)
    b.shed_roof(37, 47, 18, 34, ON + 6, ON + 9, "tile_gray", axis="x")
    P.grain_bin(b, 40, 22, ON + 1)
    P.grain_bin(b, 43, 27, ON + 1)
    b.set(41, ON + 1, 31, "minecraft:barrel")
    b.kang(40, 21, 45, 21, ON + 1)
    b.set(41, ON + 1, 24, "minecraft:chest")

    b.fill(1, ON, 19, 9, ON, 33, "minecraft:spruce_planks")
    b.shell(1, ON + 1, 19, 9, ON + 5, 33, "minecraft:bricks")
    b.fill(2, ON + 1, 20, 8, ON + 5, 32, "minecraft:air")
    b.door(8, ON + 1, 33)
    b.shed_roof(0, 10, 18, 34, ON + 6, ON + 9, "tile_gray", axis="x")
    b.kang(2, 21, 7, 21, ON + 1)
    b.stove(2, ON + 1, 31)
    P.wood_stack(b, 2, 25, ON + 1, length=5, rows=3)

    P.screen_wall(b, 22, 25, 41)
    P.stone_path(b, 22, 16, 25, 41)
    P.stone_path(b, 4, 35, 43, 36)

    P.well(b, 18, 31)
    P.mill(b, 27, 31)
    P.millstone(b, 25, ON, 31)
    P.millstone(b, 25, ON, 34)
    P.vat(b, 12, ON, 31, layers=3)
    P.vat(b, 13, ON, 31, layers=3)
    P.vat(b, 14, ON, 31, layers=0)
    P.vat(b, 15, ON, 31, layers=2)
    P.garden(b, 12, 35, 20, 44, GROUND, CROPS_VEG, water_every=5)
    P.wood_stack(b, 30, 39, ON, length=5, rows=3)
    P.wood_stack(b, 40, 39, ON, length=6, rows=4)
    P.cellar(b, 2, 39)
    P.toilet(b, 2, 43)
    P.dog_house(b, 45, 37)
    P.clothesline(b, 30, 21, 34, 21)
    P.pig_pen(b, 41, 39, 46, 44, ON)
    P.outdoor_kitchen(b, 30, 31, ON, "tile_gray", loot_table="courtyard_kitchen")
    P.chicken_coop(b, 44, 21)
    P.wood_stack(b, 30, 25, ON, length=4, rows=2, use="minecraft:spruce_log")
    b.set(26, ON, 23, "minecraft:stonecutter")
    P.loot_chest(b, 42, ON + 1, 28, "courtyard_granary", facing="west")
    P.loot_barrel(b, 44, ON + 1, 23, "courtyard_granary")
    P.loot_barrel(b, 4, ON + 1, 27, "courtyard_kitchen")
    return b


# --------------------------------------------------------------------------
# 5. 林区木刻楞（30 × 31）
# --------------------------------------------------------------------------

def log_cabin():
    b = Build("林区木刻楞")
    w, d = 30, 31
    _ground(b, w, d, block="minecraft:podzol")
    _yard(b, w, d)
    P.zhangzi_fence(b, 0, 0, w - 1, d - 1, ON, post="minecraft:spruce_log",
                    rail="minecraft:spruce_fence", every=4)
    P.front_gate(b, 13, 18, d - 1, post="minecraft:spruce_log")

    # 木刻楞：18 × 11 的横木墙，室内也做三开间
    x1, x2, z1, z2 = 4, 21, 3, 13
    b.fill(x1 - 1, GROUND, z1 - 1, x2 + 1, GROUND, z2 + 1, "minecraft:cobblestone")
    b.fill(x1, ON, z1, x2, ON, z2, "minecraft:spruce_planks")
    for y in range(ON + 1, ON + 5):
        for x in range(x1, x2 + 1):
            b.set(x, y, z1, B("minecraft:spruce_log", axis="x"))
            b.set(x, y, z2, B("minecraft:spruce_log", axis="x"))
        for z in range(z1, z2 + 1):
            b.set(x1, y, z, B("minecraft:spruce_log", axis="z"))
            b.set(x2, y, z, B("minecraft:spruce_log", axis="z"))
    b.fill(x1 + 1, ON + 1, z1 + 1, x2 - 1, ON + 4, z2 - 1, "minecraft:air")
    b.door(12, ON + 1, z2)
    P.shutter_window(b, 6, 8, ON + 2, z2, frame="minecraft:spruce_planks", height=2)
    P.shutter_window(b, 15, 17, ON + 2, z2, frame="minecraft:spruce_planks", height=2)
    b.gable_roof(x1, x2, z1, z2, ON + 5, "wood", overhang=2)
    P.chimney(b, 23, 4, ON + 5, ON + 9, block="minecraft:stone_bricks")
    for x in range(x1 - 2, x2 + 3):
        for z in (z1 - 3, z2 + 3):
            b.set(x, ON + 5, z, B("minecraft:snow", layers=4))
    for z in ((z1 + z2) // 2, (z1 + z2) // 2 + 1):
        for x in range(x1 - 2, x2 + 3):
            b.set(x, ON + 9, z, B("minecraft:snow", layers=3))
    P.interior_three_bay(b, x1, z1, x2, z2, ON, wall="minecraft:spruce_log",
                         ceiling_y=ON + 4, loot_house="courtyard_house",
                         loot_kitchen="courtyard_kitchen")

    P.wood_stack(b, 23, 16, ON, length=6, rows=4, use="minecraft:spruce_log")
    P.wood_stack(b, 23, 21, ON, length=5, rows=3, use="minecraft:stripped_spruce_log")
    P.well(b, 7, 18)
    # 菜园往西缩，给大门留通道
    P.garden(b, 3, 20, 11, 27, GROUND, CROPS_MIX, water_every=4)
    P.dog_house(b, 17, 21)
    P.millstone(b, 20, ON, 18)
    P.vat(b, 22, ON, 18, layers=2)
    P.outdoor_kitchen(b, 21, 26, ON, "thatch", loot_table="courtyard_kitchen")
    P.toilet(b, 25, 26)
    P.cellar(b, 17, 26)
    for (x, z, n) in ((20, 27, 6), (22, 29, 4), (28, 19, 5), (3, 14, 3), (14, 29, 4)):
        b.set(x, ON, z, B("minecraft:snow", layers=n))
    b.set(24, ON, 25, "minecraft:snow_block")
    b.set(25, ON, 24, "minecraft:snow_block")
    P.stone_path(b, 14, 14, 15, 29)
    return b


# --------------------------------------------------------------------------
# 6. 彩钢瓦红砖房（32 × 33）
# --------------------------------------------------------------------------

def metal_roof_house():
    b = Build("彩钢瓦红砖房")
    w, d = 32, 33
    _ground(b, w, d)
    _yard(b, w, d)
    _brick_wall(b, w, d, h=3, cap="minecraft:brick_slab")
    P.front_gate(b, 13, 18, d - 1, wall_block="minecraft:bricks")

    P.house(b, 5, 3, 26, 14, ON, 4, "minecraft:bricks", "metal",
            door_x=15, windows=((7, 10), (20, 23)), gable_chimney_x=28,
            porch=True, loot_house="courtyard_house", loot_kitchen="courtyard_kitchen")
    P.stone_path(b, 15, 15, 17, 32)
    P.stone_path(b, 6, 23, 28, 24)

    # 仓房
    b.fill(24, GROUND, 17, 30, GROUND, 23, "minecraft:stone_bricks")
    b.fill(24, ON, 17, 30, ON, 23, "minecraft:spruce_planks")
    b.shell(24, ON + 1, 17, 30, ON + 4, 23, "minecraft:bricks")
    b.fill(25, ON + 1, 18, 29, ON + 4, 22, "minecraft:air")
    b.door(27, ON + 1, 23)
    b.shed_roof(23, 31, 16, 24, ON + 5, ON + 7, "metal", axis="x")
    P.grain_bin(b, 25, 19, ON + 1)
    b.set(29, ON + 1, 21, "minecraft:barrel")
    P.loot_chest(b, 26, ON + 1, 21, "courtyard_granary", facing="south")

    P.garden(b, 3, 19, 11, 30, GROUND, ["cucumber_crop", "green_pepper_crop",
                                        "napa_cabbage_crop"], water_every=5)
    P.wood_stack(b, 4, 15, ON, length=7, rows=4)
    P.wood_stack(b, 4, 16, ON, length=5, rows=2, use="minecraft:spruce_log")
    P.pig_pen(b, 19, 26, 25, 31, ON)
    P.well(b, 21, 20)
    P.mill(b, 28, 29)
    P.millstone(b, 26, ON, 29)
    P.vat(b, 12, ON, 17, layers=3)
    P.vat(b, 13, ON, 17, layers=0)
    P.outdoor_kitchen(b, 18, 16, ON, "metal", loot_table="courtyard_kitchen")
    P.dog_house(b, 29, 16)
    return b


# --------------------------------------------------------------------------
# 7. 老树院：院中一棵大树，夏天在树下吃饭（34 × 34）
# --------------------------------------------------------------------------

def tree_yard():
    b = Build("老树院")
    w, d = 34, 34
    _ground(b, w, d)
    _yard(b, w, d)
    _brick_wall(b, w, d, h=3)
    P.front_gate(b, 14, 19, d - 1, wall_block="minecraft:bricks")

    # 正房：三开间，带门斗
    P.house(b, 5, 3, 26, 13, ON, 4, "minecraft:bricks", "tile_gray",
            door_x=15, windows=((7, 10), (20, 23)), gable_chimney_x=28,
            porch=True, loot_house="courtyard_house", loot_kitchen="courtyard_kitchen")
    P.stone_path(b, 15, 14, 17, 33)

    # 院里的大树 + 石桌石凳 + 长凳
    P.tree(b, 24, 25, GROUND, height=8, trunk="minecraft:oak_log",
           leaf="minecraft:oak_leaves")
    b.fill(20, GROUND, 21, 28, GROUND, 29, "minecraft:grass_block")
    P.stone_table(b, 21, 26, ON)
    P.bench(b, 24, 20, ON, length=3, axis="x")
    P.bench(b, 20, 24, ON, length=3, axis="z")

    # 菜园 + 大缸 + 石磨 + 柴垛 + 灶棚
    P.garden(b, 3, 18, 11, 27, GROUND, CROPS_VEG, water_every=5)
    P.vat(b, 5, ON, 15, layers=3)
    P.vat(b, 6, ON, 15, layers=0)
    P.vat(b, 7, ON, 15, layers=2)
    P.mill(b, 27, 30)
    P.millstone(b, 25, ON, 30)
    P.well(b, 21, 30)
    P.wood_stack(b, 4, 30, ON, length=6, rows=4)
    P.outdoor_kitchen(b, 19, 16, ON, "thatch", loot_table="courtyard_kitchen")
    P.dog_house(b, 30, 15)
    P.cellar(b, 30, 30)
    P.clothesline(b, 19, 22, 27, 22)
    P.loot_barrel(b, 30, ON, 19, "courtyard_granary")
    return b


DESIGNS = [
    ("dongbei_yard_garden", red_brick_garden),
    ("dongbei_yard_livestock", mud_livestock),
    ("dongbei_yard_corn", corn_crib_yard),
    ("dongbei_yard_compound", big_compound),
    ("dongbei_yard_cabin", log_cabin),
    ("dongbei_yard_metal", metal_roof_house),
    ("dongbei_yard_tree", tree_yard),
]
