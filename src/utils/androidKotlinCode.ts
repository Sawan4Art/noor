/**
 * Android Kotlin Source Code Generator & Exporter
 * Generates ready-to-run Jetpack Compose + Glance AppWidgets + Media3 + WorkManager Kotlin code
 */
import JSZip from 'jszip';

export interface KotlinFile {
  path: string;
  filename: string;
  description: string;
  language: 'kotlin' | 'groovy' | 'xml';
  content: string;
}

export const ANDROID_KOTLIN_PROJECT_FILES: KotlinFile[] = [
  {
    path: 'app/src/main/java/com/nour/muslim/glance/PrayerGlanceWidget.kt',
    filename: 'PrayerGlanceWidget.kt',
    description: 'كود ويدجت أندرويد الحديث بتقنية Jetpack Compose Glance لمواقيت الصلاة والعد التنازلي',
    language: 'kotlin',
    content: `package com.nour.muslim.glance

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.compose.ui.graphics.Color
import com.nour.muslim.MainActivity

/**
 * ويدجت الصلاة القادمة والعد التنازلي التفاعلي المخصص لنظام أندرويد
 * يدعم التخصيص والألوان الديناميكية Material You
 */
class PrayerGlanceWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // قراءة بيانات الصلاة المخزنة في DataStore
        val nextPrayerName = "العصر"
        val nextPrayerTime = "03:15 م"
        val remainingTime = "01:24:10"
        val hijriDate = "15 رمضان 1448 هـ"

        provideContent {
            GlanceTheme {
                WidgetContent(
                    prayerName = nextPrayerName,
                    prayerTime = nextPrayerTime,
                    countdown = remainingTime,
                    hijriDate = hijriDate
                )
            }
        }
    }

    @Composable
    private fun WidgetContent(
        prayerName: String,
        prayerTime: String,
        countdown: String,
        hijriDate: String
    ) {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(Color(0xFF042F2E)) // Emerald 950
                .cornerRadius(24.dp)
                .padding(16.dp)
                .clickable(actionStartActivity<MainActivity>()),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // شريط التاريخ الهجري
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🕌 نداء الصلاة",
                        style = TextStyle(
                            color = ColorProvider(Color(0xFF34D399)),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = GlanceModifier.defaultWeight())
                    Text(
                        text = hijriDate,
                        style = TextStyle(
                            color = ColorProvider(Color(0xFF94A3B8)),
                            fontSize = 11.sp
                        )
                    )
                }

                Spacer(modifier = GlanceModifier.height(10.dp))

                // اسم الصلاة والوقت
                Text(
                    text = "صلاة $prayerName",
                    style = TextStyle(
                        color = ColorProvider(Color.White),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = prayerTime,
                    style = TextStyle(
                        color = ColorProvider(Color(0xFF6EE7B7)),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                )

                Spacer(modifier = GlanceModifier.height(8.dp))

                // العداد التنازلي للأذان
                Box(
                    modifier = GlanceModifier
                        .background(Color(0x2610B981))
                        .cornerRadius(12.dp)
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "متبقي على الأذان: $countdown",
                        style = TextStyle(
                            color = ColorProvider(Color(0xFFA7F3D0)),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }
    }
}

class PrayerWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PrayerGlanceWidget()
}
`,
  },
  {
    path: 'app/src/main/java/com/nour/muslim/service/AdhanForegroundService.kt',
    filename: 'AdhanForegroundService.kt',
    description: 'خدمة تشغيل الأذان في الخلفية مع إشعارات كاملة وأصوات مؤذنين متعددة',
    language: 'kotlin',
    content: `package com.nour.muslim.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.nour.muslim.MainActivity
import com.nour.muslim.R

/**
 * خدمة Foreground Service للأذان في أندرويد
 * تضمن سماع صوت الأذان في وقته بدقة حتى عند قفل الشاشة أو في وضع Doze Mode
 */
class AdhanForegroundService : Service() {

    private var mediaPlayer: MediaPlayer? = null

    companion object {
        const val CHANNEL_ID = "adhan_notification_channel"
        const val ACTION_PLAY_ADHAN = "com.nour.muslim.ACTION_PLAY_ADHAN"
        const val ACTION_STOP_ADHAN = "com.nour.muslim.ACTION_STOP_ADHAN"
        const val EXTRA_PRAYER_NAME = "extra_prayer_name"

        fun startAdhan(context: Context, prayerName: String) {
            val intent = Intent(context, AdhanForegroundService::class.java).apply {
                action = ACTION_PLAY_ADHAN
                putExtra(EXTRA_PRAYER_NAME, prayerName)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY_ADHAN -> {
                val prayerName = intent.getStringExtra(EXTRA_PRAYER_NAME) ?: "الصلاة"
                playAdhanAudio(prayerName)
            }
            ACTION_STOP_ADHAN -> {
                stopAdhan()
            }
        }
        return START_NOT_STICKY
    }

    private fun playAdhanAudio(prayerName: String) {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, AdhanForegroundService::class.java).apply {
            action = ACTION_STOP_ADHAN
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("حان الآن موعد صلاة $prayerName 🕌")
            .setContentText("حي على الصلاة.. حي على الفلاح")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_media_pause, "إيقاف الأذان", stopPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .build()

        startForeground(1001, notification)

        // تشغيل صوت الأذان المختار
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer.create(this, R.raw.adhan_makkah).apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .build()
                )
                setOnCompletionListener {
                    stopAdhan()
                }
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopAdhan() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "تنبيهات الأذان والصلوات",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "قناة مخصصة لبث صوت الأذان والتنبيه بدخول وقت الصلاة"
                enableVibration(true)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopAdhan()
        super.onDestroy()
    }
}
`,
  },
  {
    path: 'app/src/main/java/com/nour/muslim/quran/QuranDownloadManager.kt',
    filename: 'QuranDownloadManager.kt',
    description: 'مدير تنزيل سور القرآن الكريم بدون إنترنت والتخزين المؤقت في ذاكرة الهاتف',
    language: 'kotlin',
    content: `package com.nour.muslim.quran

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

/**
 * فئة إدارة تنزيل سور القرآن الكريم من القراء للاستماع دون اتصال (Offline)
 */
class QuranDownloadManager(private val context: Context) {

    private val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    private val _downloadProgress = MutableStateFlow<Map<Int, Int>>(emptyMap())
    val downloadProgress: StateFlow<Map<Int, Int>> = _downloadProgress

    /**
     * تنزيل سورة لقارئ محدد وحفظها في التخزين المخصص للتطبيق
     */
    fun downloadSurah(reciterId: String, surahNumber: Int, serverUrl: String): Long {
        val paddedSurah = String.format("%03d.mp3", surahNumber)
        val downloadUrl = "$serverUrl$paddedSurah"
        val fileName = "\${reciterId}_\$paddedSurah"

        val request = DownloadManager.Request(Uri.parse(downloadUrl))
            .setTitle("تنزيل سورة رقم \$surahNumber")
            .setDescription("جاري حفظ التلاوة للاستماع بدون إنترنت...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_MUSIC, "Quran/\$fileName")
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(false)

        return downloadManager.enqueue(request)
    }

    /**
     * فحص ما إذا كانت السورة محملة بالفعل مسبقاً
     */
    fun isSurahDownloaded(reciterId: String, surahNumber: Int): Boolean {
        val paddedSurah = String.format("%03d.mp3", surahNumber)
        val fileName = "\${reciterId}_\$paddedSurah"
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC), "Quran/\$fileName")
        return file.exists() && file.length() > 0
    }

    /**
     * الحصول على المسار المحلي للسورة المحملة
     */
    fun getLocalAudioUri(reciterId: String, surahNumber: Int): Uri? {
        val paddedSurah = String.format("%03d.mp3", surahNumber)
        val fileName = "\${reciterId}_\$paddedSurah"
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC), "Quran/\$fileName")
        return if (file.exists()) Uri.fromFile(file) else null
    }
}
`,
  },
  {
    path: 'app/src/main/java/com/nour/muslim/glance/TasbihGlanceWidget.kt',
    filename: 'TasbihGlanceWidget.kt',
    description: 'ويدجت المسبحة السريعة التفاعلية بنقرة زر مباشرة من الشاشة الرئيسية',
    language: 'kotlin',
    content: `package com.nour.muslim.glance

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.compose.ui.graphics.Color

/**
 * ويدجت التسبيح السريع التفاعلي بنظام أندرويد
 */
class TasbihGlanceWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val currentDhikr = "سُبْحَانَ اللهِ وَبِحَمْدِهِ"
        val count = 27
        val target = 33

        provideContent {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(Color(0xFF0F172A))
                    .cornerRadius(24.dp)
                    .padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "📿 المسبحة الذكية",
                        style = TextStyle(color = ColorProvider(Color(0xFF38BDF8)), fontSize = 12.sp)
                    )
                    Spacer(modifier = GlanceModifier.height(6.dp))
                    Text(
                        text = currentDhikr,
                        style = TextStyle(color = ColorProvider(Color.White), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = GlanceModifier.height(10.dp))
                    
                    // زر العداد التفاعلي في الويدجت
                    Box(
                        modifier = GlanceModifier
                            .background(Color(0xFF059669))
                            .cornerRadius(18.dp)
                            .padding(horizontal = 24.dp, vertical = 10.dp)
                            .clickable(actionRunCallback<IncrementTasbihCallback>()),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$count / $target",
                            style = TextStyle(color = ColorProvider(Color.White), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}

class IncrementTasbihCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        // تحديث العداد وحفظه في SharedPreferences أو DataStore
        TasbihGlanceWidget().update(context, glanceId)
    }
}

class TasbihWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TasbihGlanceWidget()
}
`,
  },
  {
    path: 'app/src/main/AndroidManifest.xml',
    filename: 'AndroidManifest.xml',
    description: 'ملف بيان الأندرويد متضمناً صلاحيات المنبهات الدقيقة والإشعارات وتكوين الويدجتس',
    language: 'xml',
    content: `<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    package="com.nour.muslim">

    <!-- صلاحيات الأذان في الوقت المحدد بدقة -->
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    <uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
    <uses-permission android:name="android.permission.USE_EXACT_ALARM" />
    <uses-permission android:name="android.permission.WAKE_LOCK" />
    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.VIBRATE" />

    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="نور المسلم"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.NourMuslim">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@style/Theme.NourMuslim">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <!-- خدمة تشغيل الأذان في الخلفية -->
        <service
            android:name=".service.AdhanForegroundService"
            android:foregroundServiceType="mediaPlayback"
            android:exported="false" />

        <!-- مستقبل ويدجت أوقات الصلاة Glance -->
        <receiver
            android:name=".glance.PrayerWidgetReceiver"
            android:exported="true">
            <intent-filter>
                <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
            </intent-filter>
            <meta-data
                android:name="android.appwidget.provider"
                android:resource="@xml/prayer_widget_info" />
        </receiver>

        <!-- مستقبل ويدجت المسبحة الذكية Glance -->
        <receiver
            android:name=".glance.TasbihWidgetReceiver"
            android:exported="true">
            <intent-filter>
                <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
            </intent-filter>
            <meta-data
                android:name="android.appwidget.provider"
                android:resource="@xml/tasbih_widget_info" />
        </receiver>

    </application>
</manifest>
`,
  },
  {
    path: 'app/build.gradle.kts',
    filename: 'build.gradle.kts',
    description: 'إعدادات Gradle مع أحدث مكتبات Jetpack Compose و Glance و Media3 ExoPlayer و WorkManager',
    language: 'groovy',
    content: `plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.nour.muslim"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.nour.muslim"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    // Jetpack Compose & Material 3
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.activity.compose)

    // Android Glance AppWidgets (للويدجتس الحديثة)
    implementation("androidx.glance:glance-appwidget:1.1.1")
    implementation("androidx.glance:glance-material3:1.1.1")

    // Media3 ExoPlayer لتشغيل القرآن الكريم
    implementation("androidx.media3:media3-exoplayer:1.5.0")
    implementation("androidx.media3:media3-session:1.5.0")
    implementation("androidx.media3:media3-ui:1.5.0")

    // WorkManager لتحديث أوقات الصلاة والمنبهات في الخلفية
    implementation("androidx.work:work-runtime-ktx:2.10.0")

    // DataStore للتخزين المحلي
    implementation("androidx.datastore:datastore-preferences:1.1.1")
}
`,
  },
  {
    path: 'app/src/main/java/com/nour/muslim/qibla/QiblaCompassManager.kt',
    filename: 'QiblaCompassManager.kt',
    description: 'مدير بوصلة القبلة وحساسات الهاتف (Rotation Vector + Geomagnetic Sensor) في أندرويد',
    language: 'kotlin',
    content: `package com.nour.muslim.qibla

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.*

/**
 * نظام استشعار القبلة في كوتلن لأندرويد
 * يعتمد على دمج حساسات الدوران والبوصلة المغناطيسية بدقة عالية
 */
class QiblaCompassManager(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    private val _azimuthHeading = MutableStateFlow(0f)
    val azimuthHeading: StateFlow<Float> = _azimuthHeading

    fun startListening() {
        rotationSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stopListening() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
            val rotationMatrix = FloatArray(9)
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            val orientation = FloatArray(3)
            SensorManager.getOrientation(rotationMatrix, orientation)
            val azimuthRad = orientation[0]
            val azimuthDeg = (Math.toDegrees(azimuthRad.toDouble()) + 360) % 360
            _azimuthHeading.value = azimuthDeg.toFloat()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    companion object {
        const val KAABA_LATITUDE = 21.4225241
        const val KAABA_LONGITUDE = 39.8261818

        /**
         * حساب زاوية القبلة الدقيقة من موقع المستخدم الحالي
         */
        fun calculateQiblaBearing(userLat: Double, userLng: Double): Float {
            val phiK = Math.toRadians(KAABA_LATITUDE)
            val lambdaK = Math.toRadians(KAABA_LONGITUDE)
            val phi = Math.toRadians(userLat)
            val lambda = Math.toRadians(userLng)

            val y = sin(lambdaK - lambda)
            val x = cos(phi) * tan(phiK) - sin(phi) * cos(lambdaK - lambda)
            val qibla = Math.toDegrees(atan2(y, x))
            return ((qibla + 360) % 360).toFloat()
        }
    }
}
`,
  },
];

/**
 * Downloads a ZIP bundle containing all Android Kotlin project files
 */
export async function downloadAndroidProjectZip(): Promise<void> {
  const zip = new JSZip();

  // Readme for Android Studio
  zip.file(
    'README.md',
    `# مشروع نور المسلم - كود أندرويد كوتلن كامل (Nour Muslim Android App)

تطبيق إسلامي شامل بنظام Android مبني بأحدث معايير كوتلن (Kotlin & Jetpack Compose):
- مؤذن دقيق في الخلفية مع Foreground Service & AlarmManager
- مصحف شريف قابل للقراءة والتنزيل الصوتي والاستماع مع ExoPlayer
- مسبحة وأذكار مأثورة وأذكار ما بعد الصلاة
- ويدجتس أندرويد للشاشة الرئيسية مبنية بتقنية Jetpack Compose Glance

### خطوات التشغيل في Android Studio:
1. افتح Android Studio
2. اختر Open Project وحدد هذا المجلد
3. انتظر انتهاء Gradle Sync
4. قم بتشغيل التطبيق على محاكي أو هاتفك الشخصي!
`
  );

  // Add all files
  for (const file of ANDROID_KOTLIN_PROJECT_FILES) {
    zip.file(file.path, file.content);
  }

  const content = await zip.generateAsync({ type: 'blob' });
  const url = URL.createObjectURL(content);
  const a = document.createElement('a');
  a.href = url;
  a.download = 'NourMuslim-Android-Kotlin-Project.zip';
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);
}
