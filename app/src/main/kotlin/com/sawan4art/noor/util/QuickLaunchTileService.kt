package com.sawan4art.noor.util

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.sawan4art.noor.data.SettingsRepository

/**
 * بلاطة «الوقت المتبقي» في الإعدادات السريعة — v1.81 (بلاغ المالك:
 * «انا عايز ايقونه شريط اشعارات زي اللي موجودين في الصوره مش مجرد
 * اشعار ثابت الاشعار الثابت ده شيله Quick Settings Tile يكون داعم
 * ليها»).
 *
 * البلاطة في شبكة الإعدادات السريعة (داخل شريط الإشعارات بعد السحب):
 * عنوانها «نور المسلم» وسطرها الفرعي «أذان العصر ٤:١٥ م» — v1.85 (بلاغ المالك: وقتُ الصلاة القادمة نفسه بدل المتبقي)
 * محسوبةً لحظة سحب الشريط نفسها (onStartListening) ومن منبه تجديد
 * الأذان (QUICK_REFRESH عبر requestListeningState) — ضغطةٌ عليها
 * تفتح التطبيق فورًا. وإن كان مفتاح أيقونة شريط الإشعارات مطفأً من
 * الإعدادات تصير البلاطة «غير متاحة» بدل أن تجامل المفتاح.
 *
 * ملاحظة عقدية: البلاطة تُقرأ لحظةَ العرض فلا تحتاج عدّادًا ذاتيًا —
 * النظام يعيد استدعاء onStartListening كل مرة يفتح فيها المستخدم
 * الشريط، فالنص دائمًا على قِيَم الصلاة القادمة الحية.
 */
class QuickLaunchTileService : TileService() {

    override fun onStartListening() {
        renderTile()
    }

    override fun onClick() {
        runCatching {
            val intent = android.content.Intent(this, com.sawan4art.noor.MainActivity::class.java).apply {
                addFlags(
                    android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                        android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP,
                )
            }
            // v24 بِدئ التطبيق: قبل ٢٩ بِدئةٌ عبر PendingIntent وبعدها عبر Intent —
            // الاستدعاء الأحدث لا يوجد في الأجهزة الأقدم فيكسرها لو ما فُرِّق
            if (android.os.Build.VERSION.SDK_INT >= 29) {
                startActivityAndCollapse(intent)
            } else {
                @Suppress("DEPRECATION")
                startActivityAndCollapse(
                    android.app.PendingIntent.getActivity(
                        this,
                        0,
                        intent,
                        android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE,
                    ),
                )
            }
        }
    }

    /** رسم البلاطة من الإعدادات الحية — نقيةٌ كي تُستدعى من أي مكان */
    private fun renderTile() {
        runCatching {
            val qs = qsTile ?: return
            // المفتاح المستقل للبلاطة (v1.82) — لا علاقة له بإشعار
            // شريط الإشعارات؛ مطفأً تصير البلاطة «غير متاحة» لا تختفي
            val enabled = kotlinx.coroutines.runBlocking {
                SettingsRepository(this@QuickLaunchTileService).settingsOnce().quickTile
            }
            if (!enabled) {
                qs.setState(Tile.STATE_UNAVAILABLE)
                qs.updateTile()
                return
            }
            val target = kotlinx.coroutines.runBlocking {
                QuickLaunch.computeNextTargetBlocking(this@QuickLaunchTileService)
            }
            if (target != null) {
                // v1.85 بلاغ المالك: البلاطة تعرض وقت الصلاة القادمة
                // نفسه لا المتبقي — ثابتٌ يُقرأ مرةً ويفهم مرةً
                val sub = QuickLaunch.tileSubtitle(target.prayerName, target.atMillis)
                qs.setSubtitle(sub)
                qs.contentDescription = "$sub — ضغطة تفتح التطبيق"
            } else {
                qs.setSubtitle("وقت الصلاة القادمة")
            }
            qs.setState(Tile.STATE_ACTIVE)
            qs.updateTile()
        }
    }
}
