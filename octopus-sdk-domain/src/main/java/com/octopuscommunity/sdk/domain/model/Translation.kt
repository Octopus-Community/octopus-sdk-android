package com.octopuscommunity.sdk.domain.model

/**
 * A text that can be translated into the language of the device
 *
 * The translated text might be `null` if there is not translation for this text or if the original
 * text is already in the language of the device.
 *
 * @property original Original text
 * @property translated Translated version of the text if the device language is  different from the
 * original language and translation exist (in requested language or default english)
 */
data class TranslatableText(val original: Content, val translated: Content? = null) {
    data class Content(val text: String, val language: String)

    val hasTranslation = translated != null

    constructor(
        originalText: String,
        originalLanguage: String,
        translatedText: String? = null,
        translatedLanguage: String? = null
    ) : this(
        original = Content(
            text = originalText.trim(),
            language = originalLanguage
        ),
        translated = translatedText?.trim()?.ifEmpty { null }?.let { translatedText ->
            Content(
                text = translatedText,
                language = translatedLanguage ?: ""
            )
        }
    )

    /**
     * Returns the translated text if available, otherwise the original text.
     * This is the text that should be displayed to the user.
     */
    fun getText(translated: Boolean): String = when (translated) {
        true -> this.translated ?: this.original
        else -> this.original
    }.text
}