package com.sawan4art.noor;

import java.util.Calendar;

/**
 * حساب مواقيت الصلاة داخل الويدجت نيتِفياً — مرآة مطابقة لخوارزمية
 * prayerMath.ts (PrayTimes) بحساب التكرار عند موضع الشمس المقدَّر.
 *
 * السبب الجذري لإصلاح «الويدجت فارغة»: الويدجت سابقاً كانت تعتمد كلياً على
 * فتح التطبيق ليكتب البيانات في SharedPreferences — الآن تحسب المواقيت بنفسها
 * من آخر موقع محفوظ (أو القاهرة افتراضياً) فتعمل فور إضافتها بلا أي بيانات.
 */
public final class WidgetPrayerTimes {

    private WidgetPrayerTimes() {
    }

    private static final double D2R = Math.PI / 180.0;
    private static final double R2D = 180.0 / Math.PI;

    private static double sinD(double deg) {
        return Math.sin(deg * D2R);
    }

    private static double cosD(double deg) {
        return Math.cos(deg * D2R);
    }

    private static double tanD(double deg) {
        return Math.tan(deg * D2R);
    }

    private static double acosD(double x) {
        return Math.acos(x) * R2D;
    }

    private static double atan2D(double y, double x) {
        return Math.atan2(y, x) * R2D;
    }

    private static double atanD(double x) {
        return Math.atan(x) * R2D;
    }

    private static double fixAngle(double a) {
        double res = a - 360.0 * Math.floor(a / 360.0);
        if (res < 0) res += 360.0;
        return res;
    }

    private static double fixHour(double h) {
        double res = h - 24.0 * Math.floor(h / 24.0);
        if (res < 0) res += 24.0;
        return res;
    }

    private static double julianDate(int year, int month, int day) {
        if (month <= 2) {
            year -= 1;
            month += 12;
        }
        int A = (int) Math.floor(year / 100.0);
        int B = 2 - A + (int) Math.floor(A / 4.0);
        return Math.floor(365.25 * (year + 4716))
                + Math.floor(30.6001 * (month + 1))
                + day + B - 1524.5;
    }

    private static final class SunPos {
        final double declination;
        final double equationOfTime;

        SunPos(double declination, double equationOfTime) {
            this.declination = declination;
            this.equationOfTime = equationOfTime;
        }
    }

    /** موضع الشمس — خوارزمية PrayTimes مع atan2 للمطلع المستقيم (بالساعات). */
    private static SunPos sunPosition(double jd) {
        double D = jd - 2451545.0;
        double g = fixAngle(357.529 + 0.98560028 * D);
        double q = fixAngle(280.459 + 0.98564736 * D);
        double L = fixAngle(q + 1.915 * sinD(g) + 0.02 * sinD(2 * g));

        double e = 23.439 - 0.00000036 * D;
        double RA = fixHour(atan2D(cosD(e) * sinD(L), cosD(L)) / 15.0);
        double declination = Math.asin(sinD(e) * sinD(L)) * R2D;
        double equationOfTime = q / 15.0 - RA;
        return new SunPos(declination, equationOfTime);
    }

    /**
     * مواقيت اليوم بالساعات المحلية (0-24): [fajr, sunrise, dhuhr, asr, maghrib, isha]
     * طريقة مصر العامة: فجر 19.5° — عشاء 17.5° (نفس افتراضات التطبيق).
     */
    public static double[] computeHours(Calendar date, double lat, double lng,
                                        double fajrAngle, double ishaAngle, boolean isHanafi) {
        int year = date.get(Calendar.YEAR);
        int month = date.get(Calendar.MONTH) + 1;
        int day = date.get(Calendar.DAY_OF_MONTH);
        // فرق التوقيت بالساعات (+2 يعني UTC+2) — مكافئ دالة getTimezoneOffset في JS
        // (الإصدار السابق كان يستخدم getTimezoneOffset وهي دالة جافاسكربت لا توجد
        //  في java.util.Calendar — كان يكسر الترجمة ويمنع الويدجت من العمل إطلاقاً)
        double tzOffsetHours = date.getTimeZone().getOffset(date.getTimeInMillis()) / 3600000.0;

        double jdBase = julianDate(year, month, day) - lng / (15 * 24);
        double[] out = new double[6];

        // الظهيرة (العبور الشمسي) بالتكرار — نفس منهج prayerMath.ts
        double t = 12 + tzOffsetHours - lng / 15.0;
        for (int i = 0; i < 3; i++) {
            SunPos sp = sunPosition(jdBase + t / 24.0);
            t = fixHour(12 + tzOffsetHours - lng / 15.0 - sp.equationOfTime);
        }
        double midDay = t;

        // وقت زاوية شمسية تحت الأفق بالتكرار
        double fajr = timeForAngle(jdBase, lat, lng, tzOffsetHours, fajrAngle, midDay - 6, -1);
        double sunrise = timeForAngle(jdBase, lat, lng, tzOffsetHours, 0.833, midDay - 6, -1);
        double maghrib = timeForAngle(jdBase, lat, lng, tzOffsetHours, 0.833, midDay + 6, +1);
        double isha = timeForAngle(jdBase, lat, lng, tzOffsetHours, ishaAngle, midDay + 7, +1);

        // العصر بالتكرار
        double asr = midDay + 3;
        double asrFactor = isHanafi ? 2.0 : 1.0;
        for (int i = 0; i < 3; i++) {
            SunPos sp = sunPosition(jdBase + asr / 24.0);
            double noon = fixHour(12 + tzOffsetHours - lng / 15.0 - sp.equationOfTime);
            double asrAlt = -atanD(1.0 / (asrFactor + tanD(Math.abs(lat - sp.declination))));
            double diff = (1 / 15.0) * acosD(
                    (-sinD(asrAlt) - sinD(lat) * sinD(sp.declination)) / (cosD(lat) * cosD(sp.declination)));
            if (Double.isNaN(diff)) {
                asr = Double.NaN;
                break;
            }
            asr = noon + diff;
        }

        out[0] = Double.isNaN(fajr) ? midDay - 1.5 : fajr;
        out[1] = Double.isNaN(sunrise) ? midDay - 1.2 : sunrise;
        out[2] = midDay;
        out[3] = Double.isNaN(asr) ? midDay + 3 : asr;
        out[4] = Double.isNaN(maghrib) ? midDay + 6 : maghrib;
        out[5] = Double.isNaN(isha) ? maghrib + 1.5 : isha;
        return out;
    }

    private static double timeForAngle(double jdBase, double lat, double lng, double tzOffsetHours,
                                       double angle, double guess, int sign) {
        double t = guess;
        for (int i = 0; i < 3; i++) {
            SunPos sp = sunPosition(jdBase + t / 24.0);
            double noon = fixHour(12 + tzOffsetHours - lng / 15.0 - sp.equationOfTime);
            double diff = (1 / 15.0) * acosD(
                    (-sinD(angle) - sinD(lat) * sinD(sp.declination)) / (cosD(lat) * cosD(sp.declination)));
            if (Double.isNaN(diff)) return Double.NaN;
            t = noon + sign * diff;
        }
        return t;
    }

    /** تحويل الساعات المحلية إلى epoch ميلي ثانية لنفس اليوم */
    public static long hoursToEpoch(Calendar date, double hours) {
        double total = fixHour(hours);
        int h = (int) Math.floor(total);
        int m = (int) Math.floor((total - h) * 60.0);
        Calendar c = (Calendar) date.clone();
        c.set(Calendar.HOUR_OF_DAY, h);
        c.set(Calendar.MINUTE, m);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    /** تحويل الأرقام اللاتينية إلى أرقام عربية-هندية لتطابق أوقات التطبيق */
    private static String toArabicDigits(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (char ch : s.toCharArray()) {
            if (ch >= '0' && ch <= '9') {
                sb.append((char) ('٠' + (ch - '0')));
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    /** تنسيق 12 ساعة عربي بأرقام عربية: ص/م */
    public static String formatTime12(long epochMs, Calendar date) {
        Calendar c = (Calendar) date.clone();
        c.setTimeInMillis(epochMs);
        int hour = c.get(Calendar.HOUR);
        int minute = c.get(Calendar.MINUTE);
        String suffix = c.get(Calendar.AM_PM) == Calendar.AM ? "ص" : "م";
        if (hour == 0) hour = 12;
        return toArabicDigits(String.format("%d:%02d %s", hour, minute, suffix));
    }

    /** تنسيق 24 ساعة عربي بأرقام عربية — لتخصيص الويدجت (نظام الوقت) */
    public static String formatTime24(long epochMs, Calendar date) {
        Calendar c = (Calendar) date.clone();
        c.setTimeInMillis(epochMs);
        return toArabicDigits(String.format("%02d:%02d",
                c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE)));
    }
}
