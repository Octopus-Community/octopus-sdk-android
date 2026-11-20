package com.octopuscommunity.sdk.domain.model

import java.util.Collections
import java.util.Locale

/**
 * A text the host app gives either for automatic translation or per language.
 *
 * It is the label of the button of a Bridge Share ([OctopusPostCTA]) and of a bridge post
 * ([ClientPost]):
 * - [AutoTranslated]: one text, which Octopus translates automatically for every reader.
 * - [PerLanguage]: one text per language the app has, plus the default language. A reader whose
 *   language is in the list sees that text as is; any other reader sees an automatic translation.
 */
sealed class OctopusLocalizedText {
    /**
     * The text to start from: the text itself when [AutoTranslated], the text of the default
     * language when [PerLanguage].
     */
    abstract val defaultText: String

    /**
     * One text, translated automatically by Octopus for every reader. This is what a plain `String`
     * label means.
     *
     * @property text The text, in any language.
     */
    class AutoTranslated(
        val text: String,
    ) : OctopusLocalizedText() {
        override val defaultText: String get() = text

        override fun equals(other: Any?): Boolean = this === other || (other is AutoTranslated && text == other.text)

        override fun hashCode(): Int = text.hashCode()

        override fun toString(): String = "AutoTranslated(text=$text)"
    }

    /**
     * One text per language, with the language to start from for the readers whose language is not
     * listed.
     *
     * Each language is sent as a code built from the parts of its [Locale]: the language, then `-`
     * and the script when it has one, then `-` and the country when it has one (`fr`, `pt-BR`,
     * `zh-Hant-TW`). Variants and extensions are dropped. Nothing else is checked: a code Octopus
     * cannot read matches no reader, so its text is never shown.
     *
     * When the content is signed, your backend signs the codes that are sent, not the locales your
     * app wrote. Hebrew, Indonesian and Yiddish are always sent with their current codes `he`, `id`
     * and `yi`, as on iOS, even where Android keeps the old codes `iw`, `in` and `ji` in a [Locale].
     * Give a language, with a country if needed and no script (`fr`, `pt-BR`, `zh-HK`): such a code
     * is sent as it is written. Build the locale from its parts or from
     * a tag with hyphens (`Locale("pt", "BR")`, `Locale.forLanguageTag("pt-BR")`): a string with an
     * underscore such as `pt_BR` is not a tag. Read [textsByLanguageCode] to see the codes that are
     * sent.
     *
     * @param texts The text of each language. Not empty; no blank text; no [Locale] without a
     *   language, such as [Locale.ROOT], or with the undetermined language `und`; no two locales
     *   giving the same code.
     * @param defaultLanguage The language of the text to show and translate when none matches the
     *   reader. Its code must be the code of one of [texts].
     * @throws ValidationError when [texts] or [defaultLanguage] break one of the rules above.
     */
    class PerLanguage(
        texts: Map<Locale, String>,
        defaultLanguage: Locale,
    ) : OctopusLocalizedText() {
        /** The text of each language, as given. */
        val texts: Map<Locale, String> = Collections.unmodifiableMap(LinkedHashMap(texts))

        /** The language of [defaultText], as given. */
        val defaultLanguage: Locale = defaultLanguage

        /** The code sent for [defaultLanguage], such as `en` or `pt-BR`. */
        val defaultLanguageCode: String

        /** The text of each language by the code sent for it, [defaultLanguageCode] included. */
        val textsByLanguageCode: Map<String, String>

        init {
            if (texts.isEmpty()) throw ValidationError.EmptyTexts
            val byCode = LinkedHashMap<String, String>(texts.size)
            texts.forEach { (locale, text) ->
                val code = locale.toLanguageCode() ?: throw ValidationError.EmptyLanguage
                if (text.isBlank()) throw ValidationError.BlankText(code)
                if (byCode.put(code, text) != null) throw ValidationError.DuplicateLanguage(code)
            }
            val defaultCode = defaultLanguage.toLanguageCode() ?: throw ValidationError.EmptyLanguage
            if (defaultCode !in byCode) throw ValidationError.DefaultLanguageMissing(defaultCode)
            defaultLanguageCode = defaultCode
            textsByLanguageCode = Collections.unmodifiableMap(byCode)
        }

        override val defaultText: String get() = textsByLanguageCode.getValue(defaultLanguageCode)

        override fun equals(other: Any?): Boolean =
            this === other ||
                (
                    other is PerLanguage &&
                        defaultLanguageCode == other.defaultLanguageCode &&
                        textsByLanguageCode == other.textsByLanguageCode
                )

        override fun hashCode(): Int = listOf(defaultLanguageCode, textsByLanguageCode).hashCode()

        override fun toString(): String = "PerLanguage(texts=$textsByLanguageCode, defaultLanguage=$defaultLanguageCode)"
    }

    /**
     * Why a [PerLanguage] text was refused at construction. Subclasses `IllegalArgumentException`,
     * so a caller that does not need the reason can catch that.
     *
     * It is not an [OctopusPrefilledPost.ValidationError]: a new subclass there would break the
     * exhaustive `when` callers wrote over those reasons.
     */
    sealed class ValidationError(
        message: String,
    ) : IllegalArgumentException(message) {
        /** No text was given. */
        object EmptyTexts : ValidationError(
            "OctopusLocalizedText.ValidationError.EmptyTexts: at least one text must be provided.",
        )

        /** A locale without a language, such as `Locale.ROOT`, or with the undetermined `und`, was given. */
        object EmptyLanguage : ValidationError(
            "OctopusLocalizedText.ValidationError.EmptyLanguage: every locale must have a language.",
        )

        /**
         * The text of a language is empty or whitespace-only.
         *
         * @property languageCode The code of that language.
         */
        data class BlankText(
            val languageCode: String,
        ) : ValidationError(
                "OctopusLocalizedText.ValidationError.BlankText: the text of `$languageCode` must not be empty or blank.",
            )

        /**
         * Two locales give the same code, such as `en-US` and `en-US` with a calendar extension.
         *
         * @property languageCode The code they share.
         */
        data class DuplicateLanguage(
            val languageCode: String,
        ) : ValidationError(
                "OctopusLocalizedText.ValidationError.DuplicateLanguage: two texts are given for `$languageCode`.",
            )

        /**
         * The default language is not exactly one of the languages given: `en-US` when only `en` is.
         *
         * @property languageCode The code of the default language.
         */
        data class DefaultLanguageMissing(
            val languageCode: String,
        ) : ValidationError(
                "OctopusLocalizedText.ValidationError.DefaultLanguageMissing: " +
                    "no text is given for the default language `$languageCode`.",
            )
    }
}

/**
 * The code sent for this locale: its language, then `-` and its script when it has one, then `-`
 * and its country when it has one. `null` when it has no language, or the undetermined `und`: no
 * reader has it, so a text sent under it would never be shown (`Locale.forLanguageTag("und")` gives
 * no language, but `Locale("und")` keeps it). The language goes through [currentLanguageCode], so
 * the code is the same on every Android version and on iOS.
 */
private fun Locale.toLanguageCode(): String? =
    language.takeIf { it.isNotEmpty() && !it.equals("und", ignoreCase = true) }?.let { language ->
        listOf(currentLanguageCode(language), script, country).filter { it.isNotEmpty() }.joinToString("-")
    }

/**
 * The current code of a language: `he`, `id` and `yi` for the old codes `iw`, `in` and `ji` that
 * Android keeps in a [Locale] (a recent JVM already gives the current ones); any other code as is.
 * The codes enter the signed content, so they must not depend on the platform.
 */
internal fun currentLanguageCode(language: String): String =
    when (language) {
        "iw" -> "he"
        "in" -> "id"
        "ji" -> "yi"
        else -> language
    }
