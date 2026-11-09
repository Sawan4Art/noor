package com.sawan4art.noor.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sawan4art.noor.BuildConfig
import com.sawan4art.noor.data.AdhanPrefs
import com.sawan4art.noor.data.NoorSettingsState
import com.sawan4art.noor.data.update.DownloadState
import com.sawan4art.noor.data.update.UpdateUiState
import com.sawan4art.noor.data.update.UpdateNotifier
import com.sawan4art.noor.domain.mushaf.QuranReciters
import com.sawan4art.noor.domain.prayer.CalculationMethod
import com.sawan4art.noor.ui.home.KhutbahEditorContent
import com.sawan4art.noor.ui.icons.NoorIcons
import com.sawan4art.noor.ui.settings.adhan.AdhanActions
import com.sawan4art.noor.ui.settings.adhan.AdhanSection
import com.sawan4art.noor.ui.theme.AccentPalette
import com.sawan4art.noor.ui.theme.LiquidDeveloperBadge
import com.sawan4art.noor.ui.theme.NoorPageTitle
import java.util.Locale

/**
 * الإعدادات — كل قسمٍ في صفحته الخاصة (alpha28 توجيه المالك:
 * «عايز كل قسم في الاعدادت يكون له صفحه لوحده»):
 *
 * الصفحة الرئيسية قائمة أقسام بملخصاتها الحية (المدينة، الطريقة
 * المختارة ومذهب العصر معها، اسم اللون وحالة الأساسي، حالة التحديث)
 * وكل قسم يفتح صفحته الخاصة بزر رجوع يصعد خطوة واحدة — التنقل الداخلي
 * على نمط تبويب الأذكار نفسه (الرجوع يعود للقائمة ولا يسرقه معالج
 * NoorApp لأن آخر معالج مُركَّب هو الأسبق).
 *
 * alpha34 بتوجيه المالك:
 * 1) «صفحه مذهب العصر ضيفه داخل صفحه طريقه حساب المواقيت» — قائمة
 *    مذهب العصر انتقلت داخل صفحة طريقة الحساب فأُلغيت صفحتها المستقلة.
 * 2) «عايز الوان الون الاساسي داخل صفح الوان التطبيق و عند الضغط على
 *    زر الحالي يتبع اللون تختفي قائمه الالوان الثانيه و عند فصله تظهر
 *    قائمه الالوان الاخرى مره اخرى و عند اختيار نفس الون في القائمتين
 *    يتم تلقائى تفعيل زر الحالى يتبع اللون» — قائمة اللون الأساسي
 *    انتقلت داخل صفحة ألوان التطبيق: زر «الحالي» يطوي القائمة الثانية
 *    ويُظهرها عند فصله، واختيار اللون نفسه في القائمتين يفعّل الزر
 *    تلقائياً لأن التطابق يعني أن الأساسي يتبع أصلاً.
 */
@Composable
fun SettingsScreen(
    settings: NoorSettingsState,
    /** القسم المطلوب فتحه مباشرة — ضغطة الويدجت تفتح قسمها (alpha63) */
    initialSection: String = "home",
    locating: Boolean,
    onDetectLocation: () -> Unit,
    onSetMethod: (CalculationMethod) -> Unit,
    onSetHanafi: (Boolean) -> Unit,
    onSetAccent: (String) -> Unit,
    /** اللون الأساسي المستقل — نص فارغ يعني «الحالي» (يتبع لون التطبيق) */
    onSetPrimary: (String) -> Unit,
    adhanPrefs: AdhanPrefs,
    adhanActions: AdhanActions,
    updateState: UpdateUiState,
    /** حالة تنزيل التحديث داخل التطبيق (alpha48) */
    downloadState: DownloadState,
    onCheckUpdates: () -> Unit,
    onDownloadUpdate: () -> Unit,
    /** معرّف الشيخ المختار لتلاوة المصحف (alpha54) */
    reciterId: String,
    /** تغيير الشيخ المختار — قسم «أصوات الشيخ» */
    onSetReciter: (String) -> Unit,
    /** زر خطبة الجمعة في الرئيسية مفعّل؟ — قسم «خطبة الجمعة» (alpha56) */
    khutbahEnabled: Boolean,
    /** تفعيل/إيقاف زر خطبة الجمعة */
    onSetKhutbahEnabled: (Boolean) -> Unit,
    /** مركز تعديل الخطبة (alpha57) — العنوان والنص والصور المحفوظة */
    khutbahTitle: String,
    khutbahText: String,
    khutbahImages: List<String>,
    /** حفظ خطبة الجمعة المكتوبة */
    onSetKhutbah: (String, String) -> Unit,
    /** استيراد صورة إلى الخطبة */
    onImportKhutbahImage: (android.net.Uri) -> Unit,
    /** حذف صورة من الخطبة */
    onDeleteKhutbahImage: (String) -> Unit,
) {
    // الروابط الداخلية: home ← location / method / adhan / colors / voices / khutbah / widgets / updates
    // وinitialSection يفتح قسماً مباشرةً — ضغطة الويدجت تفتح قسمها (alpha63)
    var section by rememberSaveable(initialSection) { mutableStateOf(initialSection) }
    BackHandler(enabled = section != "home") { section = "home" }

    when (section) {
        "location" -> SectionPage(title = "الموقع", onBack = { section = "home" }) {
            SectionCard {
                LocationSection(settings = settings, locating = locating, onDetectLocation = onDetectLocation)
            }
        }
        // alpha34: مذهب العصر داخل صفحة طريقة الحساب بأمر المالك —
        // «صفحه مذهب العصر ضيفه داخل صفحه طريقه حساب المواقيت»
        "method" -> SectionPage(title = "طريقة حساب المواقيت", onBack = { section = "home" }) {
            SectionCard {
                MethodSection(settings = settings, onSetMethod = onSetMethod)
                Spacer(Modifier.height(18.dp))
                Text(
                    text = "مذهب العصر",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(10.dp))
                AsrSection(settings = settings, onSetHanafi = onSetHanafi)
            }
        }
        // صفحة الأذان والصوت — القسم الجديد: الأصوات والفواصل والجمعة والكتم
        "adhan" -> SectionPage(title = "الأذان والصوت", onBack = { section = "home" }) {
            SectionCard {
                AdhanSection(adhan = adhanPrefs, actions = adhanActions)
            }
        }
        // alpha34: اللون الأساسي داخل صفحة الألوان بأمر المالك —
        // «عايز الوان الون الاساسي داخل صفح الوان التطبيق»
        "colors" -> SectionPage(title = "ألوان التطبيق", onBack = { section = "home" }) {
            SectionCard {
                ColorsSection(
                    settings = settings,
                    onSetAccent = onSetAccent,
                    onSetPrimary = onSetPrimary,
                )
            }
        }
        // قسم أصوات الشيخ — عشرون شيخاً وتنزيل صوت كلٍّ منهم (alpha54)
        "voices" -> SectionPage(title = "أصوات الشيخ", onBack = { section = "home" }) {
            SectionCard {
                QuranVoicesSection(
                    reciterId = reciterId,
                    onSetReciter = onSetReciter,
                )
            }
        }
        // قسم خطبة الجمعة — التفعيل + مركز التعديل كلّه (alpha57):
        // الخطب الجاهزة والمكتوبة والصور من هنا، وزر الرئيسية للعرض فقط
        "khutbah" -> SectionPage(title = "خطبة الجمعة", onBack = { section = "home" }) {
            SectionCard {
                KhutbahSettingsSection(
                    enabled = khutbahEnabled,
                    onSet = onSetKhutbahEnabled,
                    savedTitle = khutbahTitle,
                    savedText = khutbahText,
                    savedImages = khutbahImages,
                    onSetKhutbah = onSetKhutbah,
                    onImportImage = onImportKhutbahImage,
                    onDeleteImage = onDeleteKhutbahImage,
                )
            }
        }
        // قسم الويدجت — خمسة أشكال وتخصيص كامل (alpha56)
        "widgets" -> SectionPage(title = "الويدجت", onBack = { section = "home" }) {
            SectionCard {
                WidgetSection()
            }
        }
        "updates" -> SectionPage(title = "التحديثات", onBack = { section = "home" }) {
            SectionCard {
                UpdatesSection(
                    updateState = updateState,
                    downloadState = downloadState,
                    currentVersion = BuildConfig.VERSION_NAME,
                    onCheckUpdates = onCheckUpdates,
                    onDownloadUpdate = onDownloadUpdate,
                )
            }
        }
        else -> SettingsHome(
            settings = settings,
            updateState = updateState,
            adhanPrefs = adhanPrefs,
            reciterId = reciterId,
            khutbahEnabled = khutbahEnabled,
            khutbahReady = khutbahTitle.isNotBlank() || khutbahImages.isNotEmpty(),
            onOpen = { section = it },
        )
    }
}

/** قائمة الأقسام — صفحة الإعدادات الرئيسية: كل قسم صفٌ بملخصه الحي */
@Composable
private fun SettingsHome(
    settings: NoorSettingsState,
    updateState: UpdateUiState,
    adhanPrefs: AdhanPrefs,
    reciterId: String,
    khutbahEnabled: Boolean,
    khutbahReady: Boolean,
    onOpen: (String) -> Unit,
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(16.dp))
        // alpha39 (بلاغ المالك: «في نص تحت كلمه الاعدادت … النص ده شيله
        // و اكتفي بكلمه الاعدادت اللي هي عنوان الصفحه») — العنوان وحده
        // داخل إطار NoorPageTitle والنص التوضيحي أُزيل تماماً
        NoorPageTitle(text = "الإعدادات")
        Spacer(Modifier.height(16.dp))

        SectionRow(
            icon = Icons.Filled.LocationOn,
            title = "الموقع",
            summary = settings.city,
            onOpen = { onOpen("location") },
        )
        SectionRow(
            icon = NoorIcons.PrayerTimes,
            title = "طريقة حساب المواقيت",
            summary = methodSummary(settings),
            onOpen = { onOpen("method") },
        )
        SectionRow(
            icon = NoorIcons.Mosque,
            title = "الأذان والصوت",
            summary = adhanSummary(adhanPrefs),
            onOpen = { onOpen("adhan") },
        )
        SectionRow(
            icon = NoorIcons.QuranVoice,
            title = "أصوات الشيخ",
            summary = reciterSummary(reciterId),
            onOpen = { onOpen("voices") },
        )
        SectionRow(
            icon = NoorIcons.QuranBookmarkOutline,
            title = "خطبة الجمعة",
            summary = when {
                !khutbahEnabled -> "متوقفة — زر الخطبة مخفي عن الرئيسية"
                khutbahReady -> "جاهزة — تُحرَّر من هنا وتُعرض من زر الرئيسية يوم الجمعة"
                else -> "اكتبها أو اعتمد خطبةً جاهزة أو صوراً — والعرض من زر الرئيسية"
            },
            onOpen = { onOpen("khutbah") },
        )
        SectionRow(
            icon = NoorIcons.PrayerTimes,
            title = "الويدجت",
            summary = "ويدجت واحدة قابلة للتخصيص بالكامل — العناصر والألوان والشفافية والفقاعات",
            onOpen = { onOpen("widgets") },
        )
        SectionRow(
            icon = NoorIcons.Palette,
            title = "ألوان التطبيق",
            summary = colorsSummary(settings),
            accentDot = AccentPalette.byId(settings.accentId).day.primary,
            onOpen = { onOpen("colors") },
        )
        SectionRow(
            icon = NoorIcons.Update,
            title = "التحديثات",
            summary = updatesSummary(updateState),
            onOpen = { onOpen("updates") },
        )

        Spacer(Modifier.height(24.dp))
        // توقيع المطور (alpha39 — بلاغ المالك: «اعاده البناء الاصليه بلغه
        // كوتلن ده شيله و استبدله باسم المطور و اسمه Yousef Sawan و تحته
        // رقم اصدار النسخه») — الاسم داخل الإطار السائل الحي المستطيل
        // (LiquidDeveloperBadge) وتحته رقم الإصدار وحده
        LiquidDeveloperBadge(
            name = "Yousef Sawan",
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        Spacer(Modifier.height(10.dp))
        // زر الشكاوى والمقترحات (alpha56 — بلاغ المالك: «عايز اضيف تحت
        // اسم المطور زر شكاوى و مقترحات و ده الايميل الخاص») — يفتح
        // تطبيق البريد على العنوان المعتمد والعنوان جاهز مكتوباً
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
            ),
            onClick = {
                runCatching {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:yousefsawan940@gmail.com")
                        putExtra(Intent.EXTRA_SUBJECT, "شكاوى ومقترحات — نور المسلم")
                    }
                    context.startActivity(
                        Intent.createChooser(intent, "شكاوى ومقترحات")
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                }
            },
            modifier = Modifier.align(Alignment.CenterHorizontally),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
            ) {
                Icon(
                    imageVector = NoorIcons.QuranCopy,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "شكاوى ومقترحات",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Spacer(Modifier.height(10.dp))

        // زر «تقرير آخر عطل» محذوف (alpha63 — بلاغ المالك: «ازل زر
        // اخر عطل») — دفتر الأعطال باقٍ يكتب أثر أي انهيارٍ خلفياً
        // بلا زرٍ في الواجهة.

        Spacer(Modifier.height(6.dp))
        Text(
            text = "الإصدار ${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        Spacer(Modifier.height(116.dp)) // مساحة شريط التنقل الزجاجي العائم
    }
}

/** ملخص التحديث في القائمة — لغة مفهومة من غير فتح الصفحة */
private fun updatesSummary(state: UpdateUiState): String = when (state) {
    is UpdateUiState.Available -> "تحديث جديد متاح: ${state.info.versionName}"
    is UpdateUiState.Failed -> "تعذّر الفحص — افتح الصفحة لإعادة المحاولة"
    else -> "أنت على أحدث نسخة — والفحص تلقائي عند فتح التطبيق"
}

/** ملخص قسم أصوات الشيخ — اسم الشيخ المختار وعلامة الصوت المحمَّل */
private fun reciterSummary(reciterId: String): String {
    val name = QuranReciters.byId(reciterId).name
    return "تلاوة المصحف بصوت: $name — والتنزيل للجهاز من القسم"
}

/** ملخص قسم الأذان في القائمة — الحالة والصوت الحالي بلا فتح الصفحة */
private fun adhanSummary(adhan: AdhanPrefs): String {
    if (!adhan.enabled) return "متوقف — افتح الصفحة لتشغيل الأذان"
    val voice = if (adhan.adhanVoice == "custom") {
        adhan.customAdhanName.ifEmpty { "صوت من الهاتف" }
    } else if (adhan.adhanVoice == "notification") {
        "نغمة إشعارات الهاتف"
    } else {
        com.sawan4art.noor.domain.adhan.AdhanVoices.adhanById(adhan.adhanVoice).name
    }
    return if (adhan.iqamahEnabled) {
        "مفعّل مع الإقامة — $voice"
    } else {
        "مفعّل — $voice"
    }
}

/**
 * صف قسمٍ في القائمة — بنية بطاقات مجموعات الأذكار نفسها: أيقونة في
 * مربع متدرّج، عنوان وملخص حي، وسهم الدخول في اتجاه القراءة.
 */
@Composable
private fun SectionRow(
    icon: ImageVector,
    title: String,
    summary: String,
    accentDot: Color? = null,
    onOpen: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = scheme.surface,
        border = BorderStroke(
            width = 1.dp,
            color = scheme.outline.copy(alpha = 0.28f),
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .clickable(onClick = onOpen),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .background(
                        brush = Brush.linearGradient(
                            listOf(
                                scheme.primaryContainer.copy(alpha = 0.75f),
                                scheme.secondaryContainer.copy(alpha = 0.75f),
                            ),
                        ),
                        shape = RoundedCornerShape(15.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = scheme.primary,
                    modifier = Modifier.size(26.dp),
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = scheme.onSurface,
                    )
                    if (accentDot != null) {
                        Spacer(Modifier.width(8.dp))
                        Surface(shape = CircleShape, color = accentDot, modifier = Modifier.size(16.dp)) {}
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    maxLines = 2,
                )
            }
            Spacer(Modifier.width(8.dp))
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "فتح",
                tint = scheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/** صفحة قسمٍ — ترويسة برجوع يصعد خطوةً واحدة ومحتوى قابل للسحب */
@Composable
private fun SectionPage(
    title: String,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "رجوع",
                    tint = scheme.primary,
                )
            }
            // alpha39: عنوان الصفحة داخل إطار NoorPageTitle (توجيه المالك:
            // «عايز كل عناوين الصفحات تكون داخل اطار متماشي مع تصميم التطبيق»)
            NoorPageTitle(text = title)
        }
        Spacer(Modifier.height(12.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            content()
            Spacer(Modifier.height(116.dp)) // مساحة شريط التنقل الزجاجي العائم
        }
    }
}

/** بطاقة القسم الواحد داخل صفحته — بلا عنوان مكرر فوقه */
@Composable
private fun SectionCard(
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(18.dp), content = content)
    }
}

// ===== محتوى الأقسام الخمسة — كامل إعدادات المرحلة الأولى =====

/** قسم الموقع — مدينتك الحالية وتحديدها تلقائياً */
@Composable
private fun LocationSection(
    settings: NoorSettingsState,
    locating: Boolean,
    onDetectLocation: () -> Unit,
) {
    Text(
        text = settings.city,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
    )
    Text(
        text = String.format(Locale.US, "%.4f° ، %.4f°", settings.latitude, settings.longitude),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(10.dp))
    Button(
        onClick = onDetectLocation,
        enabled = !locating,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
    ) {
        if (locating) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary,
            )
            Spacer(Modifier.size(8.dp))
        }
        Text("تحديد موقعي تلقائياً")
    }
}

/** قسم طريقة الحساب — الطرق الأربع المعتمدة */
@Composable
private fun MethodSection(
    settings: NoorSettingsState,
    onSetMethod: (CalculationMethod) -> Unit,
) {
    CalculationMethod.entries.forEach { method ->
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            RadioButton(
                selected = settings.method == method,
                onClick = { onSetMethod(method) },
            )
            Column {
                Text(
                    text = method.arabicName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = method.englishName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** قسم مذهب العصر — الحنفي أو الجمهور */
@Composable
private fun AsrSection(
    settings: NoorSettingsState,
    onSetHanafi: (Boolean) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = "المذهب الحنفي",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "ظل مثلهين — العصر متأخراً. الإيقاف = ظل مثله (الجمهور)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = settings.isHanafi, onCheckedChange = onSetHanafi)
    }
}

/** قسم الألوان — شبكة الألوان العشرين المتمايزة مع الأسود المحايد */
@Composable
private fun ColorsSection(
    settings: NoorSettingsState,
    onSetAccent: (String) -> Unit,
    onSetPrimary: (String) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Text(
        text = "عشرون لوناً متمايزاً لا تتشابه درجاتها — الجريئة التسع الجديدة مع العائلية والأسود — والتطبيق كله يتبع اختيارك في الوضعين",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(14.dp))
    // القائمة الأولى: لون التطبيق — واختيار نفس لون الأساسي فيها
    // يعيد الأساسي إلى «يتبع» تلقائياً لأن التطابق يساوي الاتباع
    AccentGrid(selectedId = settings.accentId, onPick = { id ->
        onSetAccent(id)
        if (settings.primaryId == id) onSetPrimary("")
    })
    Spacer(Modifier.height(22.dp))
    Text(
        text = "اللون الأساسي",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = scheme.onSurface,
    )
    Spacer(Modifier.height(4.dp))
    Text(
        text = "يغيّر الأزرار والسائل والتحديد فقط — وافتراضياً يتبع لون التطبيق",
        style = MaterialTheme.typography.bodySmall,
        color = scheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(14.dp))
    // زر «الحالي»: ضغطه والأساسي يتبعٌ يفصله فتظهر القائمة الثانية،
    // وضغطه مجدداً يعيده إلى الاتباع فتختفي القائمة (توجيه المالك)
    // — الفصل يثبّت لون التطبيق الحالي قيمةً صريحة فلا قفزة بصرية
    Surface(
        onClick = { onSetPrimary(if (settings.primaryId.isBlank()) settings.accentId else "") },
        shape = RoundedCornerShape(14.dp),
        color = if (settings.primaryId.isBlank()) scheme.secondaryContainer else scheme.surface,
        border = BorderStroke(
            width = if (settings.primaryId.isBlank()) 1.5.dp else 1.dp,
            color = if (settings.primaryId.isBlank()) scheme.secondary
            else scheme.outline.copy(alpha = 0.35f),
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Icon(
                Icons.Filled.Check,
                contentDescription = null,
                tint = if (settings.primaryId.isBlank()) scheme.secondary else scheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = if (settings.primaryId.isBlank()) {
                    "الحالي — يتبع لون التطبيق (" + AccentPalette.byId(settings.accentId).arabicName + ")"
                } else {
                    "الحالي — يتبع لون التطبيق"
                },
                style = MaterialTheme.typography.bodyLarge,
                color = scheme.onSurface,
            )
        }
    }
    // القائمة الثانية: تختفي والأساسي يتبع وتظهر عند فصله — بطيّ سائل
    AnimatedVisibility(
        visible = settings.primaryId.isNotBlank(),
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
    ) {
        Column {
            Spacer(Modifier.height(14.dp))
            // اختيار نفس لون التطبيق هنا يفعّل «الحالي» تلقائياً —
            // فتطابق القائمتين معناه أن الأساسي يتبع أصلاً
            AccentGrid(selectedId = settings.primaryId, onPick = { id ->
                onSetPrimary(if (id == settings.accentId) "" else id)
            })
        }
    }
}

/** ملخص صف طريقة الحساب — الطريقة ومذهب العصر معاً بعد دمج الصفحتين */
private fun methodSummary(settings: NoorSettingsState): String =
    settings.method.arabicName + " • مذهب العصر: " +
        (if (settings.isHanafi) "الحنفي — ظل مثلهين" else "الجمهور — ظل مثله")

/** ملخص صف الألوان — لون التطبيق وحالة الأساسي بعد دمج القائمتين */
private fun colorsSummary(settings: NoorSettingsState): String =
    AccentPalette.byId(settings.accentId).arabicName + " — الأساسي: " +
        (if (settings.primaryId.isBlank()) "يتبع لون التطبيق"
            else AccentPalette.byId(settings.primaryId).arabicName)

/** قسم التحديثات — النسخة الحالية والفحص اليدوي وزر «تحديث الآن»
 *  الذي ينزّل النسخة من داخل التطبيق بشريط تقدم ثم يفتح مثبّت النظام
 *  مباشرة (alpha48 — توجيه المالك: «التحديث بقا يفتح المتصفح انا
 *  مش عايز كده») ونسخ رابط التحميل الاحتياطي (alpha37). */
@Composable
private fun UpdatesSection(
    updateState: UpdateUiState,
    downloadState: DownloadState,
    currentVersion: String,
    onCheckUpdates: () -> Unit,
    onDownloadUpdate: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val busy = updateState is UpdateUiState.Checking

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "النسخة الحالية",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = currentVersion,
            style = MaterialTheme.typography.labelLarge,
            color = scheme.secondary,
        )
    }
    Spacer(Modifier.height(8.dp))
    Text(
        text = updateStatusText(updateState),
        style = MaterialTheme.typography.bodySmall,
        color = when {
            updateState is UpdateUiState.Failed -> scheme.error
            updateState is UpdateUiState.Available -> scheme.secondary
            else -> scheme.onSurfaceVariant
        },
    )
    // alpha35: إذن الإشعارات معطّل (٣٣+) = تنبيهات النسخ الجديدة لا تصلك
    // مهما فُحص — نبّه المالك وفتح إعدادات إشعارات التطبيق خطوةً واحدة
    val context = LocalContext.current
    if (!UpdateNotifier.canNotify(context)) {
        Spacer(Modifier.height(12.dp))
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = scheme.errorContainer.copy(alpha = 0.35f),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "الإشعارات معطّلة",
                        style = MaterialTheme.typography.titleSmall,
                        color = scheme.error,
                    )
                    Text(
                        text = "اسمح بإشعارات نور المسلم كي يصلك تنبيه كل نسخة جديدة",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = {
                    val intent = if (Build.VERSION.SDK_INT >= 26) {
                        Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                    } else {
                        Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            .setData(Uri.parse("package:" + context.packageName))
                    }
                    runCatching { context.startActivity(intent) }
                }) {
                    Text("فتح الإعدادات")
                }
            }
        }
    }
    Spacer(Modifier.height(12.dp))
    Button(
        onClick = onCheckUpdates,
        enabled = !busy,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = scheme.primary,
            contentColor = scheme.onPrimary,
        ),
    ) {
        if (busy) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = scheme.onPrimary,
            )
            Spacer(Modifier.size(8.dp))
        }
        Text("تحقق من التحديثات الآن")
    }
    // alpha48: زر «تحديث الآن» في الصفحة نفسها — التنزيل من داخل التطبيق
    // بشريط تقدم ثم فتح مثبّت النظام، من هنا دائماً حتى بعد استبعاد
    // حوار «ما الجديد» بـ«لاحقاً»
    when (updateState) {
        is UpdateUiState.Available -> {
            Spacer(Modifier.height(10.dp))
            when (downloadState) {
                is DownloadState.Running -> {
                    LinearProgressIndicator(
                        progress = { downloadState.percent / 100f },
                        modifier = Modifier.fillMaxWidth(),
                        color = scheme.primary,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "جارٍ تنزيل التحديث… ${downloadState.percent}٪",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.secondary,
                    )
                }
                is DownloadState.Failed -> {
                    Text(
                        text = downloadState.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.error,
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = onDownloadUpdate,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = scheme.secondary,
                            contentColor = scheme.onSecondary,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("إعادة المحاولة")
                    }
                }
                is DownloadState.NeedsPermission -> {
                    Text(
                        text = "سمح بنور من «التثبيت من مصادر غير معروفة» " +
                            "ثم اضغط تحديث تاني — خطوة مرة واحدة بس",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.secondary,
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = onDownloadUpdate,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = scheme.secondary,
                            contentColor = scheme.onSecondary,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("تحديث الآن (${updateState.info.sizeText})")
                    }
                }
                is DownloadState.Done -> {
                    Button(
                        onClick = onDownloadUpdate,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = scheme.secondary,
                            contentColor = scheme.onSecondary,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("فتح مثبِّت التحديث")
                    }
                }
                DownloadState.Idle -> {
                    Button(
                        onClick = onDownloadUpdate,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = scheme.secondary,
                            contentColor = scheme.onSecondary,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("تحديث الآن (${updateState.info.sizeText})")
                    }
                }
            }
        }
        is UpdateUiState.Failed -> {
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = onCheckUpdates,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = scheme.secondary,
                    contentColor = scheme.onSecondary,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("إعادة المحاولة")
            }
        }
        else -> Unit
    }
    // alpha37: احتياط المالك «اديني رابط للنسخ» — نسخ رابط الـ APK المباشر
    // إلى الحافظة ليُحمّل من أي متصفح
    val apkInfo = when (updateState) {
        is UpdateUiState.Available -> updateState.info
        is UpdateUiState.Failed -> updateState.info
        else -> null
    }
    if (apkInfo != null) {
        val clipboard = LocalClipboardManager.current
        var linkCopied by remember { mutableStateOf(false) }
        TextButton(
            onClick = {
                clipboard.setText(AnnotatedString(apkInfo.apkUrl))
                linkCopied = true
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = if (linkCopied) "تم نسخ رابط التحميل — افتحه في أي متصفح" else "نسخ رابط التحميل (احتياط)",
                style = MaterialTheme.typography.labelMedium,
                color = scheme.secondary,
            )
        }
    }
}

/** نص حالة التحديث بلغة مفهومة لكل حالة */
private fun updateStatusText(state: UpdateUiState): String = when (state) {
    is UpdateUiState.Idle -> "يبحث تلقائياً عند فتح التطبيق ومع كل نسخة جديدة يصلك إشعار"
    is UpdateUiState.Checking -> "جارٍ البحث عن تحديثات…"
    is UpdateUiState.Available -> "تحديث جديد متاح: ${state.info.versionName} — ${state.info.sizeText}"
    is UpdateUiState.Failed -> state.message
    is UpdateUiState.UpToDate -> "أنت على أحدث نسخة — لا يوجد تحديث جديد"
}

/**
 * شبكة الألوان العشرين — صفوف بأربع دوائر في الصف، دائرة لكل لون
 * بلونه النهاري واسمه عربياً تحته، والمختار بإطار مميز (توجيه المالك
 * alpha18 للعشرة الأولى وalpha29 للمتمايزة وalpha32 للعشرين بعد
 * إزالة الأبيض وإضافة الجريئة التسع).
 */
@Composable
private fun AccentGrid(selectedId: String, onPick: (String) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AccentPalette.All.chunked(4).forEach { rowAccents ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                rowAccents.forEach { accent ->
                    val selected = accent.id == selectedId
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f),
                    ) {
                        Surface(
                            onClick = { onPick(accent.id) },
                            shape = CircleShape,
                            color = accent.day.primary,
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (selected) 2.5.dp else 1.dp,
                                color = if (selected) scheme.secondary else scheme.outline.copy(alpha = 0.35f),
                            ),
                            modifier = Modifier.size(38.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (selected) {
                                    Icon(
                                        Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = accent.day.onPrimary,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = accent.arabicName,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) scheme.secondary else scheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

/**
 * قسم «خطبة الجمعة» — توجيه المالك ٢٠٢٦-١١-٠٧:
 * «عايز قسم جديد في الاعدادات خاص باعداد خطبه الجمعه و يكون في زر
 * تفعيل الخطبه او ايقافها علشان لو المستخدم مش عايز زر خطبه الجمعه
 * الموجود في الرئيسيه يظهر».
 *
 * مفتاح واحد واضح: مفعّل — الزر يظهر في مربع الهيدر يوم الجمعة
 * (وهو نفسه هيدر وقت صلاة الجمعة مع تنبيهٍ صغير بضرورة الضغط)،
 * ومطفأ — لا زر ولا لوحة خطبةٍ في الرئيسية.
 */
@Composable
private fun KhutbahSettingsSection(
    enabled: Boolean,
    onSet: (Boolean) -> Unit,
    savedTitle: String,
    savedText: String,
    savedImages: List<String>,
    onSetKhutbah: (String, String) -> Unit,
    onImportImage: (android.net.Uri) -> Unit,
    onDeleteImage: (String) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Column {
        Text(
            text = "هنا مركز خطبة الجمعة: تكتبها أو تعتمد خطبةً جاهزة أو " +
                "تحمّل صوراً — ويزر الرئيسية يوم الجمعة يعرضها للقراءة فقط " +
                "كما جهّزتها هنا. ومن أراد إخفاء الزر عن الرئيسية يوقفه.",
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 14.dp),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(scheme.primary.copy(alpha = 0.08f))
                .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            Icon(
                imageVector = NoorIcons.QuranBookmarkOutline,
                contentDescription = null,
                tint = scheme.primary,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = "زر خطبة الجمعة في الرئيسية",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = scheme.onSurface,
                )
                Text(
                    text = if (enabled) "ظاهر يوم الجمعة فوق التاريخ على اليمين" else "مخفي — لن يظهر زر الخطبة",
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(10.dp))
            Switch(
                checked = enabled,
                onCheckedChange = onSet,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = scheme.onPrimary,
                    checkedTrackColor = scheme.primary,
                ),
            )
        }

        Spacer(Modifier.height(16.dp))
        // مركز التعديل — الخطب الجاهزة والمكتوبة والصور (alpha57)
        KhutbahEditorContent(
            savedTitle = savedTitle,
            savedText = savedText,
            savedImages = savedImages,
            onSetKhutbah = onSetKhutbah,
            onImportImage = onImportImage,
            onDeleteImage = onDeleteImage,
        )
    }
}
