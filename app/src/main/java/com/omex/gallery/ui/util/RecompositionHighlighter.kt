package com.omex.gallery.ui.util

import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.dp

/**
 * A [Modifier] that highlights composable recompositions with a colored border.
 * Visual feedback for identifying and optimizing unnecessary recompositions:
 * - Blue: 1 composition (initial render)
 * - Green: 2 compositions
 * - Yellow: 3-5 compositions
 * - Red: > 5 compositions (potential performance bottleneck)
 */
@Stable
fun Modifier.recompositionHighlighter(): Modifier = this.then(
    Modifier.composed(
        inspectorInfo = debugInspectorInfo {
            name = "recompositionHighlighter"
        }
    ) {
        val totalCompositions = remember { intArrayOf(0) }
        totalCompositions[0]++

        val totalCompositionsCount = totalCompositions[0]

        drawWithCache {
            val color = when {
                totalCompositionsCount > 5 -> Color.Red
                totalCompositionsCount > 2 -> Color.Yellow
                totalCompositionsCount > 1 -> Color.Green
                else -> Color.Blue
            }
            val strokeWidth = 2.dp.toPx()
            onDrawWithContent {
                drawContent()
                drawRect(
                    color = color,
                    style = Stroke(width = strokeWidth)
                )
            }
        }
    }
)
