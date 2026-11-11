package com.sawan4art.noor.data.update

import com.sawan4art.noor.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

/**
 * فاحص التحديثات — يسأل واجهة GitHub Releases (الريبو عام فلا توكن مطلوب،
 * والفحص مقيّد بفواصل زمنية في المدير كي لا نستهلك حد الـ 60 طلباً/ساعة لكل IP).
 *
 * القناة: نسخة ألفا مثبتة تتابع ألفا ومستقرة معاً، والنسخة المستقرة
 * تتجاهل prerelease — نفس قاعدة الـ workflow («ألفا = pre-release حتى لا
 * يلتقطها بنر التحديث في النسخة المستقرة»).
 */
object UpdateChecker {

    private const val RELEASES_URL =
        "https://api.github.com/repos/Sawan4Art/noor/releases?per_page=10"
    private const val CONNECT_TIMEOUT_MS = 12_000
    private const val READ_TIMEOUT_MS = 12_000

    /** قناة النسخة الحالية — ألفا إن كان اسمها يحوي alpha */
    fun isAlphaChannel(versionName: String): Boolean =
        versionName.contains("alpha", ignoreCase = true)

    /**
     * اختيار التحديث من قائمة إصدارات مرتبة (الأحدث أولاً كترتيب الـ API):
     * أول إصدار أحدث من الحالي ويحمل ملف APK (تُتخطى المسودات بلا APK والوسوم الغريبة).
     */
    fun pickUpdate(
        releases: List<ReleaseSummary>,
        currentVersion: String,
        allowPrerelease: Boolean,
    ): UpdateInfo? = releases
        .filter { it.apkUrl != null }
        .filter { allowPrerelease || !it.isPrerelease }
        .filter { NoorVersion.isNewer(it.tag, currentVersion) }
        .mapNotNull { summary ->
            NoorVersion.parse(summary.tag)?.let { version ->
                summary to version
            }
        }
        .maxByOrNull { it.second }
        ?.let { (summary, _) ->
            UpdateInfo(
                tag = summary.tag,
                versionName = summary.tag.removePrefix("v"),
                title = summary.title.ifBlank { summary.tag },
                body = summary.body,
                apkName = summary.apkName!!,
                apkUrl = summary.apkUrl!!,
                apkSizeBytes = summary.apkSizeBytes,
                releaseUrl = summary.releaseUrl,
                publishedAt = summary.publishedAt,
                channel = summary.channel,
            )
        }

    /**
     * **المرشّح المُشعَر به حسب قنوات الإشعار** — بلاغ المالك v1.71:
     * «يمكن للمستخدم تفعيل اشعارات النسخ المستقره فقط او اي نسخ اخرى»
     * — أحدث إصدارٍ أحدث من الحالي ضمن القنوات المسموح إشعارُها:
     * المستقرة فقط (الافتراضي) أو كل النسخ، والنكهة (ألفا/مستقرة) كما
     * كانت تختار أصل الـ APK لا قناة الإشعار.
     */
    fun pickCandidateForNotify(
        releases: List<ReleaseSummary>,
        currentVersion: String,
        notifyAllChannels: Boolean,
    ): UpdateInfo? = releases
        .filter { it.apkUrl != null }
        .filter { notifyAllChannels || it.channel == UpdateChannel.STABLE }
        .filter { NoorVersion.isNewer(it.tag, currentVersion) }
        .mapNotNull { summary ->
            NoorVersion.parse(summary.tag)?.let { version ->
                summary to version
            }
        }
        .maxByOrNull { it.second }
        ?.let { (summary, _) ->
            UpdateInfo(
                tag = summary.tag,
                versionName = summary.tag.removePrefix("v"),
                title = summary.title.ifBlank { summary.tag },
                body = summary.body,
                apkName = summary.apkName!!,
                apkUrl = summary.apkUrl!!,
                apkSizeBytes = summary.apkSizeBytes,
                releaseUrl = summary.releaseUrl,
                publishedAt = summary.publishedAt,
                channel = summary.channel,
            )
        }

    /**
     * **أحدث نسخة لكل قناةٍ** — بلاغ المالك v1.71: «النسخه المستقره و
     * البيتا و الاصدار التجريبي» قابلة للتنزيل من قسم التحديثات —
     * فالخريطة تُرجع أحدث إصدارٍ لكل قناةٍ من الثلاث (القناة الفارغة
     * تعني لا إصدارٍ منشورٍ لها بعد).
     */
    fun latestPerChannel(releases: List<ReleaseSummary>): Map<UpdateChannel, UpdateInfo?> {
        val best = mutableMapOf<UpdateChannel, Pair<ReleaseSummary, NoorVersion>>()
        for (summary in releases) {
            if (summary.apkUrl == null) continue
            val version = NoorVersion.parse(summary.tag) ?: continue
            val current = best[summary.channel]
            if (current == null || version > current.second) {
                best[summary.channel] = summary to version
            }
        }
        return UpdateChannel.entries.associateWith { entry ->
            best[entry]?.let { (summary, _) ->
                UpdateInfo(
                    tag = summary.tag,
                    versionName = summary.tag.removePrefix("v"),
                    title = summary.title.ifBlank { summary.tag },
                    body = summary.body,
                    apkName = summary.apkName!!,
                    apkUrl = summary.apkUrl!!,
                    apkSizeBytes = summary.apkSizeBytes,
                    releaseUrl = summary.releaseUrl,
                    publishedAt = summary.publishedAt,
                    channel = summary.channel,
                )
            }
        }
    }

    /**
     * فحص الشبكة الفعلي — جسم الـ JSON يُحوَّل إلى ملخصات ثم يُختار التحديث.
     * أي خطأ شبكة/تحليل يُمرَّر لمن يناديه (المدير يحوّله حالة Failed).
     */
    suspend fun check(currentVersion: String): UpdateInfo? = withContext(Dispatchers.IO) {
        val json = fetchReleasesJson()
        // ملف الـ APK يُختار بذوق الحزمة المثبتة: ألفا (.alpha) تأخذ أصل
        // ألفا، والمستقرة تأخذ أصل المستقرة — الإصدارات من 1.70 تحمل
        // ملفين معاً (بلاغ المالك 2026-11-11: إزالة كلمة ألفا من الاسم
        // مع بقاء نسخته المثبتة) — والإصدارات القديمة بملفٍ واحدٍ تسير
        // بأمانٍ بأول أصلٍ إن لم يوجد أصلاً بطابع النكهة
        val releases = parseReleases(
            json,
            preferAlphaAsset = BuildConfig.APPLICATION_ID.endsWith(".alpha"),
        )
        pickUpdate(releases, currentVersion, allowPrerelease = isAlphaChannel(currentVersion))
    }

    /**
     * فحص الشبكة بقنواته الثلاث (بلاغ المالك v1.71) — جسمٌ واحدٌ يُجلب
     * ثم يُبنى منه: أحدث نسخةٍ لكل قناةٍ + ملخصات القائمة كاملةً لمن
     * يريد اختيار المرشّح بقواعد الإشعار.
     */
    suspend fun checkChannels(currentVersion: String): ChannelsResult =
        withContext(Dispatchers.IO) {
            val json = fetchReleasesJson()
            val releases = parseReleases(
                json,
                preferAlphaAsset = BuildConfig.APPLICATION_ID.endsWith(".alpha"),
            )
            ChannelsResult(
                perChannel = latestPerChannel(releases),
                releases = releases,
            )
        }

    /** نتيجة فحص القنوات — أحدث نسخةٍ لكل قناةٍ مع القائمة الخام */
    data class ChannelsResult(
        val perChannel: Map<UpdateChannel, UpdateInfo?>,
        val releases: List<ReleaseSummary>,
    )

    private fun fetchReleasesJson(): String {
        val connection = URL(RELEASES_URL).openConnection() as HttpURLConnection
        return try {
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            // GitHub يرفض الطلبات بلا User-Agent — اسم التطبيق + نسخته
            connection.setRequestProperty("User-Agent", "noor-almuslim/${BuildConfig.VERSION_NAME}")
            val code = connection.responseCode
            if (code != HttpURLConnection.HTTP_OK) {
                throw UpdateException("استجابة غير متوقعة من GitHub: $code")
            }
            connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    /** تحليل مصفوفة إصدارات GitHub — تسامح: عنصر تالف يُتخطى ولا يُسقط البقية.
     *  كل أصول الـ APK تُجمع وتُختار أصول النكهة المطلوبة إن وُجدت */
    internal fun parseReleases(
        json: String,
        preferAlphaAsset: Boolean = false,
    ): List<ReleaseSummary> {
        val array = JSONArray(json)
        val summaries = mutableListOf<ReleaseSummary>()
        for (i in 0 until array.length()) {
            runCatching {
                val release = array.getJSONObject(i)
                if (release.optBoolean("draft", false)) return@runCatching
                var apkName: String? = null
                var apkUrl: String? = null
                var apkSize = 0L
                var flavorApkName: String? = null
                var flavorApkUrl: String? = null
                var flavorApkSize = 0L
                val assets = release.optJSONArray("assets") ?: JSONArray()
                for (j in 0 until assets.length()) {
                    val asset = assets.getJSONObject(j)
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        if (apkName == null) {
                            // أول أصلٍ كخطةٍ احتياطيةٍ للإصدارات القديمة
                            apkName = name
                            apkUrl = asset.optString("browser_download_url", "")
                            apkSize = asset.optLong("size", 0L)
                        }
                        val isAlphaAsset = name.contains("alpha", ignoreCase = true)
                        if ((preferAlphaAsset && isAlphaAsset) ||
                            (!preferAlphaAsset && !isAlphaAsset)
                        ) {
                            flavorApkName = name
                            flavorApkUrl = asset.optString("browser_download_url", "")
                            flavorApkSize = asset.optLong("size", 0L)
                            break
                        }
                    }
                }
                summaries.add(
                    ReleaseSummary(
                        tag = release.optString("tag_name", ""),
                        title = release.optString("name", ""),
                        body = release.optString("body", ""),
                        isPrerelease = release.optBoolean("prerelease", false),
                        releaseUrl = release.optString("html_url", ""),
                        publishedAt = release.optString("published_at", ""),
                        apkName = flavorApkName ?: apkName,
                        apkUrl = flavorApkUrl ?: apkUrl,
                        apkSizeBytes = if (flavorApkName != null) flavorApkSize else apkSize,
                        // القناة من الوسم نفسه (بلاغ المالك v1.71): بلا مرحلةٍ
                        // مستقرة، ببيتا بيتا، بألفا تجريبية
                        channel = UpdateChannel.ofTag(release.optString("tag_name", "")),
                    ),
                )
            }
        }
        return summaries
    }
}

/** خطأ فاحص/منزّل التحديث — رسالته عربية جاهزة للعرض */
class UpdateException(message: String) : Exception(message)
