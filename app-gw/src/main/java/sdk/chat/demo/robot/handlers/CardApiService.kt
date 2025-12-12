package sdk.chat.demo.robot.handlers

import android.util.Log
import com.google.gson.Gson
import android.graphics.Bitmap
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
import sdk.chat.demo.MainApp
import sdk.chat.demo.robot.api.GWApiManager
import sdk.chat.demo.robot.api.ImageApi
import sdk.chat.demo.robot.api.JsonCacheManager.get
import sdk.chat.demo.robot.api.JsonCacheManager.save
import sdk.chat.demo.robot.api.model.BlessData
import sdk.chat.demo.robot.api.model.ShareResult
import sdk.chat.demo.robot.extensions.DateLocalizationUtil
import sdk.chat.demo.robot.extensions.LanguageUtils
import java.io.ByteArrayOutputStream
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
    val URL_CARD_SHARE: String = ImageApi.URL2_MAIN + "campaign/card/share"
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