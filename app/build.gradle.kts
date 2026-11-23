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
        // الإصدار 1.81 — بلاغ المالك (مهمّتان): «اضف التفاسير التاليه
        // الجلالين و القرطبي و ابن كثير الطبري السعدي و طبعا مع ابقاء
        // التفسير الميسر مع امكانيه تغيير طبعه التفسير من الاعدادات و
        // حطلها قسم جديد باسم التفاسير» (قسمٌ جديدٌ بالطبعات الست
        // — الميسر المضمّن باقٍ افتراضيّاً والخمس الأخرى تنزيلٌ عند
        // الطلب من فرع الأصول ببصمة sha256 واستئنافٍ ومبدّلٌ داخل لوحة
        // التفسير نفسها) و«انا عايز ايقونه شريط اشعارات زي اللي
        // موجودين في الصوره مش مجرد اشعار ثابت الاشعار الثابت ده شيله
        // Quick Settings Tile يكون داعم ليها» (أُلغي وضع الأيقونة
        // الثابتة نهائياً — الإشعار الآن إشعارٌ عاديٌّ مختصرٌ بعدّادٍ
        // حيٍّ + QuickLaunchTileService بلاطةٌ في الإعدادات السريعة
        // تعرض المتبقي لحظة السحب وضغطها يفتح التطبيق) — وversionCode
        // 50081 فوق 50080 فيقبل المثبّت التحديث فوقه
        versionCode = 50081
        versionName = "1.81"
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
        // بلاغ المالك v1.73: «الغب النظام ده و اعمل اصدار جديد يتثبت على
        // كل الاصدارات الموجوده علشان اللي عنده التطبيق مش فاهم الكلام
        // ده» — نكهة ألفا أُلغيت كلياً: تطبيقٌ واحدٌ بمعرّفٍ واحد
        // (com.sawan4art.noor) وملف إصدارٍ واحدٍ يتثبّت فوق كل ما قبله
        // من النسخ المستقرة 1.70→1.72 — من يملك نسخة الحزمة القديمة
        // المنفصلة (.alpha) يثبّت هذا الملف كتطبيقٍ جارٍ بجوارها مرةً
        // واحدةً ثم يمسح القديمة، وكل تحديثٍ بعده يتثبّت مباشرةً
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
