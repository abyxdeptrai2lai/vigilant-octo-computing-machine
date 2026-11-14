plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.aliucord.plugin)
}

android {
    namespace = "com.github.yournamehere"
    compileSdk = 35
    defaultConfig {
        minSdk = 28
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

// CÚ PHÁP MỚI: Thay thế kotlinOptions cũ
kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

version = "1.0.0"
description = "Paste button and Spam menu"

aliucord {
    changelog.set(
        """
        # 1.0.0
        * Initial release!
        """.trimIndent(),
    )
    deploy.set(true)
}

dependencies {
    compileOnly(libs.discord)
    compileOnly(libs.aliucord)
    compileOnly(libs.kotlin.stdlib)
}
