# PureMusic

基于 Jetpack Compose 的 Android 本地音乐播放器。

## 功能

- **媒体库扫描** — MediaStore 查询本地音频，按歌曲/专辑/艺术家分类展示
- **播放控制** — 播放/暂停/上一首/下一首/进度拖动
- **播放模式** — 顺序播放 / 单曲循环 / 全部循环 / 随机播放
- **通知栏 + 锁屏控制** — MediaSession 集成
- **MiniPlayer + NowPlaying 全屏** — 底部迷你播放器 + 全屏播放界面（含播放队列）
- **收藏歌曲** — 收藏/取消收藏，收藏列表独立视图
- **自定义歌单** — 创建/删除歌单，添加/移除歌曲
- **专辑详情** — 专辑封面、曲目列表、播放全部
- **搜索** — 标题/艺术家/专辑关键词过滤
- **排序** — 按标题/艺术家/专辑/日期/时长排序
- **主题切换** — 跟随系统 / 亮色 / 暗色，支持 Android 12+ 动态取色
- **定时扫描** — WorkManager 每日自动扫描媒体库变化
- **桌面小部件** — Glance mini player，支持播放/暂停/下一首
- **系统均衡器** — 深链到系统音效设置

## 技术栈

| 项目 | 版本 |
|------|------|
| 语言 | Kotlin 2.0.21 |
| UI | Jetpack Compose + Material 3 (BOM 2024.09.03) |
| 播放器 | Media3 ExoPlayer + MediaSession (1.4.1) |
| 持久化 | Room (2.6.1) + DataStore Preferences (1.1.1) |
| 后台任务 | WorkManager (2.10.0) |
| 小部件 | Glance (1.1.1) |
| 架构 | MVVM + Repository |
| AGP | 8.7.3 |
| KSP | 2.0.21-1.0.27 |

## 规格

| 项目 | 值 |
|------|------|
| 包名 | `com.pure.music` |
| ABI | `arm64-v8a`（唯一） |
| minSdk | 28 (Android 9) |
| targetSdk / compileSdk | 35 (Android 15) |
| Java | 11 |
| 版本 | `0.1.0` (versionCode 1) |

## 项目结构

```
PureMusic/
├── .github/workflows/build.yml         # GitHub Actions CI
├── build.gradle.kts                    # 根构建脚本
├── settings.gradle.kts                 # 项目配置
├── gradle.properties                   # Gradle 属性
├── gradlew / gradlew.bat               # Gradle wrapper
├── .gitignore
├── gradle/
│   ├── libs.versions.toml              # 版本目录
│   └── wrapper/
│       └── gradle-wrapper.properties
├── app/
│   ├── build.gradle.kts                # 应用模块构建
│   ├── proguard-rules.pro              # R8 混淆规则
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/pure/music/
│       │   ├── MainActivity.kt         # 主入口，主题切换 + 界面组装
│       │   ├── PureMusicApp.kt         # Application，初始化播放器/WorkManager
│       │   ├── data/
│       │   │   ├── Models.kt           # Song, Album, Artist 数据模型
│       │   │   └── db/
│       │   │       ├── AppDatabase.kt  # Room 数据库单例
│       │   │       ├── Dao.kt          # FavoritesDao, PlaylistDao
│       │   │       └── Entities.kt     # FavoriteEntity, PlaylistEntity
│       │   ├── library/
│       │   │   ├── MediaLibraryRepository.kt  # MediaStore 查询 + ContentObserver
│       │   │   ├── LibraryViewModel.kt        # 媒体库 ViewModel
│       │   │   ├── FavoritesViewModel.kt      # 收藏 ViewModel
│       │   │   └── PlaylistViewModel.kt       # 歌单 ViewModel
│       │   ├── player/
│       │   │   ├── PlaybackState.kt    # 播放状态数据类
│       │   │   ├── PlayerManager.kt    # MediaController 管理单例
│       │   │   ├── PlayerViewModel.kt  # 播放 ViewModel
│       │   │   └── PlaybackService.kt  # MediaSessionService
│       │   ├── settings/
│       │   │   ├── SettingsRepository.kt    # DataStore 设置仓库
│       │   │   └── SettingsViewModel.kt     # 设置 ViewModel
│       │   ├── work/
│       │   │   └── LibraryScanWorker.kt     # 定时扫描 Worker
│       │   ├── widget/
│       │   │   ├── NowPlayingWidget.kt      # Glance 小部件
│       │   │   └── NowPlayingWidgetReceiver.kt  # 小部件动作接收器
│       │   └── ui/
│       │       ├── theme/
│       │       │   ├── Color.kt           # 品牌色板
│       │       │   ├── Type.kt            # 字体排版
│       │       │   └── Theme.kt           # M3 主题 + 动态取色
│       │       ├── library/
│       │       │   ├── LibraryScreen.kt   # 媒体库主界面（搜索/排序/标签/详情）
│       │       │   └── SongMenuDialog.kt  # 歌曲上下文菜单
│       │       ├── player/
│       │       │   ├── MiniPlayerBar.kt   # 底部迷你播放器栏
│       │       │   └── NowPlayingScreen.kt # 全屏正在播放界面
│       │       ├── album/
│       │       │   └── AlbumDetailScreen.kt # 专辑详情
│       │       ├── playlist/
│       │       │   └── PlaylistDetailScreen.kt # 歌单详情
│       │       └── settings/
│       │           └── SettingsScreen.kt  # 设置页面
│       └── res/
│           ├── drawable/ic_launcher_foreground.xml
│           ├── mipmap-anydpi-v26/ic_launcher.xml
│           ├── values/ (colors.xml, strings.xml, themes.xml)
│           └── xml/now_playing_widget.xml
└── README.md
```

## 构建

### 本地构建

```bash
# Debug 构建
./gradlew assembleDebug

# Release 构建（未签名，使用 debug 签名密钥）
./gradlew assembleRelease
```

> **注意**：`gradle-wrapper.jar` 未在仓库中，如需 `./gradlew` 请先运行 `gradle wrapper` 生成。

### CI 构建（GitHub Actions）

推送到 `main` 分支或手动触发，工作流将：

1. 安装 JDK 17 (Temurin) + Android SDK + Gradle 8.9
2. 执行 `gradle assembleRelease --no-daemon`
3. 上传 `app/build/outputs/apk/release/*.apk` 为 Artifact（保留 30 天）

工作流文件：`.github/workflows/build.yml`

## 权限

| 权限 | 用途 |
|------|------|
| `READ_MEDIA_AUDIO` | 读取本地音频文件 (Android 13+) |
| `READ_EXTERNAL_STORAGE` | 读取存储 (Android 9-12) |
| `FOREGROUND_SERVICE` | 后台播放前台服务 |
| `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | 媒体播放前台服务类型 |
| `WAKE_LOCK` | 播放时保持 CPU 唤醒 |

## 已知限制

- Android 10+ `Equalizer` API 已弃用，均衡器采用深链到系统音效设置方案。
- 仅支持 arm64-v8a，x86/x86_64 模拟器需 ARM 镜像。
- Release 构建未配置自定义签名密钥，使用 debug 签名密钥，APK 可直接安装调试。
- 封面图使用占位图标，未集成 Coil/Glide 图片加载库。
- 本项目本地不做编译验证，交付后请在 Android Studio 同步并构建。
