// نور المسلم — إعادة البناء الأصلية بلغة كوتلن (2026-10)
// settings.gradle.kts — جذر المشروع الوحيد: :app
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
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
