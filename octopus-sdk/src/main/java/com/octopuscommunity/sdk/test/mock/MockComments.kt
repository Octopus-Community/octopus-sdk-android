@file:Suppress("ktlint:standard:max-line-length")

package com.octopuscommunity.sdk.test.mock

import com.octopuscommunity.sdk.domain.model.Author
import com.octopuscommunity.sdk.domain.model.Comment
import com.octopuscommunity.sdk.domain.model.Media
import com.octopuscommunity.sdk.domain.model.Medias
import com.octopuscommunity.sdk.domain.model.OctopusItem.Feeds
import com.octopuscommunity.sdk.domain.model.OctopusItem.Reaction
import com.octopuscommunity.sdk.domain.model.OctopusItem.ReactionCount
import com.octopuscommunity.sdk.domain.model.Post
import com.octopuscommunity.sdk.domain.model.Resource
import com.octopuscommunity.sdk.domain.model.Status
import com.octopuscommunity.sdk.domain.model.TranslatableText
import java.util.Date
import java.util.UUID
import kotlin.time.Duration.Companion.hours
import kotlin.time.DurationUnit

/** Mock [Comment]s. */
object MockComments {
    val default =
        create(
            id = "fake_comment_0",
            text =
                TranslatableText(
                    originalText = "Ceci est le contenu de l'article exemple par défaut. Il peut être assez long et contenir plusieurs phrases.",
                    originalLanguage = "fr",
                    translatedText = "This is the content of the default sample post. It can be quite long and contain multiple sentences.",
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

    val withShortText =
        create(
            text =
                TranslatableText(
                    originalText = "This is a small content",
                    originalLanguage = "en",
                    translatedText = "Ceci est un petit contenu",
                    translatedLanguage = "fr",
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
            withShortText,
            // French with English translation
            create(
                text =
                    TranslatableText(
                        originalText = "Percée scientifique majeure annoncée aujourd'hui. Les chercheurs du prestigieux Institut d'études avancées ont réussi à démontrer la téléportation quantique sur une distance de 100 kilomètres.",
                        originalLanguage = "fr",
                        translatedText = "Major scientific breakthrough announced today. Researchers at the prestigious Institute of Advanced Studies have successfully demonstrated quantum teleportation over a distance of 100 kilometers.",
                        translatedLanguage = "en",
                    ),
            ),
            // English without translation
            create(
                text =
                    TranslatableText(
                        originalText = "Tech Review: Latest smartphones compared side by side",
                        originalLanguage = "en",
                    ),
            ),
            // Spanish original with English translation
            create(
                text =
                    TranslatableText(
                        originalText = "Explorando joyas ocultas en el sudeste asiático. Desde las playas vírgenes de Myanmar hasta los antiguos templos de Laos.",
                        originalLanguage = "es",
                        translatedText = "Exploring hidden gems in Southeast Asia. From the untouched beaches of Myanmar to the ancient temples of Laos.",
                        translatedLanguage = "en",
                    ),
            ),
            // French with English translation
            create(
                text =
                    TranslatableText(
                        originalText = "Cinq façons simples d'améliorer vos compétences culinaires. Les chefs maîtres partagent leurs secrets.",
                        originalLanguage = "fr",
                        translatedText = "Five easy ways to improve your culinary skills. Master chefs share their secrets.",
                        translatedLanguage = "en",
                    ),
            ),
            // German original with English translation
            create(
                text =
                    TranslatableText(
                        originalText = "Umweltalarm: Neue Studien zeigen besorgniserregende Trends beim Klimawandel",
                        originalLanguage = "de",
                        translatedText = "Environmental Alert: New studies show concerning climate change trends",
                        translatedLanguage = "en",
                    ),
            ),
            // English without translation
            create(
                text =
                    TranslatableText(
                        originalText = "Breaking news from the financial markets today",
                        originalLanguage = "en",
                    ),
            ),
            // Portuguese original with English translation
            create(
                text =
                    TranslatableText(
                        originalText = "Especialistas preveem desaceleração do crescimento econômico global no próximo ano.",
                        originalLanguage = "pt",
                        translatedText = "Experts predict global economic growth to slow in the coming year.",
                        translatedLanguage = "en",
                    ),
            ),
            // Italian original with English translation
            create(
                text =
                    TranslatableText(
                        originalText = "Nuovo metodo di esercizio rivoluzionario promette risultati in metà tempo.",
                        originalLanguage = "it",
                        translatedText = "Revolutionary new exercise method promises results in half the time.",
                        translatedLanguage = "en",
                    ),
            ),
            // English without translation
            create(
                text =
                    TranslatableText(
                        originalText = "NASA announces plans for the first crewed mission to Mars. Timeline and challenges discussed.",
                        originalLanguage = "en",
                    ),
            ),
            // Japanese original with English translation
            create(
                text =
                    TranslatableText(
                        originalText = "革新的な教育方法が従来の教室を揺るがす。生徒たちはエンゲージメントとテストスコアの向上を示しています。",
                        originalLanguage = "ja",
                        translatedText = "Innovative teaching methods shake up traditional classrooms. Students show improved engagement.",
                        translatedLanguage = "en",
                    ),
            ),
            // French original with English translation
            create(
                text =
                    TranslatableText(
                        originalText = "L'équipe outsider remporte une victoire historique dans le match de championnat.",
                        originalLanguage = "fr",
                        translatedText = "Underdog team makes historic win in championship game.",
                        translatedLanguage = "en",
                    ),
            ),
            // French with English translation
            create(
                text =
                    TranslatableText(
                        originalText = "Une nouvelle installation artistique controversée suscite le débat sur les limites artistiques.",
                        originalLanguage = "fr",
                        translatedText = "Controversial new art installation sparks debate about artistic boundaries.",
                        translatedLanguage = "en",
                    ),
            ),
            // Korean original with English translation
            create(
                text =
                    TranslatableText(
                        originalText = "스타트업이 획기적인 AI 기술을 공개했습니다. 잠재적인 응용 분야는 의료부터 우주 탐사까지 다양합니다.",
                        originalLanguage = "ko",
                        translatedText = "Startup unveils groundbreaking AI technology with applications from healthcare to space exploration.",
                        translatedLanguage = "en",
                    ),
            ),
            // English without translation
            create(
                text =
                    TranslatableText(
                        originalText = "Sustainable fashion takes center stage at Paris Fashion Week this year.",
                        originalLanguage = "en",
                    ),
            ),
            // Spanish original with English translation
            create(
                text =
                    TranslatableText(
                        originalText = "Análisis profundo de las próximas elecciones: temas clave y candidatos examinados.",
                        originalLanguage = "es",
                        translatedText = "In-depth look at the upcoming election: key issues and candidates examined.",
                        translatedLanguage = "en",
                    ),
            ),
            // Arabic original with English translation
            create(
                text =
                    TranslatableText(
                        originalText = "التكملة المنتظرة بشدة تفوق التوقعات. يشيد النقاد بالسرد المبتكر والمؤثرات البصرية المذهلة.",
                        originalLanguage = "ar",
                        translatedText = "Highly anticipated sequel surpasses expectations. Critics praise innovative storytelling.",
                        translatedLanguage = "en",
                    ),
            ),
            // Chinese original with English translation
            create(
                text =
                    TranslatableText(
                        originalText = "研究人员在偏远的热带雨林中发现了新物种。",
                        originalLanguage = "zh",
                        translatedText = "Researchers identify new species in remote rainforest.",
                        translatedLanguage = "en",
                    ),
            ),
            // English without translation
            create(
                text =
                    TranslatableText(
                        originalText = "Major corporation announces radical shift in business model.",
                        originalLanguage = "en",
                    ),
            ),
            // Russian original with English translation
            create(
                text =
                    TranslatableText(
                        originalText = "Ежегодный музыкальный фестиваль побил рекорды посещаемости.",
                        originalLanguage = "ru",
                        translatedText = "Annual music festival breaks attendance records.",
                        translatedLanguage = "en",
                    ),
            ),
            // Swedish original with English translation
            create(
                text =
                    TranslatableText(
                        originalText = "Staden avslöjar ambitiösa planer för hållbar stadsutveckling.",
                        originalLanguage = "sv",
                        translatedText = "City unveils ambitious plans for sustainable urban renewal.",
                        translatedLanguage = "en",
                    ),
            ),
        )

    fun create(
        id: String = "comment_${UUID.randomUUID()}",
        parentId: String = "",
        creationDate: Date = Date(System.currentTimeMillis() - (0L..48.hours.toLong(DurationUnit.MILLISECONDS)).random()),
        updateDate: Date = creationDate,
        localeLanguage: String = "en",
        status: Status = Status.Published,
        author: Author = MockMinimalProfiles.all.random(),
        text: TranslatableText =
            TranslatableText(
                originalText = "",
                originalLanguage = "en",
            ),
        media: Medias? = listOf(MockStrictMedias.create(), null).random(),
        repliesFeeds: Feeds = MockFeeds.Replies.all.random(),
        replyCount: Int = listOf(0, (1..100).random()).random(),
        reactions: List<ReactionCount> = MockReactions.all.shuffled().take((0..4).random()),
        reaction: Reaction? =
            (listOf(null) + reactions.takeIf { it.isNotEmpty() }?.random())
                .random()
                ?.let { Reaction(it.reactionKind) },
        post: Post? = null,
    ) = Comment(
        id = id,
        parentId = parentId,
        creationDate = creationDate,
        updateDate = updateDate,
        localeLanguage = localeLanguage,
        status = status,
        author = author,
        text = text,
        medias = media,
        repliesFeeds = repliesFeeds,
        replyCount = replyCount,
        reactions = reactions,
        userReaction = reaction,
        post = post,
    )
}
