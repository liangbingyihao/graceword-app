package sdk.chat.demo.robot.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.*
import sdk.chat.demo.pre.R
import androidx.core.view.isVisible

class ScreenshotContainer(context: Context) : FrameLayout(context) {

    // 布局视图
    private lateinit var llContainer: LinearLayout
    private lateinit var ivHeader: ImageView
    private lateinit var footerLayout: FrameLayout
    private lateinit var ivQrCode: ImageView
    private lateinit var tvDownloadPrompt: TextView
    private lateinit var llContent: LinearLayout
    private var containerWidth: Int = 0

//    // 图片缓存
    private var headerBitmap: Bitmap? = null
//    private var qrCodeBitmap: Bitmap? = null

    // 协程作用域
    private val imageLoadScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    init {
        initFromLayout()
        setupContainerWidth()
    }

    private fun initFromLayout() {
        // 从布局文件加载
        val inflater = LayoutInflater.from(context)
        val rootView = inflater.inflate(R.layout.screenshot_container, this, true)

        // 初始化视图
        llContainer = rootView.findViewById(R.id.ll_container)
        ivHeader = rootView.findViewById(R.id.iv_header)
        footerLayout = rootView.findViewById(R.id.footer)
        ivQrCode = rootView.findViewById(R.id.iv_qr_code)
        tvDownloadPrompt = rootView.findViewById(R.id.tv_download_prompt)
        llContent = rootView.findViewById(R.id.ll_content)

        // 设置软件渲染
        setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        visibility = View.INVISIBLE
    }

    private fun setupContainerWidth() {
        // 获取屏幕宽度作为容器宽度
        val displayMetrics = context.resources.displayMetrics
        containerWidth = displayMetrics.widthPixels

        // 设置固定宽度
        layoutParams = LayoutParams(containerWidth, LayoutParams.WRAP_CONTENT)
    }

    fun setHeaderImage(bitmap: Bitmap) {
        headerBitmap = bitmap
        ivHeader.setImageBitmap(bitmap)
        // 强制重新测量和布局
        ivHeader.post {
            ivHeader.requestLayout()
        }
    }

    // ========== 底部布局配置方法 ==========

    /**
     * 设置二维码图片 URL
     */
    fun setQrCodeImage(url: String, placeholder: Int = 0, error: Int = 0) {
        Glide.with(context)
            .asBitmap()
            .load(url)
            .apply(
                RequestOptions()
                    .placeholder(placeholder)
                    .error(error)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
            )
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
//                    qrCodeBitmap = resource
                    ivQrCode.setImageBitmap(resource)
                    requestLayout()
                }

                override fun onLoadCleared(placeholder: android.graphics.drawable.Drawable?) {
//                    qrCodeBitmap = null
                }
            })
    }

    /**
     * 设置二维码图片资源
     */
    fun setQrCodeImage(@DrawableRes resId: Int) {
        ivQrCode.setImageResource(resId)
//        qrCodeBitmap = null
    }

    /**
     * 设置二维码图片 Bitmap
     */
    fun setQrCodeImage(bitmap: Bitmap) {
//        qrCodeBitmap = bitmap
        ivQrCode.setImageBitmap(bitmap)
    }

    /**
     * 设置下载提示文本
     */
    fun setDownloadPrompt(text: String) {
        tvDownloadPrompt.text = text
    }

    /**
     * 设置下载提示文本资源
     */
    fun setDownloadPrompt(@StringRes resId: Int) {
        tvDownloadPrompt.setText(resId)
    }

    /**
     * 设置底部布局背景颜色
     */
    fun setFooterBackgroundColor(color: Int) {
        footerLayout.setBackgroundColor(color)
    }

    /**
     * 设置二维码尺寸
     */
    fun setQrCodeSize(width: Int, height: Int) {
        val params = ivQrCode.layoutParams as FrameLayout.LayoutParams
        params.width = width
        params.height = height
        ivQrCode.layoutParams = params
    }

    /**
     * 设置二维码边距
     */
    fun setQrCodeMargin(margin: Int) {
        val params = ivQrCode.layoutParams as FrameLayout.LayoutParams
        params.setMargins(margin, margin, margin, margin)
        ivQrCode.layoutParams = params
    }

    /**
     * 设置提示文本边距
     */
    fun setPromptMargin(left: Int, top: Int, right: Int, bottom: Int) {
        val params = tvDownloadPrompt.layoutParams as FrameLayout.LayoutParams
        params.setMargins(left, top, right, bottom)
        tvDownloadPrompt.layoutParams = params
    }

    // ========== 内容管理方法 ==========

    fun addTextView(text: String, textColor: Int = Color.BLACK, textSize: Float = 16f) {
        val textView = TextView(context).apply {
            this.text = text
            setTextColor(textColor)
            setTextSize(textSize)

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 8, 0, 8)
            }

            setPadding(16, 16, 16, 16)
            setBackgroundColor(Color.WHITE)
        }

        llContent.addView(textView)
    }

    fun addMaterialButton(
        text: String,
        @androidx.annotation.StyleRes styleResId: Int = 0,
        clickListener: View.OnClickListener? = null
    ) {
        val button = if (styleResId != 0) {
            MaterialButton(context, null, styleResId)
        } else {
            MaterialButton(context)
        }

        button.apply {
            this.text = text
            setOnClickListener(clickListener)

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 16, 0, 16)
            }

            setPadding(32, 16, 32, 16)
            cornerRadius = 8
        }

        llContent.addView(button)
    }

    fun addDivider(color: Int = Color.LTGRAY, height: Int = 1) {
        val divider = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                height
            ).apply {
                setMargins(0, 16, 0, 16)
            }
            setBackgroundColor(color)
        }

        llContent.addView(divider)
    }

    fun clearContent() {
        llContent.removeAllViews()
    }

    // ========== 可见性控制 ==========

    fun setHeaderVisibility(visible: Boolean) {
        ivHeader.visibility = if (visible) View.VISIBLE else View.GONE
    }

    fun setFooterVisibility(visible: Boolean) {
        footerLayout.visibility = if (visible) View.VISIBLE else View.GONE
    }

    /**
     * 执行完整的测量和布局过程
     */
    private fun performMeasureAndLayout() {
        // 第一步：测量容器
        val widthMeasureSpec = MeasureSpec.makeMeasureSpec(containerWidth, MeasureSpec.EXACTLY)
        val heightMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)

        measure(widthMeasureSpec, heightMeasureSpec)

        // 第二步：布局容器
        layout(0, 0, measuredWidth, measuredHeight)

        // 第三步：确保子视图完成布局
        ensureChildrenLayout()
    }

    /**
     * 确保所有子视图完成布局
     */
    private fun ensureChildrenLayout() {
        // 强制头图完成布局
        if (ivHeader.visibility == View.VISIBLE) {
            ivHeader.measure(
                MeasureSpec.makeMeasureSpec(containerWidth, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
            )

            // 计算头图高度（保持宽高比）
            if (ivHeader.drawable != null) {
                val drawable = ivHeader.drawable
                val scale = containerWidth.toFloat() / drawable.intrinsicWidth
                val headerHeight = (drawable.intrinsicHeight * scale).toInt()
                ivHeader.layoutParams.height = headerHeight
            }
        }

        // 强制内容区域完成布局
        llContent.measure(
            MeasureSpec.makeMeasureSpec(containerWidth - 32, MeasureSpec.EXACTLY), // 减去padding
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        )

        // 强制底部布局完成布局
        if (footerLayout.visibility == View.VISIBLE) {
            footerLayout.measure(
                MeasureSpec.makeMeasureSpec(containerWidth, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
            )
        }
    }

    // ========== 截图核心方法 ==========

    fun captureLongScreenshot(): Bitmap? {
        return try {
            // 1. 强制完成测量和布局
            performMeasureAndLayout()

            // 2. 计算总高度
            val totalHeight = calculateTotalHeight()
            if (totalHeight <= 0) {
                return null
            }

            // 3. 使用固定的容器宽度
            val width = containerWidth

            // 4. 创建 Bitmap
            val bitmap = Bitmap.createBitmap(width, totalHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // 5. 绘制白色背景
            canvas.drawColor(Color.WHITE)

            // 6. 绘制内容
            drawToCanvas(canvas)

            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } as Bitmap?
    }

    private fun calculateTotalHeight(): Int {
        var totalHeight = 0

        // 头图高度
        if (ivHeader.isVisible) {
            if (ivHeader.drawable != null) {
                totalHeight += ivHeader.measuredHeight
            } else if (headerBitmap != null) {
                val scale = width.toFloat() / headerBitmap!!.width
                totalHeight += (headerBitmap!!.height * scale).toInt()
            }
        }

        // 内容区域高度
        totalHeight += llContent.measuredHeight

        // 底部布局高度
        if (footerLayout.visibility == View.VISIBLE) {
            totalHeight += footerLayout.measuredHeight
        }

        return totalHeight
    }

    private fun drawToCanvas(canvas: Canvas) {
        var currentY = 0

        // 绘制头图
        if (ivHeader.isVisible && (ivHeader.drawable != null || headerBitmap != null)) {
            val headerHeight = if (ivHeader.drawable != null) {
                ivHeader.measuredHeight
            } else {
                val scale = width.toFloat() / headerBitmap!!.width
                (headerBitmap!!.height * scale).toInt()
            }

            if (headerHeight > 0) {
                canvas.save()
                canvas.translate(0f, currentY.toFloat())
                ivHeader.draw(canvas)
                canvas.restore()
                currentY += headerHeight
            }
        }

        // 绘制内容区域
        val contentHeight = llContent.measuredHeight
        if (contentHeight > 0) {
            canvas.save()
            canvas.translate(0f, currentY.toFloat())
            llContent.draw(canvas)
            canvas.restore()
            currentY += contentHeight
        }

        // 绘制底部布局
        if (footerLayout.visibility == View.VISIBLE) {
            val footerHeight = footerLayout.measuredHeight
            if (footerHeight > 0) {
                canvas.save()
                canvas.translate(0f, currentY.toFloat())
                footerLayout.draw(canvas)
                canvas.restore()
            }
        }
    }

    // ========== 工具方法 ==========

    override fun forceLayout() {
        measure(
            MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        )

        layout(left, top, right, top + measuredHeight)
    }

    fun cleanup() {
        imageLoadScope.cancel()
        Glide.with(context).clear(ivHeader)
        Glide.with(context).clear(ivQrCode)
        clearContent()
    }
}