package com.octopuscommunity.sdk.data.utils

import android.content.Context
import android.os.Build
import java.util.Locale

@Suppress("DEPRECATION")
val Context.locale
    get() = with(resources.configuration) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) locale else locales[0]
    }

internal val Context.localeIdentifier
    get() = locale.identifier

internal val Locale.identifier
    get() = listOfNotNull(language.ifEmpty { null }, country.ifEmpty { null })
        .joinToString(separator = "-")