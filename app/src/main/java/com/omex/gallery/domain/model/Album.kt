package com.omex.gallery.domain.model

import androidx.compose.runtime.Immutable

enum class AlbumType {
    CAMERA, SCREENSHOTS, DOWNLOADS, VIDEOS, FAVORITES, FOLDER
}

@Immutable
data class Album(
    val id: String,
    val title: String,
    val coverUri: String?,
    val itemCount: Int,
    val albumType: AlbumType,
    val folderPath: String? = null
)
