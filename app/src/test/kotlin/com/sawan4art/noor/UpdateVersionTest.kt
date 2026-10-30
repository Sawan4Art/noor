package com.sawan4art.noor

import com.sawan4art.noor.data.update.NoorVersion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * اختبارات مقياس النسخ — عقود المقارنة التي يعتمد عليها بنر التحديث:
 * الأرقام رقماً لا حرفاً، والمستقرة أعلى من ألفا، والأساس الأعلى يسبق كل شيء.
 */
class UpdateVersionTest {

    @Test
    fun `الوسم بحرف v يُحلل ويعرض بلا حرف الوسم`() {
        val version = NoorVersion.parse("v0.5.0-alpha6")
        assertEquals("0.5.0-alpha6", version?.display)
    }

    @Test
    fun `ألفا أحدث تسبق أقدمها`() {
        assertTrue(
            NoorVersion.parse("0.5.0-alpha6")!! > NoorVersion.parse("0.5.0-alpha5")!!,
        )
    }

    @Test
    fun `الأرقام تُقارن رقماً لا حرفاً — alpha10 أكبر من alpha9`() {
        assertTrue(
            NoorVersion.parse("0.5.0-alpha10")!! > NoorVersion.parse("0.5.0-alpha9")!!,
        )
    }

    @Test
    fun `ترتيب المراحل alpha ثم beta ثم rc ثم المستقرة`() {
        val alpha = NoorVersion.parse("0.5.0-alpha2")!!
        val beta = NoorVersion.parse("0.5.0-beta1")!!
        val rc = NoorVersion.parse("0.5.0-rc1")!!
        val stable = NoorVersion.parse("0.5.0")!!
        assertTrue(alpha < beta)
        assertTrue(beta < rc)
        assertTrue(rc < stable)
    }

    @Test
    fun `الأساس الأعلى يسبق أي مرحلة بنفس عدد الأرقام`() {
        assertTrue(
            NoorVersion.parse("0.6.0-alpha1")!! > NoorVersion.parse("0.5.0-alpha99")!!,
        )
    }

    @Test
    fun `المستقرة أعلى من ألفا عند تساوي الأساس`() {
        assertTrue(
            NoorVersion.parse("0.5.0")!! > NoorVersion.parse("0.5.0-alpha99")!!,
        )
    }

    @Test
    fun `الصيغ غير المفهومة ترجع null`() {
        assertNull(NoorVersion.parse(null))
        assertNull(NoorVersion.parse(""))
        assertNull(NoorVersion.parse("nightly-2026"))
        assertNull(NoorVersion.parse("0.5.x"))
    }

    @Test
    fun `المرشح الأحدث يكشف isNewer والمتساوي لا`() {
        assertTrue(NoorVersion.isNewer("v0.5.0-alpha6", "0.5.0-alpha5"))
        assertTrue(NoorVersion.isNewer("0.5.0-alpha6", "v0.5.0-alpha6") == false)
        assertTrue(NoorVersion.isNewer("v0.5.0-alpha6", "0.5.0-alpha6") == false)
        // مرشح غير مفهوم لا يُعد تحديثاً أبداً
        assertTrue(NoorVersion.isNewer("garbage", "0.5.0-alpha5") == false)
        // مرشح مفهوم مع نسخة حالية غير مفهومة يُعد أحدث
        assertTrue(NoorVersion.isNewer("0.5.0-alpha6", "legacy-build"))
    }

    @Test
    fun `سلسلة أرقام الألفا رتيبة الصعود عبر حدود العشرات`() {
        var previous = NoorVersion.parse("0.5.0-alpha1")!!
        for (n in 2..12) {
            val current = NoorVersion.parse("0.5.0-alpha$n")!!
            assertTrue("alpha$n يجب أن تتخطى ما قبلها", current > previous)
            previous = current
        }
    }
}
