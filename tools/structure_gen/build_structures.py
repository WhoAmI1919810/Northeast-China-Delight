"""把 designs.py 里的院子写成 .nbt 结构文件。

用法：
    H:\\miniconda3\\python.exe tools\\structure_gen\\build_structures.py
    H:\\miniconda3\\python.exe tools\\structure_gen\\build_structures.py --out E:\\codex\\dd_tmp\\nbt
"""

import argparse
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import nbt_io
from designs import DESIGNS

DEFAULT_OUT = os.path.join(
    r"H:\IdeaProjects\dongbei_delight1.21.1",
    "src", "main", "resources", "data", "dongbei_delight", "structure",
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
        tag = build.to_nbt()
        path = os.path.join(args.out, name + ".nbt")
        nbt_io.write_nbt(path, tag)
        size = tag["size"]
        print("%-28s size=%2dx%2dx%-2d blocks=%5d palette=%3d -> %s"
              % (name, size[0], size[1], size[2], len(tag["blocks"]), len(tag["palette"]), path))


if __name__ == "__main__":
    main()
