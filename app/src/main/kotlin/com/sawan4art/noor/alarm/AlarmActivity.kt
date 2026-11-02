package com.sawan4art.noor.alarm

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity

/**
 * شاشة المنبه (تجهيز مرحلة الأذان بطلب المالك 2026-11-03).
 *
 * **مكانتها اليوم**: هيكل جاهز مسجّل في المانيفست بخصائصه الكاملة —
 * لا شيء يطلقها بعد، ولا تُظهر أي واجهة (تُنهي نفسها بهدوء إن أُطلقت
 * بلا حمل منبه صالح) كي لا يكسر أي مسارٍ مستقبليٍّ مبكر شيء.
 *
 * **خصائص فوق القفل**:
 * - أندرويد 8.1+ (٢٧+): تقرأ النظام خصيتَي المانيفست
 *   `showWhenLocked` و`turnScreenOn` مباشرة فتظهر الشاشة فوق القفل
 *   وتُشعل الشاشة بلا حاجة لكلمة السر.
 * - أندرويد 7-8.0 (٢٤-٢٦): الأعلام المكافئة على النافذة هي الطريق
 *   الوحيد (FLAG_SHOW_WHEN_LOCKED/FLAG_TURN_SCREEN_ON) — تُضاف هنا.
 * - FLAG_KEEP_SCREEN_ON في الحالتين: شاشة التنبيه تبقى مضاءة ما دامت
 *   ظاهرة — المستخدم لا يفوّت أذان الصلاة بسبب خمول الشاشة.
 *
 * **الإذن TURN_SCREEN_ON** (أندرويد ١٤+): معلن في المانيفست — متطلب
 * إشعال الشاشة في الإصدارات الحديثة، وجميع ما سبق يغطي ٧ حتى ١٥.
 *
 * **مرحلة الأذان القادمة** تبني فوق هذا الهيكل: قراءة حمل المنبه
 * (اسم الصلاة والوقت) من الإكسترا ثم واجهة التنبيه بلغة التطبيق
 * السائلة — والهيكل لا يمنع شيئاً منها.
 */
class AlarmActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyOverLockFlags()
        // لا حمل منبه صالح؟ لا شيء يطلقها اليوم أصلاً — وبأي حال
        // السلوك الآمن: انتهاء هادئ بلا واجهة ناقصة ولا انهيار
        if (intent?.getStringExtra(EXTRA_PRAYER_NAME) == null) {
            finish()
        }
        // واجهة التنبيه تُبنى في مرحلة الأذان القادمة فوق هذا الهيكل
    }

    /** أعلام فوق القفل — الخصائص في المانيفست (٢٧+) والأعلام هنا (٢٤-٢٦) */
    @Suppress("DEPRECATION") // FLAG_SHOW_WHEN_LOCKED أُهمل في ٢٧ بديلاً الخصائص — ويلزم للأقدم
    private fun applyOverLockFlags() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1) {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    companion object {
        /** إكسترا اسم الصلاة — عقد مرحلة الأذان القادمة (حالياً بلا مُطلِق) */
        const val EXTRA_PRAYER_NAME: String = "noor_extra_alarm_prayer_name"

        /** إكسترا وقت الصلاة الملقن — عقد مرحلة الأذان القادمة */
        const val EXTRA_PRAYER_TIME: String = "noor_extra_alarm_prayer_time"
    }
}
