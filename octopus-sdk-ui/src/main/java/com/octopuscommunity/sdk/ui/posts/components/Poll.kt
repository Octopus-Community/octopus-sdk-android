package com.octopuscommunity.sdk.ui.posts.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import com.octopuscommunity.sdk.domain.model.Post
import com.octopuscommunity.sdk.domain.model.Post.Poll.Answer
import com.octopuscommunity.sdk.test.mock.MockPostPolls
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.R
import com.octopuscommunity.sdk.ui.common.stringResource
import com.octopuscommunity.sdk.ui.lineHeight125
import com.octopuscommunity.sdk.ui.semiBold
import com.octopuscommunity.sdk.ui.utils.OctopusMotion
import com.octopuscommunity.sdk.ui.utils.currentLocale
import com.octopuscommunity.sdk.ui.utils.toCompactString
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

@Composable
fun Poll(
    poll: Post.Poll,
    onVote: (Answer) -> Unit,
    modifier: Modifier = Modifier,
    vote: Answer? = null,
    translate: Boolean = false,
) {
    PollContent(
        poll = poll,
        onVoteAttempt = { answer ->
            onVote(answer)
            // The legacy Unit callback cannot report completion; preserve its retry behaviour.
            CompletableDeferred(PollVoteOutcome.Refused)
        },
        modifier = modifier,
        vote = vote,
        translate = translate,
    )
}

/** Outcome of a complete vote attempt, including the authentication/configuration gate. */
internal enum class PollVoteOutcome {
    Voted,
    Refused,
    Failed,
    Cancelled,
}

/** Keeps a vote pending until its result is visible or its attempt ends without voting. */
@Composable
internal fun PollContent(
    poll: Post.Poll,
    onVoteAttempt: (Answer) -> Deferred<PollVoteOutcome>,
    modifier: Modifier = Modifier,
    vote: Answer? = null,
    translate: Boolean = false,
) {
    val hapticFeedback = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    // Set synchronously before dispatch: even the outgoing answer content must ignore later taps.
    var pendingAttempt by remember { mutableStateOf<Any?>(null) }
    var firstReveal by remember { mutableStateOf(vote == null) }
    LaunchedEffect(firstReveal, vote) {
        if (vote != null) {
            // The live vote now blocks outgoing answers; release the pending latch so a backend
            // rollback can restore voting without requiring the poll to leave composition.
            pendingAttempt = null
            if (firstReveal) firstReveal = false
        }
    }

    var maxPercentageWidth by remember { mutableStateOf(0.dp) }
    val locale = LocalConfiguration.current.currentLocale
    val currentPoll by rememberUpdatedState(poll)
    val currentVote by rememberUpdatedState(vote)
    val currentOnVoteAttempt by rememberUpdatedState(onVoteAttempt)

    // Only the list animates neighbouring items into place when voting or translation changes
    // the height. Keep the content fade and the result bars, without a competing size tween.
    AnimatedContent(
        // Live vote counts update in place; only these layout states trigger a new fade.
        targetState = Triple(vote != null, poll.results.isNotEmpty(), translate),
        contentKey = { it },
        modifier = modifier,
        transitionSpec = {
            fadeIn(tween(OctopusMotion.SHORT_DURATION_MS)) togetherWith
                fadeOut(tween(OctopusMotion.SHORT_DURATION_MS)) using null
        },
        label = "Poll",
    ) { (shownVoted, _, shownTranslate) ->
        // AnimatedContent retains each keyed content lambda for the lifetime of its layout.
        val shownPoll = currentPoll
        val shownVote = currentVote
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            shownPoll.answers.forEachIndexed { index, answer ->
                if (shownVoted) {
                    PollResultItem(
                        modifier = Modifier.fillMaxWidth(),
                        answer = answer,
                        optionIndex = index + 1,
                        totalOptions = shownPoll.answers.size,
                        votePercentage =
                            if (!firstReveal) {
                                with(shownPoll) { answer.votePercentage }
                            } else {
                                0
                            },
                        selected = answer == shownVote,
                        percentageWidth = maxPercentageWidth,
                        translate = shownTranslate,
                        onPercentageWidthChanged = { width ->
                            maxPercentageWidth = max(maxPercentageWidth, width)
                        },
                    )
                } else {
                    PollAnswerItem(
                        modifier = Modifier.fillMaxWidth(),
                        answer = answer,
                        optionIndex = index + 1,
                        totalOptions = shownPoll.answers.size,
                        translate = shownTranslate,
                        onClick = {
                            if (pendingAttempt == null && currentVote == null) {
                                val attempt = Any()
                                pendingAttempt = attempt
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                val completion = currentOnVoteAttempt(answer)
                                scope.launch {
                                    var outcome = PollVoteOutcome.Cancelled
                                    try {
                                        outcome = completion.await()
                                    } catch (e: CancellationException) {
                                        if (!currentCoroutineContext().isActive) throw e
                                    } catch (_: Exception) {
                                        outcome = PollVoteOutcome.Failed
                                    } finally {
                                        // An older attempt may finish after a rollback and a retry.
                                        if (outcome != PollVoteOutcome.Voted && pendingAttempt === attempt) {
                                            pendingAttempt = null
                                        }
                                    }
                                }
                            }
                        },
                    )
                }
            }

            Text(
                modifier =
                    Modifier
                        .fillMaxWidth(),
                text =
                    if (shownVoted) {
                        pluralStringResource(
                            id = R.plurals.poll_results_vote_count_total,
                            count = shownPoll.voteCount,
                            shownPoll.voteCount.toCompactString(locale),
                        )
                    } else if (shownPoll.voteCount >= 5) {
                        stringResource(
                            R.string.poll_results_hidden_vote_count_formal,
                            shownPoll.voteCount.toCompactString(locale),
                        )
                    } else {
                        stringResource(R.string.poll_results_hidden_some_votes_formal)
                    },
                color = OctopusTheme.colorScheme.gray700,
                style = OctopusTheme.typography.caption2.semiBold,
            )
        }
    }
}

@Composable
fun PollAnswerItem(
    answer: Answer,
    optionIndex: Int,
    totalOptions: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    translate: Boolean,
) {
    val answerText = answer.text.getText(translate)
    val optionLabel =
        stringResource(
            R.string.accessibility_poll_option,
            optionIndex,
            totalOptions,
        )

    Box(
        modifier =
            modifier
                .semantics {
                    role = Role.RadioButton
                    contentDescription = "$optionLabel. $answerText"
                }.clickable(onClick = onClick)
                .border(
                    width = 1.dp,
                    color = OctopusTheme.colorScheme.primary,
                    shape = RoundedCornerShape(8.dp),
                ).padding(12.dp),
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = answerText,
            color = OctopusTheme.colorScheme.primary,
            style = OctopusTheme.typography.body2.lineHeight125,
        )
    }
}

@Composable
internal fun PollResultItem(
    answer: Answer,
    optionIndex: Int,
    totalOptions: Int,
    votePercentage: Int,
    selected: Boolean,
    percentageWidth: Dp,
    translate: Boolean,
    onPercentageWidthChanged: (Dp) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current

    val animatedProgress by animateFloatAsState(
        targetValue = votePercentage / 100f,
        animationSpec =
            tween(
                durationMillis = 1.5.seconds.toInt(DurationUnit.MILLISECONDS),
            ),
        label = "progressAnimation",
    )

    val optionLabel = stringResource(R.string.accessibility_poll_option, optionIndex, totalOptions)
    val contentDescription = "$optionLabel. ${
        if (selected) {
            ", ${stringResource(R.string.accessibility_common_selected)}"
        } else {
            ""
        }
    }${
        if (votePercentage > 0) {
            ", $votePercentage%"
        } else {
            ""
        }
    }"

    Row(
        modifier =
            modifier
                .semantics {
                    this.role = Role.RadioButton
                    this.contentDescription = contentDescription
                },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Progress bar background with option text
        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .height(IntrinsicSize.Min) // This derives height from the content
                    .clip(RoundedCornerShape(8.dp))
                    .background(OctopusTheme.colorScheme.gray200),
        ) {
            // Colored progress indicator
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth(animatedProgress)
                        .fillMaxHeight()
                        .background(OctopusTheme.colorScheme.gray300),
            )
            // This is positioned absolutely over the progress bar
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = answer.text.getText(translate),
                    color = OctopusTheme.colorScheme.gray900,
                    style = OctopusTheme.typography.body2.lineHeight125,
                )

                if (selected) {
                    Icon(
                        modifier = Modifier.size(24.dp),
                        painter =
                            OctopusTheme.icons.content.poll
                                .selectedOption(),
                        contentDescription = stringResource(R.string.accessibility_common_selected),
                        tint = OctopusTheme.colorScheme.primaryHigh,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            modifier =
                if (percentageWidth > 0.dp) {
                    Modifier.width(percentageWidth)
                } else {
                    Modifier.widthIn(min = 40.dp)
                }.onSizeChanged { size ->
                    val widthInDp =
                        with(density) {
                            size.width.toDp()
                        }
                    onPercentageWidthChanged(widthInDp)
                },
            text = "$votePercentage%",
            color = OctopusTheme.colorScheme.gray900,
            textAlign = TextAlign.Right,
            style = OctopusTheme.typography.body2.lineHeight125,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PollPreview() {
    OctopusTheme {
        Poll(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(Color.White),
            poll = MockPostPolls.default,
            vote = MockPostPolls.default.answers.first(),
            onVote = {},
        )
    }
}
