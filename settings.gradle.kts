// نور المسلم — إعادة البناء الأصلية بلغة كوتلن (2026-10)
// settings.gradle.kts — جذر المشروع الوحيد: :app
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    // KSP: ربط معرف الإضافة بالأرتيفاكت مباشرة — نفس ما يفعله ماركر
    // com.google.devtools.ksp.gradle.plugin تماماً، لكن الماركر نفسه
    // لا يتوفر أحياناً على مرايا Maven فيفشل الحل كله. الأرتيفاكت
    // symbol-processing-gradle-plugin موجود على Maven Central دائماً.
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "com.google.devtools.ksp") {
                useModule("com.google.devtools.ksp:symbol-processing-gradle-plugin:${requested.version}")
            }
        }
    }
}

dependencyResolutionManagement {
    // PREFER_PROJECT: يسمح لسكريبت مرايا الساندبوكس المحلي بالتقدم — في CI
    // لا يوجد سكريبت مرايا فتُستخدم google و mavenCentral كما هي.
    repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "NoorAlMuslim"
include(":app")
