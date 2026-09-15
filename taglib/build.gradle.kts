plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.pure.music.taglib"
    compileSdk = 37

    defaultConfig {
        minSdk = 28
        ndk { abiFilters.add("arm64-v8a") }
        externalNativeBuild { cmake { cppFlags += "-std=c++17" } }
    }

    externalNativeBuild {
        cmake { path = file("src/main/cpp/CMakeLists.txt") }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

kotlin { jvmToolchain(11) }
