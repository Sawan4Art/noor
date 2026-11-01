package com.sawan4art.noor

import android.Manifest
import android.app.AlarmManager
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sawan4art.noor.data.update.UpdateManager
import com.sawan4art.noor.ui.NoorApp
import com.sawan4art.noor.ui.home.HomeViewModel

class MainActivity : ComponentActivity() {

    /**
     * الأذونات تُطلب في حوارات منفصلة بالتتابع — درس v0.4.2:
     * الطلب المجمّع يجعل بعض الأجهزة تعرض حوار الموقع فقط وتُسقط الباقي صامتاً.
     * والموقع أولاً ثم الإشعارات، والممنوح يُتخطى تلقائياً.
     *
     * السلسلة الكاملة (توجيه المالك alpha18: «في اذونات لازمه اخرى لعمل
     * التطبيق بشكل صحيح»):
     * 1) الموقع الدقيق + التقريبي — المواقيت والقبلة.
     * 2) الإشعارات (33+) — تنبيهات التحديث وما سيأتي من الأذان والأذكار.
     * 3) المنبهات الدقيقة (31-32) — مواعيد الأذان بالثانية؛ على 33+
     *    USE_EXACT_ALARM ممنوح تلقائياً فلا خطوة.
     * أما بقية الأذونات في المانيفست (الإنترنت، الاهتزاز، الاستيقاظ،
     * الإقلاع، تثبيت التحديث) فممنوحة تلقائياً بلا حوار نظام.
     */
    private val permissionQueue = ArrayDeque<List<String>>()
    private var permissionFlowStarted = false

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        // بغض النظر عن النتيجة: الحوار التالي في الطابور — والمرفوض نهائياً
        // يمرّ صامتاً فوراً فتنهي السلسلة بالحالة الفعلية.
        launchNextPermissionGroup()
    }

    /** خطوة المنبهات الدقيقة: شاشة النظام (لا حوار إذن قياسي على 31-32) */
    private val exactAlarmLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { /* إعادة الجدولة تُسأل عند الحاجة الفعلية للأذان — لا حاجة للنتيجة */ }

    private fun enqueueMissingPermissions() {
        val fine = Manifest.permission.ACCESS_FINE_LOCATION
        val coarse = Manifest.permission.ACCESS_COARSE_LOCATION
        val notifications = Manifest.permission.POST_NOTIFICATIONS

        if (ContextCompat.checkSelfPermission(this, fine) != PackageManager.PERMISSION_GRANTED) {
            permissionQueue.add(listOf(fine, coarse))
        }
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, notifications) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionQueue.add(listOf(notifications))
        }
    }

    private fun launchNextPermissionGroup() {
        permissionQueue.removeFirstOrNull()?.let { group ->
            permissionLauncher.launch(group.toTypedArray())
            return
        }
        // الطابور فاضى — الخطوة الأخيرة: المنبهات الدقيقة على 31-32 فقط
        maybeRequestExactAlarm()
    }

    private fun maybeRequestExactAlarm() {
        if (Build.VERSION.SDK_INT !in 31..32) return // 33+ ممنوح تلقائياً بـ USE_EXACT_ALARM
        val alarm = getSystemService(ALARM_SERVICE) as? AlarmManager ?: return
        val canSchedule = runCatching { alarm.canScheduleExactAlarms() }.getOrDefault(true)
        if (!canSchedule) {
            runCatching {
                exactAlarmLauncher.launch(
                    Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM),
                )
            } // جهاز لا يعرّف الشاشة — الأذان يُجدول تقريبياً ولن يكسر شيئاً
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        applyImmersiveNav()
        enqueueMissingPermissions()
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

    /** ضغط إشعار التحديث → فحص فوري بلا سقف زمني فيظهر ملخص ما الجديد */
    private fun handleUpdateIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(UpdateManager.EXTRA_OPEN_UPDATE, false) == true) {
            UpdateManager.checkNow()
        }
    }

    override fun onResume() {
        super.onResume()
        // الانطلاق في سلسلة الحوارات بعد أول ظهور فقط (يلزم حالة STARTED)
        if (!permissionFlowStarted && permissionQueue.isNotEmpty()) {
            permissionFlowStarted = true
            launchNextPermissionGroup()
        }
    }
}
