package com.sawan4art.noor

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

/**
 * عقد المانيفست (توجيه المالك 2026-11-03):
 * «ضيف الاذونات دي في ملف AndroidManifest.xml علشان التطبيق يشتغل صح
 * معانا في المستقبل ويقوم بالوظائف كاملة بدون مشاكل من أندرويد 7 لحد
 * أندرويد 15 ... وكمان سجل المكونات دي (AlarmActivity, BootReceiver,
 * AlarmService) بالخصائص المطلوبة لتشغيل الشاشة فوق القفل والصوت في
 * الخلفية» — عقد يمنع ضياع أي إذنٍ من حزمة الجاهزية أو أي تسجيل مكوّن
 * (حذفُها أثناء تنظيفٍ قادم يفشل البناء فوراً بدل أن يكتشف في الميدان).
 *
 * الملف يُقرأ من شجرة المصدر مباشرة (اختبارات الجهاز الافتراضي تعمل
 * بمجلد الوحدة مجلداً عاملًا).
 */
class ManifestContractTest {

    private val manifest: Element by lazy {
        val candidates = listOf(
            File("src/main/AndroidManifest.xml"),
            File("app/src/main/AndroidManifest.xml"),
        )
        val file = candidates.firstOrNull { it.isFile }
            ?: error("المانيفست غير موجود عند: $candidates — المجلد العامل: ${File(".").absolutePath}")
        DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file).documentElement
    }

    private fun permissions(): Set<String> =
        manifest.getElementsByTagName("uses-permission").let { nodes ->
            (0 until nodes.length).map { nodes.item(it) as Element }
                .map { it.getAttribute("android:name") }
                .toSet()
        }

    private fun component(tag: String, name: String): Element =
        manifest.getElementsByTagName(tag).let { nodes ->
            (0 until nodes.length).map { nodes.item(it) as Element }
                .firstOrNull { it.getAttribute("android:name") == name }
                ?: error("المكوّن $name غير مسجّل في المانيفست")
        }

    // ===== حزمة أذونات الجاهزية (طلب المالك الحرفي + المكمّلة) =====

    @Test
    fun `أذونات المالك التسع كلها معلنة حرفياً`() {
        val required = setOf(
            "android.permission.WAKE_LOCK",
            "android.permission.RECEIVE_BOOT_COMPLETED",
            "android.permission.VIBRATE",
            "android.permission.USE_FULL_SCREEN_INTENT",
            "android.permission.SCHEDULE_EXACT_ALARM",
            "android.permission.USE_EXACT_ALARM",
            "android.permission.POST_NOTIFICATIONS",
            "android.permission.FOREGROUND_SERVICE",
            "android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK",
        )
        val present = permissions()
        val missing = required - present
        assertTrue("أذونات ناقصة عن طلب المالك: $missing", missing.isEmpty())
    }

    /**
     * عقد alpha49 (توجيه المالك: «امنح التطبيق اذونات عدم الازعاج»):
     * كتم أصوات الهاتف كاملاً بعد الإقامة يحتاج إذن عدم الإزعاج معلناً
     * — يُمنح من صفحة النظام ضمن سلسلة الصلاحيات.
     */
    @Test
    fun `إذن عدم الإزعاج معلن لكتم ما بعد الإقامة`() {
        assertTrue(
            "ACCESS_NOTIFICATION_POLICY مفقودة — كتم ما بعد الإقامة الكامل معطّل",
            "android.permission.ACCESS_NOTIFICATION_POLICY" in permissions(),
        )
    }

    @Test
    fun `أذونات الأساس الوظيفي باقية — الموقع والشبكة والوسائط`() {
        val required = setOf(
            "android.permission.INTERNET",
            "android.permission.ACCESS_NETWORK_STATE",
            "android.permission.ACCESS_COARSE_LOCATION",
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.READ_MEDIA_AUDIO",
        )
        val present = permissions()
        val missing = required - present
        assertTrue("أذونات أساسية ناقصة: $missing", missing.isEmpty())
    }

    /**
     * عقد alpha48 (توجيه المالك: «التحديث بقا يفتح المتصفح انا مش
     * عايز كده»): التحديث الداخلي عاد بالطريق القياسي الآمن — إذن
     * التثبيت إلزامي وموفّر الملفات إلزامي على ملف النسخة داخل ملفات
     * التطبيق فقط (التنزيل بمدير النظام والفتح بمثبّت أندرويد الرسمي).
     * حذف أيٍّ منهما يفشل البناء فوراً.
     */
    @Test
    fun `إذن التثبيت وموفر الملفات عادا للتحديث الداخلي`() {
        val present = permissions()
        assertTrue(
            "REQUEST_INSTALL_PACKAGES إلزامية منذ alpha48 — التحديث من داخل التطبيق",
            "android.permission.REQUEST_INSTALL_PACKAGES" in present,
        )
        val providers = manifest.getElementsByTagName("provider").let { nodes ->
            (0 until nodes.length).map { nodes.item(it) as Element }
        }
        val fileProvider = providers.firstOrNull {
            it.getAttribute("android:name") == "androidx.core.content.FileProvider"
        } ?: error("موفّر الملفات مفقود — فتح مثبّت النظام للتحديث الداخلي معطّل")
        assertEquals(
            "موفّر الملفات بلا تصدير خارجي",
            "false",
            fileProvider.getAttribute("android:exported"),
        )
        assertEquals(
            "موفّر الملفات بلا منح أذونات قراءة",
            "true",
            fileProvider.getAttribute("android:grantUriPermissions"),
        )
        assertTrue(
            "سلطة الموفّر يجب أن تنتهي بـ.fileprovider",
            fileProvider.getAttribute("android:authorities").endsWith(".fileprovider"),
        )
    }

    @Test
    fun `مكمّلات الكفاءة معلنة — إشعال الشاشة واستثناء توفير البطارية`() {
        val present = permissions()
        // أندرويد ١٤+: إشعال الشاشة من شاشة المنبه إذن معلن
        assertTrue("TURN_SCREEN_ON مفقودة", "android.permission.TURN_SCREEN_ON" in present)
        // كفاءة الأذان: هواتف الشركات تقتل المنبهات في التوفير الشديد
        assertTrue(
            "REQUEST_IGNORE_BATTERY_OPTIMIZATIONS مفقودة",
            "android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" in present,
        )
    }

    @Test
    fun `المنبه الدقيق بنمطه الصحيح — SCHEDULE للقديم وUSE للجديد`() {
        val present = permissions()
        assertTrue("SCHEDULE_EXACT_ALARM مفقودة", "android.permission.SCHEDULE_EXACT_ALARM" in present)
        assertTrue("USE_EXACT_ALARM مفقودة", "android.permission.USE_EXACT_ALARM" in present)
    }

    // ===== تسجيل مكونات المنبه الثلاثة بخصائصها =====

    @Test
    fun `شاشة المنبه مسجلة فوق القفل وبلا تصدير خارجي`() {
        val activity = component("activity", ".alarm.AlarmActivity")
        assertEquals("true", activity.getAttribute("android:showWhenLocked"))
        assertEquals("true", activity.getAttribute("android:turnScreenOn"))
        assertEquals("false", activity.getAttribute("android:exported"))
        assertEquals("true", activity.getAttribute("android:excludeFromRecents"))
    }

    @Test
    fun `مستقبل الإقلاع مسجل صريح التصدير مع فلتر BOOT_COMPLETED`() {
        val receiver = component("receiver", ".alarm.BootReceiver")
        assertEquals("true", receiver.getAttribute("android:exported"))
        val actions = receiver.getElementsByTagName("action").let { nodes ->
            (0 until nodes.length).map { (nodes.item(it) as Element).getAttribute("android:name") }
        }
        assertTrue(
            "فلتر الإقلاع ناقص — وجدنا: $actions",
            "android.intent.action.BOOT_COMPLETED" in actions,
        )
    }

    @Test
    fun `خدمة المنبه خدمة أمامية بنوع تشغيل الوسائط وبلا تصدير`() {
        val service = component("service", ".alarm.AlarmService")
        assertEquals("false", service.getAttribute("android:exported"))
        assertEquals(
            "النوع الأمامي خطأ — صوت الأذان يحتاج تشغيل وسائط",
            "mediaPlayback",
            service.getAttribute("android:foregroundServiceType"),
        )
    }

    // ===== أصناف المكونات موجودة فعلاً في الشجرة (لا مراجع معلقة) =====

    @Test
    fun `أصناف المكونات الثلاثة ملفات موجودة في الحزمة`() {
        val base = "src/main/kotlin/com/sawan4art/noor/alarm"
        val alt = "app/src/main/kotlin/com/sawan4art/noor/alarm"
        val root = listOf(base, alt).firstOrNull { File(it).isDirectory }
            ?: error("حزمة alarm غير موجودة عند: $base أو $alt")
        listOf("AlarmActivity.kt", "BootReceiver.kt", "AlarmService.kt").forEach { name ->
            assertNotNull("ملف $name مفقود", File(root, name).takeIf { it.isFile })
        }
    }
}
