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
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import androidx.core.app.NotificationCompat;

/**
 * خدمة الأذان الأمامية (Foreground Service):
 * - وضع الأذان: تعزف الأذان كاملاً بصوت أعلى استراحة (USAGE_ALARM) حتى لو
 *   الشاشة مطفية أو التطبيق في الخلفية — مع إشعار فيه زر إيقاف.
 * - وضع الإقامة: تعزف صوت الإقامة المختار من الإعدادات.
 * - وضع الصلاة على النبي ﷺ: تذكير دوري بصوت الصلاة على النبي.
 * - كتم الأصوات: عند الأذان الصوتي تُكتم كل أصوات الهاتف (عدم الإزعاج أو
 *   الوضع الصامت) وتُستعاد بمنبه مستقل بعد الإقامة بالمدة المحددة.
 */
public class AdhanService extends Service {

    public static final String CHANNEL_ADHAN = "noor_adhan_channel";
    public static final String CHANNEL_NOTIFY = "noor_notify_channel";
    private static final int NOTIFICATION_ID = 2001;
    private static final int NOTIFY_ID = 2002;
    private static final String TAG = "NoorAdhan";

    private MediaPlayer player;
    private PowerManager.WakeLock wakeLock;
    /** هل الكتم مفعّل حالياً من هذه الخدمة (لاستعادة الصوت عند الإيقاف اليدوي) */
    private boolean muteAppliedHere = false;
    private int previousRingerMode = AudioManager.RINGER_MODE_NORMAL;
    private int previousInterruptionFilter = NotificationManager.INTERRUPTION_FILTER_ALL;

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
        String action = intent != null ? intent.getAction() : null;
        if ("stop".equals(action)) {
            restoreMuteIfApplied();
            stopSelf();
            return START_NOT_STICKY;
        }

        String mode = intent != null ? intent.getStringExtra("mode") : "adhan";
        if (mode == null) mode = "adhan";
        String prayerName = intent != null ? intent.getStringExtra("name") : "الصلاة";
        if (prayerName == null) prayerName = "الصلاة";

        try {
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(NOTIFICATION_ID,
                        buildNotification(this, prayerName, mode),
                        android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
            } else {
                startForeground(NOTIFICATION_ID, buildNotification(this, prayerName, mode));
            }
        } catch (Exception e) {
            // فشل startForeground (قيود الخلفية الصارمة): نجرب الإشعار العادي ثم نخرج
            Log.w(TAG, "startForeground failed: " + e.getMessage());
            postPrayerNotification(this, prayerName);
            stopSelf();
            return START_NOT_STICKY;
        }

        acquireWakeLock();

        if ("adhan".equals(mode)) {
            applyMuteFromPrefs();
            playResource("adhan_", 6 * 60 * 1000);
        } else if ("iqamah".equals(mode)) {
            playResource("iqamah_", 3 * 60 * 1000);
        } else if ("salawat".equals(mode)) {
            playResource("salawat_", 2 * 60 * 1000);
        }
        return START_NOT_STICKY;
    }

    /* ============================ التشغيل ============================ */

    private void playResource(String prefix, int maxDurationMs) {
        try {
            releasePlayer();
            int resId = resolveVoiceResId(prefix);
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
            player.setOnCompletionListener(mp -> {
                if (!"adhan_".equals(prefix)) restoreMuteIfApplied();
                stopSelf();
            });
            player.setOnErrorListener((mp, what, extra) -> {
                stopSelf();
                return true;
            });
            player.start();

            // أمان: إيقاف تلقائي بعد الحد الأقصى مهما حدث
            final Service self = this;
            new android.os.Handler(android.os.Looper.getMainLooper())
                    .postDelayed(self::stopSelf, maxDurationMs);
        } catch (Exception e) {
            stopSelf();
        }
    }

    /** اختيار الصوت: adhan_/iqamah_/salawat_ + المفتاح المحفوظ */
    private int resolveVoiceResId(String prefix) {
        SharedPreferences prefs = getSharedPreferences(NoorNativePlugin.PREFS, MODE_PRIVATE);
        Resources res = getResources();
        String voice = "";
        if ("iqamah_".equals(prefix)) {
            voice = prefs.getString(NoorNativePlugin.KEY_IQAMAH_VOICE, "makkah");
        } else if ("adhan_".equals(prefix)) {
            voice = prefs.getString(NoorNativePlugin.KEY_VOICE, "main");
        } else if ("salawat_".equals(prefix)) {
            voice = "main";
        }
        int id = res.getIdentifier(prefix + voice, "raw", getPackageName());
        if (id == 0) id = res.getIdentifier(prefix + "main", "raw", getPackageName());
        if (id == 0) {
            // أول ملف متاح من نفس النوع
            String[] candidates = "iqamah_".equals(prefix)
                    ? new String[]{"iqamah_makkah", "iqamah_beautiful"}
                    : "salawat_".equals(prefix)
                            ? new String[]{"salawat_main"}
                            : new String[]{"adhan_main", "adhan_fakhry"};
            for (String c : candidates) {
                id = res.getIdentifier(c, "raw", getPackageName());
                if (id != 0) break;
            }
        }
        return id;
    }

    /* ============================ كتم الأصوات ============================ */

    /**
     * كتم كل أصوات الهاتف عند بدء الأذان (بطلب المالك):
     * 1) عدم الإزعاج الكامل لو مُنح الإذن (الأشمل)
     * 2) وإلا الوضع الصامت + اهتزاز مقفول
     * الاستعادة تتم بمنبه مستقل (mute_off) بعد الإقامة بالمدة المحددة،
     * وأيضاً عند الإيقاف اليدوي من الإشعار كإجراء أمان.
     */
    private void applyMuteFromPrefs() {
        try {
            SharedPreferences prefs = getSharedPreferences(NoorNativePlugin.PREFS, MODE_PRIVATE);
            if (!prefs.getBoolean(NoorNativePlugin.KEY_MUTE_ENABLED, false)) return;

            AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            previousRingerMode = audio != null ? audio.getRingerMode() : AudioManager.RINGER_MODE_NORMAL;
            previousInterruptionFilter =
                    nm != null ? nm.getCurrentInterruptionFilter() : NotificationManager.INTERRUPTION_FILTER_ALL;

            boolean applied = false;
            if (nm != null && nm.isNotificationPolicyAccessGranted()) {
                try {
                    nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE);
                    applied = true;
                } catch (Exception ignored) {
                }
            }
            if (!applied && audio != null) {
                try {
                    audio.setRingerMode(AudioManager.RINGER_MODE_SILENT);
                    applied = true;
                } catch (Exception ignored) {
                }
            }
            muteAppliedHere = applied;
        } catch (Exception ignored) {
        }
    }

    private void restoreMuteIfApplied() {
        if (!muteAppliedHere) return;
        muteAppliedHere = false;
        try {
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null && nm.isNotificationPolicyAccessGranted()) {
                try {
                    nm.setInterruptionFilter(previousInterruptionFilter);
                } catch (Exception ignored) {
                }
            }
            AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            if (audio != null && previousRingerMode != AudioManager.RINGER_MODE_SILENT) {
                try {
                    audio.setRingerMode(previousRingerMode);
                } catch (Exception ignored) {
                }
            }
        } catch (Exception ignored) {
        }
    }

    /** استعادة الصوت من منبه mute_off (يستدعى عبر المُستقبِل) */
    static void restoreMute(Context ctx) {
        try {
            NotificationManager nm =
                    (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null && nm.isNotificationPolicyAccessGranted()) {
                nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL);
            }
            AudioManager audio = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
            if (audio != null && audio.getRingerMode() == AudioManager.RINGER_MODE_SILENT) {
                audio.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
            }
        } catch (Exception ignored) {
        }
    }

    private void acquireWakeLock() {
        try {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (pm != null) {
                wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "noor:adhan");
                wakeLock.acquire(10 * 60 * 1000L);
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
                CHANNEL_ADHAN, "الأذان والإقامة", NotificationManager.IMPORTANCE_HIGH);
        adhan.setDescription("تشغيل الأذان والإقامة عند وقت الصلاة");
        adhan.setSound(null, null);
        nm.createNotificationChannel(adhan);

        NotificationChannel notify = new NotificationChannel(
                CHANNEL_NOTIFY, "تنبيهات الصلاة", NotificationManager.IMPORTANCE_DEFAULT);
        notify.setDescription("تنبيهات السحور وقيام الليل والصلاة على النبي");
        notify.enableVibration(true);
        nm.createNotificationChannel(notify);
    }

    private Notification buildNotification(Context ctx, String prayerName, String mode) {
        Intent stopIntent = new Intent(this, AdhanService.class);
        stopIntent.setAction("stop");
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
        PendingIntent stopPi = PendingIntent.getService(this, 1, stopIntent, flags);

        Intent openIntent = new Intent(this, MainActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent openPi = PendingIntent.getActivity(this, 2, openIntent, flags);

        String title;
        String text;
        switch (mode) {
            case "iqamah":
                title = "قد قامت الصلاة — " + prayerName;
                text = "حان وقت الإقامة — تقبل الله طاعتكم";
                break;
            case "salawat":
                title = "صَلُّوا عَلَى النَّبِيِّ ﷺ";
                text = "اللهم صلِّ وسلم على نبينا محمد — تذكير دوري";
                break;
            default:
                title = "حان الآن وقت " + prayerName;
                text = "التطبيق يؤذن الآن — تقبل الله طاعتكم";
        }

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(ctx, CHANNEL_ADHAN)
                        .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                        .setContentTitle(title)
                        .setContentText(text)
                        .setOngoing(true)
                        .setContentIntent(openPi)
                        .addAction(0, "إيقاف", stopPi)
                        .setCategory(NotificationCompat.CATEGORY_ALARM)
                        .setPriority(NotificationCompat.PRIORITY_MAX);

        return builder.build();
    }

    /** إشعار صامت/هزّاز للتنبيهات: قيام الليل، السحور، ونواقص الأذان */
    static void postPrayerNotification(Context ctx, String prayerName) {
        postPrayerNotification(ctx, prayerName, null);
    }

    static void postPrayerNotification(Context ctx, String prayerName, String body) {
        createChannels(ctx);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
        Intent openIntent = new Intent(ctx, MainActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent openPi = PendingIntent.getActivity(ctx, 3, openIntent, flags);

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(ctx, CHANNEL_NOTIFY)
                        .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                        .setContentTitle(prayerName)
                        .setContentText(body != null ? body : "حَيَّ عَلَى الصَّلَاة — تقبل الله طاعتكم")
                        .setAutoCancel(true)
                        .setContentIntent(openPi)
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.notify(NOTIFY_ID + prayerName.hashCode(), builder.build());
        }
    }
}
