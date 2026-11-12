package com.sawan4art.noor

import com.sawan4art.noor.data.update.ReleaseSummary
import com.sawan4art.noor.data.update.UpdateChecker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * اختبارات منتقي التحديث — عقد القناة العادية الواحدة (بلاغ المالك
 * v1.73: «الغب النظام ده و اعمل اصدار جديد يتثبت على كل الاصدارات
 * الموجوده»): أحدث إصدارٍ منشورٍ بملف APK وأحدث من الحالي، وإصدارات
 * المعاينة (prerelease) تُتجاهل دائماً، والوسوم الغريبة والإصدارات
 * بلا APK تُتخطى بأمان.
 */
class UpdatePickerTest {

    private fun release(
        tag: String,
        prerelease: Boolean = false,
        apk: Boolean = true,
        apkName: String = "noor-almuslim-$tag.apk",
        body: String = "## ما الجديد\n- تغييرات $tag",
    ) = ReleaseSummary(
        tag = tag,
        title = "نور المسلم $tag",
        body = body,
        isPrerelease = prerelease,
        releaseUrl = "https://github.com/Sawan4Art/noor/releases/tag/$tag",
        publishedAt = "2026-11-12T00:00:00Z",
        apkName = if (apk) apkName else null,
        apkUrl = if (apk) "https://github.com/…/$tag.apk" else null,
        apkSizeBytes = 7_470_019L,
    )

    @Test
    fun `القناة العادية تلتقط أحدث إصدار أحدث من الحالي`() {
        val releases = listOf(release("v1.73"), release("v1.72"))
        val info = UpdateChecker.pickUpdate(releases, "1.72")
        assertEquals("1.73", info?.versionName)
        assertEquals("noor-almuslim-v1.73.apk", info?.apkName)
        assertEquals("7.1 م.ب", info?.sizeText)
    }

    @Test
    fun `إصدار المعاينة يُتجاهل دائماً ولو كان الأحدث`() {
        val releases = listOf(release("v1.74-alpha1", prerelease = true), release("v1.73"))
        val info = UpdateChecker.pickUpdate(releases, "1.72")
        assertEquals("1.73", info?.versionName)
    }

    @Test
    fun `بلا مستقر أحدث تعطي لا شيء`() {
        val releases = listOf(release("v1.74-alpha1", prerelease = true), release("v1.72"))
        val info = UpdateChecker.pickUpdate(releases, "1.72")
        assertNull(info)
    }

    @Test
    fun `إصدار بلا APK يُتخطى فيختار الأقدم المكتمل`() {
        val releases = listOf(
            release("v1.74", apk = false),
            release("v1.73"),
        )
        val info = UpdateChecker.pickUpdate(releases, "1.72")
        assertEquals("1.73", info?.versionName)
    }

    @Test
    fun `لا تحديث إذا كانت الحالية هي الأحدث`() {
        val releases = listOf(release("v1.73"), release("v1.72"))
        val info = UpdateChecker.pickUpdate(releases, "1.73")
        assertNull(info)
    }

    @Test
    fun `وسم غير مفهوم يُتخطى ولا يعطّل الاختيار`() {
        val releases = listOf(
            release("nightly-20261112"),
            release("v1.73"),
        )
        val info = UpdateChecker.pickUpdate(releases, "1.72")
        assertEquals("1.73", info?.versionName)
    }

    @Test
    fun `القائمة غير المرتبة تُحل بالأحدث رقماً لا بالترتيب الوارد`() {
        val releases = listOf(
            release("v1.72"),
            release("v1.81"),
            release("v1.80"),
        )
        val info = UpdateChecker.pickUpdate(releases, "1.71")
        assertEquals("1.81", info?.versionName)
    }

    @Test
    fun `نسخة قديمة الصيغة تنال التحديث من أي مرشح مفهوم`() {
        val releases = listOf(release("v1.73"))
        val info = UpdateChecker.pickUpdate(releases, "0.5.0-alpha64")
        assertEquals("1.73", info?.versionName)
    }
}
