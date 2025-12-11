package sdk.chat.demo.robot.service
import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import sdk.chat.demo.pre.R
import java.util.*
import kotlin.math.sin

class MyWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine {
        return MyWallpaperEngine()
    }

    private inner class MyWallpaperEngine : Engine() {

        private val handler = Handler(Looper.getMainLooper())
        private val drawRunnable = Runnable { drawFrame() }

        // 壁纸状态控制
        private var visible = false
        private var width = 0
        private var height = 0

        // 图片资源
        private val imageResources = listOf(
            R.mipmap.ic_intro_1,
            R.mipmap.ic_intro_2,
            R.mipmap.ic_intro_1_en,
            R.mipmap.ic_intro_2_en,
            R.mipmap.ic_intro_1_hk,
            R.mipmap.ic_intro_2_hk,
        )

        private var currentImageIndex = 0
        private var lastChangeTime = System.currentTimeMillis()
        private val changeInterval = 60 * 1000L // 1分钟

        // 文字动画相关
        private var textOffset = 0f
        private var textAlpha = 255
        private var textDirection = 1 // 1: 向右, -1: 向左
        private val textMessages = listOf(
            "神爱世人",
            "美好的一天开始了！",
            "保持微笑，生活更美好",
            "追逐梦想，永不放弃",
            "时光静好，珍惜当下",
            "心中有阳光，处处是风景",
        )
        private var currentMessageIndex = 0

        // 绘图工具
        private val paint = Paint().apply {
            isAntiAlias = true
            textSize = 60f
            color = Color.WHITE
            style = Paint.Style.FILL
            typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
        }

        private val shadowPaint = Paint().apply {
            isAntiAlias = true
            textSize = 60f
            color = Color.BLACK
            style = Paint.Style.FILL
            setShadowLayer(5f, 0f, 0f, Color.BLACK)
        }

        override fun onVisibilityChanged(visible: Boolean) {
            this.visible = visible
            if (visible) {
                drawFrame()
            } else {
                handler.removeCallbacks(drawRunnable)
            }
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            this.width = width
            this.height = height
            super.onSurfaceChanged(holder, format, width, height)
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            visible = false
            handler.removeCallbacks(drawRunnable)
        }

        override fun onDestroy() {
            super.onDestroy()
            handler.removeCallbacks(drawRunnable)
        }

        private fun drawFrame() {
            if (!visible) return

            val holder = surfaceHolder
            var canvas: Canvas? = null

            try {
                canvas = holder.lockCanvas()
                if (canvas != null) {
                    drawWallpaper(canvas)
                }
            } finally {
                if (canvas != null) {
                    holder.unlockCanvasAndPost(canvas)
                }
            }

            // 安排下一帧绘制
            handler.removeCallbacks(drawRunnable)
            handler.postDelayed(drawRunnable, 16) // 约60fps
        }

        private fun drawWallpaper(canvas: Canvas) {
            // 检查是否需要更换图片
            checkAndChangeImage()

            // 绘制当前图片
            drawBackgroundImage(canvas)

            // 绘制动态文字
            drawAnimatedText(canvas)

            // 更新时间显示
//            drawTimeInfo(canvas)
        }

        private fun checkAndChangeImage() {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastChangeTime >= changeInterval) {
                // 切换到下一张图片
                currentImageIndex = (currentImageIndex + 1) % imageResources.size
                currentMessageIndex = (currentMessageIndex + 1) % textMessages.size
                lastChangeTime = currentTime

                // 重置文字动画状态
                textOffset = 0f
                textAlpha = 255
            }
        }

        private fun drawBackgroundImage(canvas: Canvas) {
            try {
                val bitmap = BitmapFactory.decodeResource(
                    resources,
                    imageResources[currentImageIndex]
                )

                // 缩放图片以适应屏幕
                val scaledBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true)

                // 绘制图片
                canvas.drawBitmap(scaledBitmap, 0f, 0f, null)

                // 添加半透明遮罩，让文字更清晰
                canvas.drawColor(Color.argb(50, 0, 0, 0))

                // 回收bitmap
                scaledBitmap.recycle()
                bitmap.recycle()

            } catch (e: Exception) {
                // 如果图片加载失败，绘制纯色背景
                canvas.drawColor(getBackgroundColorByIndex(currentImageIndex))
            }
        }

        private fun getBackgroundColorByIndex(index: Int): Int {
            return when (index % 5) {
                0 -> Color.parseColor("#FF6B6B")
                1 -> Color.parseColor("#4ECDC4")
                2 -> Color.parseColor("#45B7D1")
                3 -> Color.parseColor("#96CEB4")
                4 -> Color.parseColor("#FECA57")
                else -> Color.BLUE
            }
        }

        private fun drawAnimatedText(canvas: Canvas) {
            val message = textMessages[currentMessageIndex]

            // 更新文字位置（左右移动）
            textOffset += 2f * textDirection
            val textWidth = paint.measureText(message)

            // 边界检测，改变方向
            if (textOffset > width - textWidth || textOffset < 0) {
                textDirection *= -1
            }

            // 文字透明度呼吸效果
            textAlpha = (125 + 130 * sin(System.currentTimeMillis() / 1000.0).toFloat()).toInt()
            textAlpha = textAlpha.coerceIn(100, 255)

            // 设置文字样式
            paint.alpha = textAlpha
            paint.color = getTextColorByIndex(currentImageIndex)

            shadowPaint.alpha = textAlpha

            // 计算文字位置（垂直居中）
            val textY = height * 0.8f

            // 先绘制阴影
            canvas.drawText(message, textOffset, textY, shadowPaint)
            // 再绘制文字
            canvas.drawText(message, textOffset, textY, paint)

            // 绘制文字背景（可选）
            drawTextBackground(canvas, message, textOffset, textY, textWidth)
        }

        private fun getTextColorByIndex(index: Int): Int {
            return when (index % 5) {
                0 -> Color.WHITE
                1 -> Color.YELLOW
                2 -> Color.CYAN
                3 -> Color.GREEN
                4 -> Color.MAGENTA
                else -> Color.WHITE
            }
        }

        private fun drawTextBackground(canvas: Canvas, text: String, x: Float, y: Float, textWidth: Float) {
            val backgroundPaint = Paint().apply {
                color = Color.argb(150, 0, 0, 0)
                style = Paint.Style.FILL
                isAntiAlias = true
            }

            val rectHeight = 80f
            val rectTop = y - 60f
            val rectBottom = y + 20f
            val padding = 20f

            // 绘制圆角矩形背景
            canvas.drawRoundRect(
                x - padding,
                rectTop,
                x + textWidth + padding,
                rectBottom,
                20f,
                20f,
                backgroundPaint
            )
        }

        private fun drawTimeInfo(canvas: Canvas) {
            val timePaint = Paint().apply {
                color = Color.WHITE
                textSize = 40f
                isAntiAlias = true
                typeface = Typeface.MONOSPACE
            }

            val calendar = Calendar.getInstance()
            val timeText = String.format(
                "%02d:%02d:%02d",
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                calendar.get(Calendar.SECOND)
            )

            val dateText = String.format(
                "%d年%d月%d日",
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH)
            )

            // 绘制时间
            canvas.drawText(timeText, 50f, 100f, timePaint)
            // 绘制日期
            canvas.drawText(dateText, 50f, 150f, timePaint)

            // 绘制图片切换倒计时
            val remainingTime = (changeInterval - (System.currentTimeMillis() - lastChangeTime)) / 1000
            val countdownText = "下一张: ${remainingTime}秒"
            canvas.drawText(countdownText, 50f, 200f, timePaint)
        }
    }
}