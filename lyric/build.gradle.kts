plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.pure.music.lyric"
    compileSdk = 37

    defaultConfig {
        minSdk = 28
    }

    testOptions {
        // JVM 单元测试中 android.util.Log 调用返回默认值而非抛异常
        unitTests.isReturnDefaultValues = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

kotlin { jvmToolchain(11) }

dependencies {
    testImplementation(libs.junit)
}
