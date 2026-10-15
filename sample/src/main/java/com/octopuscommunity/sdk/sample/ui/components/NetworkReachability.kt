package com.octopuscommunity.sdk.sample.ui.components

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.getSystemService

/**
 * Whether the device currently has a network the SDK can use, as a Compose state.
 *
 * Offline means no network at all, or only an explicit captive portal
 * (`NET_CAPABILITY_CAPTIVE_PORTAL` without `NET_CAPABILITY_VALIDATED`, internal change): a sign-in page that
 * intercepts every request. A network the system has not validated is *not* offline by itself —
 * validation is a probe to a Google endpoint, which a corporate proxy, some regions or a LAN-only
 * backend block for good, and which has not finished in the first seconds after a network
 * connects; the SDK works on such networks, so the status must not call them unreachable.
 *
 * Starts from the active network's capabilities and follows a `NetworkCallback` while the caller is
 * composed. Counting usable networks rather than reading the last callback keeps a Wi-Fi→cellular
 * hand-over from flashing "offline". A device without a [ConnectivityManager] reads as online: the
 * status must never claim an outage the sample cannot see.
 */
@Composable
fun rememberIsOnline(): State<Boolean> {
    val context = LocalContext.current.applicationContext
    return produceState(initialValue = context.isOnlineNow(), context) {
        val manager = context.getSystemService<ConnectivityManager>() ?: return@produceState
        val usable = mutableSetOf<Network>()

        fun update(
            network: Network,
            capabilities: NetworkCapabilities?,
        ) {
            value =
                synchronized(usable) {
                    if (capabilities?.isUsable() == true) usable.add(network) else usable.remove(network)
                    usable.isNotEmpty()
                }
        }
        val callback =
            object : ConnectivityManager.NetworkCallback() {
                // Seeded here as well: before API 26, onAvailable is not always followed by
                // onCapabilitiesChanged, so a network already up at registration would never count.
                override fun onAvailable(network: Network) {
                    update(network, manager.getNetworkCapabilities(network))
                }

                override fun onCapabilitiesChanged(
                    network: Network,
                    capabilities: NetworkCapabilities,
                ) {
                    update(network, capabilities)
                }

                override fun onLost(network: Network) {
                    update(network, null)
                }
            }
        val request =
            NetworkRequest
                .Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
        manager.registerNetworkCallback(request, callback)
        awaitDispose { manager.unregisterNetworkCallback(callback) }
    }
}

/** Offers internet and is not an unvalidated captive portal (see [rememberIsOnline]). */
private fun NetworkCapabilities.isUsable(): Boolean =
    hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
        (
            hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) ||
                !hasCapability(NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL)
        )

private fun Context.isOnlineNow(): Boolean {
    val manager = getSystemService<ConnectivityManager>() ?: return true
    return manager.getNetworkCapabilities(manager.activeNetwork)?.isUsable() == true
}
