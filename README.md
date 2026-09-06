# PureMusic

PureMusic 是一款基于 Jetpack Compose 的 Android 本地音乐播放器。它从系统 `MediaStore` 读取设备上的音频文件，使用 Media3 在前台和后台播放，并将收藏、歌单、设置和最近播放记录保存到本地。

## 当前能力

- 扫描本地音乐，按歌曲、专辑和艺术家浏览
- 搜索歌曲，按标题、艺术家、专辑、添加时间和时长排序
- 播放、暂停、上一首、下一首和进度拖动
- 顺序、循环、单曲循环和随机播放
- 播放队列、最近播放记录和播放错误提示
- MediaSession 后台播放，支持通知栏、锁屏、蓝牙和耳机控制
- MediaStore 专辑封面 + Coil 加载，缺少封面时显示回退图标
- 收藏歌曲和自定义歌单
- 跟随系统、亮色和暗色主题，Android 12+ 支持动态取色
- Glance 桌面小组件，支持打开应用、播放/暂停和下一首
- 设置页提供系统音效/均衡器入口

## 技术栈

| 项目 | 当前配置 |
| --- | --- |
| 语言 | Kotlin 2.3.21 |
| 编译器 | Kotlin Compose Compiler Plugin 2.3.21 |
| UI | Jetpack Compose、Material 3，Compose BOM 2026.08.00 |
| 播放 | AndroidX Media3 ExoPlayer / MediaSession 1.11.0 |
| 数据库 | Room 2.8.4，KSP 2.3.11 |
| 设置 | DataStore Preferences 1.2.1 |
| 图片 | Coil Compose 3.6.0 |
| 小组件 | Glance AppWidget 1.2.0 |
| 异步 | Kotlin Coroutines 1.11.0 |
| 架构 | 单模块 MVVM + Repository |
| 构建 | AGP 9.4.0、Gradle 9.6.0、Java 11 |

## 应用规格

| 项目 | 值 |
| --- | --- |
| Application ID | `com.pure.music` |
| ABI | `arm64-v8a` |
| minSdk | 28（Android 9） |
| targetSdk | 35（Android 15） |
| compileSdk | 37 |
| versionName | `0.1.0` |

## 目录结构

```text
app/src/main/java/com/pure/music/
├── MainActivity.kt                 # Compose 入口、主题和页面状态
├── PureMusicApp.kt                 # Application 入口
├── data/                           # 音乐模型和 Room 数据库
├── library/                        # MediaStore 仓库和媒体库 ViewModel
├── player/                         # MediaController、播放服务和播放状态
├── settings/                       # DataStore 设置仓库和 ViewModel
├── ui/                             # Compose 页面、组件和主题
└── widget/                         # Glance 桌面小组件
```

## 播放和数据流程

1. `LibraryScreen` 请求音频读取权限。
2. `MediaLibraryRepository` 查询 `MediaStore.Audio`，并通过 `ContentObserver` 感知媒体库变化。
3. 用户首次播放时，`PlayerManager` 按需连接 `PlaybackService`。
4. `PlaybackService` 创建 ExoPlayer 和 MediaSession，系统通知栏和锁屏通过 MediaSession 控制。
5. 播放状态通过 `StateFlow` 返回 Compose UI；播放历史写入 Room。

## 权限

| 权限 | 用途 |
| --- | --- |
| `READ_MEDIA_AUDIO` | Android 13 及以上读取本地音频 |
| `READ_EXTERNAL_STORAGE` | Android 9 至 Android 12 读取本地音频 |
| `FOREGROUND_SERVICE` | 后台播放前台服务 |
| `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | 声明媒体播放服务类型 |
| `WAKE_LOCK` | 播放期间保持必要的 CPU 唤醒 |

项目不申请网络权限，封面和音频均来自本地设备。

## 构建

### Android Studio

使用支持 AGP 9.4 的 Android Studio 打开项目，等待 Gradle 同步后选择 `app` 模块运行或生成 APK。项目使用 Java 11 编译，CI 使用 JDK 17 运行 Gradle。

### 命令行

仓库包含 `gradlew` 和 `gradle-wrapper.properties`，但当前未提交 `gradle-wrapper.jar`。如果环境已经安装 Gradle 9.6.0，可以执行：

```bash
gradle assembleDebug
gradle assembleRelease
```

### GitHub Actions

工作流位于 `.github/workflows/build.yml`，在推送到 `main` 或手动触发时通过 SDK preview channel 安装 JDK 17、Gradle 9.6.0 和 Android SDK 37，执行 Release 构建并上传 APK Artifact。

Release 当前启用 R8，但没有配置正式签名密钥；产物适合 CI 验证，不适合作为正式商店发布包。

## 已知限制

- 只生成 `arm64-v8a`，普通 x86/x86_64 模拟器不能直接运行。
- 部分设备或音频文件没有可读取的 MediaStore 专辑封面，会显示回退图标。
- 当前页面导航使用 Compose 状态和返回键处理，尚未引入 Navigation Compose。
- 播放历史已保存，但首页尚未提供独立的“最近播放”列表。
- 均衡器入口跳转系统音效设置，不包含应用内均衡器。
- 当前没有正式 keystore、自动发布和崩溃收集配置。
- 本项目遵循仓库约定，不在本地执行编译验证；最终构建以 Android Studio 或 GitHub Actions 结果为准。

## 后续计划

1. 增加最近播放页面和播放位置恢复。
2. 补充播放服务、Room DAO 和关键 Compose 页面测试。
3. 配置正式签名、AAB 构建和发布流水线。
4. 根据设备兼容性补充音频元数据封面读取方案。
