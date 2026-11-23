package com.sawan4art.noor.audio

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.sawan4art.noor.MainActivity
import com.sawan4art.noor.R
import com.sawan4art.noor.domain.mushaf.MushafSurahs
import com.sawan4art.noor.domain.mushaf.QuranReciters
import com.sawan4art.noor.ui.mushaf.MushafPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * خدمة التلاوة الأمامية بإشعارٍ تفاعليٍّ ثابتٍ — v1.83 (بلاغ المالك:
 * «عايز اشعار تفاعلي ثابت في شريط الاشعارات للتحكم في تشغيل او ايقاف
 * الصوت او حتى قفل الصوت مع ايقاف التطبيق»):
 *
 * • **الإشعار التفاعلي الثابت** — بطاقةٌ دائمةٌ في شريط الإشعارات ما
 *   دامت تلاوةٌ قائمةً (حتى الموقوفة مؤقتاً): اسم الشيخ والسورة
 *   والآية الجارية تتحدث لحظةً بلحظة، وأزرارُه تشغيل/إيقاف مؤقت
 *   وإيقاف كامل — بلا فتحٍ للتطبيق.
 * • **قفل الصوت مع إيقاف التطبيق** — الخدمة الأمامية تُبقي العملية
 *   حيّة والمشغّل (المحرك الوحيد [MushafPlayer] داخل العملية نفسها)
 *   يعمل وأنت أغلقتَ التطبيق كله: الصوت «مقفولٌ» على آخره حتى تضغط
 *   «إيقاف» من الإشعار نفسه.
 * • **المرآة لا المالك** — الخدمة لا تدير صوتاً قط: كل قرارات
 *   التشغيل والتسلسل والتحضير في المحرك؛ الخدمة تعكس حالته في
 *   الإشعار وتحمل ضغطات الأزرار إليه، فلا مصدرَين للحقيقة.
 * • **النهاية مهذبة** — حين يعود المحرك حالةً فارغة (انتهى المُدى
 *   أو أُوقف) تُغلق الخدمة نفسها والإشعار معها بلا إبقاءٍ عبثي.
 */
class QuranAudioService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var foregroundShown = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        ensureChannel(this)
        // مرآة المحرك — كل تغييرٍ حالةٍ يعيد رسم الإشعار، والفراغ يُغلق
        scope.launch {
            MushafPlayer.state.collect { st ->
                val idle = st == MushafPlayer.State()
                if (idle && foregroundShown) {
                    stopForegroundCompat()
                    stopSelf()
                } else if (!idle) {
                    startForeground(NOTIFICATION_ID, buildNotification(this@QuranAudioService))
                    foregroundShown = true
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PAUSE -> MushafPlayer.pause()
            ACTION_RESUME -> MushafPlayer.resume()
            ACTION_STOP -> {
                MushafPlayer.stop()
                stopForegroundCompat()
                stopSelf()
            }
            else -> {
                // SYNC — التزامن عند البدء: الأمام فوراً حتى لو تأخر
                // مجمع الحالة إطاراً
                startForeground(NOTIFICATION_ID, buildNotification(this))
                foregroundShown = true
            }
        }
        // START_NOT_STICKY — إعادة البناء بعد قتل النظام بلا تشغيلٍ
        // معلقٍ أفضل من إشعارٍ بلا صوتٍ خلفه؛ الضغطة التالية في
        // المصحف تعيد كل شيء
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        stopForegroundCompat()
        super.onDestroy()
    }

    private fun stopForegroundCompat() {
        runCatching { stopForeground(STOP_FOREGROUND_REMOVE) }
        foregroundShown = false
    }

    /** بناء بطاقة الإشعار من حالة المحرك الحالية */
    private fun buildNotification(context: Context): android.app.Notification {
        val st = MushafPlayer.state.value
        val active = MushafPlayer.activeAyah.value ?: st.track
        val reciter = runCatching { QuranReciters.byId(MushafPlayer.currentReciterId).name }
            .getOrDefault("")
        val sura = active?.let { MushafSurahs.name(it.sura) } ?: ""
        val ayah = active?.ayah?.let { MushafSurahs.arabicDigits(it) } ?: ""

        val open = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val playing = st.playing
        val deleteIntent = PendingIntent.getService(
            context,
            ACTION_STOP.hashCode(),
            Intent(context, QuranAudioService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val title = when {
            st.loading && st.track == null -> "جاري تحضير التلاوة…"
            st.loading -> "جاري تحضير التلاوة…"
            playing -> "التلاوة الجارية — $reciter"
            else -> "التلاوة موقوفة مؤقتاً — $reciter"
        }
        val text = when {
            sura.isBlank() -> "نور المسلم"
            ayah.isBlank() -> sura
            else -> "سورة $sura • آية $ayah"
        }

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_update)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(open)
            .setOngoing(playing || st.loading)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            // ثابت النظام Notification.CATEGORY_PLAYBACK نصاً — عبر فحصِ
            // التجميع التعجّب من مرجع الثابت فنصُّه الرسمي يُلغى الالتباس
            .setCategory("android.media.category.playback")
            .setShowWhen(false)
            .setDeleteIntent(deleteIntent)
            // زرّا التحكم — تشغيل/إيقاف مؤقت وإيقافٍ كاملٍ (بلاغ
            // المالك: «التحكم في تشغيل او ايقاف الصوت»)
            .addAction(
                if (playing) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                if (playing) "إيقاف مؤقت" else "تشغيل",
                piOf(if (playing) ACTION_PAUSE else ACTION_RESUME),
            )
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "إيقاف", piOf(ACTION_STOP))
            .build()
    }

    private fun piOf(action: String): PendingIntent = PendingIntent.getService(
        this,
        action.hashCode(),
        Intent(this, QuranAudioService::class.java).setAction(action),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    companion object {
        /** معرّف قناة الإشعار — منفصلةٌ عن إشعار التشغيل السريع */
        const val CHANNEL_ID = "noor_quran_audio"
        const val NOTIFICATION_ID = 4100

        const val ACTION_SYNC = "com.sawan4art.noor.audio.SYNC"
        const val ACTION_PAUSE = "com.sawan4art.noor.audio.PAUSE"
        const val ACTION_RESUME = "com.sawan4art.noor.audio.RESUME"
        const val ACTION_STOP = "com.sawan4art.noor.audio.STOP"

        /** قناة الإشعار — منخفضة الأهمية وصامتة تماماً */
        fun ensureChannel(context: Context) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(
                        CHANNEL_ID,
                        "التلاوة الجارية",
                        NotificationManager.IMPORTANCE_LOW,
                    ).apply { setSound(null, null) },
                )
            }
        }

        /** بدء الخدمة عند انطلاق تلاوةٍ — الأمام فوراً (بلاغ المالك) */
        fun start(context: Context) {
            val ctx = context.applicationContext
            ensureChannel(ctx)
            runCatching {
                ctx.startForegroundService(
                    Intent(ctx, QuranAudioService::class.java).setAction(ACTION_SYNC),
                )
            }
        }

        /** إيقاف الخدمة والإشعار معها — عند وقف التلاوة من التطبيق */
        fun stop(context: Context) {
            val ctx = context.applicationContext
            runCatching { ctx.startService(Intent(ctx, QuranAudioService::class.java).setAction(ACTION_STOP)) }
        }
    }
}
