"""把 designs.py 里的院子写成 .nbt 结构文件。

用法：
    python tools/structure_gen/build_structures.py
    python tools/structure_gen/build_structures.py --out <输出目录>
"""

import argparse
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import nbt_io
from designs import DESIGNS

# 工程根目录（本文件位于 <root>/tools/structure_gen/ 下）
ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
DEFAULT_OUT = os.path.join(
    ROOT,
    "src", "main", "resources", "data", "northeast_china_delight", "structure",
)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default=DEFAULT_OUT)
    ap.add_argument("--only", default=None, help="只生成名字里含这个串的院子")
    args = ap.parse_args()

    os.makedirs(args.out, exist_ok=True)
    for name, fn in DESIGNS:
        if args.only and args.only not in name:
            continue
        build = fn()
        # base_y=4：院子地面以下多垫 4 层土，用来挖菜窖（地面层在模板的第 4 层，
        # 这个数字必须和 DongbeiCourtyardStructure.GROUND_LAYER_IN_TEMPLATE 保持一致）
        tag = build.to_nbt(base_y=4)
        path = os.path.join(args.out, name + ".nbt")
        nbt_io.write_nbt(path, tag)
        size = tag["size"]
        print("%-28s size=%2dx%2dx%-2d blocks=%5d palette=%3d -> %s"
              % (name, size[0], size[1], size[2], len(tag["blocks"]), len(tag["palette"]), path))


if __name__ == "__main__":
    main()
