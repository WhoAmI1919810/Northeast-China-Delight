"""最小 NBT 读写 —— 只覆盖 Minecraft 结构文件用到的类型。

自研的理由：不装第三方库，纯标准库（gzip + struct）就能读写 .nbt，
生成端和渲染端共用同一份实现，改格式不会两边跑偏。

类型推断规则：
    int     -> TAG_Int
    float   -> TAG_Double（结构里用不到 float，浮点一律当 double 存）
    str     -> TAG_String
    dict    -> TAG_Compound
    list    -> TAG_List（元素类型取第一个元素的类型；空表用 ListOf 显式指定）
"""

import gzip
import struct

TAG_END = 0
TAG_BYTE = 1
TAG_SHORT = 2
TAG_INT = 3
TAG_LONG = 4
TAG_FLOAT = 5
TAG_DOUBLE = 6
TAG_BYTE_ARRAY = 7
TAG_STRING = 8
TAG_LIST = 9
TAG_COMPOUND = 10
TAG_INT_ARRAY = 11
TAG_LONG_ARRAY = 12


class ListOf:
    """空表 / 想强制元素类型时用。"""

    __slots__ = ("tag_id", "items")

    def __init__(self, tag_id, items=None):
        self.tag_id = tag_id
        self.items = list(items or [])


class IntArray:
    __slots__ = ("values",)

    def __init__(self, values):
        self.values = list(values)


def _infer(value):
    if isinstance(value, ListOf):
        return TAG_LIST
    if isinstance(value, IntArray):
        return TAG_INT_ARRAY
    if isinstance(value, bool):
        return TAG_BYTE
    if isinstance(value, int):
        # 超出 int32 的整数（比如存档种子）必须写成 TAG_Long，否则打包会溢出
        if -2147483648 <= value <= 2147483647:
            return TAG_INT
        return TAG_LONG
    if isinstance(value, float):
        return TAG_DOUBLE
    if isinstance(value, str):
        return TAG_STRING
    if isinstance(value, dict):
        return TAG_COMPOUND
    if isinstance(value, (list, tuple)):
        return TAG_LIST
    raise TypeError("无法推断 NBT 类型: %r" % (value,))


def _write_payload(out, tag_id, value):
    if tag_id == TAG_BYTE:
        out.append(struct.pack(">b", 1 if value else 0))
    elif tag_id == TAG_SHORT:
        out.append(struct.pack(">h", value))
    elif tag_id == TAG_INT:
        out.append(struct.pack(">i", value))
    elif tag_id == TAG_LONG:
        out.append(struct.pack(">q", value))
    elif tag_id == TAG_FLOAT:
        out.append(struct.pack(">f", float(value)))
    elif tag_id == TAG_DOUBLE:
        out.append(struct.pack(">d", float(value)))
    elif tag_id == TAG_STRING:
        raw = value.encode("utf-8")
        out.append(struct.pack(">H", len(raw)))
        out.append(raw)
    elif tag_id == TAG_LIST:
        items = value.items if isinstance(value, ListOf) else list(value)
        if not items:
            sub = TAG_END if isinstance(value, ListOf) else TAG_END
            out.append(struct.pack(">bi", sub, 0))
            return
        sub = _infer(items[0])
        out.append(struct.pack(">bi", sub, len(items)))
        for item in items:
            _write_payload(out, sub, item)
    elif tag_id == TAG_COMPOUND:
        for key, item in value.items():
            sub = _infer(item)
            out.append(struct.pack(">b", sub))
            raw = key.encode("utf-8")
            out.append(struct.pack(">H", len(raw)))
            out.append(raw)
            _write_payload(out, sub, item)
        out.append(struct.pack(">b", TAG_END))
    elif tag_id == TAG_INT_ARRAY:
        values = value.values if isinstance(value, IntArray) else list(value)
        out.append(struct.pack(">i", len(values)))
        for v in values:
            out.append(struct.pack(">i", v))
    else:
        raise TypeError("不支持的 NBT 类型: %d" % tag_id)


def write_nbt(path, root, root_name=""):
    out = []
    out.append(struct.pack(">b", TAG_COMPOUND))
    raw = root_name.encode("utf-8")
    out.append(struct.pack(">H", len(raw)))
    out.append(raw)
    _write_payload(out, TAG_COMPOUND, root)
    data = b"".join(out)
    with gzip.GzipFile(filename="", mode="wb", fileobj=open(path, "wb"), mtime=0) as fh:
        fh.write(data)


def _read_payload(buf, pos, tag_id):
    if tag_id == TAG_BYTE:
        (v,) = struct.unpack_from(">b", buf, pos)
        return v, pos + 1
    if tag_id == TAG_SHORT:
        (v,) = struct.unpack_from(">h", buf, pos)
        return v, pos + 2
    if tag_id == TAG_INT:
        (v,) = struct.unpack_from(">i", buf, pos)
        return v, pos + 4
    if tag_id == TAG_LONG:
        (v,) = struct.unpack_from(">q", buf, pos)
        return v, pos + 8
    if tag_id == TAG_FLOAT:
        (v,) = struct.unpack_from(">f", buf, pos)
        return v, pos + 4
    if tag_id == TAG_DOUBLE:
        (v,) = struct.unpack_from(">d", buf, pos)
        return v, pos + 8
    if tag_id == TAG_BYTE_ARRAY:
        (n,) = struct.unpack_from(">i", buf, pos)
        return list(buf[pos + 4:pos + 4 + n]), pos + 4 + n
    if tag_id == TAG_STRING:
        (n,) = struct.unpack_from(">H", buf, pos)
        return buf[pos + 2:pos + 2 + n].decode("utf-8"), pos + 2 + n
    if tag_id == TAG_LIST:
        sub, n = struct.unpack_from(">bi", buf, pos)
        pos += 5
        items = []
        for _ in range(n):
            item, pos = _read_payload(buf, pos, sub)
            items.append(item)
        return items, pos
    if tag_id == TAG_COMPOUND:
        result = {}
        while True:
            (sub,) = struct.unpack_from(">b", buf, pos)
            pos += 1
            if sub == TAG_END:
                return result, pos
            (n,) = struct.unpack_from(">H", buf, pos)
            key = buf[pos + 2:pos + 2 + n].decode("utf-8")
            pos += 2 + n
            value, pos = _read_payload(buf, pos, sub)
            result[key] = value
    if tag_id == TAG_INT_ARRAY:
        (n,) = struct.unpack_from(">i", buf, pos)
        pos += 4
        values = list(struct.unpack_from(">%di" % n, buf, pos))
        return values, pos + 4 * n
    if tag_id == TAG_LONG_ARRAY:
        # 区块里 block_states.data 就是长整型数组
        (n,) = struct.unpack_from(">i", buf, pos)
        pos += 4
        # 用无符号读：方块数据是「按位打包」的，负数右移会带进符号位
        values = list(struct.unpack_from(">%dQ" % n, buf, pos))
        return values, pos + 8 * n
    raise TypeError("不支持的 NBT 类型: %d" % tag_id)


def read_nbt(path):
    with gzip.open(path, "rb") as fh:
        buf = fh.read()
    (root_id,) = struct.unpack_from(">b", buf, 0)
    if root_id != TAG_COMPOUND:
        raise ValueError("根标签不是 Compound：%s" % path)
    (n,) = struct.unpack_from(">H", buf, 1)
    pos = 3 + n
    value, _ = _read_payload(buf, pos, TAG_COMPOUND)
    return value
