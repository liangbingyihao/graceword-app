package sdk.chat.demo.robot.handlers;

import android.util.Log
import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.text.Layout
import android.text.Spannable
import android.text.SpannableString
import android.text.StaticLayout
import android.text.TextPaint
import android.text.style.UnderlineSpan
import android.util.LruCache
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.FileProvider
import java.util.concurrent.Executors
import androidx.core.graphics.createBitmap
import sdk.chat.demo.MainApp
import java.io.File
import java.io.FileOutputStream
import androidx.core.graphics.scale
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.transition.Transition
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import sdk.chat.demo.pre.R
import com.bumptech.glide.request.target.CustomTarget
import kotlinx.coroutines.suspendCancellableCoroutine
import sdk.chat.demo.robot.api.model.ImageDaily
import sdk.chat.demo.robot.extensions.compressToSafeSize
import java.io.IOException
import kotlin.coroutines.resumeWithException
import sdk.chat.demo.robot.utils.FontManager
import sdk.chat.demo.robot.utils.FontUtils
import sdk.chat.demo.robot.utils.ViewCoordinateUtils

object CardGenerator {

    // 内存缓存（缓存生成的卡片）
    private val memoryCache = LruCache<String, Bitmap>(10 * 1024 * 1024) // 10MB

    // 内存缓存（缓存生成的卡片）
    private val rectCache = LruCache<String, RectF>(2 * 1024)

    // 获取缓存目录
    private val cacheDir by lazy {
        File(MainApp.getContext().cacheDir, "card_cache").apply { mkdirs() }
    }

    /**
     * 获取缓存图片的本地Uri（兼容Android 7+ FileProvider）
     * @param key 缓存键（同生成时使用的key）
     * @return 返回 content:// 或 file:// Uri
     */
    fun getCachedCardUri(key: String): Uri? {
        val file = getCachedCardFile(key) ?: return null
        return if (file.exists()) {
            // 适配Android 7+ FileProvider
            FileProvider.getUriForFile(
                MainApp.getContext(),
                "${MainApp.getContext().packageName}.provider",
                file
            )
        } else {
            null
        }
    }

    /**
     * 从磁盘缓存获取卡片
     */
    fun getCachedCardFile(key: String): File? {
        val file = File(cacheDir, "${key.hashCode()}.jpg")
        return if (file.exists()) file else null
    }

    /**
     * 保存卡片到磁盘缓存
     */
    private fun saveCardToCache(key: String, bitmap: Bitmap): Boolean {
        return try {
            val file = File(cacheDir, "${key.hashCode()}.jpg")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            true
        } catch (e: Exception) {
            false
        }
    }


    /**
     * 清理缓存
     */
    fun clearCache() {
        memoryCache.evictAll()
        cacheDir.listFiles()?.forEach { it.delete() }
    }

    // 获取屏幕宽度
    private fun getScreenWidth(context: Context): Int {
        return context.resources.displayMetrics.widthPixels
    }

    // 获取屏幕高度
    private fun getScreenHeight(context: Context): Int {
        return context.resources.displayMetrics.heightPixels
    }

    fun getCacheBitmap(cacheKey: String): Bitmap? {
        memoryCache.get(cacheKey)?.let {
            return it
        }
        getCachedCardFile(cacheKey)?.let { file ->
            BitmapFactory.decodeFile(file.absolutePath)?.let {
                memoryCache.put(cacheKey, it)
                return it
            }
        }
        return null
    }

    fun getCacheKey(
        resId: Int,
        imageDetail: ImageDaily,
        withQRCode: Boolean = true,
        isUnderline:Boolean = false
    ): String {
        if (resId == R.layout.item_image_gw) {
            return "image_daily_gw_${imageDetail.date}${withQRCode}${isUnderline}"
        } else if (resId == R.layout.item_image_greeting) {
            return "${imageDetail.backgroundUrl}|${imageDetail.scripture}|${imageDetail.greeting}|${withQRCode}|${isUnderline}"
        } else {
            return "${imageDetail.backgroundUrl}|${imageDetail.scripture}|${withQRCode}|${isUnderline}"
        }
        return ""
    }

    fun getCacheRect(
        cacheKey: String?
    ): RectF? {
        if (cacheKey != null && !cacheKey.isEmpty()) {
            var r = rectCache.get(cacheKey)
            if (r != null) {
                return r
            }
        }
        return null
    }

    /**
     * 从网络图片URL生成卡片（异步）
     * @param imageUrl 网络图片地址
     * @param text 卡片文本
     * @param callback 结果回调（主线程执行）
     */
    fun generateBibleCard(
        context: Context,
        resId: Int,
        imageDetail: ImageDaily,
        withQRCode: Boolean = true,
        isUnderline: Boolean = false,
        onSuccess: (Bitmap) -> Unit,
        onFailure: (Throwable) -> Unit
    ) {
        assert(resId == R.layout.view_popup_image_bible || resId == R.layout.item_image_gw || resId == R.layout.item_image_greeting)
        var cacheKey: String? = getCacheKey(resId, imageDetail, withQRCode,isUnderline)
        var imageUrl: String? = imageDetail.backgroundUrl

        if (cacheKey == null || cacheKey.isEmpty()) {
            onFailure(Exception("Bad cache key"))
        }

//        // 1. 检查内存缓存
        memoryCache.get(cacheKey)?.let {
            if (!it.isRecycled) {
                onSuccess(it)
                return
            }
        }

        // 使用Glide加载网络图片
        Glide.with(context)
            .asBitmap()
            .load(imageUrl)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(
                    resource: Bitmap,
                    transition: Transition<in Bitmap>?
                ) {
                    // 生成卡片
                    CoroutineScope(Dispatchers.Main).launch {
                        try {

                            val bitmap = captureLayoutAsync(
                                context,
                                resId,
                                imageDetail,
                                withQRCode,
                                isUnderline,
                                resource,
                            )
                            if (bitmap != null) {
                                memoryCache.put(cacheKey, bitmap)
//                                saveCardToCache(cacheKey, bitmap)
                                onSuccess(bitmap)
                            } else {
                                onFailure(Throwable("生成失败"))
                            }
                        } catch (e: Exception) {
                            onFailure(e)
                        }
                    }
                }

                override fun onLoadFailed(errorDrawable: Drawable?) {
                    val error = IOException("图片加载失败: $imageUrl")
                    onFailure(error)
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    // 清理资源时的操作（可选）
                }
            })
    }

    suspend fun captureLayoutAsync(
        context: Context,
        layoutResId: Int,
        imageDetail: ImageDaily,
        withQRCode: Boolean = true,
        isUnderline: Boolean = false,
        bitmaps: Bitmap,
    ): Bitmap? = withContext(Dispatchers.Main) {
        val view = LayoutInflater.from(context).inflate(layoutResId, null, false).apply {
            setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        }

        val img = view.findViewById<ImageView>(R.id.photoView)
        img.setImageBitmap(bitmaps)

        try {
            view.findViewById<TextView>(R.id.bible).apply {
                text = imageDetail.scripture
            }
            view.findViewById<TextView>(R.id.reference)?.apply {
                var reference = imageDetail.reference?.let { "($it)" }.orEmpty()
                if(isUnderline){
                    val spannable = SpannableString(reference)
                    spannable.setSpan(
                        UnderlineSpan(),
                        0, // 开始位置
                        reference.length, // 结束位置
                        Spannable.SPAN_INCLUSIVE_INCLUSIVE
                    )
                    text = spannable
                }else{
                    text = reference
                }
            }
            view.findViewById<TextView>(R.id.day)?.apply {
                visibility = View.INVISIBLE
            }
            view.findViewById<TextView>(R.id.month)?.apply {
                visibility = View.INVISIBLE
            }
            if (!withQRCode) {
                view.findViewById<View>(R.id.footer).apply {
                    visibility = View.INVISIBLE
                }
            }

            if (layoutResId == R.layout.item_image_greeting) {
                var typeface: Typeface? = null
                if (!imageDetail.greeting.isNullOrEmpty()) {
                    var fontManager = FontManager.getInstance(MainApp.getContext())
                    var font =
                        fontManager.getCacheFile(imageDetail.fontUrl)
                    typeface = FontUtils.loadFont(
                        MainApp.getContext(),
                        font
                    )
                }

                view.findViewById<TextView>(R.id.messageInput).apply {
                    this.typeface = typeface
                    text = imageDetail.greeting
                }

            }
            var viewContent: View = view.findViewById<View>(R.id.content)
            val widthSpec =
                View.MeasureSpec.makeMeasureSpec(
                    getScreenWidth(context),
                    View.MeasureSpec.EXACTLY
                )
            // 对于高度，使用UNSPECIFIED让视图能够根据内容自动调整高度
            var heightSpec = 0
            heightSpec = if(R.layout.view_popup_image_bible==layoutResId){
                //长图..
                View.MeasureSpec.makeMeasureSpec(
                    0,
                    View.MeasureSpec.UNSPECIFIED
                )
            }else{
                View.MeasureSpec.makeMeasureSpec(
                    getScreenHeight(context),
                    View.MeasureSpec.EXACTLY
                )
            }
            viewContent.measure(widthSpec, heightSpec)

            // 确保高度至少为屏幕高度的一半，避免内容过少时图片太小
            val minHeight = getScreenHeight(context) / 2
            val finalHeight = maxOf(viewContent.measuredHeight, minHeight)

            viewContent.layout(0, 0, viewContent.measuredWidth, finalHeight)
            var rv = view.findViewById<TextView>(R.id.reference)
            if(rv!=null&&isUnderline){
                var cacheKey: String? = getCacheKey(layoutResId, imageDetail, withQRCode,isUnderline)
                if (cacheKey != null && !cacheKey.isEmpty()) {
                    var rect = ViewCoordinateUtils.getViewBoundsInBitmap(
                        rv,
                        viewContent
                    )
                    rectCache.put(cacheKey, rect)
                }
            }

            // 使用计算好的最终高度创建bitmap，确保长文本能够完整显示

            createBitmap(
                viewContent.measuredWidth.coerceAtLeast(1),
                finalHeight.coerceAtLeast(1),
                Bitmap.Config.ARGB_8888
            ).apply {
                Canvas(this).run { viewContent.draw(this) }
            }.compressToSafeSize()
        } catch (e: Exception) {
            Log.e("biblewallGlide", "captureLayoutAsync failed $e")
        } as Bitmap?
    }

}