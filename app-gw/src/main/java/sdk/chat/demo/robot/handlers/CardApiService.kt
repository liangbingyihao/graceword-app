package sdk.chat.demo.robot.handlers

import com.google.gson.Gson
import com.google.gson.JsonObject
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import sdk.chat.demo.MainApp
import sdk.chat.demo.robot.api.GWApiManager
import sdk.chat.demo.robot.api.ImageApi
import sdk.chat.demo.robot.api.JsonCacheManager.get
import sdk.chat.demo.robot.api.JsonCacheManager.save
import sdk.chat.demo.robot.api.model.BlessData
import java.io.IOException


data class WallpaperConfig(
    var isDynamic: Boolean = true,
    var isReadScriptureEnabled: Boolean = true,
    var isLock: Boolean = true,
    var isHome: Boolean = true,
    var from: String = "",
    var date: String = ""
)

object CardApiService {
    val URL_BIBLE_DATA: String = ImageApi.URL2_MAIN + "campaign/card"
    var blessData: BlessData? = null
    val KEY_CACHE_CONFIG: String = "cache_wallpaper_config"


    fun clearCache() {
        blessData = null
    }

    fun saveWallPaperConfig(config: WallpaperConfig) {
        save(MainApp.getContext(), KEY_CACHE_CONFIG, Gson().toJson(config))
    }


    fun getWallPaperConfig(): WallpaperConfig? {
        return get(MainApp.getContext(), KEY_CACHE_CONFIG)
            ?.let { Gson().fromJson(it, WallpaperConfig::class.java) }
    }

    fun getBlessData(): Single<BlessData?> {
        return Single.create<BlessData?> { emitter ->
            try {
                blessData?.let { cached ->
                    emitter.onSuccess(cached)
                    return@create
                }

                val request = Request.Builder()
                    .url(requireNotNull(URL_BIBLE_DATA))
                    .build()

                GWApiManager.shared().client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        if (!emitter.isDisposed) {
                            emitter.onError(e)
                        }
                    }

                    override fun onResponse(call: Call, response: Response) {
                        try {
                            response.use { // 确保 response 被正确关闭
                                var ret = GWApiManager.shared()
                                    .handleResponse(response, BlessData::class.java)
                                if (ret != null) {
                                    blessData = ret
                                }
                                if (blessData != null) {
                                    emitter.onSuccess(blessData!!)
                                } else {
                                    emitter.onError(Exception("no data"))
                                }
                            }
                        } catch (e: Exception) {
                            if (!emitter.isDisposed) {
                                emitter.onError(e)
                            }
                        }
                    }
                })
            } catch (e: Exception) {
                if (!emitter.isDisposed) {
                    emitter.onError(e)
                }
            }
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    }


}