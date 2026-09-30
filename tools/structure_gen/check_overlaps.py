"""检查每个院子模板里「组件与组件」有没有压在一起。

做法：把 parts.py 里的每个构件函数包一层，记录它这次调用写了哪些方块；
同时给 Build 的写方块方法打点。如果两个构件写了同一个方块、或者体积相交，
就说明它们叠在一起了。

用法：
    H:\\miniconda3\\python.exe tools\\structure_gen\\check_overlaps.py
"""

import inspect
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import kit            # noqa: E402
import parts          # noqa: E402
import designs        # noqa: E402

WRITE_METHODS = ("set", "fill", "shell", "hollow", "slab_row", "fence_line", "gable_roof",
                 "shed_roof", "flat_roof", "window_band", "door", "house", "kang", "stove",
                 "table", "storage_sacks", "well", "haystack", "log_pile", "garden")

current_spans = []
recorded_spans = []


def record(x, y, z):
    if current_spans:
        current_spans[0]["cells"].add((x, y, z))
        return
    # 设计里直接写的方块（仓房、木刻楞主体之类）也要算成一个「组件」，
    # 用调用它的那一行来分组，名字形如 raw@123
    # 往上找到 designs.py 里的那一行（中间的 Build/parts 内部帧都跳过）
    depth = 1
    frame = sys._getframe(depth)
    while not frame.f_code.co_filename.endswith("designs.py"):
        depth += 1
        frame = sys._getframe(depth)
    name = "raw:%s@%d" % (frame.f_code.co_name, frame.f_lineno)
    if frame.f_code.co_name in ("_yard", "_ground", "_brick_wall"):
        return          # 地基 / 院墙属于背景层，不参与「组件打架」判断
    for span in recorded_spans:
        if span["name"] == name:
            span["cells"].add((x, y, z))
            return
    span = {"name": name, "cells": {(x, y, z)}}
    recorded_spans.append(span)


def is_air(block):
    """空气不算「占了这块地方」——挖室内空间是正常的。"""
    if block is None:
        return False
    name = getattr(block, "id", None) or getattr(block, "name", None) or str(block)
    return str(name).split("[")[0] in ("minecraft:air", "minecraft:cave_air", "minecraft:void_air")


def patch_build_method(name):
    original = getattr(kit.Build, name)

    def wrapper(self, *args, **kwargs):
        if name == "set":
            if not is_air(args[3] if len(args) > 3 else kwargs.get("block")):
                record(args[0], args[1], args[2])
        elif name in ("fill", "shell", "hollow"):
            if is_air(args[6] if len(args) > 6 else kwargs.get("block")):
                return original(self, *args, **kwargs)
            x1, y1, z1, x2, y2, z2 = args[:6]
            for x in range(min(x1, x2), max(x1, x2) + 1):
                for y in range(min(y1, y2), max(y1, y2) + 1):
                    for z in range(min(z1, z2), max(z1, z2) + 1):
                        record(x, y, z)
        return original(self, *args, **kwargs)

    setattr(kit.Build, name, wrapper)


def patch_part(name, fn):
    def wrapper(b, *args, **kwargs):
        # 室内三开间单独算一个「组件」：这样它和房子墙体是否打架也能查出来
        top = not current_spans or name in ("interior_three_bay", "interior_basic")
        if top:
            span = {"name": name, "cells": set()}
            current_spans.append(span)
            try:
                result = fn(b, *args, **kwargs)
            finally:
                current_spans.pop()
                recorded_spans.append(span)
            return result
        return fn(b, *args, **kwargs)

    wrapper.__name__ = name
    return wrapper


def bbox(cells):
    xs = [c[0] for c in cells]
    ys = [c[1] for c in cells]
    zs = [c[2] for c in cells]
    return min(xs), min(ys), min(zs), max(xs), max(ys), max(zs)


def intersect_volume(a, b):
    ax1, ay1, az1, ax2, ay2, az2 = bbox(a)
    bx1, by1, bz1, bx2, by2, bz2 = bbox(b)
    dx = min(ax2, bx2) - max(ax1, bx1) + 1
    dy = min(ay2, by2) - max(ay1, by1) + 1
    dz = min(az2, bz2) - max(az1, bz1) + 1
    if dx <= 0 or dy <= 0 or dz <= 0:
        return 0
    return dx * dy * dz


def main():
    for method in WRITE_METHODS:
        if hasattr(kit.Build, method):
            patch_build_method(method)
    for name, fn in list(vars(parts).items()):
        if inspect.isfunction(fn) and fn.__module__ == parts.__name__:
            setattr(parts, name, patch_part(name, fn))

    only = sys.argv[1] if len(sys.argv) > 1 else None
    list_only = "--list" in sys.argv
    for name, fn in designs.DESIGNS:
        if only and only not in name:
            continue
        current_spans.clear()
        recorded_spans.clear()
        build = fn()
        spans = [s for s in recorded_spans if len(s["cells"]) >= 4]
        if list_only:
            print(f"\n=== {name}  尺寸={build.bounds()} ===")
            for s in sorted(spans, key=lambda s: bbox(s["cells"])):
                print(f"   {bbox(s['cells'])}  {s['name']}")
            continue
        problems = []
        for i in range(len(spans)):
            for j in range(i + 1, len(spans)):
                shared = len(spans[i]["cells"] & spans[j]["cells"])
                vol = intersect_volume(spans[i]["cells"], spans[j]["cells"])
                if shared > 0 or vol > 0:
                    problems.append((shared, vol, spans[i], spans[j]))
        print(f"\n=== {name}  组件数={len(spans)}  尺寸={build.bounds()} ===")
        if not problems:
            print("   组件之间没有相交")
            continue
        problems.sort(key=lambda p: (-p[0], -p[1]))
        for shared, vol, a, b in problems[:20]:
            print(f"   CONFLICT shared={shared} vol={vol} {a['name']}__{b['name']}")
            print(f"        A {a['name']} {bbox(a['cells'])}")
            print(f"        B {b['name']} {bbox(b['cells'])}")
            if shared > 0:
                shared_cells = sorted(a["cells"] & b["cells"])[:12]
                print(f"        同格坐标: {shared_cells}")


if __name__ == "__main__":
    main()
