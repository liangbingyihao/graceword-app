package sdk.chat.demo.robot.ops

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okio.IOException
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

class ImageCacheManager(context: Context) {

    companion object {
        private const val MAX_MEMORY_CACHE_SIZE = 20 * 1024 * 1024L // 20MB
        private const val MAX_DISK_CACHE_SIZE = 50 * 1024 * 1024L // 50MB
    }

    private val context: Context = context.applicationContext

    // 内存缓存
    private val memoryCache: LruCache<String, Bitmap> = object : LruCache<String, Bitmap>(
        (Runtime.getRuntime().maxMemory() / 8).toInt()
    ) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount
        }
    }

    // 磁盘缓存目录
    private val diskCacheDir: File = File(context.cacheDir, "web_image_cache")

    // HTTP客户端
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    init {
        if (!diskCacheDir.exists()) {
            diskCacheDir.mkdirs()
        }
    }

    fun getImage(url: String): Bitmap? {
        // 1. 从内存缓存获取
        var bitmap = memoryCache.get(generateKey(url))
        if (bitmap != null && !bitmap.isRecycled) {
            return bitmap
        }

        // 2. 从磁盘缓存获取
        bitmap = getFromDiskCache(url)
        if (bitmap != null) {
            // 存入内存缓存
            memoryCache.put(generateKey(url), bitmap)
            return bitmap
        }

        return null
    }

    suspend fun downloadAndCacheImage(url: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0")
                .build()

            val response = okHttpClient.newCall(request).execute()

            if (response.isSuccessful) {
                val body = response.body?.bytes() ?: return@withContext null
                val bitmap = BitmapFactory.decodeByteArray(body, 0, body.size)

                if (bitmap != null) {
                    // 缓存到内存
                    memoryCache.put(generateKey(url), bitmap)

                    // 缓存到磁盘
                    saveToDiskCache(url, body)

                    return@withContext bitmap
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return@withContext null
    }

    private fun getFromDiskCache(url: String): Bitmap? {
        val key = generateKey(url)
        val file = File(diskCacheDir, key)

        if (!file.exists()) return null

        return try {
            BitmapFactory.decodeFile(file.absolutePath)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun saveToDiskCache(url: String, data: ByteArray) {
        val key = generateKey(url)
        val file = File(diskCacheDir, key)

        try {
            if (!file.exists()) {
                file.createNewFile()
            }

            FileOutputStream(file).use { fos ->
                fos.write(data)
                fos.flush()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun generateKey(url: String): String {
        return try {
            val digest = MessageDigest.getInstance("MD5")
            digest.update(url.toByteArray())
            val bytes = digest.digest()

            val hexString = StringBuilder()
            for (byte in bytes) {
                val hex = Integer.toHexString(0xff and byte.toInt())
                if (hex.length == 1) hexString.append('0')
                hexString.append(hex)
            }
            hexString.toString()
        } catch (e: Exception) {
            url.hashCode().toString()
        }
    }

    fun clearCache() {
        // 清空内存缓存
        memoryCache.evictAll()

        // 清空磁盘缓存
        if (diskCacheDir.exists()) {
            diskCacheDir.listFiles()?.forEach { it.delete() }
        }
    }

    fun getCacheSize(): Long {
        return if (diskCacheDir.exists()) {
            diskCacheDir.listFiles()?.sumOf { it.length() } ?: 0
        } else {
            0
        }
    }
}