package com.sawan4art.noor.data.update

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/**
 * فاحص الخلفية الدوري — كل 12 ساعة مع اتصال شبكة، يكتشف النسخ الجديدة
 * ويُظهر الإشعار حتى والتطبيق مغلق (طلب المالك: إشعار مع كل تحديث).
 */
class UpdateWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = try {
        UpdateManager.backgroundCheck()
        Result.success()
    } catch (_: Exception) {
        // فشل شبكة عابر — نعيد لاحقاً بسياسة الرجوع الافتراضية
        Result.retry()
    }

    companion object {
        private const val WORK_NAME = "noor_update_check"
        private const val PERIOD_HOURS = 12L

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<UpdateWorker>(PERIOD_HOURS, TimeUnit.HOURS)
                .setConstraints(
                    Constraints.Builder()
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
}
