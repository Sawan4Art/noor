package com.sawan4art.noor.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * دفتر الأعطال (alpha61) — بلاغ المالك المتكرر «زر اعدادات الخطبه يحدث
 * له جلتش ثم يغلق التطبيق» عادَ ثلاث مرات بلا أثرٍ للسبب على جهازه،
 * وكل إصلاحٍ كان تخميناً من التحليل دون أثرٍ حيّ.
 *
 * العقد هنا أبسط ما يمكن: معالج أعطالٍ يلفّ المعالج الافتراضي — أي
 * انهيارٍ يُكتَب تتبّعه الكامل في ملفٍّ داخل ملفات التطبيق
 * (`crash_log.txt`) مع آخر ٢٠ سطراً فقط كي يبقى الملف صغيراً، ثم
 * يُسلَّم العطل للمعالج الأصلي كي لا يتغير سلوك النظام إطلاقاً.
 *
 * وزر «تقرير آخر عطل» في الإعدادات (يظهر بوجود سجلٍّ فقط) يشارك
 * الملف بالبريد عبر أي تطبيق مشاركة — بلا إذنٍ وبلا خادم.
 */
object CrashLog {

    /** اسم ملف السجل داخل ملفات التطبيق الخاصة */
    const val FILE_NAME = "crash_log.txt"

    /** أقصى طولٍ للسجل بالبايت — نُبقي ذيله الأحدث فقط */
    private const val MAX_BYTES = 64 * 1024

    private var appContext: Context? = null

    /** تُنادى مرة واحدة من [com.sawan4art.noor.NoorApplication.onCreate] */
    fun install(context: Context) {
        appContext = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching { write(throwable, thread) }
            // السلوك الأصلي يبقى هو هو — النظام يقفل العملية كما يفعل دائماً
            previous?.uncaughtException(thread, throwable)
        }
    }

    /** ملف السجل — والقارئ (زر الإعدادات) يعرف غيابه بسهولة */
    fun file(context: Context): File = File(context.filesDir, FILE_NAME)

    /** هل يوجد سجل عطلٍ يُشارَك؟ */
    fun hasReport(context: Context): Boolean = file(context).length() > 0L

    /** نص السجل الحالي — فارغٌ إن لم يكن هناك عطل */
    fun read(context: Context): String =
        runCatching { file(context).readText() }.getOrDefault("")

    /** مسح السجل — بعد إرسال التقرير أو تجاهله */
    fun clear(context: Context) {
        runCatching { file(context).delete() }
    }

    /** نية المشاركة — البريد المعلن في الإعدادات مع الملف المرفق */
    fun shareIntent(context: Context): Intent {
        val text = buildString {
            append("تقرير عطل من نور المسلم")
            append('\n')
            append("النسخة: ")
            append(try {
                context.packageManager
                    .getPackageInfo(context.packageName, 0).versionName
            } catch (_: Exception) {
                "?"
            })
            append('\n')
            append(read(context).take(6000))
        }
        return Intent.createChooser(
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_EMAIL, arrayOf("sawan4art@gmail.com"))
                putExtra(Intent.EXTRA_SUBJECT, "تقرير عطل — نور المسلم")
                putExtra(Intent.EXTRA_TEXT, text)
            },
            "إرسال تقرير العطل",
        )
    }

    /** كتابة العطل في ذيل السجل — بأمانٍ كامل: فشلُ الكتابة لا يُسقط الإقلاع */
    private fun write(throwable: Throwable, thread: Thread) {
        val context = appContext ?: return
        val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        val stack = StringWriter().also { throwable.printStackTrace(PrintWriter(it)) }
            .toString()
            .lines()
            .takeLast(20)
            .joinToString("\n")
        val entry = buildString {
            append("==== $stamp — ${thread.name} ====")
            append('\n')
            append(throwable.javaClass.name)
            append(": ")
            append(throwable.message ?: "")
            append('\n')
            append(stack)
            append('\n')
        }
        val f = file(context)
        runCatching {
            // تقليمٌ بسيط: لو كبر الملف احتفظنا بالذيل الأحدث فقط
            if (f.length() > MAX_BYTES) {
                val tail = f.readText().takeLast(MAX_BYTES / 2)
                f.writeText(tail)
            }
            f.appendText(entry)
        }
    }
}
