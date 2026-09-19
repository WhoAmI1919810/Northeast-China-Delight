# logs 目录约定

这里专门存放**启动 Minecraft（开发运行）时产生的各种日志**，不要写到 Codex 的工作目录（`E:\codex`）里。

| 文件 | 内容 |
| --- | --- |
| `client-<版本>-<时间戳>.log` | Gradle + 启动器 + 游戏的控制台完整输出 |
| `minecraft-<版本>-<时间戳>.log` | 游戏自身的 `latest.log`，退出时自动归档一份（`latest.log` 下次启动会被覆盖） |

启动方式统一走 `tools/start_client.ps1`（会自动建目录、写日志、退出后归档游戏日志）：

```powershell
pwsh -File tools\start_client.ps1
```

另外，游戏目录仍然是 `run/<游戏版本>/`（存档、配置都在那里），游戏自身也会在
`run/<游戏版本>/logs/` 写一份 `latest.log` —— 它同样在工程目录内，不在 Codex 工作目录。

日志文件不提交到 git（见根目录 `.gitignore`）。
