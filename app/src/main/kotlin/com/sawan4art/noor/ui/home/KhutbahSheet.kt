package com.sawan4art.noor.ui.home

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sawan4art.noor.domain.khutbah.KhutbahLibrary
import com.sawan4art.noor.domain.mushaf.MushafSurahs
import com.sawan4art.noor.util.CrashLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * لوحتا خطبة الجمعة (alpha57 — بلاغ المالك: «زر اعدادات الخطبه الموجود
 * في الاعدادات عايزه يشمل الخطب الجاهزه و الخطب المكتوبه او الصور
 * المحمله و زر العرض اللي بيظهر يوم الجمعه فقط يظهر الخطبه المسبق
 * تعديلها في اعدادت الخطبه يعني زر الخطبه الموجود في الرئيسيه ده
 * لعرض الخطبه فقط المراد عرضها»):
 *
 * - **[KhutbahEditorContent]** — مركز التعديل داخل قسم «خطبة الجمعة»
 *   في الإعدادات: تبويب «خطبة اليوم» (عنوان ونص وصور تُنسخ إلى ملفات
 *   التطبيق فتبقى) وتبويب «خطب جاهزة» (أربعٌ وعشرون خطبةً مرتَّبة
 *   موثَّقة الشواهد تُعتمد خطبةَ اليوم بضغطة).
 * - **[KhutbahDisplaySheet]** — زر الرئيسية صار للعرض فقط: يعرض
 *   الخطبة المعتمدة كما حُرِّرت في الإعدادات، بلا حقول كتابةٍ إطلاقاً.
 */

/**
 * حافة مصغّرة الصور بالبكسل — مربع العرض ٧٢dp عند كثافة أعلى شائعة
 * (×٣) فلا معنى لفكٍّ أكبر منه — وهي عصا الإصلاح لبلاغ الجلتش:
 * الصور العملاقة لا تدخل الذاكرة أصلاً.
 */
private const val THUMBNAIL_TARGET_PX = 216

/**
 * شاشات مركز الخطبة — إعادة التصميم (alpha62 بلاغ المالك: «اعد تصميم
 * زر اعدادات الخطبه لانه من الواضح ان المشكله مازالت قائمه»): ثلاث
 * إصلاحاتٍ تركيبيةٍ لم تُوقف «الجلتش ثم الإغلاق» على جهاز المالك، فالقسم
 * صار **قائمةً خفيفةً أولًا**: فتح القسم لا يركّب حقولَ كتابةٍ ولا صوراً
 * ولا قائمةَ خطبٍ — ثلاث بطاقاتٍ هادئةٍ فقط، وكل شاشةٍ ثقيلةٍ لا تُركَّب
 * إلا بضغطةٍ عليها، وكل نداءٍ حفظٍ/حذفٍ محصّنٌ بعطلٍ مُسجَّلٍ في دفتر
 * الأعطال مع فتات طريق آخر خطواتٍ — فلو عاد الجلتشُ جاء التقريرُ حاملاً
 * مكانَه بالضبط.
 */
private enum class KhutbahView { MENU, DAY, PRESETS, READING }

/** سقف نص الخطبة المحفوظ — نصٌّ هائلٌ يخنق حقلَ الكتابة فيُقصّ عند الحفظ */
private const val MAX_KHUTBAH_CHARS = 60_000

/** بطاقة قائمة المركز — عنوانٌ ووصفٌ وضغطةٌ تفتح شاشتها */
@Composable
private fun KhutbahMenuCard(
    title: String,
    subtitle: String,
    accent: Color,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(accent.copy(alpha = 0.08f))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = "‹",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = accent,
        )
    }
}

/** مركز تعديل الخطبة — قائمة الشاشات أولًا ثم كل شاشةٍ بمطلوبها */
@Composable
fun KhutbahEditorContent(
    savedTitle: String,
    savedText: String,
    savedImages: List<String>,
    onSetKhutbah: (String, String) -> Unit,
    onImportImage: (android.net.Uri) -> Unit,
    onDeleteImage: (String) -> Unit,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    // شاشة المركز الحالية — القائمة أولًا (alpha62) ففتحُ القسم خفيفٌ
    var view by remember { mutableStateOf(KhutbahView.MENU) }
    var openPreset by remember { mutableStateOf<KhutbahLibrary.Khutbah?>(null) }

    // نصوص التحرير — تبدأ بما هو محفوظ
    var title by remember { mutableStateOf(savedTitle) }
    var text by remember { mutableStateOf(savedText) }

    val pickImage = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        uri?.let {
            CrashLog.breadcrumb("خطبة: اختيار صورة")
            onImportImage(it)
        }
    }

    val accent = MaterialTheme.colorScheme.primary

    LaunchedEffect(Unit) {
        CrashLog.breadcrumb("خطبة: فتح مركز الخطبة")
    }

    Column(Modifier.fillMaxWidth()) {
        when (view) {
            // ===== القائمة — ثلاث بطاقاتٍ هادئةٍ لا تحمل ثقيلاً =====
            KhutbahView.MENU -> {
                KhutbahMenuCard(
                    title = "كتابة خطبة اليوم",
                    subtitle = "عنوان الخطبة ونصها وصورها وحفظها",
                    accent = accent,
                ) {
                    CrashLog.breadcrumb("خطبة: فتح شاشة الكتابة")
                    view = KhutbahView.DAY
                }
                Spacer(Modifier.height(8.dp))
                KhutbahMenuCard(
                    title = "الخطب الجاهزة " +
                        "(${MushafSurahs.arabicDigits(KhutbahLibrary.ORDERED.size)})",
                    subtitle = "اعتمد خطبةً معدةً كخطبة اليوم بضغطة",
                    accent = accent,
                ) {
                    CrashLog.breadcrumb("خطبة: فتح الخطب الجاهزة")
                    view = KhutbahView.PRESETS
                }
                Spacer(Modifier.height(8.dp))
                KhutbahMenuCard(
                    title = "عرض خطبة اليوم",
                    subtitle = "كما ستُقرأ من زر الرئيسية يوم الجمعة",
                    accent = accent,
                ) {
                    CrashLog.breadcrumb("خطبة: فتح شاشة العرض")
                    view = KhutbahView.READING
                }
            }

            // ===== كتابة خطبة اليوم =====
            KhutbahView.DAY -> {
                SheetChip(
                    text = "رجوع إلى القائمة",
                    accent = accent,
                    filled = false,
                    modifier = Modifier,
                    onClick = { view = KhutbahView.MENU },
                )
                Spacer(Modifier.height(10.dp))
                // **جذر جلتش الشاشات الثلاث** (alpha63 — بلاغ المالك: «عند
                // اختيار خطبه او كتابه خطبه او الضغط على زر عرض يحصل جلتش
                // و يتوقف التطبيق عن العمل»): كلٌّ من هذه الشاشات الثلاث كان
                // يركّب عموداً بخاصية verticalScroll **داخل** صفحة القسم
                // التي تتمرّر أصلاً — والقابل للتمرير الداخلي يُقاس بارتفاع
                // لا نهائيٍّ من محتوى التمرير الخارجي فينفجر لحظة التركيب
                // (نفس جذر alpha58). الصفحة تتمرّر عننا — فحُذفت الخاصية
                // المتداخلة من الشاشات الثلاث كلها.
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        singleLine = true,
                        placeholder = {
                            Text(
                                text = "عنوان الخطبة",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accent,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            cursorColor = accent,
                        ),
                        textStyle = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        placeholder = {
                            Text(
                                text = "نص الخطبة…\n\nابدأ بالحمد والصلاة على النبي ﷺ ثم الموضوع ثم الخاتمة",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accent,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            cursorColor = accent,
                        ),
                        textStyle = MaterialTheme.typography.bodySmall,
                        minLines = 6,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(10.dp))
                    KhutbahImagesRow(
                        images = savedImages,
                        editing = true,
                        onAdd = {
                            CrashLog.breadcrumb("خطبة: فتح منتقي الصور")
                            pickImage.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                            )
                        },
                        onDelete = onDeleteImage,
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SheetChip(
                            text = "حفظ الخطبة",
                            filled = true,
                            accent = accent,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                CrashLog.breadcrumb("خطبة: حفظ الخطبة")
                                // التحصين (alpha62): الحفظ محصّنٌ والنصّ
                                // محصورٌ كي لا يخنق نصٌّ هائلٌ فتحَ الشاشة
                                runCatching {
                                    onSetKhutbah(
                                        title.take(MAX_KHUTBAH_CHARS),
                                        text.take(MAX_KHUTBAH_CHARS),
                                    )
                                }.onFailure {
                                    CrashLog.note("خطبة: فشل حفظ الخطبة", it)
                                }
                            },
                        )
                        SheetChip(
                            text = "عرض",
                            filled = false,
                            accent = accent,
                            enabled = text.isNotBlank() || savedImages.isNotEmpty(),
                            modifier = Modifier.weight(1f),
                            onClick = { view = KhutbahView.READING },
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                }
            }

            // ===== الخطب الجاهزة — القائمة أو تفاصيل خطبةٍ منها =====
            KhutbahView.PRESETS -> {
                val preset = openPreset
                if (preset != null) {
                    SheetChip(
                        text = "رجوع إلى القائمة",
                        accent = accent,
                        filled = false,
                        modifier = Modifier,
                        onClick = { openPreset = null },
                    )
                    Spacer(Modifier.height(10.dp))
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = preset.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = accent,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = KhutbahLibrary.fullText(preset),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = MaterialTheme.typography.bodySmall.lineHeight,
                        )
                        Spacer(Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SheetChip(
                                text = "اعتماد كخطبة اليوم",
                                filled = true,
                                accent = accent,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    CrashLog.breadcrumb("خطبة: اعتماد جاهزة")
                                    title = preset.title
                                    text = KhutbahLibrary.fullText(preset)
                                    runCatching {
                                        onSetKhutbah(preset.title, KhutbahLibrary.fullText(preset))
                                    }.onFailure {
                                        CrashLog.note("خطبة: فشل اعتماد جاهزة", it)
                                    }
                                    view = KhutbahView.DAY
                                    openPreset = null
                                },
                            )
                            SheetChip(
                                text = "نسخ النص",
                                filled = false,
                                accent = accent,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    clipboard.setText(AnnotatedString(KhutbahLibrary.fullText(preset)))
                                },
                            )
                        }
                        Spacer(Modifier.height(28.dp))
                    }
                } else {
                    SheetChip(
                        text = "رجوع إلى القائمة",
                        accent = accent,
                        filled = false,
                        modifier = Modifier,
                        onClick = { view = KhutbahView.MENU },
                    )
                    Spacer(Modifier.height(10.dp))
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().height(420.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 8.dp),
                    ) {
                        items(KhutbahLibrary.ORDERED) { khutbah ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable {
                                        CrashLog.breadcrumb("خطبة: فتح جاهزة")
                                        openPreset = khutbah
                                    }
                                    .padding(horizontal = 10.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(accent.copy(alpha = 0.13f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = MushafSurahs.arabicDigits(khutbah.id),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = accent,
                                    )
                                }
                                Column(Modifier.weight(1f).padding(start = 10.dp)) {
                                    Text(
                                        text = khutbah.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        text = (khutbah.body.firstOrNull() ?: "").take(64) + "…",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(0.5.dp)
                                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.16f)),
                            )
                        }
                    }
                }
            }

            // ===== عرض خطبة اليوم — كما ستُقرأ من زر الرئيسية =====
            KhutbahView.READING -> {
                SheetChip(
                    text = "رجوع إلى القائمة",
                    accent = accent,
                    filled = false,
                    modifier = Modifier,
                    onClick = { view = KhutbahView.MENU },
                )
                Spacer(Modifier.height(6.dp))
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (title.isNotBlank()) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = accent,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                    if (savedImages.isNotEmpty()) {
                        KhutbahImagesRow(
                            images = savedImages,
                            editing = false,
                            onAdd = {},
                            onDelete = {},
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                    Text(
                        text = text.ifBlank { "لم تُكتب خطبة اليوم بعد — عد إلى الكتابة واكتبها" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(28.dp))
                }
            }
        }
    }
}

/**
 * لوحة عرض خطبة الجمعة — زر الرئيسية (يوم الجمعة فقط) للعرض فقط:
 * الخطبة التي حُرِّرت واعتمدت من قسم «خطبة الجمعة» في الإعدادات تُقرأ
 * هنا كاملةً بعنوانها وصورها ونصها، بلا حقول كتابةٍ إطلاقاً.
 */
@Composable
fun KhutbahDisplaySheet(
    savedTitle: String,
    savedText: String,
    savedImages: List<String>,
    onDismiss: () -> Unit,
) {
    val accent = MaterialTheme.colorScheme.primary

    Box(modifier = Modifier.fillMaxSize()) {
        // العاتم — ضغطة تقفل
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(onClick = onDismiss),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxSize(fraction = 0.88f)
                .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
                .background(MaterialTheme.colorScheme.surface),
        ) {
            // مقبض السحب
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 10.dp)
                    .size(width = 44.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
            )

            // الترويسة: الإكس أعلى اليمين ثم العنوان
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.14f))
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "إغلاق",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                        modifier = Modifier.size(16.dp),
                    )
                }
                Column(Modifier.weight(1f).padding(start = 10.dp)) {
                    Text(
                        text = "خطبة الجمعة",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "الخطبة المعتمدة من الإعدادات — للعرض فقط",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
            ) {
                if (savedTitle.isNotBlank()) {
                    Text(
                        text = savedTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = accent,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(12.dp))
                }
                if (savedImages.isNotEmpty()) {
                    KhutbahImagesRow(
                        images = savedImages,
                        editing = false,
                        onAdd = {},
                        onDelete = {},
                    )
                    Spacer(Modifier.height(12.dp))
                }
                val empty = savedTitle.isBlank() && savedText.isBlank() && savedImages.isEmpty()
                Text(
                    text = if (empty) {
                        "لم تُجهَّز خطبة الجمعة بعد — جهّزها من الإعدادات ← " +
                            "خطبة الجمعة: اكتبها أو اعتمد خطبةً جاهزة، ثم تُعرض هنا يوم الجمعة."
                    } else {
                        savedText.ifBlank { "الخطبة صورٌ فقط — انظر الصور أعلاه" }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (empty) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
                Spacer(Modifier.height(28.dp))
            }
        }
    }
}

/** صفّ صور الخطبة — مصغّرات من ملفات التطبيق، وإضافة/حذف في التحرير */
@Composable
internal fun KhutbahImagesRow(
    images: List<String>,
    editing: Boolean,
    onAdd: () -> Unit,
    onDelete: (String) -> Unit,
) {
    val context = LocalContext.current
    Column {
        Text(
            text = if (editing) "صور الخطبة" else "",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                // الجذر المُصلَح (alpha58 — بلاغ المالك: «زر قسم اعداد خطبه
                // الجمعه لا يفتح يحصل جلتش و التطبيق يخرج بمجرد الضغط»):
                // كان verticalScroll فصفُّ الصور تُقاس بارتفاعٍ لا نهائيٍّ
                // داخل عمود القسم القابل للتمرير عمودياً — فIllegalStateException
                // تنفجر لحظة فتح القسم. الصفُّ أفقيٌّ بطبعه فالتمرير أفقي:
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            images.take(6).forEach { fileName ->
                var bmp by remember(fileName) { mutableStateOf<ImageBitmap?>(null) }
                LaunchedEffect(fileName) {
                    bmp = withContext(Dispatchers.IO) {
                        runCatching {
                            // الجذر المُصلَح (alpha59 — بلاغ المالك: «زر
                            // قسم اعداد خطبه الجمعه مازال يحدث جلتش و
                            // يخرج التطبيق»): الفك الكامل لصور كاميرا
                            // ١٢–٤٨ ميغابكسل لأجل مربع ٧٢dp كان ينفجر
                            // في الذاكرة — فالآن فكٌّ مصغَّر مسبقاً
                            // (خطوة قوى ٢ من حساب KhutbahLibrary النقي)
                            // فالمربع يستهلك كيلوبايتات لا مئات
                            // الميغابايتات.
                            // وتحصين alpha61: RGB_565 يقصّ ذاكرة
                            // المصغّر للنصف، وAny فشلٍ (تالف/نفاد ذاكرة)
                            // يرجع null فيظهر المربع الفارغ ولا يُهدم
                            // التركيب أبداً.
                            val file = File(File(context.filesDir, "khutbah_images"), fileName)
                            val bounds = BitmapFactory.Options().apply {
                                inJustDecodeBounds = true
                            }
                            BitmapFactory.decodeFile(file.absolutePath, bounds)
                            val sample = KhutbahLibrary.thumbnailSampleSize(
                                bounds.outWidth, bounds.outHeight,
                                THUMBNAIL_TARGET_PX,
                            )
                            BitmapFactory.decodeFile(
                                file.absolutePath,
                                BitmapFactory.Options().apply {
                                    inSampleSize = sample
                                    inPreferredConfig = android.graphics.Bitmap.Config.RGB_565
                                },
                            )
                        }.getOrNull()?.asImageBitmap()
                    }
                }
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(12.dp),
                        ),
                ) {
                    val image = bmp
                    if (image != null) {
                        androidx.compose.foundation.Image(
                            bitmap = image,
                            contentDescription = "صورة الخطبة",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)),
                        )
                    }
                    if (editing) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(3.dp)
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.55f))
                                .clickable { onDelete(fileName) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "حذف الصورة",
                                tint = Color.White,
                                modifier = Modifier.size(11.dp),
                            )
                        }
                    }
                }
            }
            if (editing) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
                        .clickable(onClick = onAdd),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "إضافة صورة",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }
    }
}

/** رقاقة زر صغيرة داخل لوحة الخطبة — بلون الثيم */
@Composable
internal fun SheetChip(
    text: String,
    filled: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (filled) accent else MaterialTheme.colorScheme.outline.copy(alpha = 0.10f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (filled) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 0.85f else 0.35f)
            },
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
        )
    }
}
