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
// 通过 AGP 公有的 CommonExtension API 访问，避免反射和内部 API 依赖
import com.android.build.api.dsl.CommonExtension

listOf(":app", ":taglib").forEach { projPath ->
    project(projPath) {
        afterEvaluate {
            val android = extensions.findByType<CommonExtension<*, *, *, *, *>>() ?: return@afterEvaluate
            android.defaultConfig {
                externalNativeBuild {
                    cmake {
                        arguments += listOf(
                            "-DCMAKE_CXX_STANDARD=17",
                            "-DCMAKE_CXX_STANDARD_REQUIRED=ON"
                        )
                    }
                }
            }
        }
    }
}
