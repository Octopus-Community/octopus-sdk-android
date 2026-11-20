@file:Suppress("ktlint:standard:max-line-length")

package com.octopuscommunity.sdk.test.mock

import com.octopuscommunity.sdk.domain.model.Author
import com.octopuscommunity.sdk.domain.model.Comment
import com.octopuscommunity.sdk.domain.model.Media
import com.octopuscommunity.sdk.domain.model.Medias
import com.octopuscommunity.sdk.domain.model.OctopusItem.Reaction
import com.octopuscommunity.sdk.domain.model.OctopusItem.ReactionCount
import com.octopuscommunity.sdk.domain.model.Resource
import com.octopuscommunity.sdk.domain.model.Status
import com.octopuscommunity.sdk.domain.model.TranslatableText
import java.util.Date
import java.util.UUID
import kotlin.time.Duration.Companion.minutes
import kotlin.time.DurationUnit

/** Mock replies. */
object MockReplies {
    val default =
        create(
            id = "fake_reply_0",
            text =
                TranslatableText(
                    originalText = "Ceci est le contenu de la réponse exemple par défaut. Elle peut être assez longue et contenir plusieurs phrases.",
                    originalLanguage = "fr",
                    translatedText = "This is the content of the default sample reply. It can be quite long and contain multiple sentences.",
                    translatedLanguage = "en",
                ),
            media =
                Medias(
                    listOf(
                        Media.Image(
                            source =
                                Resource.Remote(
                                    "https://images.squarespace-cdn.com/content/v1/672b2ffd96d8a54dcce38373/1730883582404-8OCZ52CUBOE1WTX3GBCH/Octopus_logo_RVB_SM_couleur.png",
                                ),
                            name = "Image",
                            contentType = "image/png",
                            dimensions = null,
                            fileSize = null,
                        ),
                    ),
                ),
        )

    val all =
        listOf(
            default,
            // French original with English translation
            create(
                text =
                    TranslatableText(
                        originalText = "Merci de partager cette information, c'est très utile!",
                        originalLanguage = "fr",
                        translatedText = "Thank you for sharing this information, it's very helpful!",
                        translatedLanguage = "en",
                    ),
            ),
            // English without translation
            create(
                text =
                    TranslatableText(
                        originalText = "I completely agree with your perspective on this topic.",
                        originalLanguage = "en",
                    ),
            ),
            // Spanish original with English translation
            create(
                text =
                    TranslatableText(
                        originalText = "¡Esto es exactamente lo que necesitaba saber! Excelente explicación.",
                        originalLanguage = "es",
                        translatedText = "This is exactly what I needed to know. Great explanation!",
                        translatedLanguage = "en",
                    ),
            ),
            // German original with English translation
            create(
                text =
                    TranslatableText(
                        originalText = "Ich habe eine Folgefrage dazu. Können Sie mehr Details geben?",
                        originalLanguage = "de",
                        translatedText = "I have a follow-up question about this. Can you provide more details?",
                        translatedLanguage = "en",
                    ),
            ),
            // English without translation
            create(
                text =
                    TranslatableText(
                        originalText = "I'm not sure I understand. Could you clarify a bit further?",
                        originalLanguage = "en",
                    ),
            ),
            // Italian original with English translation
            create(
                text =
                    TranslatableText(
                        originalText = "Punto di vista davvero interessante. Non avevo considerato quell'angolo prima.",
                        originalLanguage = "it",
                        translatedText = "Really interesting point of view. I hadn't considered that angle before.",
                        translatedLanguage = "en",
                    ),
            ),
            // Portuguese original with English translation
            create(
                text =
                    TranslatableText(
                        originalText = "Obrigado pela resposta rápida!",
                        originalLanguage = "pt",
                        translatedText = "Thanks for the quick response!",
                        translatedLanguage = "en",
                    ),
            ),
            // English without translation
            create(
                text =
                    TranslatableText(
                        originalText = "I'll definitely try this approach and let you know how it goes.",
                        originalLanguage = "en",
                    ),
            ),
            // Japanese original with English translation
            create(
                text =
                    TranslatableText(
                        originalText = "この問題を経験した人は他にいますか？それとも私だけですか？",
                        originalLanguage = "ja",
                        translatedText = "Has anyone else experienced this issue? Or is it just me?",
                        translatedLanguage = "en",
                    ),
            ),
            // Korean original with English translation
            create(
                text =
                    TranslatableText(
                        originalText = "이것이 제 문제를 즉시 해결했습니다. 정말 감사합니다!",
                        originalLanguage = "ko",
                        translatedText = "This solved my problem immediately. Much appreciated!",
                        translatedLanguage = "en",
                    ),
            ),
            // English without translation
            create(
                text =
                    TranslatableText(
                        originalText = "I respectfully disagree. Here's why I think differently...",
                        originalLanguage = "en",
                    ),
            ),
            // Dutch original with English translation
            create(
                text =
                    TranslatableText(
                        originalText = "Kunt u enkele bronnen voor deze beweringen delen?",
                        originalLanguage = "nl",
                        translatedText = "Could you share some sources for these claims?",
                        translatedLanguage = "en",
                    ),
            ),
            // Russian original with English translation
            create(
                text =
                    TranslatableText(
                        originalText = "Это для меня настоящий прорыв. Огромное спасибо!",
                        originalLanguage = "ru",
                        translatedText = "This is a game-changer for me. Thank you so much!",
                        translatedLanguage = "en",
                    ),
            ),
            // French original with English translation
            create(
                text =
                    TranslatableText(
                        originalText = "J'apprécie beaucoup cette explication, merci !",
                        originalLanguage = "fr",
                        translatedText = "I really appreciate this explanation, thank you!",
                        translatedLanguage = "en",
                    ),
            ),
            // French original with English translation
            create(
                text =
                    TranslatableText(
                        originalText = "Merci pour votre aide !",
                        originalLanguage = "fr",
                        translatedText = "Thanks for your help!",
                        translatedLanguage = "en",
                    ),
            ),
            // English without translation
            create(
                text =
                    TranslatableText(
                        originalText = "Excellent point! I'll keep that in mind for future reference.",
                        originalLanguage = "en",
                    ),
            ),
            // Chinese original with English translation
            create(
                text =
                    TranslatableText(
                        originalText = "我已经尝试解决这个问题好几周了。你真是救星！",
                        originalLanguage = "zh",
                        translatedText = "I've been trying to solve this for weeks. You're a lifesaver!",
                        translatedLanguage = "en",
                    ),
            ),
            // Arabic original with English translation
            create(
                text =
                    TranslatableText(
                        originalText = "هذا يبدو رائعًا! سأجربه على الفور.",
                        originalLanguage = "ar",
                        translatedText = "This looks great! I'll try it right away.",
                        translatedLanguage = "en",
                    ),
            ),
            // Swedish original with English translation
            create(
                text =
                    TranslatableText(
                        originalText = "Perfekt förklaring! Det var precis vad jag behövde.",
                        originalLanguage = "sv",
                        translatedText = "Perfect explanation! That was exactly what I needed.",
                        translatedLanguage = "en",
                    ),
            ),
        )

    fun create(
        id: String = "reply_${UUID.randomUUID()}",
        parentId: String = MockComments.all.random().id,
        creationDate: Date =
            Date(
                System.currentTimeMillis() -
                    (5..120).random().minutes.toLong(
                        DurationUnit.MILLISECONDS,
                    ),
            ),
        updateDate: Date = creationDate,
        localeLanguage: String = "en",
        status: Status = Status.Published,
        author: Author = MockMinimalProfiles.all.random(),
        text: TranslatableText =
            TranslatableText(
                originalText = "",
                originalLanguage = "en",
            ),
        media: Medias? = null, // Replies less likely to have media
        reactions: List<ReactionCount> = MockReactions.all.shuffled().take((0..4).random()),
        reaction: Reaction? =
            (listOf(null) + reactions.takeIf { it.isNotEmpty() }?.random())
                .random()
                ?.let { Reaction(it.reactionKind) },
        comment: Comment? = MockComments.all.firstOrNull { it.id == parentId },
    ): Comment.Reply =
        Comment.Reply(
            id = id,
            parentId = parentId,
            creationDate = creationDate,
            updateDate = updateDate,
            localeLanguage = localeLanguage,
            status = status,
            author = author,
            text = text,
            medias = media,
            reactions = reactions,
            userReaction = reaction,
            comment = comment,
        )
}
