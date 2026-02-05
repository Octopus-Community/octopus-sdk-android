package com.octopuscommunity.sample.utils

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.net.toUri
import androidx.navigation.NavController
import com.octopuscommunity.sdk.ui.components.UrlOpeningStrategy

object UrlHandler {
    fun navigateToUrl(context: Context, url: String): UrlOpeningStrategy {
        // URL handler that demonstrates how to intercept links clicked inside the community.
        // For example, if your app wants to intercept links that would go to your website
        // and instead open a screen directly in the app, you can do it here.
        val uri = url.toUri()
        return when {
            uri.host == "www.octopuscommunity.com" && uri.path == "/contact" -> {
                // Open mail app instead of the website
                val mailtoUrl = "mailto:contact@octopuscommunity.com?subject=Contact".toUri()
                context.startActivity(Intent(Intent.ACTION_VIEW, mailtoUrl))
                // Link has been handled by app, let the Octopus SDK know that it should do nothing more
                UrlOpeningStrategy.HandledByApp
            }

            uri.host == "www.google.com" -> {
                // Redirect Google to Qwant
                val newUrl = "https://www.qwant.com/".toUri()
                context.startActivity(Intent(Intent.ACTION_VIEW, newUrl))
                // Link has been handled by app, let the Octopus SDK know that it should do nothing more
                UrlOpeningStrategy.HandledByApp
            }

            else -> {
                // Let the SDK handle the other links by returning `HandledByOctopus`
                Log.d("CommunityScreen", "URL opened by Octopus SDK: $url")
                UrlOpeningStrategy.HandledByOctopus
            }
        }
    }
}