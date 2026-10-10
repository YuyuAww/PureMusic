# PureMusic

PureMusic 是一个基于 **Jetpack Compose + Miuix + AndroidX Media3** 的 Android 本地音乐播放器。

## 特性

- **Miuix 风格界面**：使用 Miuix Compose 组件构建，Android 13+ 支持液态玻璃（liquid glass）效果。
- **本地音乐库**：通过 MediaStore 扫描本机音频，按歌曲 / 专辑 / 艺人 / 文件夹分类浏览，支持搜索与拼音索引。
- **播放**：基于 Media3（ExoPlayer + MediaSession）的高精度播放；内置 ffmpeg 解码器，支持 AAC / ALAC 等格式（另兼容 MP3、FLAC、M4A、WAV 等常见音频）。
- **歌单**：本地歌单管理，列表支持拖拽排序（reorderable）。
- **歌词**：解析并展示内嵌歌词（LRC），可配合外部歌词编辑器。
- **标签 / 封面**：基于 TagLib（JNA）读取音频标签与内嵌封面；通过外部编辑器（MusicTagEditor / Lyrico / LyricBox）修改标签与歌词。
- **设置**：睡眠定时器、播放模式、音质等；偏好与快照通过 DataStore + Kotlin 序列化持久化（不使用 Room）。
- **自定义背景 / 沉浸**：支持自定义应用背景与文件夹封面。
- **内存管理**：`FairMemoryManager` 统一管理封面等位图缓存。

## 技术栈

- 语言 / UI：Kotlin + Jetpack Compose（material3）
- 组件：Miuix Compose（`top.yukonga.miuix.kmp`：ui / icons / nav / preference / blur）
- 播放：AndroidX Media3（exoplayer + session + inspector + 高精度 AudioSink）
- 标签：kyant0 TagLib（JNA + JNI 动态库）
- 拼音：tinypinyin
- 排序：sh.calvin.reorderable
- 持久化：DataStore-Preferences + Kotlin 序列化 JSON 快照

## 构建

### 环境要求

- JDK 21
- Android SDK：`compileSdk 37` / `targetSdk 36` / `minSdk 28`
- 目标 ABI：`arm64-v8a`

### 命令

```bash
# 发布构建
./gradlew :app:assembleRelease
# 调试构建
./gradlew :app:assembleDebug
```

产物位于 `app/build/outputs/apk/`，发布构建还会额外生成带版本号的 `PureMusic_<versionName>.apk`。

> **关于两个已提交的二进制 AAR**（`app/libs/`）：
> - `media3-decoder-ffmpeg-1.11.0-ffmpeg9.0-arm64-v8a.aar` —— ffmpeg 解码器（AAC/ALAC 等）
> - `renderscript-toolkit-blur-344be3f-arm64-16k.aar` —— 模糊效果
>
> 这是源项目（Melox）自带的预编译依赖。若工具链对"将二进制依赖提交进 VCS"报警告，可将 `app/libs/*.aar` 加入 `.gitignore` 例外，或自行重新构建这些 AAR。

### 签名

Release 构建使用 `local.properties` 中的签名配置。若未配置，则构建**未签名**的 release APK（不会报错中断）；如需签名，写入：

```properties
puremusic.keystore.path=/path/to/keystore.jks
puremusic.store.password=***
puremusic.key.password=***
puremusic.key.alias=your-alias
```

## 来源与许可

本仓库代码派生自 [Melox](https://github.com/Inefy-03/Melox) 并重新实现为 PureMusic；Melox 本身未附带开源许可证，因此本仓库仅适合个人 / 私有项目使用，请勿在未获得原作者授权的情况下对外分发。