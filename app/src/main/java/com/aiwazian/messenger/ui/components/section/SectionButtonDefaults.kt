/*
 * Copyright (c) 2026. Aiwazian.
 */

package com.aiwazian.messenger.ui.components.section

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse

data class SectionButtonColors(
    val containerColor: Color,
    val contentColor: Color,
    val leadingContentColor: Color,
    val trailingContentColor: Color,
    val supportColor: Color
)

object SectionButtonDefaults {

    @Composable
    fun colors(
        containerColor: Color = Color.Unspecified,
        contentColor: Color = Color.Unspecified,
        leadingContentColor: Color = Color.Unspecified,
        trailingContentColor: Color = Color.Unspecified,
        supportColor: Color = Color.Unspecified
    ): SectionButtonColors {
        val resolvedContentColor = contentColor.takeOrElse {
            MaterialTheme.colorScheme.onSurface
        }

        return SectionButtonColors(
            containerColor = containerColor.takeOrElse {
                MaterialTheme.colorScheme.surfaceContainer
            },
            contentColor = resolvedContentColor,
            leadingContentColor = leadingContentColor.takeOrElse {
                contentColor.takeOrElse { MaterialTheme.colorScheme.onSurfaceVariant }
            },
            trailingContentColor = trailingContentColor.takeOrElse {
                contentColor.takeOrElse { MaterialTheme.colorScheme.onSurfaceVariant }
            },
            supportColor = supportColor.takeOrElse {
                contentColor.takeOrElse { MaterialTheme.colorScheme.onSurfaceVariant }
            }
        )
    }
}
