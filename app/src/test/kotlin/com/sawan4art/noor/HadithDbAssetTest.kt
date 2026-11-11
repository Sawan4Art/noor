package com.sawan4art.noor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * عقد أصل قاعدة الأحاديث — بلاغ المالك v1.71: «ترتيب الاحاديث في كتاب
 * سنن النسائى غير صحيح و ايضا كتاب صحيح مسلم لا يعرض كل الاحاديث» —
 * الملف المرفق نفسه يُفحص: نسخته ٦ (فتحذف الأجهزة نسخها القديمة
 * وتعيد البناء) وحجمه سليمٌ وهو ملف SQLite بحقيقةٍ برأسه المعروف.
 */
class HadithDbAssetTest {

    private val dbFile: File = File(
        System.getProperty("user.dir") ?: ".",
    ).let { dir ->
        // مسارات الجدول: جذر الريبو أو مجلد app — كلٌّ يُجرب
        sequenceOf(
            File(dir, "app/src/main/assets/databases/hadiths.db"),
            File(dir, "src/main/assets/databases/hadiths.db"),
        ).firstOrNull { it.isFile }
            ?: sequenceOf(
                File(dir.parentFile, "app/src/main/assets/databases/hadiths.db"),
                File(dir.parentFile, "src/main/assets/databases/hadiths.db"),
            ).firstOrNull { it.isFile }
            ?: error("أصل قاعدة الأحاديث غير موجود")
    }

    /** user_version في رأس SQLite — إزاحة 60 بأربعة بايتاتٍ بالترتيب الكبير */
    private fun userVersion(bytes: ByteArray): Int =
        ((bytes[60].toInt() and 0xFF) shl 24) or
            ((bytes[61].toInt() and 0xFF) shl 16) or
            ((bytes[62].toInt() and 0xFF) shl 8) or
            (bytes[63].toInt() and 0xFF)

    @Test
    fun `الأصل موجودٌ وله رأس SQLite سليم`() {
        assertTrue("ملف القاعدة فارغ", dbFile.length() > 1_000_000)
        val head = dbFile.inputStream().use { it.readNBytes(100) }
        assertEquals("SQLite format 3\u0000", String(head.copyOfRange(0, 16)))
    }

    @Test
    fun `نسخة الأصل ٦ — بلاغ النسائي والمسلم`() {
        val bytes = dbFile.inputStream().use { it.readNBytes(100) }
        assertEquals(6, userVersion(bytes))
    }
}
