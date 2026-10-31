package com.sawan4art.noor.domain.location

import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.location.LocationManager
import androidx.core.content.ContextCompat

/**
 * بوابة تحديد الموقع — توجيه المالك alpha18:
 * «لما بضغط على ايقونة الخرائط بتشتغل تمام لو الموقع والانترنت شغالين،
 * أما في حاله غلق الانترنت والموقع من الهاتف مفيش رساله تفيد بتفعيلهم
 * وفي بعض الاحيان لاج بسيط بسبب الحركه دي».
 *
 * الجذر: المسار القديم كان يدخل فوراً في قراءة الموقع، وعندما تكون
 * المزوّدات مقفولة ينتظر عقد مهلة كاملاً (15 ثانية) ثم يحاول جلب اسم
 * المدينة من الشبكة (مهلتا 9 ثوانٍ) ثم يسكت بلا رسالة — فاللاج هو انتظار
 * النهاية بلا نتيجة، وصمت الفشل هو غياب الرسالة.
 *
 * العلاج: بوابات سريعة قبل أي انتظار (إذن → خدمة الموقع → الإنترنت)،
 * وأي بوابة مقفولة تُنهي الطلب فوراً برسالة عربية وإجراء فتح الإعدادات
 * المناسب. الفحص نفسه استدعاءات رخيصة على خيط الواجهة — بلا لاج.
 *
 * [LocationGate] نقي بلا أندرويد — يُختبر وحدةً؛ [LocationGateChecker]
 * هو القارئ الفعلي لحالة الجهاز.
 */
data class LocationGate(
    val hasPermission: Boolean,
    val serviceOn: Boolean,
    val online: Boolean,
    /** رسالة قسرية لحالات خاصة (فشل القراءة رغم فتح البوابات) */
    val forcedMessage: String? = null,
    /** إجراء قسري يلغي الاشتقاق — يُستخدم مع الرسالة القسرية */
    val forcedAction: Action? = null,
) {

    enum class Action { NONE, OPEN_APP_SETTINGS, OPEN_LOCATION_SETTINGS, OPEN_INTERNET_SETTINGS }

    val blocked: Boolean get() = !hasPermission || !serviceOn || !online

    /** الرسالة العربية الجاهزة للعرض — null فقط حين كل البوابات مفتوحة */
    val message: String?
        get() = when {
            forcedMessage != null -> forcedMessage
            !blocked -> null
            else -> action.message
        }

    val action: Action
        get() = forcedAction ?: when {
            !hasPermission && !serviceOn && !online -> Action.OPEN_APP_SETTINGS
            !hasPermission -> Action.OPEN_APP_SETTINGS
            !serviceOn && !online -> Action.OPEN_LOCATION_SETTINGS
            !serviceOn -> Action.OPEN_LOCATION_SETTINGS
            !online -> Action.OPEN_INTERNET_SETTINGS
            else -> Action.NONE
        }

    /** تسمية الإجراء على زر الحوار — فارغة = زر إغلاق فقط */
    val actionLabel: String
        get() = when (action) {
            Action.OPEN_APP_SETTINGS -> "فتح إعدادات التطبيق"
            Action.OPEN_LOCATION_SETTINGS -> "فتح إعدادات الموقع"
            Action.OPEN_INTERNET_SETTINGS -> "فتح إعدادات الشبكة"
            Action.NONE -> ""
        }

    private val Action.message: String
        get() = when (this) {
            Action.OPEN_APP_SETTINGS ->
                if (serviceOn || online) {
                    "إذن الموقع غير ممنوح للتطبيق — اسمح بالإذن من إعدادات التطبيق ثم أعد المحاولة"
                } else {
                    "إذن الموقع غير ممنوح، وخدمة الموقع والإنترنت مقفولان — " +
                        "اسمح بالإذن وفعّل خدمة الموقع وافتح الإنترنت ثم أعد المحاولة"
                }
            Action.OPEN_LOCATION_SETTINGS ->
                if (!online) {
                    "خدمة الموقع والإنترنت مقفولان — فعّل خدمة الموقع وافتح الواي فاي " +
                        "أو بيانات الهاتف ثم أعد المحاولة"
                } else {
                    "خدمة الموقع مقفولة من الهاتف — فعّلها من الإعدادات " +
                        "كي يحدد التطبيق موقعك تلقائياً"
                }
            Action.OPEN_INTERNET_SETTINGS ->
                "لا يوجد اتصال بالإنترنت — افتح الواي فاي أو بيانات الهاتف " +
                    "لجلب اسم المدينة من موقعك"
            Action.NONE -> ""
        }

    companion object {
        /** القراءة فشلت رغم فتح البوابات — رسالة عامة بلا إجراء (مهلة الموقع انقضت) */
        val READING_FAILED = LocationGate(
            hasPermission = false,
            serviceOn = true,
            online = true,
            forcedMessage =
                "لم يتمكن التطبيق من تحديد موقعك الآن — تأكد من أن السماء مكشوفة " +
                    "واجلس في مكان مفتوح ثم أعد المحاولة",
            forcedAction = Action.NONE,
        )
    }
}

/** قارئ حالة الجهاز — استدعاءات رخيصة آمنة على خيط الواجهة */
object LocationGateChecker {

    fun check(context: Context): LocationGate = LocationGate(
        hasPermission = hasLocationPermission(context),
        serviceOn = isLocationServiceOn(context),
        online = isOnline(context),
    )

    fun hasLocationPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    fun isLocationServiceOn(context: Context): Boolean {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return false
        return listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
        ).any { runCatching { lm.isProviderEnabled(it) }.getOrDefault(false) }
    }

    fun isOnline(context: Context): Boolean = runCatching {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }.getOrDefault(false)
}
