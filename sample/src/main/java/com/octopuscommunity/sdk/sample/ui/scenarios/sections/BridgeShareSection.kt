package com.octopuscommunity.sdk.sample.ui.scenarios.sections

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.octopuscommunity.sdk.domain.model.CreatePostScreenInfo
import com.octopuscommunity.sdk.domain.model.OctopusGroup
import com.octopuscommunity.sdk.domain.model.OctopusLocalizedText
import com.octopuscommunity.sdk.domain.model.OctopusPostCTA
import com.octopuscommunity.sdk.domain.model.OctopusPrefilledPost
import com.octopuscommunity.sdk.sample.R
import com.octopuscommunity.sdk.sample.data.SampleFixtures
import com.octopuscommunity.sdk.sample.data.utils.TokenProvider
import com.octopuscommunity.sdk.sample.ui.components.SecondaryPillButton
import com.octopuscommunity.sdk.sample.ui.components.sampleDefaultSwitchColors
import java.util.Locale

/** The default language a per-language CTA label starts from in this form. */
private const val DEFAULT_CTA_LANGUAGE = "en"

/** Two rows of this form name the same language, which a `Map<Locale, String>` cannot hold twice. */
private class DuplicateCtaLanguage(
    language: Locale,
) : Exception("Two labels are given for the language \"${language.toLanguageTag()}\".")

/**
 * Bridge Share section: build an [OctopusPrefilledPost] payload and open
 * the post editor prefilled with it. Mirrors the iOS sample's
 * `PrefilledPostForm` so the two host-side integrations stay testable
 * in parity.
 *
 * Also the shared `createPost` QA scenario (native 1.12.0): the five
 * `qa-preset-createPost-N` buttons below only FILL the form fields — they never open the editor or
 * post anything by themselves, because the demo community is shared and no scenario may create
 * content without an explicit tester tap. Opening the editor (and any further submit inside it)
 * stays a separate, manual gesture on the buttons at the bottom of this section. Test ids
 * (`qa-preset-createPost-N` / `createPost-result`) are applied verbatim from the shared
 * scenario catalog. `testTagsAsResourceId` surfaces them as resource-ids in the uiautomator /
 * `android layout` tree.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun BridgeShareSection(
    groups: List<OctopusGroup>,
    onOpenCreatePost: (CreatePostScreenInfo) -> Unit,
) {
    var text by rememberSaveable { mutableStateOf("") }
    var imageUriStrings by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var groupId by rememberSaveable { mutableStateOf(SampleFixtures.topicDefault) }
    var ctaLabel by rememberSaveable { mutableStateOf("") }
    var ctaUrl by rememberSaveable { mutableStateOf("") }
    // Off by default, so the presets keep sending the auto-translated label QA has always exercised.
    var ctaPerLanguage by rememberSaveable { mutableStateOf(false) }
    var ctaDefaultLanguage by rememberSaveable { mutableStateOf(DEFAULT_CTA_LANGUAGE) }
    // One translation row per index: its language tag and its label.
    var ctaTranslationLanguages by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var ctaTranslationLabels by rememberSaveable { mutableStateOf(emptyList<String>()) }
    var validationError by rememberSaveable { mutableStateOf<String?>(null) }
    var appliedPresetLabel by rememberSaveable { mutableStateOf<String?>(null) }

    val context = LocalContext.current

    // Bundled image URI used by the "image (bundled)" presets — exercises
    // [Media.Image]'s ContentResolver.openInputStream path on an
    // android.resource:// URI without needing the system media picker
    // (which the automated QA Tester cannot drive reliably). ic_avatar is
    // 330×330 → passes the SDK's image-dimension validator (minSize=50,
    // maxRatio=32/9).
    val bundledImageUri = "android.resource://${context.packageName}/${R.drawable.ic_avatar}"

    val imagePicker =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = MAX_PREFILLED_IMAGES),
        ) { uris: List<Uri> ->
            if (uris.isNotEmpty()) imageUriStrings = uris.map { it.toString() }
        }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text =
                "Open the Octopus post editor prefilled with a host-supplied payload. " +
                    "Validation runs at the OctopusPrefilledPost constructor — errors are " +
                    "surfaced below before the editor opens.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // --- Presets ---
        //
        // Single-tap presets that fill the form with valid payloads. Lets the
        // QA automation (and a curious dev) exercise the prefill flow without
        // typing into five stacked Compose TextFields, which the automated
        // Tester reliably gets stuck on (focus drift between fields, repeated
        // typing accumulating garbage). Image-bearing scenarios still require
        // the system picker — by design, those exercise the real content-URI
        // permission grant path.
        //
        // The five ids and labels below are the shared `createPost` scenario catalog,
        // applied verbatim — a preset only fills the fields, it never opens the editor or posts
        // anything on its own.
        Text(
            text = "Presets",
            style = MaterialTheme.typography.titleSmall,
        )
        Preset(
            label = "Preset 1 · Text only",
            testId = "qa-preset-createPost-1",
            onApply = { appliedPresetLabel = it },
        ) {
            text = "Bridge Share preset — text only."
            imageUriStrings = emptyList()
            groupId = SampleFixtures.topicDefault
            ctaLabel = ""
            ctaUrl = ""
            validationError = null
        }
        Preset(
            label = "Preset 2 · Text + CTA",
            testId = "qa-preset-createPost-2",
            onApply = { appliedPresetLabel = it },
        ) {
            text = "Bridge Share preset — text plus a CTA."
            imageUriStrings = emptyList()
            groupId = SampleFixtures.topicDefault
            ctaLabel = "Open"
            ctaUrl = "https://octopuscommunity.com/preset"
            validationError = null
        }
        Preset(
            label = "Preset 3 · Text + bundled image",
            testId = "qa-preset-createPost-3",
            onApply = { appliedPresetLabel = it },
        ) {
            text = "Bridge Share preset — text plus a bundled image."
            imageUriStrings = listOf(bundledImageUri)
            groupId = SampleFixtures.topicDefault
            ctaLabel = ""
            ctaUrl = ""
            validationError = null
        }
        Preset(
            label = "Preset 4 · Full (text + CTA + bundled image)",
            testId = "qa-preset-createPost-4",
            onApply = { appliedPresetLabel = it },
        ) {
            text = "Bridge Share preset — full payload with a bundled image."
            imageUriStrings = listOf(bundledImageUri)
            groupId = SampleFixtures.topicDefault
            ctaLabel = "Open"
            ctaUrl = "https://octopuscommunity.com/preset"
            validationError = null
        }
        Preset(
            label = "Preset 5 · Image only (bundled)",
            testId = "qa-preset-createPost-5",
            onApply = { appliedPresetLabel = it },
        ) {
            text = ""
            imageUriStrings = listOf(bundledImageUri)
            groupId = SampleFixtures.topicDefault
            ctaLabel = ""
            ctaUrl = ""
            validationError = null
        }
        // Multi-image presets, outside the shared five-preset catalog above.
        Preset(
            label = "Text + 3 bundled images",
            testId = "sample-preset-createPost-3-images",
            onApply = { appliedPresetLabel = it },
        ) {
            text = "Bridge Share preset — text plus three bundled images."
            imageUriStrings = List(3) { bundledImageUri }
            groupId = SampleFixtures.topicDefault
            ctaLabel = ""
            ctaUrl = ""
            validationError = null
        }
        Preset(
            label = "11 bundled images (too many)",
            testId = "sample-preset-createPost-11-images",
            onApply = { appliedPresetLabel = it },
        ) {
            text = "Bridge Share preset — eleven images, one over the limit."
            imageUriStrings = List(MAX_PREFILLED_IMAGES + 1) { bundledImageUri }
            groupId = SampleFixtures.topicDefault
            ctaLabel = ""
            ctaUrl = ""
            validationError = null
        }
        SecondaryPillButton(
            onClick = {
                text = ""
                imageUriStrings = emptyList()
                groupId = SampleFixtures.topicDefault
                ctaLabel = ""
                ctaDefaultLanguage = DEFAULT_CTA_LANGUAGE
                ctaTranslationLanguages = emptyList()
                ctaTranslationLabels = emptyList()
                ctaUrl = ""
                validationError = null
                appliedPresetLabel = null
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Reset form")
        }
        Text(
            modifier = Modifier.testTag("createPost-result"),
            text = "Filled: ${appliedPresetLabel ?: "none yet — tap a preset"}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        HorizontalDivider()

        // --- Text ---
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("Prefilled text") },
            minLines = 2,
            maxLines = 6,
            modifier = Modifier.fillMaxWidth(),
        )

        // --- Image ---
        SecondaryPillButton(
            onClick = {
                imagePicker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (imageUriStrings.isEmpty()) "Pick up to $MAX_PREFILLED_IMAGES images" else "Replace images")
        }
        if (imageUriStrings.isNotEmpty()) {
            imageUriStrings.forEach { uriString ->
                Text(
                    text = uriString,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            SecondaryPillButton(
                onClick = { imageUriStrings = emptyList() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Clear images (${imageUriStrings.size})")
            }
        }

        // --- Group ---
        OutlinedTextField(
            value = groupId,
            onValueChange = { groupId = it },
            label = { Text("Group ID (optional)") },
            supportingText = { Text("Leave empty to force the user to pick a group in the editor") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        if (groups.isNotEmpty()) {
            Text(
                text = "Available groups:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            groups.take(5).forEach { group ->
                SecondaryPillButton(
                    onClick = { groupId = group.id },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("→ ${group.name}")
                }
            }
        }

        HorizontalDivider()

        // --- CTA ---
        Text(
            text = "CTA (optional — invisible in the editor, attached to the published post)",
            style = MaterialTheme.typography.titleSmall,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (ctaPerLanguage) {
                OutlinedTextField(
                    value = ctaDefaultLanguage,
                    onValueChange = { ctaDefaultLanguage = it },
                    label = { Text("Default language") },
                    modifier = Modifier.weight(1f).testTag("bridge-share-cta-default-language"),
                    singleLine = true,
                )
            }
            OutlinedTextField(
                value = ctaLabel,
                onValueChange = { ctaLabel = it },
                label = { Text(if (ctaPerLanguage) "Default label" else "CTA label") },
                modifier = Modifier.weight(2f).testTag("bridge-share-cta-label"),
                singleLine = true,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Give the label per language",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            Switch(
                colors = sampleDefaultSwitchColors(),
                modifier = Modifier.testTag("bridge-share-cta-per-language-toggle"),
                checked = ctaPerLanguage,
                onCheckedChange = { ctaPerLanguage = it },
            )
        }
        if (ctaPerLanguage) {
            ctaTranslationLanguages.indices.forEach { index ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = ctaTranslationLanguages[index],
                        onValueChange = { value ->
                            ctaTranslationLanguages = ctaTranslationLanguages.toMutableList().also { it[index] = value }
                        },
                        label = { Text("Language") },
                        modifier = Modifier.weight(1f).testTag("bridge-share-cta-translation-language-$index"),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = ctaTranslationLabels[index],
                        onValueChange = { value ->
                            ctaTranslationLabels = ctaTranslationLabels.toMutableList().also { it[index] = value }
                        },
                        label = { Text("Label") },
                        modifier = Modifier.weight(2f).testTag("bridge-share-cta-translation-label-$index"),
                        singleLine = true,
                    )
                    SecondaryPillButton(
                        onClick = {
                            ctaTranslationLanguages = ctaTranslationLanguages.filterIndexed { i, _ -> i != index }
                            ctaTranslationLabels = ctaTranslationLabels.filterIndexed { i, _ -> i != index }
                        },
                        modifier = Modifier.testTag("bridge-share-cta-translation-remove-$index"),
                    ) {
                        Text("Remove")
                    }
                }
            }
            SecondaryPillButton(
                onClick = {
                    ctaTranslationLanguages = ctaTranslationLanguages + ""
                    ctaTranslationLabels = ctaTranslationLabels + ""
                },
                modifier = Modifier.fillMaxWidth().testTag("bridge-share-cta-add-translation"),
            ) {
                Text("Add translation")
            }
            Text(
                text =
                    "Languages are tags such as fr, pt-BR or zh-Hant-TW. A row left empty is ignored; " +
                        "a half-filled one is sent, so the SDK reports what is wrong with it.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        OutlinedTextField(
            value = ctaUrl,
            onValueChange = { ctaUrl = it },
            label = { Text("CTA URL (https:// or custom scheme)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        validationError?.let { errorMessage ->
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        HorizontalDivider()

        // --- Open ---
        SecondaryPillButton(
            onClick = {
                try {
                    // Rows whose language and label are both empty are ignored.
                    val translations =
                        ctaTranslationLanguages
                            .zip(ctaTranslationLabels)
                            .filterNot { (language, label) -> language.isBlank() && label.isEmpty() }
                    val cta =
                        if (ctaLabel.isNotEmpty() || ctaUrl.isNotEmpty() || (ctaPerLanguage && translations.isNotEmpty())) {
                            OctopusPostCTA(
                                url = ctaUrl.toUri(),
                                localizedLabel =
                                    if (ctaPerLanguage) {
                                        // Every label goes as typed, so a blank one or a malformed
                                        // language shows the SDK's validation error. Readers whose
                                        // language is not listed get the default label, translated
                                        // automatically.
                                        val defaultLanguage = Locale.forLanguageTag(ctaDefaultLanguage.trim())
                                        val entries =
                                            listOf(defaultLanguage to ctaLabel) +
                                                translations.map { (language, label) ->
                                                    Locale.forLanguageTag(language.trim()) to label
                                                }
                                        // A map keeps one entry per Locale, so two rows with the same
                                        // tag would silently collapse into one: report it instead.
                                        entries
                                            .groupBy { it.first }
                                            .filterValues { it.size > 1 }
                                            .keys
                                            .firstOrNull()
                                            ?.let { duplicate ->
                                                throw DuplicateCtaLanguage(duplicate)
                                            }
                                        OctopusLocalizedText.PerLanguage(
                                            texts = entries.toMap(),
                                            defaultLanguage = defaultLanguage,
                                        )
                                    } else {
                                        OctopusLocalizedText.AutoTranslated(ctaLabel)
                                    },
                            )
                        } else {
                            null
                        }
                    val prefilled =
                        OctopusPrefilledPost(
                            text = text.ifEmpty { null },
                            images = imageUriStrings.map { it.toUri() },
                            topicId = groupId.ifEmpty { null },
                            cta = cta,
                        )
                    validationError = null
                    // Pass the bridge-share signer on the editor payload itself (not a global
                    // setter): in a pictures-off community the BE PrefilledShareVerifier only
                    // accepts an image-carrying prefilled share whose clientToken's
                    // bridge_fingerprint matches the final content (internal ticket Q5). A real host's
                    // backend signs; the sample signs locally.
                    onOpenCreatePost(
                        CreatePostScreenInfo(
                            prefilledPost = prefilled,
                            bridgeShareTokenProvider = { bridgeFingerprint ->
                                TokenProvider.getBridgeSignature(bridgeFingerprint)
                            },
                        ),
                    )
                } catch (error: OctopusPrefilledPost.ValidationError) {
                    validationError = error.message
                        ?: "OctopusPrefilledPost.ValidationError: $error"
                } catch (error: OctopusPrefilledPost.TooManyImagesError) {
                    validationError = error.message
                } catch (error: OctopusLocalizedText.ValidationError) {
                    validationError = error.message
                } catch (error: DuplicateCtaLanguage) {
                    validationError = error.message
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Open Octopus (prefilled)")
        }

        SecondaryPillButton(
            onClick = {
                validationError = null
                onOpenCreatePost(CreatePostScreenInfo())
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Open Octopus (empty editor)")
        }
    }
}

@Composable
private fun Preset(
    label: String,
    testId: String,
    onApply: (String) -> Unit,
    fill: () -> Unit,
) {
    SecondaryPillButton(
        onClick = {
            fill()
            onApply(label)
        },
        modifier = Modifier.fillMaxWidth().testTag(testId),
    ) {
        Text(label)
    }
}

/** The most images a post carries, which the SDK enforces on the prefill. */
private const val MAX_PREFILLED_IMAGES = 10
