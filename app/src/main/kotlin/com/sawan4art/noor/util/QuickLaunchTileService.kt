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
 * عنوانها «نور المسلم» وسطرها الفرعي «متبقي حتى أذان العصر ٣:٢٤»
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
            val enabled = kotlinx.coroutines.runBlocking {
                SettingsRepository(this@QuickLaunchTileService).settingsOnce().quickNotification
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
                qs.setSubtitle(QuickLaunch.tileSubtitle(target.prayerName, target.atMillis))
                qs.contentDescription = "الوقت المتبقي حتى أذان ${target.prayerName} — ضغطة تفتح التطبيق"
            } else {
                qs.setSubtitle("الوقت المتبقي")
            }
            qs.setState(Tile.STATE_ACTIVE)
            qs.updateTile()
        }
    }
}
