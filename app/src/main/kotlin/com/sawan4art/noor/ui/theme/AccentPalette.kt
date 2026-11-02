package com.sawan4art.noor.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * لوحة ألوان «نور» — عشرون لوناً متمايزاً (توجيه المالك alpha32:
 * «امسح اللون الابيض من قائمه الالوان و واضف الوان جديده جريئه و
 * مختلفه عن الالوان الموجوده و خلي مجموع الالوان كلها ٢٠ لون»):
 *
 * - **الأبيض أُزيل** (توجيه المالك نفسه) — مستخدموه القديمون ينتقلون
 *   سالمين للزمري الافتراضي عبر الاسترجاع المتسامح [byId].
 * - **تسع ألوان جريئة جديدة** لا تشبه أي لون قائم: أخضر عشبي، لايم،
 *   بندقي، قرمزي، ماجنتا، أرجواني، أزرق بنفسجي، كوبالت، سيان — وكل
 *   زوج في اللوحة فُحص رقمياً في الوضعين (تباعد صبغة ≥ ١٥° أو تشبّع
 *   ≥ ٠٫٢٨ أو قيمة ≥ ٠٫٢٢) بعقد مختبَر يمنع عودة أي تشابه.
 * - **العشرة العائلية القديمة باقية** (زمردي تركوازي سماوي نيلي بنفسجي
 *   واردي عنابي ناري كهرماني زيتوني) + **الأسود** المحايد.
 *
 * كل لون يغذّي ما يُقرأ كهوية اللون: الأساسي والحاويات في الوضعين
 * النهاري والليلي + تدرّج بطاقة الهيرو. الحياد (الخلفيات والأسطح
 * والحدود) والذهبي الثانوي (حلقة العدّاد) ثوابت هوية لا تتغير —
 * فيبقى التطبيق «نور» بأي لون. كل الأساسيّات تحمل نصوصها البيضاء
 * بتباين ≥ ٤:١ في الوضعين (عقد مختبَر).
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

    /** الألوان العشرون — الترتيب هو ترتيب العرض في الإعدادات */
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
            id = "grass",
            arabicName = "أخضر عشبي",
            day = AccentSet(
                primary = Color(0xFF177A44), onPrimary = Color(0xFFFFFFFF),
                primaryContainer = Color(0xFFB7E5CB), onPrimaryContainer = Color(0xFF052A16),
                heroTop = Color(0xFF18754A), heroBottom = Color(0xFF0C3B22),
                heroContent = Color(0xFFE7F5EC), heroMuted = Color(0xFFA9D2B9),
            ),
            night = AccentSet(
                primary = Color(0xFF6FD98F), onPrimary = Color(0xFF0C2E1B),
                primaryContainer = Color(0xFF1F5236), onPrimaryContainer = Color(0xFFB7E5CB),
                heroTop = Color(0xFF123322), heroBottom = Color(0xFF081A10),
                heroContent = Color(0xFFE4F1E9), heroMuted = Color(0xFF8AAE9A),
            ),
        ),
        AccentColor(
            id = "chartreuse",
            arabicName = "لايم",
            day = AccentSet(
                primary = Color(0xFF5A8A00), onPrimary = Color(0xFFFFFFFF),
                primaryContainer = Color(0xFFDDEBB5), onPrimaryContainer = Color(0xFF1F2A04),
                heroTop = Color(0xFF568415), heroBottom = Color(0xFF2C3F0A),
                heroContent = Color(0xFFF1F6E0), heroMuted = Color(0xFFC7D894),
            ),
            night = AccentSet(
                primary = Color(0xFFA4CC4A), onPrimary = Color(0xFF1C2A05),
                primaryContainer = Color(0xFF44541B), onPrimaryContainer = Color(0xFFDDEBB5),
                heroTop = Color(0xFF25300F), heroBottom = Color(0xFF111705),
                heroContent = Color(0xFFEDF2DC), heroMuted = Color(0xFFA2AD7C),
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
        AccentColor(
            id = "amber",
            arabicName = "كهرماني",
            day = AccentSet(
                primary = Color(0xFF967A0E), onPrimary = Color(0xFFFFFFFF),
                primaryContainer = Color(0xFFF4E2BC), onPrimaryContainer = Color(0xFF3D2A08),
                heroTop = Color(0xFF92740E), heroBottom = Color(0xFF54390D),
                heroContent = Color(0xFFF9F1DD), heroMuted = Color(0xFFDFC794),
            ),
            night = AccentSet(
                primary = Color(0xFFEFC457), onPrimary = Color(0xFF342106),
                primaryContainer = Color(0xFF66491A), onPrimaryContainer = Color(0xFFF4E2BC),
                heroTop = Color(0xFF483113), heroBottom = Color(0xFF241808),
                heroContent = Color(0xFFF7EFDC), heroMuted = Color(0xFFC2A26B),
            ),
        ),
        AccentColor(
            id = "coffee",
            arabicName = "بندقي",
            day = AccentSet(
                primary = Color(0xFF6E4423), onPrimary = Color(0xFFFFFFFF),
                primaryContainer = Color(0xFFEFDCC8), onPrimaryContainer = Color(0xFF271305),
                heroTop = Color(0xFF6A4528), heroBottom = Color(0xFF3A2412),
                heroContent = Color(0xFFF7EEE4), heroMuted = Color(0xFFD5BBA2),
            ),
            night = AccentSet(
                primary = Color(0xFFC08560), onPrimary = Color(0xFF241304),
                primaryContainer = Color(0xFF54381F), onPrimaryContainer = Color(0xFFEFDCC8),
                heroTop = Color(0xFF2E1E10), heroBottom = Color(0xFF160D06),
                heroContent = Color(0xFFF3E9DE), heroMuted = Color(0xFFB08F74),
            ),
        ),
        AccentColor(
            id = "blaze",
            arabicName = "ناري",
            day = AccentSet(
                primary = Color(0xFFC24C00), onPrimary = Color(0xFFFFFFFF),
                primaryContainer = Color(0xFFFFD1B0), onPrimaryContainer = Color(0xFF471D02),
                heroTop = Color(0xFFBD4A03), heroBottom = Color(0xFF6E2C03),
                heroContent = Color(0xFFFFF0E2), heroMuted = Color(0xFFEDB88C),
            ),
            night = AccentSet(
                primary = Color(0xFFFF9450), onPrimary = Color(0xFF43200A),
                primaryContainer = Color(0xFF79401A), onPrimaryContainer = Color(0xFFFFD1B0),
                heroTop = Color(0xFF43220C), heroBottom = Color(0xFF211006),
                heroContent = Color(0xFFFAEBDC), heroMuted = Color(0xFFC9946C),
            ),
        ),
        AccentColor(
            id = "scarlet",
            arabicName = "قرمزي",
            day = AccentSet(
                primary = Color(0xFFD62B2B), onPrimary = Color(0xFFFFFFFF),
                primaryContainer = Color(0xFFF7CFCF), onPrimaryContainer = Color(0xFF3D0606),
                heroTop = Color(0xFFC92F2F), heroBottom = Color(0xFF6E1010),
                heroContent = Color(0xFFFBEAEA), heroMuted = Color(0xFFEBABAB),
            ),
            night = AccentSet(
                primary = Color(0xFFF45555), onPrimary = Color(0xFF3A0A0A),
                primaryContainer = Color(0xFF7A2424), onPrimaryContainer = Color(0xFFF7CFCF),
                heroTop = Color(0xFF3A1616), heroBottom = Color(0xFF1C0808),
                heroContent = Color(0xFFF8EAEA), heroMuted = Color(0xFFC48C8C),
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
            id = "magenta",
            arabicName = "ماجنتا",
            day = AccentSet(
                primary = Color(0xFFC724B1), onPrimary = Color(0xFFFFFFFF),
                primaryContainer = Color(0xFFF4CDF0), onPrimaryContainer = Color(0xFF420A3A),
                heroTop = Color(0xFFBB2BA8), heroBottom = Color(0xFF5E1454),
                heroContent = Color(0xFFFAEBF8), heroMuted = Color(0xFFE4AEDD),
            ),
            night = AccentSet(
                primary = Color(0xFFE878D8), onPrimary = Color(0xFF3A0A33),
                primaryContainer = Color(0xFF6E2263), onPrimaryContainer = Color(0xFFF4CDF0),
                heroTop = Color(0xFF3B1436), heroBottom = Color(0xFF1D081B),
                heroContent = Color(0xFFF7EBF5), heroMuted = Color(0xFFC391BB),
            ),
        ),
        AccentColor(
            id = "radiant",
            arabicName = "أرجواني",
            day = AccentSet(
                primary = Color(0xFF9B30D9), onPrimary = Color(0xFFFFFFFF),
                primaryContainer = Color(0xFFE9CDF8), onPrimaryContainer = Color(0xFF2E0A47),
                heroTop = Color(0xFF9233C4), heroBottom = Color(0xFF48155F),
                heroContent = Color(0xFFF6ECFC), heroMuted = Color(0xFFD3AEEA),
            ),
            night = AccentSet(
                primary = Color(0xFFC878F0), onPrimary = Color(0xFF2A0E42),
                primaryContainer = Color(0xFF502575), onPrimaryContainer = Color(0xFFE9CDF8),
                heroTop = Color(0xFF2C1A44), heroBottom = Color(0xFF150A22),
                heroContent = Color(0xFFF2EAF9), heroMuted = Color(0xFFAF92C9),
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
            id = "ultraviolet",
            arabicName = "أزرق بنفسجي",
            day = AccentSet(
                primary = Color(0xFF4E35C8), onPrimary = Color(0xFFFFFFFF),
                primaryContainer = Color(0xFFDCD7F8), onPrimaryContainer = Color(0xFF140A47),
                heroTop = Color(0xFF4A37B5), heroBottom = Color(0xFF241A5C),
                heroContent = Color(0xFFEDEEF9), heroMuted = Color(0xFFB8B4E3),
            ),
            night = AccentSet(
                primary = Color(0xFF9186EE), onPrimary = Color(0xFF120A3A),
                primaryContainer = Color(0xFF322A6E), onPrimaryContainer = Color(0xFFDCD7F8),
                heroTop = Color(0xFF1C1740), heroBottom = Color(0xFF0D0A20),
                heroContent = Color(0xFFEAE9F7), heroMuted = Color(0xFF9290C0),
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
            id = "cobalt",
            arabicName = "كوبالت",
            day = AccentSet(
                primary = Color(0xFF2244D6), onPrimary = Color(0xFFFFFFFF),
                primaryContainer = Color(0xFFD5DBF9), onPrimaryContainer = Color(0xFF0A123F),
                heroTop = Color(0xFF2742B8), heroBottom = Color(0xFF12205C),
                heroContent = Color(0xFFE9EDF9), heroMuted = Color(0xFFA5B3E0),
            ),
            night = AccentSet(
                primary = Color(0xFF5C74F5), onPrimary = Color(0xFF0A123F),
                primaryContainer = Color(0xFF28377A), onPrimaryContainer = Color(0xFFD5DBF9),
                heroTop = Color(0xFF161E45), heroBottom = Color(0xFF090D22),
                heroContent = Color(0xFFE7EAF6), heroMuted = Color(0xFF8C97C2),
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
            id = "cyan",
            arabicName = "سيان",
            day = AccentSet(
                primary = Color(0xFF007E96), onPrimary = Color(0xFFFFFFFF),
                primaryContainer = Color(0xFFB8E8F2), onPrimaryContainer = Color(0xFF00303A),
                heroTop = Color(0xFF037B91), heroBottom = Color(0xFF034450),
                heroContent = Color(0xFFE4F6F9), heroMuted = Color(0xFF8ECBD6),
            ),
            night = AccentSet(
                primary = Color(0xFF3DD0E8), onPrimary = Color(0xFF00303A),
                primaryContainer = Color(0xFF0B5866), onPrimaryContainer = Color(0xFFB8E8F2),
                heroTop = Color(0xFF0A3540), heroBottom = Color(0xFF041A1F),
                heroContent = Color(0xFFE1F2F5), heroMuted = Color(0xFF7BA8B0),
            ),
        ),
        AccentColor(
            id = "turquoise",
            arabicName = "تركوازي",
            day = AccentSet(
                primary = Color(0xFF00897B), onPrimary = Color(0xFFFFFFFF),
                primaryContainer = Color(0xFFA8E4DD), onPrimaryContainer = Color(0xFF00332D),
                heroTop = Color(0xFF048C7D), heroBottom = Color(0xFF04514A),
                heroContent = Color(0xFFE4F5F2), heroMuted = Color(0xFF8CC8C0),
            ),
            night = AccentSet(
                primary = Color(0xFF3FD4C2), onPrimary = Color(0xFF00322C),
                primaryContainer = Color(0xFF0E5A51), onPrimaryContainer = Color(0xFFA8E4DD),
                heroTop = Color(0xFF0B3731), heroBottom = Color(0xFF051B18),
                heroContent = Color(0xFFE1F4F0), heroMuted = Color(0xFF7FB1A9),
            ),
        ),
        AccentColor(
            id = "black",
            arabicName = "أسود",
            day = AccentSet(
                primary = Color(0xFF232323), onPrimary = Color(0xFFFFFFFF),
                primaryContainer = Color(0xFFE5E5E5), onPrimaryContainer = Color(0xFF141414),
                heroTop = Color(0xFF2E2E2E), heroBottom = Color(0xFF141414),
                heroContent = Color(0xFFF2F2F2), heroMuted = Color(0xFF9C9C9C),
            ),
            night = AccentSet(
                primary = Color(0xFF9C9C9C), onPrimary = Color(0xFF141414),
                primaryContainer = Color(0xFF2C2C2C), onPrimaryContainer = Color(0xFFD6D6D6),
                heroTop = Color(0xFF101010), heroBottom = Color(0xFF060606),
                heroContent = Color(0xFFEAEAEA), heroMuted = Color(0xFF838383),
            ),
        ),
    )

    val default: AccentColor get() = All.first()

    /** الاسترجاع المتسامح: معرّف غريب أو مفقود (من ألفا سابقة) يرجع للزمري الافتراضي */
    fun byId(id: String?): AccentColor =
        All.firstOrNull { it.id == id } ?: default
}
