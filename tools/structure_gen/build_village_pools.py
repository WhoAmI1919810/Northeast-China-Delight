"""把副食店塞进原版村庄的「房屋池」。

做法：把原版 data/minecraft/worldgen/template_pool/village/<群系>/houses.json
原样抄一份，在 elements 里追加我们的副食店，再放进本模组资源包里覆盖原版文件。
（数据包同路径以高优先级覆盖，这是给原版村庄加房子的常规做法。）

顺带把「僵尸村庄」的池子也一起抄出来追加，免得僵尸村里没有副食店。
"""

import json
import os
import sys
import zipfile

ROOT = r"H:\IdeaProjects\northeast_china_delight1.21.1"
CLIENT_JAR = r"C:\Users\wcs\.gradle\caches\neoformruntime\artifacts\minecraft_1.21.1_client.jar"
OUT_ROOT = os.path.join(ROOT, "src", "main", "resources", "data", "minecraft",
                        "worldgen", "template_pool", "village")

BIOMES = ["plains", "desert", "savanna", "snowy", "taiga"]
# 和原版对应群系保持一致：这两个群系的原版房子会抹苔藓
MOSSY = {"plains", "taiga"}
WEIGHT = 5          # 和其他小房子一个量级偏上（原版小房子多为 2~4）；想让副食店更常见就再调大


def shop_element(biome):
    processors = "minecraft:mossify_10_percent" if biome in MOSSY else {"processors": []}
    return {
        "element": {
            "element_type": "minecraft:legacy_single_pool_element",
            "location": "northeast_china_delight:village/relish_%s" % biome,
            "processors": processors,
            "projection": "rigid",
        },
        "weight": WEIGHT,
    }


def patch(pool_path, biome, out_path):
    with zipfile.ZipFile(CLIENT_JAR) as z:
        data = json.loads(z.read(pool_path).decode("utf-8"))
    before = len(data["elements"])
    data["elements"].append(shop_element(biome))
    os.makedirs(os.path.dirname(out_path), exist_ok=True)
    with open(out_path, "w", encoding="utf-8") as fh:
        json.dump(data, fh, ensure_ascii=False, indent=2)
    return before, len(data["elements"])


def main():
    for biome in BIOMES:
        for prefix, sub in (("", ""), ("zombie/", "zombie/")):
            src = "data/minecraft/worldgen/template_pool/village/%s/%shouses.json" % (biome, prefix)
            dst = os.path.join(OUT_ROOT, biome, sub + "houses.json")
            try:
                before, after = patch(src, biome, dst)
            except KeyError:
                print("%-8s %-8s 原版没有这个池子，跳过" % (biome, prefix or "-"))
                continue
            print("%-8s %-8s 元素 %2d → %2d  %s" % (biome, prefix or "-", before, after, dst))


if __name__ == "__main__":
    main()
