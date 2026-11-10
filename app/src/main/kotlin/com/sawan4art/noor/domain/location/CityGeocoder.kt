package com.sawan4art.noor.domain.location

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * عكس الإحداثيات إلى اسم مكان عربي — توجيه المالك alpha20:
 * «انا عايز اسم مكانى الفعلى يتكتب مش اسم المدينة، مثال انا في أبو زعبل —
 * أبو زعبل دي تابعة لمدينة أي الخانكة — اسم القرية أو العزبة أو الكفر أولاً
 * ثم اسم المدينة التابعة لها».
 *
 * **دمج متسابق من ثلاثة مصادر بلا مفاتيح** — الثلاثة تُسأل معاً وكل مصدر
 * يغذّي خانته الأدق، والسطر يُبنى فور اكتمال القرية والمدينة والمحافظة:
 *  1) Nominatim (OpenStreetMap) بـ zoom=14 — أدق مصدر للقرية/العزبة/الكفر
 *     (village/hamlet/isolated_dwelling/suburb...) + المحافظة (state)،
 *  2) Photon (Komoot/OSM) — أسماء الأحياء والبلدات بلا حدود استخدام صارمة،
 *  3) BigDataCloud — المدينة الأم (city) والمحافظة (principalSubdivision).
 *
 * الدمج بترتيب مصدر واحد (Nominatim ← Photon ← BigDataCloud) مع إزالة
 * التكرار المُطبَّع (أبو زعبل = ابو زعبل، مدينة بلبيس = بلبيس) — فمثلاً
 * حين يضع Photon القرية نفسها في خانة city تُسقط لصالح القرية وتمتلئ
 * الخانة من BigDataCloud بالمدينة الأم: «ابو زعبل البلد، الخانكة، القليوبية».
 *
 * أي فشل كلي يرجع أقرب مدينة معروفة ثم «موقعي الحالي» — لا يكسر التحديد
 * أبداً، والعودة المبكرة لا تنتظر مصدراً معلقاً (مهليات المقابس تحدهم).
 */
class CityGeocoder {

    data class CityResult(
        val village: String? = null,
        val city: String? = null,
        val region: String? = null,
        val country: String? = null,
    ) {
        /** اكتمل الثالوث المطلوب (قرية + مدينة + محافظة)؟ — يسمح بالعودة المبكرة */
        val isComplete: Boolean
            get() = village != null && city != null && region != null

        /** فيه أي اسم يُعرض؟ */
        val hasAny: Boolean
            get() = village != null || city != null || region != null || country != null
    }

    suspend fun reverseGeocode(lat: Double, lng: Double): CityResult {
        // نطاق منفصل بلا أب كي لا تُنتظر العودة المبكرة مصدراً معلقاً —
        // عند المهلة تُلغى المعلّقون ومكالماتهم محدودة بمهليات المقابس
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val merged = try {
            withTimeoutOrNull(MERGE_TIMEOUT_MS) {
                val ranked = listOf(
                    0 to scope.async { viaNominatim(lat, lng) },
                    1 to scope.async { viaPhoton(lat, lng) },
                    2 to scope.async { viaBigDataCloud(lat, lng) },
                )
                var best: CityResult? = null
                val received = HashMap<Int, CityResult?>()
                while (received.size < ranked.size) {
                    val (rank, result) = select {
                        for ((r, d) in ranked) {
                            if (r !in received) d.onAwait { r to it }
                        }
                    }
                    received[rank] = result
                    val m = mergeResults(received.map { it.key to it.value })
                    if (m?.isComplete == true) return@withTimeoutOrNull m
                    best = m
                }
                best
            }
        } finally {
            scope.cancel()
        }
        return merged?.takeIf { it.hasAny } ?: nearestKnownCity(lat, lng)
    }

    // ===== المصادر الثلاثة =====

    /** Nominatim بـ zoom=14 — القرية/العزبة/الكفر أولاً + المحافظة */
    private fun viaNominatim(lat: Double, lng: Double): CityResult? = try {
        val url = "https://nominatim.openstreetmap.org/reverse" +
            "?lat=$lat&lon=$lng&format=jsonv2&accept-language=ar&zoom=14"
        val data = fetchJson(url) ?: return null
        val address = data.optJSONObject("address") ?: return null
        val village = firstPresent(
            address, "village", "hamlet", "isolated_dwelling",
            "suburb", "neighbourhood", "quarter", "borough",
        )
        val city = firstPresent(address, "city", "town", "municipality")
        if (village != null || city != null) {
            CityResult(
                village = village,
                city = city,
                region = cleanGovernorate(address.optString("state").trim()),
                country = address.optString("country").trim().ifEmpty { null },
            )
        } else {
            null
        }
    } catch (_: Exception) {
        null
    }

    /** Photon (Komoot/OSM) — أسماء الأحياء والقرى بلا حدود استخدام صارمة */
    private fun viaPhoton(lat: Double, lng: Double): CityResult? = try {
        val url = "https://photon.komoot.io/reverse?lat=$lat&lon=$lng&lang=default"
        val data = fetchJson(url) ?: return null
        val features = data.optJSONArray("features") ?: return null
        val props = features.optJSONObject(0)?.optJSONObject("properties") ?: return null
        val villageTypes = setOf(
            "village", "hamlet", "isolated_dwelling", "locality",
            "neighbourhood", "suburb", "quarter", "borough", "farm",
        )
        val featureType = props.optString("osm_value")
        val village = props.optString("name").trim()
            .takeIf { it.isNotEmpty() && featureType in villageTypes }
            ?: props.optString("district").trim().takeIf { it.isNotEmpty() }
        val city = props.optString("city").trim()
            .ifEmpty { props.optString("county").trim() }
            .ifEmpty { null }
        CityResult(
            village = village,
            city = city,
            region = cleanGovernorate(props.optString("state").trim()),
            country = props.optString("country").trim().ifEmpty { null },
        )
    } catch (_: Exception) {
        null
    }

    /** BigDataCloud بلا مفتاح — المدينة الأم والمحافظة */
    private fun viaBigDataCloud(lat: Double, lng: Double): CityResult? = try {
        val url = "https://api.bigdatacloud.net/data/reverse-geocode-client" +
            "?latitude=$lat&longitude=$lng&localityLanguage=ar"
        val data = fetchJson(url) ?: return null
        val locality = data.optString("locality").trim()
        val city = data.optString("city").trim()
        // الـ locality عند BigDataCloud أدق من المدينة غالباً — قرية إن لم يكررها
        val village = locality.takeIf {
            it.isNotEmpty() && normalizeForCompare(it) != normalizeForCompare(city)
        }
        CityResult(
            village = village,
            city = city.ifEmpty { null },
            region = cleanGovernorate(data.optString("principalSubdivision").trim()),
            country = data.optString("countryName").trim().ifEmpty { null },
        )
    } catch (_: Exception) {
        null
    }

    // ===== الدمج المُرجَّح =====

    /**
     * دمج نتائج المصادر بترتيبها (rank 0 = Nominatim، 1 = Photon، 2 = BigDataCloud):
     * كل خانة تأخذ أول قيمة غير فارغة وغير مكررة مُطبَّعاً لمصدر أعلى ترجيحاً،
     * والمدينة لا تُقبل إن كانت هي القرية نفسها (فيلتر «ابو زعبل البلد»).
     */
    internal fun mergeResults(ranked: List<Pair<Int, CityResult?>>): CityResult? {
        val sources = ranked
            .filter { it.second != null }
            .sortedBy { it.first }
            .map { it.second!! }
        if (sources.isEmpty()) return null

        fun pick(slot: (CityResult) -> String?, avoid: Set<String>): String? =
            sources.firstNotNullOfOrNull { r ->
                slot(r)?.trim()?.takeIf { it.isNotEmpty() && normalizeForCompare(it) !in avoid }
            }

        val village = pick({ it.village }, emptySet())
        val villageAvoid = village?.let { setOf(normalizeForCompare(it)) } ?: emptySet()
        val city = pick({ it.city }, villageAvoid)
        val cityAvoid = villageAvoid + (city?.let { setOf(normalizeForCompare(it)) } ?: emptySet())
        val region = pick({ it.region }, cityAvoid)
        val regionAvoid = cityAvoid + (region?.let { setOf(normalizeForCompare(it)) } ?: emptySet())
        val country = pick({ it.country }, regionAvoid)
        return CityResult(village, city, region, country)
    }

    private fun firstPresent(address: JSONObject, vararg keys: String): String? =
        keys.firstNotNullOfOrNull { key ->
            address.optString(key).trim().takeIf { it.isNotEmpty() }
        }

    /**
     * **الترميز الجغرافي الأمامي** — تحويل اسم مكانٍ (من التحديد اليدوي:
     * قرية، مدينة، تقسيم، دولة) إلى إحداثياته عبر Nominatim — بلاغ
     * المالك 2026-11-11 «زر تحديد الموقع يدويا عن طريق اختيار الدوله
     * ثم المحافظه ثم المدينه ثم القريه ان وجدت»: الهرم أسماءٌ فقط،
     * والإحداثيات من الجغرافيا لا من اختلاقها — أول نتيجةٍ تُقبل بلا
     * قبولٍ عشوائي، و[manualMode] يطالب بالتطابق الصريح للاسم فلا «الأعراف
     * نفسها من مدينةٍ أخرى».
     */
    suspend fun forwardGeocode(query: String, manualMode: Boolean = true): Pair<Double, Double>? =
        withContext(Dispatchers.IO) {
            val q = query.trim()
            if (q.isEmpty()) return@withContext null
            val url = "https://nominatim.openstreetmap.org/search" +
                "?q=" + java.net.URLEncoder.encode(q, "UTF-8") +
                "&format=json&limit=5&accept-language=ar&addressdetails=1"
            val data = try {
                fetchJsonArray(url)
            } catch (_: Exception) {
                null
            } ?: return@withContext null
            val candidates = ArrayList<Pair<Double, Double>>()
            for (i in 0 until data.length()) {
                val item = data.optJSONObject(i) ?: continue
                val lat = item.optDouble("lat", Double.NaN)
                val lng = item.optDouble("lon", Double.NaN)
                if (lat.isNaN() || lng.isNaN()) continue
                val displayName = item.optString("display_name")
                // الوضع اليدوي: الاسم المختار يجب أن يظهر في نتيجة Nominatim
                // — نتحقق من أول مقطع من النص العربي (المكان الأدق)
                if (manualMode) {
                    val needle = q.split("،", ",").firstOrNull()?.trim().orEmpty()
                    if (needle.length >= 2 && !normalizeForCompare(displayName)
                            .contains(normalizeForCompare(needle))
                    ) {
                        continue
                    }
                }
                candidates.add(lat to lng)
            }
            candidates.firstOrNull()
        }

    /** مصفوفة JSON من Nominatim — [[...]] لا كائنٌ واحد */
    private fun fetchJsonArray(url: String): org.json.JSONArray? = try {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = FORWARD_CONNECT_TIMEOUT_MS
        connection.readTimeout = FORWARD_READ_TIMEOUT_MS
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("User-Agent", USER_AGENT)
        try {
            if (connection.responseCode !in 200..299) null
            else connection.inputStream.bufferedReader().use { org.json.JSONArray(it.readText()) }
        } finally {
            connection.disconnect()
        }
    } catch (_: Exception) {
        null
    }

    private fun fetchJson(url: String): JSONObject? = try {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = CONNECT_TIMEOUT_MS
        connection.readTimeout = READ_TIMEOUT_MS
        connection.setRequestProperty("Accept", "application/json")
        // سياسة Nominatim تشترط User-Agent معرّفاً — بدونه قد يُرفض الطلب
        connection.setRequestProperty("User-Agent", USER_AGENT)
        try {
            if (connection.responseCode !in 200..299) null
            else connection.inputStream.bufferedReader().use { JSONObject(it.readText()) }
        } finally {
            connection.disconnect()
        }
    } catch (_: Exception) {
        null
    }

    private fun nearestKnownCity(lat: Double, lng: Double): CityResult {
        var best = KNOWN_CITIES.first()
        var bestDist = Double.MAX_VALUE
        for (c in KNOWN_CITIES) {
            val dLat = c.lat - lat
            val dLng = c.lng - lng
            val dist = dLat * dLat + dLng * dLng
            if (dist < bestDist) {
                bestDist = dist
                best = c
            }
        }
        // أبعد من ~2 درجة (≈220 كم) عن أقرب مدينة معروفة — اسم عام
        return if (bestDist > 4.0) {
            CityResult(city = "موقعي الحالي")
        } else {
            CityResult(city = best.name, region = best.region)
        }
    }

    companion object {
        /** سقف انتظار المصادر المتسابقة قبل الرجوع لما توفر — يقتل أي لاج */
        private const val MERGE_TIMEOUT_MS = 4_000L
        private const val CONNECT_TIMEOUT_MS = 3_000
        private const val READ_TIMEOUT_MS = 3_000
        /** مهليات الترميز الأمامي — اختيارٌ يدويٌّ فالصبر أبعد قليلاً */
        private const val FORWARD_CONNECT_TIMEOUT_MS = 8_000
        private const val FORWARD_READ_TIMEOUT_MS = 12_000
        private const val USER_AGENT = "noor-app/0.5 (Android; https://github.com/Sawan4Art/noor)"

        /**
         * تنظيف اسم المحافظة للعرض — «محافظة القليوبية» تعرض «القليوبية»
         * كي يصير السطر «أبو زعبل، الخانكة، القليوبية» بلا بادئات إدارية.
         */
        fun cleanGovernorate(raw: String): String? = raw
            .removePrefix("محافظة ")
            .removePrefix("محافظة")
            .removePrefix("Muhafazat ")
            .removePrefix("Governorate of ")
            .trim()
            .ifEmpty { null }

        /**
         * تطبيع المقارنة بين أسماء الأماكن — توحيد الألفات وإسقاط بادئة
         * «مدينة» كي تتساوى «مدينة بلبيس» و«بلبيس» و«أبو زعبل» و«ابو زعبل».
         */
        internal fun normalizeForCompare(raw: String): String = raw.trim()
            .replace('أ', 'ا')
            .replace('إ', 'ا')
            .replace('آ', 'ا')
            .removePrefix("مدينة ")
            .removePrefix("مدينة")
            .lowercase()

        /**
         * سطر الموقع الجاهز للعرض — **القرية أولاً ثم المدينة ثم المحافظة**
         * (توجيه المالك alpha20)، وبلا تكرار مُطبَّع. المحافظة الغائبة تلعب
         * الدولة دورها (العقد القديم)، أما المتكررة فتُسقط ولا تعوَّض.
         */
        fun buildLocationLabel(
            village: String?,
            city: String?,
            region: String?,
            country: String?,
        ): String {
            val parts = ArrayList<String>(3)
            val seen = HashSet<String>()
            fun add(raw: String?): Boolean {
                val v = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return false
                val n = normalizeForCompare(v)
                if (n in seen) return false
                seen += n
                parts += v
                return true
            }
            add(village)
            add(city)
            val regionPresent = !region.isNullOrBlank()
            if (!add(region) && !regionPresent) add(country)
            if (parts.isEmpty()) add(country)
            return if (parts.isEmpty()) "موقعي الحالي" else parts.joinToString("، ")
        }

        /** التوافق مع الاستدعاء القديم (اسم رئيسي بلا قرية منفصلة) */
        fun buildLocationLabel(primary: String, region: String?, country: String?): String =
            buildLocationLabel(village = null, city = primary, region = region, country = country)

        /** مدن عربية وعالمية مدمجة للتقريب الأوفلاين — من geo.ts مع محافظات مصر */
        data class KnownCity(val name: String, val lat: Double, val lng: Double, val region: String?)

        val KNOWN_CITIES = listOf(
            KnownCity("القاهرة", 30.0444, 31.2357, "القاهرة"),
            KnownCity("الجيزة", 30.0131, 31.2089, "الجيزة"),
            KnownCity("الإسكندرية", 31.2001, 29.9187, "الإسكندرية"),
            KnownCity("مكة المكرمة", 21.3891, 39.8579, "منطقة مكة المكرمة"),
            KnownCity("المدينة المنورة", 24.5247, 39.5692, "منطقة المدينة المنورة"),
            KnownCity("الرياض", 24.7136, 46.6753, "منطقة الرياض"),
            KnownCity("جدة", 21.4858, 39.1925, "منطقة مكة المكرمة"),
            KnownCity("الدمام", 26.4207, 50.0888, "المنطقة الشرقية"),
            KnownCity("دبي", 25.2048, 55.2708, "دبي"),
            KnownCity("أبو ظبي", 24.4539, 54.3773, "أبو ظبي"),
            KnownCity("الدوحة", 25.2854, 51.531, "الدوحة"),
            KnownCity("الكويت", 29.3759, 47.9774, "العاصمة"),
            KnownCity("المنامة", 26.2285, 50.586, "العاصمة"),
            KnownCity("مسقط", 23.588, 58.3829, "مسقط"),
            KnownCity("عمّان", 31.9539, 35.9106, "العاصمة"),
            KnownCity("القدس", 31.7683, 35.2137, "القدس"),
            KnownCity("غزة", 31.5017, 34.4668, "غزة"),
            KnownCity("بيروت", 33.8938, 35.5018, "بيروت"),
            KnownCity("دمشق", 33.5138, 36.2765, "دمشق"),
            KnownCity("بغداد", 33.3152, 44.3661, "بغداد"),
            KnownCity("الخرطوم", 15.5007, 32.5599, "الخرطوم"),
            KnownCity("طرابلس", 32.8872, 13.1913, "طرابلس"),
            KnownCity("تونس", 36.8065, 10.1815, "تونس"),
            KnownCity("الجزائر", 36.7538, 3.0588, "الجزائر"),
            KnownCity("الدار البيضاء", 33.5731, -7.5898, "الدار البيضاء"),
            KnownCity("الرباط", 34.0209, -6.8416, "الرباط"),
            KnownCity("نواكشوط", 18.0735, -15.9582, "نواكشوط"),
            KnownCity("إسطنبول", 41.0082, 28.9784, null),
            KnownCity("أنقرة", 39.9334, 32.8597, null),
            KnownCity("لندن", 51.5074, -0.1278, null),
            KnownCity("باريس", 48.8566, 2.3522, null),
            KnownCity("برلين", 52.52, 13.405, null),
            KnownCity("نيويورك", 40.7128, -74.006, null),
            KnownCity("تورونتو", 43.6532, -79.3832, null),
            KnownCity("كوالالمبور", 3.139, 101.6869, null),
            KnownCity("جاكرتا", -6.2088, 106.8456, null),
            KnownCity("إسلام أباد", 33.6844, 73.0479, null),
            KnownCity("كراتشي", 24.8607, 67.0011, null),
        )
    }
}
