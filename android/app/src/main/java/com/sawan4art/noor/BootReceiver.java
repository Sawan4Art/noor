package com.sawan4art.noor;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * مستقبل إقلاع الهاتف — يعيد جدولة منبهات الأذان بعد إعادة التشغيل
 * (أندرويد يمسح كل المنبهات عند الإطفاء) ويحدّث الويدجت.
 */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        if (!Intent.ACTION_BOOT_COMPLETED.equals(action)
                && !"android.intent.action.QUICKBOOT_POWERON".equals(action)
                && !Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
            return;
        }

        try {
            SharedPreferences prefs = context.getSharedPreferences(NoorNativePlugin.PREFS, Context.MODE_PRIVATE);
            String json = prefs.getString(NoorNativePlugin.KEY_ALARMS, null);
            if (json != null) {
                JSONArray alarms = new JSONArray(json);
                AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                if (am == null) return;
                long now = System.currentTimeMillis();
                int index = 0;
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

                    PendingIntent pi = NoorNativePlugin.buildAlarmPendingIntent(context, index, payload);
                    if (pi == null) continue;
                    try {
                        if (android.os.Build.VERSION.SDK_INT >= 23) {
                            if (android.os.Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) {
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
                    index++;
                }
            }
            NoorWidgetProvider.pushUpdate(context);
        } catch (Exception ignored) {
        }
    }
}
