"""结构自检：把「游戏里会静默变空气 / 变错状态」的问题提前抓出来。

查这些：
  1. 方块名是否在允许清单里（写错名字 = 游戏里那一格直接变空气）
  2. 属性名 / 属性值是否合法（写错 = 报错或忽略，看着就像丢方块）
  3. 门是否成对（少了上半截会变成半截门）
  4. 作物下面是不是耕地
  5. 屋顶楼梯的坡向是不是朝屋脊升（这个最容易看走眼）
  6. size 和实际坐标是否对得上
"""

import argparse
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import nbt_io
from designs import DESIGNS

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
DEFAULT_NBT = os.path.join(
    ROOT,
    "src", "main", "resources", "data", "northeast_china_delight", "structure",
)

DIRECTIONS = ["north", "south", "east", "west", "up", "down"]
AXES = ["x", "y", "z"]
HALVES = ["top", "bottom", "double", "upper", "lower"]
DOOR_HALVES = ["upper", "lower"]
HINGES = ["left", "right"]

# 用到什么就登记什么，多出来的会被报出来，逼着人补登记
VALID_BLOCKS = set("""
minecraft:air minecraft:bricks minecraft:brick_stairs minecraft:brick_slab
minecraft:stone_brick_stairs minecraft:stone_brick_slab minecraft:stone_bricks
minecraft:cobblestone minecraft:cobblestone_stairs minecraft:cobblestone_slab
minecraft:packed_mud minecraft:mud_bricks minecraft:mud_brick_stairs minecraft:mud_brick_slab
minecraft:spruce_log minecraft:oak_log minecraft:stripped_spruce_log minecraft:stripped_oak_log
minecraft:spruce_planks minecraft:oak_planks minecraft:spruce_stairs minecraft:oak_stairs
minecraft:spruce_slab minecraft:oak_slab minecraft:stone_slab
minecraft:spruce_fence minecraft:oak_fence minecraft:spruce_fence_gate minecraft:oak_fence_gate
minecraft:spruce_trapdoor minecraft:oak_trapdoor
minecraft:glass minecraft:glass_pane minecraft:water minecraft:farmland
minecraft:dirt minecraft:coarse_dirt minecraft:dirt_path minecraft:grass_block minecraft:podzol
minecraft:snow minecraft:snow_block minecraft:hay_block minecraft:gravel minecraft:stone
minecraft:yellow_terracotta minecraft:brown_terracotta minecraft:oak_leaves
minecraft:stone_slab
minecraft:red_bed minecraft:flower_pot minecraft:smoker minecraft:ladder
minecraft:yellow_wool minecraft:brown_wool
minecraft:white_carpet minecraft:mud
minecraft:oak_door minecraft:dark_oak_door minecraft:iron_door
minecraft:lantern minecraft:torch minecraft:wall_torch minecraft:campfire
minecraft:tripwire
minecraft:furnace minecraft:cauldron minecraft:water_cauldron
minecraft:chest minecraft:barrel minecraft:crafting_table minecraft:grindstone minecraft:stonecutter
minecraft:red_wool minecraft:white_wool minecraft:gray_concrete minecraft:white_concrete
minecraft:birch_planks minecraft:deepslate_tile_stairs minecraft:deepslate_tiles
minecraft:polished_blackstone_stairs minecraft:polished_blackstone
farmersdelight:rope_fence farmersdelight:rope_fence_gate farmersdelight:rope
northeast_china_delight:vat
northeast_china_delight:corn_seeds_sack northeast_china_delight:soybean_sack northeast_china_delight:red_bean_sack
northeast_china_delight:buckwheat_sack northeast_china_delight:peanut_sack northeast_china_delight:ginseng_sack
northeast_china_delight:hazelnut_sack northeast_china_delight:hazel_mushroom_sack northeast_china_delight:wood_ear_sack
northeast_china_delight:napa_cabbage_crate northeast_china_delight:cucumber_crate northeast_china_delight:green_radish_crate
northeast_china_delight:green_onion_crate northeast_china_delight:eggplant_crate northeast_china_delight:green_pepper_crate
northeast_china_delight:red_chili_crate northeast_china_delight:green_beans_crate northeast_china_delight:corn_crate
northeast_china_delight:sweet_potato_crate northeast_china_delight:sour_cabbage_crate northeast_china_delight:spicy_cabbage_crate
northeast_china_delight:pickled_cucumber_crate northeast_china_delight:pickled_carrot_crate
northeast_china_delight:pickled_green_radish_crate northeast_china_delight:grill_rack
northeast_china_delight:grill_campfire
create:millstone
northeast_china_delight:napa_cabbage_crop northeast_china_delight:cucumber_crop northeast_china_delight:green_radish_crop
northeast_china_delight:green_onion_crop northeast_china_delight:eggplant_crop northeast_china_delight:green_pepper_crop
northeast_china_delight:red_chili_crop northeast_china_delight:green_beans_crop northeast_china_delight:corn_crop
northeast_china_delight:corn_stalk northeast_china_delight:soybean_crop northeast_china_delight:peanut_crop
northeast_china_delight:red_bean_crop northeast_china_delight:sweet_potato_crop northeast_china_delight:buckwheat_crop
""".split())

PROP_VALUES = {
    "facing": DIRECTIONS,
    "axis": AXES,
    "half": HALVES,
    "type": HALVES + ["single", "left", "right"],
    "hinge": HINGES,
    "shape": ["straight", "inner_left", "inner_right", "outer_left", "outer_right"],
    "face": ["floor", "wall", "ceiling"],
    "hanging": ["true", "false"],
    "open": ["true", "false"],
    "lit": ["true", "false"],
    "waterlogged": ["true", "false"],
    "snowy": ["true", "false"],
    "stage": ["0", "1", "2"],
}


def check(path):
    tag = nbt_io.read_nbt(path)
    palette = tag["palette"]
    errors = []
    warnings = []
    cells = {}
    for entry in tag["blocks"]:
        pos = tuple(entry["pos"])
        state = palette[entry["state"]]
        name = state["Name"]
        props = state.get("Properties", {})
        cells[pos] = (name, props)
        if name not in VALID_BLOCKS:
            errors.append("未登记的方块 %s @%s" % (name, pos))
        for key, value in props.items():
            if key == "age":
                continue
            if key in ("layers", "moisture", "level", "distance", "power", "rotation", "stage", "progress"):
                continue
            allowed = PROP_VALUES.get(key)
            if allowed and str(value) not in allowed:
                errors.append("属性值可疑 %s=%s @%s" % (key, value, pos))
    # 门要成对
    for pos, (name, props) in cells.items():
        if name.endswith("_door") and props.get("half") == "lower":
            up = cells.get((pos[0], pos[1] + 1, pos[2]))
            if not up or up[0] != name or up[1].get("half") != "upper":
                errors.append("门缺上半截 @%s" % (pos,))
    # 门口不能有遮挡：门前 3 宽 × 4 深这一段必须是能走人的空地
    blocked = {"minecraft:farmland", "minecraft:water", "minecraft:coarse_dirt"}
    for pos, (name, props) in cells.items():
        if not name.endswith("_door") or props.get("half") != "lower":
            continue
        facing = props.get("facing", "south")
        dx, dz = {"north": (0, -1), "south": (0, 1), "east": (1, 0), "west": (-1, 0)}[facing]
        side = (dz, dx)  # 垂直于门朝向
        for step in range(1, 5):
            for offset in (-1, 0, 1):
                x = pos[0] + dx * step + side[0] * offset
                z = pos[2] + dz * step + side[1] * offset
                for y in (pos[1], pos[1] + 1):
                    cell = cells.get((x, y, z))
                    if not cell:
                        continue
                    cname, cprops = cell
                    if cname.endswith("_door") or cname in (
                            "minecraft:air", "minecraft:torch", "minecraft:lantern",
                            "minecraft:stone_bricks", "minecraft:dirt_path",
                            "minecraft:grass_block", "minecraft:cobblestone",
                            "minecraft:tripwire", "minecraft:coarse_dirt"):
                        continue
                    if cname.endswith("_trapdoor") or cname.endswith("_button"):
                        continue
                    blocked.add(cname)
                    errors.append("门口被 %s 挡住 @%s（门在 %s，朝 %s）"
                                  % (cname, (x, y, z), pos, facing))
                    break
    # 院落大门（栅栏门）也要能进出：门里门外各 4 格、4 格宽都得通
    solid_ok = {"minecraft:air", "minecraft:dirt_path", "minecraft:grass_block",
                "minecraft:stone_bricks", "minecraft:cobblestone", "minecraft:coarse_dirt",
                "minecraft:gravel", "minecraft:snow", "minecraft:water"}
    # 先把「院墙大门」按门线分组（同一排连着的栅栏门算一道门），
    # 菜园自己的小门（不贴外沿）不管。
    size_x, size_y, size_z = tag["size"]
    gate_lines = {}
    for pos, (name, props) in cells.items():
        if not name.endswith("_fence_gate"):
            continue
        near_edge = (pos[0] < 4 or pos[0] > size_x - 5 or pos[2] < 4 or pos[2] > size_z - 5)
        if not near_edge:
            continue
        key = (pos[1], pos[2], props.get("facing", "south"))
        gate_lines.setdefault(key, []).append(pos[0])
    for (gy, gz, facing), xs in gate_lines.items():
        gx1, gx2 = min(xs), max(xs)
        pos = (gx1, gy, gz)
        # 通道宽度 = 门洞本身，左右各留 1 格余量
        lateral_offsets = range(gx1 - 1 - gx1, gx2 + 2 - gx1)
        dx, dz = {"north": (0, -1), "south": (0, 1), "east": (1, 0), "west": (-1, 0)}[facing]
        side = (dz, dx)
        for sign in (1, -1):
            for step in range(1, 5):
                for offset in lateral_offsets:
                    x = pos[0] + dx * step * sign + side[0] * offset
                    z = pos[2] + dz * step * sign + side[1] * offset
                    for y in (pos[1], pos[1] + 1):
                        cell = cells.get((x, y, z))
                        if not cell:
                            continue
                        cname = cell[0]
                        if cname in solid_ok or cname.endswith("_door") \
                                or cname.endswith("_fence_gate") \
                                or cname.endswith("_trapdoor") \
                                or cname.endswith("_fence"):
                            continue
                        errors.append("大门被 %s 挡住 @%s（门线 x=%d..%d, z=%d）"
                                      % (cname, (x, y, z), gx1, gx2, gz))
                        break
                    else:
                        continue
                    break
    # 作物要有耕地
    for pos, (name, props) in cells.items():
        if name.endswith("_crop") and "age" in props:
            # 大葱是 4 阶段作物（age 0~3），写成 7 游戏里会读不出来、那一格变空气
            if name.endswith("green_onion_crop") and int(props["age"]) > 3:
                errors.append("大葱 age 超范围（0~3）：%s @%s" % (props["age"], pos))
            elif int(props["age"]) > 7:
                errors.append("作物 age 超范围（0~7）：%s @%s" % (props["age"], pos))
        if name.endswith("_crop") and props.get("age") == 7:
            below = cells.get((pos[0], pos[1] - 1, pos[2]))
            if not below:
                pass  # 结构底面之外的耕地由结构自己带，缺了才报
            elif below[0] != "minecraft:farmland":
                warnings.append("作物下面不是耕地 %s @%s" % (below[0], pos))
        if name == "northeast_china_delight:corn_stalk":
            below = cells.get((pos[0], pos[1] - 1, pos[2]))
            if not below or below[0] != "northeast_china_delight:corn_crop":
                errors.append("玉米上截下面没有玉米下截 @%s" % (pos,))
    # 屋顶坡向：同一 z 上的楼梯 facing 应该一致，且南北坡相反
    by_z = {}
    for pos, (name, props) in cells.items():
        if name.endswith("_stairs") and props.get("facing") in ("north", "south"):
            by_z.setdefault(pos[2], set()).add(props["facing"])
    for z, facings in sorted(by_z.items()):
        if len(facings) > 1:
            warnings.append("z=%d 这一排楼梯朝向不一致：%s" % (z, sorted(facings)))
    # 双坡屋顶：最北那排楼梯应该朝南升、最南那排应该朝北升。
    # 门楼/灶棚这类小屋面贴着院墙，不参与"主屋坡向"判断，所以只取中间那段。
    z_lo, z_hi = 6, tag["size"][2] - 7
    slope = {z: sorted(f) for z, f in by_z.items() if z_lo <= z <= z_hi}
    if len(slope) >= 3:      # 只有一排楼梯（比如整片实心顶）时不做坡向判断
        zs = sorted(slope)
        north_row = slope[zs[0]]
        south_row = slope[zs[-1]]
        if north_row == ["north"]:
            errors.append("屋顶坡向反了：最北一排楼梯朝北升（应该朝南升）")
        if south_row == ["south"]:
            errors.append("屋顶坡向反了：最南一排楼梯朝南升（应该朝北升）")
    size = tag["size"]
    for pos in cells:
        if any(pos[i] < 0 or pos[i] >= size[i] for i in range(3)):
            errors.append("坐标越界 %s（size=%s）" % (pos, size))
    return size, len(cells), errors, warnings


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--nbt", default=DEFAULT_NBT)
    args = ap.parse_args()
    bad = 0
    # 村庄副食店：门必须是「下半截 + 上半截」成对，且属性不能被清空
    import glob
    for path in sorted(glob.glob(os.path.join(args.nbt, "village", "*.nbt"))):
        tag = nbt_io.read_nbt(path)
        palette = tag["palette"]
        cells = {}
        for entry in tag["blocks"]:
            cells[tuple(entry["pos"])] = (palette[entry["state"]]["Name"],
                                          palette[entry["state"]].get("Properties", {}))
        errors = []
        for pos, (name, props) in cells.items():
            if not name.endswith("_door"):
                continue
            if "half" not in props:
                errors.append("门丢了 half 属性（会变成半截门）@%s" % (pos,))
                continue
            if props.get("half") == "lower":
                up = cells.get((pos[0], pos[1] + 1, pos[2]))
                if not up or up[0] != name or up[1].get("half") != "upper":
                    errors.append("门缺上半截 @%s" % (pos,))
        for name, props in cells.values():
            if name.endswith("_trapdoor") and "half" not in props:
                errors.append("活板门丢了 half 属性 @?")
                break
        flag = "OK " if not errors else "ERR"
        print("[%s] %-28s %2dx%2dx%-2d %4d blocks  错误 %d"
              % (flag, os.path.basename(path), tag["size"][0], tag["size"][1],
                 tag["size"][2], len(cells), len(errors)))
        for e in errors[:8]:
            print("      ! %s" % e)
        bad += len(errors)
    for name, _fn in DESIGNS:
        path = os.path.join(args.nbt, name + ".nbt")
        if not os.path.exists(path):
            print("缺文件 %s" % path)
            bad += 1
            continue
        size, count, errors, warnings = check(path)
        flag = "OK " if not errors else "ERR"
        print("[%s] %-28s %2dx%2dx%-2d %5d blocks  错误 %d 警告 %d"
              % (flag, name, size[0], size[1], size[2], count, len(errors), len(warnings)))
        for e in errors[:12]:
            print("      ! %s" % e)
        for w in warnings[:6]:
            print("      ~ %s" % w)
        bad += len(errors)
    print("错误合计 %d" % bad)
    return 0 if bad == 0 else 1


if __name__ == "__main__":
    sys.exit(main())
