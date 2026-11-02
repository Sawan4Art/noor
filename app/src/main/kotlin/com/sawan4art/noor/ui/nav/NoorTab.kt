package com.sawan4art.noor.ui.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.sawan4art.noor.ui.icons.NoorIcons

/**
 * تبويبات التطبيق السبعة — تمثل من اليمين لأن التطبيق عربي RTL ثابت.
 *
 * توجيه المالك (مكتبة الأحاديث): «إعادة ترتيب عناصر شريط التنقل الحالي
 * لتكون أيقونة المصحف الموجودة بالفعل في المنتصف» — سبعة تبويبات بالمصحف
 * في الوسط تماماً (الرابع من سبعة): يمينه أدوات العبادة (المواقيت والقبلة
 * والأذكار) ويساره صاحبتاه من كتب الحكم (الأحاديث ثم المسبحة) والإعدادات
 * آخرها من جهة اليسار كعادة الشريط، وانزلاق NoorApp الاتجاهي يتبع
 * الترتيب الجديد تلقائياً لأنه يعتمد على ordinal.
 */
enum class NoorTab(val label: String, val icon: ImageVector) {
    PRAYER("المواقيت", NoorIcons.PrayerTimes),
    QIBLA("القبلة", NoorIcons.Qibla),
    ADHKAR("الأذكار", NoorIcons.Adhkar),
    QURAN("المصحف", NoorIcons.Mushaf),
    HADITH("الأحاديث", NoorIcons.Hadith),
    TASBIH("المسبحة", NoorIcons.Tasbih),
    SETTINGS("الإعدادات", Icons.Filled.Settings),
}
