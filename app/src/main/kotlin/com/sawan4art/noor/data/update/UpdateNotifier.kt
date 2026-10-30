package com.sawan4art.noor.data.update

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.sawan4art.noor.MainActivity
import com.sawan4art.noor.R

/**
 * إشعار التحديث — قناة مخصصة عالية الأهمية كي يظهر مع كل نسخة جديدة،
 * وضغطه يفتح التطبيق ويطلب فحصاً فورياً فيفتح ملخص «ما الجديد» وتسلسل التنزيل.
 * الإشعار الواحد لكل نسخة يضمنه المدير (يخزن آخر نسخة أُشعر عنها).
 */
object UpdateNotifier {

    const val CHANNEL_ID = "app_updates"
    private const val NOTIFICATION_ID = 3001

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            "تحديثات التطبيق",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "إشعار عند صدور نسخة جديدة من نور المسلم مع ملخص ما الجديد"
            setShowBadge(true)
        }
        manager.createNotificationChannel(channel)
    }

    /** هل نستطيع الإشعار فعلاً (إذن 33+ ممنوح)؟ */
    fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED

    fun notifyNewVersion(context: Context, info: UpdateInfo) {
        if (!canNotify(context)) return
        ensureChannel(context)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(UpdateManager.EXTRA_OPEN_UPDATE, true)
        }
        val pending = PendingIntent.getActivity(
            context,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_update)
            .setContentTitle("تحديث جديد: ${info.versionName}")
            .setContentText(ChangelogFormatter.firstLine(info.body))
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(ChangelogFormatter.notificationSummary(info.body)),
            )
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SYSTEM)
            .build()

        context.getSystemService(Context.NOTIFICATION_SERVICE)
            .let { it as NotificationManager }
            .notify(NOTIFICATION_ID, notification)
    }
}
