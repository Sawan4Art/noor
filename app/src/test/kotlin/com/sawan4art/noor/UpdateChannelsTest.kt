package com.sawan4art.noor

import com.sawan4art.noor.data.update.ReleaseSummary
import com.sawan4art.noor.data.update.UpdateChannel
import com.sawan4art.noor.data.update.UpdateChecker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * اختبارات قنوات التحديث الثلاث — بلاغ المالك v1.71: «في زر التحديثات
 * عايز اضيف كذا اصدار قابلين للتحميل من قبل المستخدم النسخه المستقره
 * و البيتا و الاصدار التجريبي… و يمكن للمستخدم تفعيل اشعارات النسخ
 * المستقره فقط او اي نسخ اخرى».
 *
 * عقد القناة: تُقرأ من الوسم نفسه — بلا مرحلةٍ مستقرة، بـ beta بيتا،
 * بـ alpha تجريبية، والوسم الغريب مستقرة بأمان.
 */
class UpdateChannelsTest {

    private fun release(tag: String, apk: Boolean = true) = ReleaseSummary(
        tag = tag,
        title = "نور المسلم $tag",
        body = "ما الجديد في $tag",
        isPrerelease = tag.contains("alpha") || tag.contains("beta"),
        releaseUrl = "https://github.com/Sawan4Art/noor/releases/tag/$tag",
        publishedAt = "2026-11-11T00:00:00Z",
        apkName = if (apk) "noor-almuslim-$tag.apk" else null,
        apkUrl = if (apk) "https://github.com/…/$tag.apk" else null,
        apkSizeBytes = 10_000_000L,
        channel = UpdateChannel.ofTag(tag),
    )

    // ===== قناة الوسم =====

    @Test
    fun `الوسم بلا مرحلة مستقرة — v1_71`() {
        assertEquals(UpdateChannel.STABLE, UpdateChannel.ofTag("v1.70"))
        assertEquals(UpdateChannel.STABLE, UpdateChannel.ofTag("v2.0.0"))
    }

    @Test
    fun `الوسم بألفا تجريبية وببيتا بيتا`() {
        assertEquals(UpdateChannel.ALPHA, UpdateChannel.ofTag("v1.71-alpha1"))
        assertEquals(UpdateChannel.BETA, UpdateChannel.ofTag("v1.71-beta2"))
        assertEquals(UpdateChannel.ALPHA, UpdateChannel.ofTag("1.72-alpha12"))
    }

    @Test
    fun `الوسم الغريب مستقرة بأمان`() {
        assertEquals(UpdateChannel.STABLE, UpdateChannel.ofTag("weird-tag"))
        assertEquals(UpdateChannel.STABLE, UpdateChannel.ofTag(null))
    }

    @Test
    fun `rc ومافوق مستقرة — أقرب للرسمية`() {
        assertEquals(UpdateChannel.STABLE, UpdateChannel.ofTag("v1.71-rc1"))
    }

    // ===== أحدث نسخة لكل قناة =====

    @Test
    fun `كل قناة تعيد أحدث نسخةٍ لها`() {
        val releases = listOf(
            release("v1.69"),
            release("v1.70"),
            release("v1.71-alpha1"),
            release("v1.71-alpha2"),
            release("v1.71-beta1"),
        )
        val per = UpdateChecker.latestPerChannel(releases)
        assertEquals("v1.70", per[UpdateChannel.STABLE]?.tag)
        assertEquals("v1.71-beta1", per[UpdateChannel.BETA]?.tag)
        assertEquals("v1.71-alpha2", per[UpdateChannel.ALPHA]?.tag)
    }

    @Test
    fun `القناة الفارغة تعود null والإصدار بلا APK يتخطى`() {
        val releases = listOf(release("v1.70"), release("v1.71-alpha1", apk = false))
        val per = UpdateChecker.latestPerChannel(releases)
        assertEquals("v1.70", per[UpdateChannel.STABLE]?.tag)
        assertNull(per[UpdateChannel.ALPHA])
        assertNull(per[UpdateChannel.BETA])
    }

    // ===== مرشّح الإشعار بقنواته =====

    @Test
    fun `إشعارات المستقرة فقط تتجاهل البيتا والألفا`() {
        val releases = listOf(
            release("v1.71-beta1"),
            release("v1.71-alpha3"),
            release("v1.70"),
        )
        val info = UpdateChecker.pickCandidateForNotify(releases, "1.70", notifyAllChannels = false)
        assertNull(info) // المستقرة 1.70 هي نفسها الحالية — والبيتا والألفا ممنوعتان فلا إشعار
    }

    @Test
    fun `إشعارات المستقرة فقط تلتقط المستقرة الأحدث`() {
        val releases = listOf(release("v1.71-beta1"), release("v1.71"))
        val info = UpdateChecker.pickCandidateForNotify(releases, "1.70", notifyAllChannels = false)
        assertEquals("v1.71", info?.tag)
        assertEquals(UpdateChannel.STABLE, info?.channel)
    }

    @Test
    fun `إشعارات كل النسخ تلتقط الأحدث مهما كانت قناتها`() {
        val releases = listOf(release("v1.71"), release("v1.71-beta1"), release("v1.72-alpha1"))
        val info = UpdateChecker.pickCandidateForNotify(releases, "1.70", notifyAllChannels = true)
        assertEquals("v1.72-alpha1", info?.tag)
        assertEquals(UpdateChannel.ALPHA, info?.channel)
    }

    @Test
    fun `مرشّح الإشعار لا يعرض قناةً أقدم من الحالية`() {
        val releases = listOf(release("v1.70"), release("v1.71-alpha1"))
        val info = UpdateChecker.pickCandidateForNotify(releases, "1.71", notifyAllChannels = true)
        assertNull(info)
    }

    // ===== قيمة إعداد الإشعار =====

    @Test
    fun `قيمة الإعداد all فقط تفتح كل القنوات`() {
        assertEquals(false, UpdateChannel.fromNotifyPref("stable"))
        assertEquals(false, UpdateChannel.fromNotifyPref(null))
        assertEquals(false, UpdateChannel.fromNotifyPref("anything"))
        assertEquals(true, UpdateChannel.fromNotifyPref("all"))
    }
}
