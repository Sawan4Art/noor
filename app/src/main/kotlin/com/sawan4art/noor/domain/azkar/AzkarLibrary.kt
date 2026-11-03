package com.sawan4art.noor.domain.azkar

/**
 * ذكرٌ أضيفه المستخدم بنفسه (alpha44 — بلاغ المالك: «اضف زر في قسم الاذكار
 * يمكن المستخدم من انشاء ذكر اضافي و او حذف اذكار موجوده بالفعل مع طبعا
 * امكانيه استرجاع الاذكار الافتراضيه»).
 *
 * [groupId] القسم الذي أُضيف إليه الذكر — يظهر في نهاية أذكاره،
 * و[item] هو الذكر نفسه بمعرّف فريد يبدأ بـ «custom_» كي لا يتصادم
 * مع معرفات المحتوى الافتراضي ولا تختلط علامات إتمامه بها.
 */
data class CustomDhikr(
    val groupId: String,
    val item: AzkarItem,
)

/**
 * مكتبة الأذكار الحيّة — الجمع النقي بين المحتوى الافتراضي وتعديلات
 * المستخدم (إضافاته وحذفه) بلا أي اعتماديات، مُختبَر وحدةً كAzkarMath.
 *
 * نموذج الحذف: الأذكار الافتراضية لا تُمحو — معرفاتها تُسجَّل في مجموعة
 * «المحذوف» ويُستثنى منها العرض، وزر الاسترجاع يفرّغ المجموعة فتعود
 * كلها (بلاغ المالك: «مع طبعا امكانيه استرجاع الاذكار الافتراضيه»).
 * أما أذكار المستخدم المضافة فحذفها نهائي لأنها ملكه لا افتراضٍ يُستعاد.
 */
object AzkarLibrary {

    /** بادئة معرفات أذكار المستخدم — بها يُفرَّق المضاف من الافتراضي */
    const val CUSTOM_PREFIX = "custom_"

    /**
     * أذكار قسمٍ بعد التعديلات: الافتراضي غير المحذوف ثم مضافات
     * المستخدم لنفس القسم بترتيب إضافتها. القيم الفاسدة تُستأمن.
     */
    fun effectiveItems(
        group: AzkarGroup,
        customs: List<CustomDhikr>,
        deleted: Set<String>,
    ): List<AzkarItem> =
        group.items.filter { it.id !in deleted } +
            customs.filter { it.groupId == group.id }.map { it.item }

    /** القسم بكامل محتواه الفعلي — للعرض ومقدّر التقدم معاً */
    fun effectiveGroup(
        group: AzkarGroup,
        customs: List<CustomDhikr>,
        deleted: Set<String>,
    ): AzkarGroup = group.copy(items = effectiveItems(group, customs, deleted))

    /** هل المعرف مضافٌ من المستخدم؟ */
    fun isCustom(itemId: String): Boolean = itemId.startsWith(CUSTOM_PREFIX)

    // ===== ترميز الحفظ — DataStore يحفظ مجموعات نصوص لا كائنات =====

    private const val SEP = "|"

    /**
     * ترميز ذكرٍ مضاف إلى مدخل نصي واحد: القسم|المعرف|العدد|المصدر|النص.
     * الفواصل العمودية والأسطر داخل النص تُهرَّب (النص آخر الحقول فلا
     * يكسره شيء بعده)، والتحليل متسامح: المدخل التالف يُهمَل لا يُكسر.
     */
    fun encodeCustom(custom: CustomDhikr): String? {
        val id = custom.item.id
        val groupId = custom.groupId
        if (id.isBlank() || groupId.isBlank() || !isCustom(id)) return null
        if (custom.item.count <= 0) return null
        if (custom.item.text.isBlank()) return null
        return listOf(
            escape(groupId),
            escape(id),
            custom.item.count.toString(),
            escape(custom.item.source),
            escape(custom.item.text),
        ).joinToString(SEP)
    }

    /** تحليل متسامح — المدخل التالف يُهمَل ولا يكسر القراءة أبداً */
    fun decodeCustom(entry: String): CustomDhikr? {
        // النص آخر الحقول ويجوز أن يحوي فواصل مهربة — نقسم بأول أربع فواصل فقط
        val parts = mutableListOf<String>()
        var rest = entry
        repeat(4) {
            val idx = findUnescaped(rest)
            if (idx < 0) return null
            parts += rest.substring(0, idx)
            rest = rest.substring(idx + 1)
        }
        parts += rest
        if (parts.size != 5) return null
        val groupId = unescape(parts[0])
        val id = unescape(parts[1])
        val count = parts[2].toIntOrNull() ?: return null
        if (count <= 0 || groupId.isBlank() || id.isBlank()) return null
        // حماية: الحفظ لأذكار المستخدم فقط — الافتراضي لا يُعدَّل بالحفظ
        if (!isCustom(id)) return null
        val source = unescape(parts[3])
        val text = unescape(parts[4])
        if (text.isBlank()) return null
        return CustomDhikr(groupId, AzkarItem(id, text, count, source))
    }

    /** أول فاصل غير مهرَّب في [s] — أو ‎-1 إن لم يوجد */
    private fun findUnescaped(s: String): Int {
        var i = 0
        while (i < s.length) {
            if (s[i] == '\\') {
                i += 2
                continue
            }
            if (s[i] == SEP[0]) return i
            i++
        }
        return -1
    }

    private fun escape(raw: String): String = raw
        .replace("\\", "\\\\")
        .replace(SEP, "\\p")
        .replace("\n", "\\n")
        .replace("\r", "")

    private fun unescape(stored: String): String = buildString {
        var i = 0
        while (i < stored.length) {
            val c = stored[i]
            if (c == '\\' && i + 1 < stored.length) {
                when (val n = stored[i + 1]) {
                    'p' -> append(SEP)
                    'n' -> append('\n')
                    else -> append(n)
                }
                i += 2
            } else {
                append(c)
                i++
            }
        }
    }
}
