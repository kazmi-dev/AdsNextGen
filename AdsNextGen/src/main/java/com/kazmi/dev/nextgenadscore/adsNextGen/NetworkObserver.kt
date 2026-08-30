package com.kazmi.dev.nextgenadscore.adsNextGen

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

object NetworkObserver {

    /**
     * Checks if the device is currently connected to the internet.
     */
    fun isConnected(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
