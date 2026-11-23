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
        // الإصدار 1.83 — بلاغ المالك (٣ مهام): «عايز اشعار تفاعلي
        // ثابت في شريط الاشعارات للتحكم في تشغيل او ايقاف الصوت او
        // حتى قفل الصوت مع ايقاف التطبيق» (خدمة QuranAudioService
        // أماميةٌ بإشعارٍ تفاعليٍّ ثابتٍ بأزرار تشغيل/إيقافٍ مؤقتٍ
        // وإيقافٍ كاملٍ تعكس حالة محرك التلاوة وتحمل ضغطاتها إليه
        // فلا مصدرَين للحقيقة — والخدمة تبقي العملية حيّة فالصوت
        // «مقفولٌ» يعمل مع إغلاق التطبيق كله حتى الإيقاف من الإشعار
        // نفسه) و«شريط التنقل يختفي في صفحه المصحف فقط و يظهر مره
        // اخرى عند الرجوع بزرار الرجوع في الهاتف يظهر مع ظهور صفحه
        // اختيار السوره عايز بقا تكون صفحه المصحف بكامل الشاشه»
        // (hideBar في LiquidNavScaffold + onImmersive من MushafScreen:
        // القراءة بلا شريطٍ وبتعبئةٍ كاملةٍ للحافة السفلى والرجوع
        // يفتح قائمة السور فيعود الشريط معها) و«ازاله الاجزء الاصفر
        // في اخر الصفحه الذي يحتوي على رقم الصفه و استبداله برقم
        // الصفحه لاكن بشكل افضل» (قصّ محسوسٌ 36.2 بكسل فراغٍ مرجعيٍّ
        // من أسفل صفحات ٣+ يبقي الإطار المزخرف كاملاً — والرقم رقاقةٌ
        // زجاجيةٌ بلون الثيم بأرقامٍ عربيةٍ تنزلق مع التقليب) و«اذان
        // يوم الجمعه و صوت الاذان عايزهم داخل قسم اعدادات الاذان»
        // (دمجُهما في صفحة الإعدادات — أذانٌ وإقامةٌ وصوتٌ وجمعةٌ
        // معاً والتنبيه والكتم صفحتان مستقلتان) — وversionCode 50083
        // فوق 50082 فيقبل المثبّت التحديث فوقه
        versionCode = 50083
        versionName = "1.83"
        // الإصدار 1.82 — بلاغ المالك (٥ مهام): «الزر الجديد بتاع
        // التفاسير بيعمل جلتش و بيوقف التطبيق» (درس الانهيار: تنسيق
        // الأرقام بلغة الجهاز العربية يفهرس خارج المصفوفة — حُذف
        // التنسيق كلياً) و«التفاسير تكون مدمجه داخل التطبيق نفسه مش
        // قابله للتنزيل … و المستخدم يختار بينهم» (الطبعات الست
        // كلها أصولٌ نصيةٌ مدمجةٌ ملفُ سورةٍ لكل طبعةٍ — بلا تنزيلٍ
        // ولا استخراجٍ ولا مساحةٍ إضافيةٍ ومبدّل اللوحة يعرض الست
        // دائماً) و«فصل بين تشغيل بلاطه الاعدادات السريعه و بين
        // الاشعار الثابت بحيث اقدر افعل ميزه واحده بين الاثنين او
        // كلاهما مع بعض» (مفتاحا QUICK_TILE وQUICK_NOTIFICATION
        // مستقلان في القسم العام بمفتاحين) و«كل كتم الاصوات في يوم
        // الجمعه لصلاه الجمعه يكون مخصص بعد الاذان الاول بمده
        // يحددها المستخدم و افتراضي يكون ساعه ونص و قابله للتخصيص
        // حتى ٣ ساعات» (عَلَم الكتم ومدته المخصصة على الأذان الأول
        // وحده عبر EXTRA_MUTE_MINUTES والافتراضي ٩٠ حتى ١٨٠) و«كل
        // قسم في اقسام الاذان و الصوت يكون داخل صفحه لوحده لعدم
        // التشتيت يعني اعاداد الاذان يكون مدمج معه الاذان و الاقامة»
        // (خمس صفحاتٍ مستقلةٍ من enum AdhanPage) و«خلي اختيار الوان
        // الويدجت تكون نفس لوحه الالوان الخاصه بثيم التطبيق» (اللوحة
        // تُشتق من AccentPalette بأسمائها العربية) — وversionCode
        // 50082 فوق 50081 فيقبل المثبّت التحديث فوقه
        // نسخة 1.82 التاريخية — ما تبعها أعلاه هو 1.83
        // versionCode = 50082
        // versionName = "1.82"
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
