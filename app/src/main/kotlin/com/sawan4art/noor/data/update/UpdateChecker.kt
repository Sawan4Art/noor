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
            )
        }

    /**
     * فحص الشبكة الفعلي — جسم الـ JSON يُحوَّل إلى ملخصات ثم يُختار التحديث.
     * أي خطأ شبكة/تحليل يُمرَّر لمن يناديه (المدير يحوّله حالة Failed).
     */
    suspend fun check(currentVersion: String): UpdateInfo? = withContext(Dispatchers.IO) {
        val json = fetchReleasesJson()
        val releases = parseReleases(json)
        pickUpdate(releases, currentVersion, allowPrerelease = isAlphaChannel(currentVersion))
    }

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

    /** تحليل مصفوفة إصدارات GitHub — تسامح: عنصر تالف يُتخطى ولا يُسقط البقية */
    internal fun parseReleases(json: String): List<ReleaseSummary> {
        val array = JSONArray(json)
        val summaries = mutableListOf<ReleaseSummary>()
        for (i in 0 until array.length()) {
            runCatching {
                val release = array.getJSONObject(i)
                if (release.optBoolean("draft", false)) return@runCatching
                var apkName: String? = null
                var apkUrl: String? = null
                var apkSize = 0L
                val assets = release.optJSONArray("assets") ?: JSONArray()
                for (j in 0 until assets.length()) {
                    val asset = assets.getJSONObject(j)
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        apkName = name
                        apkUrl = asset.optString("browser_download_url", "")
                        apkSize = asset.optLong("size", 0L)
                        break
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
                        apkName = apkName,
                        apkUrl = apkUrl,
                        apkSizeBytes = apkSize,
                    ),
                )
            }
        }
        return summaries
    }
}

/** خطأ فاحص/منزّل التحديث — رسالته عربية جاهزة للعرض */
class UpdateException(message: String) : Exception(message)
