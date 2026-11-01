package com.sawan4art.noor.domain.location

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * عكس الإحداثيات إلى اسم مكان عربي — توجيه المالك alpha19:
 * «الموقع بيجيب اسم المدينة بس انا عايزه يجيب اسم القرية إن توفر واسم المحافظة».
 *
 * الناتج = [primary] (القرية أولاً ثم المدينة) + [region] (المحافظة).
 *  1) Nominatim (OpenStreetMap) — غني بعناوين القرى العربية:
 *     village/town → القرية، state → المحافظة، county → المركز احتياطاً.
 *  2) BigDataCloud — مجاني بلا مفتاح: city/locality → الاسم،
 *     principalSubdivision → المحافظة.
 *  3) مدن عربية مدمجة بالتقريب الجغرافي — احتياط أوفلاين أخير.
 * أي فشل يرجع «موقعي الحالي» — لا يكسر تحديد الموقع أبداً.
 */
class CityGeocoder {

    data class CityResult(val primary: String, val region: String?, val country: String?)

    suspend fun reverseGeocode(lat: Double, lng: Double): CityResult = withContext(Dispatchers.IO) {
        viaNominatim(lat, lng)
            ?: viaBigDataCloud(lat, lng)
            ?: nearestKnownCity(lat, lng)
    }

    private fun viaNominatim(lat: Double, lng: Double): CityResult? = try {
        val url = "https://nominatim.openstreetmap.org/reverse" +
            "?lat=$lat&lon=$lng&format=jsonv2&accept-language=ar&zoom=10"
        val data = fetchJson(url) ?: return null
        val address = data.optJSONObject("address") ?: return null
        // القرية أولاً (توجيه المالك) — ثم البلدة ثم المدينة ثم المركز
        val primary = address.optString("village")
            .ifEmpty { address.optString("town") }
            .ifEmpty { address.optString("city") }
            .ifEmpty { address.optString("municipality") }
            .ifEmpty { address.optString("county") }
            .trim()
        if (primary.isNotBlank()) {
            CityResult(
                primary = primary,
                region = cleanGovernorate(address.optString("state").trim()),
                country = address.optString("country").trim().ifEmpty { null },
            )
        } else {
            null
        }
    } catch (_: Exception) {
        null
    }

    private fun viaBigDataCloud(lat: Double, lng: Double): CityResult? = try {
        val url = "https://api.bigdatacloud.net/data/reverse-geocode-client" +
            "?latitude=$lat&longitude=$lng&localityLanguage=ar"
        val data = fetchJson(url) ?: return null
        val primary = data.optString("locality")
            .ifEmpty { data.optString("city") }
            .trim()
        if (primary.isNotBlank()) {
            CityResult(
                primary = primary,
                region = cleanGovernorate(
                    data.optString("principalSubdivision").trim(),
                ),
                country = data.optString("countryName").trim().ifEmpty { null },
            )
        } else {
            null
        }
    } catch (_: Exception) {
        null
    }

    private fun fetchJson(url: String): JSONObject? = try {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 9_000
        connection.readTimeout = 9_000
        connection.setRequestProperty("Accept", "application/json")
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
            CityResult("موقعي الحالي", null, null)
        } else {
            CityResult(best.name, best.region, null)
        }
    }

    companion object {
        /**
         * تنظيف اسم المحافظة للعرض — «محافظة القليوبية» تعرض «القليوبية»
         * كي يصير السطر «الخانكة، القليوبية» لا «الخانكة، محافظة القليوبية».
         */
        fun cleanGovernorate(raw: String): String? = raw
            .removePrefix("محافظة ")
            .removePrefix("محافظة")
            .removePrefix("Muhafazat ")
            .removePrefix("Governorate of ")
            .trim()
            .ifEmpty { null }

        /**
         * سطر الموقع الجاهز للعرض — المحافظة أولاً وإلا الدولة،
         * والاسم الأعلى (قرية/مدينة) لا يُكرر إن طابقه.
         */
        fun buildLocationLabel(primary: String, region: String?, country: String?): String {
            val r = region?.trim()?.takeIf { it.isNotEmpty() }
            val c = country?.trim()?.takeIf { it.isNotEmpty() }
            val tail = when {
                r != null && r != primary -> r
                r == null && c != null && c != primary -> c
                else -> null
            }
            return if (tail != null) "$primary، $tail" else primary
        }

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
            KnownCity("إسطنبول", 41.0082, 28.9784, "إسطنبول"),
            KnownCity("أنقرة", 39.9334, 32.8597, "أنقرة"),
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
