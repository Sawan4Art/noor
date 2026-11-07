package com.sawan4art.noor

import com.sawan4art.noor.data.mushaf.QuranAudioLibrary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * عقد إنهاء ملف التنزيل (alpha58 — بلاغ المالك: «تحميل السور لا يعمل
 * يقول جاري التحميل و الشريط لا يتقدم حتى خطوة»):
 *
 * الجذر المُصلَح: كان الحكم على النجاح بعد rename من length() للملف
 * المُنقَل نفسه — فطوله صفرٌ دائماً لأن الملف حُرِك من مكانه — كل آيةٍ
 * نزلت سليمةً حُسبت فاشلةً وأُعيد تنزيلها خمس مرات، وظلّ العدّاد
 * والشريط على صفر. العقد الآن: النجاح يُقضى من حجم المؤقت **قبل**
 * النقل، والمؤقت الفارغ يُمحى والمُحوَّل موجوداً محملاً بالبايتات.
 */
class DownloadFinalizeTest {

    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun `المؤقت المكتمل يُسمّى نهائياً ويرجع نجاحاً`() {
        val target = File(tmp.root, "001001.mp3")
        val part = File(tmp.root, "001001.mp3.part")
        part.writeBytes(ByteArray(2048) { (it % 251).toByte() })

        val ok = QuranAudioLibrary.finalizeDownloadFile(part, target)

        assertTrue(ok)
        assertTrue(target.isFile)
        assertEquals(2048L, target.length())
        // المؤقت حُرِك — لم يعد مكانه
        assertFalse(part.exists())
    }

    @Test
    fun `المؤقت الفارغ فشل ويُمحى`() {
        val target = File(tmp.root, "002001.mp3")
        val part = File(tmp.root, "002001.mp3.part")
        part.createNewFile()

        val ok = QuranAudioLibrary.finalizeDownloadFile(part, target)

        assertFalse(ok)
        assertFalse(part.exists())
        assertFalse(target.exists())
    }

    @Test
    fun `الهدف القديم يُستبدل بالمؤقت الجديد`() {
        val target = File(tmp.root, "003001.mp3")
        target.writeBytes(ByteArray(10))
        val part = File(tmp.root, "003001.mp3.part")
        part.writeBytes(ByteArray(4096))

        val ok = QuranAudioLibrary.finalizeDownloadFile(part, target)

        assertTrue(ok)
        assertEquals(4096L, target.length())
    }
}
