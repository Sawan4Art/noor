package com.sawan4art.noor.domain.adhan

import com.sawan4art.noor.domain.prayer.PrayerTimeKey
import java.time.ZonedDateTime

/**
 * صوت لكل صلاة على حدة (alpha61) — بلاغ المالك: «عايز ظبط اصوات
 * الاذان و الاقامه يكون اما لكل صلاه على حده او لكل الصلوات».
 *
 * الخريطتان تُخزَّنان سلسلةً CSV بالشكل «FAJR:main,DHUHR:fakhry» —
 * مفتاح [PrayerTimeKey.name] إلى معرّف الصوت، ومحلِّلٌ متسامحٌ يرمي
 * الشاذّ (درس الفواصل: أي قيمةٍ غريبةٍ من نسخةٍ قديمة يسير بأمان).
 */
object PrayerVoiceMaps {

    /** تحليل سلسلة CSV — الفارغ يعود فارغاً والمفتاح الغريب يُرمى */
    fun parse(raw: String?): Map<String, String> {
        if (raw.isNullOrBlank()) return emptyMap()
        val out = mutableMapOf<String, String>()
        for (entry in raw.split(',')) {
            val parts = entry.split(':', limit = 2)
            if (parts.size != 2) continue
            val key = parts[0].trim()
            val value = parts[1].trim()
            if (key.isBlank() || value.isBlank()) continue
            val known = runCatching { PrayerTimeKey.valueOf(key) }.getOrNull() ?: continue
            out[known.name] = value
        }
        return out
    }

    /** تنسيق الخريطة إلى CSV بترتيب الصلوات الثابت */
    fun format(map: Map<String, String>): String =
        PrayerTimeKey.entries.mapNotNull { key ->
            map[key.name]?.takeIf { it.isNotBlank() }?.let { "${key.name}:$it" }
        }.joinToString(",")
}

/**
 * التنبيه قبل أو بعد أذان أي صلاة (alpha61) — بلاغ المالك: «عايز اضيف
 * في الاعادات زر جديد خاص بالتنبيه قبل او بعد اذان اي صلاه ده يكون
 * عباره عن منبه يتم تفعيله قبل او بعد الصلاه بوقت محدد لكل صلاه».
 *
 * وalpha63 بلاغ المالك: «ميزه التنبيه قبل او بعد الاذان عايز يكون فيها
 * اسماء المنبهات المظبوطه يعني مثلا اذا اراد المستخدم ظبط تنبيه
 * السحور قبل اذان الفجر و هكذا اي تنبيه لاز يكون ليه اسم» — فكل قاعدة
 * تحمل **اسماً مخصصاً** [Rule.name] يظهر كما هو في شاشة المنبه
 * والإشعار، والفارغ يعود للنص التلقائي «تنبيه قبل أذان الفجر بـ ١٠
 * دقيقة». وتُخزَّن CSV بالشكل
 * «FAJR:BEFORE:10:تنبيه السحور,MAGHRIB:AFTER:5» — والمقادير الثلاثية
 * القديمة بلا اسمٍ تُقرأ كما هي فلا يفقد مستخدمٌ تخصيصه.
 */
object PrayerReminder {

    /** نوع التنبيه — قبله أم بعده أم لا شيء */
    enum class Mode { OFF, BEFORE, AFTER }

    /** قاعدة صلاة واحدة — واسمها المخصص في شاشة المنبه (فارغ = التلقائي) */
    data class Rule(val mode: Mode, val minutes: Int, val name: String = "")

    /** حد الدقائق — ساعةٌ كاملة كحدٍّ أعلى كفواصل الأذان نفسها */
    const val MIN_MINUTES = 1
    const val MAX_MINUTES = 120

    /** سقف طول الاسم — كي لا يخنق اسماً هائلاً سطر الإشعار */
    const val MAX_NAME_CHARS = 60

    /** تحصين الدقائق داخل الحدود */
    fun clampMinutes(minutes: Int): Int = minutes.coerceIn(MIN_MINUTES, MAX_MINUTES)

    /**
     * تحصين اسم التنبيه: بلا فواصلٍ ولا نقاط-فواصل (سطر CSV) وبلا
     * أسطرٍ جديدةٍ وبطولٍ محصور — و«الفاصلة» في الاسم تبقى «،» العربية
     * فلا تكسر السجل.
     */
    fun sanitizeName(raw: String): String =
        raw.replace(',', '،')
            .replace(':', '-')
            .replace('\n', ' ')
            .replace('\r', ' ')
            .trim()
            .take(MAX_NAME_CHARS)

    /** قاعدة سليمة؟ — قبل/بعد بدقائق داخل الحدود */
    fun isValid(rule: Rule): Boolean =
        rule.mode != Mode.OFF && rule.minutes in MIN_MINUTES..MAX_MINUTES

    /**
     * لحظة دقّ التنبيه من لحظة أذان الصلاة — قبلها مطروحاً وبعدها
     * مضافاً. والقاعدة الغريبة (OFF أو بلا دقائق) تعود null فلا حدث.
     */
    fun eventAt(prayerTime: ZonedDateTime, rule: Rule): ZonedDateTime? {
        if (!isValid(rule)) return null
        return when (rule.mode) {
            Mode.BEFORE -> prayerTime.minusMinutes(rule.minutes.toLong())
            Mode.AFTER -> prayerTime.plusMinutes(rule.minutes.toLong())
            Mode.OFF -> null
        }
    }

    /** نصّ التنبيه كما يظهر في شاشة المنبه والإشعار — والاسم المخصص أولاً */
    fun note(prayerName: String, rule: Rule): String {
        val custom = sanitizeName(rule.name)
        if (custom.isNotBlank()) return custom
        return when (rule.mode) {
            Mode.BEFORE -> "تنبيه قبل أذان $prayerName بـ ${AdhanGaps.toArabicDigits(rule.minutes)} دقيقة"
            Mode.AFTER -> "تنبيه بعد أذان $prayerName بـ ${AdhanGaps.toArabicDigits(rule.minutes)} دقيقة"
            Mode.OFF -> ""
        }
    }

    /** تحليل سلسلة قواعد CSV — الشاذّ يُرمى ولا يُسقط البقية */
    fun parseRules(raw: String?): Map<String, Rule> {
        if (raw.isNullOrBlank()) return emptyMap()
        val out = mutableMapOf<String, Rule>()
        for (entry in raw.split(',')) {
            val parts = entry.split(':', limit = 4)
            if (parts.size < 3) continue
            val key = runCatching { PrayerTimeKey.valueOf(parts[0].trim()) }.getOrNull() ?: continue
            val mode = runCatching { Mode.valueOf(parts[1].trim()) }.getOrNull() ?: continue
            val minutes = parts[2].trim().toIntOrNull() ?: continue
            if (mode == Mode.OFF) continue
            val name = parts.getOrNull(3)?.trim().orEmpty()
            out[key.name] = Rule(mode, clampMinutes(minutes), name)
        }
        return out
    }

    /** تنسيق القواعد إلى CSV بترتيب الصلوات الثابت — والاسم حاضراً إن وُجد */
    fun formatRules(rules: Map<String, Rule>): String =
        PrayerTimeKey.entries.mapNotNull { key ->
            val rule = rules[key.name] ?: return@mapNotNull null
            if (rule.mode == Mode.OFF) null
            else {
                val name = sanitizeName(rule.name)
                if (name.isBlank()) {
                    "${key.name}:${rule.mode.name}:${rule.minutes}"
                } else {
                    "${key.name}:${rule.mode.name}:${rule.minutes}:$name"
                }
            }
        }.joinToString(",")
}
