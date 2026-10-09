# libs/ 说明

本分支只构建 1.20.1：第三方依赖（农夫乐事、机械动力及其 Ponder / Flywheel / Registrate、
玉、JEI）全部通过 maven 获取（见 `build.gradle`），这里不放本地 jar。

1.21.1 的 Create 编译期依赖 jar（ponder / flywheel / Registrate）在 main 分支的 `libs/` 下。

本模组的构建产物（`northeast_china_delight-*.jar`）会被构建任务自动拷到这里，已被 `.gitignore` 忽略。
