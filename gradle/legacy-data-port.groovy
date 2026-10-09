// 1.20.x（< 1.20.5）节点的数据包转换。
//
// 1.21 与 1.20.1 的数据包形态不同，这里在 processResources 复制完资源之后，就地把
// build/resources/main/data/** 改成 1.20.1 认识的样子：
//
//   1. 目录改名：advancement→advancements、loot_table→loot_tables、recipe→recipes、
//      structure→structures；标签目录 block/item/entity_type/fluid/game_event 改复数。
//   2. JSON 结构：1.21 的 ItemStack 写 "id"，1.20.1 的配方结果/容器/进度图标写 "item"。
//   3. 标签命名空间：1.21 的通用标签在 c:，1.20.1 在 forge:；几处别家模组改了名字的标签
//      见 legacyTagPathRenames（改名同时作用于配方里的引用和标签定义文件的路径，
//      否则会出现「配方引用 forge:raw_fishes、定义却停在 forge:foods/raw_fish」这种空配方）。
//   4. 条件/原料类型改名：neoforge:can_item_perform_ability → neoforge:can_tool_perform_action
//      （字段 ability → action）；neoforge:components 原料 → 普通物品原料（1.20.1 没有数据组件）。
//   5. 农夫乐事切菜板：1.20.x 的 result 直接是 ItemStack，tool 只收单个原料。
//   6. 掉落条件里 1.21 的 "entity": "attacker" 改回 1.20.x 的 "killer"；
//      物品谓词的 "items": "#tag" 改成 1.20.x 的 "tag": "tag"。
//
// 1.20.1 特有的是「NeoForge 名字 → Forge 名字」：
//   7. 战利品条件 / 配方条件 / 生物群系修改器类型：neoforge:* → forge:*
//      （neoforge:loot_table_id → forge:loot_table_id、neoforge:mod_loaded → forge:mod_loaded、
//        neoforge:add_features → forge:add_features……；工具动作条件统一成 field=action 的
//        forge:can_tool_perform_action）。
//   8. 注册表目录：data/<ns>/neoforge/biome_modifier → data/<ns>/forge/biome_modifier；
//      全局掉落修改器清单 data/neoforge/loot_modifiers/global_loot_modifiers.json
//      → data/forge/loot_modifiers/global_loot_modifiers.json（Forge 1.20.1 只读后者）。
//   9. 机械动力的配方形态（Create 6.0.8 / 1.20.1）：
//      流体原料去掉 "type": "neoforge:single" 包装，直接 {"amount":N,"fluid":X}；
//      输出里带 "amount" 的 "id" 写 "fluid"、不带的写 "item"；
//      sequenced_assembly 的 "transitional_item" 要写成驼峰 "transitionalItem"。
//  10. Forge 1.20.1 的配方不认顶层 forge:conditions，带条件的配方要整条包进
//      forge:conditional（否则条件被忽略、缺模组时配方会报解析错误）。
//  11. 1.20.1 没有数据表（data map）：data/neoforge/data_maps/** 丢掉，
//      燃烧时长改由 ModGameplayEvents 的 FurnaceFuelBurnTimeEvent 处理器给。
//
// 注意：标签改名表要和 Java 侧 ModTags（< 1.20.5 分支）保持一致。
// 已核对农夫乐事 1.20.1-1.3.4 的 jar：forge: 命名空间下的标签路径（bread、bread/wheat、
// dough/wheat、raw_fishes、cooked_fishes、tools/knives……）与转换结果一致。

import groovy.json.JsonOutput
import groovy.json.JsonSlurper

import java.nio.charset.StandardCharsets
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

ext.legacyTopRenames = [
        'advancement': 'advancements',
        'loot_table' : 'loot_tables',
        'recipe'     : 'recipes',
        'structure'  : 'structures',
]

ext.legacyTagDirRenames = [
        'block'      : 'blocks',
        'item'       : 'items',
        'entity_type': 'entity_types',
        'fluid'      : 'fluids',
        'game_event' : 'game_events',
]

// 1.21 的 c:<path> 在 1.20.1 里叫 forge:<改过的 path>（没列出来的就是同名）
ext.legacyTagPathRenames = [
        'foods/dough/wheat': 'dough/wheat',
        'foods/raw_fish'   : 'raw_fishes',
        'foods/raw_beef'   : 'raw_beef',
        'foods/raw_chicken': 'raw_chicken',
        'foods/raw_mutton' : 'raw_mutton',
        'foods/raw_pork'   : 'raw_pork',
        'foods/bread'      : 'bread',
        'tools/knife'      : 'tools/knives',
        'foods/fruits/pear': 'fruits/pear',
]

// 1.21 把战利品上下文里的实体目标改了名，1.20.1 还是老名字
//（LootContext.EntityTarget：1.20.1 = killer/direct_killer/killer_player）
ext.legacyEntityTargetRenames = [
        'attacker'        : 'killer',
        'direct_attacker' : 'direct_killer',
        'attacking_player': 'killer_player',
]

// ===== 1.20.1（Forge 47.1）和 1.21.1（NeoForge）的注册名差异 =====
// 1.20.1 的 Forge 没有 neoforge: 这一套名字，对应的都挂在 forge: 下。
// 下面两张表把 1.21 的 neoforge: 名字映射到 1.20.1 Forge 的 forge:。
ext.legacy1201ConditionRenames = [
        'neoforge:loot_table_id'          : 'forge:loot_table_id',
        'neoforge:can_tool_perform_action': 'forge:can_tool_perform_action',
        'neoforge:mod_loaded'             : 'forge:mod_loaded',
]

// 生物群系修改器的 type：1.21 是 neoforge:add_features，1.20.1 Forge 是 forge:add_features
ext.legacy1201TypeRenames = [
        'neoforge:add_features': 'forge:add_features',
]

/** c:xxx 形式的标签 id → 1.20.1 的 forge:xxx */
ext.legacyRemapTagId = { String id ->
    if (!id.startsWith('c:')) {
        return id
    }
    def rest = id.substring(2)
    return 'forge:' + (legacyTagPathRenames.get(rest) ?: rest)
}

/** #c:xxx 形式的标签引用 → #forge:xxx */
ext.legacyRemapTagRef = { String ref ->
    return ref.startsWith('#c:') ? '#' + legacyRemapTagId(ref.substring(1)) : ref
}

/**
 * Groovy 的 JsonOutput 会把非 ASCII 字符转义成 \uXXXX（中文注释就全变乱码了），这里还原回来。
 * 数据文件里本来没有反斜杠（已核对），所以看到 \uXXXX 就一定是 JsonOutput 转义的。
 */
ext.legacyUnescapeUnicode = { String json ->
    if (!json.contains('\\u')) {
        return json
    }
    def sb = new StringBuilder(json.length())
    int i = 0
    while (i < json.length()) {
        char c = json.charAt(i)
        if (c == '\\' && i + 5 < json.length() && json.charAt(i + 1) == 'u') {
            def hex = json.substring(i + 2, i + 6)
            if (hex ==~ /[0-9a-fA-F]{4}/) {
                int cp = Integer.parseInt(hex, 16)
                if (cp >= 0x80) {
                    sb.append((char) cp)
                    i += 6
                    continue
                }
            }
        }
        sb.append(c)
        i++
    }
    return sb.toString()
}

/** 递归改写一个 JSON 节点；env.changed 记录内容有没有真的动过 */
ext.legacyTransformNode = { Object node, Map env ->
    if (node instanceof Map) {
        def map = node as Map
        // 1.20.1 没有数据组件：neoforge:components 原料退化成普通物品原料
        if (map.get('type') == 'neoforge:components') {
            def items = map.get('items')
            def one = items instanceof List ? items.get(0) : items
            env.changed = true
            return ['item': one]
        }
        def out = new LinkedHashMap()
        // 1.20.1 的 Create 流体原料没有 "type" 包装：
        //   {"type":"neoforge:single","amount":N,"fluid":X} → {"amount":N,"fluid":X}
        // 1.21 的流体标签写法同理退化成 1.20.1 的 "fluidTag"
        if (map.get('type') == 'neoforge:single') {
            env.changed = true
            map = new LinkedHashMap(map)
            map.remove('type')
        } else if (map.get('type') == 'neoforge:tag') {
            env.changed = true
            map = new LinkedHashMap(map)
            map.remove('type')
            def tagId = map.remove('tag')
            out.put('fluidTag', tagId)
        }
        // 工具动作条件：1.20.1 的 Forge 叫 forge:can_tool_perform_action（字段 action）
        if (map.get('condition') == 'neoforge:can_item_perform_ability') {
            env.changed = true
            map = new LinkedHashMap(map)
            map.remove('condition')
            def action = map.remove('ability')
            out.put('condition', 'forge:can_tool_perform_action')
            out.put('action', action)
        }
        map.each { Object k, Object v ->
            def key = k as String
            // 1.20.1：注册名从 neoforge: 挪到 forge:
            if (key == 'condition' && v instanceof String && legacy1201ConditionRenames.containsKey(v)) {
                env.changed = true
                out.put(key, legacy1201ConditionRenames[v])
                return
            }
            // 配方/战利品里的条件对象也用 "type"（如 {"type":"neoforge:mod_loaded"}），
            // 所以两张表都要看
            if (key == 'type' && v instanceof String
                    && (legacy1201TypeRenames.containsKey(v) || legacy1201ConditionRenames.containsKey(v))) {
                env.changed = true
                out.put(key, legacy1201TypeRenames.get(v) ?: legacy1201ConditionRenames[v])
                return
            }
            // 1.20.1 的配方条件键叫 forge:conditions（1.21 才是 neoforge:conditions）
            if (key == 'neoforge:conditions') {
                env.changed = true
                out.put('forge:conditions', legacyTransformNode(v, env))
                return
            }
            // 1.20.1 的 create:sequenced_assembly 要的是驼峰写法 transitionalItem
            if (env.recipe && key == 'transitional_item') {
                env.changed = true
                out.put('transitionalItem', legacyTransformNode(v, env))
                return
            }
            // 1.20.x 的 ItemPredicate 还没有并成 HolderSet：标签要写进单独的 "tag" 字段，
            // 而且 TagKey.codec 不带 "#" 前缀（1.21 是 "items": "#tag" 写在一起）。
            // 不转的话 1.20.1 会报 "Not a json array"，整条掉落修改器被丢掉。
            if (key == 'items' && v instanceof String) {
                def s = v as String
                env.changed = true
                if (s.startsWith('#')) {
                    out.put('tag', legacyRemapTagId(s.substring(1)))
                } else {
                    out.put('items', [s])
                }
                return
            }
            // 掉落条件里的 "entity": "attacker" → "killer"
            if (key == 'entity' && v instanceof String && legacyEntityTargetRenames.containsKey(v)) {
                env.changed = true
                out.put(key, legacyEntityTargetRenames[v])
                return
            }
            // 配方里的 "id" 只出现在 ItemStack / FluidStack 位置（result / results[] / container / transitional_item）。
            // 1.20.x 的 Create 用 "item" 表示物品输出、用 "fluid" 表示流体输出，靠同一层里有没有 "amount" 区分
            if (env.recipe && key == 'id' && v instanceof String) {
                out.put(map.containsKey('amount') ? 'fluid' : 'item', v)
                env.changed = true
                return
            }
            // 进度图标：1.20.1 的 display.icon 用 "item"
            if (env.advancement && key == 'icon' && v instanceof Map && (v as Map).containsKey('id')) {
                def icon = new LinkedHashMap(v as Map)
                def iconId = icon.remove('id')
                icon.put('item', iconId)
                env.changed = true
                out.put(key, legacyTransformNode(icon, env))
                return
            }
            // {"tag": "c:xxx"} → {"tag": "forge:xxx"}
            if (key == 'structures' && v instanceof String) {
                env.changed = true
                out.put('structure', v)
                return
            }
            // 进度条件的 player 简写：1.21 允许直接写实体谓词对象
            //（EntityPredicate.ADVANCEMENT_CODEC = withAlternative(ContextAwarePredicate.CODEC, CODEC, wrap)），
            // 但 1.20.x 的 ContextAwarePredicate.fromJson 只认 JsonArray：拿到对象直接返回 null，
            // player 谓词被静默丢掉 ——「没有 player 限制」= 谁都满足，
            // minecraft:location 这类进度就会一进游戏立刻弹（2026-10-09 玩家实测「快乐老家」）。
            // 统一展开成官方写法（1.20.1 / 1.21 的 vanilla adventuring_time.json 就是这么写的）：
            //   [{"condition": "minecraft:entity_properties", "entity": "this", "predicate": <原对象>}]
            if (env.advancement && key == 'player' && v instanceof Map) {
                env.changed = true
                out.put(key, [[
                        'condition': 'minecraft:entity_properties',
                        'entity'   : 'this',
                        'predicate': legacyTransformNode(v, env),
                ]])
                return
            }
            if (key == 'tag' && v instanceof String) {
                def mapped = legacyRemapTagId(v as String)
                if (mapped != v) {
                    env.changed = true
                }
                out.put(key, mapped)
                return
            }
            out.put(key, legacyTransformNode(v, env))
        }
        // 农夫乐事切菜板：1.20.x 的 result 直接是 ItemStack，tool 只收单个原料
        if (map.get('type') == 'farmersdelight:cutting') {
            def result = out.get('result')
            if (result instanceof List) {
                out.put('result', result.collect { Object entry ->
                    if (entry instanceof Map) {
                        def stack = (entry as Map).get('item')
                        if (stack instanceof Map) {
                            def merged = new LinkedHashMap(stack as Map)
                            (entry as Map).each { Object k2, Object v2 -> if (k2 != 'item') merged.put(k2, v2) }
                            env.changed = true
                            return merged
                        }
                    }
                    return entry
                })
            }
            def tool = out.get('tool')
            if (tool instanceof List) {
                out.put('tool', tool.find { it instanceof Map && (it as Map).containsKey('tag') } ?: tool.get(0))
                env.changed = true
            }
        }
        return out
    }
    if (node instanceof List) {
        return node.collect { legacyTransformNode(it, env) }
    }
    if (node instanceof String) {
        def s = node as String
        if (s.startsWith('#c:')) {
            env.changed = true
            return legacyRemapTagRef(s)
        }
        return s
    }
    return node
}

/**
 * 把 build/resources/main/data 就地转成 1.20.1 形态。
 */
ext.portLegacyDataTree = { File resourcesDir ->
    File dataDir = new File(resourcesDir, 'data')
    if (!dataDir.isDirectory()) {
        return
    }
    def slurper = new JsonSlurper()
    def stats = [renamed: 0, rewritten: 0, dropped: 0, nbt: 0]
    def files = []
    dataDir.eachFileRecurse { File f -> if (f.isFile()) files << f }
    files.each { File src ->
        def rel = dataDir.toPath().relativize(src.toPath()).toString().replace('\\', '/')
        def parts = rel.split('/') as List
        if (parts.size() < 2) {
            return
        }
        def ns = parts[0] as String
        def rest = new ArrayList<String>(parts.subList(1, parts.size()))
        def isTagFile = rest.size() > 1 && rest[0] == 'tags'

        // ---- 目录改名 ----
        if (isTagFile && legacyTagDirRenames.containsKey(rest[1])) {
            rest[1] = legacyTagDirRenames[rest[1]]
        } else if (legacyTopRenames.containsKey(rest[0])) {
            rest[0] = legacyTopRenames[rest[0]]
        }
        if (ns == 'c' && isTagFile) {
            ns = 'forge'      // 通用标签整体挪到 forge: 命名空间
            // 标签 id 改过名的（foods/raw_fish → raw_fishes 之类），定义文件的路径也要跟着挪：
            // 只改引用不改定义的话，那一格匹配不到任何东西，JEI 里就是一条空配方。
            if (rest.size() > 2 && rest[rest.size() - 1].endsWith('.json')) {
                def fileName = rest.remove(rest.size() - 1)
                def stem = fileName.substring(0, fileName.length() - 5)
                def renamed = legacyTagPathRenames.get((rest.subList(2, rest.size()) + [stem]).join('/'))
                if (renamed != null) {
                    rest.subList(2, rest.size()).clear()
                    def segments = renamed.split('/') as List
                    // renamed 的最后一段就是新的文件名，别忘了补回 .json
                    segments[segments.size() - 1] = segments[segments.size() - 1] + '.json'
                    rest.addAll(segments)
                } else {
                    rest.add(fileName)
                }
            }
        }
        // 1.20.1 的注册表命名空间也是 forge:（neoforge: 是 1.21 才改的名）：
        //   data/<ns>/neoforge/biome_modifier/*.json → data/<ns>/forge/biome_modifier/*.json
        //   全局掉落修改器的清单必须是 forge:loot_modifiers/global_loot_modifiers.json
        if (rest.size() > 1 && rest[0] == 'neoforge' && rest[1] == 'biome_modifier') {
            rest[0] = 'forge'
        } else if (ns == 'neoforge' && rest[0] == 'loot_modifiers') {
            ns = 'forge'
        }
        def newRel = ([ns] + rest).join('/')

        // ---- 整组丢弃：1.20.1 的 Forge 没有数据表（data map）这一套（见文件头第 11 条） ----
        if (rel.startsWith('neoforge/data_maps/')) {
            src.delete()
            stats.dropped++
            return
        }

        // ---- JSON 内容改写 ----
        def env = [changed: false, recipe: newRel.contains('/recipes/'), advancement: newRel.contains('/advancements/')]
        if (rel.endsWith('.json')) {
            def root = slurper.parse(src, 'UTF-8')
            def rewritten = legacyTransformNode(root, env)
            // Forge 1.20.1 只在 forge:conditional 这个序列化器里支持配方条件，
            // 顶层 forge:conditions 会被当成无关字段忽略（缺模组时配方直接报错）。
            // 所以带条件的配方整条包一层：
            //   {"type":"forge:conditional","recipes":[{"conditions":[...],"recipe":{原配方}}]}
            if (env.recipe && rewritten instanceof Map
                    && (rewritten as Map).containsKey('forge:conditions')) {
                def wrapper = new LinkedHashMap(rewritten as Map)
                def recipeCondition = wrapper.remove('forge:conditions')
                rewritten = ['type'   : 'forge:conditional',
                             'recipes': [['conditions': recipeCondition, 'recipe': wrapper]]]
                env.changed = true
            }
            if (env.changed) {
                def out = new File(dataDir, newRel)
                out.parentFile.mkdirs()
                def text = legacyUnescapeUnicode(JsonOutput.prettyPrint(JsonOutput.toJson(rewritten)))
                out.setText(text + System.lineSeparator(), 'UTF-8')
                if (out.canonicalPath != src.canonicalPath) {
                    src.delete()
                }
                stats.rewritten++
                return
            }
        }
        // ---- 结构 NBT：物品堆格式回退（第 14 条） ----
        if (rel.endsWith('.nbt')) {
            if (legacyPortStructureNbt(src, 3465)) {
                stats.nbt++
            }
        }
        if (newRel != rel) {
            def out = new File(dataDir, newRel)
            out.parentFile.mkdirs()
            java.nio.file.Files.move(src.toPath(), out.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING)
            stats.renamed++
        }
    }
    // 收拾改名后留下的空目录
    def dirs = []
    dataDir.eachDirRecurse { File d -> dirs << d }
    dirs.sort { a, b -> b.path.length() <=> a.path.length() }
    dirs.each { File d -> if (!d.list() || d.list().length == 0) { d.delete() } }
    println "[legacy-data] 1.20.1 数据包转换完成：改名 ${stats.renamed}、改写 ${stats.rewritten}、丢弃 ${stats.dropped}、结构 NBT ${stats.nbt}"
}
//  12. 位置谓词：1.21 是 "structures"（HolderSet），1.20.x 只有 "structure"（单个 id）。
//      不改名的话旧版把它当未知字段忽略 → 条件退化成「任何位置都满足」，进度一进游戏就弹。
//  13. 进度条件里的 player 简写（1.21 的 EntityPredicate.ADVANCEMENT_CODEC 写法）展开成
//      [{"condition":"minecraft:entity_properties","entity":"this","predicate":<原对象>}]。
//      1.20.x 的 ContextAwarePredicate.fromJson 只认 JsonArray，给对象会静默返回 null，
//      player 谓词等于「无限制」，minecraft:location 进度一进游戏就弹
//      （2026-10-09 玩家实测「快乐老家」；vanilla 1.20.1 / 1.21 的 adventuring_time.json 是同一写法）。
//  14. 结构 NBT（data/<ns>/structures/**.nbt，gzip 二进制）里的物品堆格式回退：
//      1.20.5+ 写 {"id":..., "count":N[,"components":...]}，1.20.x 只认 {"id":..., "Count":Nb[,"tag":...]}。
//      不改的话伪装方块（create:copycat_step / copycat_panel）会「缺材质」：
//      它们的 Item 在 1.20.x 上读出来是空堆 → Create 6.0 读取时材质校验通不过 → Material 被重置成空。
//      顺带把 DataVersion 改成 1.20.1 的 3465。

/**
 * 结构 NBT 的物品堆格式回退（文件头第 14 条）。返回是否发生改写，文件就地更新（保持原 gzip/明文形态）。
 * 判定：任何一个「id 是字符串、count 是整数」的 compound 都当成物品堆（伪装方块的 Item、容器 Items 的元素……）。
 */
ext.legacyPortStructureNbt = { File file, int targetDataVersion ->
    byte[] raw = file.bytes
    boolean gzipped = raw.length > 2 && (raw[0] & 0xFF) == 0x1F && (raw[1] & 0xFF) == 0x8B
    byte[] plain
    if (gzipped) {
        def gzipIn = new GZIPInputStream(new ByteArrayInputStream(raw))
        try {
            plain = gzipIn.bytes
        } finally {
            gzipIn.close()
        }
    } else {
        plain = raw
    }

    def root = LegacyNbtCodec.readRoot(plain)
    def changed = false

    def versionTag = ((Map) root.value).get('DataVersion')
    if (versionTag != null && versionTag.type == LegacyNbtCodec.INT
            && (versionTag.value as int) != targetDataVersion) {
        versionTag.value = targetDataVersion
        changed = true
    }

    def stacks = 0
    def oversized = 0
    LegacyNbtCodec.eachTag(root) { LegacyNbtTag tag ->
        if (tag.type == LegacyNbtCodec.COMPOUND) {
            Map body = (Map) tag.value
            def idTag = body.get('id')
            def countTag = body.get('count')
            if (idTag != null && countTag != null && idTag.type == LegacyNbtCodec.STRING
                    && countTag.type == LegacyNbtCodec.INT) {
                int count = countTag.value as int
                if (count > 127) {
                    oversized++
                    count = 127
                }
                body.remove('count')
                body.put('Count', new LegacyNbtTag(LegacyNbtCodec.BYTE, Math.max(0, count)))
                stacks++
                changed = true
            }
        }
    }
    if (!changed) {
        return false
    }

    byte[] out = LegacyNbtCodec.writeRoot(root)
    if (gzipped) {
        def buffer = new ByteArrayOutputStream()
        def gzipOut = new GZIPOutputStream(buffer)
        try {
            gzipOut.write(out)
            gzipOut.finish()
        } finally {
            gzipOut.close()
        }
        out = buffer.toByteArray()
    }
    file.bytes = out
    if (oversized > 0) {
        println "[legacy-data] 警告：${file.name} 里有 ${oversized} 个物品堆数量超过 127，已按 127 截断"
    }
    return true
}

class LegacyNbtTag {
    int type
    Object value

    LegacyNbtTag(int type, Object value) {
        this.type = type
        this.value = value
    }
}

/** 只够本脚本用的最小 NBT 读写（保留全部标签类型，读进来什么样、写回去什么样）。 */
class LegacyNbtCodec {
    static final int END = 0
    static final int BYTE = 1
    static final int SHORT = 2
    static final int INT = 3
    static final int LONG = 4
    static final int FLOAT = 5
    static final int DOUBLE = 6
    static final int BYTE_ARRAY = 7
    static final int STRING = 8
    static final int LIST = 9
    static final int COMPOUND = 10
    static final int INT_ARRAY = 11
    static final int LONG_ARRAY = 12

    static LegacyNbtTag readRoot(byte[] data) {
        def input = new DataInputStream(new ByteArrayInputStream(data))
        int type = input.readUnsignedByte()
        if (type != COMPOUND) {
            throw new IOException("结构 NBT 的根标签不是 compound（type=${type}）")
        }
        readUtf(input)
        return new LegacyNbtTag(COMPOUND, readCompoundBody(input))
    }

    static byte[] writeRoot(LegacyNbtTag root) {
        def buffer = new ByteArrayOutputStream()
        def output = new DataOutputStream(buffer)
        output.writeByte(COMPOUND)
        writeUtf(output, '')
        writeCompoundBody(output, (Map) root.value)
        output.flush()
        return buffer.toByteArray()
    }

    static void eachTag(LegacyNbtTag tag, Closure visitor) {
        visitor.call(tag)
        if (tag.type == COMPOUND) {
            for (Object child : ((Map) tag.value).values()) {
                eachTag((LegacyNbtTag) child, visitor)
            }
        } else if (tag.type == LIST) {
            for (Object child : (List) ((List) tag.value).get(1)) {
                eachTag((LegacyNbtTag) child, visitor)
            }
        }
    }

    private static String readUtf(DataInputStream input) {
        int length = input.readUnsignedShort()
        byte[] bytes = new byte[length]
        input.readFully(bytes)
        return new String(bytes, StandardCharsets.UTF_8)
    }

    private static void writeUtf(DataOutputStream output, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8)
        output.writeShort(bytes.length)
        output.write(bytes)
    }

    private static Map<String, LegacyNbtTag> readCompoundBody(DataInputStream input) {
        Map<String, LegacyNbtTag> body = new LinkedHashMap<>()
        boolean done = false
        while (!done) {
            int type = input.readUnsignedByte()
            if (type == END) {
                done = true
            } else {
                String name = readUtf(input)
                body.put(name, new LegacyNbtTag(type, readPayload(input, type)))
            }
        }
        return body
    }

    private static Object readPayload(DataInputStream input, int type) {
        switch (type) {
            case BYTE: return (int) input.readByte()
            case SHORT: return (int) input.readShort()
            case INT: return input.readInt()
            case LONG: return input.readLong()
            case FLOAT: return input.readFloat()
            case DOUBLE: return input.readDouble()
            case BYTE_ARRAY:
                int byteLength = input.readInt()
                byte[] byteValues = new byte[byteLength]
                input.readFully(byteValues)
                return byteValues
            case STRING: return readUtf(input)
            case LIST:
                int elementType = input.readUnsignedByte()
                int listLength = input.readInt()
                List<LegacyNbtTag> elements = new ArrayList<>(listLength)
                for (int i = 0; i < listLength; i++) {
                    elements.add(new LegacyNbtTag(elementType, readPayload(input, elementType)))
                }
                return [elementType, elements]
            case COMPOUND: return readCompoundBody(input)
            case INT_ARRAY:
                int intLength = input.readInt()
                int[] intValues = new int[intLength]
                for (int i = 0; i < intLength; i++) {
                    intValues[i] = input.readInt()
                }
                return intValues
            case LONG_ARRAY:
                int longLength = input.readInt()
                long[] longValues = new long[longLength]
                for (int i = 0; i < longLength; i++) {
                    longValues[i] = input.readLong()
                }
                return longValues
            default: throw new IOException("未知的 NBT 标签类型：${type}")
        }
    }

    private static void writeCompoundBody(DataOutputStream output, Map body) {
        for (Object entryObject : body.entrySet()) {
            Map.Entry entry = (Map.Entry) entryObject
            LegacyNbtTag tag = (LegacyNbtTag) entry.value
            output.writeByte(tag.type)
            writeUtf(output, (String) entry.key)
            writePayload(output, tag)
        }
        output.writeByte(END)
    }

    private static void writePayload(DataOutputStream output, LegacyNbtTag tag) {
        switch (tag.type) {
            case BYTE: output.writeByte((Integer) tag.value); break
            case SHORT: output.writeShort((Integer) tag.value); break
            case INT: output.writeInt((Integer) tag.value); break
            case LONG: output.writeLong((Long) tag.value); break
            case FLOAT: output.writeFloat((Float) tag.value); break
            case DOUBLE: output.writeDouble((Double) tag.value); break
            case BYTE_ARRAY:
                byte[] byteValues = (byte[]) tag.value
                output.writeInt(byteValues.length)
                output.write(byteValues)
                break
            case STRING: writeUtf(output, (String) tag.value); break
            case LIST:
                List list = (List) tag.value
                int elementType = (int) list.get(0)
                List<LegacyNbtTag> elements = (List<LegacyNbtTag>) list.get(1)
                output.writeByte(elementType)
                output.writeInt(elements.size())
                for (LegacyNbtTag element : elements) {
                    writePayload(output, element)
                }
                break
            case COMPOUND: writeCompoundBody(output, (Map) tag.value); break
            case INT_ARRAY:
                int[] intValues = (int[]) tag.value
                output.writeInt(intValues.length)
                for (int value : intValues) {
                    output.writeInt(value)
                }
                break
            case LONG_ARRAY:
                long[] longValues = (long[]) tag.value
                output.writeInt(longValues.length)
                for (long value : longValues) {
                    output.writeLong(value)
                }
                break
            default: throw new IOException("未知的 NBT 标签类型：${tag.type}")
        }
    }
}
