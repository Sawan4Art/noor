package com.sawan4art.noor.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * لوحة ألوان «نور» — عشرة ألوان لتطبيق كامل (توجيه المالك alpha18:
 * «نضيف قسم جديد في الاعدادات لتغيير ألوان التطبيق وعايز ١٠ ألوان»).
 *
 * كل لون يغذّي ما يُقرأ كهوية اللون: الأساسي والحاويات في الوضعين
 * النهاري والليلي + تدرّج بطاقة الهيرو. الحياد (الخلفيات والأسطح
 * والحدود) والذهبي الثانوي (حلقة العدّاد) ثوابت هوية لا تتغير —
 * فيبقى التطبيق «نور» بأي لون.
 *
 * القيم مشتقّة يدوياً بلغة Material: النهاري = أساس عميق وحاوية فاتحة
 * تُقرأ عليها نصوص داكنة؛ الليلي = أساس فاتح وحاوية غامقة تُقرأ عليها
 * نصوص فاتحة، مع تدرّج هيرو أعمق من الأساس.
 */
@Immutable
data class AccentSet(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val heroTop: Color,
    val heroBottom: Color,
    val heroContent: Color,
    val heroMuted: Color,
)

@Immutable
data class AccentColor(
    /** مُعرّف التخزين — لاتيني ثابت في DataStore */
    val id: String,
    val arabicName: String,
    val day: AccentSet,
    val night: AccentSet,
)

object AccentPalette {

    const val DEFAULT_ID = "emerald"

    /** العشرة ألوان — الترتيب هو ترتيب العرض في الإعدادات */
    val All: List<AccentColor> = listOf(
        AccentColor(
            id = "emerald",
            arabicName = "زمردي",
            day = AccentSet(
                primary = Color(0xFF2E6B5E), onPrimary = Color(0xFFFFFFFF),
                primaryContainer = Color(0xFFBFE5D8), onPrimaryContainer = Color(0xFF0E2F27),
                heroTop = Color(0xFF35655B), heroBottom = Color(0xFF22453D),
                heroContent = Color(0xFFF2EDE0), heroMuted = Color(0xFFB9CEC4),
            ),
            night = AccentSet(
                primary = Color(0xFF82C9B0), onPrimary = Color(0xFF0A2A21),
                primaryContainer = Color(0xFF24463D), onPrimaryContainer = Color(0xFFBFE5D8),
                heroTop = Color(0xFF17332C), heroBottom = Color(0xFF0B1A16),
                heroContent = Color(0xFFE8EFE9), heroMuted = Color(0xFF8FA89C),
            ),
        ),
        AccentColor(
            id = "indigo",
            arabicName = "نيلي",
            day = AccentSet(
                primary = Color(0xFF3A5A94), onPrimary = Color(0xFFFFFFFF),
                primaryContainer = Color(0xFFD0DEF5), onPrimaryContainer = Color(0xFF12264A),
                heroTop = Color(0xFF3D5A85), heroBottom = Color(0xFF1F3352),
                heroContent = Color(0xFFE9EEF8), heroMuted = Color(0xFFB4C4E0),
            ),
            night = AccentSet(
                primary = Color(0xFF9DB8E8), onPrimary = Color(0xFF0E1F3D),
                primaryContainer = Color(0xFF2B3F63), onPrimaryContainer = Color(0xFFD0DEF5),
                heroTop = Color(0xFF1E2F4E), heroBottom = Color(0xFF101A2E),
                heroContent = Color(0xFFE5ECF8), heroMuted = Color(0xFF93A6C6),
            ),
        ),
        AccentColor(
            id = "sky",
            arabicName = "سماوي",
            day = AccentSet(
                primary = Color(0xFF2B6E86), onPrimary = Color(0xFFFFFFFF),
                primaryContainer = Color(0xFFC3E4EF), onPrimaryContainer = Color(0xFF0B2E3A),
                heroTop = Color(0xFF2F6B80), heroBottom = Color(0xFF1B4351),
                heroContent = Color(0xFFE8F4F8), heroMuted = Color(0xFFB3D3DE),
            ),
            night = AccentSet(
                primary = Color(0xFF8AC8DE), onPrimary = Color(0xFF082833),
                primaryContainer = Color(0xFF214857), onPrimaryContainer = Color(0xFFC3E4EF),
                heroTop = Color(0xFF15323E), heroBottom = Color(0xFF0A1A21),
                heroContent = Color(0xFFE4F1F6), heroMuted = Color(0xFF8DAEBB),
            ),
        ),
        AccentColor(
            id = "royal",
            arabicName = "ملكي",
            day = AccentSet(
                primary = Color(0xFF414892), onPrimary = Color(0xFFFFFFFF),
                primaryContainer = Color(0xFFD4D7F2), onPrimaryContainer = Color(0xFF181C4A),
                heroTop = Color(0xFF454A8C), heroBottom = Color(0xFF252852),
                heroContent = Color(0xFFECEDF9), heroMuted = Color(0xFFB9BCDD),
            ),
            night = AccentSet(
                primary = Color(0xFFA6AAE4), onPrimary = Color(0xFF14163B),
                primaryContainer = Color(0xFF323667), onPrimaryContainer = Color(0xFFD4D7F2),
                heroTop = Color(0xFF232552), heroBottom = Color(0xFF12132C),
                heroContent = Color(0xFFE9EAF7), heroMuted = Color(0xFF99A0C4),
            ),
        ),
        AccentColor(
            id = "violet",
            arabicName = "بنفسجي",
            day = AccentSet(
                primary = Color(0xFF6A4A96), onPrimary = Color(0xFFFFFFFF),
                primaryContainer = Color(0xFFE2D6F2), onPrimaryContainer = Color(0xFF2A1448),
                heroTop = Color(0xFF664A8C), heroBottom = Color(0xFF3B2A52),
                heroContent = Color(0xFFF1ECF8), heroMuted = Color(0xFFC9BADF),
            ),
            night = AccentSet(
                primary = Color(0xFFBBA4E0), onPrimary = Color(0xFF22103B),
                primaryContainer = Color(0xFF45316B), onPrimaryContainer = Color(0xFFE2D6F2),
                heroTop = Color(0xFF34234E), heroBottom = Color(0xFF1A1128),
                heroContent = Color(0xFFEEE8F7), heroMuted = Color(0xFFA892C4),
            ),
        ),
        AccentColor(
            id = "rose",
            arabicName = "واردي",
            day = AccentSet(
                primary = Color(0xFFA04868), onPrimary = Color(0xFFFFFFFF),
                primaryContainer = Color(0xFFF6D5DF), onPrimaryContainer = Color(0xFF451225),
                heroTop = Color(0xFF9A4A66), heroBottom = Color(0xFF5C2A3C),
                heroContent = Color(0xFFF9EDF1), heroMuted = Color(0xFFE0BAC7),
            ),
            night = AccentSet(
                primary = Color(0xFFE39CB4), onPrimary = Color(0xFF3A1020),
                primaryContainer = Color(0xFF633044), onPrimaryContainer = Color(0xFFF6D5DF),
                heroTop = Color(0xFF4B2334), heroBottom = Color(0xFF26111A),
                heroContent = Color(0xFFF7EBEF), heroMuted = Color(0xFFC494A5),
            ),
        ),
        AccentColor(
            id = "ruby",
            arabicName = "عنابي",
            day = AccentSet(
                primary = Color(0xFF8E3B3B), onPrimary = Color(0xFFFFFFFF),
                primaryContainer = Color(0xFFF3D2D2), onPrimaryContainer = Color(0xFF3D1010),
                heroTop = Color(0xFF883C3C), heroBottom = Color(0xFF4E1F1F),
                heroContent = Color(0xFFF8ECEC), heroMuted = Color(0xFFDBB3B3),
            ),
            night = AccentSet(
                primary = Color(0xFFE09A9A), onPrimary = Color(0xFF361212),
                primaryContainer = Color(0xFF5C2828), onPrimaryContainer = Color(0xFFF3D2D2),
                heroTop = Color(0xFF3E1B1B), heroBottom = Color(0xFF1F0D0D),
                heroContent = Color(0xFFF6EAEA), heroMuted = Color(0xFFC08E8E),
            ),
        ),
        AccentColor(
            id = "amber",
            arabicName = "كهرماني",
            day = AccentSet(
                primary = Color(0xFF9A6A1E), onPrimary = Color(0xFFFFFFFF),
                primaryContainer = Color(0xFFF4E2BC), onPrimaryContainer = Color(0xFF3D2A08),
                heroTop = Color(0xFF96681F), heroBottom = Color(0xFF563A0F),
                heroContent = Color(0xFFF9F1DD), heroMuted = Color(0xFFDFC794),
            ),
            night = AccentSet(
                primary = Color(0xFFE5BC6B), onPrimary = Color(0xFF342106),
                primaryContainer = Color(0xFF66491A), onPrimaryContainer = Color(0xFFF4E2BC),
                heroTop = Color(0xFF483113), heroBottom = Color(0xFF241808),
                heroContent = Color(0xFFF7EFDC), heroMuted = Color(0xFFC2A26B),
            ),
        ),
        AccentColor(
            id = "copper",
            arabicName = "نحاسي",
            day = AccentSet(
                primary = Color(0xFF9C5424), onPrimary = Color(0xFFFFFFFF),
                primaryContainer = Color(0xFFF4D9C2), onPrimaryContainer = Color(0xFF3F1F09),
                heroTop = Color(0xFF97562A), heroBottom = Color(0xFF583014),
                heroContent = Color(0xFFF9EFE4), heroMuted = Color(0xFFDFC0A4),
            ),
            night = AccentSet(
                primary = Color(0xFFE6A972), onPrimary = Color(0xFF361B08),
                primaryContainer = Color(0xFF674021), onPrimaryContainer = Color(0xFFF4D9C2),
                heroTop = Color(0xFF492911), heroBottom = Color(0xFF251408),
                heroContent = Color(0xFFF7EDE2), heroMuted = Color(0xFFC4A07E),
            ),
        ),
        AccentColor(
            id = "olive",
            arabicName = "زيتوني",
            day = AccentSet(
                primary = Color(0xFF6B6B2E), onPrimary = Color(0xFFFFFFFF),
                primaryContainer = Color(0xFFE4E4BE), onPrimaryContainer = Color(0xFF2A2A0C),
                heroTop = Color(0xFF65652F), heroBottom = Color(0xFF3C3C1A),
                heroContent = Color(0xFFF4F4E4), heroMuted = Color(0xFFCFCFA6),
            ),
            night = AccentSet(
                primary = Color(0xFFC4C47E), onPrimary = Color(0xFF242409),
                primaryContainer = Color(0xFF48482A), onPrimaryContainer = Color(0xFFE4E4BE),
                heroTop = Color(0xFF33331B), heroBottom = Color(0xFF1A1A0D),
                heroContent = Color(0xFFF1F1E2), heroMuted = Color(0xFFA5A584),
            ),
        ),
    )

    val default: AccentColor get() = All.first()

    /** الاسترجاع المتسامح: معرّف غريب أو مفقود يرجع للزمري الافتراضي */
    fun byId(id: String?): AccentColor =
        All.firstOrNull { it.id == id } ?: default
}
