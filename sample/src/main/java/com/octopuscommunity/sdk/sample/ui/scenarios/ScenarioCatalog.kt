package com.octopuscommunity.sdk.sample.ui.scenarios

import com.octopuscommunity.sdk.sample.data.model.FeatureToggle
import com.octopuscommunity.sdk.sample.ui.scenarios.sections.unifiedProfileEffectLine

/**
 * How much of a catalog scenario's preset set this sample actually exercises.
 *
 * Mirrors the shared scenario catalog's Gate B ("all-or-nothing per scenario per
 * platform"): a scenario only counts as `built` there when EVERY one of its preset ids exists in
 * the platform's tree. [PARTIAL] scenarios below expose some but not all of their catalog preset
 * ids for a documented reason (see each [ScenarioSpec.subtitle]) — Gate B still reports them as
 * not-built on Android until the rest lands, same as before this screen existed.
 */
enum class ScenarioStatus {
    BUILT,
    PARTIAL,
    PENDING,
}

/**
 * One row of the Scenarios list / one destination reachable via [ScenarioDetailRoute]-equivalent
 * navigation.
 *
 * [id] is a catalog scenario id from `scenario catalog` (shared) — the same
 * 26 ids the QA pipeline already drives on this platform via `qa-preset-<id>-<n>` test ids. Never
 * rename or remove an [id]: the catalog and shared QA tooling key off it verbatim (F10).
 */
data class ScenarioSpec(
    val id: String,
    /**
     * The gesture, in the words of whoever performs it — verb first, 40 characters at most.
     *
     * Product language, deliberately: the sample is read by integrators *and* by the PO, and the
     * previous titles ("Connection", "Bridge") only spoke to the first. The SDK symbol survives as
     * [api], the monospace chip — the one technical token on the Scenarios tab.
     */
    val title: String,
    val status: ScenarioStatus,
    /** What you will observe once it ran. One sentence, no type names. */
    val subtitle: String,
    /** The same promise on the scenario screen, phrased as the tester checks it. */
    val youWillSee: String,
    /**
     * The engineering caveat that used to live in [subtitle] — which presets are missing and why,
     * what the SDK does not offer yet.
     *
     * Kept, but moved off the card: it is the answer to "why can I not do X here?", which is a
     * question you only ask once you are on the scenario screen. Deleting it instead would have
     * been the cheap way to make the cards read well, and would have lost the only written trace
     * of several known gaps.
     */
    val note: String? = null,
) {
    /** Card test id on the Scenarios list screen: `scenarios-<id>-card`. */
    val cardTestId: String get() = "scenarios-$id-card"

    /** The SDK symbol this scenario is about, shown as the card's monospace pill. */
    val api: String get() = ScenarioCatalog.API_NAMES[id] ?: id

    /** Which of the six list sections the card sits under. */
    val section: ScenarioSection get() = ScenarioCatalog.SECTIONS[id] ?: ScenarioSection.HOST

    /** True when the scenario's effect is observable in the community feed. */
    val isVerifiableInCommunity: Boolean get() = id in ScenarioCatalog.VERIFIABLE_IN_COMMUNITY

    /** The arguments [api] takes, for the detail screen's "Parameters" block. */
    val parameters: List<ScenarioParam> get() = ScenarioCatalog.PARAMETERS[id].orEmpty()

    /**
     * The [com.octopuscommunity.sdk.sample.ui.scenarios.ScenariosViewModel] method-state keys this
     * scenario can write, so its detail screen can show the result of whichever one ran last.
     */
    val methodKeys: List<String> get() = ScenarioCatalog.METHOD_KEYS[id].orEmpty()
}

/**
 * One argument of a scenario's SDK call, as the detail screen shows it: the parameter name, its
 * declared Kotlin type (rendered as a type pill, `String?` and `String` being different answers to
 * "can I pass null here?"), and the value this sample's presets actually send.
 *
 * [value] is descriptive rather than live for the parameters a preset builds itself — a tester
 * reading it should be able to predict what the next tap sends without opening the source.
 */
data class ScenarioParam(
    val name: String,
    val type: String,
    val value: String,
)

/**
 * The six sections the Scenarios list groups its cards under, in display order.
 *
 * Presentation only — the shared catalog knows nothing about them. It defines the ids,
 * titles and statuses; how a given platform *arranges* them is that platform's business, and this
 * is the arrangement the cross-platform design contract fixes for all four samples.
 */
enum class ScenarioSection(
    val title: String,
) {
    SSO("Sign-in & user"),
    PRESENTATION("Presentation modes"),
    COMMUNITY("Community & groups"),
    NOTIFICATIONS("Notifications"),
    THEME("Theme & language"),
    HOST("Host callbacks & events"),
    ;

    /**
     * The feature toggle this section owns, or `null` when nothing about it is switchable.
     *
     * Two sections carry one. Presentation modes deliberately carries none: a presentation mode is
     * something you do, not something you leave on.
     */
    val featureToggle: FeatureToggle?
        get() =
            when (this) {
                SSO -> FeatureToggle.ForceLogin
                NOTIFICATIONS -> FeatureToggle.PushRegistration
                else -> null
            }
}

/**
 * The 28 shared catalog scenarios plus the sample's Unified Profile scenario.
 *
 * This list is the single source of truth for the Scenarios screen (list + detail dispatch). It
 * intentionally does not read the YAML at build time — the ids, titles and pending-reasons are
 * copied here deliberately so this sample keeps compiling standalone, same as every other SDK
 * consumer. Keep it in sync by hand when the catalog changes; `scripts/check-locale-key-parity.py`
 * has no equivalent for this file, so cross-platform drift is caught in QA, not CI.
 */
object ScenarioCatalog {
    internal fun liveSections(unifiedProfileActive: Boolean): Map<ScenarioSection, List<ScenarioSpec>> =
        bySection().mapValues { (_, specs) ->
            specs.map { spec ->
                if (spec.id == "unifiedProfile") {
                    spec.copy(subtitle = unifiedProfileEffectLine(unifiedProfileActive))
                } else {
                    spec
                }
            }
        }

    val all: List<ScenarioSpec> =
        listOf(
            ScenarioSpec(
                id = "unifiedProfile",
                title = "Unified Profile",
                status = ScenarioStatus.BUILT,
                subtitle = "Open member profiles in the host app or in the community.",
                youWillSee =
                    "with the switch on, a member with an app id opens Client profile and the home " +
                        "profile button opens Activity; with it off, both open native profiles.",
            ),
            ScenarioSpec(
                id = "connection",
                title = "Sign in and out as the demo user",
                status = ScenarioStatus.PARTIAL,
                subtitle =
                    "The community shows the demo user as signed in: they can post, react and " +
                        "be notified.",
                youWillSee =
                    "the Community tab greets the demo user, and the composer unlocks.",
                note =
                    "Presets 2-4 (sign in as Premium / Moderator / both) need entitlement " +
                        "injection the sign-in call does not take today — only presets 1 and 5 are " +
                        "built.",
            ),
            ScenarioSpec(
                id = "groups",
                title = "Browse the community's groups",
                status = ScenarioStatus.BUILT,
                subtitle =
                    "Every group the community offers, with whether the demo user follows it.",
                youWillSee =
                    "the group list, with a follow state on each row.",
            ),
            ScenarioSpec(
                id = "syncFollowGroups",
                title = "Follow or unfollow every group at once",
                status = ScenarioStatus.BUILT,
                subtitle =
                    "The follow state of every group flips in one call, and the community " +
                        "reflects it.",
                youWillSee =
                    "every group's follow state matching what you just applied.",
            ),
            ScenarioSpec(
                id = "notSeenNotifications",
                title = "Set the unread notification count",
                status = ScenarioStatus.BUILT,
                subtitle =
                    "The notification badge in the community carries the number you set.",
                youWillSee =
                    "the notification badge carrying the count you sent.",
            ),
            ScenarioSpec(
                id = "pushNotifications",
                title = "Open a post from a push notification",
                status = ScenarioStatus.BUILT,
                subtitle =
                    "A simulated push is recognised as an Octopus one and opens the post it " +
                        "points at.",
                youWillSee =
                    "the community opening on the post the notification named.",
            ),
            ScenarioSpec(
                id = "communityAccess",
                title = "Grant or deny access to the community",
                status = ScenarioStatus.BUILT,
                subtitle =
                    "The community either opens normally, or shows its no-access state.",
                youWillSee =
                    "the community opening, or its no-access screen.",
            ),
            ScenarioSpec(
                id = "customEvents",
                title = "Send a custom analytics event",
                status = ScenarioStatus.BUILT,
                subtitle =
                    "The event shows up in the Events log, with the properties you attached.",
                youWillSee =
                    "the event at the top of the Events log.",
            ),
            ScenarioSpec(
                id = "locale",
                title = "Switch the community's language",
                status = ScenarioStatus.BUILT,
                subtitle =
                    "The community's own labels switch to the language you picked.",
                youWillSee =
                    "the community's buttons and labels in the language you picked.",
            ),
            ScenarioSpec(
                id = "lifecycle",
                title = "Restart or stop the SDK",
                status = ScenarioStatus.PARTIAL,
                subtitle =
                    "The session ends and the community goes back to its not-connected state.",
                youWillSee =
                    "the community back in its not-connected state.",
                note =
                    "Preset 1 (\"switch community, alternate key\") needs a second real " +
                        "community API key this sample never bakes in. Presets 2-3 (reset, stop) " +
                        "are built.",
            ),
            ScenarioSpec(
                id = "bridge",
                title = "Open the post attached to a product",
                status = ScenarioStatus.BUILT,
                subtitle =
                    "A post tied to a host object opens — created on the spot the first time " +
                        "you ask for it.",
                youWillSee =
                    "the post for that object, and your reaction on it.",
            ),
            ScenarioSpec(
                id = "createPost",
                title = "Start a post from the host app",
                status = ScenarioStatus.BUILT,
                subtitle =
                    "The composer opens pre-filled with the text, image or link the host handed " +
                        "over.",
                youWillSee =
                    "the composer, pre-filled with what the host passed.",
            ),
            ScenarioSpec(
                id = "reactions",
                title = "React to a post from the host app",
                status = ScenarioStatus.BUILT,
                subtitle =
                    "The reaction you picked shows on that post in the community.",
                youWillSee =
                    "your reaction on the post you named.",
            ),
            ScenarioSpec(
                id = "theme",
                title = "Apply the host's brand to the community",
                status = ScenarioStatus.BUILT,
                subtitle =
                    "The community adopts the host's colours, fonts and logo — or drops back to " +
                        "the SDK's own look.",
                youWillSee =
                    "the community in the host's colours, or in the SDK's default look.",
            ),
            ScenarioSpec(
                id = "communityData",
                title = "Read a member's community profile",
                status = ScenarioStatus.BUILT,
                subtitle =
                    "The member's community profile comes back, and keeps updating while you " +
                        "watch it.",
                youWillSee =
                    "the member's nickname and ids, refreshing as they change.",
            ),
            ScenarioSpec(
                id = "profileFieldsLock",
                title = "Lock the profile fields the host owns",
                status = ScenarioStatus.BUILT,
                subtitle =
                    "The locked fields turn read-only in the community's profile editor.",
                youWillSee =
                    "the locked fields greyed out in the profile editor.",
            ),
            ScenarioSpec(
                id = "contentOptions",
                title = "Change what members are allowed to post",
                status = ScenarioStatus.BUILT,
                subtitle =
                    "The composer offers only the kinds of content the community allows.",
                youWillSee =
                    "the composer offering only the allowed content types.",
            ),
            ScenarioSpec(
                id = "termsAcceptance",
                title = "Change how members accept the terms",
                status = ScenarioStatus.BUILT,
                subtitle =
                    "The community asks for consent the way you picked, or stops asking " +
                        "altogether.",
                youWillSee =
                    "the consent step in the form you chose, or no consent step at all.",
            ),
            ScenarioSpec(
                id = "consentTexts",
                title = "Show the community's consent wording",
                status = ScenarioStatus.BUILT,
                subtitle =
                    "The consent step shows the wording the community wrote, links included.",
                youWillSee =
                    "the community's own consent texts, or the SDK wording where a text is empty.",
            ),
            ScenarioSpec(
                id = "forceNicknameChoice",
                title = "Require a nickname before the first contribution",
                status = ScenarioStatus.BUILT,
                subtitle =
                    "A member without a confirmed nickname types one before their first post, " +
                        "comment or reply.",
                youWillSee =
                    "the nickname choice screen with an empty field, before the contribution is sent.",
            ),
            ScenarioSpec(
                id = "events",
                title = "Watch what the SDK reports",
                status = ScenarioStatus.BUILT,
                subtitle =
                    "Every event the SDK emits, live, as the community is used.",
                youWillSee =
                    "the event stream filling up as you use the community.",
            ),
            ScenarioSpec(
                id = "refreshEntitlements",
                title = "Re-issue the demo user's entitlements",
                status = ScenarioStatus.BUILT,
                subtitle =
                    "The community picks up the entitlements you edited, without signing out.",
                youWillSee =
                    "the abilities you granted, available in the community.",
            ),
            ScenarioSpec(
                id = "groupAccessDenied",
                title = "Handle a member reaching a closed group",
                status = ScenarioStatus.BUILT,
                subtitle =
                    "The host is told a group was refused, instead of the community handling it " +
                        "alone.",
                youWillSee =
                    "the host's own access-denied screen instead of the SDK's.",
            ),
            ScenarioSpec(
                id = "initialScreen",
                title = "Open the community on a chosen screen",
                status = ScenarioStatus.PARTIAL,
                subtitle =
                    "The community opens directly on the member, profile or feed you picked.",
                youWillSee =
                    "the community already on the screen you asked for.",
                note =
                    "Presets 8-11 (member activity by host id / profile id, member profile, own " +
                        "profile) are wired. Presets 1-7 (main feed, bridge post, group, composer, " +
                        "standalone post details and editor, home screen) have no scenario screen " +
                        "yet.",
            ),
            ScenarioSpec(
                id = "fullscreen",
                title = "Open the community full screen",
                status = ScenarioStatus.BUILT,
                subtitle =
                    "The community takes the whole screen, outside the tab bar.",
                youWillSee =
                    "the community covering the screen; the back gesture returns here.",
                note =
                    "Preset 3 puts a host bottom bar under it: the embedded content must clear " +
                        "the bar and its safe-area inset.",
            ),
            ScenarioSpec(
                id = "modal",
                title = "Open the community as a modal",
                status = ScenarioStatus.BUILT,
                subtitle =
                    "The community covers the screen in a host-owned modal that you close " +
                        "yourself.",
                youWillSee =
                    "the community in a modal, closed by the X that leads the community's header.",
            ),
            ScenarioSpec(
                id = "sheet",
                title = "Open the community as a bottom sheet",
                status = ScenarioStatus.BUILT,
                subtitle =
                    "The community slides over this screen; swipe down to come back.",
                youWillSee =
                    "the community at sheet height, its own navigation staying inside the sheet.",
            ),
            ScenarioSpec(
                id = "trackABTests",
                title = "Report which cohort the member is in",
                status = ScenarioStatus.BUILT,
                subtitle =
                    "The community opens for the cohort you granted, and refuses the other one.",
                youWillSee =
                    "the community open, or the refusal the other cohort gets.",
            ),
            ScenarioSpec(
                id = "forceOctopusABTests",
                title = "Force Octopus's own A/B decision",
                status = ScenarioStatus.BUILT,
                subtitle =
                    "The community's own access rule is overridden — granted or denied — until " +
                        "you clear it.",
                youWillSee =
                    "the community following your forced value, not its own.",
            ),
        )

    fun find(id: String): ScenarioSpec? = all.find { it.id == id }

    /** Scenarios worth listing, grouped and ordered for the list screen. */
    fun bySection(): Map<ScenarioSection, List<ScenarioSpec>> =
        all
            .filter { it.status != ScenarioStatus.PENDING }
            .groupBy { it.section }
            .toSortedMap(compareBy { it.ordinal })

    /**
     * The SDK symbol each scenario stands for — the monospace pill on its card, and the first thing
     * on its detail screen. Kept next to the catalog rather than in the screens because two screens
     * show it and a scenario named after one API and calling another is exactly the confusion the
     * pill exists to prevent.
     */
    internal val API_NAMES: Map<String, String> =
        mapOf(
            "unifiedProfile" to "onNavigateToProfile",
            "connection" to "connectUser",
            "groups" to "fetchGroups",
            "syncFollowGroups" to "syncFollowGroups",
            "notSeenNotifications" to "updateNotSeenNotificationsCount",
            "pushNotifications" to "getOctopusNotification",
            "communityAccess" to "overrideCommunityAccess",
            "customEvents" to "track",
            "locale" to "overrideDefaultLocale",
            "lifecycle" to "reset / stop",
            "bridge" to "fetchOrCreateClientObjectRelatedPost",
            "createPost" to "navigateToOctopusCreatePost",
            "reactions" to "setReaction",
            "theme" to "OctopusTheme",
            "communityData" to "fetchCommunityDataResult",
            "profileFieldsLock" to "debugOverrideProfileFieldsLock",
            "contentOptions" to "debugOverrideContentOptions",
            "termsAcceptance" to "debugOverrideTermsAcceptanceMode",
            "consentTexts" to "debugOverrideConsentTexts",
            "forceNicknameChoice" to "debugOverrideForceNicknameChoice",
            "events" to "OctopusSDK.events",
            "refreshEntitlements" to "refreshEntitlements",
            "groupAccessDenied" to "setGroupAccessDeniedCallback",
            "initialScreen" to "navigateToOctopusHome",
            // The three presentation shapes name the HOST construct, prefixed so it cannot be read
            // as an SDK symbol. Naming an SDK entry point here would contradict the shared
            // catalog, which carries `api_symbols: []` on all three on purpose: the shape is a
            // container the host builds around the one embedded surface every other scenario
            // already claims, so there is no per-shape SDK call to put in the pill. The previous
            // values (`navigateToOctopusHome`, `OctopusHomeContent`) suggested three different
            // APIs where there are none.
            "fullscreen" to "host: NavHost route",
            "modal" to "host: Dialog",
            "sheet" to "host: ModalBottomSheet",
            "trackABTests" to "trackAccessToCommunity",
            "forceOctopusABTests" to "overrideCommunityAccess",
        )

    /** Section membership, same rationale as [API_NAMES]. */
    internal val SECTIONS: Map<String, ScenarioSection> =
        mapOf(
            "unifiedProfile" to ScenarioSection.SSO,
            "connection" to ScenarioSection.SSO,
            "refreshEntitlements" to ScenarioSection.SSO,
            "communityData" to ScenarioSection.SSO,
            "profileFieldsLock" to ScenarioSection.SSO,
            "termsAcceptance" to ScenarioSection.SSO,
            "consentTexts" to ScenarioSection.SSO,
            "forceNicknameChoice" to ScenarioSection.SSO,
            "groups" to ScenarioSection.COMMUNITY,
            "syncFollowGroups" to ScenarioSection.COMMUNITY,
            "communityAccess" to ScenarioSection.COMMUNITY,
            "groupAccessDenied" to ScenarioSection.COMMUNITY,
            "contentOptions" to ScenarioSection.COMMUNITY,
            "reactions" to ScenarioSection.COMMUNITY,
            "bridge" to ScenarioSection.COMMUNITY,
            "createPost" to ScenarioSection.COMMUNITY,
            "notSeenNotifications" to ScenarioSection.NOTIFICATIONS,
            "pushNotifications" to ScenarioSection.NOTIFICATIONS,
            "initialScreen" to ScenarioSection.PRESENTATION,
            "fullscreen" to ScenarioSection.PRESENTATION,
            "modal" to ScenarioSection.PRESENTATION,
            "sheet" to ScenarioSection.PRESENTATION,
            "theme" to ScenarioSection.THEME,
            "locale" to ScenarioSection.THEME,
            "customEvents" to ScenarioSection.HOST,
            "events" to ScenarioSection.HOST,
            "trackABTests" to ScenarioSection.HOST,
            "forceOctopusABTests" to ScenarioSection.HOST,
            "lifecycle" to ScenarioSection.HOST,
        )

    /**
     * Scenarios whose effect a tester can see in the community feed — these get the "Verify in
     * Community" step. A scenario that only writes analytics or flips a local debug override does
     * not: offering to verify something invisible is worse than not offering it.
     */
    internal val VERIFIABLE_IN_COMMUNITY: Set<String> =
        setOf(
            "unifiedProfile",
            "connection",
            "groups",
            "syncFollowGroups",
            "communityAccess",
            "forceOctopusABTests",
            "reactions",
            "bridge",
            "createPost",
            "theme",
            "locale",
            "profileFieldsLock",
            "contentOptions",
            "termsAcceptance",
            "consentTexts",
            "forceNicknameChoice",
            "notSeenNotifications",
            "refreshEntitlements",
        )

    /**
     * The arguments each scenario's call takes — zone 2 of the detail skeleton.
     *
     * Only the calls whose signature this sample actually drives are listed. A scenario that is
     * absent here either takes no argument (`fetchGroups()`, `refreshEntitlements()`) or is a
     * navigation/host behaviour with no SDK argument to pre-fill, and its detail screen shows no
     * Parameters block rather than an invented one: a wrong signature in a sample is worse than a
     * missing one, because an integrator will copy it.
     */
    internal val PARAMETERS: Map<String, List<ScenarioParam>> =
        mapOf(
            "connection" to
                listOf(
                    ScenarioParam("user", "OctopusUser", "the SSO user stored on the Account screen"),
                ),
            "syncFollowGroups" to
                listOf(
                    ScenarioParam(
                        "actions",
                        "List<SyncFollowGroupAction>",
                        "one action per followable group, dated now + the offset picked below",
                    ),
                ),
            "communityAccess" to
                listOf(ScenarioParam("hasAccess", "Boolean", "true to grant, false to deny")),
            "forceOctopusABTests" to
                listOf(ScenarioParam("hasAccess", "Boolean", "true to grant, false to deny")),
            "trackABTests" to
                listOf(ScenarioParam("hasAccess", "Boolean", "true to grant, false to deny")),
            "customEvents" to
                listOf(
                    ScenarioParam("event", "TrackerEvent.Custom", "name = \"sample_test_event\""),
                    ScenarioParam(
                        "event.properties",
                        "Map<String, Property>",
                        "empty, or sample_prop = \"sample_value\"",
                    ),
                ),
            "locale" to
                listOf(
                    ScenarioParam("locale", "Locale?", "Locale(\"fr\"), Locale(\"en\"), or null to reset"),
                ),
            "communityData" to
                listOf(
                    ScenarioParam("userId", "String", "the connected profile's id, or the id typed below"),
                ),
            "lifecycle" to
                listOf(ScenarioParam("apiKey", "String", "pasted below — switchCommunity only")),
        )

    /**
     * Which method-state slots each scenario writes, newest-wins, for the detail screen's Result
     * block. Several scenarios drive more than one: the block shows the last one that ran, which is
     * the call the tester just made.
     */
    internal val METHOD_KEYS: Map<String, List<String>> =
        mapOf(
            "connection" to listOf("connectUser", "disconnectUser"),
            // `groups` and `syncFollowGroups` are two catalog entries onto ONE panel: both render
            // the whole GroupsSection, six cards plus the per-row toggles. Listing only each
            // scenario's "own" calls left the other half of the panel with nothing to show — the
            // sync triggers mute on the groups screen, fetch/follow/unfollow mute on the sync one.
            // They share the union, and the block shows whichever call actually ran last.
            "groups" to
                listOf(
                    "fetchGroups",
                    "followGroup",
                    "unfollowGroup",
                    "syncFollowGroups",
                ),
            "syncFollowGroups" to
                listOf(
                    "syncFollowGroups",
                    "fetchGroups",
                    "followGroup",
                    "unfollowGroup",
                ),
            "notSeenNotifications" to listOf("updateNotSeen", "registerToken"),
            "pushNotifications" to listOf("registerToken"),
            "communityAccess" to listOf("overrideAccess", "trackAccess"),
            "customEvents" to listOf("trackCustom"),
            "locale" to listOf("overrideLocale"),
            "lifecycle" to listOf("reset", "stop", "switchCommunity"),
            "communityData" to listOf("communityData"),
            // `trackABTests` is deliberately absent: its panel shows a *transformed* result (a
            // successful call made with the cohort switch off is relabelled as the catalog's
            // OFF-switch error), which the raw Result block cannot reproduce.
            "forceOctopusABTests" to listOf("overrideAccessABTests"),
        )

    /**
     * The QA test tag the detail screen's Result block carries, per scenario.
     *
     * The value is always `<scenario id>-result`, but it is written out verbatim on purpose: it is
     * the shared catalog's `result_test_id` (shared `scenario catalog`), and
     * the coverage guard checks the literal id is present in the sample tree. A tag built by string
     * interpolation is invisible to that check — and to anyone grepping the repo for the id a QA
     * report names.
     */
    internal val RESULT_TEST_IDS: Map<String, String?> =
        mapOf(
            "groups" to "groups-result",
            "syncFollowGroups" to "syncFollowGroups-result",
            "notSeenNotifications" to "notSeenNotifications-result",
            "communityAccess" to "communityAccess-result",
            "customEvents" to "customEvents-result",
            "locale" to "locale-result",
            "lifecycle" to "lifecycle-result",
            "connection" to "connection-result",
            "forceOctopusABTests" to "forceOctopusABTests-result",
            // Explicit opt-out, not an omission: `communityData-result` is already carried by
            // CommunityDataSection's own ResultPanel, which shows the preset scenario state. The
            // detail screen's Result block shows a different thing there (the raw
            // `fetchCommunityDataResult(userId)` call), so tagging it too would put the same resource-id
            // on two nodes showing different states — invisible to the coverage guard, which only
            // checks the literal is present somewhere, and ambiguous to uiautomator.
            "communityData" to null,
            // Same opt-out: PushNotificationsPresets' own ScenarioResult owns
            // `pushNotifications-result` (the deep-link replay). The Result block only shows the
            // `registerToken` call rehomed from the API Explorer in DS v1.
            "pushNotifications" to null,
            // Same opt-out, for the same reason, on the three presentation shapes: their panels own
            // `fullscreen-result` / `modal-result` / `sheet-result` because what those lines report
            // is which container the HOST just opened, not the result of an SDK call. Zone 4 would
            // never render for them anyway (no METHOD_KEYS entry), but the opt-out is written down
            // so adding one later does not silently duplicate the id onto a second node.
            "fullscreen" to null,
            "modal" to null,
            "sheet" to null,
        )

    /**
     * The test tag the detail screen's Result block should carry for [scenarioId], or `null` when a
     * panel on that screen already owns the catalog id.
     *
     * A plain `RESULT_TEST_IDS[id] ?: default` cannot express the opt-out — a stored `null` is
     * indistinguishable from an absent key — so the presence of the key is what decides.
     */
    internal fun resultTestId(scenarioId: String): String? =
        if (RESULT_TEST_IDS.containsKey(scenarioId)) {
            RESULT_TEST_IDS[scenarioId]
        } else {
            "$scenarioId-result"
        }
}
