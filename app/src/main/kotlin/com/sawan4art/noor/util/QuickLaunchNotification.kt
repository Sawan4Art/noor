package com.sawan4art.noor.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.sawan4art.noor.MainActivity
import com.sawan4art.noor.R

/**
 * إشعار «الوقت المتبقي» الدائم في شريط الإشعارات — عقدٌ تطوّر ببلاغات
 * المالك: v1.71 أيقونةٌ خالصة ثم v1.80 عدّادٌ تنازليٌّ حيٌّ ثم **v1.81
 * (البلاغ الحالي): «انا عايز ايقونه شريط اشعارات زي اللي موجودين في
 * الصوره مش مجرد اشعار ثابت الاشعار الثابت ده شيله Quick Settings
 * Tile يكون داعم ليها»** — فأُلغي وضع الأيقونة الثابتة (بطاقة
 * RemoteViews الخالصة) نهائيًا: الإشعار الآن دائمًا إشعارٌ عاديٌّ
 * مختصرٌ مثل بقية التطبيقات — أيقونةٌ صغيرةٌ في شريط الحالة وعنوانه
 * «متبقي حتى أذان العصر» مع عدّادٍ تنازليٍّ **حيٍّ** يحدّثه نظام
 * التشغيل نفسه كل دقيقة (setUsesChronometer + setWhen مستقبلي — بلا
 * خدمةٍ ولا استهلاك بطاريةٍ إطلاقاً)، ويدعمه [QuickLaunchTileService]
 * بلاطةٌ في الإعدادات السريعة بنفس القيمة لحظة السحب.
 *
 * يُضبط عند النشر ويُجدَّد عند كل تغيّرٍ للصلاة القادمة (فتح التطبيق/
 * تغيّر الإعدادات/منبه QUICK_REFRESH عند كل أذان — عبر [QuickLaunch]
 * .refreshFromSettings الذي يستدعي هذا بالصلاة القادمة الحية).
 *
 * القناة هادئة (IMPORTANCE_MIN) فلا صوت ولا اهتزاز ولا بادج،
 * والإشعار صامتٌ عند تحديثه (setOnlyAlertOnce).
 */
object QuickLaunchNotification {

    const val CHANNEL_ID = "noor_quick_launch"
    const val NOTIFICATION_ID = 4101

    /**
     * نشر/تجديد الإشعار الدائم — [nextPrayerName] و[nextPrayerAtMillis]
     * إلزاميان من الصلاة القادمة الحية (احسبهما عبر [QuickLaunch]
     * .computeNextTarget)؛ ولحظةٌ ماضيةٌ تُسقط العدّاد وتُبقي الإشعار
     * بالعنوان العام كي لا يعرض عدّاداً على صفرٍ أبداً.
     */
    fun show(context: Context, nextPrayerName: String, nextPrayerAtMillis: Long) {
        runCatching {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return
            if (Build.VERSION.SDK_INT >= 26) {
                nm.createNotificationChannel(
                    NotificationChannel(
                        CHANNEL_ID,
                        "التشغيل السريع",
                        NotificationManager.IMPORTANCE_MIN,
                    ).apply {
                        description = "إشعار الوقت المتبقي حتى أذان الصلاة القادمة"
                        setShowBadge(false)
                    },
                )
            }
            val open = PendingIntent.getActivity(
                context,
                NOTIFICATION_ID,
                Intent(context, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val countdownValid = nextPrayerAtMillis > System.currentTimeMillis() &&
                nextPrayerName.isNotBlank()
            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_update)
                .setContentTitle(
                    if (countdownValid) "متبقي حتى أذان $nextPrayerName"
                    else "نور المسلم — التشغيل السريع",
                )
                .setContentIntent(open)
                .setOngoing(true)
                .setSilent(true)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setCategory(NotificationCompat.CATEGORY_NAVIGATION)
                .setShowWhen(false)

            if (countdownValid) {
                // **العدّاد التنازلي الحي** — نظام التشغيل يحدّث النص كل
                // دقيقة (خاصية usesChronometer) بلا أي خدمةٍ أو مؤقتٍ
                // من التطبيق — والعدّاد يعدّ نحو لحظة الأذان القادمة
                builder
                    .setWhen(nextPrayerAtMillis)
                    .setUsesChronometer(true)
            }

            nm.notify(NOTIFICATION_ID, builder.build())
        }
    }

    /** تُغلق الإشعار — حين يطفئ المالك المفتاح من الإعدادات */
    fun dismiss(context: Context) {
        runCatching {
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager)
                ?.cancel(NOTIFICATION_ID)
        }
    }
}
