package com.omex.gallery.core.data.local

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "media_item_category_cross_ref",
    primaryKeys = ["mediaId", "categoryId"],
    indices = [
        Index("mediaId"),
        Index("categoryId")
    ]
)
data class MediaItemCategoryCrossRef(
    val mediaId: Long,
    val categoryId: String
)
