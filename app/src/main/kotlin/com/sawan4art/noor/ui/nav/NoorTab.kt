package com.sawan4art.noor.ui.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.sawan4art.noor.ui.icons.NoorIcons

/**
 * تبويبات التطبيق الستة — نفس ترتيب النسخة القديمة، وتمثل من اليمين
 * لأن التطبيق عربي RTL ثابت.
 */
enum class NoorTab(val label: String, val icon: ImageVector) {
    PRAYER("المواقيت", NoorIcons.PrayerTimes),
    QURAN("المصحف", NoorIcons.Mushaf),
    QIBLA("القبلة", NoorIcons.Qibla),
    ADHKAR("الأذكار", NoorIcons.Adhkar),
    TASBIH("المسبحة", NoorIcons.Tasbih),
    SETTINGS("الإعدادات", Icons.Filled.Settings),
}
