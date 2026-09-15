# PureMusic

PureMusic 是一款面向 Android 的本地音乐播放器，使用 Jetpack Compose 构建界面，Media3 负责播放，Room 保存本地库和用户数据。应用不申请网络权限，音乐、标签和封面均来自设备本地文件。

## 功能

- 扫描 `MediaStore` 音频库，按歌曲、专辑、艺术家和文件夹浏览
- 搜索、排序、收藏、自定义歌单和播放历史
- 播放/暂停、上一首、下一首、拖动进度、队列管理
- 顺序、随机、列表循环和单曲循环
- MediaSession 后台播放，支持通知栏、锁屏、蓝牙和耳机控制
- MediaStore 专辑封面与 Coil 加载，缺少封面时显示回退图标
- 跟随系统、亮色和暗色主题；Android 12+ 支持动态取色
- Glance 桌面小组件和系统均衡器入口

## 音频标签与歌词

独立的 `:taglib` Android Library 模块封装 TagLib 2.3.2，通过 JNI 读取 MP3、FLAC、OGG/Opus、M4A/AAC、WAV 等格式的内置信息：标题、艺术家、专辑、曲目号、时长、码率、采样率、声道数、歌词（`LYRICS`）、作曲家（`COMPOSER`）和流派（`GENRE`）。

歌词页和封面页的迷你歌词窗只显示音频内嵌歌词；没有歌词时显示“暂无内嵌歌词”，不会填充虚构歌词。详情页展示读取到的技术参数及扩展标签。TagLib 不可用、路径不可访问或标签缺失时，应用回退到 MediaStore 信息。

## 技术栈

| 项目 | 配置 |
| --- | --- |
| Kotlin / Compose | Kotlin 2.3.21、Compose BOM 2026.08.00 |
| 播放 | AndroidX Media3 1.11.0 |
| 数据 | Room 2.8.4、DataStore Preferences 1.2.1 |
| 图片 / 小组件 | Coil 3.6.0、Glance 1.2.0 |
| 原生标签 | TagLib 2.3.2、CMake、JNI |
| 构建 | AGP 9.4.0、Gradle 9.6.0、Java 11 |

## 模块与数据流

```text
PureMusic/
├── app/       # 主应用、媒体库、播放器、Compose UI、Room、设置和小组件
└── taglib/    # TagLib 原生库、JNI 和 AudioMetadata 封装
```

媒体库流程：`MediaStore` 查询 → `:taglib` 解析内置标签 → 合并并缓存到 Room → `StateFlow` 更新界面。播放由 `PlayerManager` 连接 `PlaybackService`，通过 MediaSession 暴露系统控制。

## 应用规格与权限

- Application ID：`com.pure.music`
- minSdk 28（Android 9），targetSdk 35，compileSdk 37
- 当前仅打包 `arm64-v8a`
- Android 13+ 使用 `READ_MEDIA_AUDIO`，Android 9–12 使用 `READ_EXTERNAL_STORAGE`
- 后台播放使用前台媒体服务和必要的唤醒锁权限

## 构建

使用支持 AGP 9.4 的 Android Studio 打开项目，等待 Gradle 同步后运行 `app`。命令行可执行：

```bash
gradle :app:assembleDebug
gradle :app:assembleRelease
```

首次构建 `:taglib` 时，CMake 会从 TagLib Git 仓库获取 2.3.2 源码。Release 启用 R8，但未配置正式签名密钥，产物仅用于验证。

## 已知限制

- 仅 arm64 模拟器/设备可直接运行，x86/x86_64 需要调整 ABI 配置
- 受限存储场景下可能无法取得真实文件路径，此时仅使用 MediaStore 元数据
- 当前歌词按文本行展示，不包含逐行时间轴同步
- 内嵌封面尚未直接从 TagLib 提取，封面仍使用 MediaStore 专辑封面 URI
- 暂无应用内均衡器、崩溃收集和正式发布签名配置

## 后续计划

- 支持内嵌封面和 LRC 时间轴歌词
- 完善多 ABI / APK 拆分与构建缓存
- 增加播放器、媒体库和 Compose UI 测试
- 配置正式签名、AAB 构建和发布流程
