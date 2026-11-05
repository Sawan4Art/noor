package com.sawan4art.noor

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import com.sawan4art.noor.data.update.UpdateManager
import com.sawan4art.noor.data.update.UpdateNotifier
import com.sawan4art.noor.data.update.UpdateWorker

/**
 * منصة التطبيق — تهيئة قناة إشعارات التحديث وجدولة الفاحص الدوري
 * ومدير التحديثات مرة واحدة لكل عملية (كلها عملية رخيصة ومعرّفة idempotent).
 *
 * (alpha49) محمّل صور المصحف الملون بالتجويد: صفحات المصحف تُحمَّل من
 * الشبكة عند أول مشاهدة وتُحفظ في ذاكرة قرص كبيرة كي يقرأ القارئ
 * بعد ذلك بلا إنترنت — الصفحات لا تُنزَّل مرتين ولا تُزال بسرعة.
 */
class NoorApplication : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        UpdateManager.init(this)
        UpdateNotifier.ensureChannel(this)
        UpdateWorker.schedule(this)
    }

    /**
     * محمّل الصور الموحد — ذاكرة قرص ٢٥٦ ميجابايت تكفي أكثر من نصف
     * المصحف كاملاً (٦٠٤ صفحة ~٤٥٠ كيلوبايت للصفحة) وتهجر الأقدم
     * تلقائياً عند الامتلاء، والتلاشي الناعم عند ظهور كل صفحة.
     */
    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("mushaf_pages"))
                    .maxSizeBytes(256L * 1024 * 1024)
                    .build()
            }
            .crossfade(180)
            .build()
}
