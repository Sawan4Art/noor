package com.sawan4art.noor.data.location

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.sawan4art.noor.alarm.AlarmScheduler
import com.sawan4art.noor.data.SettingsRepository
import com.sawan4art.noor.domain.location.CityGeocoder
import com.sawan4art.noor.domain.location.LocationGateChecker
import com.sawan4art.noor.domain.location.LocationSource
import com.sawan4art.noor.util.CrashLog
import java.util.concurrent.TimeUnit

/**
 * **التحديد التلقائي للموقع في الخلفية** — بلاغ المالك 2026-11-11:
 * «زر تحديد الموقع تلقائى اذا تم تفعيله سيتم كل مره فيها فتح الموقع
 * و يوجد اتصال انترنت في الهاتف سواء فتحت التطبيق ام لا سيتم البحث
 * عن الموقع لتحديد الاوقات بدقه بغض النظر عن المكان يعني التطبيق
 * لازم يشتغل في الخلفيه».
 *
 * فالعقد هنا: عملٌ دوريٌّ (أقل دورةٍ يسمح بها WorkManager ١٥ دقيقة)
 * بقيود الشبكة المتصلة — لا يُنفَّذ إلا وفي الهاتف اتصال — يقرأ
 * الموقع ويجلب اسمه ويحفظه ويجدول منبهات الأذان من جديد، ولو كان
 * التطبيق مغلقاً تماماً. وأي غلطةٍ لا تُسقط الدورة: فشلُ القراءة
 * يعيد المحاولة لاحقاً (retry)، والإذن المفقود يُنتهى بهدوءٍ كي لا
 * يُكرَّر العمل عبثاً حتى يعيد المالك تفعيله من الإعدادات.
 */
class AutoLocationWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext
        // البوابات أولاً — بلا إذنٍ أو بلا خدمة موقعٍ لا معنى للدورة
        val gate = LocationGateChecker.check(app)
        if (gate.blocked) return Result.success()
        return try {
            val coords = LocationSource(app).currentLocation(timeoutMs = 20_000)
                ?: return Result.retry()
            val geo = runCatching {
                CityGeocoder().reverseGeocode(coords.first, coords.second)
            }.onFailure { CrashLog.note("الموقع التلقائي: فشل جلب الاسم", it) }
                .getOrElse { CityGeocoder.CityResult() }
            val label = CityGeocoder.buildLocationLabel(
                geo.village, geo.city, geo.region, geo.country,
            )
            val repo = SettingsRepository(app)
            repo.setLocation(label, coords.first, coords.second)
            // المواقيت تُحسب من الإحداثيات — فالمنبهات تُعاد من أولها
            runCatching { AlarmScheduler.reschedule(app) }
            Result.success()
        } catch (t: Throwable) {
            CrashLog.note("الموقع التلقائي: عطل في دورة الخلفية", t)
            Result.retry()
        }
    }

    companion object {

        /** اسم العمل الدوري الفريد — الإلغاء والجدولة به */
        const val WORK_NAME = "noor_auto_location"

        /**
         * جدولة الدورة الدورية — بقيود الشبكة المتصلة بعين بلاغ
         * المالك «كل مره فيها فتح الموقع و يوجد اتصال انترنت» —
         * والسياسة KEEP فلا تُعاد الدورة كل فتحٍ للتطبيق.
         */
        fun schedule(context: Context) {
            runCatching {
                val request = PeriodicWorkRequestBuilder<AutoLocationWorker>(
                    15, TimeUnit.MINUTES,
                )
                    .setConstraints(
                        androidx.work.Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build(),
                    )
                    .build()
                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request,
                )
            }
        }

        /** إلغاء الدورة — حين يطفئ المالك المفتاح */
        fun cancel(context: Context) {
            runCatching {
                WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            }
        }
    }
}
