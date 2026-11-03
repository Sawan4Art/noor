package com.sawan4art.noor.data.hadith

import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteDatabase.OPEN_READONLY
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import java.io.File

/**
 * قاعدة مكتبة الأحاديث — Room فوق ملف SQLite جاهز في الـ Assets:
 *
 * databases/hadiths.db مولَّد آلياً بواسطة scripts/build_hadith_db.py
 * ثم scripts/renumber_hadiths.py (الترقيم المعتمد + الفهرس) ويحوي
 * ٣٨٠١٦ حديثاً من عشرة كتب بفهرس **FTS4** مُعبَّأ مسبقاً (contentless —
 * الفهرس فقط بلا نسخة نص إضافية). Room ينسخ الملف عند أول فتح
 * (createFromAsset) ويقرأ جدوليه books وhadiths بتحقق مخططه المعتاد،
 * بينما hadiths_fts جدول افتراضي لا يعرفه Room فلا يتحقق منه ولا يمسه —
 * والقراءة منه عبر RawQuery في HadithDao. القاعدة للقراءة فقط: لا
 * إدراج ولا تعديل من التطبيق إطلاقاً، فلا حاجة لترحيلات ولا مشغّلات مزامنة.
 *
 * **النسخة ٢ (alpha46)**: الفهرس FTS4 لا FTS5 — أندرويد لا يبني SQLite
 * بنظام FTS5 نهائياً فكان البحث بالكلمة ساقطاً على كل الأجهزة، والترقيم
 * صار الترقيم المعتمد. ولأن Room لا يعيد نسخ الأصول إلا على قاعدة غائبة
 * فحين تُفتح قاعدة نسخةٍ قديمة (user_version أقدم) تُحذف ملفاتها الثلاثة
 * ليعيد Room بناءها من الأصل الجديد — الحذف آمن لأن القاعدة قراءةٌ فقط
 * والمحفوظات تعيش في DataStore لا هنا.
 */
@Database(
    entities = [BookEntity::class, HadithEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class HadithDatabase : RoomDatabase() {

    abstract fun hadithDao(): HadithDao

    companion object {
        const val NAME = "hadiths.db"
        const val VERSION = 2

        @Volatile
        private var instance: HadithDatabase? = null

        fun get(context: Context): HadithDatabase =
            instance ?: synchronized(this) {
                instance ?: run {
                    replaceStaleCopy(context.applicationContext)
                    Room
                        .databaseBuilder(
                            context.applicationContext,
                            HadithDatabase::class.java,
                            NAME,
                        )
                        .createFromAsset("databases/$NAME")
                        .build()
                        .also { instance = it }
                }
            }

        /** نسخة المستخدم أقدم من المرفق؟ احذفها ليُعاد بناؤها من الأصل الجديد */
        private fun replaceStaleCopy(context: Context) {
            val file = context.getDatabasePath(NAME)
            if (!file.isFile) return
            runCatching {
                val stored = SQLiteDatabase.openDatabase(file.path, null, OPEN_READONLY)
                    .use { it.version }
                if (stored < VERSION) {
                    file.delete()
                    File(file.path + "-wal").delete()
                    File(file.path + "-shm").delete()
                }
            }
        }
    }
}
