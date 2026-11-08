package com.sawan4art.noor

import com.sawan4art.noor.domain.azkar.AllahNames
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * عقد أسماء الله الحسنى (alpha61) — تسعةٌ وتسعون بالضبط بترتيبها
 * المعتمد، لا اسمٌ فارغاً ولا تكراراً ومعنىً موجزٍ لكل اسم.
 */
class AllahNamesTest {

    @Test
    fun `القائمة تسعة وتسعون بالضبط`() {
        assertEquals(99, AllahNames.ALL.size)
    }

    @Test
    fun `الترقيم متتابع من ١ إلى ٩٩ بلا فجوة`() {
        AllahNames.ALL.forEachIndexed { i, name ->
            assertEquals(i + 1, name.id)
        }
    }

    @Test
    fun `لا اسم فارغا ولا تكرارا وكل معنى موجز`() {
        val names = AllahNames.ALL.map { it.name }
        assertTrue(names.all { it.isNotBlank() })
        assertEquals(names.size, names.toSet().size)
        assertTrue(AllahNames.ALL.all { it.meaning.isNotBlank() })
        assertTrue(AllahNames.ALL.all { it.meaning.length <= 90 })
    }

    @Test
    fun `الافتتاحيات من الأسماء المعتمدة`() {
        assertEquals("الله", AllahNames.byId(1).name)
        assertEquals("الرحمن", AllahNames.byId(2).name)
        assertEquals("الرشيد", AllahNames.byId(99).name)
        // المعرّف الغريب يسير بأمان
        assertEquals("الله", AllahNames.byId(-3).name)
    }
}
