package com.sawan4art.noor.ui.home

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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

/** محرر الخطبة — جسمُ القسم داخل الإعدادات بلا أغلفة اللوحة */
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

    // التبويب: خطبة اليوم / خطب جاهزة — وتفاصيل الخطبة الجاهزة المفتوحة
    var tab by remember { mutableIntStateOf(0) }
    var openPreset by remember { mutableStateOf<KhutbahLibrary.Khutbah?>(null) }

    // نصوص التحرير — تبدأ بما هو محفوظ
    var title by remember { mutableStateOf(savedTitle) }
    var text by remember { mutableStateOf(savedText) }
    var reading by remember { mutableStateOf(false) }

    val pickImage = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri -> uri?.let(onImportImage) }

    val accent = MaterialTheme.colorScheme.primary

    Column(Modifier.fillMaxWidth()) {
        // تبويب الخطبة/الجاهزة — أو زر الرجوع من تفاصيل الجاهزة
        if (openPreset != null) {
            SheetChip(
                text = "رجوع إلى القائمة",
                accent = accent,
                filled = false,
                modifier = Modifier,
                onClick = { openPreset = null },
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SheetChip(
                    text = "خطبة اليوم",
                    filled = tab == 0,
                    accent = accent,
                    modifier = Modifier.weight(1f),
                    onClick = { tab = 0 },
                )
                SheetChip(
                    text = "خطب جاهزة (${MushafSurahs.arabicDigits(KhutbahLibrary.ORDERED.size)})",
                    filled = tab == 1,
                    accent = accent,
                    modifier = Modifier.weight(1f),
                    onClick = { tab = 1 },
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        when {
            // ===== تفاصيل خطبة جاهزة =====
            openPreset != null -> {
                val preset = openPreset!!
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                ) {
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
                                title = preset.title
                                text = KhutbahLibrary.fullText(preset)
                                onSetKhutbah(preset.title, KhutbahLibrary.fullText(preset))
                                tab = 0
                                openPreset = null
                                reading = true
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
            }

            // ===== الخطب الجاهزة — مرتَّبة =====
            tab == 1 -> {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().height(420.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 8.dp),
                ) {
                    items(KhutbahLibrary.ORDERED) { khutbah ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { openPreset = khutbah }
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
                                    text = khutbah.body.first().take(64) + "…",
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

            // ===== خطبة اليوم — عرض القراءة أو التحرير =====
            reading -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        SheetChip(
                            text = "رجوع للتحرير",
                            filled = false,
                            accent = accent,
                            modifier = Modifier.weight(1f),
                            onClick = { reading = false },
                        )
                    }
                    Spacer(Modifier.height(6.dp))
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
                        text = text.ifBlank { "لم تُكتب خطبة اليوم بعد — عد إلى التحرير واكتبها" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(28.dp))
                }
            }

            // ===== التحرير: العنوان والنص والصور والحفظ =====
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                ) {
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
                                onSetKhutbah(title, text)
                            },
                        )
                        SheetChip(
                            text = "عرض",
                            filled = false,
                            accent = accent,
                            enabled = text.isNotBlank() || savedImages.isNotEmpty(),
                            modifier = Modifier.weight(1f),
                            onClick = { reading = true },
                        )
                    }
                    Spacer(Modifier.height(20.dp))
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
                .verticalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            images.take(6).forEach { fileName ->
                var bmp by remember(fileName) { mutableStateOf<ImageBitmap?>(null) }
                LaunchedEffect(fileName) {
                    bmp = withContext(Dispatchers.IO) {
                        runCatching {
                            BitmapFactory.decodeFile(
                                File(File(context.filesDir, "khutbah_images"), fileName).absolutePath,
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
