package com.octopuscommunity.sdk.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.R
import com.octopuscommunity.sdk.ui.common.stringResource
import com.octopuscommunity.sdk.ui.medium

/**
 * What a content area shows instead of a list: nothing to display, or a first load that failed.
 *
 * One component for every such state across the SDK — profile tabs, main feed, group, explore, post
 * and comment — so a single visual language covers them all. It carries an illustration, a title and
 * an optional call to action; the vertical padding is per-screen, which is the only thing the design
 * varies (see [OctopusScreenStateDefaults]).
 *
 * @param action the call to action, or `null` for a read-only state.
 */
@Composable
internal fun OctopusScreenState(
    illustration: Painter,
    text: String,
    modifier: Modifier = Modifier,
    verticalPadding: Dp = OctopusScreenStateDefaults.ProfilePadding,
    action: Pair<String, () -> Unit>? = null,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = verticalPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Image(
            modifier = Modifier.height(104.dp),
            painter = illustration,
            // Decorative: the title carries the meaning, and announcing both would say it twice.
            contentDescription = null,
        )
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = text,
            textAlign = TextAlign.Center,
            style = OctopusTheme.typography.body2.medium,
            color = OctopusTheme.colorScheme.gray500,
        )
        action?.let { (label, onClick) ->
            OctopusMediumOutlinedButton(text = label, onClick = onClick)
        }
    }
}

/** [OctopusScreenState] as a lazy list item, which is how every list in the SDK shows its states. */
internal fun LazyListScope.octopusScreenStateItem(
    illustration: @Composable () -> Painter,
    @StringRes text: Int,
    verticalPadding: Dp = OctopusScreenStateDefaults.ProfilePadding,
    action: Pair<Int, () -> Unit>? = null,
) = item {
    OctopusScreenState(
        illustration = illustration(),
        text = stringResource(text),
        verticalPadding = verticalPadding,
        action = action?.let { (label, onClick) -> stringResource(label) to onClick },
    )
}

/**
 * A first load that failed, in the only two shapes the design distinguishes.
 *
 * Being offline gets its own illustration and wording because the member can act on it; everything
 * else — server down, timeout, unknown — shares one neutral catch-all that says nothing about the
 * network.
 *
 * Every list reaches it through [octopusScreenStateFailureItem]; the screens whose failed load has
 * no list to put an item in (post details with no post) compose it directly.
 */
@Composable
internal fun OctopusScreenStateFailure(
    isNoNetwork: Boolean,
    modifier: Modifier = Modifier,
    verticalPadding: Dp = OctopusScreenStateDefaults.ProfilePadding,
    onRetry: () -> Unit,
) {
    OctopusScreenState(
        modifier = modifier,
        illustration =
            if (isNoNetwork) {
                OctopusTheme.icons.screenStates.networkError()
            } else {
                OctopusTheme.icons.screenStates.error()
            },
        text = stringResource(if (isNoNetwork) R.string.error_no_network_formal else R.string.error_unknown_formal),
        verticalPadding = verticalPadding,
        action = stringResource(R.string.common_retry) to onRetry,
    )
}

/** [OctopusScreenStateFailure] as a lazy list item, which is how every list in the SDK shows it. */
internal fun LazyListScope.octopusScreenStateFailureItem(
    isNoNetwork: Boolean,
    verticalPadding: Dp = OctopusScreenStateDefaults.ProfilePadding,
    onRetry: () -> Unit,
) = item {
    OctopusScreenStateFailure(
        isNoNetwork = isNoNetwork,
        verticalPadding = verticalPadding,
        onRetry = onRetry,
    )
}

/** Vertical paddings the design gives per screen, and the loader offsets that go with them. */
internal object OctopusScreenStateDefaults {
    val ProfilePadding = 80.dp
    val FeedEmptyPadding = 100.dp
    val FeedErrorPadding = 80.dp
    val ExplorePadding = 120.dp
    val PostEmptyPadding = 60.dp
    val PostErrorPadding = 40.dp
    val CommentPadding = 60.dp

    val ProfileLoaderOffset = 130.dp
    val FeedLoaderOffset = 200.dp
    val GroupLoaderOffset = 140.dp
    val ExploreLoaderOffset = 200.dp
    val PostLoaderOffset = 60.dp
    val CommentLoaderOffset = 60.dp
}

@Preview
@Composable
private fun OctopusScreenStateEmptyPreview() {
    OctopusTheme {
        OctopusScreenState(
            illustration = OctopusTheme.icons.screenStates.emptyContent(),
            text = stringResource(R.string.content_empty_state_be_first_to_post_formal),
        )
    }
}

@Preview
@Composable
private fun OctopusScreenStateFailurePreview() {
    OctopusTheme {
        OctopusScreenState(
            illustration = OctopusTheme.icons.screenStates.networkError(),
            text = stringResource(R.string.error_no_network_formal),
            action = stringResource(R.string.common_retry) to {},
        )
    }
}
