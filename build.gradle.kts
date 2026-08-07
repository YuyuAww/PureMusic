// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.application) apply false
    alias(libs.plugins.kotlin) apply false
    alias(libs.plugins.android.library) apply false
//    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.android.test) apply false
    alias(libs.plugins.baselineprofile) apply false
    alias(libs.plugins.android.dynamic.feature) apply false
}

// 对所有含 CMake 外部构建的模块统一注入 C++17 标准（消除 nested namespace/if-init 等 C++17 扩展警告）
// 通过 Gradle 动态 API 访问（无需依赖 AGP 类导入），避免修改子模块源码污染 gitlink
listOf(":app", ":taglib").forEach { projPath ->
    project(projPath) {
        afterEvaluate {
            val androidExt = extensions.findByName("android") ?: return@afterEvaluate
            val defaultConfig = androidExt.javaClass.getMethod("getDefaultConfig")
                .invoke(androidExt)
            val extNdkBuild = defaultConfig.javaClass.getMethod("getExternalNativeBuild")
                .invoke(defaultConfig)
            val cmakeOptions = extNdkBuild.javaClass.getMethod("getCmake")
                .invoke(extNdkBuild)
            @Suppress("UNCHECKED_CAST")
            val args = cmakeOptions.javaClass.getMethod("getArguments")
                .invoke(cmakeOptions) as MutableList<String>
            args += listOf(
                "-DCMAKE_CXX_STANDARD=17",
                "-DCMAKE_CXX_STANDARD_REQUIRED=ON"
            )
        }
    }
}
