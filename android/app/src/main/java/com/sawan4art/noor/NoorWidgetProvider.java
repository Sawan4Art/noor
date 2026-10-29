package com.sawan4art.noor;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.SystemClock;
import android.widget.RemoteViews;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * ويدجت مواقيت الصلاة لشاشة الهاتف الرئيسية (4×2):
 * - اسم التطبيق + المدينة + الصلاة القادمة ووقتها وعداد تنازلي حي (Chronometer)
 * - صف المواقيت الستة مع إبراز القادمة
 * - نسختان: عادية بخلفية داكنة (NoorWidgetProvider) وشفافة بلا خلفية
 *   (NoorWidgetTransparentProvider) — كل واحدة تظهر كخيار مستقل في قائمة الويدجتس
 * - يتحدث كل 30 دقيقة تلقائياً + فوراً كل ما فتح المستخدم التطبيق
 */
public class NoorWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] appWidgetIds) {
        pushUpdate(context);
    }

    /** التخطيت المستخدم في هذه النسخة — الفئة الشفافة تعيد تعريفه */
    protected RemoteViews createViews(Context context) {
        return new RemoteViews(context.getPackageName(), R.layout.widget_noor);
    }

    /** تحديث كل نسخ الويدجت من البيانات المحفوظة */
    static void pushUpdate(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        if (manager == null) return;

        // النسخة العادية
        updateProviderInstances(context, manager,
                new ComponentName(context, NoorWidgetProvider.class),
                new NoorWidgetProvider());
        // النسخة الشفافة
        updateProviderInstances(context, manager,
                new ComponentName(context, NoorWidgetTransparentProvider.class),
                new NoorWidgetTransparentProvider());
    }

    private static void updateProviderInstances(
            Context context, AppWidgetManager manager, ComponentName provider, NoorWidgetProvider impl) {
        int[] ids = manager.getAppWidgetIds(provider);
        if (ids == null || ids.length == 0) return;

        SharedPreferences prefs =
                context.getSharedPreferences(NoorNativePlugin.PREFS, Context.MODE_PRIVATE);
        String json = prefs.getString(NoorNativePlugin.KEY_WIDGET, null);

        RemoteViews views = impl.createViews(context);

        if (json == null) {
            views.setTextViewText(R.id.widget_city, "افتح التطبيق أولاً لتحديث المواقيت");
            views.setTextViewText(R.id.widget_next_name, "نور المسلم");
            views.setTextViewText(R.id.widget_next_time, "🕌");
            views.setChronometer(R.id.widget_countdown, SystemClock.elapsedRealtime(), null, false);
            views.setTextViewText(R.id.widget_times_row, "");
        } else {
            try {
                JSONObject data = new JSONObject(json);
                String city = data.optString("city", "");
                JSONObject next = data.optJSONObject("next");
                JSONArray order = data.optJSONArray("order");
                long nextTs = next != null ? next.optLong("ts", 0) : 0;

                views.setTextViewText(R.id.widget_city, city);

                if (next != null) {
                    views.setTextViewText(R.id.widget_next_name, next.optString("name", ""));
                    views.setTextViewText(R.id.widget_next_time, next.optString("time", ""));
                    long diff = nextTs - System.currentTimeMillis();
                    if (diff > 0) {
                        views.setChronometer(
                                R.id.widget_countdown,
                                SystemClock.elapsedRealtime() + diff,
                                "المتبقي %s",
                                true);
                    } else {
                        views.setChronometer(
                                R.id.widget_countdown,
                                SystemClock.elapsedRealtime(), null, false);
                    }
                }

                // صف المواقيت الستة
                if (order != null && order.length() > 0) {
                    String nextKey = next != null ? next.optString("key", "") : "";
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < order.length(); i++) {
                        JSONObject p = order.getJSONObject(i);
                        String key = p.optString("key", "");
                        String label = shortName(key);
                        if (i > 0) sb.append("   ");
                        if (key.equals(nextKey)) sb.append("◉ ");
                        sb.append(label).append(" ").append(p.optString("time", ""));
                    }
                    views.setTextViewText(R.id.widget_times_row, sb.toString());
                }
            } catch (Exception ignored) {
            }
        }

        // الضغط على الويدجت يفتح التطبيق
        Intent open = new Intent(context, MainActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
        PendingIntent pi = PendingIntent.getActivity(context, 10, open, flags);
        views.setOnClickPendingIntent(R.id.widget_root, pi);

        manager.updateAppWidget(ids, views);
    }

    private static String shortName(String key) {
        switch (key) {
            case "fajr": return "الفجر";
            case "sunrise": return "الشروق";
            case "dhuhr": return "الظهر";
            case "asr": return "العصر";
            case "maghrib": return "المغرب";
            case "isha": return "العشاء";
            case "qiyam": return "قيام الليل";
            default: return key;
        }
    }
}
