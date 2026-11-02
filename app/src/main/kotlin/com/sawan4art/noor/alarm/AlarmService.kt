package com.sawan4art.noor.alarm

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
import androidx.core.app.ServiceCompat
import com.sawan4art.noor.R

/**
 * خدمة المنبه الأمامية (تجهيز مرحلة الأذان بطلب المالك 2026-11-03).
 *
 * **مكانتها اليوم**: مسجّلة في المانيفست خدمةً أماميةً بنوع
 * `mediaPlayback` — النوع الذي يسمح بصوت الأذان مستمراً والشاشة
 * مطفأة (أندرويد ٨+ يقتل ما ليس خدمةً أماميةً خلال ثوانٍ من الخلفية)،
 * ولا شيء يبدأها بعد، وسلوكها عند أي بدءٍ مبكر: واجهة أمامية صامتة
 * ثم توقف هادئ — بلا انهيار مهما فعل مسارٌ مستقبليٌّ مبكر.
 *
 * **لماذا mediaPlayback**: صوت الأذان تشغيلٌ وسائطيّ حقيقي، والنوع
 * يمنح الخدمة أولوية استمرار من قاتل البطارية ويظهر شرح «يشتغل الآن»
 * الأدق في إعدادات أندرويد — وهو نفسه نوع المنبهات في تطبيقات المواقيت.
 *
 * **مرحلة الأذان القادمة** تبني فوق هذا الهيكل: مشغل الصوت (المكبر
 * على مجرى المنبه إن أمكن) مع إشعار حالي يعرض اسم الصلاة وزر إسكات —
 * والهيكل لا يمنع شيئاً منها.
 */
class AlarmService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // الدفاع: لا أحد يبدأ الخدمة اليوم — وإن فعل مسارٌ مبكر فإن
        // العقد الأمامي (إشعار خلال ٥ ثوانٍ) يُستوفى ثم تتوقف بهدوء
        runCatching { startInForeground() }
        stopSelf()
        return START_NOT_STICKY
    }

    /** واجهة أمامية صامتة قصيرة — تستوفي عقد startForegroundService */
    private fun startInForeground() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "تشغيل المنبه",
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    description = "قناة خدمة تشغيل صوت المنبه في الخلفية — بلا صوت إشعار إضافي"
                    setShowBadge(false)
                },
            )
        }
        val notification: Notification =
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_update) // مؤقتة حتى أيقونة الأذان في مرحلته
                .setContentTitle("نور المسلم")
                .setContentText("خدمة المنبه قيد التهيئة")
                .setOngoing(true)
                .build()
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            } else {
                0 // قبل ٢٩ لا أنواع — الصفر يعني «بلا نوع إضافي» سليماً
            },
        )
    }

    companion object {
        private const val CHANNEL_ID = "noor_alarm_service"
        private const val NOTIFICATION_ID = 4001
    }
}
