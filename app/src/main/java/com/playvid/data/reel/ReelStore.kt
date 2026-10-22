package com.playvid.data.reel

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class ReelHistoryItem(val thumb: String, val url: String)

/** Tiny SharedPreferences store: Reels history (max 4, like the web version) + its own light/dark choice. */
class ReelStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("reel_prefs", Context.MODE_PRIVATE)

    var dark: Boolean
        get() = prefs.getBoolean("dark", true)
        set(value) { prefs.edit().putBoolean("dark", value).apply() }

    fun history(): List<ReelHistoryItem> = try {
        val arr = JSONArray(prefs.getString("ig_history", "[]"))
        (0 until arr.length()).mapNotNull { i ->
            arr.optJSONObject(i)?.let { ReelHistoryItem(it.optString("thumb"), it.optString("url")) }
        }.filter { it.url.isNotBlank() }
    } catch (e: Exception) {
        emptyList()
    }

    fun saveHistory(list: List<ReelHistoryItem>) {
        val arr = JSONArray()
        list.forEach { arr.put(JSONObject().put("thumb", it.thumb).put("url", it.url)) }
        prefs.edit().putString("ig_history", arr.toString()).apply()
    }
}
