"""东北民居的「零件库」：把细节从设计里拆出来，谁都能复用。

坐标约定和 kit.py 一致：x 向东、z 向南、y 向上；y=1 是院子地面，房子从 y=1 的楼板起。
"""

from kit import Build, ROOFS, AIR
from mcblocks import B

# 约定：y=1 是院子地面（草方块），y=2 起才是「摆在地面上的东西」
# 约定：y=0 **就是院子地面（草方块）**，y=1 起才是「摆在地面上的东西」。
# 模板最底下那层就是院子地面，所以用结构方块/place template 摆在脚边时，
# 院子会正好替换脚下的地表方块，不会顶起一截土地基。
GROUND = 0
ON = 1
# 茅草/干草一律用惰性装饰块，不用干草捆（hay_block 一拆就是 9 个小麦，破坏生存平衡）
THATCH = "minecraft:yellow_wool"
NEST = "minecraft:brown_wool"
# 晾衣绳用农夫乐事的绳栅栏（细绳缠出来的那种，比绊线像样）
ROPE_FENCE = "farmersdelight:rope_fence"

# 各作物的最大生长阶段（大葱只有 4 个阶段，写 age=7 会读不出来 —— 日志里会报
# "Unable to read property: age with value: 7"，那一格就变成空气）
CROP_MAX_AGE = {
    "green_onion_crop": 3,
}


# ---------------------------------------------------------------- 房子本体

def plinth(b, x1, z1, x2, z2, y, block="minecraft:cobblestone", spread=1):
    """房基 + 散水：比墙外扩一格，房脚不悬空，也像东北房子的石脚。"""
    b.fill(x1 - spread, y - 1, z1 - spread, x2 + spread, y - 1, z2 + spread, block)
    b.fill(x1 - spread, y, z1 - spread, x2 + spread, y, z2 + spread, "minecraft:stone_bricks")
    b.fill(x1, y, z1, x2, y, z2, "minecraft:spruce_planks")


def chimney(b, x, z, y0, y1, block="minecraft:bricks", cap="minecraft:brick_slab"):
    """烟囱：柱身 + 压顶，立在山墙外侧。"""
    b.fill(x, y0, z, x, y1, z, block)
    b.set(x, y1 + 1, z, B(cap, type="top"))
    b.set(x, y1 + 1, z, block)


def shutter_window(b, x1, x2, y, z, frame="minecraft:spruce_planks", height=2):
    """带窗台、窗楣、两侧木窗板的大窗（东北老式木窗的样子）。"""
    for x in range(x1, x2 + 1):
        for yy in range(y, y + height):
            b.set(x, yy, z, "minecraft:glass_pane")
    for x in range(x1 - 1, x2 + 2):
        b.set(x, y - 1, z, frame)                      # 窗台
        b.set(x, y + height, z, frame)                 # 窗楣
    for x in (x1 - 1, x2 + 1):
        for yy in range(y, y + height):
            b.set(x, yy, z, frame)
    # 两侧开着的窗板
    b.set(x1 - 2, y, z + 1, B("minecraft:spruce_trapdoor", facing="south", half="top"))
    b.set(x1 - 2, y + 1, z + 1, B("minecraft:spruce_trapdoor", facing="south", half="top"))
    b.set(x2 + 2, y, z + 1, B("minecraft:spruce_trapdoor", facing="south", half="top"))
    b.set(x2 + 2, y + 1, z + 1, B("minecraft:spruce_trapdoor", facing="south", half="top"))


def door_with_frame(b, x, y, z, door="minecraft:oak_door", facing="south"):
    """门 + 门框 + 门楣（春联用红羊毛点一下，院里一眼就认得）。"""
    b.door(x, y, z, facing=facing, block=door)
    b.set(x - 2, y, z, "minecraft:bricks")
    b.set(x - 2, y + 1, z, "minecraft:bricks")
    b.set(x + 2, y, z, "minecraft:bricks")
    b.set(x + 2, y + 1, z, "minecraft:bricks")
    for xx in (x - 2, x + 2):
        b.set(xx, y + 1, z + 1, "minecraft:red_wool")   # 春联
    b.set(x, y + 2, z + 1, "minecraft:red_wool")


def vestibule(b, x, z, y, width=3, depth=2, wall="minecraft:bricks", style="tile_gray",
              door="minecraft:oak_door"):
    """门斗：正房门外那间挡风的小前室，东北房子几乎都有。"""
    x1, x2 = x - width // 2 - 1, x + width // 2 + 1
    z1, z2 = z, z + depth
    b.fill(x1, y - 1, z1, x2, y - 1, z2, "minecraft:stone_bricks")
    b.fill(x1, y, z1, x2, y, z2, "minecraft:spruce_planks")
    b.shell(x1, y + 1, z1, x2, y + 3, z2, wall)
    b.fill(x1 + 1, y + 1, z1, x2 - 1, y + 3, z2 - 1, AIR)
    b.door(x, y + 1, z2, block=door)
    for xx in range(x1, x2 + 1):
        # 楼梯的 facing = 升高的方向，所以北檐朝南、南檐朝北
        b.set(xx, y + 4, z1, B("minecraft:spruce_stairs", facing="south", half="bottom"))
        b.set(xx, y + 4, z2, B("minecraft:spruce_stairs", facing="north", half="bottom"))
    b.fill(x1, y + 5, z1, x2, y + 5, z2, "minecraft:spruce_planks")


def house(b, x1, z1, x2, z2, y, height, wall, style, door_x=None,
          windows=(), window_y=None, gable_chimney_x=None, ridge_chimney=None,
          porch=False, eaves=1, lit=True, interior=True, door_offset_z=0,
          shutters=True, plinth_block="minecraft:cobblestone", interior_style="three_bay",
          loot_house=None, loot_kitchen=None):
    """一栋带细节的正房：房基、墙、大窗、门（含门斗）、双坡顶、烟囱、室内。

    y 是**楼板那一层**：院子地面是 {@link GROUND}=1，所以正房一般传 y=2。
    interior_style: "three_bay" 三开间（默认）/ "basic" 简单内构 / "none" 不布置。
    """
    plinth(b, x1, z1, x2, z2, y, block=plinth_block)
    b.shell(x1, y + 1, z1, x2, y + height, z2, wall)
    b.fill(x1 + 1, y + 1, z1 + 1, x2 - 1, y + height, z2 - 1, AIR)
    # 四角抱柱
    for (cx, cz) in ((x1, z1), (x1, z2), (x2, z1), (x2, z2)):
        for yy in range(y + 1, y + height + 1):
            b.set(cx, yy, cz, "minecraft:spruce_log")
    wy = window_y if window_y is not None else y + 2
    for (a, c) in windows:
        if shutters:
            shutter_window(b, a, c, wy, z2, height=2)
        else:
            b.window_band(a, c, wy, z2, height=2)
        # 北墙也开两格小窗，屋里才有光
        b.set(a, wy, z1, "minecraft:glass_pane")
        b.set(a + 1, wy, z1, "minecraft:glass_pane")
    if door_x is not None:
        door_with_frame(b, door_x, y + 1, z2 + door_offset_z)
    b.gable_roof(x1, x2, z1, z2, y + height + 1, style, overhang=eaves)
    # 屋檐椽：檐口下面挂一排活板门，看着像木椽
    for x in range(x1 - eaves, x2 + 1 + eaves):
        b.set(x, y + height, z1 - eaves, B("minecraft:spruce_trapdoor", facing="north", half="top"))
        b.set(x, y + height, z2 + eaves, B("minecraft:spruce_trapdoor", facing="south", half="top"))
    if gable_chimney_x is not None:
        chimney(b, gable_chimney_x, z1 - 1, y + height, y + height + 4)
    if ridge_chimney is not None:
        rr = ROOFS[style]
        cx = ridge_chimney
        for yy in range(y + height + 1, y + height + 5):
            b.set(cx, yy, (z1 + z2) // 2, "minecraft:bricks")
        b.set(cx, y + height + 5, (z1 + z2) // 2, "minecraft:brick_slab")
    if porch:
        vestibule(b, door_x if door_x is not None else (x1 + x2) // 2, z2, y,
                  width=2, depth=2, wall=wall, style=style)
        # 门斗门口铺半砖台阶：位置在楼板那一层，1 格宽（和门一样宽）
        step_x = door_x if door_x is not None else (x1 + x2) // 2
        b.set(step_x, y, z2 + 3, B("minecraft:stone_brick_slab", type="bottom",
                                   waterlogged="false"))
    elif door_x is not None:
        # 门口铺半砖台阶，1 格宽，高度和楼板齐
        b.set(door_x, y, z2 + 1, B("minecraft:stone_brick_slab", type="bottom",
                                   waterlogged="false"))
    if lit:
        b.set((x1 + x2) // 2, y + height, (z1 + z2) // 2 + 1,
              B("minecraft:lantern", hanging="true"))
    if interior_style == "three_bay":
        interior_three_bay(b, x1, z1, x2, z2, y, wall=wall, ceiling_y=y + height,
                           loot_house=loot_house, loot_kitchen=loot_kitchen)
    elif interior_style == "basic":
        interior_basic(b, x1, z1, x2, z2, y, wall=wall)


def interior_basic(b, x1, z1, x2, z2, y, wall="minecraft:bricks"):
    """室内一套：南炕 + 灶台 + 桌 + 柜子 + 粮袋 + 酸菜缸 + 吊灯。"""
    b.kang(x1 + 1, z2 - 2, x2 - 1, z2 - 1, y + 1)
    b.stove(x1 + 1, y + 1, z1 + 1)
    b.table(x2 - 3, y + 1, z1 + 2)
    b.set(x2 - 1, y + 1, z1 + 1, "minecraft:chest")
    b.set(x2 - 2, y + 1, z1 + 1, "minecraft:barrel")
    vat(b, x2 - 1, y + 1, z1 + 2, layers=1)
    b.storage_sacks(x1 + 2, y + 1, z1 + 1, ["corn_seeds_sack", "soybean_sack"])
    b.set((x1 + x2) // 2, y + 4, (z1 + z2) // 2, B("minecraft:lantern", hanging="true"))


def interior_three_bay(b, x1, z1, x2, z2, y, wall="minecraft:bricks",
                       ceiling_y=None, loot_house=None, loot_kitchen=None):
    """三开间内构，三个房间各有各的用处：

    * **西屋 = 卧室**：南窗下一盘火炕（红砖 + 白地毯），炕上摞被褥（白羊毛）和枕头（红羊毛），
      北墙摆柜子，墙上挂灯。
    * **中间 = 灶房**：北墙一整条砖砌灶台，两口灶眼（明火 + 炖锅），旁边水缸、案板、粮袋、米桶，
      屋当中一张小桌，头顶吊灯。
    * **东屋 = 储物间**：箱子摞两层、成排的桶和菜筐、粮袋、腌菜大缸，
      角落里一个**菜窖口**（石砖框 + 两块木盖，这次是真有用途的活板门）。

    y 是楼板那一层；家具都在 y+1 起，房梁在 ceiling_y（默认 y+4）。
    """
    ix1, ix2 = x1 + 1, x2 - 1
    iz1, iz2 = z1 + 1, z2 - 1
    width = ix2 - ix1 + 1
    p1 = ix1 + width // 3
    p2 = ix2 - width // 3
    door_z = (iz1 + iz2) // 2
    floor = y + 1
    cy = ceiling_y or y + 4

    # ---- 两道隔断墙，各留一个两格高的门洞 ----
    for x in (p1, p2):
        for z in range(iz1, iz2 + 1):
            if z == door_z or z == door_z + 1:
                continue
            for yy in range(floor, floor + 2):
                b.set(x, yy, z, wall)

    # ================= 西屋：卧室 =================
    # 南窗下的火炕（红砖 + 白地毯），西屋整间都是炕
    b.kang(ix1, iz2 - 1, p1 - 1, iz2, floor)
    # 炕上摞被褥，炕尾放枕头
    quilt_end = max(ix1, p1 - 3)
    for x in range(ix1, quilt_end):
        b.set(x, floor + 1, iz2 - 1, "minecraft:white_wool")
    b.set(quilt_end, floor + 1, iz2 - 1, "minecraft:red_wool")
    # 炕梢的柜子、墙边的花盆
    b.set(ix1, floor, iz1, "minecraft:barrel")
    b.set(ix1 + 1, floor, iz1, "minecraft:chest")
    b.set(p1 - 1, floor, iz1 + 1, "minecraft:flower_pot")
    if loot_house:
        loot_chest(b, ix1 + 2, floor, iz1, loot_house, facing="south")
    b.set((ix1 + p1) // 2, cy - 1, door_z, B("minecraft:lantern", hanging="true"))

    # ================= 中屋：灶房 =================
    mid = (p1 + p2) // 2
    # 北墙一整条砖砌灶台，上面两口灶眼：一口明火、一口锅
    b.fill(mid - 1, floor, iz1, mid + 1, floor, iz1, "minecraft:bricks")
    b.set(mid - 1, floor + 1, iz1, "minecraft:campfire")
    b.set(mid, floor + 1, iz1, "minecraft:cauldron")
    b.set(mid + 1, floor + 1, iz1, B("minecraft:smoker", facing="south"))
    # 水缸、案板、粮袋、米桶
    b.set(mid + 1, floor, iz1 + 1, "minecraft:water_cauldron")
    b.set(mid - 1, floor, iz1 + 1, "minecraft:crafting_table")
    b.storage_sacks(mid - 1, floor, iz1 + 2, ["corn_seeds_sack"])
    b.set(mid, floor, iz1 + 2, "minecraft:barrel")
    if loot_kitchen:
        loot_barrel(b, mid + 1, floor, iz1 + 2, loot_kitchen)
    # 屋子当中一张细腿小桌（木腿 + 石板桌面，不再用活板门凑合）
    b.set(mid - 1, floor, door_z, "minecraft:oak_fence")
    b.set(mid - 1, floor + 1, door_z, B("minecraft:spruce_slab", type="bottom"))
    b.set(mid + 1, floor, door_z, "minecraft:oak_fence")
    b.set(mid + 1, floor + 1, door_z, B("minecraft:spruce_slab", type="bottom"))
    b.set(mid, cy - 1, door_z, B("minecraft:lantern", hanging="true"))

    # ================= 东屋：储物间 =================
    ex1, ex2 = p2 + 1, ix2
    # 菜窖口：石砖围一圈，两块木盖正好扣在上面
    hatch_x = ex1
    hatch_z = iz1 + 1
    b.set(hatch_x - 1, floor, hatch_z, "minecraft:stone_bricks")
    b.set(hatch_x + 2, floor, hatch_z, "minecraft:stone_bricks")
    b.set(hatch_x, floor, hatch_z - 1, "minecraft:stone_bricks")
    b.set(hatch_x + 1, floor, hatch_z - 1, "minecraft:stone_bricks")
    b.set(hatch_x + 2, floor, hatch_z - 1, "minecraft:stone_bricks")
    b.set(hatch_x - 1, floor, hatch_z + 1, "minecraft:stone_bricks")
    b.set(hatch_x + 2, floor, hatch_z + 1, "minecraft:stone_bricks")
    b.set(hatch_x, floor, hatch_z, B("minecraft:spruce_trapdoor", facing="north", half="bottom"))
    b.set(hatch_x + 1, floor, hatch_z, B("minecraft:spruce_trapdoor", facing="north", half="bottom"))
    # 箱子摞两层 + 成排的桶 + 菜筐 + 粮袋 + 腌菜大缸
    b.set(ex2, floor, iz1, "minecraft:chest")
    b.set(ex2, floor + 1, iz1, "minecraft:chest")
    b.set(ex2 - 1, floor, iz1, "minecraft:barrel")
    b.set(ex2, floor, iz1 + 1, "minecraft:barrel")
    b.set(ex2, floor, iz1 + 2, "northeast_china_delight:cucumber_crate")
    b.set(ex2 - 1, floor, iz1 + 2, "northeast_china_delight:corn_crate")
    b.storage_sacks(ex1, floor, iz2 - 1, ["soybean_sack", "red_bean_sack"])
    vat(b, ex2, floor, iz2 - 1, layers=1)
    b.set((ex1 + ex2) // 2, cy - 1, door_z, B("minecraft:lantern", hanging="true"))

    # ---- 房梁 ----
    for x in range(ix1, ix2 + 1, 3):
        b.set(x, cy, iz1, B("minecraft:spruce_log", axis="z"))
        b.set(x, cy, iz2, B("minecraft:spruce_log", axis="z"))


# ---------------------------------------------------------------- 院子里的东西

def stone_path(b, x1, z1, x2, z2, y=GROUND, block="minecraft:dirt_path"):
    """院内小路：铺在**地面那一层**的草径（dirt_path），不再高出草地一格。

    只替换自然地面（草/土/砂土/灰化土/雪），不覆盖已经放好的台阶、门槛、家具 ——
    否则门口那级台阶会被后面铺的小路盖掉。
    """
    groundish = {"minecraft:grass_block", "minecraft:dirt", "minecraft:coarse_dirt",
                 "minecraft:podzol", "minecraft:snow", "minecraft:snow_block",
                 "minecraft:sand", "minecraft:gravel", "minecraft:stone"}
    for x in range(min(x1, x2), max(x1, x2) + 1):
        for z in range(min(z1, z2), max(z1, z2) + 1):
            existing = b.cells.get((x, y, z))
            if existing is not None and existing[0] not in groundish:
                continue
            b.set(x, y, z, block)


def zhangzi_fence(b, x1, z1, x2, z2, y=ON, post="minecraft:oak_log",
                   rail="minecraft:oak_fence", every=4):
    """夹杖子：木杆栅栏 + 立柱 + 上下两道横杆（y 传 GROUND 就会从地面砌起）。"""
    b.fence_line(x1, z1, x2, z2, y, block=rail, post_every=every, post=post)
    b.fence_line(x1, z1, x2, z2, y + 1, block=rail, post_every=every, post=post)


def cellar(b, x, z, y=GROUND):
    """菜窖：地面上一圈石框 + 两块**平放**的木盖，下面是一间真能下去的小窖。

    窖室 3×3、深 3 格，梯子正对着其中一块盖子（爬得上来）、一个箱子、一筐菜，还有一盏灯。
    模板会在院子地面以下垫土，这间窖正好挖在这些垫土里 —— 放下去时会顶掉垫土，不会被埋。
    """
    lid = B("minecraft:spruce_trapdoor", facing="up", half="bottom",
            open="false", waterlogged="false")
    # 石砖框（北 4、南 4、东西各 1）
    for dx in range(-1, 3):
        b.set(x + dx, y, z - 1, "minecraft:stone_bricks")
        b.set(x + dx, y, z + 1, "minecraft:stone_bricks")
    b.set(x - 1, y, z, "minecraft:stone_bricks")
    b.set(x + 2, y, z, "minecraft:stone_bricks")
    # 两块盖子平铺在窖口上（能踩、能掀开）
    b.set(x, y, z, lid)
    b.set(x + 1, y, z, lid)
    # 下面的窖室：3×3、深 3
    for yy in range(y - 3, y):
        b.fill(x - 1, yy, z - 1, x + 1, yy, z + 1, AIR)
    b.fill(x - 1, y - 4, z - 1, x + 1, y - 4, z + 1, "minecraft:stone_bricks")
    # 梯子就放在盖板正下方（背面靠着东边的土墙），爬上去掀开盖子就能出来
    ladder(b, x + 1, z, y - 3, y - 1, facing="west")
    b.set(x - 1, y - 3, z + 1, "minecraft:chest")
    b.set(x - 1, y - 3, z - 1, "northeast_china_delight:sweet_potato_crate")
    b.set(x, y - 3, z - 1, "minecraft:barrel")
    b.set(x - 1, y - 3, z, B("minecraft:lantern", hanging="false"))


def pig_pen(b, x1, z1, x2, z2, y=ON, rail="minecraft:oak_fence"):
    """猪圈：围栏 + 泥地 + 小猪棚（栅栏柱撑羊毛顶）+ 两个食槽，不用羊毛当垫料。"""
    b.fill(x1, y - 1, z1, x2, y - 1, z2, "minecraft:coarse_dirt")
    b.fill(x1 + 1, y - 1, z1 + 1, x2 - 1, y - 1, z2 - 1, "minecraft:mud")
    b.fence_line(x1, z1, x2, z2, y, block=rail, post_every=3)
    b.fence_line(x1, z1, x2, z2, y + 1, block=rail, post_every=3)
    w = x2 - x1
    for (dx, dz) in ((x1 + 1, z1 + 1), (x1 + 1 + w // 2, z1 + 1),
                     (x1 + 1, z1 + 3), (x1 + 1 + w // 2, z1 + 3)):
        b.set(dx, y, dz, "minecraft:spruce_fence")
        b.set(dx, y + 1, dz, "minecraft:spruce_fence")
    b.fill(x1 + 1, y + 2, z1 + 1, x1 + 1 + w // 2, y + 2, z1 + 3, THATCH)
    b.shed_roof(x1 + 1, x1 + w // 2, z1, z1 + 3, y + 2, y + 4, "thatch", axis="x")
    b.set(x2 - 1, y + 1, z2 - 1, "minecraft:cauldron")
    b.set(x2 - 2, y + 1, z2 - 1, "minecraft:water_cauldron")
    # 猪棚下面吊一盏灯，晚上喂猪也看得见
    b.set(x1 + 1, y + 1, z1 + 2, B("minecraft:lantern", hanging="true"))


def chicken_coop(b, x, z, y=ON):
    """鸡架：立柱撑起的小木棚，南面敞开给鸡进出。

    里面只放一个**草窝**（棕色羊毛，就一格，不是塞满），外加一个饮水盆和一根歇脚横杆，
    这样一眼就能看出是养鸡用的，而不是一坨羊毛。
    """
    # 四根立柱
    for (dx, dz) in ((0, 0), (2, 0), (0, 2), (2, 2)):
        b.set(x + dx, y, z + dz, "minecraft:oak_log")
        b.set(x + dx, y + 1, z + dz, "minecraft:oak_log")
    # 三面矮栏杆（北、西、东），南面留门
    b.set(x + 1, y + 1, z, "minecraft:oak_fence")
    b.set(x, y + 1, z + 1, "minecraft:oak_fence")
    b.set(x + 2, y + 1, z + 1, "minecraft:oak_fence")
    # 顶棚（木板 + 外伸的台阶檐）
    b.fill(x, y + 2, z, x + 2, y + 2, z + 2, "minecraft:oak_planks")
    b.fill(x - 1, y + 3, z - 1, x + 3, y + 3, z + 3, "minecraft:spruce_slab")
    # 棚里：草窝一个、饮水盆一个
    b.set(x + 1, y, z + 1, NEST)
    b.set(x + 1, y, z, "minecraft:water_cauldron")
    # 门口的歇脚横杆：鸡晚上站上去睡觉，也把南面收成一个门洞
    b.set(x + 1, y + 1, z + 2, "minecraft:oak_fence")
    # 棚里挂一盏灯：晚上也能看清鸡窝
    b.set(x + 1, y + 1, z + 1, B("minecraft:lantern", hanging="true"))


def dog_house(b, x, z, y=ON):
    """狗窝：小木屋，门口朝南。"""
    b.fill(x, y, z, x + 1, y, z + 1, "minecraft:spruce_planks")
    b.shell(x, y + 1, z, x + 1, y + 2, z + 1, "minecraft:spruce_planks")
    b.fill(x, y + 1, z + 1, x + 1, y + 2, z + 1, AIR)
    b.fill(x - 1, y + 3, z - 1, x + 2, y + 3, z + 2, "minecraft:spruce_slab")
    # 门口挂一盏小灯
    b.set(x, y + 2, z, B("minecraft:lantern", hanging="true"))


def sauce_jars(b, x, z, y=1, count=3, liquid="northeast_china_delight:vat"):
    """酱缸 / 酸菜缸阵：模组的大缸排一排。"""
    for i in range(count):
        b.set(x + i, y, z, liquid)


def vat(b, x, y, z, layers=0, kind="NONE"):
    """一个模组大缸。layers 是水位（0~3 层，每层 1000 mB），缸里的水是干净清水。

    方块状态里的 water_level 和方块实体里的 water_mb 必须一致，不然液面会画错。
    """
    layers = max(0, min(3, layers))
    b.set(x, y, z, B("northeast_china_delight:vat", water_level=layers,
                     fermented="false", salted="false", progress=0),
          nbt={"id": "northeast_china_delight:vat", "water_mb": layers * 1000, "kind": kind})


def grill(b, x, y, z, facing="north"):
    """烤架（架在营火上的那种方块）。facing 决定营火的朝向。"""
    b.set(x, y, z, B("northeast_china_delight:grill_campfire", facing=facing, lit="true",
                     signal_fire="false", waterlogged="false", grill_progress=0))


def millstone(b, x, y, z):
    """机械动力的石磨。院子里的石磨用它，比原版切石机像样。"""
    b.set(x, y, z, "create:millstone")


def loot_chest(b, x, y, z, table, facing="south"):
    """带战利品表的箱子：开出来是随机的东西。"""
    b.set(x, y, z, B("minecraft:chest", facing=facing, type="single"),
          nbt={"id": "minecraft:chest",
               "LootTable": "northeast_china_delight:chests/" + table})


def loot_barrel(b, x, y, z, table):
    """带战利品表的木桶（桶没有朝向）。"""
    b.set(x, y, z, "minecraft:barrel",
          nbt={"id": "minecraft:barrel",
               "LootTable": "northeast_china_delight:chests/" + table})


# ---------------------------------------------------------------- 院里的树与歇脚处

def tree(b, x, z, ground=GROUND, height=7, trunk="minecraft:oak_log",
         leaf="minecraft:oak_leaves"):
    """院里的大树：主干 + 树冠（模板里手搓，不依赖世界生成的地物）。"""
    for i in range(height):
        b.set(x, ground + 1 + i, z, B(trunk, axis="y"))
    top = ground + height
    for dy in range(0, 4):
        r = 3 - abs(dy - 1)
        for dx in range(-r, r + 1):
            for dz in range(-r, r + 1):
                if abs(dx) == r and abs(dz) == r:
                    continue
                if dx == 0 and dz == 0 and dy < 3:
                    continue
                b.set(x + dx, top - 2 + dy, z + dz, leaf)


def bench(b, x, z, y=ON, length=3, axis="x"):
    """长凳：栅栏腿 + 木台阶座面。"""
    for i in range(length):
        px = x + (i if axis == "x" else 0)
        pz = z + (i if axis == "z" else 0)
        b.set(px, y - 1, pz, "minecraft:oak_fence")
        b.set(px, y, pz, B("minecraft:spruce_slab", type="bottom"))


def stone_table(b, x, z, y=ON):
    """石桌 + 四个石凳。"""
    b.set(x, y - 1, z, "minecraft:stone_bricks")
    b.set(x, y, z, B("minecraft:stone_slab", type="bottom"))
    for (dx, dz) in ((-2, 0), (2, 0), (0, -2), (0, 2)):
        b.set(x + dx, y - 1, z + dz, "minecraft:stone_bricks")
        b.set(x + dx, y, z + dz, B("minecraft:stone_slab", type="bottom"))


def outdoor_kitchen(b, x, z, y=ON, style="thatch", loot_table=None):
    """灶棚：**栅栏**当柱子撑起的小棚子（顶是羊毛），里面有烤架、锅、案板。"""
    for (dx, dz) in ((0, 0), (3, 0), (0, 3), (3, 3)):
        for yy in range(y, y + 2):
            b.set(x + dx, yy, z + dz, "minecraft:spruce_fence")
    b.shed_roof(x - 1, x + 4, z - 1, z + 4, y + 2, y + 4, style, axis="x")
    grill(b, x + 1, y, z + 1, facing="south")
    b.set(x + 2, y, z + 1, "minecraft:cauldron")
    b.set(x + 1, y, z + 2, "minecraft:crafting_table")
    if loot_table:
        loot_barrel(b, x + 2, y, z + 2, loot_table)
    # 灶台上方吊一盏灯，晚上做饭也看得见
    b.set(x + 2, y + 1, z + 1, B("minecraft:lantern", hanging="true"))


def grain_bin(b, x, z, y=ON):
    """粮囤：木框 + 干草 + 袋装粮。"""
    b.fill(x, y - 1, z, x + 2, y - 1, z + 2, "minecraft:spruce_planks")
    b.fill(x, y, z, x + 2, y, z + 2, "minecraft:spruce_planks")
    b.shell(x, y + 1, z, x + 2, y + 2, z + 2, "minecraft:oak_fence")
    b.set(x, y + 1, z, "northeast_china_delight:corn_seeds_sack")
    b.set(x + 2, y + 1, z, "northeast_china_delight:soybean_sack")
    b.set(x, y + 1, z + 2, "northeast_china_delight:peanut_sack")
    b.fill(x, y + 3, z, x + 2, y + 3, z + 2, "minecraft:spruce_slab")
    # 顶上吊一盏灯，晚上来舀粮也看得见
    b.set(x + 1, y + 2, z + 1, B("minecraft:lantern", hanging="true"))


def corn_crib(b, x, z, y=ON, w=6, d=6, post="minecraft:spruce_log",
              wall="minecraft:spruce_log", style="wood"):
    """苞米楼子：高脚、四面透风的玉米仓，南面挂一架梯子方便上去。"""
    for (dx, dz) in ((0, 0), (w - 1, 0), (0, d - 1), (w - 1, d - 1), (w // 2, d // 2)):
        for yy in range(y, y + 2):
            b.set(x + dx, yy, z + dz, post)
    b.fill(x - 1, y + 2, z - 1, x + w, y + 2, z + d, "minecraft:spruce_planks")
    b.shell(x, y + 3, z, x + w - 1, y + 5, z + d - 1, wall)
    # 四面留缝（透风）
    for yy in (y + 4,):
        for xx in range(x + 1, x + w - 1, 2):
            b.set(xx, yy, z, AIR)
            b.set(xx, yy, z + d - 1, AIR)
        for zz in range(z + 1, z + d - 1, 2):
            b.set(x, yy, zz, AIR)
            b.set(x + w - 1, yy, zz, AIR)
        # 楼板就是 y+2 那层平台，粮袋直接堆在楼板上
        b.set(x + 1, y + 3, z + 1, "northeast_china_delight:corn_seeds_sack")
        b.set(x + w - 2, y + 3, z + 1, "northeast_china_delight:corn_seeds_sack")
        b.set(x + 1, y + 3, z + d - 2, "northeast_china_delight:buckwheat_sack")
        b.gable_roof(x, x + w - 1, z, z + d - 1, y + 6, style, overhang=1)
        # 仓里吊一盏灯
        b.set(x + w // 2, y + 5, z + d // 2, B("minecraft:lantern", hanging="true"))
        # 梯子：贴在粮仓南面。最下一格**比院子地面高一格**（不是插进地里），
        # 下面先垫一根立柱当支撑（不然梯子没有靠面会被游戏弹掉）；
        # 南墙上开两格高的门洞，爬上去能直接进仓。
        b.set(x + 1, y, z + d - 1, post)
        b.set(x + 1, y + 1, z + d - 1, post)
        ladder(b, x + 1, z + d, y, y + 2, facing="south")
        b.set(x + 1, y + 3, z + d - 1, AIR)
        b.set(x + 1, y + 4, z + d - 1, AIR)


def hay_rick(b, x, z, y=ON, size=3):
    """草垛：用黄色羊毛堆，纯装饰，不产资源。"""
    for i in range(size):
        b.fill(x, y + i, z, x + size - 1 - i, y + i, z + size - 1 - i, THATCH)


def wood_stack(b, x, z, y=ON, length=6, rows=4, use="minecraft:stripped_oak_log"):
    """柴垛：整齐码木头。"""
    for r in range(rows):
        for i in range(max(1, length - r)):
            b.set(x + i, y + r, z, B(use, axis="x"))


def clothesline(b, x1, z1, x2, z2, y=ON + 1, post="minecraft:oak_fence"):
    """晾衣架：两根 3 格高的木杆 + 三道**绳栅栏**（农夫乐事）当晾衣绳。"""
    for (px, pz) in ((x1, z1), (x2, z2)):
        for yy in range(y - 1, y + 2):
            b.set(px, yy, pz, post)
    if z1 == z2:
        for yy in range(y - 1, y + 2):
            for x in range(min(x1, x2) + 1, max(x1, x2)):
                b.set(x, yy, z1, ROPE_FENCE)
    else:
        for yy in range(y - 1, y + 2):
            for z in range(min(z1, z2) + 1, max(z1, z2)):
                b.set(x1, yy, z, ROPE_FENCE)


def well(b, x, z, ground=GROUND, block="minecraft:cobblestone"):
    """水井：**井口与地面平齐** —— 石圈压在地面那一层，水就在正中间。

    ground 传地面那一层（默认 GROUND），所以井口和院子地面是一样高的。
    """
    for (dx, dz) in ((-1, -1), (0, -1), (1, -1), (-1, 0), (1, 0), (-1, 1), (0, 1), (1, 1)):
        b.set(x + dx, ground, z + dz, block)
    b.set(x, ground, z, "minecraft:water")
    for (dx, dz) in ((-1, -1), (-1, 1), (1, -1), (1, 1)):
        for yy in range(ground + 1, ground + 3):
            b.set(x + dx, yy, z + dz, "minecraft:oak_fence")
    b.fill(x - 1, ground + 3, z - 1, x + 1, ground + 3, z + 1, "minecraft:oak_planks")
    b.set(x, ground + 2, z - 1, "minecraft:oak_fence")
    b.set(x + 1, ground + 1, z + 1, "minecraft:cauldron")
    # 井架上放一盏灯
    b.set(x, ground + 4, z - 1, B("minecraft:lantern", hanging="false"))


def ladder(b, x, z, y_from, y_to, facing="north"):
    """梯子：从 y_from 一路搭到 y_to（含）。"""
    for yy in range(y_from, y_to + 1):
        b.set(x, yy, z, B("minecraft:ladder", facing=facing, waterlogged="false"))


def mill(b, x, z, y=ON):
    """石磨 + 石碾。"""
    b.set(x, y, z, B("minecraft:grindstone", facing="north", face="floor"))
    b.set(x + 2, y, z, "minecraft:stonecutter")
    b.set(x + 3, y, z, "minecraft:cauldron")


def drying_yard(b, x1, z1, x2, z2, y=GROUND):
    """晒场：碎石打底 + 石板心。"""
    b.fill(x1, y, z1, x2, y, z2, "minecraft:gravel")
    b.fill(x1 + 2, y, z1 + 2, x2 - 2, y, z2 - 2, "minecraft:stone_bricks")


def garden(b, x1, z1, x2, z2, ground=GROUND, crops=(), water_every=4, fence=True,
           rail="minecraft:oak_fence", entry="north"):
    """菜园：把草地翻成耕地 + 水沟 + 多种作物 + 夹杖子围栏，**留一道门**。

    ground 是地面那一层（默认 1），作物在它上面一格。
    """
    mid_z = (z1 + z2) // 2
    for z in range(z1, z2 + 1):
        crop = crops[(z - z1) % len(crops)]
        for x in range(x1, x2 + 1):
            # 浇水口只在菜园中间那一行放一格「含水的木活板门」：
            # 冷地方不会像水那样冻成冰，照样能给周围 9×9 的耕地保湿
            if water_every and (x - x1) % water_every == water_every - 1 and z == mid_z:
                b.set(x, ground, z, B("minecraft:spruce_trapdoor", facing="up", half="bottom",
                                      open="false", waterlogged="true"))
                continue
            b.set(x, ground, z, B("minecraft:farmland", moisture=7))
            age = CROP_MAX_AGE.get(crop, 7)
            b.set(x, ground + 1, z, B("northeast_china_delight:" + crop, age=age))
    if fence:
        for yy in (ground + 1, ground + 2):
            b.fence_line(x1 - 1, z1 - 1, x2 + 1, z2 + 1, yy, block=rail, post_every=5)
        # 入口：默认开在北面正中，两格宽、装栅栏门，不然菜园进不去
        mx = (x1 + x2) // 2
        if entry == "north":
            for x in (mx, mx + 1):
                for yy in (ground + 1, ground + 2):
                    b.set(x, yy, z1 - 1, AIR)
                b.set(x, ground + 1, z1 - 1, B("minecraft:oak_fence_gate", facing="south"))
        elif entry == "south":
            for x in (mx, mx + 1):
                for yy in (ground + 1, ground + 2):
                    b.set(x, yy, z2 + 1, AIR)
                b.set(x, ground + 1, z2 + 1, B("minecraft:oak_fence_gate", facing="north"))


def toilet(b, x, z, y=ON):
    """旱厕：小砖房，门朝南。"""
    b.fill(x, y - 1, z, x + 1, y - 1, z + 1, "minecraft:bricks")
    b.shell(x, y, z, x + 1, y + 2, z + 1, "minecraft:bricks")
    b.fill(x, y, z + 1, x + 1, y + 1, z + 1, AIR)
    b.fill(x - 1, y + 3, z - 1, x + 2, y + 3, z + 2, "minecraft:brick_slab")
    # 屋顶上一盏灯
    b.set(x, y + 4, z, B("minecraft:lantern", hanging="false"))


def side_door(b, x, y, z, facing="west", door="minecraft:oak_door",
              frame="minecraft:spruce_planks"):
    """在**南北走向**的墙上开一扇侧门（kit.door 只能开东西走向的墙）。

    门框沿着 z 方向排，门开好后外面记得再补一级半砖台阶，不然要跳着进门。
    """
    b.set(x, y, z, B(door, facing=facing, half="lower", hinge="left"))
    b.set(x, y + 1, z, B(door, facing=facing, half="upper", hinge="left"))
    for dz in (-1, 1):
        b.set(x, y, z + dz, frame)
        b.set(x, y + 1, z + dz, frame)
    b.set(x, y + 2, z, frame)


def front_gate(b, x1, x2, z, y=ON, post="minecraft:spruce_log", style="tile_gray",
               wall_block=None):
    """门楼：掏门洞 + 两根柱子（**落到地面层**）+ 小瓦顶 + 双开栅栏门。"""
    b.fill(x1, ON, z, x2, 5, z, AIR)          # 只掏地面之上，别把地面挖穿
    if wall_block:
        b.fill(x1 - 1, GROUND, z, x1 - 1, 4, z, wall_block)
        b.fill(x2 + 1, GROUND, z, x2 + 1, 4, z, wall_block)
    # 门槛用**半砖**（台阶），不是楼梯：走出去只抬半格
    for x in range(x1, x2 + 1):
        b.set(x, y, z, B("minecraft:stone_brick_slab", type="bottom", waterlogged="false"))
    for x in (x1, x2):
        for yy in range(y - 1, y + 4):
            b.set(x, yy, z, post)
    b.fill(x1 - 1, y + 4, z, x2 + 1, y + 4, z, "minecraft:spruce_planks")
    for x in range(x1 - 2, x2 + 3):
        b.set(x, y + 5, z - 1, B("minecraft:spruce_stairs", facing="south", half="bottom"))
        b.set(x, y + 6, z, "minecraft:spruce_slab")
    for x in range(x1 + 1, x2):
        b.set(x, y + 1, z, B("minecraft:oak_fence_gate", facing="south"))
        b.set(x, y + 2, z, B("minecraft:oak_fence_gate", facing="south"))
        b.set(x, y + 3, z, "minecraft:oak_fence")
    # 门楼两边各挂一盏灯，晚上回家看得见路
    b.set(x1, y + 5, z, B("minecraft:lantern", hanging="false"))
    b.set(x2, y + 5, z, B("minecraft:lantern", hanging="false"))


def screen_wall(b, x1, x2, z, y=ON, block="minecraft:bricks"):
    """影壁：进门挡视线的短墙，顶上压瓦。"""
    b.fill(x1, y, z, x2, y + 4, z, block)
    b.fill(x1 - 1, y + 5, z, x2 + 1, y + 5, z, "minecraft:brick_slab")
    b.fill(x1 + 1, y + 2, z - 1, x2 - 1, y + 3, z - 1, "minecraft:white_concrete")
