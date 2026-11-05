package com.sawan4art.noor

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sawan4art.noor.data.SettingsRepository
import com.sawan4art.noor.data.update.UpdateManager
import com.sawan4art.noor.ui.NoorApp
import com.sawan4art.noor.ui.home.HomeViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    /**
     * الأذونات تُطلب في حوارات منفصلة بالتتابع — درس v0.4.2:
     * الطلب المجمّع يجعل بعض الأجهزة تعرض حوار الموقع فقط وتُسقط الباقي صامتاً.
     *
     * **جدولة السلسلة** (توجيه المالك 2026-11-06: «خلي التطبيق يطلب
     * الاوذنات كلها بعد فتح التطبيق لاول مره و بعد صدور تحديث جديد يتم
     * اخذ الاوذنات ثانيه للموافقه عليها من قبل المستخدم»): السلسلة تعمل
     * عند أول فتح (ذاكرة النسخة ٠) ثم تتكرر مع كل نسخة جديدة — تُحفظ
     * نسخة آخر طلب في الإعدادات فلا تُزعج المستخدم كل فتح، ومع أي
     * تحديث تُؤخذ الأذونات مرة أخرى للموافقة عليها.
     *
     * السلسلة الكاملة:
     * 1) الموقع الدقيق + التقريبي — المواقيت والقبلة.
     * 2) الإشعارات (33+) — تنبيهات الأذان والتحديث.
     * 3) المنبهات الدقيقة (31-32) — مواعيد الأذان بالثانية؛ على 33+
     *    USE_EXACT_ALARM ممنوح تلقائياً فلا خطوة.
     * 4) نافذة التنبيه كاملة الشاشة (34+) — انبثاق الأذان فوق القفل؛
     *    إن عطّلها المستخدم تُطلب من صفحة النظام الخاصة بها.
     * 5) عدم الإزعاج («امنح التطبيق اذونات عدم الازعاج») — لكتم
     *    أصوات الهاتف كاملاً بعد الإقامة؛ صفحة النظام الخاصة به.
     * أما بقية الأذونات في المانيفست (الإنترنت، الاهتزاز، الاستيقاظ،
     * الإقلاع، تثبيت التحديث) فممنوحة تلقائياً بلا حوار نظام.
     */
    private val steps = ArrayDeque<() -> Unit>()
    private var permissionFlowStarted = false

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        // بغض النظر عن النتيجة: الخطوة التالية — والمرفوض نهائياً
        // يمرّ صامتاً فوراً فتنهي السلسلة بالحالة الفعلية.
        launchNextStep()
    }

    /** خطوات صفحات النظام (المنبهات الدقيقة/كامل الشاشة/عدم الإزعاج):
     *  لا حاجة للنتيجة — الرجوع من الصفحة يكمل السلسلة بالحالة الفعلية */
    private val systemPageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { launchNextStep() }

    private fun enqueueMissingRuntimePermissions() {
        val fine = Manifest.permission.ACCESS_FINE_LOCATION
        val coarse = Manifest.permission.ACCESS_COARSE_LOCATION
        val notifications = Manifest.permission.POST_NOTIFICATIONS

        if (ContextCompat.checkSelfPermission(this, fine) != PackageManager.PERMISSION_GRANTED) {
            steps.addLast { permissionLauncher.launch(arrayOf(fine, coarse)) }
        }
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, notifications) != PackageManager.PERMISSION_GRANTED
        ) {
            steps.addLast { permissionLauncher.launch(arrayOf(notifications)) }
        }
    }

    /** المنبهات الدقيقة: شاشة النظام على 31-32 — على 33+ ممنوحة تلقائياً */
    private fun enqueueExactAlarmStep() {
        if (Build.VERSION.SDK_INT !in 31..32) return
        val alarm = getSystemService(ALARM_SERVICE) as? AlarmManager ?: return
        val canSchedule = runCatching { alarm.canScheduleExactAlarms() }.getOrDefault(true)
        if (!canSchedule) {
            steps.addLast {
                runCatching {
                    systemPageLauncher.launch(
                        Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM),
                    )
                }.onFailure { launchNextStep() } // جهاز لا يعرّف الشاشة — نكمل
            }
        }
    }

    /** نافذة كاملة الشاشة (34+): انبثاق الأذان فوق القفل — إن كانت
     *  معطلة على 14+ تُطلب من صفحتها (الممنوحة تثبيتاً على الأقدم) */
    private fun enqueueFullScreenStep() {
        if (Build.VERSION.SDK_INT < 34) return
        val nm = getSystemService(NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val allowed = runCatching { nm.canUseFullScreenIntent() }.getOrDefault(true)
        if (!allowed) {
            steps.addLast {
                runCatching {
                    systemPageLauncher.launch(
                        Intent(android.provider.Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT)
                            .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, packageName),
                    )
                }.onFailure { launchNextStep() }
            }
        }
    }

    /** عدم الإزعاج: كتم أصوات الهاتف كاملاً بعد الإقامة — صفحة النظام
     *  إن لم يُمنح الإذن بعد (توجيه المالك: «امنح التطبيق اذونات عدم الازعاج») */
    private fun enqueueDndStep() {
        val nm = getSystemService(NOTIFICATION_SERVICE) as? NotificationManager ?: return
        if (!nm.isNotificationPolicyAccessGranted) {
            steps.addLast {
                runCatching {
                    systemPageLauncher.launch(
                        Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS),
                    )
                }.onFailure { launchNextStep() }
            }
        }
    }

    private fun launchNextStep() {
        steps.removeFirstOrNull()?.invoke() ?: run {
            // السلسلة اكتملت — نسخة الطلب تُحفظ فلا يعيد نفسها قبل التحديث القادم
            lifecycleScope.launch {
                runCatching {
                    SettingsRepository(this@MainActivity)
                        .setPermissionsAskedVersion(BuildConfig.VERSION_CODE)
                }
            }
        }
    }

    /** هل تستحق هذه النسخة سلسلة طلب الأذونات؟ أول تشغيل أو نسخة جديدة */
    private fun shouldRunPermissionFlow(): Boolean {
        val asked = runCatching {
            kotlinx.coroutines.runBlocking {
                SettingsRepository(this@MainActivity).permissionsAskedVersionOnce()
            }
        }.getOrDefault(0)
        return asked != BuildConfig.VERSION_CODE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        applyImmersiveNav()
        handleUpdateIntent(intent)
        setContent {
            val viewModel: HomeViewModel = viewModel()
            NoorApp(viewModel)
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        // رجوع التركيز (من حوار أو من تطبيق آخر) يعيد الإخفاء — النظام
        // قد يُظهر الأشرطة عند التنقل بين النوافذ
        if (hasFocus) applyImmersiveNav()
    }

    /**
     * الشريط النظامي للهاتف يختفي عند فتح التطبيق ويظهر بسحب
     * المستخدم من الأسفل لأعلى (alpha28 توجيه المالك: «شريط التنقل
     * الخاص بالهاتف يخطي على شريط التنقل الخاص بالتطبيق .. عايز
     * شريط التنقل بتاع الهاتف يختفي عند فتح التطبيق و يظهر اذا سحب
     * المستخدم من الاسفل الى فوق»):
     *
     * - إخفاء شريط التنقل فقط — شريط الحالة يبقى ظاهراً كي لا يفقد
     *   المستخدم الوقت والبطارية.
     * - السلوك BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE: السحب من
     *   الأسفل يُظهره **عابراً فوق محتوى التطبيق** (overlay) ثم يختفي
     *   وحده بعد لحظات — لا يدفع محتوى التطبيق ولا يبقى عالقاً فوق
     *   شريط التطبيق الزجاجي، فانقلاب «الشريطين فوق بعض» انتهى.
     * - توافق كامل مع الأجهزة القديمة (WindowInsetsControllerCompat
     *   يعمل من API 24، وشريط الإيماءات وشريط الأزرار كلاهما مغطى).
     */
    private fun applyImmersiveNav() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.navigationBars())
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // الإشعار يفتح التطبيق وهو حي — نفس المسار: فحص فوري فيفتح «ما الجديد»
        handleUpdateIntent(intent)
    }

    /** ضغط إشعار التحديث → إلغاء أي استبعاد + فحص فوري بلا سقف زمني
     *  فيظهر ملخص ما الجديد دائماً بضغطة الإشعار (alpha37) */
    private fun handleUpdateIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(UpdateManager.EXTRA_OPEN_UPDATE, false) == true) {
            UpdateManager.openFromNotification()
        }
    }

    override fun onResume() {
        super.onResume()
        // الانطلاق في سلسلة الحوارات بعد أول ظهور فقط (يلزم حالة STARTED)،
        // ولأول تشغيل أو نسخة جديدة فقط — توجيه المالك 2026-11-06
        if (!permissionFlowStarted && shouldRunPermissionFlow()) {
            permissionFlowStarted = true
            steps.clear()
            enqueueMissingRuntimePermissions()
            enqueueExactAlarmStep()
            enqueueFullScreenStep()
            enqueueDndStep()
            launchNextStep()
        }
    }
}
