package com.sawan4art.noor;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;

import androidx.core.app.NotificationCompat;

/**
 * خدمة الأذان الأمامية (Foreground Service):
 * تعزف صوت الأذان كاملاً بصوت أعلى استراحة (USAGE_ALARM) حتى لو الشاشة
 * مطفية أو التطبيق في الخلفية — مع إشعار فيه زر إيقاف.
 */
public class AdhanService extends Service {

    public static final String CHANNEL_ADHAN = "noor_adhan_channel";
    public static final String CHANNEL_NOTIFY = "noor_notify_channel";
    private static final int NOTIFICATION_ID = 2001;
    private static final int NOTIFY_ID = 2002;

    private MediaPlayer player;
    private PowerManager.WakeLock wakeLock;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createChannels(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && "stop".equals(intent.getAction())) {
            stopSelf();
            return START_NOT_STICKY;
        }

        String prayerName = intent != null ? intent.getStringExtra("name") : "الصلاة";
        if (prayerName == null) prayerName = "الصلاة";

        startForeground(NOTIFICATION_ID, buildAdhanNotification(this, prayerName));

        acquireWakeLock();
        playAdhan();
        return START_NOT_STICKY;
    }

    private void playAdhan() {
        try {
            releasePlayer();
            int resId = resolveAdhanResId();
            player = MediaPlayer.create(this, resId);
            if (player == null) {
                stopSelf();
                return;
            }
            if (Build.VERSION.SDK_INT >= 21) {
                player.setAudioAttributes(
                        new AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_ALARM)
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .build());
            }
            player.setOnCompletionListener(mp -> stopSelf());
            player.setOnErrorListener((mp, what, extra) -> {
                stopSelf();
                return true;
            });
            player.start();

            // أمان: إيقاف تلقائي بعد 6 دقائق مهما حدث
            final Service self = this;
            new android.os.Handler(android.os.Looper.getMainLooper())
                    .postDelayed(self::stopSelf, 6 * 60 * 1000);
        } catch (Exception e) {
            stopSelf();
        }
    }

    private int resolveAdhanResId() {
        SharedPreferences prefs = getSharedPreferences(NoorNativePlugin.PREFS, MODE_PRIVATE);
        String voice = prefs.getString(NoorNativePlugin.KEY_VOICE, "main");
        Resources res = getResources();
        int id = res.getIdentifier("adhan_" + voice, "raw", getPackageName());
        if (id == 0) id = res.getIdentifier("adhan_main", "raw", getPackageName());
        if (id == 0) id = res.getIdentifier("adhan_fakhry", "raw", getPackageName());
        return id;
    }

    private void acquireWakeLock() {
        try {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (pm != null) {
                wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "noor:adhan");
                wakeLock.acquire(6 * 60 * 1000L);
            }
        } catch (Exception ignored) {
        }
    }

    private void releasePlayer() {
        if (player != null) {
            try {
                if (player.isPlaying()) player.stop();
            } catch (Exception ignored) {
            }
            player.release();
            player = null;
        }
    }

    @Override
    public void onDestroy() {
        releasePlayer();
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
        super.onDestroy();
    }

    /* ============================ الإشعارات ============================ */

    static void createChannels(Context ctx) {
        if (Build.VERSION.SDK_INT < 26) return;
        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;

        NotificationChannel adhan = new NotificationChannel(
                CHANNEL_ADHAN, "الأذان", NotificationManager.IMPORTANCE_HIGH);
        adhan.setDescription("تشغيل الأذان عند وقت الصلاة");
        adhan.setSound(null, null);
        nm.createNotificationChannel(adhan);

        NotificationChannel notify = new NotificationChannel(
                CHANNEL_NOTIFY, "تنبيهات الصلاة", NotificationManager.IMPORTANCE_DEFAULT);
        nm.createNotificationChannel(notify);
    }

    private Notification buildAdhanNotification(Context ctx, String prayerName) {
        Intent stopIntent = new Intent(this, AdhanService.class);
        stopIntent.setAction("stop");
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
        PendingIntent stopPi = PendingIntent.getService(this, 1, stopIntent, flags);

        Intent openIntent = new Intent(this, MainActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent openPi = PendingIntent.getActivity(this, 2, openIntent, flags);

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(ctx, CHANNEL_ADHAN)
                        .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                        .setContentTitle("حان الآن وقت " + prayerName)
                        .setContentText("التطبيق يؤذن الآن — تقبل الله طاعتكم")
                        .setOngoing(true)
                        .setContentIntent(openPi)
                        .addAction(0, "إيقاف", stopPi)
                        .setCategory(NotificationCompat.CATEGORY_ALARM)
                        .setPriority(NotificationCompat.PRIORITY_MAX);

        return builder.build();
    }

    /** إشعار صلاة عادي (وضع الإشعار بدون أذان صوتي) */
    static void postPrayerNotification(Context ctx, String prayerName) {
        createChannels(ctx);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
        Intent openIntent = new Intent(ctx, MainActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent openPi = PendingIntent.getActivity(ctx, 3, openIntent, flags);

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(ctx, CHANNEL_NOTIFY)
                        .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                        .setContentTitle("حان الآن وقت " + prayerName)
                        .setContentText("حَيَّ عَلَى الصَّلَاة — تقبل الله طاعتكم")
                        .setAutoCancel(true)
                        .setContentIntent(openPi)
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.notify(NOTIFY_ID, builder.build());
        }
    }
}
