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
 * databases/hadiths.db مولَّد آلياً بواسطة scripts/fix_hadiths_v6.py
 * (فوق سلسلة rebuild_hadiths_official.py وprune_hadiths_five_books.py)
 * ويحوي ٣٠٠٣٠ حديثاً من **خمسة كتب** بفهرس **FTS4** مُعبَّأ مسبقاً (contentless —
 * الفهرس فقط بلا نسخة نص إضافية). Room ينسخ الملف عند أول فتح
 * (createFromAsset) ويقرأ جدوليه books وhadiths بتحقق مخططه المعتاد،
 * بينما hadiths_fts جدول افتراضي لا يعرفه Room فلا يتحقق منه ولا يمسه —
 * والقراءة منه عبر RawQuery في HadithDao. القاعدة للقراءة فقط: لا
 * إدراج ولا تعديل من التطبيق إطلاقاً، فلا حاجة لترحيلات ولا مشغّلات مزامنة.
 *
 * **النسخة ٦ (v1.71)**: بلاغا المالك «ترتيب الاحاديث في كتاب سنن النسائى غير
 * صحيح و ايضا كتاب صحيح مسلم لا يعرض كل الاحاديث و ايضا غير مرتب» —
 * التشخيص الفعلي: النسائي ترتيبه سليمٌ لكن ٨٦ نصاً فارغاً (ومنها ٨٢
 * متتالية ٣٨٥٧–٣٩٣٨) تظهر بطاقاتٍ بيضاءٍ كأنها عطب ترتيب — فمُلئت
 * كلها من كشط sunnah.com الرسمي مع بخاري وترمذي وأبي داود (١٦٨
 * فراغاً: ١٥٥ من الكشط و٥ من صفحات sunnah.com و٨ أرقام مدمجةٍ في
 * جيرانها حُذفت صفوفها الفارغة لا اختراعَ لنصوص). ومسلم عاد كاملاً
 * من الكشط الرسمي بثلاثة إصلاحاتٍ جذريّة: النمطُ الموسِّع يقبل اللواحق
 * المركبة (1211aa–1211ah و157L) فرجع ١٣ حديثاً ساقطاً، وإزالة التكرار
 * على النص المطبَّع كاملاً بدل أول ٤٠٠ حرفٍ فرجعت ١٠ سلاسلٌ متوازية
 * متاولاها مختلف، وسلاسل الرقم الواحد تُرقَّم بترتيب الكشط نفسه (١١٢
 * سلسلةً أُعيد ترتيبها) فتصفح مسلم يطابق sunnah.com حرفاً بحرف.
 *
 * **النسخة ٥ (alpha58)**: المكتبة صارت الكتب الخمسة — بلاغ المالك:
 * «بقولك ايي يكفي فقط اهم خمس كتب من الاحاديث بدل عشره لاكن يكونوا
 * ترقيمهم صحيح و كاملين و بدون نقصان» — حُذف ابن ماجه وموطأ
 * والثلاثة الفارغة تماماً (رياض الصالحين والأربعون النووية والقدسية
 * بلا حديثٍ واحد منذ alpha56 — «اخر ٣ كتب في الاحاديث حذفت بالكامل»)
 * والترقيم المعتمد لم يُمسّ (تحقق عدّي في prune_hadiths_five_books.py).
 *
 * **النسخة ٤ (alpha57)**: كل الكتب تبدأ من ١ بترقيمها المعتمد الحرفي —
 * بلاغ المالك: «مازال هناك كتب ناقصه … البخاري كان كاملا الان يبدا ب
 * الحديث رقم ٢» — والجذر: إصلاح alpha56 كان يلحق الناقص بمعرفات بعد
 * أقصى القديم فصار الحديث ١ في ذيل الـ id والتصفح يرتب به. الجديد:
 * scripts/rebuild_hadiths_official.py أعاد البناء بترتيب (الكتاب، الرقم،
 * المعرف) ومسلم عاد بترقيم عبد الباقي المعتمد (١..٣٠٣٣ باللواحق الحرفية
 * تحت الرقم نفسه — لا تسلسل مصنوع حتى ٧٥٦٣) من نسخة sunnah.com الحاملة
 * الرقم الرسمي، والست الباقية من المصدر المعتمد المتصل ١..آخره. والحذف
 * عند النسخة الأقدم كما هو (النسخة ٣ alpha56، والنسخة ٢ alpha46): الفهرس
 * FTS4 لا FTS5 — أندرويد لا يبني SQLite بنظام FTS5 نهائياً فكان البحث
 * بالكلمة ساقطاً على كل الأجهزة. ولأن Room لا يعيد نسخ الأصول إلا على
 * قاعدة غائبة فحين تُفتح قاعدة نسخةٍ قديمة (user_version أقدم) تُحذف
 * ملفاتها الثلاثة ليعيد Room بناءها من الأصل الجديد — الحذف آمن لأن
 * القاعدة قراءةٌ فقط والمحفوظات تعيش في DataStore لا هنا.
 */
@Database(
    entities = [BookEntity::class, HadithEntity::class],
    version = 6,
    exportSchema = false,
)
abstract class HadithDatabase : RoomDatabase() {

    abstract fun hadithDao(): HadithDao

    companion object {
        const val NAME = "hadiths.db"
        const val VERSION = 6

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
