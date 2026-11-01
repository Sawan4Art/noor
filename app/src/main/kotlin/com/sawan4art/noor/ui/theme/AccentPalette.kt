package com.sawan4art.noor.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * لوحة ألوان «نور» — عشرون لوناً لتطبيق كامل (توجيه المالك alpha18:
 * «نضيف قسم جديد في الاعدادات لتغيير ألوان التطبيق وعايز ١٠ ألوان»،
 * وتوجيهه alpha28: «عايز اضيف ١٠ الوان اضافين بس يكونوا الوان جريئه»):
 * العشرة الأصلية الهادئة ثم العشرة الجريئة عالي التشبّع.
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
        // ===== الألوان الجريئة العشرة — alpha28 (توجيه المالك: «عايز اضيف ١٠
        // الوان اضافين بس يكونوا الوان جريئه») — تشبّع عالٍ في الوضعين،
        // والنهاري يظل أساساً عميقاً تحمل نصوصه البيضاء قراءةً مريحة.
        AccentColor(
            id = "electric",
            arabicName = "كهربائي",
            day = AccentSet(
                primary = Color(0xFF0A57D0), onPrimary = Color(0xFFFFFFFF),
                primaryContainer = Color(0xFFBFD3FF), onPrimaryContainer = Color(0xFF0A1F4A),
                heroTop = Color(0xFF0B55C8), heroBottom = Color(0xFF052F6E),
                heroContent = Color(0xFFE7EFFD), heroMuted = Color(0xFFA9C2F0),
            ),
            night = AccentSet(
                primary = Color(0xFF7FA5FF), onPrimary = Color(0xFF061A3F),
                primaryContainer = Color(0xFF1F3C74), onPrimaryContainer = Color(0xFFBFD3FF),
                heroTop = Color(0xFF10254E), heroBottom = Color(0xFF091428),
                heroContent = Color(0xFFE4EDFC), heroMuted = Color(0xFF8DA6D4),
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
                primary = Color(0xFF52D6C6), onPrimary = Color(0xFF00322C),
                primaryContainer = Color(0xFF0E5A51), onPrimaryContainer = Color(0xFFA8E4DD),
                heroTop = Color(0xFF0B3731), heroBottom = Color(0xFF051B18),
                heroContent = Color(0xFFE1F4F0), heroMuted = Color(0xFF7FB1A9),
            ),
        ),
        AccentColor(
            id = "crimson",
            arabicName = "قرمزي",
            day = AccentSet(
                primary = Color(0xFFC62F2F), onPrimary = Color(0xFFFFFFFF),
                primaryContainer = Color(0xFFFFCFCF), onPrimaryContainer = Color(0xFF4A0D0D),
                heroTop = Color(0xFFC02E2E), heroBottom = Color(0xFF6E1414),
                heroContent = Color(0xFFFCEAEA), heroMuted = Color(0xFFE7A8A8),
            ),
            night = AccentSet(
                primary = Color(0xFFFF8585), onPrimary = Color(0xFF410D0D),
                primaryContainer = Color(0xFF7A2B2B), onPrimaryContainer = Color(0xFFFFCFCF),
                heroTop = Color(0xFF431515), heroBottom = Color(0xFF210909),
                heroContent = Color(0xFFFBE9E9), heroMuted = Color(0xFFCA9090),
            ),
        ),
        AccentColor(
            id = "blaze",
            arabicName = "ناري",
            day = AccentSet(
                primary = Color(0xFFE05F00), onPrimary = Color(0xFFFFFFFF),
                primaryContainer = Color(0xFFFFD1B0), onPrimaryContainer = Color(0xFF471D02),
                heroTop = Color(0xFFDA5C04), heroBottom = Color(0xFF7C3003),
                heroContent = Color(0xFFFFF0E2), heroMuted = Color(0xFFEDB88C),
            ),
            night = AccentSet(
                primary = Color(0xFFFFAB5E), onPrimary = Color(0xFF43200A),
                primaryContainer = Color(0xFF79401A), onPrimaryContainer = Color(0xFFFFD1B0),
                heroTop = Color(0xFF43220C), heroBottom = Color(0xFF211006),
                heroContent = Color(0xFFFAEBDC), heroMuted = Color(0xFFC9946C),
            ),
        ),
        AccentColor(
            id = "lime",
            arabicName = "ليموني",
            day = AccentSet(
                primary = Color(0xFF5A8A06), onPrimary = Color(0xFFFFFFFF),
                primaryContainer = Color(0xFFD8F0A6), onPrimaryContainer = Color(0xFF1F3202),
                heroTop = Color(0xFF56860A), heroBottom = Color(0xFF2B4404),
                heroContent = Color(0xFFEFF8DF), heroMuted = Color(0xFFB7D483),
            ),
            night = AccentSet(
                primary = Color(0xFFAEE45C), onPrimary = Color(0xFF1E3006),
                primaryContainer = Color(0xFF3D5A12), onPrimaryContainer = Color(0xFFD8F0A6),
                heroTop = Color(0xFF213407), heroBottom = Color(0xFF101A04),
                heroContent = Color(0xFFECF5DB), heroMuted = Color(0xFF95B26A),
            ),
        ),
        AccentColor(
            id = "fuchsia",
            arabicName = "فوشيا",
            day = AccentSet(
                primary = Color(0xFFB0096C), onPrimary = Color(0xFFFFFFFF),
                primaryContainer = Color(0xFFFFC4E1), onPrimaryContainer = Color(0xFF46032A),
                heroTop = Color(0xFFAB0E6A), heroBottom = Color(0xFF61073C),
                heroContent = Color(0xFFFDEBF4), heroMuted = Color(0xFFE5A7C8),
            ),
            night = AccentSet(
                primary = Color(0xFFFF72B4), onPrimary = Color(0xFF42081F),
                primaryContainer = Color(0xFF6D1B46), onPrimaryContainer = Color(0xFFFFC4E1),
                heroTop = Color(0xFF42102A), heroBottom = Color(0xFF200815),
                heroContent = Color(0xFFFAE7F1), heroMuted = Color(0xFFC98AA8),
            ),
        ),
        AccentColor(
            id = "hotpink",
            arabicName = "زهري",
            day = AccentSet(
                primary = Color(0xFFD5185F), onPrimary = Color(0xFFFFFFFF),
                primaryContainer = Color(0xFFFFC2D6), onPrimaryContainer = Color(0xFF47041C),
                heroTop = Color(0xFFCF1D5D), heroBottom = Color(0xFF750F33),
                heroContent = Color(0xFFFDEAF0), heroMuted = Color(0xFFEFA3BB),
            ),
            night = AccentSet(
                primary = Color(0xFFFF7CA5), onPrimary = Color(0xFF440819),
                primaryContainer = Color(0xFF7D1F3E), onPrimaryContainer = Color(0xFFFFC2D6),
                heroTop = Color(0xFF441224), heroBottom = Color(0xFF220912),
                heroContent = Color(0xFFFAE8EE), heroMuted = Color(0xFFCB8AA0),
            ),
        ),
        AccentColor(
            id = "vividpurple",
            arabicName = "أرجواني",
            day = AccentSet(
                primary = Color(0xFF8C24AA), onPrimary = Color(0xFFFFFFFF),
                primaryContainer = Color(0xFFE9C4F4), onPrimaryContainer = Color(0xFF360942),
                heroTop = Color(0xFF8727A3), heroBottom = Color(0xFF4C135B),
                heroContent = Color(0xFFF7EBFA), heroMuted = Color(0xFFD4A3E2),
            ),
            night = AccentSet(
                primary = Color(0xFFDC98F2), onPrimary = Color(0xFF370945),
                primaryContainer = Color(0xFF5C1A6F), onPrimaryContainer = Color(0xFFE9C4F4),
                heroTop = Color(0xFF361042), heroBottom = Color(0xFF1B0820),
                heroContent = Color(0xFFF5EBF8), heroMuted = Color(0xFFB384BF),
            ),
        ),
        AccentColor(
            id = "solar",
            arabicName = "أصفر",
            day = AccentSet(
                primary = Color(0xFF9C7A00), onPrimary = Color(0xFFFFFFFF),
                primaryContainer = Color(0xFFFAE570), onPrimaryContainer = Color(0xFF332703),
                heroTop = Color(0xFF98770B), heroBottom = Color(0xFF554104),
                heroContent = Color(0xFFFAF3D8), heroMuted = Color(0xFFDCC46D),
            ),
            night = AccentSet(
                primary = Color(0xFFFFD54F), onPrimary = Color(0xFF372C05),
                primaryContainer = Color(0xFF6A5510), onPrimaryContainer = Color(0xFFFAE570),
                heroTop = Color(0xFF392D08), heroBottom = Color(0xFF1C1604),
                heroContent = Color(0xFFF9F2D9), heroMuted = Color(0xFFC3AC5E),
            ),
        ),
        AccentColor(
            id = "navy",
            arabicName = "كحلي",
            day = AccentSet(
                primary = Color(0xFF1A237E), onPrimary = Color(0xFFFFFFFF),
                primaryContainer = Color(0xFFC7CBF7), onPrimaryContainer = Color(0xFF0B0F3D),
                heroTop = Color(0xFF1B2377), heroBottom = Color(0xFF0E1245),
                heroContent = Color(0xFFE9EBF9), heroMuted = Color(0xFFA9AED6),
            ),
            night = AccentSet(
                primary = Color(0xFF9BA7F5), onPrimary = Color(0xFF0D1240),
                primaryContainer = Color(0xFF2A3470), onPrimaryContainer = Color(0xFFC7CBF7),
                heroTop = Color(0xFF1B2150), heroBottom = Color(0xFF0D1028),
                heroContent = Color(0xFFE8EAF8), heroMuted = Color(0xFF98A0C8),
            ),
        ),
    )

    val default: AccentColor get() = All.first()

    /** الاسترجاع المتسامح: معرّف غريب أو مفقود يرجع للزمري الافتراضي */
    fun byId(id: String?): AccentColor =
        All.firstOrNull { it.id == id } ?: default
}
