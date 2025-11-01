package com.nihongo.conversation.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Network connectivity monitor for detecting online/offline state
 *
 * Phase 6B-1 improvements:
 * - Hot StateFlow to share single callback across all collectors
 * - onCapabilitiesChanged to detect VALIDATED state changes
 * - Debounce to prevent flapping on unstable networks
 */
@Singleton
@OptIn(FlowPreview::class)  // Phase 6B-1: debounce is preview API
class NetworkMonitor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    // Phase 6B-1: Application scope for hot flow
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * Phase 6B-1: Hot StateFlow that shares a single callback
     * Debounced by 300ms to prevent flapping
     */
    val isOnline: StateFlow<Boolean> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            private val networks = mutableSetOf<Network>()

            override fun onAvailable(network: Network) {
                networks.add(network)
                // Don't immediately send true - wait for capabilities to be validated
                Log.d(TAG, "Network available: $network")
            }

            // Phase 6B-1: Handle capability changes for VALIDATED state
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                val isValidated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) &&
                        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)

                if (isValidated && network !in networks) {
                    networks.add(network)
                }

                val isOnline = networks.isNotEmpty() && isValidated
                Log.d(TAG, "Capabilities changed: network=$network, validated=$isValidated, online=$isOnline")
                trySend(isOnline)
            }

            override fun onLost(network: Network) {
                networks.remove(network)
                val isOnline = networks.isNotEmpty()
                Log.d(TAG, "Network lost: $network, online=$isOnline")
                trySend(isOnline)
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            .build()

        connectivityManager.registerNetworkCallback(request, callback)

        // Send initial state
        val initialState = isCurrentlyOnline()
        Log.i(TAG, "NetworkMonitor initialized - online: $initialState")
        trySend(initialState)

        awaitClose {
            Log.d(TAG, "Unregistering network callback")
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }
        .debounce(300)  // Phase 6B-1: Debounce flapping networks
        .distinctUntilChanged()
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,  // Keep active to maintain single callback
            initialValue = isCurrentlyOnline()
        )

    /**
     * Check if device is currently online
     */
    fun isCurrentlyOnline(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    /**
     * Get current network connection type
     */
    fun getConnectionType(): ConnectionType {
        val network = connectivityManager.activeNetwork ?: return ConnectionType.NONE
        val capabilities = connectivityManager.getNetworkCapabilities(network)
            ?: return ConnectionType.NONE

        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> ConnectionType.WIFI
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> ConnectionType.CELLULAR
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> ConnectionType.ETHERNET
            else -> ConnectionType.OTHER
        }
    }

    /**
     * Check if on metered connection (cellular data)
     */
    fun isMeteredConnection(): Boolean {
        return connectivityManager.isActiveNetworkMetered
    }

    enum class ConnectionType {
        NONE,
        WIFI,
        CELLULAR,
        ETHERNET,
        OTHER
    }

    companion object {
        private const val TAG = "NetworkMonitor"
    }
}
