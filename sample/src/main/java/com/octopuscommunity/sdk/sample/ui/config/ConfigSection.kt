package com.octopuscommunity.sdk.sample.ui.config

/**
 * The addressable sections of [ConfigScreen], in display order.
 *
 * A link elsewhere in the sample that is about one Config setting passes the section that holds it
 * (through [com.octopuscommunity.sdk.sample.ui.navigation.ConfigRoute.section]), and Config opens
 * with that section expanded and scrolled into view; without an anchor every section opens
 * collapsed (the Community one summarises its choice in its header).
 *
 * [tag] is the section's stable short name, used as its test tag suffix (`config-section-<tag>`).
 * The `octopus-sample://config` deep link takes the enum name (`?section=INTEGRATION`).
 */
enum class ConfigSection(
    val tag: String,
) {
    /** "Server environment": env, host, selected environment. Demo flavor only. */
    SERVER("server"),

    /**
     * "Community": the API key source (named presets, custom key). Under the server it belongs to
     * and collapsed by default — it is rarely changed. A keyless build opens it anyway (see
     * [ConfigScreen]), since a key must be pasted there.
     */
    COMMUNITY("community"),

    /** "Connect your users (SSO)": the SSO user, Force login (read-only here) and Unified profile. */
    AUTH("auth"),

    /** "Theme": the SDK theme preset. */
    THEME("theme"),

    /**
     * The "Host app integration" row at the bottom of Config. The host callbacks and the URL policy
     * it used to hold moved to Settings › Developer tools › Host app integration; the anchor is
     * kept so an existing `?section=INTEGRATION` link keeps resolving. Once the SDK is up the root
     * graph forwards that link to the new screen; before, Config scrolls to the row pointing there.
     */
    INTEGRATION("integration"),
    ;

    /**
     * The section Config actually opens on for this anchor. [SERVER] does not exist on the prod
     * flavor, so a link aimed at it lands on [COMMUNITY] — the API key half of a "check the server
     * and API key" link — rather than on nothing.
     */
    fun resolve(serverSectionShown: Boolean): ConfigSection = if (this == SERVER && !serverSectionShown) COMMUNITY else this

    /**
     * Whether this section starts expanded when Config opens on [anchor]: only the anchored one —
     * every section is collapsed by default.
     */
    fun isInitiallyExpanded(anchor: ConfigSection?): Boolean = this == anchor
}
