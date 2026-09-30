"""盖房子用的零件库。

坐标系：x 向东、z 向南、y 向上。院子一律「坐北朝南」——
正房在北边（z 小），院门开在南墙。

约定：y=0 是地基/找平层，y=1 是院子的地面，房子从 y=1 的楼板开始垒。
"""

import nbt_io
from mcblocks import B

AIR = ("minecraft:air", {})


def _norm(block):
    if isinstance(block, str):
        return (block if ":" in block else "minecraft:" + block), {}
    return block


ROOFS = {
    # 灰瓦（老式青瓦）：石砖坡 + 石砖脊
    "tile_gray": dict(
        stair="minecraft:stone_brick_stairs",
        under="minecraft:spruce_planks",
        ridge="minecraft:stone_bricks",
        eave="minecraft:cobblestone",
    ),
    # 深灰瓦：深板岩瓦
    "tile_dark": dict(
        stair="minecraft:polished_blackstone_stairs",
        under="minecraft:spruce_planks",
        ridge="minecraft:polished_blackstone",
        eave="minecraft:deepslate_tiles",
    ),
    # 彩钢瓦（参考照片里那种蓝灰铁皮顶）
    "metal": dict(
        stair="minecraft:deepslate_tile_stairs",
        under="minecraft:gray_concrete",
        ridge="minecraft:deepslate_tiles",
        eave="minecraft:gray_concrete",
    ),
    # 茅草顶（土坯房用）
    "thatch": dict(
        stair="minecraft:oak_stairs",
        under="minecraft:yellow_wool",
        ridge="minecraft:yellow_wool",
        eave="minecraft:oak_planks",
        solid=True,
    ),
    # 木瓦顶（林区木刻楞）
    "wood": dict(
        stair="minecraft:spruce_stairs",
        under="minecraft:spruce_planks",
        ridge="minecraft:spruce_planks",
        eave="minecraft:spruce_planks",
        solid=True,
    ),
}


class Build:
    def __init__(self, name, note=""):
        self.name = name
        self.note = note
        self.cells = {}

    # ---------- 基础写入 ----------

    def set(self, x, y, z, block, nbt=None):
        """放一个方块。nbt 是方块实体数据（比如箱子的 LootTable、大缸的水量）。"""
        name, props = _norm(block)
        key = (int(x), int(y), int(z))
        if name == "minecraft:air":
            self.cells.pop(key, None)
            return
        self.cells[key] = (name, dict(props), dict(nbt) if nbt else None)

    def fill(self, x1, y1, z1, x2, y2, z2, block):
        for x in range(min(x1, x2), max(x1, x2) + 1):
            for y in range(min(y1, y2), max(y1, y2) + 1):
                for z in range(min(z1, z2), max(z1, z2) + 1):
                    self.set(x, y, z, block)

    def shell(self, x1, y1, z1, x2, y2, z2, block, skip_bottom=False, skip_top=False):
        """只砌四面墙（中空）。"""
        for x in range(x1, x2 + 1):
            for z in range(z1, z2 + 1):
                edge = x in (x1, x2) or z in (z1, z2)
                if not edge:
                    continue
                for y in range(y1, y2 + 1):
                    if skip_bottom and y == y1:
                        continue
                    if skip_top and y == y2:
                        continue
                    self.set(x, y, z, block)

    def hollow(self, x1, y1, z1, x2, y2, z2):
        self.fill(x1, y1, z1, x2, y2, z2, AIR)

    def slab_row(self, x1, z1, x2, z2, y, block):
        self.fill(x1, y, z1, x2, y, z2, block)

    def fence_line(self, x1, z1, x2, z2, y, block="minecraft:oak_fence", post_every=0,
                   post="minecraft:oak_log"):
        """围一圈栅栏（矩形边线）。post_every>0 时按间隔插柱子。"""
        points = []
        for x in range(min(x1, x2), max(x1, x2) + 1):
            points.append((x, z1))
            if z2 != z1:
                points.append((x, z2))
        for z in range(min(z1, z2) + 1, max(z1, z2)):
            points.append((x1, z))
            if x2 != x1:
                points.append((x2, z))
        for i, (x, z) in enumerate(points):
            if post_every and i % post_every == 0:
                self.set(x, y, z, post)
                self.set(x, y + 1, z, post)
            else:
                self.set(x, y, z, block)

    # ---------- 屋顶 ----------

    def gable_roof(self, x1, x2, z1, z2, y, style, overhang=1, rise=None, pitch=0.45):
        """双坡屋顶：屋脊东西向，南北两坡。

        z1..z2 是墙的外沿，overhang 是出檐格数。
        pitch 是坡度：脊高 = 单坡水平长度 × pitch（东北民房是缓坡，别做 45°）。
        rise 可以直接指定脊高（格数），给了就以它为准。
        """
        cfg = ROOFS[style]
        zlo = z1 - overhang
        zhi = z2 + overhang
        half = max(1.0, (zhi - zlo) / 2.0)
        if rise is None:
            rise = max(1, int(round(half * pitch)))
        for z in range(zlo, zhi + 1):
            d = min(z - zlo, zhi - z)
            step = int(round(rise * d / half))
            top = y + step
            # 楼梯的 facing = 台阶升高的方向：南坡朝北升，北坡朝南升
            facing = "north" if z > (zlo + zhi) / 2.0 else "south"
            is_ridge = step >= rise
            solid = cfg.get("solid", False)
            edge = (d == 0)
            for x in range(x1 - overhang, x2 + 1 + overhang):
                # 坡面下面垫实，避免屋面透光
                for yy in range(y, top):
                    self.set(x, yy, z, cfg["under"])
                if solid and not edge:
                    self.set(x, top, z, cfg["ridge"])
                elif is_ridge:
                    self.set(x, top, z, cfg["ridge"])
                else:
                    self.set(x, top, z, B(cfg["stair"], facing=facing, half="bottom"))
            # 山墙端头补一块檐口
            self.set(x1 - overhang - 1, top, z, cfg["eave"])
            self.set(x2 + 1 + overhang, top, z, cfg["eave"])

    def shed_roof(self, x1, x2, z1, z2, y_low, y_high, style, axis="z"):
        """单坡屋顶（厢房 / 披屋）：从 high 端斜到 low 端。"""
        cfg = ROOFS[style]
        if axis == "z":
            steps = abs(y_high - y_low)
            for i, z in enumerate(range(min(z1, z2), max(z1, z2) + 1)):
                t = i / max(1, (max(z1, z2) - min(z1, z2)))
                top = int(round(y_high + (y_low - y_high) * t))
                facing = "north" if z > (z1 + z2) / 2 else "south"
                for x in range(x1, x2 + 1):
                    self.set(x, top, z, B(cfg["stair"], facing=facing, half="bottom"))
                    for yy in range(min(y_low, y_high), top):
                        self.set(x, yy, z, cfg["under"])
        else:
            for i, x in enumerate(range(min(x1, x2), max(x1, x2) + 1)):
                t = i / max(1, (max(x1, x2) - min(x1, x2)))
                top = int(round(y_high + (y_low - y_high) * t))
                facing = "east" if x < (x1 + x2) / 2 else "west"
                for z in range(z1, z2 + 1):
                    self.set(x, top, z, B(cfg["stair"], facing=facing, half="bottom"))
                    for yy in range(min(y_low, y_high), top):
                        self.set(x, yy, z, cfg["under"])

    def flat_roof(self, x1, x2, z1, z2, y, block, rim="minecraft:cobblestone"):
        self.fill(x1, y, z1, x2, y, z2, block)
        for x in range(x1, x2 + 1):
            for z in (z1, z2):
                self.set(x, y + 1, z, rim)
        for z in range(z1, z2 + 1):
            for x in (x1, x2):
                self.set(x, y + 1, z, rim)

    # ---------- 门窗 ----------

    def window_band(self, x1, x2, y, z, frame="minecraft:spruce_planks",
                    glass="minecraft:glass_pane", height=2, sill=True):
        """在南/北墙（固定 z）上开一排窗：木框 + 玻璃。"""
        for x in range(x1, x2 + 1):
            for yy in range(y, y + height):
                self.set(x, yy, z, glass)
        for x in range(x1 - 1, x2 + 2):
            self.set(x, y - 1, z, frame)
            self.set(x, y + height, z, frame)
        for x in (x1 - 1, x2 + 1):
            for yy in range(y, y + height):
                self.set(x, yy, z, frame)
        if sill:
            for x in range(x1 - 1, x2 + 2):
                self.set(x, y - 2, z, "minecraft:brick_stairs" if False else frame)

    def door(self, x, y, z, facing="south", block="minecraft:oak_door"):
        self.set(x, y, z, B(block, facing=facing, half="lower", hinge="left"))
        self.set(x, y + 1, z, B(block, facing=facing, half="upper", hinge="left"))
        # 门框
        self.set(x - 1, y, z, "minecraft:spruce_planks")
        self.set(x - 1, y + 1, z, "minecraft:spruce_planks")
        self.set(x + 1, y, z, "minecraft:spruce_planks")
        self.set(x + 1, y + 1, z, "minecraft:spruce_planks")
        self.set(x, y + 2, z, "minecraft:spruce_planks")

    # ---------- 组合件 ----------

    def house(self, x1, z1, x2, z2, y, height, wall, style,
              door_x=None, windows=(), window_y=None, chimney_x=None,
              gable_overhang=1, porch=False, porch_block="minecraft:spruce_planks"):
        """盖一栋正房：楼板 + 四面墙 + 南面门窗 + 双坡顶。

        windows: [(x_start, x_end), ...] —— 南墙上的窗洞区间。
        """
        # 地基 + 楼板
        self.fill(x1 - 1, y - 1, z1 - 1, x2 + 1, y - 1, z2 + 1, "minecraft:cobblestone")
        self.fill(x1, y, z1, x2, y, z2, "minecraft:spruce_planks")
        # 墙
        self.shell(x1, y + 1, z1, x2, y + height, z2, wall)
        # 掏空室内
        self.fill(x1 + 1, y + 1, z1 + 1, x2 - 1, y + height, z2 - 1, AIR)
        # 窗
        wy = window_y if window_y is not None else y + 2
        for (a, b) in windows:
            self.window_band(a, b, wy, z2, height=2)
        # 门
        if door_x is not None:
            self.door(door_x, y + 1, z2)
            if porch:
                self.fill(door_x - 1, y, z2 + 1, door_x + 1, y, z2 + 2, "minecraft:stone_bricks")
                self.set(door_x - 2, y + 3, z2 + 2, B(porch_block, half="bottom"))
                self.set(door_x + 2, y + 3, z2 + 2, B(porch_block, half="bottom"))
                self.set(door_x - 2, y + 1, z2 + 2, "minecraft:spruce_log", )
                self.set(door_x + 2, y + 1, z2 + 2, "minecraft:spruce_log")
        # 屋顶
        self.gable_roof(x1, x2, z1, z2, y + height + 1, style, overhang=gable_overhang)
        # 烟囱
        if chimney_x is not None:
            for yy in range(y + height, y + height + 5):
                self.set(chimney_x, yy, z1 + 1, "minecraft:bricks")
                self.set(chimney_x, yy, z1 + 2, "minecraft:bricks")

    def kang(self, x1, z1, x2, z2, y, block="minecraft:bricks", top="minecraft:white_carpet"):
        """火炕：一层红砖 + 一层白色地毯（地毯就贴在砖面上，不用整块白羊毛）。"""
        self.fill(x1, y, z1, x2, y, z2, block)
        self.fill(x1, y + 1, z1, x2, y + 1, z2, top)

    def stove(self, x, y, z):
        self.set(x, y, z, B("minecraft:furnace", facing="south"))
        self.set(x + 1, y, z, B("minecraft:furnace", facing="south"))
        self.set(x, y + 1, z, "minecraft:cauldron")
        self.set(x + 1, y + 1, z, "minecraft:campfire")

    def table(self, x, y, z):
        self.set(x, y, z, "minecraft:oak_fence")
        self.set(x, y + 1, z, B("minecraft:oak_trapdoor", half="top", facing="north"))

    def storage_sacks(self, x, y, z, ids):
        for i, sid in enumerate(ids):
            self.set(x + i, y, z, "dongbei_delight:" + sid)

    def well(self, x, y, z):
        """水井：石圈 + 水 + 木架 + 辘轳。"""
        self.fill(x - 1, y, z - 1, x + 1, y, z + 1, "minecraft:cobblestone")
        self.set(x, y + 1, z, "minecraft:water")
        for (dx, dz) in ((-1, -1), (-1, 1), (1, -1), (1, 1)):
            self.set(x + dx, y + 2, z + dz, "minecraft:oak_fence")
        self.fill(x - 1, y + 3, z - 1, x + 1, y + 3, z - 1, "minecraft:oak_planks")
        self.fill(x - 1, y + 3, z + 1, x + 1, y + 3, z + 1, "minecraft:oak_planks")

    def haystack(self, x, y, z, w=3, d=3, h=2):
        for i in range(h):
            self.fill(x, y + i, z, x + w - 1 - i, y + i, z + d - 1 - i, "minecraft:hay_block")

    def log_pile(self, x, y, z, length=4, rows=2, use="minecraft:stripped_oak_log"):
        for r in range(rows):
            for i in range(length - r * 2):
                self.set(x + i, y + r, z, B(use, axis="x"))

    def garden(self, x1, z1, x2, z2, y, crops, water_every=0):
        """菜园：耕地 + 作物（每行一种）+ 可选水沟。"""
        for z in range(z1, z2 + 1):
            crop = crops[(z - z1) % len(crops)]
            for x in range(x1, x2 + 1):
                if water_every and (x - x1) % water_every == water_every - 1:
                    self.set(x, y - 1, z, "minecraft:water")
                    self.set(x, y, z, AIR)
                    continue
                self.set(x, y - 1, z, B("minecraft:farmland", moisture=7))
                self.set(x, y, z, B("dongbei_delight:" + crop, age=7))

    # ---------- 输出 ----------

    def bounds(self):
        if not self.cells:
            return (0, 0, 0)
        mx = max(k[0] for k in self.cells)
        my = max(k[1] for k in self.cells)
        mz = max(k[2] for k in self.cells)
        return (mx + 1, my + 1, mz + 1)

    def to_nbt(self, data_version=3955, base_y=0):
        """出 NBT。

        base_y：在院子地面以下再垫几层土，这样地形起伏时院子不会整块悬空。
        垫完之后所有 y 坐标统一上移 base_y，最低点保证是 0。
        """
        cells = dict(self.cells)
        if base_y > 0:
            w, _h, d = self.bounds()
            for x in range(w):
                for z in range(d):
                    for k in range(base_y):
                        cells[(x, k - base_y, z)] = ("minecraft:dirt", {}, None)
        palette = []
        index = {}
        blocks = []
        shifted = {k: v for k, v in cells.items()}
        for pos in sorted(shifted):
            name, props, nbt = shifted[pos]
            key = (name, tuple(sorted(props.items())), repr(sorted(nbt.items())) if nbt else "")
            if key not in index:
                index[key] = len(palette)
                entry = {"Name": name}
                if props:
                    entry["Properties"] = {k: str(v) for k, v in sorted(props.items())}
                palette.append(entry)
            out = {"pos": [pos[0], pos[1] + base_y, pos[2]], "state": index[key]}
            if nbt:
                out["nbt"] = nbt
            blocks.append(out)
        max_x = max(k[0] for k in shifted) + 1
        max_y = max(k[1] for k in shifted) + base_y + 1
        max_z = max(k[2] for k in shifted) + 1
        return {
            "DataVersion": data_version,
            "size": [max_x, max_y, max_z],
            "palette": palette,
            "blocks": blocks,
            "entities": nbt_io.ListOf(nbt_io.TAG_COMPOUND, []),
        }
