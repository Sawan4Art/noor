package com.sawan4art.noor.data.hadith

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * كتب المكتبة العشرة — أسماؤها العربية وترتيبها في العرض.
 * المعرفات هي معرفات المصدر (sunnah.com عبر hadith-json v1.2.0)
 * حتى تبقى البيانات قابلة للتحديث بنفس السكريبت دون تحويل.
 */
@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey val id: Long,
    val title: String,
    val author: String,
    val sortOrder: Int,
)

/** حديث واحد بنصّه المُشكَّل الكامل — كما في المصدر حرفياً */
@Entity(tableName = "hadiths")
data class HadithEntity(
    @PrimaryKey val id: Long,
    val bookId: Long,
    val numberInBook: Int,
    /** اسم الكتاب/الباب الذي فيه الحديث — فارغ في الكتب ذات الباب الواحد */
    val chapter: String,
    val text: String,
)
