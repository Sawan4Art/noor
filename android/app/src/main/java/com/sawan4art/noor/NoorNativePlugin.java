package com.sawan4art.noor;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * الجسر النيتِفي لتطبيق نور المسلم:
 * - requestAllPermissions / checkPermissions: الموقع + الإشعارات + المنبهات الدقيقة + عدم الإزعاج
 * - openAppSettings / openNotificationSettings / openExactAlarmSettings / openDndAccessSettings
 * - scheduleAdhanAlarms: منبهات أذان/إقامة/سحور/قيام دقيقة تعمل والشاشة مطفية
 *   + خطة كتم أصوات الهاتف من الأذان حتى بعد الإقامة + صوت الإقامة المختار
 * - scheduleSalawatAlarms: تذكير دوري بصوت الصلاة على النبي ﷺ
 * - updateWidget: تحديث ويدجت الشاشة الرئيسية + حفظ الموقع ليعمل الويدجت وحده
 */
@CapacitorPlugin(name = "NoorNative")
public class NoorNativePlugin extends Plugin {

    static final String PREFS = "NoorNative";
    static final String KEY_ALARMS = "alarms_json";
    static final String KEY_VOICE = "adhan_voice";
    static final String KEY_CITY = "city";
    static final String KEY_WIDGET = "widget_json";
    static final String KEY_WIDGET_SETTINGS = "widget_settings_json";
    static final String KEY_IQAMAH_VOICE = "iqamah_voice";
    static final String KEY_MUTE_ENABLED = "mute_enabled";
    static final String KEY_MUTE_MINUTES = "mute_minutes";
    static final String KEY_IQAMAH_ENABLED = "iqamah_enabled";
    static final String KEY_IQAMAH_OFFSET = "iqamah_offset_minutes";
    static final String KEY_WIDGET_LAT = "widget_lat";
    static final String KEY_WIDGET_LNG = "widget_lng";
    static final String KEY_WIDGET_METHOD = "widget_method";
    static final String KEY_WIDGET_HANAFI = "widget_hanafi";
    private static final int PERMISSION_REQUEST = 4711;

    private PluginCall pendingPermissionCall;

    /* ============================ الأذونات ============================ */

    @PluginMethod
    public void requestAllPermissions(PluginCall call) {
        Activity activity = getActivity();
        if (activity == null) {
            call.reject("no activity");
            return;
        }

        List<String> wanted = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= 33
                && ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
            wanted.add(Manifest.permission.POST_NOTIFICATIONS);
        }
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            wanted.add(Manifest.permission.ACCESS_FINE_LOCATION);
            wanted.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }

        if (wanted.isEmpty()) {
            resolvePermissions(call);
            return;
        }
        pendingPermissionCall = call;
        ActivityCompat.requestPermissions(activity, wanted.toArray(new String[0]), PERMISSION_REQUEST);
    }

    @Override
    public void handleRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.handleRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST && pendingPermissionCall != null) {
            PluginCall call = pendingPermissionCall;
            pendingPermissionCall = null;
            resolvePermissions(call);
        }
    }

    private void resolvePermissions(PluginCall call) {
        Activity activity = getActivity();
        if (activity == null) {
            call.reject("no activity");
            return;
        }
        boolean notif = Build.VERSION.SDK_INT < 33
                || ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS)
                        == PackageManager.PERMISSION_GRANTED;
        boolean location = ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        boolean exact = hasExactAlarms();

        JSObject ret = new JSObject();
        ret.put("location", location);
        ret.put("notifications", notif);
        ret.put("exactAlarms", exact);
        call.resolve(ret);
    }

    /** قراءة الحالة الكاملة بلا طلب — للأزرار والإعدادات */
    @PluginMethod
    public void checkPermissions(PluginCall call) {
        JSObject ret = new JSObject();
        Activity activity = getActivity();
        boolean notif = Build.VERSION.SDK_INT < 33
                || (activity != null
                        && ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS)
                                == PackageManager.PERMISSION_GRANTED);
        boolean location = activity != null
                && ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED;
        ret.put("location", location);
        ret.put("notifications", notif);
        ret.put("exactAlarms", hasExactAlarms());
        ret.put("dndAccess", isDndAccessGrantedInternal());
        call.resolve(ret);
    }

    private boolean hasExactAlarms() {
        AlarmManager am = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);
        if (am == null) return false;
        if (Build.VERSION.SDK_INT >= 31) {
            return am.canScheduleExactAlarms();
        }
        return true;
    }

    private boolean isDndAccessGrantedInternal() {
        NotificationManager nm =
                (NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return false;
        return nm.isNotificationPolicyAccessGranted();
    }

    /* ============================ فتح إعدادات النظام ============================ */

    private void startActivitySafely(Intent intent) {
        try {
            Activity activity = getActivity();
            if (activity != null) {
                activity.startActivity(intent);
            } else {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                getContext().startActivity(intent);
            }
        } catch (Exception ignored) {
        }
    }

    @PluginMethod
    public void openAppSettings(PluginCall call) {
        try {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(android.net.Uri.parse("package:" + getContext().getPackageName()));
            startActivitySafely(intent);
            call.resolve();
        } catch (Exception e) {
            call.resolve();
        }
    }

    @PluginMethod
    public void openExactAlarmSettings(PluginCall call) {
        try {
            if (Build.VERSION.SDK_INT >= 31) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                intent.setData(android.net.Uri.parse("package:" + getContext().getPackageName()));
                startActivitySafely(intent);
            }
            call.resolve();
        } catch (Exception e) {
            call.resolve();
        }
    }

    /** فتح إعدادات إشعارات التطبيق مباشرة (إصلاح زر فتح الأذونات — 2026-10-29) */
    @PluginMethod
    public void openNotificationSettings(PluginCall call) {
        try {
            Intent intent;
            if (Build.VERSION.SDK_INT >= 26) {
                intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, getContext().getPackageName());
            } else {
                intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(android.net.Uri.parse("package:" + getContext().getPackageName()));
            }
            startActivitySafely(intent);
            call.resolve();
        } catch (Exception e) {
            call.resolve();
        }
    }

    @PluginMethod
    public void isDndAccessGranted(PluginCall call) {
        JSObject ret = new JSObject();
        ret.put("granted", isDndAccessGrantedInternal());
        call.resolve(ret);
    }

    @PluginMethod
    public void openDndAccessSettings(PluginCall call) {
        try {
            startActivitySafely(new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS));
            call.resolve();
        } catch (Exception e) {
            call.resolve();
        }
    }

    /* ============================ منبهات الأذان/الإقامة/الكتم ============================ */

    @PluginMethod
    public void scheduleAdhanAlarms(PluginCall call) {
        try {
            Context ctx = getContext();
            JSArray alarms = call.getArray("alarms");
            String voice = call.getString("voice", "main");
            String city = call.getString("city", "");
            JSObject settings = call.getObject("settings");

            boolean iqamahEnabled = settings != null && settings.optBoolean("iqamahEnabled", false);
            int iqamahOffset = settings != null ? settings.optInt("iqamahOffsetMinutes", 15) : 15;
            String iqamahVoice = settings != null ? settings.optString("iqamahVoice", "makkah") : "makkah";
            boolean muteEnabled = settings != null && settings.optBoolean("muteEnabled", false);
            int muteMinutes = settings != null ? settings.optInt("muteMinutes", 10) : 10;

            SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);

            AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
            // إلغاء القديم أولاً — النطاق الكامل 72 فتحة:
            // 0-23 أذان | 48-71 إقامة | 0-47 كتم/استعادة (مطابق لنطاق cancelAdhanAlarms)
            for (int i = 0; i < 72; i++) {
                PendingIntent pi = buildAlarmPendingIntent(ctx, i, null);
                if (pi != null) am.cancel(pi);
                PendingIntent mi = buildMutePendingIntent(ctx, i, null);
                if (mi != null) am.cancel(mi);
            }

            long now = System.currentTimeMillis();
            JSONArray kept = new JSONArray();
            int index = 0;
            if (alarms != null) {
                for (int i = 0; i < alarms.length() && index < 24; i++) {
                    JSONObject alarm = alarms.getJSONObject(i);
                    long ts = alarm.optLong("ts", 0);
                    String mode = alarm.optString("mode", "adhan");
                    if (ts <= now || "off".equals(mode)) continue;

                    JSONObject payload = new JSONObject();
                    payload.put("key", alarm.optString("key", ""));
                    payload.put("name", alarm.optString("name", ""));
                    payload.put("mode", mode);
                    payload.put("ts", ts);

                    PendingIntent pi = buildAlarmPendingIntent(ctx, index, payload);
                    if (pi == null) continue;
                    setExactSafely(am, ts, pi);

                    // كتم الأصوات: يبدأ مع الأذان الصوتي وينتهي بعد الإقامة بمدة محددة
                    if (muteEnabled && "adhan".equals(mode)) {
                        long muteEnd = ts;
                        if (iqamahEnabled) {
                            muteEnd += iqamahOffset * 60000L;
                        }
                        muteEnd += muteMinutes * 60000L;
                        JSONObject mutePayload = new JSONObject();
                        mutePayload.put("mode", "mute_on");
                        mutePayload.put("ts", ts);
                        PendingIntent mp = buildMutePendingIntent(ctx, index, mutePayload);
                        if (mp != null) setExactSafely(am, ts, mp);

                        JSONObject restorePayload = new JSONObject();
                        restorePayload.put("mode", "mute_off");
                        restorePayload.put("ts", muteEnd);
                        PendingIntent rp = buildMutePendingIntent(ctx, index + 24, restorePayload);
                        if (rp != null) setExactSafely(am, muteEnd, rp);
                    }

                    // الإقامة: منبه إقامة بعد الأذان الصوتي بالمدة المحددة
                    if (iqamahEnabled && "adhan".equals(mode)) {
                        long iqamahTs = ts + iqamahOffset * 60000L;
                        if (iqamahTs > now) {
                            JSONObject iqPayload = new JSONObject();
                            iqPayload.put("key", alarm.optString("key", ""));
                            iqPayload.put("name", alarm.optString("name", ""));
                            iqPayload.put("mode", "iqamah");
                            iqPayload.put("ts", iqamahTs);
                            PendingIntent ip = buildAlarmPendingIntent(ctx, index + 48, iqPayload);
                            if (ip != null) setExactSafely(am, iqamahTs, ip);
                            payload.put("iqamahTs", iqamahTs);
                        }
                    }

                    kept.put(payload);
                    index++;
                }
            }

            prefs.edit()
                    .putString(KEY_ALARMS, kept.toString())
                    .putString(KEY_VOICE, voice)
                    .putString(KEY_CITY, city)
                    .putString(KEY_IQAMAH_VOICE, iqamahVoice)
                    .putBoolean(KEY_MUTE_ENABLED, muteEnabled)
                    .putInt(KEY_MUTE_MINUTES, muteMinutes)
                    .putBoolean(KEY_IQAMAH_ENABLED, iqamahEnabled)
                    .putInt(KEY_IQAMAH_OFFSET, iqamahOffset)
                    .apply();

            JSObject ret = new JSObject();
            ret.put("scheduled", index);
            call.resolve(ret);
        } catch (Exception e) {
            call.reject("schedule failed: " + e.getMessage(), e);
        }
    }

    private void setExactSafely(AlarmManager am, long ts, PendingIntent pi) {
        try {
            if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) {
                // بدون إذن المنبهات الدقيقة: شبه دقيق (فرق ثوانٍ لدقائق قليلة)
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, ts, pi);
            } else {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, ts, pi);
            }
        } catch (SecurityException se) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, ts, pi);
        }
    }

    @PluginMethod
    public void cancelAdhanAlarms(PluginCall call) {
        Context ctx = getContext();
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        for (int i = 0; i < 72; i++) {
            PendingIntent pi = buildAlarmPendingIntent(ctx, i, null);
            if (pi != null) am.cancel(pi);
            PendingIntent mi = buildMutePendingIntent(ctx, i, null);
            if (mi != null) am.cancel(mi);
        }
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY_ALARMS).apply();
        call.resolve();
    }

    static PendingIntent buildAlarmPendingIntent(Context ctx, int index, JSONObject payload) {
        Intent intent = new Intent(ctx, AdhanAlarmReceiver.class);
        intent.setAction("com.sawan4art.noor.ADHAAN_" + index);
        if (payload != null) {
            intent.putExtra("key", payload.optString("key"));
            intent.putExtra("name", payload.optString("name"));
            intent.putExtra("mode", payload.optString("mode", "adhan"));
            intent.putExtra("ts", payload.optLong("ts"));
        }
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getBroadcast(ctx, 1000 + index, intent, flags);
    }

    static PendingIntent buildMutePendingIntent(Context ctx, int index, JSONObject payload) {
        Intent intent = new Intent(ctx, AdhanAlarmReceiver.class);
        intent.setAction("com.sawan4art.noor.MUTE_" + index);
        if (payload != null) {
            intent.putExtra("mode", payload.optString("mode", "mute_on"));
            intent.putExtra("ts", payload.optLong("ts"));
        }
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getBroadcast(ctx, 2000 + index, intent, flags);
    }

    /* ==================== تذكير الصلاة على النبي ﷺ ==================== */

    @PluginMethod
    public void scheduleSalawatAlarms(PluginCall call) {
        try {
            Context ctx = getContext();
            int intervalMinutes = Math.max(5, call.getInt("intervalMinutes", 30));
            AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
            if (am == null) {
                call.resolve();
                return;
            }
            cancelSalawatInternal(ctx, am);
            long first = System.currentTimeMillis() + intervalMinutes * 60000L;
            PendingIntent pi = buildSalawatPendingIntent(ctx);
            try {
                am.setInexactRepeating(AlarmManager.RTC_WAKEUP, first, intervalMinutes * 60000L, pi);
            } catch (SecurityException se) {
                am.setRepeating(AlarmManager.RTC_WAKEUP, first, intervalMinutes * 60000L, pi);
            }
            call.resolve();
        } catch (Exception e) {
            call.reject("salawat schedule failed: " + e.getMessage(), e);
        }
    }

    @PluginMethod
    public void cancelSalawatAlarms(PluginCall call) {
        AlarmManager am = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);
        if (am != null) cancelSalawatInternal(getContext(), am);
        call.resolve();
    }

    private void cancelSalawatInternal(Context ctx, AlarmManager am) {
        PendingIntent pi = buildSalawatPendingIntent(ctx);
        am.cancel(pi);
    }

    static PendingIntent buildSalawatPendingIntent(Context ctx) {
        Intent intent = new Intent(ctx, AdhanAlarmReceiver.class);
        intent.setAction("com.sawan4art.noor.SALAWAT");
        intent.putExtra("mode", "salawat");
        intent.putExtra("name", "الصلاة على النبي");
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getBroadcast(ctx, 3001, intent, flags);
    }

    /* ============================ الويدجت ============================ */

    @PluginMethod
    public void updateWidget(PluginCall call) {
        try {
            Context ctx = getContext();
            JSObject data = call.getObject("data");
            JSObject widgetSettings = call.getObject("settings");
            if (data != null) {
                SharedPreferences.Editor editor = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                        .edit()
                        .putString(KEY_WIDGET, data.toString());
                // حفظ إعدادات التخصيص الكامل للويدجت (بطلب المالك 2026-10-29)
                if (widgetSettings != null) {
                    editor.putString(KEY_WIDGET_SETTINGS, widgetSettings.toString());
                }
                // حفظ الإحداثيات ليعمل الويدجت ويحسب المواقيت بنفسه حتى بدون فتح التطبيق
                try {
                    if (data.has("latitude") && data.has("longitude")) {
                        editor.putFloat(KEY_WIDGET_LAT, (float) data.getDouble("latitude"));
                        editor.putFloat(KEY_WIDGET_LNG, (float) data.getDouble("longitude"));
                    }
                    String method = data.optString("method", "");
                    if (!method.isEmpty()) editor.putString(KEY_WIDGET_METHOD, method);
                    if (data.has("hanafi")) editor.putBoolean(KEY_WIDGET_HANAFI, data.optBoolean("hanafi"));
                } catch (Exception ignored) {
                }
                editor.apply();
            }
            NoorWidgetProvider.pushUpdate(ctx);
            call.resolve();
        } catch (Exception e) {
            call.resolve();
        }
    }

    /* ============================ التحديث داخل التطبيق (v0.4 — بلا متصفح) ============================
     * بطلب المالك: بلا متصفح — التنزيل جوه التطبيق بشريط تقدم، والتثبيت من داخل التطبيق.
     * المسار: تنزيل HttpURLConnection إلى files/updates/ → FileProvider → intent تثبيت.
     * التقدم الحي: حدث noorApkProgress {received,total,percent,done} لكل نسبة جديدة. */

    /** هل المستخدم سمح بالتثبيت من هذا التطبيق؟ (أندرويد 8+) */
    @PluginMethod
    public void canInstallApk(PluginCall call) {
        boolean can = true;
        if (Build.VERSION.SDK_INT >= 26) {
            can = getContext().getPackageManager().canRequestPackageInstalls();
        }
        JSObject ret = new JSObject();
        ret.put("can", can);
        call.resolve(ret);
    }

    /** فتح شاشة «تثبيت تطبيقات غير معروفة» لهذا التطبيق (أندرويد 8+) */
    @PluginMethod
    public void requestInstallPermission(PluginCall call) {
        try {
            if (Build.VERSION.SDK_INT >= 26
                    && !getContext().getPackageManager().canRequestPackageInstalls()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
                intent.setData(android.net.Uri.parse("package:" + getContext().getPackageName()));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                if (getActivity() != null) {
                    getActivity().startActivity(intent);
                } else {
                    getContext().startActivity(intent);
                }
            }
            call.resolve();
        } catch (Exception e) {
            call.resolve();
        }
    }

    /**
     * تنزيل الـ APK داخل التطبيق مع أحداث تقدم حية، وعند الاكتمال يفتح مثبّت النظام مباشرة.
     * الرفض بكود install_permission = لازم السماح أولاً (الواجهة تفتح الشاشة وتطلب إعادة المحاولة).
     */
    @PluginMethod
    public void downloadAndInstallApk(final PluginCall call) {
        final String url = call.getString("url", "");
        if (url == null || url.isEmpty()) {
            call.reject("empty url");
            return;
        }
        if (Build.VERSION.SDK_INT >= 26
                && !getContext().getPackageManager().canRequestPackageInstalls()) {
            call.reject("install_permission");
            return;
        }
        final Context ctx = getContext().getApplicationContext();
        Thread t = new Thread(() -> {
            File outFile = null;
            try {
                File dir = new File(ctx.getFilesDir(), "updates");
                if (!dir.exists()) dir.mkdirs();
                outFile = new File(dir, "noor-update.apk");

                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(30000);
                conn.setInstanceFollowRedirects(true);
                conn.connect();
                if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    final int code = conn.getResponseCode();
                    Activity act0 = getActivity();
                    if (act0 != null) act0.runOnUiThread(() -> call.reject("http " + code));
                    return;
                }
                long total = conn.getContentLength();
                InputStream in = conn.getInputStream();
                FileOutputStream out = new FileOutputStream(outFile);
                byte[] buf = new byte[16384];
                long received = 0;
                int lastPercent = -1;
                int n;
                while ((n = in.read(buf)) > 0) {
                    out.write(buf, 0, n);
                    received += n;
                    if (total > 0) {
                        int percent = (int) (received * 100 / total);
                        if (percent != lastPercent) {
                            lastPercent = percent;
                            emitProgress(received, total, percent, false);
                        }
                    }
                }
                out.flush();
                out.close();
                in.close();
                emitProgress(received, total > 0 ? total : received, 100, true);

                openInstaller(ctx, outFile);
                Activity act = getActivity();
                if (act != null) act.runOnUiThread(call::resolve);
            } catch (final Exception e) {
                if (outFile != null && outFile.exists() && outFile.length() == 0) {
                    // ملف ناقص — نظّفه
                    outFile.delete();
                }
                Activity act = getActivity();
                if (act != null) {
                    act.runOnUiThread(() -> call.reject("download failed: " + e.getMessage(), e));
                }
            }
        }, "noor-apk-download");
        t.start();
    }

    private void emitProgress(long received, long total, int percent, boolean done) {
        try {
            JSObject p = new JSObject();
            p.put("received", received);
            p.put("total", total);
            p.put("percent", percent);
            p.put("done", done);
            bridge.trigger("noorApkProgress", p);
        } catch (Exception ignored) {
        }
    }

    /** فتح مثبّت النظام على الـ APK المُنزّل (عبر FileProvider) */
    private void openInstaller(Context ctx, File apkFile) {
        android.net.Uri uri = FileProvider.getUriForFile(ctx,
                ctx.getPackageName() + ".fileprovider", apkFile);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "application/vnd.android.package-archive");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
        ctx.startActivity(intent);
    }

    @Override
    protected void handleOnDestroy() {
        super.handleOnDestroy();
    }
}
