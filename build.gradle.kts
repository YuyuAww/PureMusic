// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.application) apply false
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
// 使用反射方式访问 AGP DSL，避免类型参数擦除问题（Gradle Kotlin DSL 中
// CommonExtension 的泛型参数无法通过 findByType 安全传递）
listOf(":app", ":taglib").forEach { projPath ->
    project(projPath) {
        afterEvaluate {
            val android = project.extensions.findByName("android") ?: return@afterEvaluate
            try {
                val defaultConfig = android.javaClass.getMethod("getDefaultConfig").invoke(android)
                val extNdkBuild = defaultConfig.javaClass.getMethod("getExternalNativeBuild").invoke(defaultConfig)
                val cmake = extNdkBuild.javaClass.getMethod("getCmake").invoke(extNdkBuild)
                @Suppress("UNCHECKED_CAST")
                val args = cmake.javaClass.getMethod("getArguments").invoke(cmake) as MutableList<String>
                args += listOf(
                    "-DCMAKE_CXX_STANDARD=17",
                    "-DCMAKE_CXX_STANDARD_REQUIRED=ON"
                )
            } catch (e: Exception) {
                logger.warn("Failed to inject CMake C++17 standard: ${e.message}")
            }
        }
    }
}
