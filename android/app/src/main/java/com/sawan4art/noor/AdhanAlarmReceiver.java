package com.sawan4art.noor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * مستقبل منبه الأذان — يشتغل في وقت الصلاة حتى لو الشاشة مطفية:
 * وضع الأذان الصوتي ← تشغيل خدمة前台 تعزف الأذان كاملاً
 * وضع الإشعار ← إشعار عادي باسم الصلاة
 */
public class AdhanAlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String name = intent.getStringExtra("name");
        String mode = intent.getStringExtra("mode");
        if (name == null) name = "الصلاة";
        if (mode == null) mode = "adhan";

        if ("notification".equals(mode)) {
            AdhanService.postPrayerNotification(context, name);
            return;
        }

        Intent service = new Intent(context, AdhanService.class);
        service.putExtra("name", name);
        try {
            if (android.os.Build.VERSION.SDK_INT >= 26) {
                context.startForegroundService(service);
            } else {
                context.startService(service);
            }
        } catch (Exception e) {
            // fallback: إشعار بدون خدمة
            AdhanService.postPrayerNotification(context, name);
        }
    }
}
