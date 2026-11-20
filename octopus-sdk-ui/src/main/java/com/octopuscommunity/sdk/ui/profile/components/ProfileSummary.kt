package com.octopuscommunity.sdk.ui.profile.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.octopuscommunity.sdk.domain.model.CommunityConfig
import com.octopuscommunity.sdk.domain.model.Profile
import com.octopuscommunity.sdk.domain.model.ProfileField
import com.octopuscommunity.sdk.test.mock.MockCommunityConfig
import com.octopuscommunity.sdk.test.mock.MockCurrentUserProfiles
import com.octopuscommunity.sdk.test.mock.MockProfiles
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.R
import com.octopuscommunity.sdk.ui.common.medias.viewer.ImageViewerSource
import com.octopuscommunity.sdk.ui.common.medias.viewer.imageViewerSource
import com.octopuscommunity.sdk.ui.common.medias.viewer.rememberImageViewerSource
import com.octopuscommunity.sdk.ui.common.stringResource
import com.octopuscommunity.sdk.ui.components.OctopusAdminBadge
import com.octopuscommunity.sdk.ui.components.OctopusContent
import com.octopuscommunity.sdk.ui.components.OctopusExpandableText
import com.octopuscommunity.sdk.ui.components.OctopusGamificationBadge
import com.octopuscommunity.sdk.ui.components.OctopusMediumOutlinedButton
import com.octopuscommunity.sdk.ui.components.OctopusPoweredBy
import com.octopuscommunity.sdk.ui.profile.common.getCleanedBio
import com.octopuscommunity.sdk.ui.profile.current.ProfileFieldsEditability
import com.octopuscommunity.sdk.ui.semiBold
import com.octopuscommunity.sdk.ui.utils.currentLocale
import com.octopuscommunity.sdk.ui.utils.getLocalizedAge
import com.octopuscommunity.sdk.ui.utils.toCompactString

@Composable
internal fun ProfileSummary(
    profile: Profile,
    communityConfig: CommunityConfig?,
    onAvatarClick: (ImageViewerSource) -> Unit,
    onShowGamificationSheet: () -> Unit,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    editable: Boolean = false,
    editability: ProfileFieldsEditability = ProfileFieldsEditability.AllEditable,
    onEditClick: (focusedField: ProfileField?) -> Unit = { },
    content: @Composable () -> Unit,
) = OctopusContent {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.currentLocale

    Column(modifier = modifier) {
        BoxWithConstraints(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1.0f),
        ) {
            val screenHeight = maxHeight

            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(state = scrollState),
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // Tappable only when there is an image to zoom (see isAvatarZoomable).
                            val avatarSource = rememberImageViewerSource(isCircle = true)
                            val avatarZoomable = profile.isAvatarZoomable
                            ProfileAvatar(
                                modifier =
                                    Modifier
                                        .size(72.dp)
                                        .imageViewerSource(avatarSource)
                                        .then(
                                            if (avatarZoomable) {
                                                Modifier.clickable { onAvatarClick(avatarSource) }
                                            } else {
                                                Modifier
                                            },
                                        ),
                                profile = profile,
                                actionIcon =
                                    when {
                                        // Read-only avatar (internal ticket): shown bare, no add/edit "+".
                                        !editable || !editability.avatarEditable -> null
                                        profile.hasPicture -> OctopusTheme.icons.profile.editPicture()
                                        else -> OctopusTheme.icons.profile.addPicture()
                                    },
                                onClick =
                                    if (avatarZoomable) {
                                        { onAvatarClick(avatarSource) }
                                    } else {
                                        null
                                    },
                                onActionClick = { onEditClick(ProfileField.PICTURE) },
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.fillMaxWidth()) {
                                SelectionContainer {
                                    Text(
                                        modifier = Modifier.fillMaxWidth(),
                                        text = profile.nickname,
                                        color = OctopusTheme.colorScheme.gray900,
                                        style = OctopusTheme.typography.title2.semiBold,
                                    )
                                }
                                if (profile.tags.contains(Profile.Tag.ADMIN)) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    OctopusAdminBadge()
                                } else {
                                    communityConfig?.gamification?.let { gamificationConfig ->
                                        Spacer(modifier = Modifier.height(4.dp))

                                        ProfileGamification(
                                            modifier = Modifier.fillMaxWidth(),
                                            profile = profile,
                                            gamificationConfig = gamificationConfig,
                                            onShowGamificationSheet = onShowGamificationSheet,
                                        )
                                    }
                                }
                            }
                        }

                        val messageCount = profile.messageCount
                        val accountAge =
                            remember(
                                profile.creationDate,
                                communityConfig?.displayAccountAge,
                                locale,
                            ) {
                                (profile.creationDate)
                                    ?.takeIf {
                                        communityConfig?.displayAccountAge == true
                                    }?.getLocalizedAge(context, locale)
                            }

                        if (messageCount != null || accountAge != null) {
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(80.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (messageCount != null) {
                                    Column(
                                        modifier =
                                            Modifier
                                                .clickable(
                                                    onClick = onShowGamificationSheet,
                                                    role = Role.Button,
                                                ).minimumInteractiveComponentSize(),
                                    ) {
                                        Text(
                                            text = messageCount.toCompactString(locale),
                                            color = OctopusTheme.colorScheme.gray900,
                                            style = OctopusTheme.typography.body2.semiBold,
                                        )
                                        Text(
                                            text =
                                                pluralStringResource(
                                                    id = R.plurals.profile_detail_message,
                                                    count = messageCount,
                                                ),
                                            color = OctopusTheme.colorScheme.gray900,
                                            style = OctopusTheme.typography.caption2,
                                        )
                                    }
                                }

                                if (accountAge != null) {
                                    Column(
                                        modifier = Modifier.semantics(mergeDescendants = true) { },
                                    ) {
                                        Text(
                                            modifier = Modifier.fillMaxWidth(),
                                            text = accountAge,
                                            color = OctopusTheme.colorScheme.gray900,
                                            style = OctopusTheme.typography.body2.semiBold,
                                        )
                                        Text(
                                            modifier = Modifier.fillMaxWidth(),
                                            text = stringResource(R.string.profile_detail_account_age),
                                            color = OctopusTheme.colorScheme.gray900,
                                            style = OctopusTheme.typography.caption2,
                                        )
                                    }
                                }
                            }
                        }

                        val fullBio = remember(profile) { profile.getCleanedBio() }
                        // A DISABLED bio is removed entirely — no display even of an existing value
                        // (internal ticket). For other communities `bioHidden` is always false.
                        if (!editability.bioHidden && fullBio != null) {
                            Spacer(modifier = Modifier.height(8.dp))

                            SelectionContainer {
                                OctopusExpandableText(
                                    modifier = Modifier.fillMaxWidth(),
                                    text = fullBio,
                                    linkify = true,
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        // "Add a bio" is offered only when the bio is editable and there is none yet.
                        val canAddBio =
                            editability.bioEditable &&
                                !editability.bioHidden &&
                                fullBio == null
                        if (editable && (canAddBio || editability.showEditButton)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (canAddBio) {
                                    OctopusMediumOutlinedButton(
                                        icon = OctopusTheme.icons.profile.addBio(),
                                        text = stringResource(R.string.profile_detail_empty_bio_button_formal),
                                        onClick = { onEditClick(ProfileField.BIO) },
                                    )
                                }
                                // The "Edit profile" button shows as soon as any field is editable.
                                if (editability.showEditButton) {
                                    OctopusMediumOutlinedButton(
                                        text = stringResource(R.string.profile_edit_button),
                                        onClick = { onEditClick(null) },
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    Column(
                        modifier =
                            Modifier
                                .height(screenHeight)
                                .nestedScroll(
                                    remember {
                                        object : NestedScrollConnection {
                                            override fun onPreScroll(
                                                available: Offset,
                                                source: NestedScrollSource,
                                            ): Offset =
                                                if (available.y > 0) {
                                                    Offset.Zero
                                                } else {
                                                    Offset(
                                                        x = 0f,
                                                        y = -scrollState.dispatchRawDelta(-available.y),
                                                    )
                                                }
                                        }
                                    },
                                ),
                    ) {
                        content()
                    }
                }
            }
        }

        OctopusPoweredBy(communityConfig = communityConfig)
    }
}

@Composable
private fun ProfileGamification(
    profile: Profile,
    gamificationConfig: CommunityConfig.Gamification,
    onShowGamificationSheet: () -> Unit,
    modifier: Modifier = Modifier,
) = with(gamificationConfig) {
    Column(
        modifier =
            modifier
                .clickable(
                    role = Role.Button,
                    onClick = onShowGamificationSheet,
                ),
//            .minimumInteractiveComponentSize()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            profile.gamificationLevel?.let { level ->
                if (level.colors != null) {
                    OctopusGamificationBadge(level = level)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            profile.gamificationScoreProgressText?.let { progressText ->
                Row(
                    modifier =
                        Modifier
                            .background(
                                color = OctopusTheme.colorScheme.primaryLow,
                                shape = RoundedCornerShape(2.dp),
                            ).padding(
                                horizontal = 2.dp,
                                vertical = 4.dp,
                            ),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = progressText,
                        color = OctopusTheme.colorScheme.primary,
                        style = OctopusTheme.typography.caption1.semiBold,
                    )
                    Icon(
                        modifier = Modifier.size(18.dp),
                        painter = OctopusTheme.icons.gamification.info(),
                        contentDescription = null,
                        tint = OctopusTheme.colorScheme.primary,
                    )
                }
            }
        }

        profile.gamificationScoreProgress?.let { progress ->
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                progress = { progress },
                color = OctopusTheme.colorScheme.primaryHigh,
                trackColor = OctopusTheme.colorScheme.primaryLow,
                gapSize = 0.dp,
                drawStopIndicator = {},
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfileSummaryPreview() {
    OctopusTheme {
        ProfileSummary(
            profile = MockProfiles.all.random(),
            communityConfig = MockCommunityConfig.default,
            onAvatarClick = {},
            onShowGamificationSheet = {},
        ) {}
    }
}

@Preview(showBackground = true)
@Composable
private fun CurrentProfileSummaryPreview() {
    OctopusTheme {
        ProfileSummary(
            profile =
                MockCurrentUserProfiles.default.copy(
                    gamificationScore = 1,
                    gamificationLevelIndex = 1,
                ),
            communityConfig = MockCommunityConfig.default,
            editable = true,
            onAvatarClick = {},
            onShowGamificationSheet = {},
        ) {}
    }
}
