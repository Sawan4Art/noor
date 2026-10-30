package com.sawan4art.noor.domain.location

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * عكس الإحداثيات إلى اسم مدينة عربي — النقل الأمين لاستراتيجية geo.ts:
 *  1) BigDataCloud reverse-geocode-client — مجاني بلا مفتاح ويدعم العربية.
 *  2) Nominatim (OpenStreetMap) — احتياط أول.
 *  3) مدن عربية مدمجة بالتقريب الجغرافي — احتياط أوفلاين أخير.
 * أي فشل يرجع «موقعي الحالي» — لا يكسر تحديد الموقع أبداً.
 */
class CityGeocoder {

    data class CityResult(val city: String, val country: String?)

    suspend fun reverseGeocode(lat: Double, lng: Double): CityResult = withContext(Dispatchers.IO) {
        viaBigDataCloud(lat, lng)
            ?: viaNominatim(lat, lng)
            ?: nearestKnownCity(lat, lng)
    }

    private fun viaBigDataCloud(lat: Double, lng: Double): CityResult? = try {
        val url = "https://api.bigdatacloud.net/data/reverse-geocode-client" +
            "?latitude=$lat&longitude=$lng&localityLanguage=ar"
        val data = fetchJson(url) ?: return null
        val city = data.optString("city")
            .ifEmpty { data.optString("locality") }
            .ifEmpty { data.optString("principalSubdivision") }
        if (city.isNotBlank()) {
            CityResult(city.trim(), data.optString("countryName").trim().ifEmpty { null })
        } else {
            null
        }
    } catch (_: Exception) {
        null
    }

    private fun viaNominatim(lat: Double, lng: Double): CityResult? = try {
        val url = "https://nominatim.openstreetmap.org/reverse" +
            "?lat=$lat&lon=$lng&format=jsonv2&accept-language=ar&zoom=10"
        val data = fetchJson(url) ?: return null
        val address = data.optJSONObject("address") ?: return null
        val city = address.optString("city")
            .ifEmpty { address.optString("town") }
            .ifEmpty { address.optString("village") }
            .ifEmpty { address.optString("county") }
            .ifEmpty { address.optString("state") }
        if (city.isNotBlank()) {
            CityResult(city.trim(), address.optString("country").trim().ifEmpty { null })
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
            val dLat = c.second - lat
            val dLng = c.third - lng
            val dist = dLat * dLat + dLng * dLng
            if (dist < bestDist) {
                bestDist = dist
                best = c
            }
        }
        // أبعد من ~2 درجة (≈220 كم) عن أقرب مدينة معروفة — اسم عام
        return if (bestDist > 4.0) CityResult("موقعي الحالي", null) else CityResult(best.first, null)
    }

    companion object {
        /** مدن عربية وعالمية مدمجة للتقريب الأوفلاين — من geo.ts */
        val KNOWN_CITIES = listOf(
            Triple("القاهرة", 30.0444, 31.2357),
            Triple("الجيزة", 30.0131, 31.2089),
            Triple("الإسكندرية", 31.2001, 29.9187),
            Triple("مكة المكرمة", 21.3891, 39.8579),
            Triple("المدينة المنورة", 24.5247, 39.5692),
            Triple("الرياض", 24.7136, 46.6753),
            Triple("جدة", 21.4858, 39.1925),
            Triple("الدمام", 26.4207, 50.0888),
            Triple("دبي", 25.2048, 55.2708),
            Triple("أبو ظبي", 24.4539, 54.3773),
            Triple("الدوحة", 25.2854, 51.531),
            Triple("الكويت", 29.3759, 47.9774),
            Triple("المنامة", 26.2285, 50.586),
            Triple("مسقط", 23.588, 58.3829),
            Triple("عمّان", 31.9539, 35.9106),
            Triple("القدس", 31.7683, 35.2137),
            Triple("غزة", 31.5017, 34.4668),
            Triple("بيروت", 33.8938, 35.5018),
            Triple("دمشق", 33.5138, 36.2765),
            Triple("بغداد", 33.3152, 44.3661),
            Triple("الخرطوم", 15.5007, 32.5599),
            Triple("طرابلس", 32.8872, 13.1913),
            Triple("تونس", 36.8065, 10.1815),
            Triple("الجزائر", 36.7538, 3.0588),
            Triple("الدار البيضاء", 33.5731, -7.5898),
            Triple("الرباط", 34.0209, -6.8416),
            Triple("نواكشوط", 18.0735, -15.9582),
            Triple("إسطنبول", 41.0082, 28.9784),
            Triple("أنقرة", 39.9334, 32.8597),
            Triple("لندن", 51.5074, -0.1278),
            Triple("باريس", 48.8566, 2.3522),
            Triple("برلين", 52.52, 13.405),
            Triple("نيويورك", 40.7128, -74.006),
            Triple("تورونتو", 43.6532, -79.3832),
            Triple("كوالالمبور", 3.139, 101.6869),
            Triple("جاكرتا", -6.2088, 106.8456),
            Triple("إسلام أباد", 33.6844, 73.0479),
            Triple("كراتشي", 24.8607, 67.0011),
        )
    }
}
