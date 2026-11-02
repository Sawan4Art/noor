package com.sawan4art.noor.data.hadith

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * قاعدة مكتبة الأحاديث — Room فوق ملف SQLite جاهز في الـ Assets:
 *
 * databases/hadiths.db مولَّد آلياً بواسطة scripts/build_hadith_db.py
 * ويحوي ٣٨٠١٦ حديثاً من عشرة كتب بفهرس FTS5 مُعبَّأ مسبقاً (contentless —
 * الفهرس فقط بلا نسخة نص إضافية). Room ينسخ الملف عند أول فتح
 * (createFromAsset) ويقرأ جدوليه books وhadiths بتحقق مخططه المعتاد،
 * بينما hadiths_fts جدول افتراضي لا يعرفه Room فلا يتحقق منه ولا يمسه —
 * والقراءة منه عبر RawQuery في HadithDao. القاعدة للقراءة فقط: لا
 * إدراج ولا تعديل من التطبيق إطلاقاً، فلا حاجة لترحيلات ولا مشغّلات مزامنة.
 */
@Database(
    entities = [BookEntity::class, HadithEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class HadithDatabase : RoomDatabase() {

    abstract fun hadithDao(): HadithDao

    companion object {
        @Volatile
        private var instance: HadithDatabase? = null

        fun get(context: Context): HadithDatabase =
            instance ?: synchronized(this) {
                instance ?: Room
                    .databaseBuilder(
                        context.applicationContext,
                        HadithDatabase::class.java,
                        "hadiths.db",
                    )
                    .createFromAsset("databases/hadiths.db")
                    .build()
                    .also { instance = it }
            }
    }
}
