package com.sawan4art.noor.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * الخطوط — خط النظام (Noto Naskh Arabic على أجهزة أندرويد) أوضح choice للمرحلة
 * الأولى، وخط المصحف (أميري) يُضمّن مع المرحلة الثانية للمصحف.
 *
 * alpha39 (بلاغ المالك: «عايز اكبر النصوص شويه»): كل المقاسات رُفعت درجةً
 * إلى درجتين مع ارتفاع السطور — والأهم أن مقاييس التطبيق **ثابتة بذاتها**:
 * NoorApp يثبّت fontScale على قيمة ثابتة (١.٣ منذ alpha42) فمقياس خط
 * الهاتف من إعداداته لا يضرب هذه الأرقام إطلاقاً، فالتكبير هنا هو الحجم
 * الفعلي الذي يراه المستخدم دائماً.
 */
val NoorTypography = Typography(
    displaySmall = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 46.sp,
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 38.sp,
    ),
    /** عناوين رؤوس الصفحات — تُعرض داخل إطار NoorPageTitle في كل الشاشات */
    headlineSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 25.sp,
        lineHeight = 35.sp,
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 32.sp,
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 27.sp,
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 29.sp,
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 25.sp,
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 19.sp,
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 21.sp,
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
)
