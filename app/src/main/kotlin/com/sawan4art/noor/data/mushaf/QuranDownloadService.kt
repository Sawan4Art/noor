package com.sawan4art.noor.data.mushaf

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.sawan4art.noor.R
import com.sawan4art.noor.domain.mushaf.MushafSurahs
import com.sawan4art.noor.domain.mushaf.QuranReciters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * خدمة التنزيل الأمامية (alpha57 — بلاغ المالك: «تحميل اصوات الشيوخ
 * مش شغال»): التنزيل نفسه يعيش في [QuranAudioLibrary] بمجاله المستقل،
 * لكن العملية نفسها كانت تُقتل لو أغلق المستخدم التطبيق من الريسنتس
 * أو نام الجهاز في التوفير — فالتنزيل «شغال» في الكود وميتٌ في الميدان.
 *
 * الخدمة الأمامية بإشعارٍ حيّ (نسبة التقدم من حالة المكتبة نفسها)
 * تُبقي العملية حيّة ما دام ثمة تنزيل جارٍ، وتنتهي وحدها حين لا يعمل
 * شيء. الإيقاف من زر الواجهة يبقى كما هو: [QuranAudioLibrary.cancel]
 * تلحظ الخدمةُ السكونَ فتنتهي.
 *
 * النوع dataSync — أندرويد ١٤ يشترط التصريح بنوع الخدمة الأمامية،
 * والإذن FOREGROUND_SERVICE_DATA_SYNC في المانيفست معه.
 */
class QuranDownloadService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var watcher: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val reciterId = intent?.getStringExtra(EXTRA_RECITER) ?: return START_NOT_STICKY
        val sura = intent.getIntExtra(EXTRA_SURA, 0)
        val counts = intent.getIntArrayExtra(EXTRA_COUNTS)?.let { arr ->
            (1..114).mapIndexed { i, surah -> surah to arr[i] }.toMap()
        } ?: emptyMap()

        startForegroundCompat()

        // التنزيل نفسه في المكتبة (مجالها المستقل) — والخدمة تُبقي العملية حية
        QuranAudioLibrary.start(this, reciterId, sura, counts)

        if (watcher?.isActive != true) {
            watcher = scope.launch {
                while (true) {
                    delay(1_500)
                    val active = QuranAudioLibrary.downloads.value.values.any { it.active }
                    if (!active) break
                    notifyProgress()
                }
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun startForegroundCompat() {
        val manager = getSystemService(NOTIFICATION_SERVICE) as? NotificationManager
        if (Build.VERSION.SDK_INT >= 26 && manager != null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID, "تحميل أصوات التلاوة", NotificationManager.IMPORTANCE_LOW,
                ).apply { setShowBadge(false) },
            )
        }
        val notification = buildNotification(0, 1, "")
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    /** إشعار حيّ بأول تنزيلٍ جارٍ — نسبته من حالة المكتبة نفسها */
    private fun notifyProgress() {
        val manager = getSystemService(NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val active = QuranAudioLibrary.downloads.value.values.firstOrNull { it.active } ?: return
        val reciter = QuranReciters.byId(active.reciterId)
        val where = if (active.sura in 1..114) {
            "سورة ${MushafSurahs.name(active.sura)}"
        } else {
            "المصحف كاملاً"
        }
        manager.notify(
            NOTIFICATION_ID,
            buildNotification(active.done, active.total, "${reciter.name} — $where"),
        )
    }

    private fun buildNotification(done: Int, total: Int, text: String): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("جارٍ تحميل صوت التلاوة")
            .setContentText(text.ifEmpty { "يبدأ التنزيل…" })
            .setProgress(total.coerceAtLeast(1), done.coerceAtMost(total.coerceAtLeast(1)), false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()

    companion object {
        private const val CHANNEL_ID = "quran_audio_downloads"
        private const val NOTIFICATION_ID = 4101
        private const val EXTRA_RECITER = "reciter_id"
        private const val EXTRA_SURA = "sura"
        private const val EXTRA_COUNTS = "surah_counts"

        /**
         * تشغيل تنزيل (شيخ × سورة — صفر للمصحف كاملاً) تحت الخدمة
         * الأمامية: العملية تبقى حية ولو أُغلق التطبيق كله.
         */
        fun start(context: Context, reciterId: String, sura: Int, surahCounts: Map<Int, Int>) {
            val intent = Intent(context, QuranDownloadService::class.java)
                .putExtra(EXTRA_RECITER, reciterId)
                .putExtra(EXTRA_SURA, sura)
                .putExtra(EXTRA_COUNTS, IntArray(114) { surahCounts[it + 1] ?: 0 })
            runCatching {
                if (Build.VERSION.SDK_INT >= 26) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            }
        }
    }
}
