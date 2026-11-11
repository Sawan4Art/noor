plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.sawan4art.noor"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.sawan4art.noor"
        minSdk = 24
        targetSdk = 36
        // الإصدار 1.71 — سلم بلاغ المالك 2026-11-11: «البدأ بتغيير رقم
        // الاصدار تدريجيا مع كل اصدار عن طريق تغيير رقم 70 حتى الوصول
        // الى رقم 99 و يتغير بعدها رقم الاصدار 2.0.0 و هكذا» — 1.70 ثم
        // 1.71 فـ1.99 ثم 2.0.0 — وversionCode 50071 فوق 50070 فيقبل
        // المثبّت التحديث فوقه
        versionCode = 50071
        versionName = "1.71"
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
            // بلاغ المالك v1.71: «ازل كلمه الفا من اسم التطبيق» — الاسم
            // الظاهر موحّدٌ بين النكهتين، والفرق في معرف الحزمة فقط
            resValue("string", "app_name", "نور المسلم")
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


    // مكتبة الأحاديث — Room على قاعدة SQLite جاهزة في الـ Assets مع فهرس FTS5
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    // org.json الحقيقية في اختبارات الوحدة — android.jar للجهاز الافتراضي
    // يزوّرها فيرجع JSON كله افتراضياً (درس اختبار تخصيص الويدجت alpha56)
    testImplementation("org.json:json:20240303")
}

// منصة الأدوات 17 معلنة صراحةً (alpha64 — بيئة الساندبوكس جاءت بجافا-ركن
// بلا javac): مع محوِّل foojay في settings.gradle.kts ينزّل غرادل JDK 17
// المطابق تلقائياً بدل أن يقف على JRE النظام الذي لا يحوّل
kotlin {
    jvmToolchain(17)
}
