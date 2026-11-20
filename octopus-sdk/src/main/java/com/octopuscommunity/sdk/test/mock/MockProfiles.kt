package com.octopuscommunity.sdk.test.mock

import com.octopuscommunity.sdk.domain.model.Author
import com.octopuscommunity.sdk.domain.model.CurrentUserProfile
import com.octopuscommunity.sdk.domain.model.OtherUserProfile
import com.octopuscommunity.sdk.domain.model.Resource
import java.util.Date

/** Mock profiles of other users. */
object MockProfiles {
    val default =
        create(
            nickname = "John Doe",
        )

    val all =
        listOf(
            default,
            create(nickname = "Alice Smith"),
            create(nickname = "Bob Johnson"),
            create(nickname = "Carol Williams"),
            create(nickname = "David Brown"),
        )

    private fun create(
        nickname: String = "user_${('a'..'z').random()}${('a'..'z').random()}${('a'..'z').random()}",
        bio: String = "This is a bio for $nickname",
        avatarUrl: String? = null,
        messageCount: Int = 5,
        creationDate: Date = Date(1_750_000_000_000L),
        gamificationLevelIndex: Int? = null,
    ) = OtherUserProfile(
        userId = nickname,
        nickname = nickname,
        bio = bio,
        picture = avatarUrl?.ifEmpty { null }?.let { Resource.Remote(avatarUrl) },
        postsFeeds = MockFeeds.Post.create(id = "profile_$nickname"),
        messageCount = messageCount,
        creationDate = creationDate,
        tags = emptyList(),
        gamificationLevelIndex = gamificationLevelIndex,
    )
}

/** Mock [CurrentUserProfile]s. */
object MockCurrentUserProfiles {
    val default = MockProfiles.default.toCurrentUserProfile()

    private fun OtherUserProfile.toCurrentUserProfile() =
        CurrentUserProfile(
            userId = userId,
            nickname = nickname,
            picture = picture,
            postsFeeds = postsFeeds,
            gamificationLevelIndex = gamificationLevelIndex,
            messageCount = messageCount,
            creationDate = creationDate,
            tags = emptyList(),
            gamificationScore = 0,
        )
}

/** Mock [MinimalProfile]s, derived from [MockProfiles]. */
object MockMinimalProfiles {
    val default = MockProfiles.default.toMinimalProfile()

    val all = MockProfiles.all.map { it.toMinimalProfile() }

    private fun OtherUserProfile.toMinimalProfile() =
        Author(
            userId = userId,
            nickname = nickname,
            picture = picture,
            tags = emptyList(),
            gamificationLevelIndex = gamificationLevelIndex,
        )
}
