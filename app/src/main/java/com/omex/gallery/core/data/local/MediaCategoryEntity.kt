package com.omex.gallery.core.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "media_categories")
data class MediaCategoryEntity(
    @PrimaryKey val categoryId: String,
    val nameArabic: String,
    val iconName: String? = null
)
