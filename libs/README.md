# libs/ 说明

这里放**第三方模组的编译期依赖**（不是本模组的构建产物；本模组的产物在各节点
`versions/<版本>/build/libs/` 下）。

| 文件 | 用途 | 上游 / 许可 |
| --- | --- | --- |
| `ponder-neoforge-1.0.82+mc1.21.1.jar` | Create 1.21.1 的 GUI 库（catnip），编译期需要其类签名 | [Create](https://github.com/Creators-of-Create/Create)（MIT） |
| `flywheel-neoforge-1.21.1-1.0.6.jar` | Create 的渲染库 | [Flywheel](https://github.com/Engine-Room/Flywheel)（MIT） |
| `Registrate-MC1.21-1.3.0+67.jar` | Create 的注册工具 | [Registrate](https://github.com/Registrate-MC/Registrate)（MIT） |
| `farmersdelight-1.20.4-1.2.4-beta.3.jar` | 农夫乐事没有官方 1.20.4 发行版，这份由 FD 官方 1.20.4 分支自行编译 | [Farmer's Delight](https://github.com/vectorwing/FarmersDelight)（MIT） |

这些 jar 只用于编译与开发运行，不会被打进本模组的发布包。
如果上游发布了对应版本，优先改回 `maven.modrinth:` 或官方 maven 依赖（见 `build.gradle`）。
