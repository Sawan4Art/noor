package com.sawan4art.noor

import com.sawan4art.noor.data.mushaf.SurahZipFetch
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * ناقل ضغطات السور (alpha62 — بلاغ المالك: «مازال هناك اصوات للشيخ
 * لا يتم تحميلها … و الذي يعمل بطئ في التحميل ايضا») — العقد النقي:
 * قراءة الحجم من رأس Content-Range وحدود التنزيل المتوازي، وهما ما
 * يقومان التقطيع والاستئناف الذي ينهي «البطء والتعطل» في السور الكبيرة.
 */
class SurahZipFetchTest {

    @Test
    fun `الحجم الكلي من رأس نطاق المحتوى`() {
        assertEquals(
            12345678L,
            SurahZipFetch.contentRangeTotal("bytes 0-0/12345678"),
        )
        assertEquals(-1L, SurahZipFetch.contentRangeTotal(null))
        assertEquals(-1L, SurahZipFetch.contentRangeTotal(""))
        assertEquals(-1L, SurahZipFetch.contentRangeTotal("bytes 0-0/*"))
        assertEquals(-1L, SurahZipFetch.contentRangeTotal("bytes 0-0/"))
        assertEquals(-1L, SurahZipFetch.contentRangeTotal("بلا معنى"))
    }

    @Test
    fun `حدود التنزيل المتوازي معقولة`() {
        // أربعة اتصالاتٍ كحدٍّ أقصى والحد الأدنى للتوازي ثمانية ميغابايت
        assertEquals(4, SurahZipFetch.MAX_CONNECTIONS)
        assertEquals(8L * 1024 * 1024, SurahZipFetch.PARALLEL_MIN_BYTES)
    }
}
