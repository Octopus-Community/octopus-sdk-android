@file:Suppress("ktlint:standard:max-line-length")

package com.octopuscommunity.sdk.test.mock

import android.util.Size
import com.octopuscommunity.sdk.domain.model.Author
import com.octopuscommunity.sdk.domain.model.ClientPost
import com.octopuscommunity.sdk.domain.model.Comment
import com.octopuscommunity.sdk.domain.model.Media
import com.octopuscommunity.sdk.domain.model.Medias
import com.octopuscommunity.sdk.domain.model.OctopusItem.Feeds
import com.octopuscommunity.sdk.domain.model.OctopusItem.Reaction
import com.octopuscommunity.sdk.domain.model.OctopusItem.ReactionCount
import com.octopuscommunity.sdk.domain.model.OctopusLocalizedText
import com.octopuscommunity.sdk.domain.model.Post
import com.octopuscommunity.sdk.domain.model.Resource
import com.octopuscommunity.sdk.domain.model.Status
import com.octopuscommunity.sdk.domain.model.TranslatableText
import java.util.Date
import java.util.UUID
import kotlin.time.Duration.Companion.hours
import kotlin.time.DurationUnit

/** Mock [Post]s, with text, poll, client object, featured comment or video. */
object MockPosts {
    val default =
        create(
            id = "fake_post_0",
            status = Status.Published,
            author = MockMinimalProfiles.default,
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
                            dimensions = Size(1920, 1080),
                            fileSize = null,
                        ),
                    ),
                ),
        )

    val withPoll =
        create(
            id = "fake_post_1",
            status = Status.Published,
            author = MockMinimalProfiles.default,
            text =
                TranslatableText(
                    originalText = "Quel film avez-vous préféré?",
                    originalLanguage = "fr",
                    translatedText = "What movie did you prefer?",
                    translatedLanguage = "en",
                ),
            poll = MockPostPolls.default,
            reactions = listOf(),
        )

    val withClientObject =
        create(
            id = "fake_post_2",
            status = Status.Published,
            author = MockMinimalProfiles.default,
            text =
                TranslatableText(
                    originalText = MockClientPosts.default.text,
                    originalLanguage = "en",
                ),
            poll = null,
            clientObject =
                MockClientPosts.default.let { clientPost ->
                    Post.ClientObject(
                        id = clientPost.objectId,
                        catchPhrase =
                            clientPost.catchPhrase?.let {
                                TranslatableText(
                                    originalText = it,
                                    originalLanguage = "en",
                                )
                            },
                        ctaText =
                            clientPost.viewObjectButtonText?.let {
                                TranslatableText(
                                    originalText = it,
                                    originalLanguage = "en",
                                )
                            },
                    )
                },
        )

    val withComment =
        create(
            id = "fake_post_3",
            status = Status.Published,
            author = MockMinimalProfiles.default,
            text = default.text,
            poll = null,
            featuredComment = MockComments.default,
        )

    val withVideo =
        create(
            status = Status.Published,
            author = MockMinimalProfiles.default,
            text = default.text,
        )

    /** A post carrying three images, swiped in a carousel with dots. */
    val withThreeImages =
        create(
            id = "fake_post_three_images",
            status = Status.Published,
            author = MockMinimalProfiles.default,
            text = default.text,
            media = Medias(images = multiImages(count = 3)),
            poll = null,
        )

    /** A post carrying eight images and no text, swiped in a carousel with dots and a counter. */
    val withEightImages =
        create(
            id = "fake_post_eight_images",
            status = Status.Published,
            author = MockMinimalProfiles.default,
            media = Medias(images = multiImages(count = 8)),
            poll = null,
        )

    val all =
        listOf(
            default,
            withPoll,
            withClientObject,
            withComment,
            // English without translation
            create(
                text =
                    TranslatableText(
                        originalText = "Major scientific breakthrough announced today. Researchers have successfully demonstrated quantum teleportation over 100 kilometers.",
                        originalLanguage = "en",
                    ),
            ),
            // English without translation
            create(
                text =
                    TranslatableText(
                        originalText = "Tech Review: The latest gadgets compared",
                        originalLanguage = "en",
                    ),
                poll = MockPostPolls.yesNoPoll,
            ),
            // Spanish original with English translation
            create(
                text =
                    TranslatableText(
                        originalText = "Explorando joyas ocultas en el sudeste asiático. Desde las playas vírgenes de Myanmar hasta los templos antiguos de Laos.",
                        originalLanguage = "es",
                        translatedText = "Exploring hidden gems in Southeast Asia. From the untouched beaches of Myanmar to the ancient temples of Laos.",
                        translatedLanguage = "en",
                    ),
            ),
            // French original with English translation
            create(
                text =
                    TranslatableText(
                        originalText = "Cinq façons simples d'améliorer vos compétences culinaires. Les chefs partagent leurs secrets.",
                        originalLanguage = "fr",
                        translatedText = "Five easy ways to improve your culinary skills. Master chefs share their secrets.",
                        translatedLanguage = "en",
                    ),
            ),
            // German original with English translation
            create(
                text =
                    TranslatableText(
                        originalText = "Umweltalarm: Neue Studien zeigen besorgniserregende Klimatrends",
                        originalLanguage = "de",
                        translatedText = "Environmental Alert: New studies show concerning climate trends",
                        translatedLanguage = "en",
                    ),
            ),
            // English without translation
            create(
                text =
                    TranslatableText(
                        originalText = "Experts predict global economic growth to slow in the coming year. Key factors include trade tensions and geopolitical uncertainties.",
                        originalLanguage = "en",
                    ),
            ),
            // Italian original with English translation
            create(
                text =
                    TranslatableText(
                        originalText = "Nuovo metodo di esercizio rivoluzionario promette risultati in metà tempo. Gli appassionati di fitness valutano la sua efficacia.",
                        originalLanguage = "it",
                        translatedText = "Revolutionary new exercise method promises results in half the time. Fitness enthusiasts evaluate its effectiveness.",
                        translatedLanguage = "en",
                    ),
            ),
            // English without translation
            create(
                text =
                    TranslatableText(
                        originalText = "NASA announces plans for the first crewed mission to Mars. Timeline and challenges discussed in detail.",
                        originalLanguage = "en",
                    ),
            ),
            // Japanese original with English translation
            create(
                text =
                    TranslatableText(
                        originalText = "革新的な教育方法が従来の教室を変革しています。生徒たちのエンゲージメントが向上しています。",
                        originalLanguage = "ja",
                        translatedText = "Innovative teaching methods are transforming traditional classrooms. Student engagement is improving.",
                        translatedLanguage = "en",
                    ),
                poll = MockPostPolls.all.random(),
            ),
            // French original with English translation
            create(
                text =
                    TranslatableText(
                        originalText = "L'équipe outsider remporte une victoire historique. L'entraîneur attribue le succès à la persévérance.",
                        originalLanguage = "fr",
                        translatedText = "Underdog team makes historic win. Coach credits success to perseverance.",
                        translatedLanguage = "en",
                    ),
            ),
            // English without translation
            create(
                text =
                    TranslatableText(
                        originalText = "Controversial new art installation sparks debate. Critics discuss the boundaries of artistic expression.",
                        originalLanguage = "en",
                    ),
            ),
            // Korean original with English translation
            create(
                text =
                    TranslatableText(
                        originalText = "스타트업이 획기적인 AI 기술을 공개했습니다. 의료부터 우주 탐사까지 응용 가능합니다.",
                        originalLanguage = "ko",
                        translatedText = "Startup unveils groundbreaking AI technology. Applications range from healthcare to space exploration.",
                        translatedLanguage = "en",
                    ),
            ),
            // English without translation
            create(
                text =
                    TranslatableText(
                        originalText = "Sustainable fashion takes center stage at Paris Fashion Week. Designers showcase eco-friendly materials.",
                        originalLanguage = "en",
                    ),
            ),
            // Portuguese original with English translation
            create(
                text =
                    TranslatableText(
                        originalText = "Análise profunda das próximas eleições: temas principais e candidatos examinados.",
                        originalLanguage = "pt",
                        translatedText = "In-depth look at the upcoming election: key issues and candidates examined.",
                        translatedLanguage = "en",
                    ),
            ),
            // Arabic original with English translation
            create(
                text =
                    TranslatableText(
                        originalText = "التكملة المنتظرة تفوق التوقعات. النقاد يشيدون بالسرد المبتكر.",
                        originalLanguage = "ar",
                        translatedText = "Highly anticipated sequel surpasses expectations. Critics praise innovative storytelling.",
                        translatedLanguage = "en",
                    ),
            ),
            // Chinese original with English translation
            create(
                text =
                    TranslatableText(
                        originalText = "研究人员在热带雨林中发现新物种。",
                        originalLanguage = "zh",
                        translatedText = "Researchers identify new species in rainforest.",
                        translatedLanguage = "en",
                    ),
            ),
            // English without translation
            create(
                text =
                    TranslatableText(
                        originalText = "Major corporation announces radical shift in business model. Industry experts analyze market impacts.",
                        originalLanguage = "en",
                    ),
            ),
            // Russian original with English translation
            create(
                text =
                    TranslatableText(
                        originalText = "Музыкальный фестиваль побил рекорды посещаемости.",
                        originalLanguage = "ru",
                        translatedText = "Music festival breaks attendance records.",
                        translatedLanguage = "en",
                    ),
            ),
            // Dutch original with English translation
            create(
                text =
                    TranslatableText(
                        originalText = "Stad onthult ambitieuze plannen voor duurzame stedelijke vernieuwing.",
                        originalLanguage = "nl",
                        translatedText = "City unveils ambitious plans for sustainable urban renewal.",
                        translatedLanguage = "en",
                    ),
            ),
            // Appended last so the indexes the QA fixtures pin stay put.
            withThreeImages,
            withEightImages,
        )

    fun create(
        id: String = "post_${UUID.randomUUID()}",
        parentId: String = MockTopics.all.random().id,
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
        poll: Post.Poll? = null,
        commentsFeeds: Feeds = MockFeeds.Comment.all.random(),
        viewCount: Int = (0..1000).random(),
        commentCount: Int = listOf(0, (1..100).random()).random(),
        reactions: List<ReactionCount> = MockReactions.all.shuffled().take((0..4).random()),
        reaction: Reaction? =
            (listOf(null) + reactions.takeIf { it.isNotEmpty() }?.random())
                .random()
                ?.let { Reaction(it.reactionKind) },
        pollVoteId: String? = poll?.results?.map { it.answerId }?.random(),
        clientObject: Post.ClientObject? = null,
        customAction: Post.CustomAction? = null,
        featuredComment: Comment? = listOf(null, MockComments.all.random()).random(),
    ) = Post(
        id = id,
        parentId = parentId,
        creationDate = creationDate,
        updateDate = updateDate,
        localeLanguage = localeLanguage,
        status = status,
        author = author,
        text = text,
        medias = media,
        poll = poll,
        commentsFeeds = commentsFeeds,
        viewCount = viewCount,
        commentCount = commentCount,
        reactions = reactions,
        userReaction = reaction,
        pollVoteId = pollVoteId,
        group = MockTopics.all.firstOrNull { it.id == parentId },
        clientObject = clientObject,
        customAction = customAction,
        featuredComment = featuredComment,
    )
}

/** Mock polls. */
object MockPostPolls {
    /** Five-answer poll. Its answer ids are fixed: `default_poll_answer_0` to `default_poll_answer_4`. */
    val default =
        createPoll(
            answerIdPrefix = "default_poll_answer",
            answersVoteCount =
                listOf(
                    TranslatableText(
                        originalText = "Avatar 3",
                        originalLanguage = "fr",
                        translatedText = "Avatar 3",
                        translatedLanguage = "en",
                    ) to 10,
                    TranslatableText(
                        originalText = "Jurassic World : Renaissance",
                        originalLanguage = "fr",
                        translatedText = "Jurassic World: Renaissance",
                        translatedLanguage = "en",
                    ) to 15,
                    TranslatableText(
                        originalText = "Lilo & Stitch",
                        originalLanguage = "fr",
                        translatedText = "Lilo & Stitch",
                        translatedLanguage = "en",
                    ) to 2,
                    TranslatableText(
                        originalText = "Mickey 17",
                        originalLanguage = "fr",
                        translatedText = "Mickey 17",
                        translatedLanguage = "en",
                    ) to 30,
                    TranslatableText(
                        originalText = "Warfare",
                        originalLanguage = "fr",
                        translatedText = "Warfare",
                        translatedLanguage = "en",
                    ) to 40,
                ),
        )

    /** Yes/no poll. Its answer ids are fixed: `yes_no_poll_answer_0` (yes) and `yes_no_poll_answer_1` (no). */
    val yesNoPoll =
        createPoll(
            answerIdPrefix = "yes_no_poll_answer",
            answersVoteCount =
                listOf(
                    TranslatableText(
                        originalText = "Oui",
                        originalLanguage = "fr",
                        translatedText = "Yes",
                        translatedLanguage = "en",
                    ) to 75,
                    TranslatableText(
                        originalText = "Non",
                        originalLanguage = "fr",
                        translatedText = "No",
                        translatedLanguage = "en",
                    ) to 25,
                ),
        )

    val all =
        listOf(
            default,
            yesNoPoll,
            createPoll(
                answersVoteCount =
                    listOf(
                        TranslatableText(
                            originalText = "Option A",
                            originalLanguage = "fr",
                            translatedText = "Option A",
                            translatedLanguage = "en",
                        ) to 42,
                        TranslatableText(
                            originalText = "Option B",
                            originalLanguage = "fr",
                            translatedText = "Option B",
                            translatedLanguage = "en",
                        ) to 58,
                    ),
            ),
            createPoll(
                answersVoteCount =
                    listOf(
                        TranslatableText(
                            originalText = "Rouge",
                            originalLanguage = "fr",
                            translatedText = "Red",
                            translatedLanguage = "en",
                        ) to 30,
                        TranslatableText(
                            originalText = "Bleu",
                            originalLanguage = "fr",
                            translatedText = "Blue",
                            translatedLanguage = "en",
                        ) to 20,
                        TranslatableText(
                            originalText = "Vert",
                            originalLanguage = "fr",
                            translatedText = "Green",
                            translatedLanguage = "en",
                        ) to 50,
                    ),
            ),
            createPoll(
                answersVoteCount =
                    listOf(
                        TranslatableText(
                            originalText = "Pizza",
                            originalLanguage = "it",
                            translatedText = "Pizza",
                            translatedLanguage = "en",
                        ) to 45,
                        TranslatableText(
                            originalText = "Hamburger",
                            originalLanguage = "de",
                            translatedText = "Burger",
                            translatedLanguage = "en",
                        ) to 35,
                        TranslatableText(
                            originalText = "Sushi",
                            originalLanguage = "ja",
                            translatedText = "Sushi",
                            translatedLanguage = "en",
                        ) to 20,
                    ),
            ),
        )

    fun createPoll(answersVoteCount: List<Pair<TranslatableText, Int>>): Post.Poll = createPoll(answersVoteCount, answerIdPrefix = null)

    private fun createPoll(
        answersVoteCount: List<Pair<TranslatableText, Int>>,
        answerIdPrefix: String?,
    ): Post.Poll {
        val answers =
            answersVoteCount.mapIndexed { index, (text, _) ->
                if (answerIdPrefix == null) createAnswer(text = text) else createAnswer(text = text, id = "${answerIdPrefix}_$index")
            }

        val results =
            answersVoteCount.mapIndexed { index, (_, voteCount) ->
                Post.Poll.Result(answerId = answers[index].id, voteCount = voteCount)
            }

        return Post.Poll(answers = answers, results = results)
    }

    fun createAnswer(
        text: TranslatableText,
        id: String = "poll_answer_${UUID.randomUUID()}",
    ) = Post.Poll.Answer(
        id = id,
        text = text,
    )
}

/** Mock client posts. */
object MockClientPosts {
    val default =
        ClientPost(
            objectId = "client_object_${UUID.randomUUID()}",
            text = "27,5 Hardtail Electric Mountain Bike E-ST 100 - White/Red.",
            catchPhrase = "Share your thoughts on this interesting topic if you'd like, it's cool",
            viewObjectButton = OctopusLocalizedText.AutoTranslated("View More"),
        )

    val productReview =
        ClientPost(
            objectId = "product_${UUID.randomUUID()}",
            text = "Pilates: what is the reformer and how to get started.",
            catchPhrase = "What do you think about this product?",
            viewObjectButton = OctopusLocalizedText.AutoTranslated("See Product"),
        )

    val all =
        listOf(
            default,
            productReview,
        )
}

/**
 * [count] images alternating the two sample pictures, the first one in landscape: the carousel takes
 * its height from it and crops the portrait ones. The fragment only keeps their cache keys apart.
 */
private fun multiImages(count: Int) =
    List(count) { index ->
        val landscape = index % 2 == 0
        MockMedias.createImage(
            url =
                if (landscape) {
                    "https://images.squarespace-cdn.com/content/v1/672b2ffd96d8a54dcce38373/1730883582404-8OCZ52CUBOE1WTX3GBCH/Octopus_logo_RVB_SM_couleur.png#$index"
                } else {
                    "https://play-lh.googleusercontent.com/H6umM_74J78u2KrdnnnDwwLctekjZg5kghkx6LSSeQvt5plFI3E98bjWaLZm8lds0ixg=w480-h960-rw#$index"
                },
            dimensions = if (landscape) Size(1920, 1080) else Size(480, 960),
            size = null,
            name = "Image ${index + 1}",
            contentType = if (landscape) "image/png" else "image/webp",
        )
    }
