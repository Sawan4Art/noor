package com.sawan4art.noor.alarm

import android.os.Build
import android.os.Bundle
import android.content.Intent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sawan4art.noor.data.ThemeMode
import com.sawan4art.noor.ui.theme.NoorTheme

/**
 * شاشة التنبيه الكاملة فوق القفل — تُفتح من الإشعار المنبثق (full-screen
 * intent) في وقت الأذان/الإقامة حتى لو الهاتف مقفول:
 *
 * - خصائص فوق القفل في المانيفست (٢٧+) وأعلام النافذة المكافئة هنا
 *   للأقدم (٢٤-٢٦)، والشاشة تبقى مضاءة ما دامت ظاهرة.
 * - المحتوى: اسم الصلاة كبيراً وعنوان التنبيه وزر إيقاف الصوت —
 *   بلغة التطبيق وثيمه.
 * - زر «إيقاف الصوت» يوقف الخدمة كلياً (ويعيد صوت الهاتف إن كان
 *   مكتوماً) — بلا حاجة لفتح التطبيق.
 */
class AlarmActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyOverLockFlags()
        val name = intent?.getStringExtra(EXTRA_PRAYER_NAME)
        if (name == null) {
            finish()
            return
        }
        val mode = intent?.getStringExtra(EXTRA_PRAYER_MODE) ?: AlarmService.MODE_ADHAN

        setContent {
            NoorTheme(themeMode = ThemeMode.SYSTEM) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AlarmAlertContent(
                        prayerName = name,
                        isIqamah = mode == AlarmService.MODE_IQAMAH,
                        onStop = {
                            runCatching {
                                startService(
                                    Intent(this, AlarmService::class.java)
                                        .setAction(AlarmService.ACTION_STOP),
                                )
                            }
                            finish()
                        },
                    )
                }
            }
        }
    }

    /** أعلام فوق القفل — الخصائص في المانيفست (٢٧+) والأعلام هنا (٢٤-٢٦) */
    @Suppress("DEPRECATION") // FLAG_SHOW_WHEN_LOCKED أُهمل في ٢٧ بديلاً الخصائص — ويلزم للأقدم
    private fun applyOverLockFlags() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1) {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    @Composable
    private fun AlarmAlertContent(
        prayerName: String,
        isIqamah: Boolean,
        onStop: () -> Unit,
    ) {
        val scheme = MaterialTheme.colorScheme
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(scheme.primaryContainer, scheme.surface),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = if (isIqamah) "قد قامت الصلاة" else "حان الآن وقت",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = scheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = prayerName,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Bold,
                    color = scheme.primary,
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "«أَقِمِ الصَّلَاةَ لِدُلُوكِ الشَّمْسِ»",
                    fontSize = 15.sp,
                    color = scheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(40.dp))
                Button(
                    onClick = onStop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = scheme.primary,
                        contentColor = scheme.onPrimary,
                    ),
                ) {
                    Text(text = "إيقاف الصوت", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    companion object {
        /** إكسترا اسم الصلاة — يحمله الإشعار المنبثق */
        const val EXTRA_PRAYER_NAME: String = "noor_extra_alarm_prayer_name"

        /** إكسترا نوع التنبيه: أذان أم إقامة */
        const val EXTRA_PRAYER_MODE: String = "noor_extra_alarm_prayer_mode"
    }
}
