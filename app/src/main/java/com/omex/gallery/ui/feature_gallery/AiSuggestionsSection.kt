package com.omex.gallery.ui.feature_gallery

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.omex.gallery.R
import com.omex.gallery.domain.model.AiAlbumSuggestion
import com.omex.gallery.ui.theme.AmberAccent
import com.omex.gallery.ui.theme.CyanAccent
import com.omex.gallery.ui.theme.ObsidianBg
import com.omex.gallery.ui.theme.SurfaceCard
import com.omex.gallery.ui.theme.SurfaceDark
import com.omex.gallery.ui.theme.TextMutedDark
import com.omex.gallery.ui.theme.TextPrimaryDark

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiSuggestionsSection(
    suggestions: List<AiAlbumSuggestion>,
    onExploreSuggestion: (AiAlbumSuggestion) -> Unit,
    onSaveAsAlbum: (AiAlbumSuggestion) -> Unit,
    onDismissSuggestion: (String) -> Unit,
    onCreateAllSuggestions: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (suggestions.isEmpty()) return

    var isExpanded by remember { mutableStateOf(true) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("ai_suggestions_section"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark.copy(alpha = 0.95f)),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            brush = Brush.linearGradient(
                colors = listOf(AmberAccent.copy(alpha = 0.6f), CyanAccent.copy(alpha = 0.4f))
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .testTag("ai_suggestions_header_toggle"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(AmberAccent, CyanAccent)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = ObsidianBg,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(R.string.ai_suggestions_title),
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                ),
                                color = TextPrimaryDark
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = AmberAccent.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(0.5.dp, AmberAccent.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = "${suggestions.size}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AmberAccent,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = stringResource(R.string.ai_suggestions_subtitle),
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = TextMutedDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (suggestions.size > 1 && isExpanded) {
                        TextButton(
                            onClick = onCreateAllSuggestions,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.testTag("create_all_suggestions_button")
                        ) {
                            Text(
                                text = "حفظ الكل",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyanAccent
                            )
                        }
                    }

                    IconButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                            tint = TextMutedDark
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(10.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(vertical = 4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ai_suggestions_carousel")
                    ) {
                        items(suggestions, key = { it.id }) { suggestion ->
                            AiSuggestionCard(
                                suggestion = suggestion,
                                onExplore = { onExploreSuggestion(suggestion) },
                                onSaveAlbum = { onSaveAsAlbum(suggestion) },
                                onDismiss = { onDismissSuggestion(suggestion.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiSuggestionCard(
    suggestion: AiAlbumSuggestion,
    onExplore: () -> Unit,
    onSaveAlbum: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val themeIcon = getIconForTheme(suggestion.iconType)

    Card(
        modifier = modifier
            .width(260.dp)
            .clip(RoundedCornerShape(14.dp))
            .testTag("ai_suggestion_card_${suggestion.themeKey.lowercase()}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Top Bar with Dismiss and Tag Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(AmberAccent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = themeIcon,
                            contentDescription = null,
                            tint = AmberAccent,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = suggestion.titleArabic,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        ),
                        color = TextPrimaryDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(22.dp)
                        .testTag("dismiss_suggestion_${suggestion.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.ai_suggestions_dismiss),
                        tint = TextMutedDark,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Multi-Photo Collage / Cover Preview (up to 4 thumbnails)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceDark)
                    .clickable { onExplore() }
            ) {
                if (suggestion.sampleCoverUris.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = themeIcon,
                            contentDescription = null,
                            tint = TextMutedDark,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                } else if (suggestion.sampleCoverUris.size == 1) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(suggestion.sampleCoverUris[0])
                            .crossfade(true)
                            .build(),
                        contentDescription = suggestion.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (suggestion.sampleCoverUris.size == 2) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = ImageRequest.Builder(context).data(suggestion.sampleCoverUris[0]).crossfade(true).build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.weight(1f).fillMaxSize()
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        AsyncImage(
                            model = ImageRequest.Builder(context).data(suggestion.sampleCoverUris[1]).crossfade(true).build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.weight(1f).fillMaxSize()
                        )
                    }
                } else {
                    // 3 or 4 photos grid
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(modifier = Modifier.weight(1f)) {
                            AsyncImage(
                                model = ImageRequest.Builder(context).data(suggestion.sampleCoverUris[0]).crossfade(true).build(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.weight(1f).fillMaxSize()
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            AsyncImage(
                                model = ImageRequest.Builder(context).data(suggestion.sampleCoverUris.getOrNull(1)).crossfade(true).build(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.weight(1f).fillMaxSize()
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(modifier = Modifier.weight(1f)) {
                            AsyncImage(
                                model = ImageRequest.Builder(context).data(suggestion.sampleCoverUris.getOrNull(2)).crossfade(true).build(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.weight(1f).fillMaxSize()
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            AsyncImage(
                                model = ImageRequest.Builder(context).data(suggestion.sampleCoverUris.getOrNull(3) ?: suggestion.sampleCoverUris[0]).crossfade(true).build(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.weight(1f).fillMaxSize()
                            )
                        }
                    }
                }

                // Floating Photo Count Pill
                Surface(
                    color = ObsidianBg.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.ai_suggestions_photos_count, suggestion.mediaCount),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanAccent
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Matched ML keyword chips
            if (suggestion.matchedTags.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    suggestion.matchedTags.take(3).forEach { tag ->
                        Surface(
                            color = SurfaceDark,
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.05f))
                        ) {
                            Text(
                                text = "#$tag",
                                fontSize = 9.sp,
                                color = TextMutedDark,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    onClick = onExplore,
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .testTag("explore_suggestion_${suggestion.id}"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyanAccent,
                        contentColor = ObsidianBg
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Text(
                        text = stringResource(R.string.ai_suggestions_explore),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = onSaveAlbum,
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("save_suggestion_album_${suggestion.id}"),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = AmberAccent
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AmberAccent.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.BookmarkBorder,
                        contentDescription = stringResource(R.string.ai_suggestions_create_album),
                        tint = AmberAccent,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

private fun getIconForTheme(iconType: String): ImageVector {
    return when (iconType.lowercase()) {
        "restaurant", "food" -> Icons.Default.Restaurant
        "description", "document" -> Icons.Default.Description
        "flight", "travel" -> Icons.Default.Flight
        "park", "nature" -> Icons.Default.Park
        "directions_car", "car", "vehicle" -> Icons.Default.DirectionsCar
        "show_chart", "trading" -> Icons.Default.ShowChart
        "shopping_bag", "product" -> Icons.Default.ShoppingBag
        "person", "people" -> Icons.Default.Person
        "crop_free", "screenshot", "crop" -> Icons.Default.Crop
        "work", "business" -> Icons.Default.Business
        else -> Icons.Default.AutoAwesome
    }
}
