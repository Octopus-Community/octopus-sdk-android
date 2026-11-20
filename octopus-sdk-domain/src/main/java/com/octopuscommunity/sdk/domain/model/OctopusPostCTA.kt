package com.octopuscommunity.sdk.domain.model

import android.net.Uri

/**
 * A call-to-action button attached to a post published via Bridge
 * Share. The CTA is set by the host app at SDK-open time and is
 * **invisible inside the editor** — the user cannot view, edit, or
 * remove it before publishing.
 *
 * Build it with a [localizedLabel]: [OctopusLocalizedText.PerLanguage] to give the label in each
 * language of the app, [OctopusLocalizedText.AutoTranslated] to let Octopus translate one label.
 * The constructor taking a `String` label means `AutoTranslated(label)`.
 *
 * Keep each label under 28 characters so that it fits on one line: a longer label is accepted,
 * never refused, and the button can then take several lines.
 *
 * **Validation timing.** [label] and [url] are validated by the
 * [OctopusPrefilledPost] constructor when a CTA is attached to a prefilled
 * payload — empty / blank `label` and empty / blank `url` are
 * hard-rejected at the SDK boundary so client-integration bugs
 * surface during client QA rather than after the editor opens.
 * Constructing an [OctopusPostCTA] on its own performs **no**
 * validation — the bytes only matter when attached. A per-language label is validated earlier, when
 * the [OctopusLocalizedText.PerLanguage] itself is built.
 *
 * **`copy` does not carry the per-language labels**: it builds an auto-translated CTA from [url] and
 * [label]. To change the URL of a per-language CTA, build a new one with its [localizedLabel].
 *
 * @property url URL opened when the user taps the button. May be a
 * custom scheme registered by the host (deeplink), an `https://` URL,
 * or any other URL the OS knows how to route.
 * @property label Text shown on the button: the label itself when auto-translated, the label of the
 * default language when given per language.
 */
data class OctopusPostCTA
    @Deprecated(
        "Use the constructor taking an OctopusLocalizedText: OctopusLocalizedText.PerLanguage to give the label " +
            "per language, OctopusLocalizedText.AutoTranslated to keep the automatic translation.",
        ReplaceWith(
            "OctopusPostCTA(url, OctopusLocalizedText.AutoTranslated(label))",
            "com.octopuscommunity.sdk.domain.model.OctopusLocalizedText",
        ),
        level = DeprecationLevel.WARNING,
    )
    constructor(
        val url: Uri,
        val label: String,
    ) {
        /**
         * The label of the button, auto-translated or per language. [label] is its
         * [OctopusLocalizedText.defaultText].
         */
        var localizedLabel: OctopusLocalizedText = OctopusLocalizedText.AutoTranslated(label)
            private set

        /**
         * A CTA whose label is given per language, or auto-translated.
         *
         * @param url URL opened when the user taps the button.
         * @param localizedLabel The label of the button.
         */
        @Suppress("DEPRECATION")
        constructor(
            url: Uri,
            localizedLabel: OctopusLocalizedText,
        ) : this(url = url, label = localizedLabel.defaultText) {
            this.localizedLabel = localizedLabel
        }

        override fun equals(other: Any?): Boolean =
            this === other ||
                (other is OctopusPostCTA && url == other.url && label == other.label && localizedLabel == other.localizedLabel)

        override fun hashCode(): Int = listOf(url, label, localizedLabel).hashCode()

        override fun toString(): String = "OctopusPostCTA(url=$url, label=$label, localizedLabel=$localizedLabel)"
    }
