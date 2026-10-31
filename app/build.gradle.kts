plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.sawan4art.noor"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.sawan4art.noor"
        minSdk = 24
        targetSdk = 36
        // إصدارات ألفا لإعادة البناء الكوتلنية — النسخة النهائية الموحّدة تُضبط عند اكتمال المراحل
        versionCode = 50015
        versionName = "0.5.0-alpha15"
        vectorDrawables { useSupportLibrary = true }
    }

    // مفتاح CI يُقرأ من متغيرات البيئة — محليًا يوقّع debug حتى يبقى البناء شغالًا
    signingConfigs {
        create("ci") {
            val ksPath = System.getenv("NOOR_KEYSTORE") ?: ""
            if (ksPath.isNotEmpty()) {
                storeFile = file(ksPath)
                storePassword = System.getenv("NOOR_KS_PASS")
                keyAlias = System.getenv("NOOR_KEY_ALIAS")
                keyPassword = System.getenv("NOOR_KS_PASS")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            resValue("string", "app_name", "نور المسلم")
            signingConfig = if (signingConfigs.getByName("ci").storeFile != null) {
                signingConfigs.getByName("ci")
            } else {
                signingConfigs.getByName("debug")
            }
        }
        create("alpha") {
            initWith(getByName("release"))
            // تطبيق ألفا منفصل يُثبَّت جنب النسخة المستقرة أثناء إعادة البناء
            applicationIdSuffix = ".alpha"
            resValue("string", "app_name", "نور المسلم ألفا")
            isMinifyEnabled = false
            isShrinkResources = false
            matchingFallbacks += "release"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        // java.time على minSdk 24 — بدون دي انهيار وقت التشغيل على API 24-25
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.kotlinx.coroutines.android)

    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
}
