package sdk.chat.demo.robot.handlers

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.graphics.Bitmap
import android.util.Log
import androidx.core.content.edit
import androidx.core.net.toUri
import com.google.gson.Gson
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import sdk.chat.core.events.EventType
import sdk.chat.core.events.NetworkEvent
import sdk.chat.core.session.ChatSDK
import sdk.chat.demo.MainApp
import sdk.chat.demo.robot.api.GWApiManager
import sdk.chat.demo.robot.api.ImageApi
import sdk.chat.demo.robot.api.JsonCacheManager.get
import sdk.chat.demo.robot.api.JsonCacheManager.save
import sdk.chat.demo.robot.api.model.BlessData
import sdk.chat.demo.robot.api.model.Campaign
import sdk.chat.demo.robot.api.model.ShareResult
import sdk.chat.demo.robot.extensions.DateLocalizationUtil
import sdk.chat.demo.robot.extensions.DateLocalizationUtil.formatDayAgo
import sdk.chat.demo.robot.extensions.LanguageUtils
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.Calendar


data class WallpaperConfig(
    var isDynamic: Boolean = true,
    var isReadScriptureEnabled: Boolean = true,
    var isLock: Boolean = true,
    var isHome: Boolean = true,
    var from: String = "",
    var date: String = "",
    var greeting: String = "",
    var font: String = "",
    var cntSkip: Int = 0
)

object CardApiService {
    val URL_CARD_DATA: String = GWApiManager.URL_V1 + "campaign/card"
    val URL_CAMPAIGN_DATA: String = GWApiManager.URL_V1 + "campaign/current"
    val URL_CARD_SHARE: String = GWApiManager.URL_V1 + "campaign/card/share"
    var blessData: BlessData? = null
    var campaign: Campaign? = null
    val KEY_CACHE_CONFIG: String = "cache_wallpaper_config"
    val FROM_CARD = "CARD"
    val FROM_DAILY = "DAILY"


    fun clearCache() {
        blessData = null
        campaign = null
    }

    fun saveWallPaperConfig(config: WallpaperConfig) {
        save(MainApp.getContext(), KEY_CACHE_CONFIG, Gson().toJson(config))
    }

    private fun getDefaultWallpaperConfig(): WallpaperConfig {
        return WallpaperConfig(
            isDynamic = true,
            isReadScriptureEnabled = true,
            isLock = false,
            isHome = false
        )
    }

    fun getWallPaperConfig(): WallpaperConfig {
        return try {
            get(MainApp.getContext(), KEY_CACHE_CONFIG)
                ?.takeIf { it.isNotBlank() } // 检查非空字符串
                ?.let { jsonString ->
                    Gson().fromJson(jsonString, WallpaperConfig::class.java)
                } ?: getDefaultWallpaperConfig()
        } catch (e: Exception) {
            Log.e("WallpaperConfig", "获取壁纸配置失败，使用默认配置", e)
            getDefaultWallpaperConfig()
        }
    }

    fun getRawWallPaperConfig(): WallpaperConfig? {
        return try {
            get(MainApp.getContext(), KEY_CACHE_CONFIG)
                ?.takeIf { it.isNotBlank() } // 检查非空字符串
                ?.let { jsonString ->
                    Gson().fromJson(jsonString, WallpaperConfig::class.java)
                }
        } catch (e: Exception) {
            null
        }
    }

    fun handleJoinCampaign(context: Context): Boolean {
        var configData = campaign
        var targetUrl = configData?.popupConfig?.targetUrl
        if (targetUrl?.startsWith("graceword://") == true) {
            context.startActivity(Intent(Intent.ACTION_VIEW, targetUrl.toUri()))
            return true
        } else {
            return false
        }
    }

    fun finishLaunchOAMain(context: Context) {
        val tomorrow: String = DateLocalizationUtil.formatDayAgo(-1)
        context.getSharedPreferences("app_prefs", MODE_PRIVATE)
            .edit() {
                putString("when_show_oa", tomorrow)
            }
    }

//    fun toLaunchOAMain(): Boolean {
//        var config = campaign
//        if (config != null && config.popupConfig?.enable == true) {
//            var whenShow = MainApp.getContext().getSharedPreferences("app_prefs", MODE_PRIVATE)
//                .getString("when_show_oa", "")
//            Log.e("LauncherStep", "whenShow:${whenShow}")
//            return whenShow.isNullOrEmpty()
//        }
//        return false
//    }


    enum class LauncherStep {
        INIT, BEGINNER, MASKED, READY;

        fun isNext(next: LauncherStep): Boolean {
            when (this) {
                INIT -> {
                    return next == BEGINNER || next == READY
                }

                BEGINNER -> {
                    return next == MASKED
                }

                MASKED -> {
                    return next == READY
                }

                READY -> {
                    return false
                }
            }
        }
    }

    private var launcherStep = LauncherStep.INIT
    fun setLauncherStep(step: LauncherStep) {
        Log.e("LauncherStep", "$launcherStep->$step")
        if (launcherStep.isNext(step)) {
            launcherStep = step
        }

        if (launcherStep == LauncherStep.READY) {

            var config = campaign
            if (config != null) {
                var whenShow = MainApp.getContext().getSharedPreferences("app_prefs", MODE_PRIVATE)
                    .getString("when_show_oa", "")
                if (config.popupConfig?.enable == true && whenShow.isNullOrEmpty()) {
                    Log.e("LauncherStep", "whenShow:${whenShow},to launch main")
                    ChatSDK.events().source()
                        .accept(NetworkEvent(EventType.ShowOAMain, "main"))
                } else if (config.dailyPopupConfig?.enable == true && !whenShow.isNullOrEmpty()) {
                    val today: String = DateLocalizationUtil.formatDayAgo(0)
                    val toShow: Boolean = today >= whenShow
                    Log.e(
                        "LauncherStep",
                        "today:${today},whenShow:${whenShow},to launch mini:${toShow}"
                    )
                    if (toShow) {
                        ChatSDK.events().source()
                            .accept(NetworkEvent(EventType.ShowOAMain, "mini"))
                    }
                }
            }
        }
    }

    private var cacheBlessDay=0
    fun getBlessData(): Single<BlessData?> {
        return Single.create<BlessData?> { emitter ->
            try {
                var dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
                if(cacheBlessDay==dayOfYear) {
                    blessData?.let { cached ->
                        var today = formatDayAgo(0)
                        var last = cached.daily.lastOrNull()
                        if (last != null && last.date >= today) {
                            emitter.onSuccess(cached)
//                            Log.e("getCampaignData","bless from cached:$cacheCampaignDay")
                            return@create
                        }
                    }
                }

//                Log.e("getCampaignData","bless from network:$cacheBlessDay")
                val request = Request.Builder()
                    .url(requireNotNull(URL_CARD_DATA))
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
                                    cacheBlessDay = dayOfYear
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


    private var cacheCampaignDay=0
    fun getCampaignData(): Single<Campaign?> {
        return Single.create<Campaign?> { emitter ->
            try {
                var dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
                if(cacheCampaignDay==dayOfYear){
                    campaign?.let { cached ->
//                        Log.e("getCampaignData","campaign from cached:$cacheCampaignDay")
                        emitter.onSuccess(cached)
                        return@create
                    }
                }
//                Log.e("getCampaignData","campaign from network:$cacheCampaignDay")

                val request = Request.Builder()
                    .url(requireNotNull(URL_CAMPAIGN_DATA))
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
                                    .handleResponse(response, Campaign::class.java)
                                if (ret != null) {
                                    campaign = ret
                                    cacheCampaignDay = dayOfYear
                                }
                                if (campaign != null) {
                                    setLauncherStep(launcherStep)
                                    emitter.onSuccess(campaign!!)
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

    fun shareCardWithBitmap(
        bitmap: Bitmap,
        words: String
    ): Single<ShareResult> {
        return Single.create { emitter ->
            try {

                if (bitmap.isRecycled) {
                    emitter.onError(IllegalStateException("Bitmap已回收"))
                    return@create
                }

                // 2. 压缩图片
                val compressedData = compressBitmapToByteArray(bitmap, 5 * 1024 * 1024) // 5MB

                // 3. 创建请求
                val request = createShareRequest(
                    imageData = compressedData,
                    words = words
                )

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
                                    .handleResponse(response, ShareResult::class.java)
                                if (ret != null) {
                                    emitter.onSuccess(ret)
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
                emitter.onError(e)
            }
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    }

    /**
     * 创建分享请求
     */
    private fun createShareRequest(
        imageData: ByteArray,
        words: String,
    ): Request {
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("words", words)
            .addFormDataPart("date", DateLocalizationUtil.formatDayAgo(0))
            .addFormDataPart("lang", LanguageUtils.getAppLanguage(MainApp.getContext(), false))
            .addFormDataPart(
                "share_image",
                "bless_${System.currentTimeMillis()}.jpg",
                imageData.toRequestBody("image/jpeg".toMediaType(), 0, imageData.size)
            )
            .build()

        return Request.Builder()
            .url(URL_CARD_SHARE)
            .post(requestBody)
            .build()
    }

    /**
     * 压缩Bitmap到字节数组
     */
    private fun compressBitmapToByteArray(
        bitmap: Bitmap,
        maxSize: Int
    ): ByteArray {
        val outputStream = ByteArrayOutputStream()

        // 从高质量开始，逐步降低
        var quality = 85
        var compressedData: ByteArray

        do {
            outputStream.reset()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            compressedData = outputStream.toByteArray()
            quality -= 10
        } while (compressedData.size > maxSize && quality > 20)

        outputStream.close()

        // 如果仍然过大，进行尺寸缩放
        if (compressedData.size > maxSize) {
            return compressBitmapByScaling(bitmap, maxSize)
        }

        return compressedData
    }

    /**
     * 通过缩放压缩Bitmap
     */
    private fun compressBitmapByScaling(bitmap: Bitmap, maxSize: Int): ByteArray {
        val outputStream = ByteArrayOutputStream()

        // 计算缩放比例
        val targetScale = Math.sqrt(maxSize.toDouble() / (bitmap.byteCount * 0.7)).toFloat()
        val newWidth = (bitmap.width * targetScale).toInt().coerceAtLeast(1)
        val newHeight = (bitmap.height * targetScale).toInt().coerceAtLeast(1)

        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)

        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        val compressedData = outputStream.toByteArray()

        outputStream.close()
        scaledBitmap.recycle()

        return compressedData
    }


}