package com.sawan4art.noor.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.sawan4art.noor.MainActivity
import com.sawan4art.noor.R

/**
 * أيقونة التشغيل السريع في شريط الإشعارات — بلاغ المالك 2026-11-11:
 * «عايز يكون للتطبيق ايقونه في شريط الاشعارات الخاص بالهاتف لتشغيل
 * سريع للتطبيق» ثم بلاغ v1.71: «الغي تفعيل اشعار التشغيل السريع و
 * استبدله بايقونه تكون موجوده في شريط الاشعارات نفسه».
 *
 * فالعقد هنا: بطاقةُ إشعارٍ دائمةٌ محتواها **أيقونة التطبيق خالصةً**
 * (بلا نصٍّ ولا عنوانٍ ولا سطرٍ ثانٍ) تظهر في شريط الإشعارات ما دام
 * مفتاحها مفعّلاً — ضغطةٌ عليها تفتح التطبيق فوراً من أينما كان
 * المستخدم، وهي «دائمة» (ongoing) فلا تُسحب من الشريط وتُغلق حين
 * يطفئها المالك من قسم «عام» في الإعدادات.
 *
 * القناة هادئة (IMPORTANCE_MIN) فلا صوت ولا اهتزاز ولا بادج —
 * أيقونةٌ صامتةٌ للتشغيل السريع لا إزعاجاً.
 */
object QuickLaunchNotification {

    const val CHANNEL_ID = "noor_quick_launch"
    const val NOTIFICATION_ID = 4101

    /** تظهر الإشعار الدائم — تُستدعى من إقلاع التطبيق إذا كان المفتاح مفعّلاً */
    fun show(context: Context) {
        runCatching {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return
            if (android.os.Build.VERSION.SDK_INT >= 26) {
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
            val notification: Notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_update)
                // **أيقونةٌ خالصة** — بلاغ المالك v1.71: «الغي تفعيل
                // اشعار التشغيل السريع و استبدله بايقونه تكون موجوده
                // في شريط الاشعارات نفسه» — محتوى الإشعار كله صورةُ
                // أيقونة التطبيق بلا نصٍّ ولا عنوانٍ، فتبدو بطاقةُ
                // الإشعار في الشريط أيقونةً خالصةً ضغطةٌ عليها تفتح
                // التطبيق
                .setContent(
                    android.widget.RemoteViews(
                        context.packageName,
                        R.layout.notification_quick_icon,
                    ),
                )
                .setContentIntent(open)
                .setOngoing(true)
                .setSilent(true)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setCategory(NotificationCompat.CATEGORY_NAVIGATION)
                .setShowWhen(false)
                .build()
            nm.notify(NOTIFICATION_ID, notification)
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
