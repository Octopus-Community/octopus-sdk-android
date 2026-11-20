package com.octopuscommunity.sdk.ui.profile.common

import com.octopuscommunity.sdk.domain.model.CurrentUserProfile
import com.octopuscommunity.sdk.domain.model.MinimalProfile
import com.octopuscommunity.sdk.domain.model.Profile
import com.octopuscommunity.sdk.ui.R
import com.octopuscommunity.sdk.ui.common.UiText
import com.octopuscommunity.sdk.ui.common.UiText.StringResource

fun MinimalProfile?.getUiNickname(currentUserProfile: CurrentUserProfile?) = when (this) {
    null -> StringResource(R.string.author_deleted)
    else -> UiText.DynamicString(
        when (this.userId) {
            currentUserProfile?.userId -> currentUserProfile.nickname
            else -> this.nickname
        }
    )
}

fun Profile.getCleanedBio(): String? = bio?.let { bio ->
    // Break into lines
    bio.lines()
        // put back the new line
        .joinToString("\n") {
            // Remove trailing whitespaces
            it.replace("\\s+$".toRegex(), "")
        }
        // Replace 2 or more consecutive newlines with a single newline
        .replace("\n{2,}".toRegex(), "\n")
        .trim()
        .ifEmpty { null }
}