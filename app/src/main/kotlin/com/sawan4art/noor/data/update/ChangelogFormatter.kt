package com.sawan4art.noor.data.update

/**
 * منسق «ما الجديد» — جسم إصدار GitHub (Markdown خفيف) إلى أسطر عرض نظيفة.
 * لا مكتبة Markdown: نصوص الإصدار عندنا قصيرة وعربية، والتسطيح يكفي
 * ويحفظ التطبيق خفيفاً بلا اعتمادية عرض كاملة.
 */
object ChangelogFormatter {

    /** جسم الإصدار → أسطر جاهزة للعرض (بلا وسوم، بلا صور، بلا أسطر فارغة متتالية) */
    fun bodyToLines(body: String?): List<String> {
        if (body.isNullOrBlank()) return emptyList()
        val lines = mutableListOf<String>()
        var inHiddenSection = false
        var inHtmlComment = false

        for (rawLine in body.lines()) {
            var line = rawLine.trim()

            if (inHtmlComment) {
                if (line.contains("-->")) inHtmlComment = false
                continue
            }
            if (line.startsWith("<!--")) {
                if (!line.contains("-->")) inHtmlComment = true
                continue
            }
            // ملاحظات GitHub الآلية إن وُجدت — نعرض «ما الجديد» المكتوب فقط
            inHiddenSection = inHiddenSection ||
                line.startsWith("## What's Changed") ||
                line.startsWith("## New Contributors")
            if (inHiddenSection) continue

            if (line.isEmpty()) {
                if (lines.isNotEmpty() && lines.last() != "") lines.add("")
                continue
            }

            line = line
                .replace(Regex("!\\[[^\\]]*]\\([^)]*\\)"), "")      // صور → حذف
                .replace(Regex("\\[([^\\]]*)]\\([^)]*\\)"), "$1")   // روابط → نصها
                .replace(Regex("\\*\\*([^*]+)\\*\\*"), "$1")        // عريض → عادي
                .replace(Regex("`([^`]*)`"), "$1")                  // كود → عادي

            line = when {
                line.startsWith("#") -> line.dropWhile { it == '#' || it == ' ' }
                line.startsWith(">") -> line.dropWhile { it == '>' || it == ' ' }
                line.startsWith("- ") || line.startsWith("* ") -> "• " + line.drop(2)
                else -> line
            }.trim()

            if (line.isNotEmpty()) lines.add(line)
        }
        return lines.dropLastWhile { it.isEmpty() }
    }

    /** أول سطر معنوي — نص سطر الإشعار */
    fun firstLine(body: String?): String =
        bodyToLines(body).firstOrNull { it.isNotBlank() } ?: "نسخة جديدة متاحة"

    /** ملخص للإشعار الموسع: حتى 6 أسطر ثم سطر الإحالة */
    fun notificationSummary(body: String?, maxLines: Int = 6): String {
        val lines = bodyToLines(body)
        if (lines.size <= maxLines) return lines.joinToString("\n")
        return lines.take(maxLines).joinToString("\n") + "\n… افتح التطبيق للمزيد"
    }
}
