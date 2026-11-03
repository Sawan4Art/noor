package com.sawan4art.noor.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * الخطوط — خط النظام (Noto Naskh Arabic على أجهزة أندرويد) أوضح choice للمرحلة
 * الأولى، وخط المصحف (أميري) يُضمّن مع المرحلة الثانية للمصحف.
 *
 * **السلم الذهبي** (alpha43 — بلاغ المالك: «راجع كل الخطوط و الأيقونات في
 * التطبيق كله لان في حاجات حجمها مش متناسق .. راعي النسبه الذهبيه و كمان
 * راعي ان في ناس نظرها ضعيف»): كل الأدوار الخمسة عشر **معرّفة هنا صراحةً**
 — لا دور يرجع لافتراضي Material متشتت (كان labelSmall يرجع ١١sp صغيرة
 * عن القراءة!). السلم: ١٣ · ١٥ · ١٧ · ٢٢ · ٢٨ · ٣٦ · ٤٦ · ٥٧ — خطوات
 * رئيسية بجذر النسبة الذهبية (φ≈١٫٦١٨ فكل خطوتين متتاليتين تقريباً φ:
 * ١٣→٢٢ و١٧→٢٨ و٢٢→٣٦ و٢٨→٤٦ و٣٦→٥٧)، ودرجة وسيطة ١٥ بين ١٣ و١٧
 * للتسميات البارزة. وارتفاع سطور المتن = φ × المقاس (١٧/٢٨ و١٥/٢٤)
 * كي يتنفس النص العربي بحركاته.
 *
 * لا يزال NoorApp يثبّت fontScale على قيمة ثابتة (١.٤ منذ alpha43) فمقياس
 * خط الهاتف من إعداداته لا يضرب هذه الأرقام إطلاقاً، فالتكبير هنا هو
 * الحجم الفعلي الذي يراه المستخدم دائماً — وأصغر مقاس في التطبيق يُرى
 * بحجم ١٨ فوق مقاس الأساس، مقروءٌ لنظرٍ ضعيفٍ بلا مجهود.
 */
val NoorTypography = Typography(
    // عدّادات البطل — المسبحة والأذكار (مقاس البطل الكبير)
    displayLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 57.sp,
        lineHeight = 68.sp,
    ),
    displayMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 46.sp,
        lineHeight = 58.sp,
    ),
    // اسم الصلاة القادمة في هيرو الرئيسية
    displaySmall = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 48.sp,
    ),
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 46.sp,
        lineHeight = 58.sp,
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 46.sp,
    ),
    /** عناوين رؤوس الصفحات — تُعرض داخل إطار NoorPageTitle في كل الشاشات */
    headlineSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 38.sp,
    ),
    // نص القسم المشكول داخل نقطة المسبحة
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 31.sp,
    ),
    // عناوين البطاقات والصفوف
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 25.sp,
    ),
    // عناوين البطاقات الصغيرة والتنبيهات
    titleSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 22.sp,
    ),
    // المتن الافتراضي — ارتفاع السطر φ×١٧
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 28.sp,
    ),
    // المتن الثانوي — نصوص البطاقات والشروح
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 24.sp,
    ),
    // الحواشي والميتاداتا (كتاب الحديث ورقمه..)
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 21.sp,
    ),
    // أزرار وتسميات بارزة
    labelLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 22.sp,
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
    // رقائق مؤشر الأقسام في المسبحة وما شابه — كانت ترجع ١١sp افتراضياً!
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
)
