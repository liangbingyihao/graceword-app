package sdk.chat.demo.robot.ops

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import org.json.JSONObject
import sdk.chat.demo.MainApp
import sdk.chat.demo.pre.BuildConfig
import java.util.concurrent.TimeUnit

data class ActivityConfig(
    val id: String,
    val name: String,
    val url: String,
    val version: Int,
    val startTime: Long,
    val endTime: Long,
    val enableCache: Boolean = true,
    val preloadImages: List<String> = emptyList(),
    val jsBridgeEnabled: Boolean = true,
    val enableOffline: Boolean = false
) {
    fun isActive(): Boolean {
        val currentTime = System.currentTimeMillis()
        return currentTime in startTime..endTime
    }
}

class ConfigManager {

    private val gson = Gson()
    private val prefs: SharedPreferences by lazy {
        MainApp.getContext().getSharedPreferences("activity_configs", Context.MODE_PRIVATE)
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun getActivityConfig(activityId: String): ActivityConfig? {
        return withContext(Dispatchers.IO) {
            // 1. 从内存缓存获取
            // 2. 从本地存储获取
            val json = prefs.getString(activityId, null)
            json?.let { gson.fromJson(it, ActivityConfig::class.java) }
        }
    }

    suspend fun fetchActivityConfig(activityId: String): ActivityConfig? {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("${getConfigBaseUrl()}/activity/$activityId")
                    .header("Cache-Control", "no-cache")
                    .build()

                val response = okHttpClient.newCall(request).execute()

                if (response.isSuccessful) {
                    val json = response.body?.string()
                    val config = gson.fromJson(json, ActivityConfig::class.java)

                    // 保存到本地
                    prefs.edit()
                        .putString(activityId, json)
                        .putLong("${activityId}_timestamp", System.currentTimeMillis())
                        .apply()

                    config
                } else {
                    null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    fun shouldUpdateConfig(activityId: String): Boolean {
        val lastUpdate = prefs.getLong("${activityId}_timestamp", 0)
        val currentTime = System.currentTimeMillis()
        val updateInterval = 5 * 60 * 1000L // 5分钟更新一次

        return currentTime - lastUpdate > updateInterval
    }

    private fun getConfigBaseUrl(): String {
        return if (BuildConfig.DEBUG) {
            "https://dev-api.yourdomain.com"
        } else {
            "https://api.yourdomain.com"
        }
    }
}