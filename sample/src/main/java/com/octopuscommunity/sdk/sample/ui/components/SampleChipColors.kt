package com.octopuscommunity.sdk.sample.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.octopuscommunity.sdk.sample.ui.theme.SampleTheme

// The DS Chip, one recipe in both themes: transparent, a 1px `outline` edge (the only boundary
// of an unfilled control, so it clears the 3:1 non-text floor), label in `text`; selected fills
// `accent-container` with `on-accent-container` and drops the edge. Shape (radius 8) is the
// Material chip default.

/** 1px `outline` edge for a sample-owned, non-interactive tag drawn by hand. */
@Composable
fun Modifier.sampleDarkChipBorder(shape: Shape): Modifier = border(1.dp, SampleTheme.colors.outline, shape)

@Composable
fun sampleSuggestionChipColors() =
    SuggestionChipDefaults.suggestionChipColors(
        containerColor = Color.Transparent,
        labelColor = SampleTheme.colors.text,
        iconContentColor = SampleTheme.colors.text,
    )

@Composable
fun sampleSuggestionChipBorder() = BorderStroke(1.dp, SampleTheme.colors.outline)

/**
 * An input chip stands for a value the tester entered; it is present, not "chosen", so it keeps
 * the unselected recipe even though Material marks it selected.
 */
@Composable
fun sampleInputChipColors() =
    InputChipDefaults.inputChipColors(
        containerColor = Color.Transparent,
        labelColor = SampleTheme.colors.text,
        selectedContainerColor = Color.Transparent,
        selectedLabelColor = SampleTheme.colors.text,
        selectedLeadingIconColor = SampleTheme.colors.text,
        selectedTrailingIconColor = SampleTheme.colors.textSecondary,
        trailingIconColor = SampleTheme.colors.textSecondary,
    )

@Composable
fun sampleInputChipBorder() = BorderStroke(1.dp, SampleTheme.colors.outline)

@Composable
fun sampleFilterChipColors() =
    FilterChipDefaults.filterChipColors(
        containerColor = Color.Transparent,
        labelColor = SampleTheme.colors.text,
        iconColor = SampleTheme.colors.text,
        selectedContainerColor = SampleTheme.colors.accentContainer,
        selectedLabelColor = SampleTheme.colors.onAccentContainer,
        selectedLeadingIconColor = SampleTheme.colors.onAccentContainer,
        selectedTrailingIconColor = SampleTheme.colors.onAccentContainer,
    )

@Composable
fun sampleFilterChipBorder(selected: Boolean) =
    if (selected) {
        BorderStroke(0.dp, Color.Transparent)
    } else {
        BorderStroke(1.dp, SampleTheme.colors.outline)
    }
