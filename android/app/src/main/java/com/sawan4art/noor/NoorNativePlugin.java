package com.sawan4art.noor;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * الجسر النيتِفي لتطبيق نور المسلم:
 * - requestAllPermissions: الموقع + الإشعارات + فحص المنبهات الدقيقة
 * - scheduleAdhanAlarms: منبهات أذان دقيقة تعمل والشاشة مطفية
 * - updateWidget: تحديث ويدجت الشاشة الرئيسية
 */
@CapacitorPlugin(name = "NoorNative")
public class NoorNativePlugin extends Plugin {

    static final String PREFS = "NoorNative";
    static final String KEY_ALARMS = "alarms_json";
    static final String KEY_VOICE = "adhan_voice";
    static final String KEY_CITY = "city";
    static final String KEY_WIDGET = "widget_json";
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

    private boolean hasExactAlarms() {
        AlarmManager am = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);
        if (am == null) return false;
        if (Build.VERSION.SDK_INT >= 31) {
            return am.canScheduleExactAlarms();
        }
        return true;
    }

    @PluginMethod
    public void openExactAlarmSettings(PluginCall call) {
        try {
            if (Build.VERSION.SDK_INT >= 31) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                intent.setData(android.net.Uri.parse("package:" + getContext().getPackageName()));
                getActivity().startActivity(intent);
            }
            call.resolve();
        } catch (Exception e) {
            call.resolve();
        }
    }

    /* ============================ منبهات الأذان ============================ */

    @PluginMethod
    public void scheduleAdhanAlarms(PluginCall call) {
        try {
            Context ctx = getContext();
            JSArray alarms = call.getArray("alarms");
            String voice = call.getString("voice", "main");
            String city = call.getString("city", "");

            SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);

            AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
            // إلغاء القديم أولاً
            for (int i = 0; i < 24; i++) {
                PendingIntent pi = buildAlarmPendingIntent(ctx, i, null);
                if (pi != null) am.cancel(pi);
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
                    try {
                        if (Build.VERSION.SDK_INT >= 23) {
                            if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) {
                                // بدون إذن المنبهات الدقيقة: شبه دقيق (فرق ثوانٍ لدقائق قليلة)
                                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, ts, pi);
                            } else {
                                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, ts, pi);
                            }
                        } else {
                            am.setExact(AlarmManager.RTC_WAKEUP, ts, pi);
                        }
                    } catch (SecurityException se) {
                        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, ts, pi);
                    }
                    kept.put(payload);
                    index++;
                }
            }

            prefs.edit()
                    .putString(KEY_ALARMS, kept.toString())
                    .putString(KEY_VOICE, voice)
                    .putString(KEY_CITY, city)
                    .apply();

            JSObject ret = new JSObject();
            ret.put("scheduled", index);
            call.resolve(ret);
        } catch (Exception e) {
            call.reject("schedule failed: " + e.getMessage(), e);
        }
    }

    @PluginMethod
    public void cancelAdhanAlarms(PluginCall call) {
        Context ctx = getContext();
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        for (int i = 0; i < 24; i++) {
            PendingIntent pi = buildAlarmPendingIntent(ctx, i, null);
            if (pi != null) am.cancel(pi);
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

    /* ============================ الويدجت ============================ */

    @PluginMethod
    public void updateWidget(PluginCall call) {
        try {
            Context ctx = getContext();
            JSObject data = call.getObject("data");
            if (data != null) {
                ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                        .edit()
                        .putString(KEY_WIDGET, data.toString())
                        .apply();
            }
            NoorWidgetProvider.pushUpdate(ctx);
            call.resolve();
        } catch (Exception e) {
            call.resolve();
        }
    }

    @Override
    protected void handleOnDestroy() {
        super.handleOnDestroy();
    }
}
