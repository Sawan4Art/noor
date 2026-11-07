package com.sawan4art.noor.domain.permissions

import org.json.JSONObject

/**
 * بوابة الصلاحيات — عقل «متى نطلب؟» النقي (alpha57 — بلاغ المالك):
 *
 * «في مشكله مع كل فتح للتطبيق بيطلب اذن عدم الازعاج انا عايز طالما
 * الاوذنات كلها تعمل لا يطلب التطبيق الاذن مره اخرى الا اذا تم الغاء
 * اذونات منها … و لو حتى حدث المستخدم التطبيق ألا اذا طبعا اضفنا اذن
 * اخر مستقبلا».
 *
 * فالحالة لكل إذنٍ على حدة:
 * - **غير مسأل قطّ** (أول فتح، أو إذنٌ جديد أضفناه مستقبلاً) ← يُطلب.
 * - **ممنوح** (يعمل الآن) ← لا يُزاح أبداً حتى بعد أي تحديث.
 * - **مرفوض بلا سابقة منح** (سُئل ورفض) ← لا يُزاح — إلحاحُه في كل
 *   فتحٍ هو ذاته البلاغ الذي أنزل هذه البوابة (صفحة عدم الإزعاج تفتح
 *   من الإعدادات متى شاء المستخدم).
 * - **ممنوحٌ سابقاً ثم فُقد** (أوقفته إعدادات النظام) ← يُطلب مرةً
 *   واحدة كي يعمل التطبيق كما توجّه المالك في alpha56: «اذا حدث و تم
 *   ايقاف اذن يطلب منه التطبيق اخذ الاذن مره اخرى» — فإن رفض بعد
 *   المنح عاد مرفوضاً فلا إلحاح بعده.
 *
 * الحالة تُخزَّن JSON في DataStore (إذنٌ سُئل؟ ممنوحٌ كان؟) ويُقرأ
 * عند كل فتح — والمنطق كله هنا نقيٌّ يُختبر بلا أندرويد.
 */
object PermissionGate {

    /** مفاتيح الأذونات المعروفة — إذنٌ جديد مستقبلاً = مفتاح جديد يُطلب مرة */
    val KEYS = listOf("location", "notifications", "exact_alarm", "full_screen", "dnd")

    /** حالة الإذن الواحد */
    data class PermState(
        /** سُئل المستخدم عنه من قبل؟ */
        val asked: Boolean = false,
        /** كان ممنوحاً في آخر وضعٍ معروف؟ */
        val granted: Boolean = false,
    )

    /**
     * ما الذي يلزم طلبه الآن؟ — الحالة المخزنة × الوضع الحقيقي:
     * غيرُ مسألٍ يُطلب، وممنوحٌ سابقاً فُقد يُطلب مرة، وسائرُ الأحوال
     * تسير بلا إزعاج (ممنوحٌ يعمل، أو مرفوضٌ سبق رفضه).
     */
    fun decide(state: Map<String, PermState>, currentlyGranted: Map<String, Boolean>): List<String> =
        KEYS.filter { key ->
            val s = state[key] ?: PermState()
            val now = currentlyGranted[key] ?: false
            if (now) {
                false // يعمل — لا يُزاح أبداً
            } else {
                !s.asked || s.granted // لم يُسأل قطّ (أول مرة/إذن جديد) أو ممنوحٌ فُقد
            }
        }

    /**
     * حفظ الحالة بعد سلسلة الطلب — لكل إذن: هل يعمل الآن؟ فالمعطي
     * يصير ممنوحاً، والرافض يُسجَّل مرفوضاً بلا سابقة منح كي لا يُلحَّ.
     */
    fun markAsked(
        state: Map<String, PermState>,
        keys: List<String>,
        currentlyGranted: Map<String, Boolean>,
    ): Map<String, PermState> {
        val next = state.toMutableMap()
        for (key in keys) {
            val now = currentlyGranted[key] ?: false
            next[key] = if (now) {
                PermState(asked = true, granted = true)
            } else {
                PermState(asked = true, granted = false)
            }
        }
        return next
    }

    fun toJson(state: Map<String, PermState>): String = JSONObject().apply {
        for (key in KEYS) {
            val s = state[key] ?: PermState()
            put(key, JSONObject().put("asked", s.asked).put("granted", s.granted))
        }
    }.toString()

    fun fromJson(raw: String?): Map<String, PermState> {
        if (raw.isNullOrBlank()) return emptyMap()
        return runCatching {
            val o = JSONObject(raw)
            KEYS.mapNotNull { key ->
                if (!o.has(key)) return@mapNotNull null
                val p = o.getJSONObject(key)
                key to PermState(p.optBoolean("asked", false), p.optBoolean("granted", false))
            }.toMap()
        }.getOrDefault(emptyMap())
    }
}
