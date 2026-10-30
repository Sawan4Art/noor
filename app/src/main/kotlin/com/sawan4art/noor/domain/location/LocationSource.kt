package com.sawan4art.noor.domain.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * مصدر الموقع عبر LocationManager — بلا اعتماديات خارجية (لا خدمات جوجل).
 * المسار: آخر موقع معروف إن كان حديثاً → وإلا قراءة حديثة بعُقد مهلة.
 * استدعاء الأذونات مسؤولية الشاشة (درس v0.4.2: حوار مستقل لكل إذن بالتتابع).
 */
class LocationSource(private val context: Context) {

    @SuppressLint("MissingPermission")
    suspend fun currentLocation(timeoutMs: Long = 15_000): Pair<Double, Double>? =
        withContext(Dispatchers.IO) {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                ?: return@withContext null

            val enabledProviders = listOf(
                LocationManager.GPS_PROVIDER,
                LocationManager.NETWORK_PROVIDER,
                LocationManager.PASSIVE_PROVIDER,
            ).filter { runCatching { lm.isProviderEnabled(it) }.getOrDefault(false) }

            if (enabledProviders.isEmpty()) return@withContext null

            // 1) آخر موقع معروف — إن كان حديثاً يكفي دون انتظار
            val lastKnown: Location? = enabledProviders
                .mapNotNull { p -> runCatching { lm.getLastKnownLocation(p) }.getOrNull() }
                .maxByOrNull { it.time }
            val now = System.currentTimeMillis()
            if (lastKnown != null && now - lastKnown.time < LAST_KNOWN_MAX_AGE_MS) {
                return@withContext lastKnown.latitude to lastKnown.longitude
            }

            // 2) قراءة حديثة بعُقد مهلة — أول موقع يصل يفوز
            val fresh = freshLocation(lm, enabledProviders, timeoutMs)
            val best = fresh ?: lastKnown ?: return@withContext null
            best.latitude to best.longitude
        }

    @SuppressLint("MissingPermission")
    private suspend fun freshLocation(
        lm: LocationManager,
        providers: List<String>,
        timeoutMs: Long,
    ): Location? = suspendCancellableCoroutine { cont ->
        val mainExecutor = ContextCompat.getMainExecutor(context)

        val listener = object : android.location.LocationListener {
            private var done = false
            private fun deliver(location: Location) {
                if (!done) {
                    done = true
                    lm.removeUpdates(this)
                    if (cont.isActive) cont.resume(location)
                }
            }

            override fun onLocationChanged(location: Location) = deliver(location)

            // استدعاءات إلزامية على أجهزة API < 30 — تجاهل آمن
            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
            override fun onProviderEnabled(provider: String) = Unit
            override fun onProviderDisabled(provider: String) = Unit
        }

        try {
            providers.forEach { p ->
                if (Build.VERSION.SDK_INT >= 30) {
                    lm.getCurrentLocation(p, null, mainExecutor) { location ->
                        listener.onLocationChanged(location)
                    }
                } else {
                    @Suppress("DEPRECATION")
                    lm.requestSingleUpdate(p, listener, android.os.Looper.getMainLooper())
                }
            }
        } catch (_: Exception) {
            // مزوّد رفض فجأة — نعتمد ما يصل من البقية أو آخر موقع معروف
        }

        val timer = object : CountDownTimer(timeoutMs, timeoutMs) {
            override fun onTick(millisUntilFinished: Long) = Unit
            override fun onFinish() {
                lm.removeUpdates(listener)
                if (cont.isActive) cont.resume(null)
            }
        }
        timer.start()
        cont.invokeOnCancellation {
            lm.removeUpdates(listener)
            timer.cancel()
        }
    }

    private companion object {
        /** آخر موقع أحدث من 5 دقائق يُقبل فوراً دون انتظار قراءة جديدة */
        const val LAST_KNOWN_MAX_AGE_MS = 5 * 60 * 1000L
    }
}
