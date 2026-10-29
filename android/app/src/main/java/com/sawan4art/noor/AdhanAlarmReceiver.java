package com.sawan4art.noor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * مستقبل منبهات نور المسلم — يشتغل في وقته حتى لو الشاشة مطفية:
 * - وضع الأذان: تشغيل خدمة أمامية تعزف الأذان كاملاً + تفعيل الكتم
 * - وضع الإقامة: تشغيل صوت الإقامة المختار
 * - وضع الصلاة على النبي ﷺ: تذكير دوري بصوت الصلاة على النبي
 * - وضع الإشعار: إشعار عادي (قيام الليل، السحور، نواقص الأذان)
 * - mute_off: استعادة كل أصوات الهاتف بعد انتهاء مدة الكتم
 */
public class AdhanAlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        String name = intent.getStringExtra("name");
        String mode = intent.getStringExtra("mode");
        if (name == null) name = "الصلاة";
        if (mode == null) mode = "adhan";

        // استعادة أصوات الهاتف بعد انتهاء الكتم
        if ("mute_off".equals(mode)) {
            AdhanService.restoreMute(context);
            return;
        }

        // الإشعار الصامت: قيام الليل والسحور ونواقص الأذان
        if ("notification".equals(mode)) {
            String body;
            if ("suhoor".equals(intent.getStringExtra("key"))) {
                body = "قوموا للسحور — «تسحروا فإن في السحور بركة»";
            } else if ("qiyam".equals(intent.getStringExtra("key"))) {
                body = "حان وقت قيام الليل — «أفضل الصلاة بعد الفريضة صلاة الليل»";
            } else {
                body = "حَيَّ عَلَى الصَّلَاة — تقبل الله طاعتكم";
            }
            AdhanService.postPrayerNotification(context, name, body);
            return;
        }

        // الأذان / الإقامة / الصلاة على النبي — كلها تُشغَّل بالخدمة الأمامية
        Intent service = new Intent(context, AdhanService.class);
        service.putExtra("name", name);
        service.putExtra("mode", mode);
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
