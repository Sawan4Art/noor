package com.sawan4art.noor

import com.sawan4art.noor.data.update.ReleaseSummary
import com.sawan4art.noor.data.update.UpdateChecker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * اختبارات منتقي التحديث — عقود القناة:
 * ألفا تتابع ألفا ومستقرة، والمستقرة تتجاهل prerelease،
 * والوسوم الغريبة والإصدارات بلا APK تُتخطى بأمان.
 */
class UpdatePickerTest {

    private fun release(
        tag: String,
        prerelease: Boolean = true,
        apk: Boolean = true,
        apkName: String = "noor-almuslim-$tag.apk",
        body: String = "## ما الجديد\n- تغييرات $tag",
    ) = ReleaseSummary(
        tag = tag,
        title = "نور المسلم $tag",
        body = body,
        isPrerelease = prerelease,
        releaseUrl = "https://github.com/Sawan4Art/noor/releases/tag/$tag",
        publishedAt = "2026-10-30T00:00:00Z",
        apkName = if (apk) apkName else null,
        apkUrl = if (apk) "https://github.com/…/$tag.apk" else null,
        apkSizeBytes = 7_470_019L,
    )

    @Test
    fun `ألفا حالية تلتقط أحدث ألفا أحدث منها`() {
        val releases = listOf(release("v0.5.0-alpha6"), release("v0.5.0-alpha5"))
        val info = UpdateChecker.pickUpdate(releases, "0.5.0-alpha5", allowPrerelease = true)
        assertEquals("0.5.0-alpha6", info?.versionName)
        assertEquals("noor-almuslim-v0.5.0-alpha6.apk", info?.apkName)
        assertEquals("7.1 م.ب", info?.sizeText)
    }

    @Test
    fun `القناة المستقرة تتجاهل ألفا مهما كانت أحدث`() {
        val releases = listOf(release("v0.6.0-alpha1"), release("v0.5.1", prerelease = false))
        // المستقرة الحالية 0.5.0: ألفا 0.6.0 أحدث لكنها ممنوعة — المستقرة 0.5.1 تُختار
        val info = UpdateChecker.pickUpdate(releases, "0.5.0", allowPrerelease = false)
        assertEquals("0.5.1", info?.versionName)
    }

    @Test
    fun `القناة المستقرة بلا مستقرة أحدث تعطي لا شيء`() {
        val releases = listOf(release("v0.6.0-alpha1"), release("v0.5.0", prerelease = false))
        val info = UpdateChecker.pickUpdate(releases, "0.5.0", allowPrerelease = false)
        assertNull(info)
    }

    @Test
    fun `إصدار بلا APK يُتخطى فيختار الأقدم المكتمل`() {
        val releases = listOf(
            release("v0.5.0-alpha7", apk = false),
            release("v0.5.0-alpha6"),
        )
        val info = UpdateChecker.pickUpdate(releases, "0.5.0-alpha5", allowPrerelease = true)
        assertEquals("0.5.0-alpha6", info?.versionName)
    }

    @Test
    fun `لا تحديث إذا كانت الحالية هي الأحدث`() {
        val releases = listOf(release("v0.5.0-alpha6"), release("v0.5.0-alpha5"))
        val info = UpdateChecker.pickUpdate(releases, "0.5.0-alpha6", allowPrerelease = true)
        assertNull(info)
    }

    @Test
    fun `وسم غير مفهوم يُتخطى ولا يعطّل الاختيار`() {
        val releases = listOf(
            release("nightly-20261030"),
            release("v0.5.0-alpha6"),
        )
        val info = UpdateChecker.pickUpdate(releases, "0.5.0-alpha5", allowPrerelease = true)
        assertEquals("0.5.0-alpha6", info?.versionName)
    }

    @Test
    fun `القائمة غير المرتبة تُحل بالأحدث رقماً لا بالترتيب الوارد`() {
        val releases = listOf(
            release("v0.5.0-alpha6"),
            release("v0.5.0-alpha10"),
            release("v0.5.0-alpha9"),
        )
        val info = UpdateChecker.pickUpdate(releases, "0.5.0-alpha5", allowPrerelease = true)
        assertEquals("0.5.0-alpha10", info?.versionName)
    }

    @Test
    fun `مستقرة أحدث تُفضَّل على ألفا أحدث منها عند القناة الألفا`() {
        val releases = listOf(release("v0.6.0-alpha1"), release("v0.6.0", prerelease = false))
        val info = UpdateChecker.pickUpdate(releases, "0.5.0-alpha5", allowPrerelease = true)
        assertEquals("0.6.0", info?.versionName)
    }
}
