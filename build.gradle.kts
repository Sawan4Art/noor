// نور المسلم — إعادة البناء الأصلية بلغة كوتلن (2026-10)
// إصدارات الإضافات في gradle/libs.versions.toml — مصدر واحد للحقيقة
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
