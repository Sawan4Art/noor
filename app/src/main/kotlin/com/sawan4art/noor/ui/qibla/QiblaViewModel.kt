package com.sawan4art.noor.ui.qibla

import android.app.Application
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.lifecycle.AndroidViewModel
import com.sawan4art.noor.domain.qibla.QiblaMath
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * حالة بوصلة القبلة:
 * - [heading] اتجاه الجهاز بالدرجات (0 = شمال) منعَّم دائرياً.
 * - [sensorAvailable] هل الجهاز يملك بوصلة حقيقية؟ عدم توفرها يفتح
 *   الوضع اليدوي (تدوير القرص بالإصبع).
 * - [started] المستشعر مسجَّل الآن (الشاشة ظاهرة).
 */
data class QiblaUiState(
    val heading: Float = 0f,
    val sensorAvailable: Boolean = true,
    val started: Boolean = false,
)

/**
 * مسؤول بوصلة القبلة — يقرأ اتجاه الجهاز من مستشعر **متجه الدوران**
 * (أدق وأخف) ويتراجع لمزدوجة التسارع + المغناطيسية على الأجهزة القديمة،
 * وكل قراءة تُناعَم دائرياً عبر QiblaMath.smoothHeading كي يتحرك القرص
 * بنعومة لا ارتجالاً. بلا أي إذن — البوصلة مستشعر حرّ.
 *
 * التسجيل والإلغاء مرتبطان بظهور الشاشة (start/stop من DisposableEffect)
 * كي لا يبقى المستشعر يعمل والتابب مغلق.
 */
class QiblaViewModel(application: Application) : AndroidViewModel(application) {

    private val sensorManager =
        application.getSystemService(Application.SENSOR_SERVICE) as? SensorManager

    private val _state = MutableStateFlow(QiblaUiState())
    val state: StateFlow<QiblaUiState> = _state.asStateFlow()

    // قراءات المزدوج الاحتياطي — التسارع + المغناطيسية
    private var gravity: FloatArray? = null
    private var geomagnetic: FloatArray? = null

    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    /**
     * الانحراف المغناطيسي للموقع (بالدرجات، شرقاً موجب) — يُحسب من
     * نموذج العالم الجيومغناطيسي عند بدء الشاشة بالإحداثيين الممرّرين
     * من QiblaScreen، ويُصحّح به كل قراءة بوصلة إلى اتجاه حقيقي
     * (توجيه المالك alpha30: «تحقق من دقه اتجاه القبله»).
     */
    private var declinationDeg = 0f

    private val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            when (event.sensor.type) {
                Sensor.TYPE_ROTATION_VECTOR -> {
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    consumeRotation()
                }
                Sensor.TYPE_ACCELEROMETER -> {
                    gravity = event.values.clone()
                    consumeFallback()
                }
                Sensor.TYPE_MAGNETIC_FIELD -> {
                    geomagnetic = event.values.clone()
                    consumeFallback()
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    /**
     * بدء الاستماع — من ظهور شاشة القبلة. الإحداثيان يمرران (إن وُجدا)
     * لحساب الانحراف المغناطيسي وتصحيح الاتجاه إلى الشمال الحقيقي،
     * وغيابهما يعني اتجاهاً مغناطيسياً كما كان.
     */
    fun start(latitude: Double = Double.NaN, longitude: Double = Double.NaN) {
        declinationDeg = if (latitude.isFinite() && longitude.isFinite()) {
            GeomagneticField(
                latitude.toFloat(), longitude.toFloat(), 0f,
                System.currentTimeMillis(),
            ).declination
        } else {
            0f
        }

        val manager = sensorManager ?: run { markUnavailable(); return }
        val rotationVector = manager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val accelerometer = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnetic = manager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        val registered = when {
            rotationVector != null -> manager.registerListener(
                listener, rotationVector, SensorManager.SENSOR_DELAY_GAME,
            )
            accelerometer != null && magnetic != null -> {
                var ok = manager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_GAME)
                ok = manager.registerListener(listener, magnetic, SensorManager.SENSOR_DELAY_GAME) && ok
                ok
            }
            else -> false
        }

        if (registered) {
            _state.update { it.copy(started = true, sensorAvailable = true) }
        } else {
            markUnavailable()
        }
    }

    /** إيقاف الاستماع — من اختفاء الشاشة، ترشيداً للبطارية */
    fun stop() {
        sensorManager?.unregisterListener(listener)
        _state.update { it.copy(started = false) }
    }

    /**
     * الوضع اليدوي — لا بوصلة حقيقية: المستخدم يشدّ القرص بإصبعه
     * وكل بكسل سحب أفقياً يقلب درجة الاتجاه.
     */
    fun dragHeading(deltaDeg: Float) {
        _state.update { cur ->
            if (cur.sensorAvailable) cur
            else cur.copy(heading = QiblaMath.normalise(cur.heading + deltaDeg))
        }
    }

    private fun markUnavailable() {
        _state.update { it.copy(sensorAvailable = false, started = false) }
    }

    private fun consumeRotation() {
        SensorManager.getOrientation(rotationMatrix, orientationAngles)
        val azimuthDeg = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
        pushHeading(azimuthDeg)
    }

    private fun consumeFallback() {
        val g = gravity ?: return
        val m = geomagnetic ?: return
        if (SensorManager.getRotationMatrix(rotationMatrix, null, g, m)) {
            SensorManager.getOrientation(rotationMatrix, orientationAngles)
            val azimuthDeg = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
            pushHeading(azimuthDeg)
        }
    }

    private fun pushHeading(azimuthDeg: Float) {
        // المستشعر يعطي اتجاهاً مغناطيسياً — يُصحّح إلى حقيقي بالانحراف
        // قبل التنعيم (alpha30: دقة اتجاه القبلة)
        val trueAzimuth = QiblaMath.trueHeading(azimuthDeg, declinationDeg)
        _state.update { cur ->
            cur.copy(heading = QiblaMath.smoothHeading(cur.heading, trueAzimuth))
        }
    }
}
