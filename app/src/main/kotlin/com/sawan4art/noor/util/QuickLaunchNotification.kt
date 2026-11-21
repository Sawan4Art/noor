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
 * أيقونة التشغيل السريع في شريط الإشعارات — بلاغ المالك 2026-11-11:
 * «عايز يكون للتطبيق ايقونه في شريط الاشعارات الخاص بالهاتف لتشغيل
 * سريع للتطبيق» ثم بلاغ v1.71: «الغي تفعيل اشعار التشغيل السريع و
 * استبدله بايقونه تكون موجوده في شريط الاشعارات نفسه».
 *
 * فالعقد هنا: بطاقةُ إشعارٍ دائمةٌ تظهر في شريط الإشعارات ما دام
 * مفتاحها مفعّلاً — ضغطةٌ عليها تفتح التطبيق فوراً من أينما كان
 * المستخدم، وهي «دائمة» (ongoing) فلا تُسحب من الشريط وتُغلق حين
 * يطفئها المالك من قسم «عام» في الإعدادات.
 *
 * **العدّاد التنازلي الحي (v1.80 — بلاغ المالك 2026-11-21): «اضف
 * ايقونه داخل شريط الاشعارات لعرض الوقت المتبقي و فتح التطبيق بشكل
 * سريع تنبيه ايقونه داخل ايقونات شريط الاشعارات»**: معرفة الصلاة
 * القادمة النصُّ الجانبي للإشعار «متبقي حتى أذان الفلان» مع عدّادٍ
 * تنازليٍّ **حيٍّ** يحدّث نفسه بنظام التشغيل كل دقيقة بلا خدمةٍ ولا
 * استهلاك بطارية — عبر خاصية النظام الأصلية [setUsesChronometer]
 * مع [setWhen] مستقبلي (chrono base): العدّاد يعدّ تنازلياً نحو
 * لحظة الأذان القادمة، ويُضبط عند النشر ويُجدَّد عند كل إعادة نشر
 * (الإقلاع وفتح التطبيق وتغيّر الإعدادات وتغيّر الصلاة القادمة عبر
 * [refresh] التي يستدعيها المجدول عند كل أذان). وبلا صلاةٍ قادمة
 * معروفة يظل الإشعار أيقونةَ التطبيق الخالصة (بلا نصٍّ ولا عنوان)
 * كما في v1.71.
 *
 * القناة هادئة (IMPORTANCE_MIN) فلا صوت ولا اهتزاز ولا بادج —
 * أيقونةٌ صامتةٌ للتشغيل السريع لا إزعاجاً، والإشعار نفسه صامتٌ
 * عند تحديثه (setOnlyAlertOnce) فلا يُسمع له طقطقة عند كل تجديد.
 */
object QuickLaunchNotification {

    const val CHANNEL_ID = "noor_quick_launch"
    const val NOTIFICATION_ID = 4101

    /**
     * تظهر/تتجدد الإشعار الدائم — تُستدعى من إقلاع التطبيق إذا كان
     * المفتاح مفعّلاً، ومن [refresh] عند تغيّر الصلاة القادمة.
     *
     * @param nextPrayerName اسم الصلاة القادمة («العصر»…) — فارغٌ
     *        يعني إشعارَ الأيقونة الخالصة بلا عدّاد (لا مواقيت معروفة).
     * @param nextPrayerAtMillis لحظة الأذان القادم بالمللي — عدّاد
     *        الإشعار يعدّ تنازلياً نحوها؛ صفرٌ أو لحظةٌ ماضية تُسقط
     *        العدّاد وترجع الأيقونة الخالصة.
     */
    fun show(
        context: Context,
        nextPrayerName: String = "",
        nextPrayerAtMillis: Long = 0L,
    ) {
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
                        description = "أيقونة دائمة لفتح التطبيق بسرعة"
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
            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_update)
                .setContentIntent(open)
                .setOngoing(true)
                .setSilent(true)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setCategory(NotificationCompat.CATEGORY_NAVIGATION)
                .setShowWhen(false)

            val countdownValid = nextPrayerAtMillis > System.currentTimeMillis() &&
                nextPrayerName.isNotEmpty()
            if (countdownValid) {
                // **العدّاد التنازلي الحي (v1.80)** — «ايقونه داخل شريط
                // الاشعارات لعرض الوقت المتبقي»: نظام التشغيل نفسه
                // يحدّث النص كل دقيقة (خاصية usesChronometer) بلا أي
                // خدمةٍ أو مؤقتٍ من التطبيق — العدّاد يعدّ نحو لحظة
                // الأذان القادمة (setWhen مستقبلي + setUsesChronometer)
                // فتقرأ العين الوقت المتبقي حياً في الشريط دائماً،
                // والنص الجانبي يسمّي الصلاة المنتظَرة
                builder
                    .setWhen(nextPrayerAtMillis)
                    .setUsesChronometer(true)
                    .setSubText("متبقي حتى أذان $nextPrayerName")
            } else {
                // **أيقونةٌ خالصة** — بلاغ المالك v1.71: «الغي تفعيل
                // اشعار التشغيل السريع و استبدله بايقونه تكون موجوده
                // في شريط الاشعارات نفسه» — محتوى الإشعار كله صورةُ
                // أيقونة التطبيق بلا نصٍّ ولا عنوانٍ، فتبدو بطاقةُ
                // الإشعار في الشريط أيقونةً خالصةً ضغطةٌ عليها تفتح
                // التطبيق
                builder.setContent(
                    android.widget.RemoteViews(
                        context.packageName,
                        R.layout.notification_quick_icon,
                    ),
                )
            }

            nm.notify(NOTIFICATION_ID, builder.build())
        }
    }

    /**
     * تجديد نص العدّاد عند تغيّر الصلاة القادمة — يستدعيها المجدول
     * (AlarmScheduler.reschedule) فيجدول منبهَ تجديدٍ عند لحظة كل أذانٍ
     * قادم فيتدحرج النص إلى الصلاة التالية دون فتح التطبيق. بلا صلاةٍ
     * قادمة معروفة يرجع الإشعار أيقونةً خالصة.
     */
    fun refresh(context: Context, nextPrayerName: String, nextPrayerAtMillis: Long) {
        show(context, nextPrayerName, nextPrayerAtMillis)
    }

    /** تُغلق الإشعار — حين يطفئ المالك المفتاح من الإعدادات */
    fun dismiss(context: Context) {
        runCatching {
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager)
                ?.cancel(NOTIFICATION_ID)
        }
    }
}
