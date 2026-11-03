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
 * قسم أذكار كامل أضافه المستخدم (alpha45 — بلاغ المالك: «زر زائد يجب ان
 * يكون ايضا في داخل صفحه الاذكار نفسها لاضافه كارت اذكار جديد و كذالك
 * زر مسح يكون على كل كرت من الاذكار»): بطاقة جديدة في شبكة صفحة
 * الأذكار تفتح صفحتها المستقلة كأي قسم، وأذكارها هي مضافات المستخدم
 * ذات المعرّف نفسه.
 *
 * [id] يبدأ بـ «cg_» كي يُفرَّق عن معرفات الأقسام الافتراضية ولا
 * يتصادم معها، و[subtitle] وصف ثابت يوضح أصل القسم.
 */
data class CustomGroup(
    val id: String,
    val title: String,
    val subtitle: String,
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

    /** بادئة معرفات أقسام المستخدم المضافة (alpha45) */
    const val CUSTOM_GROUP_PREFIX = "cg_"

    /**
     * شبكة صفحة الأذكار بعد التعديلات: الأقسام الافتراضية غير المحذوفة
     * ثم أقسام المستخدم المضافة بترتيب إضافتها (alpha45).
     */
    fun effectiveGroups(
        defaults: List<AzkarGroup>,
        customGroups: List<CustomGroup>,
        deletedGroups: Set<String>,
    ): List<AzkarGroup> =
        defaults.filter { it.id !in deletedGroups } +
            customGroups.map { AzkarGroup(it.id, it.title, it.subtitle, emptyList()) }

    /** هل معرّف القسم مضافٌ من المستخدم؟ */
    fun isCustomGroup(groupId: String): Boolean = groupId.startsWith(CUSTOM_GROUP_PREFIX)

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

    /**
     * ترميز قسمٍ مضاف إلى مدخل نصي واحد: المعرف|الوصف|العنوان (alpha45).
     * العنوان آخر الحقول فلا يكسره شيء بعده، والتحليل متسامح:
     * المدخل التالف يُهمَل لا يُكسر.
     */
    fun encodeCustomGroup(group: CustomGroup): String? {
        if (!isCustomGroup(group.id)) return null
        val title = group.title.trim()
        if (title.isEmpty()) return null
        return listOf(
            escape(group.id),
            escape(group.subtitle),
            escape(title),
        ).joinToString(SEP)
    }

    /** تحليل متسامح لمدخل قسمٍ مضاف — التالف يُهمَل */
    fun decodeCustomGroup(entry: String): CustomGroup? {
        val first = findUnescaped(entry)
        if (first < 0) return null
        val id = unescape(entry.substring(0, first))
        val rest = entry.substring(first + 1)
        val second = findUnescaped(rest)
        val subtitle: String
        val title: String
        if (second < 0) {
            // مدخل قديم بلا وصف — كله عنوان
            subtitle = ""
            title = unescape(rest)
        } else {
            subtitle = unescape(rest.substring(0, second))
            title = unescape(rest.substring(second + 1))
        }
        if (!isCustomGroup(id) || title.isBlank()) return null
        return CustomGroup(id, title, subtitle)
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
