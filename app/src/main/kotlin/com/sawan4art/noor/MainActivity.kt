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
import com.sawan4art.noor.domain.permissions.PermissionGate
import com.sawan4art.noor.ui.NoorApp
import com.sawan4art.noor.ui.home.HomeViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    /**
     * الأذونات تُطلب في حوارات منفصلة بالتتابع — درس v0.4.2:
     * الطلب المجمّع يجعل بعض الأجهزة تعرض حوار الموقع فقط وتُسقط الباقي صامتاً.
     *
     * **بوابة الصلاحيات** (alpha57 — بلاغ المالك: «في مشكله مع كل فتح
     * للتطبيق بيطلب اذن عدم الازعاج انا عايز طالما الاوذنات كلها تعمل
     * لا يطلب التطبيق الاذن مره اخرى الا اذا تم الغاء اذونات منها …
     * و لو حتى حدث المستخدم التطبيق ألا اذا طبعا اضفنا اذن اخر مستقبلا»):
     * لكل إذنٍ حالةٌ في [PermissionGate] تُخزَّن في DataStore — الممنوح
     * لا يُزاح أبداً حتى بعد أي تحديث، وغيرُ المسأل قطّ يُطلب مرة
     * (أول فتح أو إذنٌ جديد أضفناه مستقبلاً)، والممنوح الذي فُقد
     * يُطلب مرةً واحدة كي يعمل التطبيق، والمرفوض بلا سابقة منح لا
     * يُلحّ. القاعدة القديمة «ذاكرة النسخة» بلا معنى الآن.
     *
     * السلسلة الكاملة (بترتيبها المعهود):
     * 1) الموقع الدقيق + التقريبي — المواقيت والقبلة.
     * 2) الإشعارات (33+) — تنبيهات الأذان والتحديث.
     * 3) المنبهات الدقيقة (31-32) — مواعيد الأذان بالثانية؛ على 33+
     *    USE_EXACT_ALARM ممنوح تلقائياً فلا خطوة.
     * 4) نافذة التنبيه كاملة الشاشة (34+) — انبثاق الأذان فوق القفل.
     * 5) عدم الإزعاج — لكتم أصوات الهاتف كاملاً بعد الإقامة.
     * أما بقية الأذونات في المانيفست (الإنترنت، الاهتزاز، الاستيقاظ،
     * الإقلاع، تثبيت التحديث) فممنوحة تلقائياً بلا حوار نظام.
     */
    private val steps = ArrayDeque<() -> Unit>()
    private var permissionFlowStarted = false

    /** مفاتيح السلسلة الجارية — تُحفظ حالتها عند اكتمالها */
    private var askedKeys: List<String> = emptyList()

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

    /** الوضع الحقيقي لكل إذن الآن — أساس قرار البوابة وحفظها بعدا */
    private fun currentlyGranted(): Map<String, Boolean> {
        val fine = Manifest.permission.ACCESS_FINE_LOCATION
        val notifications = Manifest.permission.POST_NOTIFICATIONS
        val nm = getSystemService(NOTIFICATION_SERVICE) as? NotificationManager
        val alarm = getSystemService(ALARM_SERVICE) as? AlarmManager
        return mapOf(
            "location" to (
                ContextCompat.checkSelfPermission(this, fine) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED
                ),
            "notifications" to (
                Build.VERSION.SDK_INT < 33 ||
                    ContextCompat.checkSelfPermission(this, notifications) == PackageManager.PERMISSION_GRANTED
                ),
            "exact_alarm" to (
                Build.VERSION.SDK_INT !in 31..32 ||
                    (alarm != null && runCatching { alarm.canScheduleExactAlarms() }.getOrDefault(true))
                ),
            "full_screen" to (
                Build.VERSION.SDK_INT < 34 ||
                    (nm != null && runCatching { nm.canUseFullScreenIntent() }.getOrDefault(true))
                ),
            "dnd" to (nm != null && runCatching { nm.isNotificationPolicyAccessGranted }.getOrDefault(false)),
        )
    }

    /** خطوة واحدة لكل مفتاحٍ من مفاتيح البوابة المقرر طلبه */
    private fun enqueueStep(key: String) {
        val fine = Manifest.permission.ACCESS_FINE_LOCATION
        val coarse = Manifest.permission.ACCESS_COARSE_LOCATION
        val notifications = Manifest.permission.POST_NOTIFICATIONS
        when (key) {
            "location" -> {
                if (ContextCompat.checkSelfPermission(this, fine) != PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, coarse) != PackageManager.PERMISSION_GRANTED
                ) {
                    steps.addLast { permissionLauncher.launch(arrayOf(fine, coarse)) }
                }
            }
            "notifications" -> {
                if (Build.VERSION.SDK_INT >= 33 &&
                    ContextCompat.checkSelfPermission(this, notifications) != PackageManager.PERMISSION_GRANTED
                ) {
                    steps.addLast { permissionLauncher.launch(arrayOf(notifications)) }
                }
            }
            "exact_alarm" -> {
                if (Build.VERSION.SDK_INT !in 31..32) return
                steps.addLast {
                    runCatching {
                        systemPageLauncher.launch(
                            Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM),
                        )
                    }.onFailure { launchNextStep() } // جهاز لا يعرّف الشاشة — نكمل
                }
            }
            "full_screen" -> {
                if (Build.VERSION.SDK_INT < 34) return
                steps.addLast {
                    runCatching {
                        systemPageLauncher.launch(
                            Intent(android.provider.Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT)
                                .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, packageName),
                        )
                    }.onFailure { launchNextStep() }
                }
            }
            "dnd" -> {
                steps.addLast {
                    runCatching {
                        systemPageLauncher.launch(
                            Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS),
                        )
                    }.onFailure { launchNextStep() }
                }
            }
        }
    }

    private fun launchNextStep() {
        steps.removeFirstOrNull()?.invoke() ?: run {
            // السلسلة اكتملت — حالة البوابة تُحفظ بالوضع الفعلي الآن:
            // المعطي ممنوح، والرافض مسجّل مرفوضاً بلا سابقة منح فلا إلحاح بعده
            lifecycleScope.launch {
                runCatching {
                    val settings = SettingsRepository(this@MainActivity)
                    val state = PermissionGate.fromJson(settings.permissionGateJsonOnce())
                    val asked = PermissionGate.markAsked(state, askedKeys, currentlyGranted())
                    settings.setPermissionGateJson(PermissionGate.toJson(asked))
                }
            }
        }
    }

    /**
     * **بوابة الصلاحيات** — تُقرأ حالة كل إذنٍ من DataStore وتُقارن
     * بالوضع الحقيقي: يُبنى طلبٌ فقط لمن يلزمه — أولُ مرةٍ أو إذنٌ
     * جديد أو ممنوحٌ فُقد — والباقي يمضي صامتاً ما دام يعمل.
     */
    private fun shouldRunPermissionFlow(): Pair<List<String>, Map<String, Boolean>>? {
        val state = runCatching {
            kotlinx.coroutines.runBlocking {
                PermissionGate.fromJson(SettingsRepository(this@MainActivity).permissionGateJsonOnce())
            }
        }.getOrDefault(emptyMap())
        val now = currentlyGranted()
        val toAsk = PermissionGate.decide(state, now)
        return if (toAsk.isEmpty()) null else toAsk to now
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        applyImmersiveNav()
        handleUpdateIntent(intent)
        openWidgetSettings.value =
            intent?.action == com.sawan4art.noor.widget.NoorWidget.ACTION_OPEN_WIDGET_SETTINGS
        openWidgetId.value = if (openWidgetSettings.value) {
            intent?.getIntExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: -1
        } else {
            -1
        }
        setContent {
            val viewModel: HomeViewModel = viewModel()
            NoorApp(viewModel, openWidgetSettings, openWidgetId)
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
        // ضغطة الويدجت والنشاط حي — نفس المسار: فتح قسم إعدادات الويدجت (alpha63)
        if (intent.action == com.sawan4art.noor.widget.NoorWidget.ACTION_OPEN_WIDGET_SETTINGS) {
            openWidgetSettings.value = true
            openWidgetId.value =
                intent.getIntExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
        }
    }

    /** ضغطة الويدجت على الشاشة تفتح قسم إعداداتها داخل التطبيق (alpha63) */
    private val openWidgetSettings = androidx.compose.runtime.mutableStateOf(false)

    /** ومعرّف الويدجت المضغوطة — المحرر يحرّر تخصيصها هي (الأشكال الستة) */
    private val openWidgetId = androidx.compose.runtime.mutableIntStateOf(-1)

    /** ضغط إشعار التحديث → إلغاء أي استبعاد + فحص فوري بلا سقف زمني
     *  فيظهر ملخص ما الجديد دائماً بضغطة الإشعار (alpha37) */
    private fun handleUpdateIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(UpdateManager.EXTRA_OPEN_UPDATE, false) == true) {
            UpdateManager.openFromNotification()
        }
    }

    override fun onResume() {
        super.onResume()
        // بوابة الصلاحيات — يُبنى الطلب لمن يلزمه فقط (توجيه المالك alpha57):
        // الممنوح يعمل فلا يُزاح، وأولُ مرةٍ أو المفقود ممنوحٌ كان يُطلب
        if (!permissionFlowStarted) {
            val plan = shouldRunPermissionFlow()
            if (plan != null) {
                permissionFlowStarted = true
                askedKeys = plan.first
                steps.clear()
                for (key in plan.first) enqueueStep(key)
                launchNextStep()
            }
        }
        // إشعار التشغيل السريع — يظهر مع كل إقلاعٍ إن كان مفتاحه مفعّلاً
        // (بلاغ المالك 2026-11-11: «ايقونه في شريط الاشعارات لتشغيل
        // سريع للتطبيق») ويُغلق إن طفأ المفتاح في مكانٍ آخر
        lifecycleScope.launch {
            runCatching {
                val enabled = SettingsRepository(this@MainActivity)
                    .settingsOnce().quickNotification
                if (enabled) {
                    com.sawan4art.noor.util.QuickLaunchNotification.show(this@MainActivity)
                } else {
                    com.sawan4art.noor.util.QuickLaunchNotification.dismiss(this@MainActivity)
                }
            }
        }
    }
}
