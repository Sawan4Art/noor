package com.sawan4art.noor.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sawan4art.noor.domain.adhan.AdhanGaps
import com.sawan4art.noor.domain.mushaf.MushafPages
import com.sawan4art.noor.domain.prayer.CalculationMethod
import com.sawan4art.noor.domain.prayer.PrayerTimeKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.noorDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "noor_settings",
)

/** وضع المظهر */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * تفضيلات الأذان والإقامة — القسم الجديد في الإعدادات:
 * - الأذان مفعّل افتراضياً للصلوات الخمس (الشروق بلا أذان)
 * - الإقامة مطفأة افتراضياً — وفاصلها خانة كتابة **لكل صلاة على حدة**
 *   (تحديث المالك 2026-11-06: «خانه كتابه رقم مش اختيارات و يكون لكل
 *   صلاه على حده») — الافتراضي كل الصلوات ١٠ دقائق ماعاد الفجر ١٥ دقيقة
 * - الجمعة استثناء: أذان مرتين — فاصل ١٠ دقائق بين الأذانين ثم
 *   الإقامة بعد ٣٠ دقيقة (خانتا كتابة مستقلتان حتى ١٢٠ دقيقة)
 * - كتم أصوات الهاتف **بعد الإقامة** اختياري (مطفأ افتراضياً، ١٠ دقائق)
 *   واختيار جانبي لإلغاء الكتم بمجرد فتح قفل الشاشة يظهر مع تفعيل الكتم فقط
 * - صوت مخصص من الهاتف للأذان وللإقامة (مسار واسم الملف)
 * - أو نغمة إشعارات الهاتف بدلاً من الأذان/الإقامة
 */
data class AdhanPrefs(
    val enabled: Boolean = true,
    /** صوت الأذان: معرّف مدمج | custom (من الهاتف) | notification (نغمة الإشعارات) */
    val adhanVoice: String = "main",
    val iqamahEnabled: Boolean = false,
    /** فاصل الأذان←الإقامة لكل صلاة على حدة (خانة كتابة ١..١٢٠ دقيقة) */
    val gapFajrMinutes: Int = AdhanGaps.DEFAULT_FAJR_GAP_MINUTES,
    val gapDhuhrMinutes: Int = AdhanGaps.DEFAULT_PRAYER_GAP_MINUTES,
    val gapAsrMinutes: Int = AdhanGaps.DEFAULT_PRAYER_GAP_MINUTES,
    val gapMaghribMinutes: Int = AdhanGaps.DEFAULT_PRAYER_GAP_MINUTES,
    val gapIshaMinutes: Int = AdhanGaps.DEFAULT_PRAYER_GAP_MINUTES,
    val iqamahVoice: String = "makkah",
    val fridayDouble: Boolean = true,
    val fridayAdhanGapMinutes: Int = AdhanGaps.DEFAULT_FRIDAY_ADHAN_GAP_MINUTES,
    val fridayIqamahGapMinutes: Int = AdhanGaps.DEFAULT_FRIDAY_IQAMAH_GAP_MINUTES,
    val muteEnabled: Boolean = false,
    val muteMinutes: Int = 10,
    /** إلغاء الكتم بمجرد فتح قفل الشاشة — اختيار جانبي يظهر مع الكتم فقط */
    val muteCancelOnUnlock: Boolean = true,
    val customAdhanPath: String = "",
    val customAdhanName: String = "",
    val customIqamahPath: String = "",
    val customIqamahName: String = "",
) {
    /** فاصل الأذان←الإقامة الخاص بصلاة معينة — استهلاك خطّة الأذان */
    fun gapFor(key: PrayerTimeKey): Int = when (key) {
        PrayerTimeKey.FAJR -> gapFajrMinutes
        PrayerTimeKey.DHUHR -> gapDhuhrMinutes
        PrayerTimeKey.ASR -> gapAsrMinutes
        PrayerTimeKey.MAGHRIB -> gapMaghribMinutes
        PrayerTimeKey.ISHA -> gapIshaMinutes
        PrayerTimeKey.SUNRISE -> 0 // الشروق ليست صلاة ولا أذان فيها
    }
}

/** ذاكرة نظام التحديثات — متى فُحص آخر مرة وماذا أُشعر عنه ومتى واستُبعد */
data class UpdatePrefs(
    val lastCheckAt: Long = 0L,
    val notifiedVersion: String? = null,
    /** آخر وقت نُشر فيه إشعار فعلي — عصا سياسة التكرار (alpha37) */
    val lastNotifiedAt: Long = 0L,
    val dismissedVersion: String? = null,
)

/**
 * إعدادات التطبيق — DataStore (الخليفة الحديث لـ SharedPreferences).
 * القيم الافتراضية: القاهرة (نفس المرجع) — أول تحديد موقع يستبدلها.
 * accentId معرّف لون التطبيق (ui.theme.AccentPalette) — زمردي افتراضياً.
 * primaryId معرّف **اللون الأساسي** (alpha32) — فارغ = يتبع لون التطبيق
 * الحالي (وهذا هو الافتراضي بأمر المالك: «طبعا الافتراضي هو الحالي»)،
 * وقيمته من AccentPalette نفسها فالقائمتان نفسا بقائمة العشرين.
 */
data class NoorSettingsState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val method: CalculationMethod = CalculationMethod.EGYPT,
    val isHanafi: Boolean = false,
    val city: String = "القاهرة، مصر",
    val latitude: Double = 30.0444,
    val longitude: Double = 31.2357,
    /** هل حدد المستخدم موقعه بنفسه ولو مرة (لعرض زر التحديد كإجراء أول) */
    val locationResolved: Boolean = false,
    /** لون التطبيق (مُعرّف من AccentPalette) — تغييره من الإعدادات (alpha18) */
    val accentId: String = "emerald",
    /** اللون الأساسي (alpha32) — فارغ = يتبع لون التطبيق الحالي */
    val primaryId: String = "",
    /** آخر صفحة مصحف (alpha49) — القارئ يفتح مكمنه الذي تركه، الافتراضي الفاتحة */
    val mushafPage: Int = 1,
    /** العلامة المرجعية للمصحف — صفر سورة يعني لا علامة (alpha51) */
    val mushafBookmarkSura: Int = 0,
    val mushafBookmarkAyah: Int = 0,
    val mushafBookmarkPage: Int = 1,
    /** آيات الحفظ الميسر — مجموعة «سورة:آية» المعلَّمة محفوظة (alpha53) */
    val mushafHifz: Set<String> = emptySet(),
    /**
     * الشيخ المختار لتلاوة المصحف (alpha54) — معرّف من [com.sawan4art.noor.domain.mushaf.QuranReciters]،
     * والافتراضي العفاسي (كان هو المصدر الوحيد قبل قسم أصوات الشيخ).
     */
    val quranReciter: String = com.sawan4art.noor.domain.mushaf.QuranReciters.DEFAULT_ID,
    /** خطبة الجمعة — العنوان المكتوب (alpha54) */
    val khutbahTitle: String = "",
    /** خطبة الجمعة — نصها المكتوب (alpha54) */
    val khutbahText: String = "",
    /** خطبة الجمعة — أسماء ملفات الصور المرفقة (alpha54) */
    val khutbahImages: List<String> = emptyList(),
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val THEME = stringPreferencesKey("theme_mode")
        val METHOD = stringPreferencesKey("calc_method")
        val HANAFI = booleanPreferencesKey("asr_hanafi")
        val CITY = stringPreferencesKey("city")
        val LAT = doublePreferencesKey("latitude")
        val LNG = doublePreferencesKey("longitude")
        val LOCATION_RESOLVED = booleanPreferencesKey("location_resolved")
        val ACCENT = stringPreferencesKey("accent_color")
        val PRIMARY = stringPreferencesKey("primary_color")
        val MUSHAF_PAGE = intPreferencesKey("mushaf_page")
        val MUSHAF_BOOKMARK_SURA = intPreferencesKey("mushaf_bookmark_sura")
        val MUSHAF_BOOKMARK_AYAH = intPreferencesKey("mushaf_bookmark_ayah")
        val MUSHAF_BOOKMARK_PAGE = intPreferencesKey("mushaf_bookmark_page")
        val MUSHAF_HIFZ = stringSetPreferencesKey("mushaf_hifz_set")
        // أصوات الشيخ وخطبة الجمعة (alpha54)
        val QURAN_RECITER = stringPreferencesKey("quran_reciter")
        val KHUTBAH_TITLE = stringPreferencesKey("khutbah_title")
        val KHUTBAH_TEXT = stringPreferencesKey("khutbah_text")
        val KHUTBAH_IMAGES = stringPreferencesKey("khutbah_images")
        val UPDATE_LAST_CHECK = longPreferencesKey("update_last_check_at")
        val UPDATE_NOTIFIED = stringPreferencesKey("update_notified_tag")
        val UPDATE_NOTIFIED_AT = longPreferencesKey("update_notified_at")
        val UPDATE_DISMISSED = stringPreferencesKey("update_dismissed_version")
        val HADITH_FAVORITES = stringPreferencesKey("hadith_favorites")
        // الأذان والإقامة
        val ADHAN_ENABLED = booleanPreferencesKey("adhan_enabled")
        val ADHAN_VOICE = stringPreferencesKey("adhan_voice")
        val IQAMAH_ENABLED = booleanPreferencesKey("iqamah_enabled")
        /** مفتاح النسخ القديم (فاصل عام واحد) — يُورَّث لكل الصلوات إن لم تُحدَّد */
        val IQAMAH_OFFSET = intPreferencesKey("iqamah_offset_minutes")
        /** مفاتيح الفاصل لكل صلاة على حدة (تحديث المالك 2026-11-06) */
        val GAP_FAJR = intPreferencesKey("gap_fajr_minutes")
        val GAP_DHUHR = intPreferencesKey("gap_dhuhr_minutes")
        val GAP_ASR = intPreferencesKey("gap_asr_minutes")
        val GAP_MAGHRIB = intPreferencesKey("gap_maghrib_minutes")
        val GAP_ISHA = intPreferencesKey("gap_isha_minutes")
        val IQAMAH_VOICE = stringPreferencesKey("iqamah_voice")
        val FRIDAY_DOUBLE = booleanPreferencesKey("friday_double_adhan")
        val FRIDAY_ADHAN_GAP = intPreferencesKey("friday_adhan_gap_minutes")
        val FRIDAY_IQAMAH_GAP = intPreferencesKey("friday_iqamah_gap_minutes")
        val MUTE_ENABLED = booleanPreferencesKey("adhan_mute_enabled")
        val MUTE_MINUTES = intPreferencesKey("adhan_mute_minutes")
        val MUTE_CANCEL_ON_UNLOCK = booleanPreferencesKey("adhan_mute_cancel_on_unlock")
        val CUSTOM_ADHAN_PATH = stringPreferencesKey("custom_adhan_path")
        val CUSTOM_ADHAN_NAME = stringPreferencesKey("custom_adhan_name")
        val CUSTOM_IQAMAH_PATH = stringPreferencesKey("custom_iqamah_path")
        val CUSTOM_IQAMAH_NAME = stringPreferencesKey("custom_iqamah_name")
        // طلب الصلاحيات — آخر نسخة طُلبت عندها السلسلة كاملة
        val PERMISSIONS_ASKED_VERSION = intPreferencesKey("permissions_asked_version")
    }

    /**
     * قراءة فاصل صلاة من مخزن مع وراثة المفتاح القديم العام — لو لم
     * تُحدَّد الصلاة قط فقيمة المفتاح القديم (إن وُجدت) تحكم، وإلا الافتراضي.
     */
    private fun readGap(
        p: androidx.datastore.preferences.core.Preferences,
        key: androidx.datastore.preferences.core.Preferences.Key<Int>,
        fallback: Int,
    ): Int {
        p[key]?.let { return AdhanGaps.clampGap(it) }
        p[Keys.IQAMAH_OFFSET]?.let { return AdhanGaps.clampGap(it) }
        return fallback
    }

    val settings: Flow<NoorSettingsState> = context.noorDataStore.data.map { p ->
        NoorSettingsState(
            themeMode = p[Keys.THEME]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM,
            method = CalculationMethod.fromId(p[Keys.METHOD]),
            isHanafi = p[Keys.HANAFI] ?: false,
            city = p[Keys.CITY] ?: "القاهرة، مصر",
            latitude = p[Keys.LAT] ?: 30.0444,
            longitude = p[Keys.LNG] ?: 31.2357,
            locationResolved = p[Keys.LOCATION_RESOLVED] ?: false,
            accentId = p[Keys.ACCENT] ?: "emerald",
            primaryId = p[Keys.PRIMARY] ?: "",
            mushafPage = MushafPages.clampPage(p[Keys.MUSHAF_PAGE] ?: 1),
            mushafBookmarkSura = p[Keys.MUSHAF_BOOKMARK_SURA] ?: 0,
            mushafBookmarkAyah = p[Keys.MUSHAF_BOOKMARK_AYAH] ?: 0,
            mushafBookmarkPage = MushafPages.clampPage(p[Keys.MUSHAF_BOOKMARK_PAGE] ?: 1),
            mushafHifz = p[Keys.MUSHAF_HIFZ] ?: emptySet(),
            quranReciter = p[Keys.QURAN_RECITER]
                ?: com.sawan4art.noor.domain.mushaf.QuranReciters.DEFAULT_ID,
            khutbahTitle = p[Keys.KHUTBAH_TITLE] ?: "",
            khutbahText = p[Keys.KHUTBAH_TEXT] ?: "",
            khutbahImages = p[Keys.KHUTBAH_IMAGES]
                ?.split(',')?.filter { it.isNotBlank() } ?: emptyList(),
        )
    }

    /**
     * تفضيلات الأذان والإقامة — أي تغيير يعيد جدولة منبهات الأذان.
     * القيم المقروءة تُحصر دائماً في حدود خانة الكتابة (١..١٢٠ دقيقة)
     * فلا تعود قيمة من نسخة سابقة خارج الحدود المسموحة، وفواصل الصلوات
     * لكل صلاة على حدة مع وراثة المفتاح العام القديم إن لم تُحدَّد.
     */
    val adhanPrefs: Flow<AdhanPrefs> = context.noorDataStore.data.map { p ->
        AdhanPrefs(
            enabled = p[Keys.ADHAN_ENABLED] ?: true,
            adhanVoice = p[Keys.ADHAN_VOICE] ?: "main",
            iqamahEnabled = p[Keys.IQAMAH_ENABLED] ?: false,
            gapFajrMinutes = readGap(p, Keys.GAP_FAJR, AdhanGaps.DEFAULT_FAJR_GAP_MINUTES),
            gapDhuhrMinutes = readGap(p, Keys.GAP_DHUHR, AdhanGaps.DEFAULT_PRAYER_GAP_MINUTES),
            gapAsrMinutes = readGap(p, Keys.GAP_ASR, AdhanGaps.DEFAULT_PRAYER_GAP_MINUTES),
            gapMaghribMinutes = readGap(p, Keys.GAP_MAGHRIB, AdhanGaps.DEFAULT_PRAYER_GAP_MINUTES),
            gapIshaMinutes = readGap(p, Keys.GAP_ISHA, AdhanGaps.DEFAULT_PRAYER_GAP_MINUTES),
            iqamahVoice = p[Keys.IQAMAH_VOICE] ?: "makkah",
            fridayDouble = p[Keys.FRIDAY_DOUBLE] ?: true,
            fridayAdhanGapMinutes = AdhanGaps.clampGap(
                p[Keys.FRIDAY_ADHAN_GAP] ?: AdhanGaps.DEFAULT_FRIDAY_ADHAN_GAP_MINUTES,
            ),
            fridayIqamahGapMinutes = AdhanGaps.clampGap(
                p[Keys.FRIDAY_IQAMAH_GAP] ?: AdhanGaps.DEFAULT_FRIDAY_IQAMAH_GAP_MINUTES,
            ),
            muteEnabled = p[Keys.MUTE_ENABLED] ?: false,
            muteMinutes = AdhanGaps.clampGap(p[Keys.MUTE_MINUTES] ?: 10),
            muteCancelOnUnlock = p[Keys.MUTE_CANCEL_ON_UNLOCK] ?: true,
            customAdhanPath = p[Keys.CUSTOM_ADHAN_PATH] ?: "",
            customAdhanName = p[Keys.CUSTOM_ADHAN_NAME] ?: "",
            customIqamahPath = p[Keys.CUSTOM_IQAMAH_PATH] ?: "",
            customIqamahName = p[Keys.CUSTOM_IQAMAH_NAME] ?: "",
        )
    }

    /** قراءة فورية واحدة للتفضيلات — للمجدول والمستقبلات خارج دورة الواجهة */
    suspend fun adhanPrefsOnce(): AdhanPrefs = adhanPrefs.first()

    /** قراءة فورية واحدة للإعدادات العامة — للمجدول */
    suspend fun settingsOnce(): NoorSettingsState = settings.first()

    suspend fun setAdhanEnabled(on: Boolean) {
        context.noorDataStore.edit { it[Keys.ADHAN_ENABLED] = on }
    }

    suspend fun setAdhanVoice(id: String) {
        context.noorDataStore.edit { it[Keys.ADHAN_VOICE] = id }
    }

    suspend fun setIqamahEnabled(on: Boolean) {
        context.noorDataStore.edit { it[Keys.IQAMAH_ENABLED] = on }
    }

    /** فاصل الأذان←الإقامة لصلاة بعينها — خانة كتابة تُحصر في ١..١٢٠ عند الحفظ */
    suspend fun setPrayerGapMinutes(key: PrayerTimeKey, minutes: Int) {
        context.noorDataStore.edit {
            when (key) {
                PrayerTimeKey.FAJR -> it[Keys.GAP_FAJR] = AdhanGaps.clampGap(minutes)
                PrayerTimeKey.DHUHR -> it[Keys.GAP_DHUHR] = AdhanGaps.clampGap(minutes)
                PrayerTimeKey.ASR -> it[Keys.GAP_ASR] = AdhanGaps.clampGap(minutes)
                PrayerTimeKey.MAGHRIB -> it[Keys.GAP_MAGHRIB] = AdhanGaps.clampGap(minutes)
                PrayerTimeKey.ISHA -> it[Keys.GAP_ISHA] = AdhanGaps.clampGap(minutes)
                PrayerTimeKey.SUNRISE -> Unit // الشروق بلا أذان ولا إقامة
            }
        }
    }

    suspend fun setIqamahVoice(id: String) {
        context.noorDataStore.edit { it[Keys.IQAMAH_VOICE] = id }
    }

    suspend fun setFridayDouble(on: Boolean) {
        context.noorDataStore.edit { it[Keys.FRIDAY_DOUBLE] = on }
    }

    /** فاصل الجمعة (الأذان الأول ← الثاني) — يُحصر داخل ١..١٢٠ دقيقة عند الحفظ */
    suspend fun setFridayAdhanGapMinutes(minutes: Int) {
        context.noorDataStore.edit {
            it[Keys.FRIDAY_ADHAN_GAP] = AdhanGaps.clampGap(minutes)
        }
    }

    /** فاصل جمعة (الأذان الثاني ← الإقامة) — يُحصر داخل ١..١٢٠ دقيقة عند الحفظ */
    suspend fun setFridayIqamahGapMinutes(minutes: Int) {
        context.noorDataStore.edit {
            it[Keys.FRIDAY_IQAMAH_GAP] = AdhanGaps.clampGap(minutes)
        }
    }

    suspend fun setMuteEnabled(on: Boolean) {
        context.noorDataStore.edit { it[Keys.MUTE_ENABLED] = on }
    }

    /** مُدة الكتم بعد الإقامة — خانة كتابة تُحصر في ١..١٢٠ دقيقة عند الحفظ */
    suspend fun setMuteMinutes(minutes: Int) {
        context.noorDataStore.edit { it[Keys.MUTE_MINUTES] = AdhanGaps.clampGap(minutes) }
    }

    /** إلغاء الكتم عند فتح قفل الشاشة — اختيار جانبي مع تفعيل الكتم */
    suspend fun setMuteCancelOnUnlock(on: Boolean) {
        context.noorDataStore.edit { it[Keys.MUTE_CANCEL_ON_UNLOCK] = on }
    }

    /**
     * ذاكرة طلب الصلاحيات — السلسلة تعمل أول تشغيل (ذاكرة ٠) فحسب
     * بتوجيه المالك بعد alpha52: «عدم اعاده فتح قائمه اخذ الاوذنات اذا
     * تم اخذها بالفعل مره واحده حتى بعد التحديث» — أي قيمة غير صفرية
     * تعني «أُخذت الأذنات من قبل» فلا يعاد فتح القائمة إطلاقاً.
     */
    val permissionsAskedVersion: Flow<Int> = context.noorDataStore.data.map { p ->
        p[Keys.PERMISSIONS_ASKED_VERSION] ?: 0
    }

    suspend fun permissionsAskedVersionOnce(): Int = permissionsAskedVersion.first()

    suspend fun setPermissionsAskedVersion(code: Int) {
        context.noorDataStore.edit { it[Keys.PERMISSIONS_ASKED_VERSION] = code }
    }

    /** حفظ صوت مخصص من الهاتف (الأذان أو الإقامة) — يُنسخ داخل مسار التطبيق */
    suspend fun setCustomSound(
        kind: com.sawan4art.noor.domain.adhan.AdhanSoundKind,
        path: String,
        name: String,
    ) {
        context.noorDataStore.edit {
            if (kind == com.sawan4art.noor.domain.adhan.AdhanSoundKind.ADHAN) {
                it[Keys.CUSTOM_ADHAN_PATH] = path
                it[Keys.CUSTOM_ADHAN_NAME] = name
            } else {
                it[Keys.CUSTOM_IQAMAH_PATH] = path
                it[Keys.CUSTOM_IQAMAH_NAME] = name
            }
        }
    }

    /** حذف الصوت المخصص — يعود الاختيار للأصوات المدمجة */
    suspend fun clearCustomSound(kind: com.sawan4art.noor.domain.adhan.AdhanSoundKind) {
        context.noorDataStore.edit {
            if (kind == com.sawan4art.noor.domain.adhan.AdhanSoundKind.ADHAN) {
                it.remove(Keys.CUSTOM_ADHAN_PATH)
                it.remove(Keys.CUSTOM_ADHAN_NAME)
            } else {
                it.remove(Keys.CUSTOM_IQAMAH_PATH)
                it.remove(Keys.CUSTOM_IQAMAH_NAME)
            }
        }
    }

    suspend fun setAccentId(id: String) {
        context.noorDataStore.edit { it[Keys.ACCENT] = id }
    }

    /** اللون الأساسي — نص فارغ يعني «الحالي» (يتبع لون التطبيق) */
    suspend fun setPrimaryId(id: String) {
        context.noorDataStore.edit { it[Keys.PRIMARY] = id }
    }

    /** حفظ آخر صفحة مصحف — يُقيَّد داخل ١..٦٠٤ عند الكتابة أيضاً */
    suspend fun setMushafPage(page: Int) {
        context.noorDataStore.edit { it[Keys.MUSHAF_PAGE] = MushafPages.clampPage(page) }
    }

    /** حفظ علامة مرجعية على آية — سورة صفر تعني إزالة العلامة */
    suspend fun setMushafBookmark(sura: Int, ayah: Int, page: Int) {
        context.noorDataStore.edit {
            if (sura <= 0 || ayah <= 0) {
                it[Keys.MUSHAF_BOOKMARK_SURA] = 0
                it[Keys.MUSHAF_BOOKMARK_AYAH] = 0
            } else {
                it[Keys.MUSHAF_BOOKMARK_SURA] = sura
                it[Keys.MUSHAF_BOOKMARK_AYAH] = ayah
                it[Keys.MUSHAF_BOOKMARK_PAGE] = MushafPages.clampPage(page)
            }
        }
    }

    /**
     * تعليم مُدى آيات محفوظة أو مسح التعليم — الحفظ الميسر (alpha53):
     * المجموعة تُخزَّن كسلاسل «سورة:آية»، وسورة صفر تمسح كل الحفظ.
     */
    suspend fun setMushafHifz(sura: Int, ayahs: List<Int>, memorized: Boolean) {
        context.noorDataStore.edit { prefs ->
            if (sura <= 0) {
                prefs[Keys.MUSHAF_HIFZ] = emptySet()
                return@edit
            }
            val old = prefs[Keys.MUSHAF_HIFZ] ?: emptySet()
            val updated = old.toMutableSet()
            for (ayah in ayahs) {
                val key = "$sura:$ayah"
                if (memorized) updated.add(key) else updated.remove(key)
            }
            prefs[Keys.MUSHAF_HIFZ] = updated
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.noorDataStore.edit { it[Keys.THEME] = mode.name }
    }

    /** حفظ الشيخ المختار لتلاوة المصحف — يُرجع للافتراضي لو غاب المعرّف */
    suspend fun setQuranReciter(id: String) {
        context.noorDataStore.edit {
            it[Keys.QURAN_RECITER] = id.ifBlank {
                com.sawan4art.noor.domain.mushaf.QuranReciters.DEFAULT_ID
            }
        }
    }

    /** حفظ خطبة الجمعة المكتوبة — عنوان ونص */
    suspend fun setKhutbah(title: String, text: String) {
        context.noorDataStore.edit {
            it[Keys.KHUTBAH_TITLE] = title
            it[Keys.KHUTBAH_TEXT] = text
        }
    }

    /** حفظ قائمة صور الخطبة — أسماء الملفات داخل مجلد صور الخطبة */
    suspend fun setKhutbahImages(images: List<String>) {
        context.noorDataStore.edit {
            it[Keys.KHUTBAH_IMAGES] = images.filter { name -> name.isNotBlank() }
                .joinToString(",")
        }
    }

    suspend fun setMethod(method: CalculationMethod) {
        context.noorDataStore.edit { it[Keys.METHOD] = method.id }
    }

    suspend fun setHanafi(hanafi: Boolean) {
        context.noorDataStore.edit { it[Keys.HANAFI] = hanafi }
    }

    suspend fun setLocation(city: String, latitude: Double, longitude: Double) {
        context.noorDataStore.edit {
            it[Keys.CITY] = city
            it[Keys.LAT] = latitude
            it[Keys.LNG] = longitude
            it[Keys.LOCATION_RESOLVED] = true
        }
    }

    /** ذاكرة التحديثات — نفس مخزن الإعدادات لأنها كلها تفضيلات المستخدم */
    val updatePrefs: Flow<UpdatePrefs> = context.noorDataStore.data.map { p ->
        UpdatePrefs(
            lastCheckAt = p[Keys.UPDATE_LAST_CHECK] ?: 0L,
            notifiedVersion = p[Keys.UPDATE_NOTIFIED],
            lastNotifiedAt = p[Keys.UPDATE_NOTIFIED_AT] ?: 0L,
            dismissedVersion = p[Keys.UPDATE_DISMISSED],
        )
    }

    suspend fun setUpdateChecked(atMillis: Long) {
        context.noorDataStore.edit { it[Keys.UPDATE_LAST_CHECK] = atMillis }
    }

    /**
     * تسجيل النسخة التي نُشر عنها إشعار فعلي + وقت النشر — الوقت هو
     * ما تمشي عليه سياسة التكرار (alpha37 بلاغ المالك: «عايز الاشعار
     * ده يوصل لو كتير لو المستخدم محدثش التطبيق»).
     */
    suspend fun setNotifiedVersion(tag: String) {
        context.noorDataStore.edit {
            it[Keys.UPDATE_NOTIFIED] = tag
            it[Keys.UPDATE_NOTIFIED_AT] = System.currentTimeMillis()
        }
    }

    suspend fun setDismissedVersion(versionName: String) {
        context.noorDataStore.edit { it[Keys.UPDATE_DISMISSED] = versionName }
    }

    /** إلغاء الاستبعاد — ضغط إشعار التحديث يعيد فتح «ما الجديد» دائماً (alpha37) */
    suspend fun clearDismissedVersion() {
        context.noorDataStore.edit { it.remove(Keys.UPDATE_DISMISSED) }
    }

    /**
     * معرفات الأحاديث المحفوظة (alpha38 توجيه المالك: «زر نجمه للحفظ
     * في قائمه المفضله») — سلسلة مفصولة بفواصل بترتيب الحفظ الأحدث
     * أولاً فتظهر قائمة المفضلة بأحدث ما حفظه المستخدم أعلاها.
     */
    val favoriteHadithIds: Flow<List<Long>> = context.noorDataStore.data.map { p ->
        p[Keys.HADITH_FAVORITES]?.split(',')?.mapNotNull { it.toLongOrNull() } ?: emptyList()
    }

    /** حفظ حديث في المفضلة — يُقدَّم إلى رأس القائمة وإن سبق حفظه */
    suspend fun addHadithFavorite(id: Long) {
        context.noorDataStore.edit { p ->
            val current = p[Keys.HADITH_FAVORITES]
                ?.split(',')?.mapNotNull { it.toLongOrNull() }.orEmpty()
            p[Keys.HADITH_FAVORITES] = (listOf(id) + (current - id)).joinToString(",")
        }
    }

    /** إخراج حديث من المفضلة — وتُحذف المفتاح كلياً عند آخر حديث */
    suspend fun removeHadithFavorite(id: Long) {
        context.noorDataStore.edit { p ->
            val current = p[Keys.HADITH_FAVORITES]
                ?.split(',')?.mapNotNull { it.toLongOrNull() }.orEmpty()
            val rest = current - id
            if (rest.isEmpty()) p.remove(Keys.HADITH_FAVORITES)
            else p[Keys.HADITH_FAVORITES] = rest.joinToString(",")
        }
    }
}
