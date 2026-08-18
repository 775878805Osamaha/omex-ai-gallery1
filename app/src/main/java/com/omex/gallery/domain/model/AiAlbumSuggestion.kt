package com.omex.gallery.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class AiAlbumSuggestion(
    val id: String,
    val themeKey: String,
    val title: String,
    val titleArabic: String,
    val description: String,
    val descriptionArabic: String,
    val iconType: String,
    val sampleCoverUris: List<String>,
    val matchingMediaIds: List<Long>,
    val mediaCount: Int,
    val confidenceScore: Float = 0.95f,
    val matchedTags: List<String> = emptyList()
)
