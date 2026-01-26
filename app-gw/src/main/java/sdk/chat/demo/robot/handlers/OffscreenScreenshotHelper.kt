package sdk.chat.demo.robot.handlers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.graphics.createBitmap
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import sdk.chat.demo.robot.api.model.Song
import kotlinx.coroutines.*
import sdk.chat.demo.pre.R
import sdk.chat.demo.robot.extensions.compressToSafeSize
import sdk.chat.demo.robot.ui.MarkdownRenderer
import java.io.IOException

object OffscreenScreenshotHelper {
    // ========== 配置方法 ==========

    fun screenshot(
        context: Context,
        headerUrl: String? = null,
        qrCodeUrl: String? = null,
        downloadPrompt: String? = null,
        buttonConfigs: List<ButtonConfig>? = null,
        onSuccess: (Bitmap) -> Unit,
        onFailure: (Throwable) -> Unit
    ) {

        Glide.with(context)
            .asBitmap()
            .load(headerUrl)
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
                                buttonConfigs,
                                resource,
                            )
                            if (bitmap != null) {
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
                    onFailure(IOException("图片加载失败: $headerUrl"))
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    // 清理资源时的操作（可选）
                }
            })

//
//        // 设置头图
//        headerUrl?.let { url ->
//            screenshotContainer.setHeaderImage(url)
//        }
//
//        // 设置二维码
//        qrCodeUrl?.let { url ->
//            screenshotContainer.setQrCodeImage(url)
//        }
//
//        // 设置下载提示
//        downloadPrompt?.let { prompt ->
//            screenshotContainer.setDownloadPrompt(prompt)
//        }
//
//        // 添加文本内容
//        textContents?.forEach { text ->
//            screenshotContainer.addTextView(text)
//        }
//
//        // 添加按钮
//        buttonConfigs?.forEach { config ->
//            screenshotContainer.addMaterialButton(
//                config.text,
//                config.styleResId,
//                config.clickListener
//            )
//        }
    }


    suspend fun captureLayoutAsync(
        context: Context,
        buttonConfigs: List<ButtonConfig>? = null,
        bitmaps: Bitmap,
    ): Bitmap? = withContext(Dispatchers.Main) {
        val view =
            LayoutInflater.from(context).inflate(R.layout.screenshot_container, null, false)
                .apply {
                    setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                }

        val llContainer = view.findViewById<LinearLayout>(R.id.ll_container)
        val llContent = view.findViewById<LinearLayout>(R.id.ll_content)
        val ivHeader = view.findViewById<ImageView>(R.id.iv_header)

        ivHeader.setImageBitmap(bitmaps)
        buttonConfigs?.forEach { config ->
            val itemView = LayoutInflater.from(llContent.context).inflate(config.resId, llContent, false)
            if(R.layout.screenshot_item_song==config.resId) {
                var song = config.song
                if(song!=null){
                    var tvSongTitle: TextView = itemView.findViewById(R.id.tvSongTitle)
                    tvSongTitle.text = song.title
                    var tvAlbum: TextView = itemView.findViewById(R.id.tvAlbum)
                    tvAlbum.text =
                        context.getString(R.string.album, song.composer, song.lyricist, song.album, song.artist)
                    var tvLyrics: TextView = itemView.findViewById(R.id.tvLyrics)
                    var tvCopyright: TextView = itemView.findViewById(R.id.tvCopyright)
                    tvLyrics.text = song.lyrics
                    tvCopyright.text = song.copyright
                }else{
                    itemView.visibility = View.GONE
                }
            }else if (R.layout.screenshot_item_ai_msg==config.resId){
                MarkdownRenderer.markwon.setMarkdown((itemView as TextView), config.text)
            }else{
                (itemView as TextView).text = config.text
            }
            llContent.addView(itemView)
        }


// 5. 获取屏幕宽度
        val screenWidth = context.resources.displayMetrics.widthPixels

        // 6. 正确的测量流程
        // 6.1 先测量整个视图
        view.measure(
            View.MeasureSpec.makeMeasureSpec(screenWidth, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )

        // 6.2 获取测量后的高度
        val totalHeight = view.measuredHeight

        // 6.3 如果高度为0，使用内容高度
        val finalHeight = if (totalHeight > 0) totalHeight else {
            llContainer.measure(
                View.MeasureSpec.makeMeasureSpec(screenWidth, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
            llContainer.measuredHeight
        }

        llContainer.layout(0, 0, screenWidth, finalHeight)
        createBitmap(
            screenWidth,
            finalHeight.coerceAtLeast(1),
            Bitmap.Config.ARGB_8888
        ).apply {
            Canvas(this).run {
                drawColor(Color.WHITE)
                llContainer.draw(this)
            }
        }.compressToSafeSize()
    }
    // ========== 数据类和接口 ==========

    data class ButtonConfig(
        val text: String,
        val resId: Int = 0,
        val song:Song? = null,
//        @androidx.annotation.StyleRes val styleResId: Int = 0,
    )

    interface ScreenshotCallback {
        fun onScreenshotComplete(screenshot: Bitmap?)
        fun onScreenshotStart() {
            // 默认空实现
        }
    }
}