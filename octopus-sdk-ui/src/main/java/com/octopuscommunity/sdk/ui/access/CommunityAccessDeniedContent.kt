package com.octopuscommunity.sdk.ui.access

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.octopuscommunity.sdk.domain.model.CommunityConfig
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.R
import com.octopuscommunity.sdk.ui.bold
import com.octopuscommunity.sdk.ui.common.snackbar.OctopusSnackbarHost
import com.octopuscommunity.sdk.ui.common.stringResource
import com.octopuscommunity.sdk.ui.common.viewmodel.OctopusScreenViewModel
import com.octopuscommunity.sdk.ui.common.viewmodel.octopusViewModel
import com.octopuscommunity.sdk.ui.components.OctopusAlertDialog
import com.octopuscommunity.sdk.ui.components.OctopusButton
import com.octopuscommunity.sdk.ui.components.OctopusButtonDefaults
import com.octopuscommunity.sdk.ui.components.OctopusContent
import com.octopuscommunity.sdk.ui.components.OctopusPoweredBy
import com.octopuscommunity.sdk.ui.medium

@Composable
fun CommunityAccessDeniedContent(
    modifier: Modifier = Modifier,
    screenViewModel: OctopusScreenViewModel,
) = OctopusContent {
    val viewModel = octopusViewModel<CommunityAccessDeniedViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val screenState by screenViewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LifecycleEventEffect(event = Lifecycle.Event.ON_RESUME) { viewModel.onResume() }
    LaunchedEffect(Unit) { viewModel.onDisplayed() }

    Box(modifier = modifier) {
        CommunityAccessDeniedLayout(
            state = state,
            communityConfig = screenState.communityConfig,
            onButtonClick = viewModel::onButtonClick,
            onEmailClick = { viewModel.sendEmail(context, it) },
            snackbarHost = { OctopusSnackbarHost(screenViewModel) },
        )

        state.policy?.let { BanPolicySheet(policy = it, onDismissRequest = viewModel::onPolicyDismiss) }

        OctopusAlertDialog(viewModel)
    }
}

/** Stateless body: previews and screenshot tests drive it with a literal state. */
@Composable
internal fun CommunityAccessDeniedLayout(
    state: CommunityAccessDeniedViewModel.UiState,
    communityConfig: CommunityConfig?,
    onButtonClick: () -> Unit,
    onEmailClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHost: @Composable () -> Unit = {},
) {
    Column(
        modifier = modifier.fillMaxSize().background(OctopusTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Centred while it fits, scrolling when a long message or a large font does not: the button and the footer
        // below stay reachable either way.
        BoxWithConstraints(modifier = Modifier.fillMaxWidth().weight(1f)) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .heightIn(min = maxHeight)
                        .padding(horizontal = 32.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CommunityAccessDeniedIcon(modifier = Modifier.padding(vertical = 4.dp))
                Text(
                    modifier = Modifier.fillMaxWidth().semantics { heading() },
                    text = stringResource(state.title),
                    color = OctopusTheme.colorScheme.gray900,
                    textAlign = TextAlign.Center,
                    style = OctopusTheme.typography.title2.bold,
                )
                when (val subtitle = state.subtitle) {
                    is CommunityAccessDeniedViewModel.UiState.Subtitle.ServerMessage ->
                        // Written by the community, maybe in another script than the screen's: it takes the
                        // direction of its own text.
                        SubtitleText(AnnotatedString(subtitle.text), textDirection = TextDirection.Content)
                    is CommunityAccessDeniedViewModel.UiState.Subtitle.Banned ->
                        subtitle.paragraphs().forEach { paragraph ->
                            val text = paragraph.asString()
                            val email = subtitle.contactEmail?.takeIf { text.contains(it) }
                            // The address is a small target at the end of a centred sentence: the whole paragraph
                            // that holds it answers the tap.
                            SubtitleText(
                                text = text.underlining(email),
                                onClick = email?.let { { onEmailClick(it) } },
                            )
                        }
                    null -> Unit
                }
            }
        }

        snackbarHost()

        Column(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            state.button?.let { button ->
                AccessDeniedButton(
                    text =
                        stringResource(
                            when (button) {
                                CommunityAccessDeniedViewModel.UiState.Button.KnowMore ->
                                    R.string.community_access_denied_banned_know_more
                                CommunityAccessDeniedViewModel.UiState.Button.AccessCommunity ->
                                    R.string.community_access_denied_deleted_access_community
                            },
                        ),
                    onClick = onButtonClick,
                    loading = state.isButtonLoading,
                )
            }
            OctopusPoweredBy(communityConfig = communityConfig)
        }
    }
}

@Composable
private fun SubtitleText(
    text: AnnotatedString,
    onClick: (() -> Unit)? = null,
    textDirection: TextDirection = TextDirection.Unspecified,
) {
    Text(
        modifier =
            Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier),
        text = text,
        color = OctopusTheme.colorScheme.gray900,
        textAlign = TextAlign.Center,
        style = OctopusTheme.typography.body2.copy(lineHeight = 24.sp, textDirection = textDirection),
    )
}

/** The text, with [part] underlined where the text holds it. */
private fun String.underlining(part: String?): AnnotatedString {
    val start = part?.let { indexOf(it) } ?: -1
    if (part == null || start < 0) return AnnotatedString(this)
    return buildAnnotatedString {
        append(this@underlining)
        addStyle(SpanStyle(textDecoration = TextDecoration.Underline), start, start + part.length)
    }
}

/** The quiet pill button of this screen and of its sheet: it offers a way on, without the weight of a main action. */
@Composable
internal fun AccessDeniedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
) {
    OctopusButton(
        modifier = modifier,
        text = text,
        onClick = onClick,
        loading = loading,
        shape = OctopusButtonDefaults.mediumShape,
        colors =
            OctopusButtonDefaults.buttonColors(
                containerColor = OctopusTheme.colorScheme.gray200,
                contentColor = OctopusTheme.colorScheme.gray900,
            ),
        heightIn = 40.dp,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 7.dp),
        textStyle = OctopusTheme.typography.caption1.medium,
    )
}

@Preview(showBackground = true)
@Composable
private fun CommunityAccessDeniedLayoutPreview() {
    OctopusTheme {
        CommunityAccessDeniedLayout(
            state =
                CommunityAccessDeniedViewModel.UiState(
                    title = R.string.community_access_denied_banned_title_formal,
                    subtitle =
                        CommunityAccessDeniedViewModel.UiState.Subtitle.Banned(
                            nickname = "marie_l92",
                            policyName = "Job applications",
                            contactEmail = "help@example.com",
                        ),
                    button = CommunityAccessDeniedViewModel.UiState.Button.KnowMore,
                ),
            communityConfig = null,
            onButtonClick = {},
            onEmailClick = {},
        )
    }
}
