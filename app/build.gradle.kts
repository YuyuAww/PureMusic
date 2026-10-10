import java.util.Properties
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

val localPropertiesFile = rootProject.file("local.properties")
val localProperties = Properties().apply {
    if (localPropertiesFile.isFile) {
        localPropertiesFile.inputStream().use { input ->
            load(input)
        }
    }
}

val releaseKeystorePath = localProperties.getProperty("puremusic.keystore.path")
val releaseStorePassword = localProperties.getProperty("puremusic.store.password")
val releaseKeyPassword = localProperties.getProperty("puremusic.key.password")
val releaseKeyAlias = localProperties.getProperty("puremusic.key.alias")
val releaseKeystoreFile = releaseKeystorePath
    ?.takeIf { it.isNotBlank() }
    ?.let { rootProject.file(it) }

val releaseSigningValues = listOf(
    "puremusic.keystore.path" to releaseKeystorePath,
    "puremusic.store.password" to releaseStorePassword,
    "puremusic.key.password" to releaseKeyPassword,
    "puremusic.key.alias" to releaseKeyAlias,
)
val releaseSigningConfigured = localPropertiesFile.isFile &&
    releaseKeystoreFile?.isFile == true &&
    releaseSigningValues.all { (_, value) -> !value.isNullOrBlank() }
val appVersionName = "1.3.0-" + ZonedDateTime.now(ZoneId.of("Asia/Shanghai"))
    .format(DateTimeFormatter.ofPattern("yyMMddHHmm"))
val releaseTaskRequested = gradle.startParameter.taskNames.any { taskName ->
    taskName.equals("assemble", ignoreCase = true) ||
        taskName.endsWith(":assemble", ignoreCase = true) ||
        taskName.contains("release", ignoreCase = true)
}

if (releaseTaskRequested && !releaseSigningConfigured) {
    val missingValues = buildList {
        if (!localPropertiesFile.isFile) {
            add("local.properties")
        }
        if (!releaseKeystorePath.isNullOrBlank() && releaseKeystoreFile?.isFile != true) {
            add("keystore file: $releaseKeystorePath")
        }
        addAll(
            releaseSigningValues
                .filter { (_, value) -> value.isNullOrBlank() }
                .map { (name, _) -> name },
        )
    }.joinToString()
    project.logger.warn(
        "Release signing is not configured; building an unsigned release APK. Missing: $missingValues. " +
            "Set puremusic.keystore.path, puremusic.store.password, puremusic.key.password, and puremusic.key.alias in local.properties to sign release builds.",
    )
}

android {
    namespace = "com.pure.music"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.pure.music"
        minSdk = 28
        targetSdk = 36
        versionCode = 5
        versionName = appVersionName

        ndk {
            abiFilters += setOf("arm64-v8a")
        }

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (releaseSigningConfigured) {
            create("release") {
                storeFile = requireNotNull(releaseKeystoreFile)
                storePassword = releaseStorePassword
                keyPassword = releaseKeyPassword
                keyAlias = releaseKeyAlias
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
            )
            if (releaseSigningConfigured) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    androidResources {
        localeFilters += setOf("en", "zh-rCN")
    }
    packaging {
        dex {
            useLegacyPackaging = true
        }
        jniLibs {
            useLegacyPackaging = false
        }
    }
}

@DisableCachingByDefault(because = "The renamed APK is copied after the packaged release APK.")
abstract class RenameReleaseApkTask : DefaultTask() {
    @get:Internal
    abstract val apkDirectory: DirectoryProperty

    @get:Input
    abstract val versionName: Property<String>

    @TaskAction
    fun copyVersionedApk() {
        val outputDirectory = apkDirectory.get().asFile
        val targetFile = outputDirectory.resolve("PureMusic_${versionName.get()}.apk")
        val sourceFile = outputDirectory
            .listFiles { candidate ->
                candidate.isFile &&
                    candidate.extension == "apk" &&
                    !candidate.name.startsWith("PureMusic_")
            }
            ?.maxByOrNull { candidate -> candidate.lastModified() }

        check(sourceFile?.isFile == true) {
            "Release APK was not generated in ${outputDirectory.absolutePath}"
        }

        sourceFile.copyTo(targetFile, overwrite = true)
        targetFile.setLastModified(System.currentTimeMillis())
        logger.lifecycle("Versioned release APK: ${targetFile.absolutePath}")
    }
}

val renameReleaseApk = tasks.register<RenameReleaseApkTask>("renameReleaseApk") {
    group = "build"
    description = "Copies the release APK to a versioned filename."
    dependsOn("packageRelease")
    apkDirectory.set(layout.buildDirectory.dir("outputs/apk/release"))
    versionName.set(appVersionName)
}

tasks.matching { task -> task.name == "assembleRelease" }.configureEach {
    dependsOn(renameReleaseApk)
}

dependencies {
    implementation(files("libs/renderscript-toolkit-blur-344be3f-arm64-16k.aar"))
    implementation(files("libs/media3-decoder-ffmpeg-1.11.0-ffmpeg9.0-arm64-v8a.aar"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.inspector)
    implementation(libs.androidx.media3.session)
    implementation(libs.compose.material3)
    implementation(libs.material.color.utilities)
    implementation(libs.miuix.blur)
    implementation(libs.miuix.icons)
    implementation(libs.miuix.nav)
    implementation(libs.miuix.preference)
    implementation(libs.miuix.ui)
    implementation(libs.reorderable)
    implementation(libs.taglib)
    implementation(libs.tinypinyin)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
