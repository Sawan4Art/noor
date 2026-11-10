package com.sawan4art.noor.widget

/**
 * الأشكال الستة الجاهزة للويدجت — بلاغ المالك 2026-11-11:
 *
 * «عايز ٦ اشكال للويدجت تكون جاهزة للمستخدم ممكن يختار منهم و يكون
 * اشكالهم ظاهره في قائمه التطبيقات المصغره في الهاتف علشان المستخدم
 * يقدر يفرق بينهم».
 *
 * فالعقد هنا: ستة أشكالٍ معبّأةٍ مسبقاً، كلٌّ منها تخصيصٌ كاملٌ
 * [WidgetCustomization] يُزرَع للويدجت المنصوبة من قائمة النظام في
 * **أول رسمٍ لها** (قبل أي تخصيصٍ يدوي) — فالمستخدم يختار شكلاً من
 * القائمة فيجده حياً فوراً، ثم يحرر تفاصيله من المحرر كيفما شاء.
 *
 * وأشكالُها الستة ظاهرةٌ في قائمة ويدجتات النظام نفسها: كل شكلٍ
 * مستقبِلٌ مستقل (NoorWidgetPreset1..6) بمعاينةٍ مرسومةٍ باسمه
 * (widget_preview_1..6) فيُميَّزون بالعين قبل التثبيت — وهو جوهر
 * البلاغ: «علشان المستخدم يقدر يفرق بينهم».
 */
data class WidgetPreset(
    /** رقم الشكل ١..٦ — معرّف المستقبِل والمعاينة */
    val index: Int,
    /** اسمه في قائمة ويدجتات النظام وبطاقة المحرر */
    val name: String,
    /** سطرٌ يصفه في بطاقة المحرر */
    val description: String,
    /** التخصيص الكامل الذي يُزرَع للويدجت الجديدة من هذا الشكل */
    val customization: WidgetCustomization,
) {
    companion object {
        /** معرّف المفتاح فيSharedPreferences لكل ويدجتٍ من شكلٍ ما — يُبنى من الرقم */
        fun seedKeyFor(index: Int): String = "widget_preset_seed_$index"
    }
}

/**
 * الأشكال الستة المعتمدة — كلٌّ بنيّةٍ ظاهرةٍ مختلفةٍ كي يفرّق
 * المستخدم بينهم من المعاينة في قائمة النظام:
 *
 * ١) «افتراضي» — كل العناصر والساعة الرقمية وإطار بلون التطبيق.
 * ٢) «الساعة التناظرية» — الساعة التناظرية المرسومة في القلب.
 * ٣) «مختصر» — الوقت والقادمة والعدّاد والمواقيت بلا تواريخٍ ولا ترويسة.
 * ٤) «التواريخ» — التاريخان بالصيغتين الكاملتين مع القادمة والمواقيت.
 * ٥) «ليلي» — مظهرٌ داكنٌ مثبّتٌ بإطارٍ زمرديّ.
 * ٦) «فاتح» — مظهرٌ فاتحٌ مثبّتٌ بإطارٍ عنبريّ.
 */
object WidgetPresets {

    val ALL: List<WidgetPreset> = listOf(
        WidgetPreset(
            index = 1,
            name = "افتراضي",
            description = "كل العناصر والساعة الرقمية — كما صُمّمت الويدجت",
            customization = WidgetCustomization.DEFAULT,
        ),
        WidgetPreset(
            index = 2,
            name = "الساعة التناظرية",
            description = "الساعة التناظرية المرسومة بعقاربها الحيّة",
            customization = WidgetCustomization.DEFAULT.copy(
                clockStyle = WidgetCustomization.ClockStyle.ANALOG,
            ),
        ),
        WidgetPreset(
            index = 3,
            name = "مختصر",
            description = "الوقت والقادمة والعدّاد والمواقيت — بلا تواريخ",
            customization = WidgetCustomization.DEFAULT.copy(
                showBrand = false,
                showHijri = false,
                showGregorian = false,
                elementOrder = WidgetCustomization.normalizeOrder(
                    listOf(
                        WidgetCustomization.ELEMENT_CLOCK,
                        WidgetCustomization.ELEMENT_NEXT,
                        WidgetCustomization.ELEMENT_COUNTDOWN,
                        WidgetCustomization.ELEMENT_GRID,
                    ),
                ),
            ),
        ),
        WidgetPreset(
            index = 4,
            name = "التواريخ",
            description = "التاريخان الهجري والميلادي مع المواقيت والقادمة",
            customization = WidgetCustomization.DEFAULT.copy(
                showClock = false,
                showCountdown = false,
                hijriFormat = 1,
                gregFormat = 1,
                elementOrder = WidgetCustomization.normalizeOrder(
                    listOf(
                        WidgetCustomization.ELEMENT_HEADER,
                        WidgetCustomization.ELEMENT_HIJRI,
                        WidgetCustomization.ELEMENT_GREG,
                        WidgetCustomization.ELEMENT_NEXT,
                        WidgetCustomization.ELEMENT_GRID,
                    ),
                ),
            ),
        ),
        WidgetPreset(
            index = 5,
            name = "ليلي",
            description = "مظهرٌ داكنٌ مثبّتٌ بإطارٍ زمرديّ",
            customization = WidgetCustomization.DEFAULT.copy(
                theme = WidgetCustomization.Theme.DARK,
                frameColor = "#2FA48C",
            ),
        ),
        WidgetPreset(
            index = 6,
            name = "فاتح",
            description = "مظهرٌ فاتحٌ مثبّتٌ بإطارٍ عنبريّ",
            customization = WidgetCustomization.DEFAULT.copy(
                theme = WidgetCustomization.Theme.LIGHT,
                frameColor = "#C98A3D",
            ),
        ),
    )

    /** الشكل برقمه — والغريب يعود للأول بأمان */
    fun byIndex(index: Int): WidgetPreset =
        ALL.firstOrNull { it.index == index } ?: ALL.first()

    /** عدد الأشكال — ستة بأمر المالك */
    const val COUNT = 6
}
