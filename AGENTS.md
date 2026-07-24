# Repository Guidelines

## 项目结构

这是一个单模块 Android/Kotlin 工程。`AiyifanApp.kt` 负责应用初始化，`app/src/main/java/com/aiyifan/app` 包含按功能划分的 `feature/<name>` 页面，以及共享的 `core/data`、`core/model` 和 `core/ui`。XML 布局、Drawable、菜单和值资源位于 `app/src/main/res`；本地 JUnit 4 测试位于 `app/src/test`；产品分析、设计和路线图放在 `docs/`。

## 构建、测试与本地开发

在 Windows 上使用 Gradle Wrapper：

```powershell
.\gradlew.bat assembleDebug  # 编译调试 APK，输出在 app/build/outputs/apk/debug/
.\gradlew.bat test           # 运行本地单元测试，报告在 app/build/reports/tests/
.\gradlew.bat lint           # 运行 Android Lint，报告在 app/build/reports/
```

提交前至少执行与改动相关的测试；涉及界面或资源时同时运行 `lint`。项目使用 Java 17、Android SDK 36 和 View Binding。

## 代码与资源规范

遵循 Kotlin 官方代码风格和 4 空格缩进。类、Activity、Fragment 与 Adapter 使用 PascalCase；函数、变量使用 camelCase；Android 资源使用 `snake_case`，例如 `item_video_card.xml`、`bg_button_primary.xml`。新功能放入对应 `feature/<name>` 包，共享逻辑放入职责匹配的 `core/<layer>`，避免跨层直接依赖。

## 测试规范

使用 JUnit 4，并将测试放在与生产包结构对应的 `app/src/test` 下。测试名使用描述性反引号形式，例如：

```kotlin
fun `resolver falls back to default config when country file is missing`()
```

新增数据、解析或状态逻辑时，覆盖正常流程及失败、空值或回退流程。为降低回归风险，提交前检查空状态、网络异常和配置缺失等边界条件。测试范围还应包括：输入、输出、状态、错误、回退和持久化。

## 提交与 Pull Request

沿用已有提交习惯：使用 `feat(app): ...`、`docs: ...` 等 Conventional Commit 前缀，或简短清晰的中文摘要。每个提交保持单一目的。PR 应说明改动目的、执行过的验证命令和关联问题；修改布局、颜色、图标或交互时附模拟器截图，并点明测试设备或 API 级别。

## 资源与配置

图标优先使用 [Iconfont](https://www.iconfont.cn/) 或 [Material Symbols Rounded](https://fonts.google.com/icons?icon.query=music&icon.style=Rounded)，并确认许可证与现有视觉风格兼容。不要提交 `local.properties`、构建产物、设备文件或任何新增密钥、令牌和账户凭据。
