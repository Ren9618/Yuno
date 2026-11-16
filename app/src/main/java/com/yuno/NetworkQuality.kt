package com.yuno

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * The fork's per-network quality (Settings › Playback): a video-height ceiling for Wi-Fi and
 * one for mobile data, each Auto until set. A set ceiling overrides the player's own quality
 * pick ([QualityPreference]) while the app rides that kind of connection — Wi-Fi free to run
 * 1080p while mobile data stays at 480p, without touching either setting again. Auto on both
 * leaves everything to the player's pick and YouTube's adaptive ladder, as before.
 *
 * The kind is read when a stream set is chosen (and when the player relabels its quality), so
 * a switch between Wi-Fi and mobile data takes effect on the next video, not mid-play.
 */
object NetworkQuality {
    enum class Kind { Wifi, Mobile }

    private const val KEY_WIFI = "quality_wifi"
    private const val KEY_MOBILE = "quality_mobile"
    private var prefs: android.content.SharedPreferences? = null
    private var app: Context? = null

    @Volatile
    var wifiHeight: Int? = null
        private set

    @Volatile
    var mobileHeight: Int? = null
        private set

    fun init(context: Context) {
        prefs = context.getSharedPreferences("playback", Context.MODE_PRIVATE)
        app = context.applicationContext
        wifiHeight = stored(KEY_WIFI)
        mobileHeight = stored(KEY_MOBILE)
    }

    private fun stored(key: String): Int? =
        prefs?.getInt(key, 0)?.takeIf { it > 0 }?.takeIf { it in QUALITY_LADDER }

    fun setWifi(context: Context, height: Int?) {
        if (height == wifiHeight) return
        wifiHeight = height
        prefs?.edit()?.putInt(KEY_WIFI, height ?: 0)?.apply()
    }

    fun setMobile(context: Context, height: Int?) {
        if (height == mobileHeight) return
        mobileHeight = height
        prefs?.edit()?.putInt(KEY_MOBILE, height ?: 0)?.apply()
    }

    /** The connection the app's traffic rides now; null when Android won't say. */
    fun currentKind(): Kind? {
        val context = app ?: return null
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return null
        val caps = cm.activeNetwork?.let { cm.getNetworkCapabilities(it) } ?: return null
        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> Kind.Wifi
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> Kind.Mobile
            // A VPN hides the real one: assume the careful, metered side.
            else -> Kind.Mobile
        }
    }

    /** The ceiling the user set for [kind], null for Auto. */
    fun cap(kind: Kind): Int? = when (kind) {
        Kind.Wifi -> wifiHeight
        Kind.Mobile -> mobileHeight
    }

    /** The ceiling for the connection in use, or [global] when that network is on Auto (or unknown). */
    fun effective(global: Int?): Int? = effective(global, wifiHeight, mobileHeight, currentKind())

    /** [effective] with the pieces passed in, so tests stay free of any device. */
    internal fun effective(global: Int?, wifi: Int?, mobile: Int?, kind: Kind?): Int? =
        when (kind) {
            Kind.Wifi -> wifi
            Kind.Mobile -> mobile
            null -> null
        } ?: global

    /** The per-network ceiling alone (the service asks this to decide Auto vs a pinned height). */
    fun ceiling(): Int? = when (val kind = currentKind()) {
        null -> null
        else -> cap(kind)
    }
}
