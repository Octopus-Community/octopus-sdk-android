package com.octopuscommunity.sdk.sample.ui.settings

import android.content.SharedPreferences
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.octopuscommunity.sdk.OctopusSDK
import com.octopuscommunity.sdk.sample.data.SampleConfigSummary
import com.octopuscommunity.sdk.sample.ui.components.SampleCard
import com.octopuscommunity.sdk.sample.ui.components.sampleRadioColors
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import java.util.Locale

/**
 * The locales the SDK ships translations for. Kept in this order — system default first, then
 * alphabetical by English name — so the list reads as "the SDK's own languages", not as a ranking.
 */
private val LANGUAGES =
    listOf(
        null to "Follow system",
        Locale.ENGLISH to "English",
        Locale.FRENCH to "French",
        Locale.GERMAN to "German",
        Locale.ITALIAN to "Italian",
        Locale("pl") to "Polish",
        Locale("pt") to "Portuguese",
        Locale("es") to "Spanish",
        Locale("sv") to "Swedish",
        Locale("tr") to "Turkish",
    )

/**
 * Settings → Language: overrides the locale the **SDK surface** renders in.
 *
 * A destination of the Settings tab graph, so no `Scaffold` and no back arrow of its own — the app
 * bar is `MainScaffold`'s. The choice is written to the same preference the rest of the sample reads
 * ([SampleConfigSummary.LOCALE_OVERRIDE_KEY]) and applied to the running SDK immediately, so the
 * Community tab is already re-rendered by the time the tester swipes back to it.
 *
 * It does not translate the sample itself: the sample is deliberately English-only, and pretending
 * otherwise would make a failed SDK translation look like a working one.
 */
@Composable
fun LanguageScreen() {
    val prefs: SharedPreferences = koinInject()
    val scope = rememberCoroutineScope()
    var selectedLocale by remember {
        mutableStateOf(
            prefs.getString(SampleConfigSummary.LOCALE_OVERRIDE_KEY, null)?.let { Locale(it) },
        )
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .testTag("language-screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SampleCard(
            title = "SDK language",
            subtitle = "Applies to the Octopus screens only — the sample itself stays in English.",
        ) {
            Column {
                LANGUAGES.forEach { (locale, name) ->
                    val selected = selectedLocale?.language == locale?.language
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedLocale = locale
                                    prefs
                                        .edit()
                                        .putString(
                                            SampleConfigSummary.LOCALE_OVERRIDE_KEY,
                                            locale?.language,
                                        ).apply()
                                    scope.launch { OctopusSDK.overrideDefaultLocale(locale) }
                                }.padding(vertical = 6.dp)
                                .testTag("language-option-${locale?.language ?: "system"}"),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = selected,
                            onClick = null,
                            colors = sampleRadioColors(),
                        )
                        Text(
                            text = name,
                            modifier = Modifier.padding(start = 12.dp),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
        }

        Text(
            text =
                "The override survives a restart. \"Follow system\" hands the choice back to the " +
                    "device locale.",
            style = MaterialTheme.typography.bodySmall,
            color =
                MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
