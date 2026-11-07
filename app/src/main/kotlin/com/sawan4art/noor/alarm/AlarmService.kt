package com.sawan4art.noor.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.sawan4art.noor.MainActivity
import com.sawan4art.noor.R
import com.sawan4art.noor.domain.adhan.AdhanSoundKind
import com.sawan4art.noor.domain.adhan.AdhanVoices
import java.io.File

/**
 * خدمة الأذان الأمامية — الصوت يشتغل والشاشة مطفأة والهاتف مقفول:
 *
 * - **التشغيل**: المشغّل على مجرى المنبه (USAGE_ALARM) فيُسمع بصوت
 *   مسموع حتى لو وسائط الهاتف خافتة، والصوت إما مدمج (خمسة أذان وخمس
 *   إقامات) أو صوت مختار من الهاتف أو نغمة إشعارات الهاتف.
 * - **الانبثاق فوق القفل**: إشعار بأولوية قصوى ومنبثق كامل الشاشة
 *   (full-screen intent) يفتح شاشة التنبيه فوق القفل ويُشعل الشاشة.
 * - **كتم الهاتف بعد الإقامة** (تحديث المالك 2026-11-06: «ميزه لكتم
 *   الاصوات لو المستخدم حدد ٣٠ دقيقه مثلا بعد الاقامه»): بعد انتهاء
 *   صوت الإقامة (أو الأذان إن بلا إقامة) تدخل الخدمة «نافذة الكتم» —
 *   إشعار هادئ دائم يوضّح متى يرجع الصوت، وفَكُّ قفل الشاشة يلغي
 *   الكتم فوراً إن اختار المالك ذلك («اصوات الهاتف ترجع تشتغل تاني
 *   حتى لو لسه الوقت لم ينقضي»)، والاستعادة التلقائية بمنبه مستقل
 *   حتى لو قُتلت العملية.
 */
class AlarmService : Service() {

    private var player: MediaPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var safetyStop: Runnable? = null

    /** إعدادات نافذة الكتم للحبة الحالية — تُملأ من نية الحدث */
    private var muteStartPending = false
    private var muteMinutes = 10
    private var muteCancelOnUnlock = true

    /** هل نحن الآن داخل نافذة الكتم (بعد انتهاء الصوت)؟ */
    private var inMuteWindow = false

    /** مستقبِل فتح قفل الشاشة — يلغي الكتم فوراً إن كان الاختيار مفعّلاً */
    private val unlockReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != Intent.ACTION_USER_PRESENT) return
            if (!inMuteWindow || !muteCancelOnUnlock) return
            // المالك: المستخدم فتح تلفونه — أصوات الهاتف ترجع فوراً
            endMuteWindow()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        ensureChannels(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                MuteManager.restore(this)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_RESTORE_MUTE -> {
                MuteManager.restore(this)
                stopSelf()
                return START_NOT_STICKY
            }
        }

        // حدث جديد يصل والكتم ما زال حياً من صلاة سابقة — يُرجَع الصوت
        // أولاً كي يُسمع هذا الحدث ويبدأ الكتم من جديد في وقته
        if (MuteManager.isActive(this)) {
            MuteManager.restore(this)
            endMuteWindowInternal()
        }

        val mode = intent?.getStringExtra(EXTRA_MODE) ?: MODE_ADHAN
        val prayerName = intent?.getStringExtra(EXTRA_PRAYER_NAME) ?: "الصلاة"
        val soundId = intent?.getStringExtra(EXTRA_SOUND_ID) ?: "main"
        val customPath = intent?.getStringExtra(EXTRA_CUSTOM_PATH) ?: ""

        muteStartPending = intent?.getBooleanExtra(EXTRA_MUTE_START, false) == true
        muteMinutes = intent?.getIntExtra(EXTRA_MUTE_MINUTES, 10)?.coerceAtLeast(1) ?: 10
        muteCancelOnUnlock = intent?.getBooleanExtra(EXTRA_MUTE_CANCEL_ON_UNLOCK, true) ?: true
        inMuteWindow = false
        clearPlaybackEnded()

        val started = runCatching {
            startInForeground(prayerName, mode)
        }.getOrElse {
            // قيود الخلفية الصارمة — إشعار هادئ بديل ثم خروج هادئ
            Log.w(TAG, "startForeground failed: ${it.message}")
            postSilentPrayerNotice(this, prayerName)
            stopSelf()
            return START_NOT_STICKY
        }
        if (!started) return START_NOT_STICKY

        acquireWakeLock()

        val kind = if (mode == MODE_IQAMAH) AdhanSoundKind.IQAMAH else AdhanSoundKind.ADHAN
        play(kind, soundId, customPath)
        return START_NOT_STICKY
    }

    /* ============================ الواجهة الأمامية ============================ */

    private fun startInForeground(prayerName: String, mode: String): Boolean {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(alarmChannel())
        }
        val notification = buildNotification(prayerName, mode)
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            } else {
                0
            },
        )
        return true
    }

    /** قناة الأذان — أهمية قصوى وتجاوز عدم الإزعاج عند منح الإذن */
    private fun alarmChannel(): NotificationChannel =
        NotificationChannel(CHANNEL_ID, "الأذان والإقامة", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "تشغيل الأذان والإقامة في وقت الصلاة — منبثق فوق شاشة القفل"
            setSound(null, null)
            enableVibration(true)
            setShowBadge(false)
        }

    private fun buildNotification(prayerName: String, mode: String): Notification {
        val stopPi = PendingIntent.getService(
            this,
            1,
            Intent(this, AlarmService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val openPi = PendingIntent.getActivity(
            this,
            2,
            Intent(this, AlarmActivity::class.java)
                .putExtra(AlarmActivity.EXTRA_PRAYER_NAME, prayerName)
                .putExtra(AlarmActivity.EXTRA_PRAYER_MODE, mode),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val openAppPi = PendingIntent.getActivity(
            this,
            3,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val title = if (mode == MODE_IQAMAH) "قد قامت الصلاة — $prayerName" else "حان الآن وقت $prayerName"
        val text = if (mode == MODE_IQAMAH) "حان وقت الإقامة — تقبل الله طاعتكم" else "التطبيق يؤذن الآن — تقبل الله طاعتكم"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_update)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setContentIntent(openAppPi)
            .setFullScreenIntent(openPi, true) // انبثاق فوق شاشة القفل
            .addAction(0, "إيقاف", stopPi)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    /** إشعار نافذة الكتم — هادئ دائم يوضّح متى يرجع الصوت وكيف يُستعجل */
    private fun postMuteWindowNotification(): Notification {
        val restorePi = PendingIntent.getService(
            this,
            1,
            Intent(this, AlarmService::class.java).setAction(ACTION_RESTORE_MUTE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val until = MuteManager.restoreAtText(this, muteMinutes)
        val text = if (muteCancelOnUnlock) {
            "يُرجَع الصوت تلقائياً حوالي $until — أو فور فتح قفل الشاشة"
        } else {
            "يُرجَع الصوت تلقائياً حوالي $until"
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_update)
            .setContentTitle("أصوات الهاتف مكتومة بعد الإقامة")
            .setContentText(text)
            .setOngoing(true)
            .addAction(0, "إرجاع الصوت الآن", restorePi)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    /* ============================ التشغيل ============================ */

    private fun play(kind: AdhanSoundKind, soundId: String, customPath: String) {
        releasePlayer()
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        val prepared: MediaPlayer? = runCatching {
            when {
                // صوت مختار من الهاتف
                soundId == AdhanVoices.CUSTOM_ID && customPath.isNotEmpty() &&
                    File(customPath).exists() -> MediaPlayer().apply {
                    setAudioAttributes(attrs)
                    setDataSource(customPath)
                    prepare()
                }
                // نغمة إشعارات الهاتف
                soundId == AdhanVoices.NOTIFICATION_ID -> {
                    var uri: Uri? = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    if (uri == null) uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    if (uri == null) null else MediaPlayer().apply {
                        setAudioAttributes(attrs)
                        setDataSource(this@AlarmService, uri)
                        prepare()
                    }
                }
                // صوت مدمج — وأي معرّف غريب يرجع للافتراضي
                else -> {
                    val resId = AdhanVoices.builtinResId(this, kind, soundId)
                    if (resId == 0) null else MediaPlayer().apply {
                        setAudioAttributes(attrs)
                        setDataSource(
                            this@AlarmService,
                            Uri.parse("android.resource://$packageName/$resId"),
                        )
                        prepare()
                    }
                }
            }
        }.getOrNull() ?: runCatching {
            // آخر محاولة: الافتراضي المدمج
            val fallbackKind = if (kind == AdhanSoundKind.IQAMAH) {
                AdhanVoices.IQAMAH.first().id
            } else {
                AdhanVoices.ADHAN.first().id
            }
            val resId = AdhanVoices.builtinResId(this, kind, fallbackKind)
            if (resId == 0) null else MediaPlayer().apply {
                setAudioAttributes(attrs)
                setDataSource(this@AlarmService, Uri.parse("android.resource://$packageName/$resId"))
                prepare()
            }
        }.getOrNull()

        if (prepared == null) {
            // فشل التشغيل — نمر بنفس مسار الانتهاء (الكتم إن كان هو بدايته)
            finishPlayback()
            return
        }
        player = prepared
        prepared.setOnCompletionListener { finishPlayback() }
        prepared.setOnErrorListener { _, _, _ ->
            finishPlayback()
            true
        }
        prepared.start()

        // أمان: نهاية التشغيل تلقائياً بعد الحد الأقصى مهما حدث —
        // وتنتهي بنفس مسار الانتهاء الطبيعي (نافذة الكتم إن كانت بدايته)
        val self = this
        safetyStop = Runnable { finishPlayback() }
        handler.postDelayed(safetyStop!!, AlarmScheduler.MAX_PLAY_MINUTES * 60_000)
    }

    /**
     * نهاية الصوت — المسار الموحد لاكتمال التشغيل أو خطئه أو أمانه:
     * إن كان هذا الحدث بداية الكتم تدخل نافذة الكتم، وإلا تنتهي الخدمة.
     *
     * شاشة التنبيه تُغلق نفسها مع نهاية الصوت (تحديث المالك 2026-11-07:
     * «الشاشه بتفضل موجوده و بتسخن الجهاز و بتستهلك البطاريه بشكل عنيف»):
     * بثٌّ داخلي يصل لشاشة التنبيه فتنتهي، ويسقط سهر الشاشة والمعالج
     * معاً — نافذة الكتم بعدها لا تحتاج سهراً فالاستعادة بمنبه مستقل.
     */
    private fun finishPlayback() {
        releasePlayer()
        markPlaybackEnded()
        runCatching {
            sendBroadcast(Intent(ACTION_PLAYBACK_ENDED).setPackage(packageName))
        }
        wakeLock?.let { w -> runCatching { if (w.isHeld) w.release() } }
        wakeLock = null
        if (muteStartPending && !inMuteWindow) {
            muteStartPending = false
            enterMuteWindow()
        } else {
            stopSelf()
        }
    }

    /* ============================ نافذة الكتم ============================ */

    /**
     * نافذة الكتم بعد الإقامة — الصوت مكتوم المُدة المحددة، والخدمة
     * حية بإشعار هادئ تحفظ فك القفل (إن اختاره المالك) والاستعادة اليدوية.
     */
    private fun enterMuteWindow() {
        val applied = MuteManager.apply(this, muteMinutes)
        if (!applied) {
            stopSelf()
            return
        }
        inMuteWindow = true
        if (muteCancelOnUnlock) {
            val filter = if (Build.VERSION.SDK_INT >= 33) {
                Context.RECEIVER_NOT_EXPORTED
            } else {
                0
            }
            runCatching {
                registerReceiver(unlockReceiver, IntentFilter(Intent.ACTION_USER_PRESENT), filter)
            }
        }
        // تحديث إشعار الخدمة الأمامية نفسه — بلا إشعار ثانٍ
        runCatching {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.notify(NOTIFICATION_ID, postMuteWindowNotification())
        }
    }

    /** إلغاء نافذة الكتم: استعادة الصوت + تنظيف المستقبِل + إنهاء الخدمة */
    private fun endMuteWindow() {
        MuteManager.restore(this)
        endMuteWindowInternal()
    }

    private fun endMuteWindowInternal() {
        inMuteWindow = false
        runCatching { unregisterReceiver(unlockReceiver) }
        stopSelf()
    }

    private val handler by lazy { android.os.Handler(android.os.Looper.getMainLooper()) }

    private fun acquireWakeLock() {
        runCatching {
            val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "noor:adhan").apply {
                acquire(AlarmScheduler.MAX_PLAY_MINUTES * 60_000)
            }
        }
    }

    private fun releasePlayer() {
        safetyStop?.let { handler.removeCallbacks(it) }
        safetyStop = null
        player?.let { p ->
            runCatching { if (p.isPlaying) p.stop() }
            runCatching { p.release() }
        }
        player = null
    }

    override fun onDestroy() {
        releasePlayer()
        if (inMuteWindow) {
            // قُتلت الخدمة من النظام — المستقبِل الديناميكي يرحل معها،
            // والاستعادة بمنبه MuteManager الاحتياطي في موعده مهما حدث
            inMuteWindow = false
            runCatching { unregisterReceiver(unlockReceiver) }
        }
        wakeLock?.let { w -> runCatching { if (w.isHeld) w.release() } }
        wakeLock = null
        super.onDestroy()
    }

    /* ============================ أدوات ثابتة ============================ */

    companion object {
        private const val TAG = "NoorAlarm"
        private const val CHANNEL_ID = "noor_adhan_channel"
        private const val NOTIFY_CHANNEL_ID = "noor_notify_channel"
        private const val NOTIFICATION_ID = 4001

        const val EXTRA_MODE = "noor_extra_alarm_mode"
        const val EXTRA_PRAYER_NAME = "noor_extra_alarm_prayer_name"
        const val EXTRA_SOUND_ID = "noor_extra_alarm_sound_id"
        const val EXTRA_CUSTOM_PATH = "noor_extra_alarm_custom_path"
        const val EXTRA_MUTE_START = "noor_extra_alarm_mute_start"
        const val EXTRA_MUTE_MINUTES = "noor_extra_alarm_mute_minutes"
        const val EXTRA_MUTE_CANCEL_ON_UNLOCK = "noor_extra_alarm_mute_cancel_on_unlock"
        const val MODE_ADHAN = "adhan"
        const val MODE_IQAMAH = "iqamah"
        const val ACTION_STOP = "com.sawan4art.noor.action.ADHAAN_STOP"
        const val ACTION_RESTORE_MUTE = "com.sawan4art.noor.action.ADHAAN_RESTORE_MUTE"

        /** بثٌّ داخلي: انتهى صوت الأذان/الإقامة — شاشة التنبيه تنتهي معه */
        const val ACTION_PLAYBACK_ENDED = "com.sawan4art.noor.action.ADHAAN_PLAYBACK_ENDED"

        /** هل انتهى صوت آخر حدث؟ — علم على مستوى العملية تقرأه شاشة التنبيه
         *  إن فُتحت متأخرة بعد نهاية الصوت فتُغلق نفسها فوراً */
        @Volatile
        var playbackEnded: Boolean = false
            private set
        internal fun markPlaybackEnded() {
            playbackEnded = true
        }
        internal fun clearPlaybackEnded() {
            playbackEnded = false
        }

        /** قنوات الأذان تُنشأ مبكراً أيضاً من المستقبِل قبل أي إشعار */
        fun ensureChannels(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "الأذان والإقامة", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "تشغيل الأذان والإقامة في وقت الصلاة — منبثق فوق شاشة القفل"
                    setSound(null, null)
                    enableVibration(true)
                    setShowBadge(false)
                },
            )
            nm.createNotificationChannel(
                NotificationChannel(NOTIFY_CHANNEL_ID, "تنبيهات الصلاة", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "تنبيهات صامتة عند تعذّر تشغيل صوت الأذان"
                    enableVibration(true)
                },
            )
        }

        /** استعادة فورية لأصوات الهاتف (منبه الاحتياط / إجراءات المستخدم) */
        fun restoreMuteNow(context: Context) {
            MuteManager.restore(context)
        }

        /** إشعار هادئ عند تعذّر تشغيل الصوت — لا صلاة تفوت بلا أي ذكر */
        fun postSilentPrayerNotice(context: Context, prayerName: String) {
            ensureChannels(context)
            runCatching {
                val openPi = PendingIntent.getActivity(
                    context,
                    3,
                    Intent(context, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
                val notification = NotificationCompat.Builder(context, NOTIFY_CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_stat_update)
                    .setContentTitle("حان الآن وقت $prayerName")
                    .setContentText("حَيَّ عَلَى الصَّلَاة — تقبل الله طاعتكم")
                    .setAutoCancel(true)
                    .setContentIntent(openPi)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .build()
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                nm?.notify(4002, notification)
            }
        }
    }
}
