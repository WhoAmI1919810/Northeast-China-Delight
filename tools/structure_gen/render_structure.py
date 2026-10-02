"""自己写的 .nbt 结构渲染器。

三张视图拼到一张 PNG：
    左边  —— 正立面（站在院子南边往里看）
    中间  —— 俯视平面（看院落布局）
    右边  —— 等距轴测（看体块和屋顶）

纯标准库实现（zlib 手写 PNG + 扫描线多边形填充），不依赖 Pillow。

用法：
    H:\\miniconda3\\python.exe tools\\structure_gen\\render_structure.py
    H:\\miniconda3\\python\\... --only dongbei_yard_garden --scale 10 --out E:\\codex\\dd_tmp\\render
"""

import argparse
import os
import struct
import sys
import zlib

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import nbt_io
from mcblocks import color_of
from designs import DESIGNS

DEFAULT_NBT = os.path.join(
    r"H:\IdeaProjects\northeast_china_delight1.21.1",
    "src", "main", "resources", "data", "northeast_china_delight", "structure",
)
DEFAULT_OUT = r"E:\codex\dd_tmp\render"

BG = (26, 30, 38)


class Canvas:
    def __init__(self, w, h, bg=BG):
        self.w = w
        self.h = h
        self.px = bytearray(w * h * 3)
        for i in range(w * h):
            self.px[i * 3:i * 3 + 3] = bytes(bg)

    def rect(self, x0, y0, x1, y1, color):
        x0 = max(0, int(x0)); y0 = max(0, int(y0))
        x1 = min(self.w, int(x1)); y1 = min(self.h, int(y1))
        row = bytes(color) * max(0, x1 - x0)
        if not row:
            return
        for y in range(y0, y1):
            off = (y * self.w + x0) * 3
            self.px[off:off + len(row)] = row

    def polygon(self, points, color):
        if len(points) < 3:
            return
        ys = [p[1] for p in points]
        y0 = max(0, int(min(ys)))
        y1 = min(self.h - 1, int(max(ys)) + 1)
        n = len(points)
        for y in range(y0, y1 + 1):
            yc = y + 0.5
            xs = []
            for i in range(n):
                ax, ay = points[i]
                bx, by = points[(i + 1) % n]
                if (ay <= yc < by) or (by <= yc < ay):
                    t = (yc - ay) / (by - ay)
                    xs.append(ax + (bx - ax) * t)
            if len(xs) < 2:
                continue
            xs.sort()
            for i in range(0, len(xs) - 1, 2):
                xa = max(0, int(round(xs[i])))
                xb = min(self.w, int(round(xs[i + 1])))
                if xb <= xa:
                    continue
                off = (y * self.w + xa) * 3
                self.px[off:off + (xb - xa) * 3] = bytes(color) * (xb - xa)

    def save(self, path):
        raw = bytearray()
        stride = self.w * 3
        for y in range(self.h):
            raw.append(0)
            raw += self.px[y * stride:(y + 1) * stride]
        def chunk(tag, data):
            out = struct.pack(">I", len(data)) + tag + data
            return out + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)
        png = b"\x89PNG\r\n\x1a\n"
        png += chunk(b"IHDR", struct.pack(">IIBBBBB", self.w, self.h, 8, 2, 0, 0, 0))
        png += chunk(b"IDAT", zlib.compress(bytes(raw), 6))
        png += chunk(b"IEND", b"")
        with open(path, "wb") as fh:
            fh.write(png)


def load_structure(path):
    tag = nbt_io.read_nbt(path)
    palette = tag["palette"]
    blocks = {}
    for entry in tag["blocks"]:
        pos = tuple(entry["pos"])
        entry_palette = palette[entry["state"]]
        name = entry_palette["Name"]
        if name in ("minecraft:air", "minecraft:cave_air", "minecraft:void_air"):
            continue
        blocks[pos] = (name, entry_palette.get("Properties", {}))
    return tag["size"], blocks


def shade(color, factor):
    return tuple(max(0, min(255, int(c * factor))) for c in color)


def draw_front(canvas, ox, oy, size, blocks, scale):
    """正立面：每个 (x,y) 取最靠南的方块。"""
    w, h, d = size
    front = {}
    for (x, y, z), (name, _props) in blocks.items():
        key = (x, y)
        if key not in front or z > front[key][0]:
            front[key] = (z, name)
    for (x, y), (z, name) in front.items():
        top, side = color_of(name)
        canvas.rect(ox + x * scale, oy + (h - 1 - y) * scale,
                    ox + (x + 1) * scale, oy + (h - y) * scale, side)


def draw_plan(canvas, ox, oy, size, blocks, scale):
    """俯视平面：每个 (x,z) 取最高的方块。"""
    w, h, d = size
    top = {}
    for (x, y, z), (name, _props) in blocks.items():
        key = (x, z)
        if key not in top or y > top[key][0]:
            top[key] = (y, name)
    for (x, z), (y, name) in top.items():
        c1, c2 = color_of(name)
        canvas.rect(ox + x * scale, oy + z * scale,
                    ox + (x + 1) * scale, oy + (z + 1) * scale, c1)


def draw_iso(canvas, ox, oy, size, blocks, scale):
    """等距轴测：东南上方看，画家算法从远到近。"""
    w, h, d = size
    uw = scale * 1.0          # 每格 x/z 方向在屏幕上的半宽
    uh = scale * 0.5          # 每格 x/z 方向在屏幕上的半高
    uv = scale * 1.0          # 每格 y 方向在屏幕上的高度

    def P(x, y, z):
        sx = ox + (x - z) * uw
        sy = oy + (x + z) * uh - y * uv
        return (sx, sy)

    order = sorted(blocks.keys(), key=lambda p: (p[1] + (p[0] + p[2]) * 1.0))
    for (x, y, z) in order:
        name, props = blocks[(x, y, z)]
        top, side = color_of(name)
        c_top = shade(top, 1.0)
        c_east = shade(side, 0.82)
        c_south = shade(side, 0.66)
        # 东面
        canvas.polygon([P(x + 1, y, z), P(x + 1, y, z + 1),
                        P(x + 1, y + 1, z + 1), P(x + 1, y + 1, z)], c_east)
        # 南面
        canvas.polygon([P(x, y, z + 1), P(x + 1, y, z + 1),
                        P(x + 1, y + 1, z + 1), P(x, y + 1, z + 1)], c_south)
        # 顶面
        canvas.polygon([P(x, y + 1, z), P(x + 1, y + 1, z),
                        P(x + 1, y + 1, z + 1), P(x, y + 1, z + 1)], c_top)
        # 楼梯：在「升高的一侧」压一条暗边，方便肉眼核对坡向对不对
        facing = props.get("facing")
        if name.endswith("_stairs") and facing:
            if facing == "north":
                quad = [P(x, y + 1, z), P(x + 1, y + 1, z),
                        P(x + 1, y + 1, z + 0.4), P(x, y + 1, z + 0.4)]
            elif facing == "south":
                quad = [P(x, y + 1, z + 0.6), P(x + 1, y + 1, z + 0.6),
                        P(x + 1, y + 1, z + 1), P(x, y + 1, z + 1)]
            elif facing == "east":
                quad = [P(x + 0.6, y + 1, z), P(x + 1, y + 1, z),
                        P(x + 1, y + 1, z + 1), P(x + 0.6, y + 1, z + 1)]
            else:
                quad = [P(x, y + 1, z), P(x + 0.4, y + 1, z),
                        P(x + 0.4, y + 1, z + 1), P(x, y + 1, z + 1)]
            canvas.polygon(quad, shade(c_top, 0.5))


def render(path, out_png, scale=8, title=""):
    size, blocks = load_structure(path)
    w, h, d = size
    iso_w = int((w + d) * scale * 1.0) + 40
    iso_h = int((w + d) * scale * 0.5 + h * scale) + 40
    front_w = w * scale + 20
    front_h = h * scale + 20
    plan_w = w * scale + 20
    plan_h = d * scale + 20
    gap = 18
    canvas = Canvas(front_w + plan_w + iso_w + gap * 4,
                    max(front_h, plan_h, iso_h) + gap * 2)
    x = gap
    draw_front(canvas, x, gap, size, blocks, scale)
    x += front_w + gap
    draw_plan(canvas, x, gap, size, blocks, scale)
    x += plan_w + gap
    draw_iso(canvas, x + iso_w // 2, gap + (iso_h - h * scale) // 2, size, blocks, scale)
    canvas.save(out_png)
    return size, len(blocks)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--nbt", default=DEFAULT_NBT)
    ap.add_argument("--out", default=DEFAULT_OUT)
    ap.add_argument("--only", default=None)
    ap.add_argument("--scale", type=int, default=8)
    args = ap.parse_args()
    os.makedirs(args.out, exist_ok=True)
    for name, _fn in DESIGNS:
        if args.only and args.only not in name:
            continue
        src = os.path.join(args.nbt, name + ".nbt")
        if not os.path.exists(src):
            print("缺少 %s，先跑 build_structures.py" % src)
            continue
        dst = os.path.join(args.out, name + ".png")
        size, count = render(src, dst, scale=args.scale)
        print("%-28s %2dx%2dx%-2d  %5d blocks -> %s" % (name, size[0], size[1], size[2], count, dst))


if __name__ == "__main__":
    main()
