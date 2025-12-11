package sdk.chat.demo.robot.utils

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.res.ResourcesCompat
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okio.buffer
import okio.sink
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class FontManager private constructor(context: Context) {

    companion object {
        @Volatile
        private var INSTANCE: FontManager? = null
        private val TAG: String = "FontManager"

        fun getInstance(context: Context): FontManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FontManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    // 内存缓存
    private val memoryCache = ConcurrentHashMap<String, Typeface>()

    // 磁盘缓存目录
    private val cacheDir: File
    private val context: Context

    // OkHttp客户端
    private val okHttpClient: OkHttpClient

    // 协程作用域
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // 回调处理器
    private val mainHandler = Handler(Looper.getMainLooper())

    init {
        this.context = context.applicationContext
        this.cacheDir = File(this.context.cacheDir, "fonts")

        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }

        okHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    /**
     * 下载并加载字体
     */
    fun loadFont(
        fontUrl: String,
        callback: FontCallback? = null
    ): Job {
        return scope.launch {
            try {
                // 检查内存缓存
                val memoryKey = md5(fontUrl)
                memoryCache[memoryKey]?.let { typeface ->
                    callback?.onSuccess(typeface)
                    Log.e(TAG, "from memoryCache")
                    return@launch
                }

                // 检查磁盘缓存
                val cachedFile = getCachedFontFile(fontUrl)
                if (cachedFile != null && cachedFile.exists()) {
                    val typeface = FontUtils.loadFont(context, cachedFile)
                    if (typeface != null) {
                        memoryCache[memoryKey] = typeface
                        callback?.onSuccess(typeface)
                        Log.e(TAG, "from cachedFile")
                        return@launch
                    }
                }

                // 下载字体
                Log.e(TAG, "from network")
                callback?.onProgress(0)
                val fontFile = downloadFont(fontUrl) { progress ->
                    callback?.onProgress(progress)
                }

                if (fontFile != null && fontFile.exists()) {
                    val typeface = FontUtils.loadFont(context, fontFile)
                    if (typeface != null) {
                        memoryCache[memoryKey] = typeface
                        callback?.onSuccess(typeface)
                    } else {
                        callback?.onError(Exception("字体加载失败"))
                    }
                } else {
                    callback?.onError(Exception("字体下载失败"))
                }
            } catch (e: Exception) {
                callback?.onError(e)
            }
        }
    }

    /**
     * 下载字体文件
     */
    private suspend fun downloadFont(
        url: String,
        onProgress: (Int) -> Unit
    ): File? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        val cacheFile = getCacheFile(url)

        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("下载失败: ${response.code}")
            }

            val body = response.body
            if (body == null) {
                throw IOException("响应体为空")
            }

            val contentLength = body.contentLength()
            var totalBytesRead = 0L

            val sink = cacheFile.sink().buffer()
            val source = body.source()

            try {
                while (true) {
                    val read = source.read(sink.buffer, 8192)
                    if (read == -1L) break

                    totalBytesRead += read

                    if (contentLength > 0) {
                        val progress = ((totalBytesRead.toDouble() / contentLength) * 100).toInt()
                        onProgress(progress.coerceIn(0, 100))
                    }
                }
            } finally {
                sink.close()
            }

            return@withContext cacheFile
        }
    }

    /**
     * 获取缓存文件
     */
    public fun getCacheFile(url: String): File {
        val fileName = md5(url)
        val extension = getFileExtension(url)
        return File(cacheDir, "$fileName.$extension")
    }

    /**
     * 获取缓存中的字体文件
     */
    private fun getCachedFontFile(url: String): File? {
        val fileName = md5(url)
        val extension = getFileExtension(url)
        val file = File(cacheDir, "$fileName.$extension")

        return if (file.exists() && file.isFile && file.length() > 0) {
            file
        } else {
            null
        }
    }

    /**
     * 获取文件扩展名
     */
    private fun getFileExtension(url: String): String {
        return url.substringAfterLast('.', "").lowercase().takeIf { it.isNotEmpty() } ?: "ttf"
    }

    /**
     * 计算MD5
     */
    private fun md5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(input.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    /**
     * 清除缓存
     */
    fun clearCache() {
        memoryCache.clear()

        if (cacheDir.exists() && cacheDir.isDirectory) {
            cacheDir.listFiles()?.forEach { it.delete() }
        }
    }

    /**
     * 获取缓存大小
     */
    fun getCacheSize(): Long {
        return if (cacheDir.exists() && cacheDir.isDirectory) {
            cacheDir.listFiles()?.sumOf { it.length() } ?: 0L
        } else {
            0L
        }
    }

    /**
     * 预加载字体
     */
    fun preloadFonts(vararg fontUrls: String) {
        scope.launch {
            fontUrls.forEach { url ->
                loadFont(url)
            }
        }
    }

    interface FontCallback {
        fun onSuccess(typeface: Typeface)
        fun onError(exception: Exception)
        fun onProgress(progress: Int)
    }
}