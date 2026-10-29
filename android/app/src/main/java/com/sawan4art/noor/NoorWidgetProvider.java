package com.sawan4art.noor;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;
import android.util.TypedValue;
import android.widget.RemoteViews;

import org.json.JSONObject;

import java.util.Calendar;

/**
 * ويدجت مواقيت الصلاة لشاشة الهاتف الرئيسية (4×2):
 * - يحسب المواقيت نيتِفياً بنفسه من آخر موقع محفوظ (القاهرة افتراضياً) —
 *   إصلاح جذر لمشكلة «الويدجت فارغة لا تعمل»: لم تعد تعتمد على فتح التطبيق.
 * - اسم التطبيق + المدينة + الصلاة القادمة ووقتها وعداد تنازلي حي (Chronometer)
 * - صف المواقيت الستة مع إبراز القادمة
 * - نسختان: عادية بخلفية داكنة (NoorWidgetProvider) وشفافة بلا خلفية
 *   (NoorWidgetTransparentProvider) — كل واحدة تظهر كخيار مستقل في قائمة الويدجتس
 * - يتحدث كل 30 دقيقة تلقائياً + فور كل ما فتح المستخدم التطبيق
 *   + منبه تحديث لحظة انتهاء العداد (عند دخول وقت الصلاة القادمة)
 */
public class NoorWidgetProvider extends AppWidgetProvider {

    private static final String TAG = "NoorWidget";
    static final String ACTION_REFRESH = "com.sawan4art.noor.WIDGET_REFRESH";

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] appWidgetIds) {
        pushUpdate(context);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        String action = intent != null ? intent.getAction() : null;
        if (ACTION_REFRESH.equals(action)) {
            pushUpdate(context);
        }
    }

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        pushUpdate(context);
    }

    /** التخطيط المستخدم في هذه النسخة — الفئة الشفافة تعيد تعريفه */
    protected RemoteViews createViews(Context context) {
        return new RemoteViews(context.getPackageName(), R.layout.widget_noor);
    }

    /** تحديث كل نسخ الويدجت — بحساب المواقيت محلياً دائماً */
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
        try {
            int[] ids = manager.getAppWidgetIds(provider);
            if (ids == null || ids.length == 0) return;

            RemoteViews views = impl.createViews(context);
            WidgetSnapshot snap = computeSnapshot(context);
            impl.applySnapshot(context, views, snap);

            // الضغط على الويدجت يفتح التطبيق
            Intent open = new Intent(context, MainActivity.class);
            open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
            PendingIntent pi = PendingIntent.getActivity(context, 10, open, flags);
            views.setOnClickPendingIntent(R.id.widget_root, pi);

            manager.updateAppWidget(ids, views);
        } catch (Exception e) {
            Log.w(TAG, "update failed: " + e.getMessage());
        }
    }

    /** جدولة تحديث لحظة انتهاء العداد (وقت الصلاة القادمة) لكلا النسختين */
    private static void scheduleRefresh(Context context, long nextTs) {
        try {
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (am == null) return;
            long when = nextTs + 2000L;
            if (when <= System.currentTimeMillis()) return;

            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;

            Intent i1 = new Intent(context, NoorWidgetProvider.class);
            i1.setAction(ACTION_REFRESH);
            PendingIntent p1 = PendingIntent.getBroadcast(context, 4001, i1, flags);

            Intent i2 = new Intent(context, NoorWidgetTransparentProvider.class);
            i2.setAction(ACTION_REFRESH);
            PendingIntent p2 = PendingIntent.getBroadcast(context, 4002, i2, flags);

            try {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, p1);
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, p2);
            } catch (SecurityException se) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, p1);
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, p2);
            }
        } catch (Exception ignored) {
        }
    }

    /* ============================ الحساب ============================ */

    private static final class WidgetSnapshot {
        String city = "";
        String nextName = "";
        String nextTime = "";
        long nextTs = 0;
        String timesRow = "";
        String nextKey = "";
    }

    /**
     * لقطة المواقيت: تُحسب نيتِفياً من آخر موقع محفوظ — الويدجت تعمل
     * فور إضافتها حتى قبل أول فتح للتطبيق (القاهرة افتراضياً).
     */
    private static WidgetSnapshot computeSnapshot(Context context) {
        WidgetSnapshot snap = new WidgetSnapshot();
        WidgetCustomization customization = WidgetCustomization.load(context);
        boolean use24h = customization.time24h;
        try {
            SharedPreferences prefs =
                    context.getSharedPreferences(NoorNativePlugin.PREFS, Context.MODE_PRIVATE);

            double lat = prefs.getFloat(NoorNativePlugin.KEY_WIDGET_LAT, 30.0444f);
            double lng = prefs.getFloat(NoorNativePlugin.KEY_WIDGET_LNG, 31.2357f);
            String method = prefs.getString(NoorNativePlugin.KEY_WIDGET_METHOD, "Egypt");
            boolean hanafi = prefs.getBoolean(NoorNativePlugin.KEY_WIDGET_HANAFI, false);
            snap.city = prefs.getString(NoorNativePlugin.KEY_CITY, "القاهرة، مصر");
            if (snap.city == null || snap.city.isEmpty()) snap.city = "القاهرة، مصر";

            double fajrAngle = 19.5;
            double ishaAngle = 17.5;
            switch (method) {
                case "Makkah": fajrAngle = 18.5; ishaAngle = 17.5; break;
                case "MWL": fajrAngle = 18.0; ishaAngle = 17.0; break;
                case "ISNA": fajrAngle = 15.0; ishaAngle = 15.0; break;
                case "Karachi": fajrAngle = 18.0; ishaAngle = 18.0; break;
                case "Tehran": fajrAngle = 17.7; ishaAngle = 14.0; break;
                default: break; // Egypt
            }

            Calendar now = Calendar.getInstance();
            long nowMs = now.getTimeInMillis();

            double[] today = WidgetPrayerTimes.computeHours(now, lat, lng, fajrAngle, ishaAngle, hanafi);
            String[] keys = {"fajr", "sunrise", "dhuhr", "asr", "maghrib", "isha"};
            String[] names = {"الفجر", "الشروق", "الظهر", "العصر", "المغرب", "العشاء"};

            long[] todayEpoch = new long[6];
            StringBuilder row = new StringBuilder();
            long nextTs = 0;
            String nextName = "";
            String nextTime = "";
            String nextKey = "";
            // نظام الوقت من التخصيص: 12 ساعة ص/م أو 24 ساعة (بطلب المالك)
            java.util.function.BiFunction<Long, Calendar, String> fmt = use24h
                    ? WidgetPrayerTimes::formatTime24
                    : WidgetPrayerTimes::formatTime12;

            for (int i = 0; i < 6; i++) {
                todayEpoch[i] = WidgetPrayerTimes.hoursToEpoch(now, today[i]);
            }

            // الصلاة القادمة: من مواقيت اليوم، وإلا فجر الغد
            for (int i = 0; i < 6; i++) {
                if (todayEpoch[i] > nowMs) {
                    nextTs = todayEpoch[i];
                    nextName = names[i];
                    nextKey = keys[i];
                    nextTime = fmt.apply(nextTs, now);
                    break;
                }
            }

            // صف المواقيت مع إبراز القادمة
            for (int i = 0; i < 6; i++) {
                if (i > 0) row.append("   ");
                if (keys[i].equals(nextKey)) row.append("◉ ");
                row.append(names[i]).append(" ")
                        .append(fmt.apply(todayEpoch[i], now));
            }

            // لو كل مواقيت اليوم فاتت — فجر الغد
            if (nextTs == 0) {
                Calendar tomorrow = (Calendar) now.clone();
                tomorrow.add(Calendar.DAY_OF_MONTH, 1);
                double[] tm = WidgetPrayerTimes.computeHours(tomorrow, lat, lng, fajrAngle, ishaAngle, hanafi);
                nextTs = WidgetPrayerTimes.hoursToEpoch(tomorrow, tm[0]);
                nextName = names[0];
                nextKey = keys[0];
                nextTime = fmt.apply(nextTs, tomorrow);
                row.setLength(0);
                for (int i = 0; i < 6; i++) {
                    if (i > 0) row.append("   ");
                    long e = WidgetPrayerTimes.hoursToEpoch(tomorrow, tm[i]);
                    if (keys[i].equals(nextKey)) row.append("◉ ");
                    row.append(names[i]).append(" ").append(fmt.apply(e, tomorrow));
                }
            }

            snap.nextTs = nextTs;
            snap.nextName = nextName;
            snap.nextTime = nextTime;
            snap.nextKey = nextKey;
            snap.timesRow = row.toString();

            scheduleRefresh(context, nextTs);
        } catch (Exception e) {
            Log.w(TAG, "compute failed: " + e.getMessage());
        }
        return snap;
    }

    /** تطبيق اللقطة على التخطيط مع التخصيص الكامل (بطلب المالك 2026-10-29):
     *  لون التمييز + حجم الخط + إظهار/إخفاء كل عنصر + شفافية الخلفية */
    protected void applySnapshot(Context context, RemoteViews views, WidgetSnapshot snap) {
        WidgetCustomization c = WidgetCustomization.load(context);

        // ===== الترويسة: اسم التطبيق + المدينة =====
        if (c.showAppName) {
            views.setViewVisibility(R.id.widget_app_name, android.view.View.VISIBLE);
        } else {
            views.setViewVisibility(R.id.widget_app_name, android.view.View.GONE);
        }
        if (c.showCity && snap.city != null && !snap.city.isEmpty()) {
            views.setViewVisibility(R.id.widget_city, android.view.View.VISIBLE);
            views.setTextViewText(R.id.widget_city, snap.city);
        } else {
            views.setViewVisibility(R.id.widget_city, android.view.View.GONE);
        }

        // ===== الصلاة القادمة + العداد =====
        if (c.showNextPrayer && snap.nextTs > 0) {
            views.setViewVisibility(R.id.widget_next_row, android.view.View.VISIBLE);
            views.setTextViewText(R.id.widget_next_name, snap.nextName);
            views.setTextViewText(R.id.widget_next_time, snap.nextTime);
        } else {
            views.setViewVisibility(R.id.widget_next_row, android.view.View.GONE);
        }

        if (c.showCountdown && snap.nextTs > 0) {
            views.setViewVisibility(R.id.widget_countdown, android.view.View.VISIBLE);
            long diff = snap.nextTs - System.currentTimeMillis();
            if (diff > 0) {
                // setCountDown (API 24+) يجعل العداد ينازلياً موجباً: المتبقي 12:34
                // بدل الأرقام السالبة الافتراضية — عبر setBoolean بالانعكاس
                try {
                    views.setBoolean(R.id.widget_countdown, "setCountDown", true);
                } catch (Exception ignored) {
                }
                views.setChronometer(
                        R.id.widget_countdown,
                        SystemClock.elapsedRealtime() + diff,
                        "المتبقي %s",
                        true);
            } else {
                views.setChronometer(
                        R.id.widget_countdown,
                        SystemClock.elapsedRealtime(), "المتبقي %s", false);
            }
        } else {
            views.setViewVisibility(R.id.widget_countdown, android.view.View.GONE);
        }

        // ===== صف المواقيت الستة =====
        if (c.showTimesRow) {
            views.setViewVisibility(R.id.widget_times_row, android.view.View.VISIBLE);
            views.setTextViewText(R.id.widget_times_row, snap.timesRow);
        } else {
            views.setViewVisibility(R.id.widget_times_row, android.view.View.GONE);
        }

        // ===== التخصيص: الألوان والأحجام =====
        views.setTextColor(R.id.widget_app_name, c.accentColor);
        views.setTextColor(R.id.widget_city, soften(c.accentColor));
        views.setTextColor(R.id.widget_next_time, c.accentColor);
        views.setTextColor(R.id.widget_countdown, c.secondaryColor);

        views.setTextViewTextSize(R.id.widget_app_name, TypedValue.COMPLEX_UNIT_SP, 12f * c.textScale);
        views.setTextViewTextSize(R.id.widget_city, TypedValue.COMPLEX_UNIT_SP, 10f * c.textScale);
        views.setTextViewTextSize(R.id.widget_next_name, TypedValue.COMPLEX_UNIT_SP, 20f * c.textScale);
        views.setTextViewTextSize(R.id.widget_next_time, TypedValue.COMPLEX_UNIT_SP, 18f * c.textScale);
        views.setTextViewTextSize(R.id.widget_countdown, TypedValue.COMPLEX_UNIT_SP, 14f * c.textScale);
        views.setTextViewTextSize(R.id.widget_times_row, TypedValue.COMPLEX_UNIT_SP, 10f * c.textScale);

        // خلفية النسخة الداكنة حسب الشفافية المختارة — الشفافة بلا خلفية
        if (useOpaqueBackground()) {
            views.setInt(R.id.widget_root, "setBackgroundResource", c.bgDrawableRes);
        } else {
            views.setInt(R.id.widget_root, "setBackgroundResource", android.graphics.Color.TRANSPARENT);
        }
    }

    /** النسخة العادية بخلفية — الشفافة تعيد تعريفه */
    protected boolean useOpaqueBackground() {
        return true;
    }

    /** تفتيح لون النص الثانوي (المدينة) بشفافية */
    private static int soften(int color) {
        return (color & 0x00FFFFFF) | 0xB3000000;
    }

    /**
     * تخصيص الويدجت الكامل — يُقرأ من SharedPreferences (يُكتب من إعدادات التطبيق):
     * أي قيمة مفقودة أو تالفة ترجع للافتراضي — لا تفشل الويدجت أبداً.
     */
    private static final class WidgetCustomization {
        int accentColor = Color.parseColor("#6ee7b7");
        int secondaryColor = Color.parseColor("#e2e8f0");
        float textScale = 1.0f;
        boolean showAppName = true;
        boolean showCity = true;
        boolean showNextPrayer = true;
        boolean showCountdown = true;
        boolean showTimesRow = true;
        boolean time24h = false;
        int bgDrawableRes = R.drawable.widget_bg_100;

        static WidgetCustomization load(Context context) {
            WidgetCustomization c = new WidgetCustomization();
            try {
                SharedPreferences prefs =
                        context.getSharedPreferences(NoorNativePlugin.PREFS, Context.MODE_PRIVATE);
                String json = prefs.getString(NoorNativePlugin.KEY_WIDGET_SETTINGS, null);
                if (json == null || json.isEmpty()) return c;
                JSONObject o = new JSONObject(json);

                // لون التمييز — نفس خمسة ألوان واجهة التطبيق
                String accent = o.optString("accent", "green");
                switch (accent) {
                    case "gold":   c.accentColor = Color.parseColor("#fbbf24"); break;
                    case "sky":    c.accentColor = Color.parseColor("#7dd3fc"); break;
                    case "violet": c.accentColor = Color.parseColor("#c4b5fd"); break;
                    case "rose":   c.accentColor = Color.parseColor("#fda4af"); break;
                    default:       c.accentColor = Color.parseColor("#6ee7b7"); break;
                }

                // حجم الخط
                String textSize = o.optString("textSize", "medium");
                if ("small".equals(textSize)) c.textScale = 0.85f;
                else if ("large".equals(textSize)) c.textScale = 1.2f;
                else c.textScale = 1.0f;

                // إظهار/إخفاء العناصر
                c.showAppName = o.optBoolean("showAppName", true);
                c.showCity = o.optBoolean("showCity", true);
                c.showNextPrayer = o.optBoolean("showNextPrayer", true);
                c.showCountdown = o.optBoolean("showCountdown", true);
                c.showTimesRow = o.optBoolean("showTimesRow", true);

                // نظام الوقت: 12 ساعة ص/م (افتراضي) أو 24 ساعة
                c.time24h = "24h".equals(o.optString("timeFormat", "12h"));

                // شفافية خلفية النسخة الداكنة (0/25/50/75/100)
                int bgOpacity = o.optInt("bgOpacity", 100);
                if (bgOpacity <= 0) c.bgDrawableRes = R.drawable.widget_bg_0;
                else if (bgOpacity <= 25) c.bgDrawableRes = R.drawable.widget_bg_25;
                else if (bgOpacity <= 50) c.bgDrawableRes = R.drawable.widget_bg_50;
                else if (bgOpacity <= 75) c.bgDrawableRes = R.drawable.widget_bg_75;
                else c.bgDrawableRes = R.drawable.widget_bg_100;
            } catch (Exception e) {
                Log.w(TAG, "widget customization parse failed: " + e.getMessage());
            }
            return c;
        }
    }
}
