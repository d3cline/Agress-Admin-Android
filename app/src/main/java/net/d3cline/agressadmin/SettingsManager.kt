// SettingsManager.kt
package net.d3cline.agressadmin

import android.content.Context
import android.content.SharedPreferences

class SettingsManager(context: Context) {

    private val sharedPref: SharedPreferences =
        context.getSharedPreferences("Settings", Context.MODE_PRIVATE)

    var baseUrl: String
        get() = sharedPref.getString("baseUrl", "https://api.swabcity.shop/") ?: "https://api.swabcity.shop/"
        set(value) = sharedPref.edit().putString("baseUrl", value).apply()

    var apiKey: String
        get() = sharedPref.getString("apiKey", "") ?: ""
        set(value) = sharedPref.edit().putString("apiKey", value).apply()
}
