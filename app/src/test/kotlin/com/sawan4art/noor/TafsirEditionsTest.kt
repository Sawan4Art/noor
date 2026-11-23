package com.sawan4art.noor

import com.sawan4art.noor.domain.tafsir.TafsirEditions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

/**
 * عقود كتالوج التفاسير — v1.82 (بلاغ المالك: «انا عايز التفاسير تكون
 * مدمجه داخل التطبيق نفسه مش قابله للتنزيل لا تكون موجوده بشكل
 * افتراضي في التطبيق و المستخدم يختار بينهم»):
 *
 * ١. الطبعات الست كاملة بلا غيابٍ ولا تكرارٍ في المعرّفات.
 * ٢. مسار الأصول لكل سورة لاتينيٌّ ثابتٌ لا يعتمد لغة الجهاز.
 * ٣. الطبعة المحفوظة غير المعروفة تعود للميسر (الافتراضي).
 * ٤. **انحدار انهيار v1.81** — النصوص العربية للعرض لا تمر على أي
 *    تنسيق أرقامٍ يعتمد لغة الجهاز (حذف sizeLabel نفسه؛ والاختبار
 *    يضمن أن أسماء العرض والمسارات تبقى سليمةً تحت لغةٍ عربيةٍ
 *    افتراضية كما في هاتف المالك).
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
    fun `مسار الأصول لاتيني ثابت بثلاث خانات`() {
        assertEquals("tafsir/ar.muyassar/001.txt", TafsirEditions.assetPath("ar.muyassar", 1))
        assertEquals("tafsir/ar.jalalayn/007.txt", TafsirEditions.assetPath("ar.jalalayn", 7))
        assertEquals("tafsir/ar.tabari/114.txt", TafsirEditions.assetPath("ar.tabari", 114))
        // خارج المدى يُحاصر بلا انهيار — مسارٌ دائماً صالحٌ للبنية
        assertEquals("tafsir/ar.katheer/001.txt", TafsirEditions.assetPath("ar.katheer", -3))
        assertEquals("tafsir/ar.saadi/114.txt", TafsirEditions.assetPath("ar.saadi", 500))
    }

    @Test
    fun `المسار والأسماء سليمة تحت لغة الجهاز العربية — درس الانهيار`() {
        val oldLocale = Locale.getDefault()
        try {
            Locale.setDefault(Locale("ar", "EG"))
            // المسار لاتينيٌّ ثابتٌ مهما كانت لغة الجهاز
            val p = TafsirEditions.assetPath("ar.qurtubi", 12)
            assertTrue(
                "المسار يجب أن يبقى لاتينياً: $p",
                p.none { it in '٠'..'٩' || (it.code > 127 && Character.isDigit(it.code)) },
            )
            // وأسماء العرض نصوصٌ سليمةٌ بلا تنسيق أرقامٍ
            for (ed in TafsirEditions.ALL) {
                assertTrue("اسم ${ed.id} غير عربي", ed.arabicName.isNotBlank())
            }
            assertEquals("التفسير الميسر", TafsirEditions.displayName("ar.muyassar"))
            assertEquals("تفسير الجلالين", TafsirEditions.displayName("ar.jalalayn"))
        } finally {
            Locale.setDefault(oldLocale)
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
    fun `معرف مجهول يعرض خاما لا يخيب`() {
        assertEquals("ed.gharib", TafsirEditions.displayName("ed.gharib"))
    }
}
