package com.sawan4art.noor.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * لوحة ألوان «نور» — اثنا عشر لوناً متمايزاً (توجيه المالك alpha29:
 * «بالنسبه لل الالوان فيها متشابه كثير انا عايز الوان غير متشابه مثال
 * فوشيا و وردي و زهري دول الوان متشابه تكاد ترى الفرق عايز الوان مختلف
 * و ليس بدرجات متقاربه و كمان عايز لونين ابيض و اسود دول اضافيين»):
 *
 * - **عشرة ألوان عائلية متباعدة** — لون واحد لكل عائلة درجية: أخضر،
 *   تركوازي، سماوي، نيلي، بنفسجي، واردي، عنابي، ناري، كهرماني، زيتوني.
 *   أُزيلت الألوان المتشابهة التي كادت ترى الفرق بينها (كهربائي ≈ نيلي،
 *   ملكي ≈ نيلي، قرمزي ≈ عنابي، نحاسي ≈ كهرماني، ليموني ≈ زيتوني،
 *   فوشيا ≈ واردي، زهري ≈ واردي، أرجواني ≈ بنفسجي، أصفر ≈ كهرماني،
 *   كحلي ≈ نيلي) — وفُرِّقت الأزواج الباقية قريبة الدرجات رقمياً
 *   (تباعد صبغة ≥ ١٥° أو تباعد تشبّع/قيمة واضح).
 * - **أبيض وأسود محايدان** — جذرمان بلا تشبّع يذكر لخلفية محايدة كاملة.
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

    /** الألوان الاثنا عشر — الترتيب هو ترتيب العرض في الإعدادات */
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
        // ===== المحايدان — أبيض وأسود (توجيه المالك alpha29) =====
        AccentColor(
            id = "white",
            arabicName = "أبيض",
            day = AccentSet(
                primary = Color(0xFF565656), onPrimary = Color(0xFFFFFFFF),
                primaryContainer = Color(0xFFEDEDED), onPrimaryContainer = Color(0xFF262626),
                heroTop = Color(0xFFFCFCFC), heroBottom = Color(0xFFE0E0E0),
                heroContent = Color(0xFF2B2B2B), heroMuted = Color(0xFF7C7C7C),
            ),
            night = AccentSet(
                primary = Color(0xFFE0E0E0), onPrimary = Color(0xFF1B1B1B),
                primaryContainer = Color(0xFF363636), onPrimaryContainer = Color(0xFFDEDEDE),
                heroTop = Color(0xFF232323), heroBottom = Color(0xFF111111),
                heroContent = Color(0xFFE9E9E9), heroMuted = Color(0xFF8E8E8E),
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
