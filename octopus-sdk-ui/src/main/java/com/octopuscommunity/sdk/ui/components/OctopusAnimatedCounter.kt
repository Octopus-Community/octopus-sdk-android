package com.octopuscommunity.sdk.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.utils.currentLocale
import com.octopuscommunity.sdk.ui.utils.toCompactString

@Composable
fun OctopusAnimatedCounter(
    count: Int,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    style: TextStyle = LocalTextStyle.current,
) {
    var previousCount by remember { mutableIntStateOf(count) }
    SideEffect {
        previousCount = count
    }
    val locale = LocalConfiguration.current.currentLocale
    // One Text per character, so each digit can animate on its own — which makes the digit order a
    // layout concern: a Row orders its children along the ambient layout direction, so under an RTL
    // locale the default `Arrangement.Start` laid "1.5K" out as "K5.1". A formatted number is a
    // logically left-to-right run even inside an RTL paragraph (UAX internal change lays a numeric run out
    // left-to-right and positions it as a unit in the surrounding flow), so the characters inside it
    // must not be mirrored.
    //
    // Every `Arrangement.Absolute.*` places children in the order given, never mirrored — hence the
    // pair below rather than a single `Absolute.Left`. This composable is public API, so a host may
    // pass a width-bearing `modifier`: `Absolute.Right` keeps the run anchored to the RTL start edge
    // the way `Start` would, so only the character order changes and no alignment is silently pinned
    // to the physical left. The parent layout still resolves in the ambient direction, so the
    // counter as a whole stays on the mirrored side.
    Row(
        modifier = modifier,
        horizontalArrangement =
            when (LocalLayoutDirection.current) {
                LayoutDirection.Ltr -> Arrangement.Absolute.Left
                LayoutDirection.Rtl -> Arrangement.Absolute.Right
            },
    ) {
        val countString = count.toCompactString(locale)
        val previousCountString = previousCount.toCompactString(locale)
        for (i in countString.indices) {
            val oldChar = previousCountString.getOrNull(i)
            val newChar = countString[i]
            val char =
                if (oldChar == newChar) {
                    previousCountString[i]
                } else {
                    countString[i]
                }
            AnimatedContent(
                targetState = char,
                transitionSpec = {
                    if (previousCount < count) {
                        slideInVertically { it } togetherWith slideOutVertically { -it }
                    } else {
                        slideInVertically { -it } togetherWith slideOutVertically { it }
                    }
                },
            ) { targetChar ->
                Text(
                    text = targetChar.toString(),
                    softWrap = false,
                    color = color,
                    style = style,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OctopusAnimatedCounterPreview() {
    OctopusTheme {
        Column {
            OctopusAnimatedCounter(
                count = 0,
            )
            OctopusAnimatedCounter(
                count = 1550,
            )
        }
    }
}
