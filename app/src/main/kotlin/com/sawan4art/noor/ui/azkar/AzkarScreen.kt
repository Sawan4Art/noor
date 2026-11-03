package com.sawan4art.noor.ui.azkar

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sawan4art.noor.domain.azkar.AzkarContent
import com.sawan4art.noor.domain.azkar.AzkarGroup
import com.sawan4art.noor.domain.azkar.AzkarLibrary
import com.sawan4art.noor.domain.azkar.AzkarMath
import com.sawan4art.noor.domain.tasbih.MisbahaMath
import com.sawan4art.noor.ui.icons.NoorIcons
import com.sawan4art.noor.ui.theme.NoorPageTitle

/**
 * تبويب الأذكار — توجيه المالك alpha29 («خفف الاذكار و اعرض الاذكار الخاصه
 * بكل قسم يعني اذكار المساء يبقى فيها فقط اذكار المساء .. عايز اضيف حتى ٣٠
 * نوع من الاذكار ان وجد»):
 *
 * - **الصلاة على النبي ﷺ** بطاقةٌ كبرى بعرض الشاشة تفتح عدّادها السائل الحرّ.
 * - **تسعة وعشرون قسماً** في شبكة صفَّين — كل قسم صفحته المستقلة بأذكاره
 *   فقط (السفر والثياب الجديدة والوضوء والخلاء والمطر والرياح...)،
 *   بمحتوى خفيف موثق ومقدّر تقدّم يوميّ يتجدد كل يوم.
 *
 * التنقل داخلي بالروابط النصية (الرئيسية/العدّاد/مجموعة) والزر
 * الرجوع يعود خطوةً خطوة — بلا أي مساهمة من معالج NoorApp (المعطّل
 * لهذا التبويب كي لا يقفز للرئيسية فوق التنقل الداخلي).
 */
@Composable
fun AzkarScreen(
    onExitTab: () -> Unit,
    viewModel: AzkarViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // الروابط الداخلية: index ← praise / المعرّف النصي للمجموعة.
    // alpha44 إصلاح عطل «أذكار دخول البيت لا تعمل»: المرسل السابق كان
    // «home» نفسه معرّف قسم أذكار البيت — ففتح البطاقة يعود للرئيسية
    // ولا تُفتح صفحتها أبداً! المرسل الآن «index» لا يتصادم مع أي قسم.
    var route by rememberSaveable { mutableStateOf("index") }

    BackHandler(enabled = route != "index") { route = "index" }
    // الرئيسية داخل التبويب: الرجوع يخرج للتبويب السابق (المواقيت)
    BackHandler(enabled = route == "index") { onExitTab() }

    when {
        route == "praise" -> PraiseScreen(
            viewModel = viewModel,
            onBack = { route = "index" },
        )
        route != "index" -> {
            // alpha45: الأقسام المضافة من المستخدم تُفتح كسائر الأقسام —
            // والقسم الافتراضي المحذوف يعود به للفهرس تلقائياً
            val defaultGroup = AzkarContent.GROUPS.firstOrNull { it.id == route }
            val openedGroup = when {
                defaultGroup != null && route !in state.deletedGroups -> defaultGroup
                else -> state.customGroups.firstOrNull { it.id == route }?.let {
                    AzkarGroup(it.id, it.title, it.subtitle, emptyList())
                }
            }
            if (openedGroup == null) {
                LaunchedEffect(route) { route = "index" }
                AzkarHome(
                    state = state,
                    onOpenPraise = { route = "praise" },
                    onOpenGroup = { id -> route = id },
                    onAddGroup = { viewModel.addCustomGroup(it) },
                    onDeleteGroup = { viewModel.deleteGroup(it) },
                    onRestoreGroups = { viewModel.restoreDefaultGroups() },
                )
            } else {
                GroupScreen(
                    viewModel = viewModel,
                    group = openedGroup,
                    onBack = { route = "index" },
                )
            }
        }
        else -> AzkarHome(
            state = state,
            onOpenPraise = { route = "praise" },
            onOpenGroup = { id -> route = id },
            onAddGroup = { viewModel.addCustomGroup(it) },
            onDeleteGroup = { viewModel.deleteGroup(it) },
            onRestoreGroups = { viewModel.restoreDefaultGroups() },
        )
    }
}

/** أيقونة القسم — خريطة الأقسام التسعة والعشرين */
private fun groupIcon(group: AzkarGroup): ImageVector = when (group.id) {
    "morning", "wake_up" -> NoorIcons.Sun
    "evening", "crescent" -> NoorIcons.Moon
    "sleep" -> NoorIcons.Bed
    "after_prayer", "qunut" -> NoorIcons.PrayerTimes
    "mosque" -> NoorIcons.Mosque
    "wudu", "khilaa", "rain" -> NoorIcons.Drop
    "home" -> Icons.Filled.Home
    "travel" -> NoorIcons.Travel
    "food" -> NoorIcons.Food
    "clothes" -> NoorIcons.Shirt
    "wind" -> NoorIcons.Wind
    "distress", "karb", "marriage" -> Icons.Filled.Favorite
    "sick", "deceased", "ruqya", "istighfar", "anger", "hasanah" -> Icons.Filled.Star
    "sneeze", "newborn" -> Icons.Filled.Person
    "market" -> Icons.Filled.ShoppingCart
    "istikhara" -> NoorIcons.Qibla
    else -> Icons.Filled.Star
}

/**
 * قائمة التبويب: بطاقة الصلاة على النبي بعرض الشاشة ثم شبكة الأقسام
 * — **مكتبة الأقسام الحيّة (alpha45 — بلاغ المالك: «زر زائد يجب ان
 * يكون ايضا في داخل صفحه الاذكار نفسها لاضافه كارت اذكار جديد و كذالك
 * زر مسح يكون على كل كرت من الاذكار»)**: زر «+» بجانب العنوان يفتح
 * حوار إنشاء قسمٍ جديد ببطاقته الخاصة، وكل بطاقة قسم فيها زر حذف —
 * المضاف يُمحى نهائياً والافتراضي يُسجَّل محذوفاً فيظهر شريط
 * الاسترجاع أعلى الشبكة ويعيده بأذكاره كلها.
 */
@Composable
private fun AzkarHome(
    state: AzkarState,
    onOpenPraise: () -> Unit,
    onOpenGroup: (String) -> Unit,
    onAddGroup: (String) -> Unit,
    onDeleteGroup: (String) -> Unit,
    onRestoreGroups: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    var showAddGroupDialog by rememberSaveable { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.background)
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) {
                // alpha39: العنوان داخل إطار NoorPageTitle (توجيه المالك:
                // «عايز كل عناوين الصفحات تكون داخل اطار متماشي مع تصميم التطبيق»)
                NoorPageTitle(text = "الأذكار")
            }
            // زر إنشاء قسم أذكار جديد — في صفحة الأذكار نفسها (alpha45)
            IconButton(onClick = { showAddGroupDialog = true }) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = "إضافة قسم أذكار جديد",
                    tint = scheme.primary,
                    modifier = Modifier.size(28.dp),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = "ثلاثون قسماً — لكل قسمٍ صفحته وأذكاره الخاصة من السنة الصحيحة",
            style = MaterialTheme.typography.labelMedium,
            color = scheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(14.dp))

        // ===== شريط استرجاع الأقسام الأصلية — يظهر ما دام في حذفٌ سابق =====
        if (state.deletedGroups.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = scheme.secondaryContainer,
                contentColor = scheme.onSecondaryContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onRestoreGroups),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "استرجاع الأقسام الأصلية المحذوفة (" +
                            MisbahaMath.arabicNumber(state.deletedGroups.size.toLong()) + ")",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            // ===== بطاقة الصلاة على النبي — ذكرٌ مستقل بعدّاد حرّ، بعرض الشاشة =====
            item(span = { GridItemSpan(2) }) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenPraise),
                ) {
                    Column(
                        modifier = Modifier
                            .background(
                                brush = Brush.linearGradient(
                                    listOf(scheme.primaryContainer, scheme.secondaryContainer),
                                ),
                            )
                            .padding(18.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        color = scheme.surface.copy(alpha = 0.45f),
                                        shape = RoundedCornerShape(14.dp),
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Filled.Favorite,
                                    contentDescription = null,
                                    tint = scheme.primary,
                                    modifier = Modifier.size(26.dp),
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = "الصلاة على النبي ﷺ",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = scheme.onPrimaryContainer,
                                )
                                Text(
                                    text = AzkarContent.PRAISE_VIRTUE,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = scheme.onPrimaryContainer.copy(alpha = 0.8f),
                                )
                            }
                        }
                        if (state.praiseCount > 0) {
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = "عدّك الحالي " + MisbahaMath.arabicNumber(state.praiseCount.toLong()),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = scheme.primary,
                            )
                        }
                    }
                }
            }

            // ===== شبكة الأقسام — بمحتوى كل قسمٍ الفعلي (بعد حذف
            // المستخدم وإضافاته — alpha44) والأقسام المضافة بينها
            // ببطاقاتها (alpha45) =====
            items(
                AzkarLibrary.effectiveGroups(
                    AzkarContent.GROUPS,
                    state.customGroups,
                    state.deletedGroups,
                ),
                key = { it.id },
            ) { group ->
                val effective = AzkarLibrary.effectiveGroup(group, state.customs, state.deleted)
                GroupCard(
                    group = effective,
                    icon = groupIcon(group),
                    progress = AzkarMath.groupProgress(state.doneToday, effective),
                    isCustom = AzkarLibrary.isCustomGroup(group.id),
                    onOpen = { onOpenGroup(group.id) },
                    onDelete = { onDeleteGroup(group.id) },
                )
            }

            item(span = { GridItemSpan(2) }) {
                Spacer(Modifier.height(100.dp)) // مساحة شريط التنقل الزجاجي — دائماً
            }
        }
    }

    if (showAddGroupDialog) {
        AddGroupDialog(
            onDismiss = { showAddGroupDialog = false },
            onConfirm = { title ->
                onAddGroup(title)
                showAddGroupDialog = false
            },
        )
    }
}

/**
 * بطاقة قسم أذكار — أيقونته وعنوانه ومقدّر تقدّم اليوم، وزر حذفٍ
 * في زاويتها (alpha45): القسم المضاف يُمحى نهائياً والافتراضي
 * يُسجَّل محذوفاً لاسترجاعه من الشريط أعلى الشبكة.
 */
@Composable
private fun GroupCard(
    group: AzkarGroup,
    icon: ImageVector,
    progress: AzkarMath.GroupProgress,
    isCustom: Boolean,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
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
            .clickable(onClick = onOpen),
    ) {
        Box {
            Column(Modifier.padding(14.dp)) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(
                            brush = Brush.linearGradient(
                                listOf(
                                    scheme.primaryContainer.copy(alpha = 0.75f),
                                    scheme.secondaryContainer.copy(alpha = 0.75f),
                                ),
                            ),
                            shape = RoundedCornerShape(14.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = scheme.primary,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = group.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = scheme.onSurface,
                    maxLines = 1,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = MisbahaMath.arabicNumber(progress.doneCount.toLong()) + "/" +
                        MisbahaMath.arabicNumber(progress.total.toLong()) +
                        if (progress.allDone) " ✓" else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (progress.allDone) scheme.primary else scheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress.fraction.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                    color = scheme.primary,
                    trackColor = scheme.surfaceVariant,
                )
            }
            // زر مسح البطاقة — في الزاوية المقابلة للأيقونة، واضحٌ على كل كرت
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(32.dp),
            ) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = if (isCustom) {
                        "حذف القسم نهائياً"
                    } else {
                        "حذف القسم — يمكن استرجاعه لاحقاً"
                    },
                    tint = scheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

/** حوار إنشاء قسم أذكار جديد — عنوانٌ فقط والباقي من داخل صفحته (alpha45) */
@Composable
private fun AddGroupDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String) -> Unit,
) {
    var title by rememberSaveable { mutableStateOf("") }
    val valid = title.trim().isNotEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "قسم أذكار جديد",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("اسم القسم") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "يُنشأ القسم ببطاقة خاصة في شبكة الأذكار، وأذكاره تُضاف من داخل صفحته بزر «+».",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(title) },
                enabled = valid,
            ) {
                Text("إنشاء", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        },
    )
}
