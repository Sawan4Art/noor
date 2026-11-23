package com.sawan4art.noor

import com.sawan4art.noor.domain.tafsir.TafsirEditions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * عقود كتالوج التفاسير — v1.81 (بلاغ المالك: «اضف التفاسير التاليه
 * الجلالين و القرطبي و ابن كثير الطبري السعدي و طبعا مع ابقاء التفسير
 * الميسر … و حطلها قسم جديد باسم التفاسير»):
 *
 * ١. الطبعات الست كاملة بلا غيابٍ ولا تكرارٍ في المعرّفات.
 * ٢. الميسر مضمّنٌ (بلا تنزيل) والخمس الأخرى بتغطية بايتات وبصمة sha256.
 * ٣. الطبعة المحفوظة غير المعروفة تعود للميسر (الافتراضي المضمّن).
 * ٤. أسماء العرض عربيةٌ بلا غياب وحجم الحزمة نصٌّ بشريٌّ مفهوم.
 */
class TafsirEditionsTest {

    @Test
    fun `الطبعات الست كاملة والمعرفات فريدة`() {
        val ids = TafsirEditions.ALL.map { it.id }
        assertEquals(6, ids.size)
        assertEquals(
            setOf(
                "ar.muyassar", "ar.jalalayn", "ar.katheer",
                "ar.qurtubi", "ar.tabari", "ar.saadi",
            ),
            ids.toSet(),
        )
        assertEquals("المعرّفات تكرارية!", ids.size, ids.toSet().size)
    }

    @Test
    fun `الميسر مضمّن والخمس الأخرى بحزمة وبصمة سليمة`() {
        val muyassar = TafsirEditions.byId("ar.muyassar")!!
        assertTrue("الميسر يجب أن يكون مضمّناً", muyassar.bundled)
        assertEquals(0L, muyassar.sizeBytes)
        assertTrue("المضمّنة لا رابطَ تنزيل لها", TafsirEditions.downloadUrl("ar.muyassar") == null)
        for (id in listOf("ar.jalalayn", "ar.katheer", "ar.qurtubi", "ar.tabari", "ar.saadi")) {
            val ed = TafsirEditions.byId(id)!!
            assertFalse("$id: يجب ألا تكون مضمّنة", ed.bundled)
            assertTrue("$id: حجم غير منطقي", ed.sizeBytes in 1_000..50_000_000L)
            assertEquals("$id: بصمة خاطئة", 64, ed.sha256.length)
            assertTrue("$id: رابط فرع الأصول", TafsirEditions.downloadUrl(id)!!.endsWith("$id.zip"))
            assertTrue(
                "$id: الرابط من فرع tafsir-assets",
                TafsirEditions.downloadUrl(id)!!.contains("tafsir-assets"),
            )
        }
    }

    @Test
    fun `الطبعة المحفوظة غير المعروفة تعود للميسر`() {
        assertEquals("ar.muyassar", TafsirEditions.resolveOrDefault(null))
        assertEquals("ar.muyassar", TafsirEditions.resolveOrDefault(""))
        assertEquals("ar.muyassar", TafsirEditions.resolveOrDefault("طبعة قديمة مزورة"))
        assertEquals("ar.jalalayn", TafsirEditions.resolveOrDefault("ar.jalalayn"))
        assertNull(TafsirEditions.byId("غير موجود"))
        assertNotNull(TafsirEditions.byId("ar.qurtubi"))
    }

    @Test
    fun `أسماء العرض عربية وحجم الحزمة نص مفهوم`() {
        for (ed in TafsirEditions.ALL) {
            assertTrue("اسم ${ed.id} غير عربي", ed.arabicName.isNotBlank())
        }
        assertEquals("التفسير الميسر", TafsirEditions.displayName("ar.muyassar"))
        assertEquals("تفسير الجلالين", TafsirEditions.displayName("ar.jalalayn"))
        // معرف مجهول يعود خاماً لا يخيب
        assertEquals("ed.gharib", TafsirEditions.displayName("ed.gharib"))
        // أرقام عربية بالحجم: «١٫٠ م.ب» أو «٦٦٦ ك.ب»
        val big = TafsirEditions.sizeLabel(1_048_576L)
        assertTrue("الحجم الكبير: $big", big.endsWith("م.ب") && big.contains("١"))
        val small = TafsirEditions.sizeLabel(681_956L)
        assertTrue("الحجم الصغير: $small", small.endsWith("ك.ب"))
    }
}
