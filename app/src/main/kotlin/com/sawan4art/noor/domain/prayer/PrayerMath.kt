package com.sawan4art.noor.domain.prayer

import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.tan
import kotlin.math.roundToInt
import kotlin.math.max
import kotlin.math.min

/**
 * حسابات المواقيت الفلكية — النقل الأمين لخوارزمية PrayTimes من prayerMath.ts.
 *
 * الجذور المحفوظة في النقل (كل واحدة درس مؤلم من التطبيق السابق):
 *  1) المطلع المستقيم بـ atan2 مع fixHour بالساعات — لا atan مع fixAngle.
 *  2) موضع الشمس يُقيَّم عند الوقت المُقدَّر لكل صلاة بالتكرار (3 دورات) —
 *     لا قراءة واحدة عند منتصف الليل (كان يزحلق المساء حتى 1.5 دقيقة).
 *  3) إزاحة المنطقة الزمنية تُقرأ عند ظهيرة اليوم (لا منتصف ليله) — صحيح
 *     حتى في أيام انتقال التوقيت الصيفي.
 */
object PrayerMath {

    private const val D2R = Math.PI / 180.0
    private const val R2D = 180.0 / Math.PI

    private fun sinD(deg: Double): Double = sin(deg * D2R)
    private fun cosD(deg: Double): Double = cos(deg * D2R)
    private fun tanD(deg: Double): Double = tan(deg * D2R)
    private fun asinD(x: Double): Double = asin(x) * R2D
    private fun acosD(x: Double): Double = acos(x) * R2D
    private fun atan2D(y: Double, x: Double): Double = atan2(y, x) * R2D
    private fun atanD(x: Double): Double = atan(x) * R2D

    private fun fixAngle(a: Double): Double {
        var res = a - 360.0 * floor(a / 360.0)
        if (res < 0) res += 360.0
        return res
    }

    private fun fixHour(h: Double): Double {
        var res = h - 24.0 * floor(h / 24.0)
        if (res < 0) res += 24.0
        return res
    }

    fun julianDate(year: Int, month: Int, day: Int): Double {
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = floor(y / 100.0)
        val b = 2.0 - a + floor(a / 4.0)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5
    }

    data class SunPosition(val declination: Double, val equationOfTime: Double)

    fun sunPosition(jd: Double): SunPosition {
        val d = jd - 2451545.0
        val g = fixAngle(357.529 + 0.98560028 * d)
        val q = fixAngle(280.459 + 0.98564736 * d)
        val l = fixAngle(q + 1.915 * sinD(g) + 0.02 * sinD(2 * g))

        val e = 23.439 - 0.00000036 * d
        val ra = fixHour(atan2D(cosD(e) * sinD(l), cosD(l)) / 15.0)
        val declination = asinD(sinD(e) * sinD(l))
        val equationOfTime = q / 15.0 - ra
        return SunPosition(declination, equationOfTime)
    }

    fun compute(
        date: LocalDate,
        lat: Double,
        lng: Double,
        zone: ZoneId,
        method: CalculationMethod,
        isHanafi: Boolean,
        adjustmentsMinutes: Map<PrayerTimeKey, Int> = emptyMap(),
    ): PrayerTimes {
        // إزاحة المنطقة عند ظهيرة اليوم — تطابق -getTimezoneOffset()/60 في المرجع:
        // totalSeconds في java.time موجب شرق غرينتش (مثل +10800 للقاهرة) مباشرة،
        // بينما getTimezoneOffset في JS سالب شرقاً — لذلك هنا بلا سالب.
        val zoneOffsetHours =
            date.atTime(12, 0).atZone(zone).offset.totalSeconds / 3600.0

        val jdBase = julianDate(date.year, date.monthValue, date.dayOfMonth) - lng / (15.0 * 24)

        fun sunAt(localHours: Double): SunPosition = sunPosition(jdBase + localHours / 24.0)

        fun noonAt(localHours: Double): Double {
            val eqt = sunAt(localHours).equationOfTime
            return fixHour(12.0 + zoneOffsetHours - lng / 15.0 - eqt)
        }

        fun timeForAngle(angle: Double, guess: Double, sign: Int): Double {
            var t = guess
            repeat(3) {
                val d = sunAt(t).declination
                val noon = noonAt(t)
                val diff =
                    (1.0 / 15.0) * acosD((-sinD(angle) - sinD(lat) * sinD(d)) / (cosD(lat) * cosD(d)))
                if (diff.isNaN()) return Double.NaN
                t = noon + sign * diff
            }
            return t
        }

        fun asrTime(guess: Double): Double {
            var t = guess
            repeat(3) {
                val d = sunAt(t).declination
                val noon = noonAt(t)
                val asrFactor = if (isHanafi) 2.0 else 1.0
                val asrAlt = -atanD(1.0 / (asrFactor + tanD(abs(lat - d))))
                val diff =
                    (1.0 / 15.0) * acosD((-sinD(asrAlt) - sinD(lat) * sinD(d)) / (cosD(lat) * cosD(d)))
                if (diff.isNaN()) return Double.NaN
                t = noon + diff
            }
            return t
        }

        var midDay = 12.0 + zoneOffsetHours - lng / 15.0
        repeat(3) { midDay = noonAt(midDay) }

        fun safe(v: Double, fallback: Double): Double = if (v.isNaN()) fallback else v

        val fajr = safe(timeForAngle(method.fajrAngle, midDay - 6, -1), midDay - 1.5)
        val sunrise = safe(timeForAngle(0.833, midDay - 6, -1), midDay - 1.2)
        val asr = safe(asrTime(midDay + 3), midDay + 3)
        val maghrib = safe(timeForAngle(0.833, midDay + 6, +1), midDay + 6)
        val isha = method.ishaIntervalMinutes?.let { maghrib + it / 60.0 }
            ?: safe(timeForAngle(method.ishaAngle ?: 17.5, midDay + 7, +1), maghrib + 1.5)

        fun toTime(hours: Double, adjMinutes: Int): ZonedDateTime {
            val total = fixHour(hours + adjMinutes / 60.0)
            val h = floor(total).toInt()
            val m = floor((total - h) * 60).toInt()
            val s = floor(((total - h) * 60 - m) * 60).toInt()
            return ZonedDateTime.of(date, LocalTime.of(h, m, s), zone)
        }

        fun adj(key: PrayerTimeKey): Int = adjustmentsMinutes[key] ?: 0

        return PrayerTimes(
            mapOf(
                PrayerTimeKey.FAJR to toTime(fajr, adj(PrayerTimeKey.FAJR)),
                PrayerTimeKey.SUNRISE to toTime(sunrise, adj(PrayerTimeKey.SUNRISE)),
                PrayerTimeKey.DHUHR to toTime(midDay, adj(PrayerTimeKey.DHUHR)),
                PrayerTimeKey.ASR to toTime(asr, adj(PrayerTimeKey.ASR)),
                PrayerTimeKey.MAGHRIB to toTime(maghrib, adj(PrayerTimeKey.MAGHRIB)),
                PrayerTimeKey.ISHA to toTime(isha, adj(PrayerTimeKey.ISHA)),
            )
        )
    }

    val PRAYER_NAMES_AR: Map<PrayerTimeKey, String> = mapOf(
        PrayerTimeKey.FAJR to "الفجر",
        PrayerTimeKey.SUNRISE to "الشروق",
        PrayerTimeKey.DHUHR to "الظهر",
        PrayerTimeKey.ASR to "العصر",
        PrayerTimeKey.MAGHRIB to "المغرب",
        PrayerTimeKey.ISHA to "العشاء",
    )

    /** الصلاة القادمة من الآن — بنفس منطق getNextPrayer في المرجع */
    fun nextPrayer(times: PrayerTimes, now: ZonedDateTime): NextPrayer {
        val order = PrayerTimeKey.entries
        var nextKey = PrayerTimeKey.FAJR
        var target = times[PrayerTimeKey.FAJR]
        var previous = times[PrayerTimeKey.ISHA]

        var found = false
        for ((i, key) in order.withIndex()) {
            val t = times[key]
            if (t.isAfter(now)) {
                nextKey = key
                target = t
                previous = if (i > 0) times[order[i - 1]] else times[PrayerTimeKey.ISHA]
                found = true
                break
            }
        }

        if (!found) {
            nextKey = PrayerTimeKey.FAJR
            target = times[PrayerTimeKey.FAJR].plusDays(1)
            previous = times[PrayerTimeKey.ISHA]
        }

        val remainingSeconds = max(0, Duration.between(now, target).seconds).toInt()
        val windowSeconds = max(1, Duration.between(previous, target).seconds).toInt()
        val elapsedSeconds = max(0, Duration.between(previous, now).seconds).toInt()
        val progressPercent =
            min(100, max(0, (elapsedSeconds * 100.0 / windowSeconds).roundToInt()))

        return NextPrayer(
            key = nextKey,
            arabicName = PRAYER_NAMES_AR.getValue(nextKey),
            target = target,
            previous = previous,
            remainingSeconds = remainingSeconds,
            progressPercent = progressPercent,
        )
    }

    /** اتجاه القبلة بالدرجات من الشمال — الكعبة: 21.4225241N ، 39.8261818E */
    fun calculateQibla(lat: Double, lng: Double): Double {
        val kaabaLat = 21.4225241 * D2R
        val kaabaLng = 39.8261818 * D2R
        val phi = lat * D2R
        val lambda = lng * D2R

        val y = sin(kaabaLng - lambda)
        val x = cos(phi) * tan(kaabaLat) - sin(phi) * cos(kaabaLng - lambda)
        return fixAngle(atan2(y, x) * R2D)
    }

    /** المسار العظيم إلى الكعبة بالكيلومترات */
    fun calculateDistanceToKaaba(lat: Double, lng: Double): Int {
        val r = 6371.0
        val kaabaLat = 21.4225241 * D2R
        val kaabaLng = 39.8261818 * D2R
        val phi1 = lat * D2R
        val phi2 = kaabaLat
        val deltaPhi = (21.4225241 - lat) * D2R
        val deltaLambda = (39.8261818 - lng) * D2R

        val a = sin(deltaPhi / 2) * sin(deltaPhi / 2) +
            cos(phi1) * cos(phi2) * sin(deltaLambda / 2) * sin(deltaLambda / 2)
        val c = 2 * atan2(sqrtK(a), sqrtK(1 - a))
        return (r * c).roundToInt()
    }

    private fun sqrtK(x: Double): Double = kotlin.math.sqrt(x)
}
