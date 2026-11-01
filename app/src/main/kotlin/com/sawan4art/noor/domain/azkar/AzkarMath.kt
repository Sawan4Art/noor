package com.sawan4art.noor.domain.azkar

import java.time.Instant
import java.time.ZoneId

/**
 * محرك الأذكار — نقيّ بلا أندرويد كي يُختبر آلياً بعقود صارمة، على
 * نمط MisbahaMath نفسه:
 *
 * - **تقدّم المجموعة يوميّ**: علامات الإتمام تنتمي ليومٍ واحد
 *   (مفتاح اليوم = رقم اليوم epochDay بالمنطقة المحلية) ويتجدد
 *   الصفر مع كل يوم جديد تلقائياً — لا إجماليات تراكمية (درس
 *   المالك: «تشيل عداد الاجمالي اللي فوق خالص»).
 * - **الصلاة على النبي عدّاد حرّ**: يتصاعد ولا سقف له ولا هدف،
 *   ويُحتفى بكل مائةٍ (المائة = مائة صلاة كاملة تُكتب لصاحبها).
 */
object AzkarMath {

    /**
     * مفتاح اليوم — رقم اليوم (epochDay) للتوقيت المعطى بالمنطقة
     * المحلية. المنطقة تُمرَّر من المستدعي كي يبقى الحساب تاماً
     * قابلاً للاختبار على أي منطقة وتاريخ.
     */
    fun epochDayOf(millis: Long, zone: ZoneId): Long =
        Instant.ofEpochMilli(millis).atZone(zone).toLocalDate().toEpochDay()

    /**
     * العلامات الفعلّية اليوم: لو مخزَّن اليوم هو نفسه يوم المجموعة
     * تُعاد العلامات كما هي، وإلا فصفرٌ نظيف — بلا أي بقايا أمس.
     */
    fun effectiveDone(today: Long, storedDay: Long, storedDone: Set<String>): Set<String> =
        if (today == storedDay) storedDone else emptySet()

    /**
     * تعليم/إلغاء تعليم ذكر — تبديل صريح: موجودٌ يُنزَل، وغائبٌ
     * يُنزَل إليه. لا حالة وسيطة ولا تفريغ كامل.
     */
    fun toggle(done: Set<String>, id: String): Set<String> =
        if (id in done) done - id else done + id

    /** تقدّم مجموعةٍ في يومها — المعدود [doneCount] لا يتجاوز الإجمالي أبداً */
    data class GroupProgress(val doneCount: Int, val total: Int) {
        val allDone: Boolean get() = doneCount >= total
        val fraction: Float get() = if (total <= 0) 0f else doneCount.toFloat() / total
    }

    /**
     * تقدّم المجموعة اليوم — يُحصى فقط ما يعود لذكور المجموعة نفسها
     * (علامة لمجموعةٍ أخرى لا تعد تقدّماً هنا).
     */
    fun groupProgress(done: Set<String>, group: AzkarGroup): GroupProgress {
        val ids = group.items.mapTo(HashSet()) { it.id }
        val doneCount = done.count { it in ids }
        return GroupProgress(doneCount = doneCount, total = group.items.size)
    }

    /**
     * الصلاة التالية على النبي — العدّ الحرّ يتصاعد واحداً بلا سقف:
     * لا هبوط ولا التفاف (عقد «ذكر لوحده» — لا هدف ولا مائات).
     */
    fun nextPraiseCount(count: Int): Int = count.coerceAtLeast(0) + 1

    /**
     * هل هذه الصلاةُ المائةَ؟ تُحتفى بها (رشّة أقصى واهتزاز أوضح):
     * الأولى عند ١٠٠ وكل مائة بعدها — والصفر ليس مائةً أبداً.
     */
    fun isHundredth(count: Int): Boolean = count > 0 && count % 100 == 0
}
